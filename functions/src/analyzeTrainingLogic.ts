import {HttpsError} from "firebase-functions/v2/https";
import {Firestore, Timestamp} from "firebase-admin/firestore";
import OpenAI from "openai";

export interface AnalyzeTrainingRequest {
  trainingData: string;
}

export interface AnalysisQuotas {
  monthlyCount: number;
  lastReset: Timestamp;
  lastAnalysis?: Timestamp;
}

export interface QuotaLimits {
  monthly: number;
}

/**
 * Validates the training data input
 * @param {AnalyzeTrainingRequest} data - The training data request
 */
export function validateInput(data: AnalyzeTrainingRequest): void {
  if (!data.trainingData || typeof data.trainingData !== "string") {
    throw new HttpsError("invalid-argument", "Training data is required");
  }
  if (data.trainingData.length < 50) {
    throw new HttpsError("invalid-argument", "Insufficient training data");
  }
  if (data.trainingData.length > 200000) {
    throw new HttpsError("invalid-argument", "Training data is too large");
  }
}

export const getQuotaLimits = (): QuotaLimits => ({
  monthly: 10,
});

const getInitialQuotas = (now: Timestamp): AnalysisQuotas => ({
  monthlyCount: getQuotaLimits().monthly,
  lastReset: now,
});

export const shouldResetMonthly = (lastReset: Timestamp): boolean => {
  const now = new Date();
  const lastResetDate = lastReset.toDate();
  return now.getUTCMonth() !== lastResetDate.getUTCMonth() ||
    now.getUTCFullYear() !== lastResetDate.getUTCFullYear();
};

/**
 * Decrements the user's analysis quota
 * @param {string} userId - The user ID
 * @param {Firestore} db - Firestore instance
 * @return {Promise<{remainingQuotas: QuotaLimits}>} Remaining quotas
 */
export async function decrementQuota(
  userId: string,
  db: Firestore
): Promise<{remainingQuotas: QuotaLimits}> {
  const quotaRef = db.collection("analysisQuotas").doc(userId);
  const limits = getQuotaLimits();

  return db.runTransaction(async (transaction) => {
    const doc = await transaction.get(quotaRef);
    const now = Timestamp.now();
    let quotas: AnalysisQuotas;

    if (!doc.exists) {
      quotas = getInitialQuotas(now);
    } else {
      const existingData = doc.data() as AnalysisQuotas;
      if (shouldResetMonthly(existingData.lastReset)) {
        quotas = {
          monthlyCount: limits.monthly,
          lastReset: now,
          lastAnalysis: existingData.lastAnalysis,
        };
      } else {
        quotas = existingData;
      }
    }

    if (quotas.monthlyCount <= 0) {
      throw new HttpsError(
        "resource-exhausted",
        `Analysis quota exceeded. Remaining: ${quotas.monthlyCount} monthly.`,
        {remaining: {monthly: quotas.monthlyCount}}
      );
    }

    const updatedQuotas: AnalysisQuotas = {
      monthlyCount: quotas.monthlyCount - 1,
      lastReset: quotas.lastReset,
      lastAnalysis: now,
    };

    transaction.set(quotaRef, updatedQuotas);

    return {
      remainingQuotas: {
        monthly: updatedQuotas.monthlyCount,
      },
    };
  });
}

/**
 * Refunds a user's analysis quota
 * @param {string} userId - The user ID
 * @param {Firestore} db - Firestore instance
 */
export async function refundQuota(
  userId: string,
  db: Firestore
): Promise<void> {
  const quotaRef = db.collection("analysisQuotas").doc(userId);
  await db.runTransaction(async (transaction) => {
    const doc = await transaction.get(quotaRef);
    if (!doc.exists) return;

    const quotas = doc.data() as AnalysisQuotas;
    const limits = getQuotaLimits();

    const monthlyCount = Math.min(limits.monthly, quotas.monthlyCount + 1);

    transaction.update(quotaRef, {
      monthlyCount: monthlyCount,
    });
  });
}

