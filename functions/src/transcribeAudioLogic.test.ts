/**
 * Unit tests for transcribeAudio business logic
 */

import {
  validateInput,
  getVoiceQuotaLimits,
  shouldResetPeriod,
  callWhisperAPI,
  TranscribeAudioRequest,
  VALID_MIME_TYPES,
  MAX_AUDIO_SIZE_BYTES,
  WHISPER_PROMPT,
} from "./transcribeAudioLogic";
import * as admin from "firebase-admin";

// Mock Firebase Admin
jest.mock("firebase-admin", () => {
  const mockTimestamp = (date: Date) => ({
    toDate: jest.fn(() => date),
  });

  return {
    firestore: {
      Timestamp: {
        now: jest.fn(() => mockTimestamp(new Date())),
        fromDate: jest.fn((date: Date) => mockTimestamp(date)),
      },
    },
  };
});

// Mock OpenAI
jest.mock("openai", () => {
  return jest.fn().mockImplementation(() => ({
    audio: {
      transcriptions: {
        create: jest.fn(),
      },
    },
  }));
});

// Mock toFile
jest.mock("openai/uploads", () => ({
  toFile: jest.fn().mockResolvedValue({name: "audio.m4a"}),
}));

describe("transcribeAudioLogic", () => {
  describe("validateInput", () => {
    it("should accept valid audio data", () => {
      const request: TranscribeAudioRequest = {
        audioBase64: Buffer.from("fake audio data").toString("base64"),
        mimeType: "audio/m4a",
      };

      const result = validateInput(request);
      expect(result).toBeInstanceOf(Buffer);
    });

    it("should accept all valid MIME types", () => {
      VALID_MIME_TYPES.forEach((mimeType) => {
        const request: TranscribeAudioRequest = {
          audioBase64: Buffer.from("test").toString("base64"),
          mimeType,
        };

        expect(() => validateInput(request)).not.toThrow();
      });
    });

    it("should reject empty audio data", () => {
      const request: TranscribeAudioRequest = {
        audioBase64: "",
        mimeType: "audio/m4a",
      };

      expect(() => validateInput(request)).toThrow("Audio data is required");
    });

    it("should reject audio over size limit", () => {
      const largeBuffer = Buffer.alloc(MAX_AUDIO_SIZE_BYTES + 1);
      const request: TranscribeAudioRequest = {
        audioBase64: largeBuffer.toString("base64"),
        mimeType: "audio/m4a",
      };

      expect(() => validateInput(request)).toThrow("Audio file too large");
    });

    it("should reject unsupported MIME type", () => {
      const request: TranscribeAudioRequest = {
        audioBase64: Buffer.from("test").toString("base64"),
        mimeType: "audio/ogg",
      };

      expect(() => validateInput(request)).toThrow("Unsupported audio format");
    });

    it("should default to audio/m4a when mimeType not specified", () => {
      const request = {
        audioBase64: Buffer.from("test").toString("base64"),
      } as TranscribeAudioRequest;

      expect(() => validateInput(request)).not.toThrow();
    });
  });

  describe("getVoiceQuotaLimits", () => {
    it("should return correct daily limit", () => {
      const limits = getVoiceQuotaLimits();
      expect(limits.daily).toBe(50);
    });

    it("should return correct weekly limit", () => {
      const limits = getVoiceQuotaLimits();
      expect(limits.weekly).toBe(200);
    });

    it("should return correct monthly limit", () => {
      const limits = getVoiceQuotaLimits();
      expect(limits.monthly).toBe(500);
    });
  });

  describe("shouldResetPeriod", () => {
    describe("daily", () => {
      it("should reset when day changes", () => {
        const yesterday = new Date();
        yesterday.setDate(yesterday.getDate() - 1);

        const timestamp = admin.firestore.Timestamp.fromDate(
          yesterday
        ) as admin.firestore.Timestamp;
        expect(shouldResetPeriod(timestamp, "daily")).toBe(true);
      });

      it("should not reset on same day", () => {
        const today = new Date();
        const timestamp = admin.firestore.Timestamp.fromDate(
          today
        ) as admin.firestore.Timestamp;
        expect(shouldResetPeriod(timestamp, "daily")).toBe(false);
      });
    });

    describe("weekly", () => {
      it("should reset when week changes", () => {
        const lastWeek = new Date();
        lastWeek.setDate(lastWeek.getDate() - 7);

        const timestamp = admin.firestore.Timestamp.fromDate(
          lastWeek
        ) as admin.firestore.Timestamp;
        expect(shouldResetPeriod(timestamp, "weekly")).toBe(true);
      });

      it("should not reset in same week", () => {
        const today = new Date();
        const timestamp = admin.firestore.Timestamp.fromDate(
          today
        ) as admin.firestore.Timestamp;
        expect(shouldResetPeriod(timestamp, "weekly")).toBe(false);
      });
    });

    describe("monthly", () => {
      it("should reset when month changes", () => {
        const lastMonth = new Date();
        lastMonth.setMonth(lastMonth.getMonth() - 1);

        const timestamp = admin.firestore.Timestamp.fromDate(
          lastMonth
        ) as admin.firestore.Timestamp;
        expect(shouldResetPeriod(timestamp, "monthly")).toBe(true);
      });

      it("should not reset in same month", () => {
        const today = new Date();
        const timestamp = admin.firestore.Timestamp.fromDate(
          today
        ) as admin.firestore.Timestamp;
        expect(shouldResetPeriod(timestamp, "monthly")).toBe(false);
      });
    });
  });

  describe("WHISPER_PROMPT", () => {
    it("should contain fitness terminology", () => {
      expect(WHISPER_PROMPT).toContain("fitness workout log");
      expect(WHISPER_PROMPT).toContain("exercises");
      expect(WHISPER_PROMPT).toContain("sets");
      expect(WHISPER_PROMPT).toContain("reps");
      expect(WHISPER_PROMPT).toContain("weights");
    });

    it("should contain common exercise names", () => {
      expect(WHISPER_PROMPT).toContain("bench press");
      expect(WHISPER_PROMPT).toContain("squat");
      expect(WHISPER_PROMPT).toContain("deadlift");
      expect(WHISPER_PROMPT).toContain("curls");
    });

    it("should contain weight unit terminology", () => {
      expect(WHISPER_PROMPT).toContain("plates");
      expect(WHISPER_PROMPT).toContain("kilos");
      expect(WHISPER_PROMPT).toContain("pounds");
    });

    it("should contain RPE", () => {
      expect(WHISPER_PROMPT).toContain("RPE");
    });
  });

  describe("callWhisperAPI", () => {
    it("should call OpenAI with correct parameters", async () => {
      const OpenAI = require("openai");
      const mockCreate = jest.fn().mockResolvedValue({
        text: "bench press 3 sets of 8 at 100 kilos",
      });

      OpenAI.mockImplementation(() => ({
        audio: {
          transcriptions: {
            create: mockCreate,
          },
        },
      }));

      const audioBuffer = Buffer.from("fake audio");
      const result = await callWhisperAPI("test-api-key", audioBuffer, "audio/m4a");

      expect(mockCreate).toHaveBeenCalledWith({
        file: expect.anything(),
        model: "whisper-1",
        prompt: WHISPER_PROMPT,
        response_format: "json",
      });

      expect(result).toBe("bench press 3 sets of 8 at 100 kilos");
    });

    it("should handle mp4 MIME type", async () => {
      const OpenAI = require("openai");
      const mockCreate = jest.fn().mockResolvedValue({
        text: "squats 5 by 5 at 100",
      });

      OpenAI.mockImplementation(() => ({
        audio: {
          transcriptions: {
            create: mockCreate,
          },
        },
      }));

      const result = await callWhisperAPI(
        "test-key",
        Buffer.from("audio"),
        "audio/mp4"
      );

      expect(result).toBe("squats 5 by 5 at 100");
    });

    it("should handle wav MIME type", async () => {
      const OpenAI = require("openai");
      OpenAI.mockImplementation(() => ({
        audio: {
          transcriptions: {
            create: jest.fn().mockResolvedValue({text: "deadlift 3x5"}),
          },
        },
      }));

      const result = await callWhisperAPI(
        "test-key",
        Buffer.from("audio"),
        "audio/wav"
      );

      expect(result).toBe("deadlift 3x5");
    });
  });

  describe("VALID_MIME_TYPES", () => {
    it("should include m4a", () => {
      expect(VALID_MIME_TYPES).toContain("audio/m4a");
    });

    it("should include mp4", () => {
      expect(VALID_MIME_TYPES).toContain("audio/mp4");
    });

    it("should include mpeg", () => {
      expect(VALID_MIME_TYPES).toContain("audio/mpeg");
    });

    it("should include wav", () => {
      expect(VALID_MIME_TYPES).toContain("audio/wav");
    });

    it("should include webm", () => {
      expect(VALID_MIME_TYPES).toContain("audio/webm");
    });
  });

  describe("MAX_AUDIO_SIZE_BYTES", () => {
    it("should be 25MB", () => {
      expect(MAX_AUDIO_SIZE_BYTES).toBe(25 * 1024 * 1024);
    });
  });
});
