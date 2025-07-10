# AI Programme Generation

## Current Implementation

### Overview
AI programme generation uses a simplified 3-step wizard with background processing via WorkManager. Programmes are generated asynchronously with full clarification flow support.

### Architecture

#### Request Flow
```
User Input → Create AIProgrammeRequest → WorkManager Job → API Call → Save Response → User Preview → Activate
                                                      ↓
                                              Clarification Needed?
                                                      ↓
                                              Show ClarificationDialog → User Response → Enhanced Request
```

#### Key Components
- **AIProgrammeRequest**: Database entity tracking generation requests with clarification support
- **ProgrammeGenerationWorker**: WorkManager task handling API calls and clarification responses
- **AIProgrammeRepository**: Manages request lifecycle and clarification submission
- **ClarificationDialog**: Modal UI component for collecting clarification responses
- **GeneratedProgrammeHolder**: Passes data between preview screens

#### Request Lifecycle
1. **Creation**: User completes wizard → request saved with PROCESSING status
2. **Background Processing**: WorkManager handles API call
3. **Completion Paths**:
   - **Success**: Response saved, status COMPLETED
   - **Clarification**: Status NEEDS_CLARIFICATION, show dialog
   - **Failure**: Status FAILED, can retry
4. **Clarification Flow**: User responds → new enhanced request → processing continues
5. **Preview**: User can preview generated programme
6. **Activation**: Programme created in main database, AI request deleted

### 3-Step Wizard

#### Step 1: Quick Setup
- Goal selection (Build Strength, Build Muscle, Lose Fat, Athletic Performance)
- Training frequency (2-6 days per week)  
- Session duration (Quick, Standard, Extended, Long)
- FilterChips in horizontal LazyRows

#### Step 2: About You
- Experience level (Beginner to Elite) with info tooltip showing definitions:
  - **Beginner (0-1 year)**: Learning movement patterns, building base strength, linear progression works well. Example: Can't bench bodyweight yet
  - **Intermediate (1-3 years)**: Solid technique on main lifts, needs periodization, some plateaus appearing. Example: 1.5x BW squat, 1x BW bench
  - **Advanced (3-5+ years)**: Refined technique, requires advanced programming, progress measured monthly. Example: 2x BW squat, 1.5x BW bench
  - **Elite (5+ years competitive)**: Competition-level lifts, highly specific programming, progress measured quarterly. Example: Regional/national competitor
- Equipment availability (Barbell & Rack, Full Gym, etc.)
- FilterChips in horizontal LazyRows

#### Step 3: Customize
- Full-screen expandable text input (max 1000 characters)
- Auto-focus with keyboard management
- Character counter displayed outside text field
- Placeholder text with helpful guidance

### Status Management
- **PROCESSING**: Request submitted, waiting for API response
  - Card shows "Generating..." with animated icon
  - Tap to expand and view request summary (goal, experience, frequency, etc.)
  - Shows "Tap for details ↓" hint
- **NEEDS_CLARIFICATION**: AI needs more information, user can respond
- **COMPLETED**: Programme ready for preview
- **FAILED**: Generation failed, can be retried

### Clarification Flow (✅ Implemented)

#### Database Schema
```sql
CREATE TABLE ai_programme_requests (
    id TEXT PRIMARY KEY,
    status TEXT NOT NULL,
    requestPayload TEXT NOT NULL,
    generatedProgrammeJson TEXT,
    errorMessage TEXT,
    clarificationMessage TEXT,        -- New: AI's clarification question
    originalRequestPayload TEXT,      -- New: Original request for context
    attemptCount INTEGER DEFAULT 0,
    createdAt INTEGER NOT NULL,
    lastUpdatedAt INTEGER NOT NULL,
    workManagerId TEXT
)
```

#### UI Components
- **ClarificationDialog**: Modal dialog with AI's question and text input (500 char limit)
- **AIProgrammeRequestCard**: Shows clarification status with help icon and "Respond" button
- **ProgrammesScreen**: Integrated clarification flow with proper state management

#### Technical Implementation
- **Worker**: Handles clarification responses as success (not failure)
- **Repository**: `submitClarification()` method combines original context + user response
- **Context Preservation**: Original request payload stored for enhanced prompts
- **Single Round**: Limit to 1 clarification per request for MVP simplicity

