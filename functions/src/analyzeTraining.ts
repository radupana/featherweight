import {onCall, CallableRequest, HttpsError} from "firebase-functions/v2/https";
import {getFirestore} from "firebase-admin/firestore";
import {Logging} from "@google-cloud/logging";
import {ALLOWED_CORS_ORIGINS} from "./index";
import {
  AnalyzeTrainingRequest,
  validateInput,
  decrementQuota,
  callOpenAI,
  refundQuota,
} from "./analyzeTrainingLogic";

const logging = new Logging();
const log = logging.log("analyzeTraining");

export const analyzeTraining = onCall<AnalyzeTrainingRequest>(
  {
    cors: ALLOWED_CORS_ORIGINS,
    maxInstances: 10,
    timeoutSeconds: 300,
    memory: "512MiB",
    region: "europe-west2",
    secrets: ["OPENAI_API_KEY"],
  },
  async (request: CallableRequest<AnalyzeTrainingRequest>) => {
    const isTestUser = request.auth?.token?.testUser === true;
    if (!request.app && !isTestUser) {
      await log.write(log.entry({
        severity: "WARNING",
        labels: {type: "app_check_missing"},
      }, {
        message: "App Check token missing",
      }));
      throw new HttpsError("unauthenticated", "App Check verification failed");
    }

    if (isTestUser && !request.app) {
      await log.write(log.entry({
        severity: "INFO",
        labels: {
          type: "app_check_bypassed_test_user",
          userId: request.auth?.uid || "unknown",
        },
      }, {
        message: "App Check bypassed for test user",
      }));
    }

    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Authentication required");
    }

    const signInProvider = request.auth.token.firebase?.sign_in_provider;
    if (signInProvider === "anonymous") {
      throw new HttpsError(
        "unauthenticated",
        "Sign in required to use AI training analysis"
      );
    }

    const userId = request.auth.uid;

    await log.write(log.entry({
      severity: "INFO",
      labels: {
        type: "analysis_request",
        userId,
      },
    }, {
      trainingDataLength: request.data.trainingData.length,
    }));

    let quotaDecremented = false;

    try {
      validateInput(request.data);

      const db = getFirestore("featherweight-v2");
      const {remainingQuotas} = await decrementQuota(userId, db);
      quotaDecremented = true;

      const apiKey = process.env.OPENAI_API_KEY;
      if (!apiKey) {
        throw new Error("OPENAI_API_KEY not configured");
      }

      const analysis = await callOpenAI(
        request.data.trainingData,
        apiKey
      );

      await log.write(log.entry({
        severity: "INFO",
        labels: {
          type: "analysis_success",
          userId,
        },
      }, {
        remaining: remainingQuotas,
      }));

      return {
        analysis,
        quota: {
          remaining: remainingQuotas,
        },
      };
    } catch (error) {
      const errorDetails: Record<string, unknown> = {
        message: error instanceof Error ? error.message : String(error),
        stack: error instanceof Error ? error.stack : undefined,
      };

      if (error && typeof error === "object") {
        Object.assign(errorDetails, {
          name: (error as {name?: string}).name,
          code: (error as {code?: string}).code,
          status: (error as {status?: number}).status,
          response: (error as {response?: unknown}).response,
        });
      }

      await log.write(log.entry({
        severity: "ERROR",
        labels: {
          type: "analysis_error",
          userId,
        },
      }, errorDetails));

      if (quotaDecremented && !(error instanceof HttpsError &&
          error.code === "resource-exhausted")) {
        try {
          const db = getFirestore("featherweight-v2");
          await refundQuota(userId, db);
          await log.write(log.entry({
            severity: "INFO",
            labels: {
              type: "quota_refunded",
              userId,
            },
          }));
        } catch (refundError) {
          await log.write(log.entry({
            severity: "ERROR",
            labels: {
              type: "quota_refund_failed",
              userId,
            },
          }, {
            error: refundError instanceof Error ?
              refundError.message : "Unknown error",
          }));
        }
      }

      if (error instanceof HttpsError) {
        throw error;
      }

      throw new HttpsError(
        "internal",
        "Failed to analyze training. Please try again."
      );
    }
  }
);
