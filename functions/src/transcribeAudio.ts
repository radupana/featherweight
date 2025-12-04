/**
 * Cloud Function for transcribing audio using OpenAI Whisper
 * Provides secure server-side API access with quota management
 */

import {onCall, CallableRequest, HttpsError} from "firebase-functions/v2/https";
import {getFirestore} from "firebase-admin/firestore";
import {Logging} from "@google-cloud/logging";
import * as admin from "firebase-admin";
import OpenAI from "openai";
import {toFile} from "openai/uploads";
import {ALLOWED_CORS_ORIGINS} from "./index";

const logging = new Logging();
const log = logging.log("transcribeAudio");

/**
 * Request structure for audio transcription
 */
export interface TranscribeAudioRequest {
  audioBase64: string;
  mimeType: string;
}

/**
 * Quota limits for voice transcription (separate from programme parsing)
 */
interface VoiceQuotaLimits {
  daily: number;
  weekly: number;
  monthly: number;
}

/**
 * Voice quota document in Firestore
 */
interface VoiceQuota {
  userId: string;
  dailyCount: number;
  weeklyCount: number;
  monthlyCount: number;
  lastDailyReset: admin.firestore.Timestamp;
  lastWeeklyReset: admin.firestore.Timestamp;
  lastMonthlyReset: admin.firestore.Timestamp;
  totalRequests: number;
  firstRequestAt?: admin.firestore.Timestamp;
  lastRequestAt: admin.firestore.Timestamp;
  quotaExceededCount: number;
}

/**
 * Get quota limits for voice transcription
 */
function getVoiceQuotaLimits(): VoiceQuotaLimits {
  return {
    daily: 50,
    weekly: 200,
    monthly: 500,
  };
}

/**
 * Check if a quota period needs to be reset
 */
function shouldResetPeriod(
  lastReset: admin.firestore.Timestamp,
  period: "daily" | "weekly" | "monthly"
): boolean {
  const now = new Date();
  const lastResetDate = lastReset.toDate();

  switch (period) {
  case "daily": {
    return now.getDate() !== lastResetDate.getDate() ||
           now.getMonth() !== lastResetDate.getMonth() ||
           now.getFullYear() !== lastResetDate.getFullYear();
  }
  case "weekly": {
    const weekStart = new Date(now);
    weekStart.setDate(now.getDate() - now.getDay());
    weekStart.setHours(0, 0, 0, 0);

    const lastWeekStart = new Date(lastResetDate);
    lastWeekStart.setDate(lastResetDate.getDate() - lastResetDate.getDay());
    lastWeekStart.setHours(0, 0, 0, 0);

    return weekStart.getTime() !== lastWeekStart.getTime();
  }
  case "monthly": {
    return now.getMonth() !== lastResetDate.getMonth() ||
           now.getFullYear() !== lastResetDate.getFullYear();
  }
  }
}

/**
 * Check and update voice transcription quota
 */
async function checkAndUpdateVoiceQuota(
  userId: string,
  db: admin.firestore.Firestore
): Promise<{exceeded: boolean; remaining: {daily: number; weekly: number; monthly: number}}> {
  const quotaRef = db.collection("voiceQuotas").doc(userId);

  const result = await db.runTransaction(async (transaction) => {
    const doc = await transaction.get(quotaRef);
    const now = admin.firestore.Timestamp.now();

    let quota: VoiceQuota;

    if (!doc.exists) {
      quota = {
        userId,
        dailyCount: 0,
        weeklyCount: 0,
        monthlyCount: 0,
        lastDailyReset: now,
        lastWeeklyReset: now,
        lastMonthlyReset: now,
        totalRequests: 0,
        firstRequestAt: now,
        lastRequestAt: now,
        quotaExceededCount: 0,
      };
    } else {
      quota = doc.data() as VoiceQuota;

      if (shouldResetPeriod(quota.lastDailyReset, "daily")) {
        quota.dailyCount = 0;
        quota.lastDailyReset = now;
      }

      if (shouldResetPeriod(quota.lastWeeklyReset, "weekly")) {
        quota.weeklyCount = 0;
        quota.lastWeeklyReset = now;
      }

      if (shouldResetPeriod(quota.lastMonthlyReset, "monthly")) {
        quota.monthlyCount = 0;
        quota.lastMonthlyReset = now;
      }
    }

    const limits = getVoiceQuotaLimits();

    const exceeded = quota.dailyCount >= limits.daily ||
                    quota.weeklyCount >= limits.weekly ||
                    quota.monthlyCount >= limits.monthly;

    if (exceeded) {
      quota.quotaExceededCount++;
      quota.lastRequestAt = now;
      transaction.set(quotaRef, quota);

      return {
        exceeded: true,
        remaining: {
          daily: Math.max(0, limits.daily - quota.dailyCount),
          weekly: Math.max(0, limits.weekly - quota.weeklyCount),
          monthly: Math.max(0, limits.monthly - quota.monthlyCount),
        },
      };
    }

    quota.dailyCount++;
    quota.weeklyCount++;
    quota.monthlyCount++;
    quota.totalRequests++;
    quota.lastRequestAt = now;

    transaction.set(quotaRef, quota);

    return {
      exceeded: false,
      remaining: {
        daily: limits.daily - quota.dailyCount,
        weekly: limits.weekly - quota.weeklyCount,
        monthly: limits.monthly - quota.monthlyCount,
      },
    };
  });

  return result;
}

/**
 * Whisper prompt for fitness-specific transcription
 */
