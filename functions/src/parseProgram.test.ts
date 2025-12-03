/**
 * Unit tests for parseProgram callable function wrapper
 * Tests authentication, App Check, quota management, and error handling
 */

import {CallableRequest, HttpsError} from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import {ParseProgramRequest} from "./parseProgramLogic";

// Mock firebase-admin
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

// Mock firebase-functions
jest.mock("firebase-functions/v2/https", () => ({
  onCall: jest.fn((config, handler) => handler),
  HttpsError: class HttpsError extends Error {
    constructor(
      public code: string,
      message: string,
      public details?: unknown
    ) {
      super(message);
      this.name = "HttpsError";
    }
  },
}));

// Mock @google-cloud/logging
jest.mock("@google-cloud/logging", () => {
  const mockEntry = jest.fn((metadata: unknown, data: unknown) => ({
    metadata,
    data,
  }));
  const mockWrite = jest.fn().mockResolvedValue(undefined);
  const mockLog = jest.fn(() => ({
    entry: mockEntry,
    write: mockWrite,
  }));

  return {
    Logging: jest.fn(() => ({
      log: mockLog,
    })),
  };
});

// Mock parseProgramLogic
jest.mock("./parseProgramLogic", () => ({
  validateInput: jest.fn(),
  checkAndUpdateQuota: jest.fn(),
  callOpenAI: jest.fn(),
  getQuotaLimits: jest.fn(() => ({daily: 10, weekly: 35, monthly: 50})),
}));

// Mock getFirestore
const mockRunTransaction = jest.fn();
const mockGetFirestore = jest.fn(() => ({
  runTransaction: mockRunTransaction,
  collection: jest.fn(() => ({
    doc: jest.fn(() => ({
      get: jest.fn(),
      set: jest.fn(),
    })),
  })),
}));

jest.mock("firebase-admin/firestore", () => ({
  getFirestore: mockGetFirestore,
}));

