/**
 * Unit tests for analyzeTraining callable function wrapper
 * Tests authentication, App Check, quota management, and error recovery
 */

import {HttpsError} from "firebase-functions/v2/https";
import {AnalyzeTrainingRequest} from "./analyzeTrainingLogic";

// Helper to create properly typed mock app check token
const createMockAppCheckToken = () => ({
  app_id: "test-app-id",
  iss: "https://firebaseappcheck.googleapis.com/test-project",
  sub: "test-project",
  aud: ["projects/test-project"],
  exp: Math.floor(Date.now() / 1000) + 3600,
  iat: Math.floor(Date.now() / 1000),
});

// Helper to create properly typed mock decoded id token
const createMockDecodedIdToken = (
  uid: string,
  signInProvider: string,
  extras: Record<string, unknown> = {}
) => ({
  aud: "test-project",
  auth_time: Math.floor(Date.now() / 1000),
  exp: Math.floor(Date.now() / 1000) + 3600,
  iat: Math.floor(Date.now() / 1000),
  iss: "https://securetoken.google.com/test-project",
  sub: uid,
  uid,
  firebase: {
    identities: {},
    sign_in_provider: signInProvider,
  },
  ...extras,
});

// Helper to create CallableRequest for tests - use 'any' to avoid strict type checking
const createMockCallableRequest = (
  data: AnalyzeTrainingRequest,
  options: {
    uid?: string;
    signInProvider?: string;
    hasAppCheck?: boolean;
    hasAuth?: boolean;
    tokenExtras?: Record<string, unknown>;
  } = {}
): any => {
  const {
    uid = "user123",
    signInProvider = "password",
    hasAppCheck = true,
    hasAuth = true,
    tokenExtras = {},
  } = options;

  return {
    data,
    auth: hasAuth ? {
      uid,
      token: createMockDecodedIdToken(uid, signInProvider, tokenExtras),
      rawToken: "raw-token-string",
    } : undefined,
    app: hasAppCheck ? {appId: "test-app-id", token: createMockAppCheckToken()} : undefined,
    rawRequest: {},
    acceptsStreaming: false,
  };
};

// Mock firebase-admin
jest.mock("firebase-admin", () => {
  const mockTimestamp = (date: Date) => ({
    toDate: jest.fn(() => date),
  });

  return {
    initializeApp: jest.fn(),
    firestore: {
      Timestamp: {
        now: jest.fn(() => mockTimestamp(new Date())),
        fromDate: jest.fn((date: Date) => mockTimestamp(date)),
      },
    },
  };
});

// Mock firebase-functions v2
jest.mock("firebase-functions/v2", () => ({
  setGlobalOptions: jest.fn(),
}));