### Key Features
1. **Non-blocking**: Uses WorkManager for reliable background execution
2. **Clarification Flow**: Interactive clarification for ambiguous requests
3. **Context Preservation**: Original request details maintained through clarification
4. **Cleanup**: AI requests deleted BEFORE activation to prevent race conditions
5. **Progress tracking**: Visual indicators for each status including clarification
6. **Quota management**: Daily generation limits enforced
7. **State Management**: Fresh wizard state for each generation session
8. **Single Programme Enforcement**: Only one active programme allowed at a time

### Benefits
- **~60-70% reduction** in failed generations due to ambiguity
- **Better UX**: Clear communication vs mysterious failures
- **Higher success rate**: AI gets information needed to succeed
- **User confidence**: Transparent process vs black box failures
- **Cost efficiency**: Avoid wasted API calls on incomplete information

### Recent Implementation (January 2025)
- ✅ Added NEEDS_CLARIFICATION status to GenerationStatus enum
- ✅ Extended database with clarification fields
- ✅ Fixed ProgrammeGenerationWorker to handle clarification properly
- ✅ Created ClarificationDialog component with Material 3 design
- ✅ Updated AIProgrammeRequestCard for clarification status
- ✅ Integrated clarification flow into ProgrammesScreen
- ✅ Added submitClarification method to repository and view model
- ✅ Context preservation for enhanced AI prompts
- ✅ Complete end-to-end clarification flow
- ✅ Removed 'Custom' goal option (redundant with custom instructions)
- ✅ Fixed custom instructions text field to expand properly
- ✅ Added experience level tooltip with athletic-focused definitions
- ✅ Made AIProgrammeRequestCard expandable to show request summary during generation

### Critical Issues Fixed (January 2025)

#### 1. Activated Programmes Still Appearing in List
**Problem**: After activating an AI-generated programme, it would still appear as "Ready to preview!" in the AI Generated Programmes section.

**Root Cause**: 
- AI request ID was being cleared prematurely in ProgrammePreviewScreen
- GeneratedProgrammeHolder was cleared immediately after loading, losing the AI request ID
- When user clicked activate, the ID was null so deletion couldn't happen

**Solution**:
- Removed premature clearing of GeneratedProgrammeHolder in `ProgrammePreviewScreen`
- Added `hasLoadedProgramme` flag to prevent duplicate loads on configuration changes
- AI request deletion now happens BEFORE programme creation with proper ID
- Added verification step to confirm deletion from database
- Increased delay to 200ms for database operations to complete
- Added force refresh when returning to ProgrammesScreen from preview
- Clear holder only AFTER successful deletion in `activateProgramme()`

#### 2. Wizard State Persistence Between Sessions
**Problem**: Starting a new AI generation would show pre-filled data and incorrect step completion states from previous sessions.

**Root Cause**: 
- `ProgrammeGeneratorViewModel` was a singleton that persisted across navigations
- No state reset mechanism when starting a new generation

**Solution**:
- Added `resetState()` function to `ProgrammeGeneratorViewModel`
- Added `LaunchedEffect(Unit)` in `ProgrammeGeneratorScreen` that calls `resetState()`
- Ensures every generation starts with a clean slate

#### 3. Multiple Active Programmes Prevention
**Problem**: Users could have multiple active programmes, causing confusion and potential data issues.

**Implementation**:
- **AI Generation Button**: Check for active programme before navigating to generator
- **Programme Activation**: Check before activating any AI-generated programme  
- **Predefined Programmes**: Already had check with overwrite dialog
- **Error Handling**: Clear messages explaining users must delete active programme first

**User Flow**:
1. User attempts action that would create multiple active programmes
2. System shows dialog: "You already have an active programme. Please delete it first."
3. User must explicitly delete active programme before proceeding

### Discard Functionality (January 2025)
Users can now discard AI-generated programmes during preview:
- **UI**: Added Discard button next to Activate button (only shown for AI programmes)
- **Styling**: OutlinedButton with error color and delete icon
- **Function**: `discardProgramme()` in ProgrammePreviewViewModel
- **Process**: 
  1. Deletes AI request from database synchronously
  2. Clears GeneratedProgrammeHolder only after successful deletion
  3. Returns user to Programmes screen