const WHISPER_PROMPT = "Transcribe this fitness workout log. The speaker is logging " +
  "exercises, sets, reps, and weights. Common terms: bench press, squat, deadlift, " +
  "rows, curls, RPE, plates (45lbs/20kg each), kilos, pounds, tricep pushdowns, " +
  "bicep curls, overhead press.";

/**
 * Main Cloud Function for audio transcription
 */
export const transcribeAudio = onCall<TranscribeAudioRequest>(
  {
    cors: ALLOWED_CORS_ORIGINS,
    maxInstances: 10,
    timeoutSeconds: 120,
    memory: "512MiB",
    region: "europe-west2",
    secrets: ["OPENAI_API_KEY"],
  },
  async (request: CallableRequest<TranscribeAudioRequest>) => {
    // 1. Verify App Check token - skip for test users
    const isTestUser = request.auth?.token?.testUser === true;
    if (!request.app && !isTestUser) {
      await log.write(log.entry({
        severity: "WARNING",
        labels: {type: "app_check_missing"},
      }, {
        message: "App Check token missing for transcribeAudio",
      }));
      throw new HttpsError("unauthenticated", "App Check verification failed");
    }

    // 2. Verify Firebase Auth token
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Authentication required");
    }

    const signInProvider = request.auth.token.firebase?.sign_in_provider;
    if (signInProvider === "anonymous") {
      throw new HttpsError(
        "unauthenticated",
        "Sign in required to use voice input"
      );
    }

    const userId = request.auth.uid;

    // 3. Validate input
    if (!request.data.audioBase64) {
      throw new HttpsError("invalid-argument", "Audio data is required");
    }

    const audioBuffer = Buffer.from(request.data.audioBase64, "base64");
    const maxSizeBytes = 25 * 1024 * 1024; // 25MB (Whisper limit)
    if (audioBuffer.length > maxSizeBytes) {
      throw new HttpsError(
        "invalid-argument",
        `Audio file too large (max ${maxSizeBytes / 1024 / 1024}MB)`
      );
    }

    const mimeType = request.data.mimeType || "audio/m4a";
    const validMimeTypes = ["audio/m4a", "audio/mp4", "audio/mpeg", "audio/wav", "audio/webm"];
    if (!validMimeTypes.includes(mimeType)) {
      throw new HttpsError(
        "invalid-argument",
        `Unsupported audio format: ${mimeType}`
      );
    }

    await log.write(log.entry({
      severity: "INFO",
      labels: {
        type: "transcribe_request",
        userId,
      },
    }, {
      audioSizeBytes: audioBuffer.length,
      mimeType,
    }));

    try {
      // 4. Check quota
      const db = getFirestore("featherweight-v2");
      const quotaResult = await checkAndUpdateVoiceQuota(userId, db);

      if (quotaResult.exceeded) {
        const limits = getVoiceQuotaLimits();
        throw new HttpsError(
          "resource-exhausted",
          `Voice input quota exceeded (${limits.daily} per day). ` +
          `Remaining: ${quotaResult.remaining.daily} daily.`,
          {remaining: quotaResult.remaining}
        );
      }

      // 5. Call Whisper API
      const apiKey = process.env.OPENAI_API_KEY;
      if (!apiKey) {
        throw new Error("OPENAI_API_KEY not configured");
      }

      const openai = new OpenAI({apiKey});

      const extension = mimeType.split("/")[1] || "m4a";
      const audioFile = await toFile(audioBuffer, `audio.${extension}`, {
        type: mimeType,
      });

      // Retry logic for transient OpenAI errors (403, rate limits, etc.)
      const maxRetries = 3;
      let lastError: Error | null = null;

      for (let attempt = 1; attempt <= maxRetries; attempt++) {
        try {
          const transcription = await openai.audio.transcriptions.create({
            file: audioFile,
            model: "whisper-1",
            prompt: WHISPER_PROMPT,
            response_format: "json",
          });

          // 6. Log success
          await log.write(log.entry({
            severity: "INFO",
            labels: {
              type: "transcribe_success",
              userId,
            },
          }, {
            textLength: transcription.text.length,
            remaining: quotaResult.remaining,
            attempt,
          }));

          // 7. Return result
          return {
            text: transcription.text,
            quota: {
              remaining: quotaResult.remaining,
            },
          };
        } catch (retryError) {
          lastError = retryError instanceof Error ? retryError : new Error(String(retryError));
          const errorMessage = lastError.message;

          // Log retry attempt
          await log.write(log.entry({
            severity: "WARNING",
            labels: {
              type: "transcribe_retry",
              userId,
            },
          }, {
            attempt,
            maxRetries,
            error: errorMessage,
          }));

          // Only retry on transient errors (403, 429, 5xx)
          const isRetryable = errorMessage.includes("403") ||
                             errorMessage.includes("429") ||
                             errorMessage.includes("500") ||
                             errorMessage.includes("502") ||
                             errorMessage.includes("503");

          if (!isRetryable || attempt === maxRetries) {
            throw lastError;
          }

          // Exponential backoff: 1s, 2s, 4s
          const delayMs = Math.pow(2, attempt - 1) * 1000;
          await new Promise((resolve) => setTimeout(resolve, delayMs));
        }
      }

      // Should never reach here, but TypeScript needs this
      throw lastError || new Error("Transcription failed after retries");
    } catch (error) {
      await log.write(log.entry({
        severity: "ERROR",
        labels: {
          type: "transcribe_error",
          userId,
        },
      }, {
        error: error instanceof Error ? error.message : "Unknown error",
      }));

      if (error instanceof HttpsError) {
        throw error;
      }

      throw new HttpsError(
        "internal",
        "Failed to transcribe audio. Please try again."
      );
    }
  }
);
