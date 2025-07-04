# Task Completion Guidelines

## When Task is Complete
1. **Build and Test**
   - Run `./gradlew assembleDebug` to ensure compilation
   - Run `./gradlew lint` to check code quality
   - Test the feature manually if UI changes

2. **Code Quality Checks**
   - Run `./gradlew ktlintCheck` for style validation
   - Remove unused imports and dead code
   - Ensure proper error handling

3. **Database Considerations**
   - Verify database migrations work (destructive is OK)
   - Test with fresh database if schema changes
   - Check that data persists correctly

4. **Documentation**
   - Update relevant comments in code
   - Document any new public APIs
   - Update architectural decisions if needed

## Git Workflow
- Commit frequently with clear messages
- Use descriptive commit messages
- Include relevant files in commits
- Don't commit generated or temporary files

## Testing Strategy
- Manual testing for UI changes
- Unit tests for business logic
- Integration tests for database operations
- Test with both programme and freestyle workouts