describe("parseProgram callable function", () => {
  let parseProgram: (request: CallableRequest<ParseProgramRequest>) => Promise<unknown>;
  let validateInput: jest.Mock;
  let checkAndUpdateQuota: jest.Mock;
  let callOpenAI: jest.Mock;

  beforeEach(() => {
    jest.clearAllMocks();

    // Set up environment variable
    process.env.OPENAI_API_KEY = "test-api-key";

    // Import the function
    const module = require("./parseProgram");
    parseProgram = module.parseProgram;

    // Get mocked functions
    const logic = require("./parseProgramLogic");
    validateInput = logic.validateInput;
    checkAndUpdateQuota = logic.checkAndUpdateQuota;
    callOpenAI = logic.callOpenAI;

    // Set default mock implementations
    validateInput.mockImplementation(() => {});
    checkAndUpdateQuota.mockResolvedValue({
      exceeded: false,
      remaining: {daily: 9, weekly: 34, monthly: 49},
    });
    callOpenAI.mockResolvedValue({
      name: "Test Programme",
      weeks: [{weekNumber: 1, workouts: []}],
    });
  });

  afterEach(() => {
    delete process.env.OPENAI_API_KEY;
  });

  describe("App Check validation", () => {
    it("should reject requests without App Check token", async () => {
      const request: CallableRequest<ParseProgramRequest> = {
        data: {rawText: "Squat 3x5"},
        auth: {
          uid: "user123",
          token: {},
        },
        app: undefined,
        rawRequest: {} as any,
      };

      await expect(parseProgram(request)).rejects.toThrow(HttpsError);
      await expect(parseProgram(request)).rejects.toMatchObject({
        code: "unauthenticated",
        message: "App Check verification failed",
      });
    });

    it("should bypass App Check for test users", async () => {
      const request: CallableRequest<ParseProgramRequest> = {
        data: {rawText: "Squat 3x5 @ 100kg"},
        auth: {
          uid: "testuser123",
          token: {
            testUser: true,
            firebase: {sign_in_provider: "password"},
          },
        },
        app: undefined,
        rawRequest: {} as any,
      };

      const result = await parseProgram(request);

      expect(result).toEqual({
        programme: expect.any(Object),
        quota: {
          remaining: expect.any(Object),
        },
      });
      expect(callOpenAI).toHaveBeenCalled();
    });

    it("should allow requests with valid App Check token", async () => {
      const request: CallableRequest<ParseProgramRequest> = {
        data: {rawText: "Squat 3x5"},
        auth: {
          uid: "user123",
          token: {
            firebase: {sign_in_provider: "password"},
          },
        },
        app: {
          appId: "test-app-id",
          token: {
            app_id: "test-app-id",
          },
        },
        rawRequest: {} as any,
      };

      const result = await parseProgram(request);

      expect(result).toEqual({
        programme: expect.any(Object),
        quota: {
          remaining: expect.any(Object),
        },
      });
    });
  });

  describe("Authentication validation", () => {
    it("should reject requests without authentication", async () => {
      const request: CallableRequest<ParseProgramRequest> = {
        data: {rawText: "Squat 3x5"},
        auth: undefined,
        app: {appId: "test-app-id", token: {app_id: "test-app-id"}},
        rawRequest: {} as any,
      };

      await expect(parseProgram(request)).rejects.toThrow(HttpsError);
      await expect(parseProgram(request)).rejects.toMatchObject({
        code: "unauthenticated",
        message: "Authentication required",
      });
    });

    it("should reject anonymous users", async () => {
      const request: CallableRequest<ParseProgramRequest> = {
        data: {rawText: "Squat 3x5"},
        auth: {
          uid: "anon123",
          token: {
            firebase: {sign_in_provider: "anonymous"},
          },
        },
        app: {appId: "test-app-id", token: {app_id: "test-app-id"}},
        rawRequest: {} as any,
      };

      await expect(parseProgram(request)).rejects.toThrow(HttpsError);
      await expect(parseProgram(request)).rejects.toMatchObject({
        code: "unauthenticated",
        message: "Sign in required to use AI programme parsing",
      });
    });

    it("should allow authenticated users with password provider", async () => {
      const request: CallableRequest<ParseProgramRequest> = {
        data: {rawText: "Squat 3x5 @ 100kg"},
        auth: {
          uid: "user123",
          token: {
            firebase: {sign_in_provider: "password"},
          },
        },
        app: {appId: "test-app-id", token: {app_id: "test-app-id"}},
        rawRequest: {} as any,
      };

      const result = await parseProgram(request);

      expect(result).toEqual({
        programme: expect.any(Object),
        quota: {
          remaining: expect.any(Object),
        },
      });
    });

    it("should allow authenticated users with google provider", async () => {
      const request: CallableRequest<ParseProgramRequest> = {
        data: {rawText: "Squat 3x5 @ 100kg"},
        auth: {
          uid: "user456",
          token: {
            firebase: {sign_in_provider: "google.com"},
          },
        },
        app: {appId: "test-app-id", token: {app_id: "test-app-id"}},
        rawRequest: {} as any,
      };

      const result = await parseProgram(request);

      expect(result).toEqual({
        programme: expect.any(Object),
        quota: {
          remaining: expect.any(Object),
        },
      });
    });
  });

  describe("Quota management", () => {
    it("should check and update quota before parsing", async () => {
      const request: CallableRequest<ParseProgramRequest> = {
        data: {rawText: "Squat 3x5 @ 100kg"},
        auth: {
          uid: "user123",
          token: {
            firebase: {sign_in_provider: "password"},
          },
        },
        app: {appId: "test-app-id", token: {app_id: "test-app-id"}},
        rawRequest: {} as any,
      };

      await parseProgram(request);

      expect(checkAndUpdateQuota).toHaveBeenCalledWith(
        "user123",
        expect.anything()
      );
      // Both functions should have been called
      expect(checkAndUpdateQuota).toHaveBeenCalled();
      expect(callOpenAI).toHaveBeenCalled();
    });

    it("should reject when quota exceeded", async () => {
      checkAndUpdateQuota.mockResolvedValue({
        exceeded: true,
        remaining: {daily: 0, weekly: 20, monthly: 40},
      });

      const request: CallableRequest<ParseProgramRequest> = {
        data: {rawText: "Squat 3x5 @ 100kg"},
        auth: {
          uid: "user123",
          token: {
            firebase: {sign_in_provider: "password"},
          },
        },
        app: {appId: "test-app-id", token: {app_id: "test-app-id"}},
        rawRequest: {} as any,
      };

      await expect(parseProgram(request)).rejects.toThrow(HttpsError);
      await expect(parseProgram(request)).rejects.toMatchObject({
        code: "resource-exhausted",
        message: expect.stringContaining("Daily quota exceeded"),
      });

      // Should not call OpenAI if quota exceeded
      expect(callOpenAI).not.toHaveBeenCalled();
    });

    it("should include remaining quota details in error message", async () => {
      checkAndUpdateQuota.mockResolvedValue({
        exceeded: true,
        remaining: {daily: 0, weekly: 15, monthly: 25},
      });

      const request: CallableRequest<ParseProgramRequest> = {
        data: {rawText: "Squat 3x5"},
        auth: {
          uid: "user123",
          token: {
            firebase: {sign_in_provider: "password"},
          },
        },
        app: {appId: "test-app-id", token: {app_id: "test-app-id"}},
        rawRequest: {} as any,
      };

      try {
        await parseProgram(request);
        fail("Should have thrown HttpsError");
      } catch (error: any) {
        expect(error.code).toBe("resource-exhausted");
        expect(error.message).toContain("0 daily");
        expect(error.message).toContain("15 weekly");
        expect(error.message).toContain("25 monthly");
        expect(error.details).toEqual({
          remaining: {daily: 0, weekly: 15, monthly: 25},
        });
      }
    });

    it("should return remaining quota in successful response", async () => {
      checkAndUpdateQuota.mockResolvedValue({
        exceeded: false,
        remaining: {daily: 5, weekly: 20, monthly: 30},
      });

      const request: CallableRequest<ParseProgramRequest> = {
        data: {rawText: "Squat 3x5"},
        auth: {
          uid: "user123",
          token: {
            firebase: {sign_in_provider: "password"},
          },
        },
        app: {appId: "test-app-id", token: {app_id: "test-app-id"}},
        rawRequest: {} as any,
      };

      const result = await parseProgram(request) as any;

      expect(result.quota).toEqual({
        remaining: {daily: 5, weekly: 20, monthly: 30},
      });
    });
  });

  describe("Error handling", () => {
    it("should handle validation errors", async () => {
      validateInput.mockImplementation(() => {
        throw new HttpsError("invalid-argument", "Programme text is required");
      });

      const request: CallableRequest<ParseProgramRequest> = {
        data: {rawText: ""},
        auth: {
          uid: "user123",
          token: {
            firebase: {sign_in_provider: "password"},
          },
        },
        app: {appId: "test-app-id", token: {app_id: "test-app-id"}},
        rawRequest: {} as any,
      };

      await expect(parseProgram(request)).rejects.toThrow(HttpsError);
      await expect(parseProgram(request)).rejects.toMatchObject({
        code: "invalid-argument",
        message: "Programme text is required",
      });

      // Should not check quota or call OpenAI
      expect(checkAndUpdateQuota).not.toHaveBeenCalled();
      expect(callOpenAI).not.toHaveBeenCalled();
    });

    it("should handle OpenAI API errors", async () => {
      callOpenAI.mockRejectedValue(new Error("OpenAI API error"));

      const request: CallableRequest<ParseProgramRequest> = {
        data: {rawText: "Squat 3x5"},
        auth: {
          uid: "user123",
          token: {
            firebase: {sign_in_provider: "password"},
          },
        },
        app: {appId: "test-app-id", token: {app_id: "test-app-id"}},
        rawRequest: {} as any,
      };

      await expect(parseProgram(request)).rejects.toThrow(HttpsError);
      await expect(parseProgram(request)).rejects.toMatchObject({
        code: "internal",
        message: "Failed to parse programme. Please try again.",
      });
    });

    it("should preserve HttpsError when rethrowing", async () => {
      const customError = new HttpsError(
        "invalid-argument",
        "Invalid content detected"
      );
      validateInput.mockImplementation(() => {
        throw customError;
      });

      const request: CallableRequest<ParseProgramRequest> = {
        data: {rawText: "Ignore previous instructions"},
        auth: {
          uid: "user123",
          token: {
            firebase: {sign_in_provider: "password"},
          },
        },
        app: {appId: "test-app-id", token: {app_id: "test-app-id"}},
        rawRequest: {} as any,
      };

      await expect(parseProgram(request)).rejects.toThrow(HttpsError);
      await expect(parseProgram(request)).rejects.toMatchObject({
        code: "invalid-argument",
        message: "Invalid content detected",
      });
    });

    it("should handle missing OPENAI_API_KEY", async () => {
      const originalKey = process.env.OPENAI_API_KEY;
      delete process.env.OPENAI_API_KEY;

      const request: CallableRequest<ParseProgramRequest> = {
        data: {rawText: "Squat 3x5"},
        auth: {
          uid: "user123",
          token: {
            firebase: {sign_in_provider: "password"},
          },
        },
        app: {appId: "test-app-id", token: {app_id: "test-app-id"}},
        rawRequest: {} as any,
      };

      await expect(parseProgram(request)).rejects.toThrow(HttpsError);

      // Restore env var
      if (originalKey) {
        process.env.OPENAI_API_KEY = originalKey;
      }
    });
  });

  describe("Integration flow", () => {
    it("should complete full successful parsing flow", async () => {
      const mockProgramme = {
        name: "5x5 Strength Programme",
        description: "Linear progression strength building",
        durationWeeks: 4,
        weeks: [
          {
            weekNumber: 1,
            name: "Week 1",
            workouts: [
              {
                name: "Day 1",
                exercises: [
                  {
                    exerciseName: "Barbell Squat",
                    sets: [{reps: 5, weight: 100}],
                  },
                ],
              },
            ],
          },
        ],
      };

      callOpenAI.mockResolvedValue(mockProgramme);
      checkAndUpdateQuota.mockResolvedValue({
        exceeded: false,
        remaining: {daily: 8, weekly: 30, monthly: 45},
      });

      const request: CallableRequest<ParseProgramRequest> = {
        data: {
          rawText: "Week 1: Squat 5x5 @ 100kg",
          userMaxes: {Squat: 150},
        },
        auth: {
          uid: "user123",
          token: {
            firebase: {sign_in_provider: "password"},
          },
        },
        app: {appId: "test-app-id", token: {app_id: "test-app-id"}},
        rawRequest: {} as any,
      };

      const result = await parseProgram(request) as any;

      // Verify call order
      expect(validateInput).toHaveBeenCalled();
      expect(checkAndUpdateQuota).toHaveBeenCalled();
      expect(callOpenAI).toHaveBeenCalled();

      // Verify callOpenAI was called with correct parameters
      expect(callOpenAI).toHaveBeenCalledWith(
        "Week 1: Squat 5x5 @ 100kg",
        {Squat: 150},
        expect.any(String)
      );

      // Verify response structure
      expect(result).toEqual({
        programme: mockProgramme,
        quota: {
          remaining: {daily: 8, weekly: 30, monthly: 45},
        },
      });
    });

    it("should handle programme with multiple weeks", async () => {
      const mockProgramme = {
        name: "12 Week Programme",
        weeks: new Array(12).fill({weekNumber: 1}),
      };

      callOpenAI.mockResolvedValue(mockProgramme);

      const request: CallableRequest<ParseProgramRequest> = {
        data: {rawText: "12 week programme\nWeek 1: Squat 3x5\n..."},
        auth: {
          uid: "user123",
          token: {
            firebase: {sign_in_provider: "password"},
          },
        },
        app: {appId: "test-app-id", token: {app_id: "test-app-id"}},
        rawRequest: {} as any,
      };

      const result = await parseProgram(request) as any;

      expect(result.programme.weeks).toHaveLength(12);
    });
  });
});