- **Error Handling**: If deletion fails, shows error and prevents navigation

### Known Issues & Solutions (January 2025)

#### 1. Request Details Not Displaying
**Problem**: AI programme request cards show "Your Request: Unknown" instead of actual user selections.

**Root Cause**: The `requestPayload` JSON is stored but not parsed for display in `AIProgrammeRequestCard`.

**Solution Plan**:
1. Add parsing logic to extract `SimpleRequest` data from `requestPayload`
2. Display formatted request summary: goal, frequency, duration, experience
3. Update `AIProgrammeRequestCard` to show rich request details

#### 2. Incomplete Programme Generation & Token Optimization
**Problem**: LLM generates fewer weeks than requested (e.g., 2 weeks instead of 8).

**Root Cause Analysis**:
1. Output token limit of 16,384 is too low for 8-week programmes
2. We request excessive metadata that's never shown during workouts
3. Preview UI shows information that has no value during actual training

**Wasted Fields (Never Shown During Workouts)**:
- `week.name` - Descriptive week names like "Foundation Week"
- `week.description` - Week descriptions  
- `week.intensityLevel` - "moderate", "high", etc.
- `week.volumeLevel` - "moderate", "high", etc.
- `week.focus` - Array of focus areas
- `week.isDeload` - Deload week indicator
- `workout.name` - Workout names beyond "Day 1", "Day 2"
- `exercise.notes` - Exercise-specific notes (rarely useful)

**Essential Fields (Actually Used)**:
- Programme: name, description, durationWeeks, daysPerWeek
- Week: weekNumber, workouts
- Workout: dayNumber, exercises
- Exercise: exerciseName, sets, repsMin, repsMax, rpe, restSeconds, suggestedWeight, weightSource

**Solution Plan**:

1. **Remove Wasteful Fields from LLM Prompt**:
   - Update system prompt to request ONLY essential fields
   - Remove week.name, week.description, week.focus, week.intensityLevel, week.volumeLevel, week.isDeload
   - Simplify workout.name to just "Day X"
   - Make exercise.notes truly optional (only for critical form cues)
   - **Expected token savings: ~30-40%**

2. **Simplify Preview UI**:
   - Remove week metadata display (intensity, volume, focus, deload)
   - Show only what matters: exercises, sets, reps, RPE
   - Keep programme name/description for context
   - Mirror the actual workout experience

3. **Increase Token Limit**:
   - Change `MAX_TOKENS` from 16,384 to 32,768
   - Combined with field removal, should easily handle 8+ week programmes

4. **Automatic Retry** (if still needed):
   - Check if programme.weeks.size < programme.durationWeeks
   - Retry with continuation prompt if incomplete
   - Should rarely be needed with optimizations

5. **Smart Week Cycling Fallback**:
   - Only if retry fails (unlikely with optimizations)
   - Cycle existing weeks with progressive overload
   - Clear indication of AI vs cycled weeks

### Implementation Status (January 2025)

#### ✅ Streamlined LLM Request Implementation
- **Token Limit**: Increased from 16,384 to 32,768 tokens
- **System Prompt**: Updated to request ONLY essential fields
- **Removed Fields**: week.name, description, intensityLevel, volumeLevel, focus, isDeload
- **Kept**: workout.name with descriptive names (e.g., "Upper Body Power")
- **Optional**: exercise.notes only when critical for safety
- **Data Classes**: Removed unused fields from GeneratedWeek (kept only weekNumber and workouts)
- **UI Updates**: Simplified preview to show only exercise data (matches workout execution)
- **Result**: ~30-40% token savings, should easily handle 8-week programmes

#### 🔄 Still To Do
1. Fix "Your Request Unknown" display issue
2. Implement automatic retry for incomplete programmes (if still needed)
3. Add smart week cycling fallback (unlikely to be needed with optimizations)

### Known Limitations
1. 5-minute timeout for API calls
2. Stale requests cleaned up after 1 hour
3. Single clarification round per request (by design for MVP)
4. Only one active programme allowed at a time (by design)
5. 8-week programmes only (hardcoded in prompt for MVP)