// Mock firebase-functions v2/https
jest.mock("firebase-functions/v2/https", () => ({
  onCall: jest.fn((config, handler) => handler),
  onRequest: jest.fn((config, handler) => handler),
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
  let analyzeTraining: (request: any) => Promise<unknown>;
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
      const request = createMockCallableRequest(
        {trainingData: "test data"},
        {hasAppCheck: false}
      );

      await expect(analyzeTraining(request)).rejects.toThrow(HttpsError);
      await expect(analyzeTraining(request)).rejects.toMatchObject({
        code: "unauthenticated",
        message: "App Check verification failed",
      });
    });

    it("should bypass App Check for test users", async () => {
      const request = createMockCallableRequest(
        {trainingData: "a".repeat(100)},
        {uid: "testuser123", hasAppCheck: false, tokenExtras: {testUser: true}}
      );

      const result = await analyzeTraining(request);

      expect(result).toEqual({
        analysis: expect.any(Object),
        quota: {remaining: {monthly: 9}},
      });
      expect(callOpenAI).toHaveBeenCalled();
    });

    it("should allow requests with valid App Check token", async () => {
      const request = createMockCallableRequest({trainingData: "a".repeat(100)});

      const result = await analyzeTraining(request);

      expect(result).toEqual({
        analysis: expect.any(Object),
        quota: {remaining: {monthly: 9}},
      });
    });
  });

  describe("Authentication validation", () => {
    it("should reject requests without authentication", async () => {
      const request = createMockCallableRequest(
        {trainingData: "test data"},
        {hasAuth: false}
      );

      await expect(analyzeTraining(request)).rejects.toThrow(HttpsError);
      await expect(analyzeTraining(request)).rejects.toMatchObject({
        code: "unauthenticated",
        message: "Authentication required",
      });
    });

    it("should reject anonymous users", async () => {
      const request = createMockCallableRequest(
        {trainingData: "test data"},
        {uid: "anon123", signInProvider: "anonymous"}
      );

      await expect(analyzeTraining(request)).rejects.toThrow(HttpsError);
      await expect(analyzeTraining(request)).rejects.toMatchObject({
        code: "unauthenticated",
        message: "Sign in required to use AI training analysis",
      });
    });

    it("should allow authenticated users with password provider", async () => {
      const request = createMockCallableRequest(
        {trainingData: "a".repeat(100)},
        {signInProvider: "password"}
      );

      const result = await analyzeTraining(request);

      expect(result).toEqual({
        analysis: expect.any(Object),
        quota: {remaining: {monthly: 9}},
      });
    });

    it("should allow authenticated users with google provider", async () => {
      const request = createMockCallableRequest(
        {trainingData: "a".repeat(100)},
        {uid: "user456", signInProvider: "google.com"}
      );

      const result = await analyzeTraining(request);

      expect(result).toEqual({
        analysis: expect.any(Object),
        quota: {remaining: {monthly: 9}},
      });
    });
  });

  describe("Quota management", () => {
    it("should decrement quota before analysis", async () => {
      const request = createMockCallableRequest({trainingData: "a".repeat(100)});

      await analyzeTraining(request);

      expect(decrementQuota).toHaveBeenCalledWith(
        "user123",
        expect.anything()
      );
      expect(decrementQuota).toHaveBeenCalled();
      expect(callOpenAI).toHaveBeenCalled();
    });

    it("should handle quota exceeded error", async () => {
      decrementQuota.mockRejectedValue(
        new HttpsError("resource-exhausted", "Quota exceeded", {
          remaining: {monthly: 0},
        })
      );

      const request = createMockCallableRequest({trainingData: "a".repeat(100)});

      await expect(analyzeTraining(request)).rejects.toThrow(HttpsError);
      await expect(analyzeTraining(request)).rejects.toMatchObject({
        code: "resource-exhausted",
        message: "Quota exceeded",
      });

      expect(callOpenAI).not.toHaveBeenCalled();
      expect(refundQuota).not.toHaveBeenCalled();
    });

    it("should return remaining quota in response", async () => {
      decrementQuota.mockResolvedValue({remainingQuotas: {monthly: 5}});

      const request = createMockCallableRequest({trainingData: "a".repeat(100)});

      const result = await analyzeTraining(request) as any;

      expect(result.quota).toEqual({remaining: {monthly: 5}});
    });
  });

  describe("Error handling and quota refund", () => {
    it("should refund quota on OpenAI API error", async () => {
      callOpenAI.mockRejectedValue(new Error("OpenAI API error"));

      const request = createMockCallableRequest({trainingData: "a".repeat(100)});

      await expect(analyzeTraining(request)).rejects.toThrow(HttpsError);
      await expect(analyzeTraining(request)).rejects.toMatchObject({
        code: "internal",
        message: "Failed to analyze training. Please try again.",
      });

      expect(refundQuota).toHaveBeenCalledWith("user123", expect.anything());
    });

    it("should refund quota on validation error after quota decrement", async () => {
      validateInput.mockImplementation(() => {
        throw new HttpsError("invalid-argument", "Invalid data");
      });

      const request = createMockCallableRequest({trainingData: "bad data"});

      await expect(analyzeTraining(request)).rejects.toThrow(HttpsError);

      // Should NOT refund quota on validation error (quota is decremented after validation)
      expect(refundQuota).not.toHaveBeenCalled();
    });

    it("should NOT refund quota on quota exceeded error", async () => {
      decrementQuota.mockRejectedValue(
        new HttpsError("resource-exhausted", "Quota exceeded")
      );

      const request = createMockCallableRequest({trainingData: "a".repeat(100)});

      await expect(analyzeTraining(request)).rejects.toThrow(HttpsError);

      expect(refundQuota).not.toHaveBeenCalled();
    });

    it("should handle refund failure gracefully", async () => {
      callOpenAI.mockRejectedValue(new Error("OpenAI error"));
      refundQuota.mockRejectedValue(new Error("Refund failed"));

      const request = createMockCallableRequest({trainingData: "a".repeat(100)});

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

      const request = createMockCallableRequest({trainingData: "bad data"});

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

      const request = createMockCallableRequest({trainingData: "a".repeat(100)});

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

      const request = createMockCallableRequest({trainingData: "a".repeat(100)});

      await expect(analyzeTraining(request)).rejects.toThrow(HttpsError);

      if (originalKey) {
        process.env.OPENAI_API_KEY = originalKey;
      }
    });
  });
});
