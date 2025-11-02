/**
 * Cloud Function for parsing workout programmes using OpenAI
 * Provides secure server-side API access with quota management
 */

import {onCall, CallableRequest, HttpsError} from "firebase-functions/v2/https";
import {getFirestore} from "firebase-admin/firestore";
import {Logging} from "@google-cloud/logging";
import {
  ParseProgramRequest,
  validateInput,
  checkAndUpdateQuota,
  callOpenAI,
  getQuotaLimits,
} from "./parseProgramLogic";

const logging = new Logging();
const log = logging.log("parseProgram");

/**
 * Main Cloud Function for parsing workout programmes
 * Handles authentication, quota checking, and OpenAI API calls
 */
export const parseProgram = onCall<ParseProgramRequest>(
  {
    cors: true,
    maxInstances: 10,
    timeoutSeconds: 300,
    memory: "512MiB",
    region: "europe-west2",
    secrets: ["OPENAI_API_KEY"],
  },
  async (request: CallableRequest<ParseProgramRequest>) => {
    // 1. Verify App Check token (Play Integrity)
    if (!request.app) {
      await log.write(log.entry({
        severity: "WARNING",
        labels: {type: "app_check_missing"},
      }, {
        message: "App Check token missing",
      }));
      throw new HttpsError("unauthenticated", "App Check verification failed");
    }

    // 2. Verify Firebase Auth token
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Authentication required");
    }

    const userId = request.auth.uid;
    const signInProvider = request.auth.token.firebase?.sign_in_provider;
    const isAnonymous = signInProvider === "anonymous";

    // Log request info
    await log.write(log.entry({
      severity: "INFO",
      labels: {
        type: "parse_request",
        userId,
        isAnonymous: String(isAnonymous),
      },
    }, {
      textLength: request.data.rawText.length,
      hasMaxes: !!request.data.userMaxes,
    }));

    try {
      // 3. Validate input (throws HttpsError if invalid)
      validateInput(request.data);

      // 4. Check quota
      const db = getFirestore("featherweight-v2");
      const quotaResult = await checkAndUpdateQuota(userId, isAnonymous, db);

      if (quotaResult.exceeded) {
        const limits = getQuotaLimits(isAnonymous);
        const remaining = quotaResult.remaining;
        const message = isAnonymous ?
          `Daily quota exceeded (${limits.daily} parses per day). ` +
          `Sign in for 4x more! Remaining: ${remaining.daily} daily, ` +
          `${remaining.weekly} weekly, ${remaining.monthly} monthly.` :
          `Daily quota exceeded (${limits.daily} parses per day). ` +
          `Resets at midnight. Remaining: ${remaining.daily} daily, ` +
          `${remaining.weekly} weekly, ${remaining.monthly} monthly.`;

        await log.write(log.entry({
          severity: "INFO",
          labels: {
            type: "quota_exceeded",
            userId,
            isAnonymous: String(isAnonymous),
          },
        }, quotaResult.remaining));

        throw new HttpsError("resource-exhausted", message, {
          remaining: quotaResult.remaining,
          isAnonymous,
        });
      }

      // 5. Call OpenAI API
      const apiKey = process.env.OPENAI_API_KEY;
      if (!apiKey) {
        throw new Error("OPENAI_API_KEY not configured");
      }

      const result = await callOpenAI(
        request.data.rawText,
        request.data.userMaxes,
        apiKey
      );

      // 6. Log successful parse
      const weeks = result.weeks as unknown[];
      await log.write(log.entry({
        severity: "INFO",
        labels: {
          type: "parse_success",
          userId,
          isAnonymous: String(isAnonymous),
        },
      }, {
        remaining: quotaResult.remaining,
        programmeWeeks: weeks?.length || 0,
      }));

      // 7. Return result with metadata
      return {
        programme: result,
        quota: {
          remaining: quotaResult.remaining,
          isAnonymous,
        },
      };
    } catch (error) {
      // Log error
      await log.write(log.entry({
        severity: "ERROR",
        labels: {
          type: "parse_error",
          userId,
          isAnonymous: String(isAnonymous),
        },
      }, {
        error: error instanceof Error ? error.message : "Unknown error",
      }));

      // Re-throw as HttpsError for proper client handling
      if (error instanceof HttpsError) {
        throw error;
      }

      throw new HttpsError(
        "internal",
        "Failed to parse programme. Please try again."
      );
    }
  }
);