/**
 * Gets the appropriate system prompt based on workout count
 * @param {number} workoutCount - Number of workouts
 * @param {number} weeks - Number of weeks
 * @param {boolean} hasDeviationData - Whether deviation data is included
 * @return {string} The system prompt
 */
export function getSystemPrompt(
  workoutCount: number,
  weeks: number,
  hasDeviationData: boolean = false
): string {
  const adherenceInstructions = hasDeviationData ? `

PROGRAMME ADHERENCE ANALYSIS:
The training data includes programme deviation data. Analyze adherence:
- Score adherence 0-100 based on deviation patterns
- Identify positive patterns (consistency, hitting targets)
- Identify negative patterns (frequent skips, swaps, intensity issues)
- Provide specific adherence recommendations
- Note: Not all deviations are failures - smart auto-regulation is positive

Include in your JSON response an "adherence_analysis" object with:
- "adherence_score": number (0-100)
- "score_explanation": string (brief explanation)
- "positive_patterns": array of strings
- "negative_patterns": array of strings
- "adherence_recommendations": array of strings

If no deviation data is present, set "adherence_analysis" to null.
` : "";

  const adherenceJsonSchema = hasDeviationData ?
    ", \"adherence_analysis\" (object with score, patterns, recommendations)" :
    "";
  if (workoutCount <= 5) {
    return "You are an expert strength coach providing " +
      "INITIAL feedback on a lifter's training.\n\n" +
      `CRITICAL: This analysis covers only ${workoutCount} workout(s) ` +
      `over ${weeks} week(s). Do NOT attempt to identify trends or ` +
      "progression patterns. Focus exclusively on fundamentals.\n\n" +
      "Analyze the training data and provide:\n" +
      "1. Exercise Selection: Are exercises appropriate and balanced?\n" +
      "2. Weight Selection: Are weights reasonable for rep ranges?\n" +
      "3. Volume: Is total set count reasonable (12-20 sets per session)?\n" +
      "4. Safety: Any red flags (excessive volume, imbalanced " +
      "push/pull ratio > 2:1)?\n" +
      "5. Encouragement: Brief note to continue training consistently.\n\n" +
      "Keep assessment under 100 words. Be supportive but honest.\n\n" +
      adherenceInstructions +
      "Return JSON with keys: \"overall_assessment\" (string), " +
      "\"key_insights\" (array of objects with \"category\", \"message\", " +
      "\"severity\"), \"recommendations\" (array of strings), " +
      "\"warnings\" (array of strings)" + adherenceJsonSchema + ".\n\n" +
      "CRITICAL: For each insight:\n" +
      "- category must be ONE of: VOLUME, INTENSITY, FREQUENCY, " +
      "PROGRESSION, RECOVERY, CONSISTENCY, BALANCE, TECHNIQUE\n" +
      "- severity must be ONE of: SUCCESS, INFO, WARNING, CRITICAL";
  }

  if (workoutCount <= 11) {
    return "You are an expert strength coach analyzing " +
      "early-stage training data.\n\n" +
      `IMPORTANT: This analysis covers ${workoutCount} workouts over ` +
      `${weeks} weeks. Patterns are EMERGING but NOT confirmed. ` +
      "Use cautious language (\"appears to\", \"may\", \"early signs\").\n\n" +
      "Analyze and provide:\n" +
      "1. Consistency: Is training frequency adequate (2-4x/week)?\n" +
      "2. Early Patterns: Any tentative observations about weight " +
      "progression?\n" +
      "3. Volume Stability: Is volume per session roughly consistent?\n" +
      "4. Balance: Push/pull ratio, compound/isolation distribution\n" +
      "5. Recommendations: 2-3 actionable suggestions for next 2-4 weeks\n\n" +
      "Caveat any claims about trends. Keep assessment under 125 words.\n\n" +
      adherenceInstructions +
      "Return JSON with keys: \"overall_assessment\" (string), " +
      "\"key_insights\" (array of objects with \"category\", \"message\", " +
      "\"severity\"), \"recommendations\" (array of strings), " +
      "\"warnings\" (array of strings)" + adherenceJsonSchema + ".\n\n" +
      "IMPORTANT: For each insight:\n" +
      "- category must be ONE of: VOLUME, INTENSITY, FREQUENCY, " +
      "PROGRESSION, RECOVERY, CONSISTENCY, BALANCE, TECHNIQUE\n" +
      "- severity must be ONE of: SUCCESS, INFO, WARNING, CRITICAL";
  }

  return "You are an expert strength coach analyzing a " +
    "lifter's training history.\n\n" +
    `This analysis covers ${workoutCount} workouts over ${weeks} weeks ` +
    "- sufficient data for trend analysis.\n\n" +
    "Analyze and provide:\n" +
    "1. Volume Trend: Weekly volume increasing/decreasing/stagnant " +
    "for major lifts?\n" +
    "2. Intensity Trend: Average weight progression for key exercises?\n" +
    "3. Progression: Which exercises progressing? Which plateaued?\n" +
    "4. Program Balance: Push/pull, compound/isolation, muscle " +
    "distribution\n" +
    "5. Recovery: Signs of over/undertraining?\n" +
    "6. Recommendations: 2-3 specific, evidence-based actions\n\n" +
    "Be direct and actionable. Keep assessment under 150 words.\n\n" +
    adherenceInstructions +
    "Return JSON with keys: \"overall_assessment\" (string), " +
    "\"key_insights\" (array of objects with \"category\", \"message\", " +
    "\"severity\"), \"recommendations\" (array of strings), " +
    "\"warnings\" (array of strings)" + adherenceJsonSchema + ".\n\n" +
    "IMPORTANT: For each insight:\n" +
    "- category must be ONE of: VOLUME, INTENSITY, FREQUENCY, " +
    "PROGRESSION, RECOVERY, CONSISTENCY, BALANCE, TECHNIQUE\n" +
    "- severity must be ONE of: SUCCESS, INFO, WARNING, CRITICAL";
}

