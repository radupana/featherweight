/**
 * Cloud Functions for Featherweight
 * Centralized logging infrastructure and secure API proxy
 */

import {setGlobalOptions} from "firebase-functions/v2";
import {onRequest} from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import {Logging} from "@google-cloud/logging";

admin.initializeApp();

const logging = new Logging();
const logName = "featherweight-app-logs";
const log = logging.log(logName);

setGlobalOptions({
  maxInstances: 10,
  region: "europe-west2",
});

/**
 * Log event structure from Android app
 */
interface LogEvent {
  level: "DEBUG" | "INFO" | "WARN" | "ERROR";
  tag: string;
  message: string;
  timestamp: number; // Unix timestamp in milliseconds
  context?: {
    userId?: string;
    installationId?: string;
    appVersion?: string;
    deviceModel?: string;
    androidVersion?: string;
    screen?: string;
  };
  throwable?: {
    message: string;
    stackTrace: string;
  };
}

/**
 * Batch of log events from Android app
 */
interface LogBatch {
  events: LogEvent[];
}

/**
 * HTTPS Cloud Function to receive and process log events
 * Verifies App Check token, Firebase Auth token, and writes to Cloud Logging
 */
export const logEvent = onRequest(
  {
    cors: true,
    maxInstances: 5,
    timeoutSeconds: 10,
    memory: "256MiB",
  },
  async (req, res) => {
    if (req.method !== "POST") {
      res.status(405).send("Method Not Allowed");
      return;
    }

    try {
      const appCheckToken = req.header("X-Firebase-AppCheck");
      if (!appCheckToken) {
        res.status(401).send("Unauthorized: Missing App Check token");
        return;
      }

      try {
        await admin.appCheck().verifyToken(appCheckToken);
      } catch (error) {
        await log.write(log.entry({severity: "ERROR"}, {
          message: "App Check verification failed",
          error: error instanceof Error ? error.message : String(error),
        }));
        res.status(401).send("Unauthorized: Invalid App Check token");
        return;
      }

      const authHeader = req.headers.authorization;
      if (!authHeader || !authHeader.startsWith("Bearer ")) {
        res.status(401).send("Unauthorized: Missing or invalid auth token");
        return;
      }

      const idToken = authHeader.split("Bearer ")[1];
      let decodedToken;
      try {
        decodedToken = await admin.auth().verifyIdToken(idToken);
      } catch (error) {
        await log.write(log.entry({severity: "ERROR"}, {
          message: "Token verification failed",
          error: error instanceof Error ? error.message : String(error),
        }));
        res.status(401).send("Unauthorized: Invalid auth token");
        return;
      }

      const userId = decodedToken.uid;

      const batch: LogBatch = req.body;
      if (!batch || !batch.events || !Array.isArray(batch.events)) {
        res.status(400).send("Bad Request: Invalid log batch format");
        return;
      }

      if (batch.events.length > 100) {
        res.status(400).send("Bad Request: Batch too large (max 100 events)");
        return;
      }

      const entries = batch.events.map((event) => {
        const severity = mapLogLevel(event.level);

        const metadata = {
          severity: severity,
          timestamp: new Date(event.timestamp),
          labels: {
            tag: event.tag,
            userId: event.context?.userId || userId,
            installationId: event.context?.installationId || "unknown",
            appVersion: event.context?.appVersion || "unknown",
          },
          resource: {
            type: "global",
            labels: {
              project_id: process.env.GCLOUD_PROJECT || "featherweight-app",
            },
          },
        };

        const data: Record<string, unknown> = {
          message: event.message,
          tag: event.tag,
          level: event.level,
          userId: event.context?.userId || userId,
          installationId: event.context?.installationId,
          appVersion: event.context?.appVersion,
          deviceModel: event.context?.deviceModel,
          androidVersion: event.context?.androidVersion,
          screen: event.context?.screen,
        };

        if (event.throwable) {
          data.error = {
            message: event.throwable.message,
            stackTrace: event.throwable.stackTrace,
          };
        }

        return log.entry(metadata, data);
      });

      await log.write(entries);

      res.status(200).json({
        success: true,
        eventsProcessed: batch.events.length,
      });
    } catch (error) {
      await log.write(log.entry({severity: "ERROR"}, {
        message: "Error processing log batch",
        error: error instanceof Error ? error.message : String(error),
      }));
      res.status(500).send("Internal Server Error");
    }
  }
);

/**
 * Map Android log level to Cloud Logging severity
 * @param {string} level - Android log level (DEBUG/INFO/WARN/ERROR)
 * @return {string} Cloud Logging severity
 */
function mapLogLevel(
  level: string,
): "DEBUG" | "INFO" | "WARNING" | "ERROR" | "DEFAULT" {
  switch (level) {
  case "DEBUG":
    return "DEBUG";
  case "INFO":
    return "INFO";
  case "WARN":
    return "WARNING";
  case "ERROR":
    return "ERROR";
  default:
    return "DEFAULT";
  }
}

// Export the parseProgram function
export {parseProgram} from "./parseProgram";

// Export the analyzeTraining function
export {analyzeTraining} from "./analyzeTraining";

// Export voice input functions
export {transcribeAudio} from "./transcribeAudio";
export {parseVoiceWorkout} from "./parseVoiceWorkout";
