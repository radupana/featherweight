/**
 * Business logic for audio transcription using OpenAI Whisper
 * Separated from Cloud Function for testability
 */

import * as admin from "firebase-admin";
import OpenAI from "openai";
import {toFile} from "openai/uploads";

/**
 * Request structure for audio transcription
 */
export interface TranscribeAudioRequest {
  audioBase64: string;
  mimeType: string;
}

/**
 * Quota limits for voice transcription
 */
export interface VoiceQuotaLimits {
  daily: number;
  weekly: number;
  monthly: number;
}

/**
 * Voice quota document structure
 */
export interface VoiceQuota {
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
 * Quota check result
 */
export interface QuotaCheckResult {
  exceeded: boolean;
  remaining: {
    daily: number;
    weekly: number;
    monthly: number;
  };
}

/**
 * Transcription result
 */
export interface TranscriptionResult {
  text: string;
  quota: {
    remaining: {
      daily: number;
      weekly: number;
      monthly: number;
    };
  };
}

/**
 * Valid audio MIME types
 */
export const VALID_MIME_TYPES = [
  "audio/m4a",
  "audio/mp4",
  "audio/mpeg",
  "audio/wav",
  "audio/webm",
];

/**
 * Maximum audio file size (25MB - Whisper limit)
 */
export const MAX_AUDIO_SIZE_BYTES = 25 * 1024 * 1024;

/**
 * Whisper prompt for fitness-specific transcription
 */
export const WHISPER_PROMPT =
  "Transcribe this fitness workout log. The speaker is logging " +
  "exercises, sets, reps, and weights. Common terms: bench press, squat, deadlift, " +
  "rows, curls, RPE, plates (45lbs/20kg each), kilos, pounds, tricep pushdowns, " +
  "bicep curls, overhead press.";

/**
 * Get quota limits for voice transcription
 */
export function getVoiceQuotaLimits(): VoiceQuotaLimits {
  return {
    daily: 50,
    weekly: 200,
    monthly: 500,
  };
}

/**
 * Check if a quota period needs to be reset
 */
export function shouldResetPeriod(
  lastReset: admin.firestore.Timestamp,
  period: "daily" | "weekly" | "monthly"
): boolean {
  const now = new Date();
  const lastResetDate = lastReset.toDate();

  switch (period) {
  case "daily": {
    return (
      now.getDate() !== lastResetDate.getDate() ||
      now.getMonth() !== lastResetDate.getMonth() ||
      now.getFullYear() !== lastResetDate.getFullYear()
    );
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
    return (
      now.getMonth() !== lastResetDate.getMonth() ||
      now.getFullYear() !== lastResetDate.getFullYear()
    );
  }
  }
}

/**
 * Validate the audio transcription request
 */
export function validateInput(request: TranscribeAudioRequest): Buffer {
  if (!request.audioBase64) {
    throw new Error("Audio data is required");
  }

  const audioBuffer = Buffer.from(request.audioBase64, "base64");

  if (audioBuffer.length > MAX_AUDIO_SIZE_BYTES) {
    throw new Error(
      `Audio file too large (max ${MAX_AUDIO_SIZE_BYTES / 1024 / 1024}MB)`
    );
  }

  const mimeType = request.mimeType || "audio/m4a";
  if (!VALID_MIME_TYPES.includes(mimeType)) {
    throw new Error(`Unsupported audio format: ${mimeType}`);
  }

  return audioBuffer;
}

/**
 * Call Whisper API for transcription
 */
export async function callWhisperAPI(
  apiKey: string,
  audioBuffer: Buffer,
  mimeType: string
): Promise<string> {
  const openai = new OpenAI({apiKey});

  const extension = mimeType.split("/")[1] || "m4a";
  const audioFile = await toFile(audioBuffer, `audio.${extension}`, {
    type: mimeType,
  });

  const transcription = await openai.audio.transcriptions.create({
    file: audioFile,
    model: "whisper-1",
    prompt: WHISPER_PROMPT,
    response_format: "json",
  });

  return transcription.text;
}