/**
 * Calls OpenAI API for training analysis
 * @param {string} trainingData - Training data JSON
 * @param {string} apiKey - OpenAI API key
 * @return {Promise<Record<string, unknown>>} AI response
 */
export async function callOpenAI(
  trainingData: string,
  apiKey: string
): Promise<Record<string, unknown>> {
  const openai = new OpenAI({apiKey});

  let workoutCount = 1;
  let weeks = 1;
  let hasDeviationData = false;
  try {
    const data = JSON.parse(trainingData);
    workoutCount = data.analysis_period?.total_workouts || 1;
    weeks = data.analysis_period?.total_weeks || 1;
    hasDeviationData = !!data.programme_deviation_summary;
  } catch (e) {
    console.error("Failed to parse training data JSON, using defaults:", e);
    workoutCount = 1;
    weeks = 1;
    hasDeviationData = false;
  }

  const systemPrompt = getSystemPrompt(workoutCount, weeks, hasDeviationData);

  console.log("Calling OpenAI with workout count:", workoutCount,
    "weeks:", weeks, "hasDeviationData:", hasDeviationData);
  console.log("Training data payload length:", trainingData.length);
  console.log("Training data payload:", trainingData);
  console.log("System prompt:", systemPrompt);

  const completion = await openai.chat.completions.create({
    model: "gpt-5-mini",
    messages: [
      {role: "system", content: systemPrompt},
      {role: "user", content: `Training data:\n\n${trainingData}`},
    ],
    response_format: {type: "json_object"},
  });

  const content = completion.choices[0]?.message?.content;
  if (!content) {
    throw new HttpsError("internal", "No content in OpenAI response");
  }

  console.log("OpenAI response:", content);

  return JSON.parse(content);
}
