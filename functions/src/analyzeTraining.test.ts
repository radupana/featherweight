/**
 * Unit tests for analyzeTraining callable function wrapper
 * Tests authentication, App Check, quota management, and error recovery
 */

import {CallableRequest, HttpsError} from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import {AnalyzeTrainingRequest} from "./analyzeTrainingLogic";

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

// Mock analyzeTrainingLogic
jest.mock("./analyzeTrainingLogic", () => ({
  validateInput: jest.fn(),
  decrementQuota: jest.fn(),
  refundQuota: jest.fn(),
  callOpenAI: jest.fn(),
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

describe("analyzeTraining callable function", () => {
  let analyzeTraining: (request: CallableRequest<AnalyzeTrainingRequest>) => Promise<unknown>;
  let validateInput: jest.Mock;
  let decrementQuota: jest.Mock;
  let refundQuota: jest.Mock;
  let callOpenAI: jest.Mock;

  beforeEach(() => {
    jest.clearAllMocks();

    // Set up environment variable
    process.env.OPENAI_API_KEY = "test-api-key";

    // Import the function
    const module = require("./analyzeTraining");
    analyzeTraining = module.analyzeTraining;

    // Get mocked functions
    const logic = require("./analyzeTrainingLogic");
    validateInput = logic.validateInput;
    decrementQuota = logic.decrementQuota;
    refundQuota = logic.refundQuota;
    callOpenAI = logic.callOpenAI;

    // Set default mock implementations
    validateInput.mockImplementation(() => {});
    decrementQuota.mockResolvedValue({remainingQuotas: {monthly: 9}});
    callOpenAI.mockResolvedValue({
      overall_assessment: "Good progress",
      key_insights: [],
      recommendations: [],
      warnings: [],
    });
  });

  afterEach(() => {
    delete process.env.OPENAI_API_KEY;
  });

  describe("App Check validation", () => {
    it("should reject requests without App Check token", async () => {
      const request: CallableRequest<AnalyzeTrainingRequest> = {
        data: {trainingData: "test data"},
        auth: {
          uid: "user123",
          token: {},
        },
        app: undefined,
        rawRequest: {} as any,
      };

      await expect(analyzeTraining(request)).rejects.toThrow(HttpsError);
      await expect(analyzeTraining(request)).rejects.toMatchObject({
        code: "unauthenticated",
        message: "App Check verification failed",
      });
    });

    it("should bypass App Check for test users", async () => {
      const request: CallableRequest<AnalyzeTrainingRequest> = {
        data: {trainingData: "a".repeat(100)},
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

      const result = await analyzeTraining(request);

      expect(result).toEqual({
        analysis: expect.any(Object),
        quota: {remaining: {monthly: 9}},
      });
      expect(callOpenAI).toHaveBeenCalled();
    });

    it("should allow requests with valid App Check token", async () => {
      const request: CallableRequest<AnalyzeTrainingRequest> = {
        data: {trainingData: "a".repeat(100)},
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

      const result = await analyzeTraining(request);

      expect(result).toEqual({
        analysis: expect.any(Object),
        quota: {remaining: {monthly: 9}},
      });
    });
  });

  describe("Authentication validation", () => {
    it("should reject requests without authentication", async () => {
      const request: CallableRequest<AnalyzeTrainingRequest> = {
        data: {trainingData: "test data"},
        auth: undefined,
        app: {appId: "test-app-id", token: {app_id: "test-app-id"}},
        rawRequest: {} as any,
      };

      await expect(analyzeTraining(request)).rejects.toThrow(HttpsError);
      await expect(analyzeTraining(request)).rejects.toMatchObject({
        code: "unauthenticated",
        message: "Authentication required",
      });
    });

    it("should reject anonymous users", async () => {
      const request: CallableRequest<AnalyzeTrainingRequest> = {
        data: {trainingData: "test data"},
        auth: {
          uid: "anon123",
          token: {
            firebase: {sign_in_provider: "anonymous"},
          },
        },
        app: {appId: "test-app-id", token: {app_id: "test-app-id"}},
        rawRequest: {} as any,
      };

      await expect(analyzeTraining(request)).rejects.toThrow(HttpsError);
      await expect(analyzeTraining(request)).rejects.toMatchObject({
        code: "unauthenticated",
        message: "Sign in required to use AI training analysis",
      });
    });

    it("should allow authenticated users with password provider", async () => {
      const request: CallableRequest<AnalyzeTrainingRequest> = {
        data: {trainingData: "a".repeat(100)},
        auth: {
          uid: "user123",
          token: {
            firebase: {sign_in_provider: "password"},
          },
        },
        app: {appId: "test-app-id", token: {app_id: "test-app-id"}},
        rawRequest: {} as any,
      };

      const result = await analyzeTraining(request);

      expect(result).toEqual({
        analysis: expect.any(Object),
        quota: {remaining: {monthly: 9}},
      });
    });

    it("should allow authenticated users with google provider", async () => {
      const request: CallableRequest<AnalyzeTrainingRequest> = {
        data: {trainingData: "a".repeat(100)},
        auth: {
          uid: "user456",
          token: {
            firebase: {sign_in_provider: "google.com"},
          },
        },
        app: {appId: "test-app-id", token: {app_id: "test-app-id"}},
        rawRequest: {} as any,
      };

      const result = await analyzeTraining(request);

      expect(result).toEqual({
        analysis: expect.any(Object),
        quota: {remaining: {monthly: 9}},
      });
    });
  });

  describe("Quota management", () => {
    it("should decrement quota before analysis", async () => {
      const request: CallableRequest<AnalyzeTrainingRequest> = {
        data: {trainingData: "a".repeat(100)},
        auth: {
          uid: "user123",
          token: {
            firebase: {sign_in_provider: "password"},
          },
        },
        app: {appId: "test-app-id", token: {app_id: "test-app-id"}},
        rawRequest: {} as any,
      };

      await analyzeTraining(request);

      expect(decrementQuota).toHaveBeenCalledWith(
        "user123",
        expect.anything()
      );
      // Both functions should have been called
      expect(decrementQuota).toHaveBeenCalled();
      expect(callOpenAI).toHaveBeenCalled();
    });

    it("should handle quota exceeded error", async () => {
      decrementQuota.mockRejectedValue(
        new HttpsError("resource-exhausted", "Quota exceeded", {
          remaining: {monthly: 0},
        })
      );

      const request: CallableRequest<AnalyzeTrainingRequest> = {
        data: {trainingData: "a".repeat(100)},
        auth: {
          uid: "user123",
          token: {
            firebase: {sign_in_provider: "password"},
          },
        },
        app: {appId: "test-app-id", token: {app_id: "test-app-id"}},
        rawRequest: {} as any,
      };

      await expect(analyzeTraining(request)).rejects.toThrow(HttpsError);
      await expect(analyzeTraining(request)).rejects.toMatchObject({
        code: "resource-exhausted",
        message: "Quota exceeded",
      });

      // Should not call OpenAI if quota exceeded
      expect(callOpenAI).not.toHaveBeenCalled();
      // Should not refund quota on quota exceeded error
      expect(refundQuota).not.toHaveBeenCalled();
    });

    it("should return remaining quota in response", async () => {
      decrementQuota.mockResolvedValue({remainingQuotas: {monthly: 5}});

      const request: CallableRequest<AnalyzeTrainingRequest> = {
        data: {trainingData: "a".repeat(100)},
        auth: {
          uid: "user123",
          token: {
            firebase: {sign_in_provider: "password"},
          },
        },
        app: {appId: "test-app-id", token: {app_id: "test-app-id"}},
        rawRequest: {} as any,
      };

      const result = await analyzeTraining(request) as any;

      expect(result.quota).toEqual({remaining: {monthly: 5}});
    });
  });

  describe("Error handling and quota refund", () => {
    it("should refund quota on OpenAI API error", async () => {
      callOpenAI.mockRejectedValue(new Error("OpenAI API error"));

      const request: CallableRequest<AnalyzeTrainingRequest> = {
        data: {trainingData: "a".repeat(100)},
        auth: {
          uid: "user123",
          token: {
            firebase: {sign_in_provider: "password"},
          },
        },
        app: {appId: "test-app-id", token: {app_id: "test-app-id"}},
        rawRequest: {} as any,
      };

      await expect(analyzeTraining(request)).rejects.toThrow(HttpsError);
      await expect(analyzeTraining(request)).rejects.toMatchObject({
        code: "internal",
        message: "Failed to analyze training. Please try again.",
      });

      expect(refundQuota).toHaveBeenCalledWith("user123", expect.anything());
    });

    it("should refund quota on validation error after quota decrement", async () => {
      // Validation passes initially, but fails during processing
      validateInput.mockImplementation(() => {
        throw new HttpsError("invalid-argument", "Invalid data");
      });

      const request: CallableRequest<AnalyzeTrainingRequest> = {
        data: {trainingData: "bad data"},
        auth: {
          uid: "user123",
          token: {
            firebase: {sign_in_provider: "password"},
          },
        },
        app: {appId: "test-app-id", token: {app_id: "test-app-id"}},
        rawRequest: {} as any,
      };

      await expect(analyzeTraining(request)).rejects.toThrow(HttpsError);

      // Should NOT refund quota on validation error (quota is decremented after validation)
      expect(refundQuota).not.toHaveBeenCalled();
    });

    it("should NOT refund quota on quota exceeded error", async () => {
      decrementQuota.mockRejectedValue(
        new HttpsError("resource-exhausted", "Quota exceeded")
      );

      const request: CallableRequest<AnalyzeTrainingRequest> = {
        data: {trainingData: "a".repeat(100)},
        auth: {
          uid: "user123",
          token: {
            firebase: {sign_in_provider: "password"},
          },
        },
        app: {appId: "test-app-id", token: {app_id: "test-app-id"}},
        rawRequest: {} as any,
      };

      await expect(analyzeTraining(request)).rejects.toThrow(HttpsError);

      expect(refundQuota).not.toHaveBeenCalled();
    });

    it("should handle refund failure gracefully", async () => {
      callOpenAI.mockRejectedValue(new Error("OpenAI error"));
      refundQuota.mockRejectedValue(new Error("Refund failed"));

      const request: CallableRequest<AnalyzeTrainingRequest> = {
        data: {trainingData: "a".repeat(100)},
        auth: {
          uid: "user123",
          token: {
            firebase: {sign_in_provider: "password"},
          },
        },
        app: {appId: "test-app-id", token: {app_id: "test-app-id"}},
        rawRequest: {} as any,
      };

      // Should still throw the original error, not the refund error
      await expect(analyzeTraining(request)).rejects.toThrow(HttpsError);
      await expect(analyzeTraining(request)).rejects.toMatchObject({
        code: "internal",
        message: "Failed to analyze training. Please try again.",
      });
    });

    it("should preserve HttpsError when rethrowing", async () => {
      const customError = new HttpsError(
        "invalid-argument",
        "Custom validation error"
      );
      validateInput.mockImplementation(() => {
        throw customError;
      });

      const request: CallableRequest<AnalyzeTrainingRequest> = {
        data: {trainingData: "bad data"},
        auth: {
          uid: "user123",
          token: {
            firebase: {sign_in_provider: "password"},
          },
        },
        app: {appId: "test-app-id", token: {app_id: "test-app-id"}},
        rawRequest: {} as any,
      };

      await expect(analyzeTraining(request)).rejects.toThrow(HttpsError);
      await expect(analyzeTraining(request)).rejects.toMatchObject({
        code: "invalid-argument",
        message: "Custom validation error",
      });
    });
  });

  describe("Integration flow", () => {
    it("should complete full successful analysis flow", async () => {
      const mockAnalysis = {
        overall_assessment: "Strong progress on major lifts",
        key_insights: [
          {
            category: "PROGRESSION",
            message: "Bench press showing steady gains",
            severity: "SUCCESS",
          },
        ],
        recommendations: ["Continue current programming"],
        warnings: [],
      };

      callOpenAI.mockResolvedValue(mockAnalysis);
      decrementQuota.mockResolvedValue({remainingQuotas: {monthly: 7}});

      const request: CallableRequest<AnalyzeTrainingRequest> = {
        data: {trainingData: "a".repeat(100)},
        auth: {
          uid: "user123",
          token: {
            firebase: {sign_in_provider: "password"},
          },
        },
        app: {appId: "test-app-id", token: {app_id: "test-app-id"}},
        rawRequest: {} as any,
      };

      const result = await analyzeTraining(request) as any;

      // Verify call order
      expect(validateInput).toHaveBeenCalled();
      expect(decrementQuota).toHaveBeenCalled();
      expect(callOpenAI).toHaveBeenCalled();
      expect(refundQuota).not.toHaveBeenCalled();

      // Verify response structure
      expect(result).toEqual({
        analysis: mockAnalysis,
        quota: {
          remaining: {monthly: 7},
        },
      });
    });

    it("should handle missing OPENAI_API_KEY", async () => {
      const originalKey = process.env.OPENAI_API_KEY;
      delete process.env.OPENAI_API_KEY;

      const request: CallableRequest<AnalyzeTrainingRequest> = {
        data: {trainingData: "a".repeat(100)},
        auth: {
          uid: "user123",
          token: {
            firebase: {sign_in_provider: "password"},
          },
        },
        app: {appId: "test-app-id", token: {app_id: "test-app-id"}},
        rawRequest: {} as any,
      };

      await expect(analyzeTraining(request)).rejects.toThrow(HttpsError);

      // Restore env var
      if (originalKey) {
        process.env.OPENAI_API_KEY = originalKey;
      }
    });
  });
});
