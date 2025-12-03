import {Timestamp} from "firebase-admin/firestore";
import {HttpsError} from "firebase-functions/v2/https";
import {
  validateInput,
  getQuotaLimits,
  shouldResetMonthly,
  getSystemPrompt,
  decrementQuota,
  refundQuota,
  callOpenAI,
  AnalyzeTrainingRequest,
} from "./analyzeTrainingLogic";

// Mock OpenAI
jest.mock("openai", () => {
  return jest.fn().mockImplementation(() => ({
    chat: {
      completions: {
        create: jest.fn(),
      },
    },
  }));
});

describe("analyzeTrainingLogic", () => {
  describe("validateInput", () => {
    it("should accept valid training data", () => {
      const validData: AnalyzeTrainingRequest = {
        trainingData: "a".repeat(500),
      };
      expect(() => validateInput(validData)).not.toThrow();
    });

    it("should accept minimal valid training data", () => {
      const validData: AnalyzeTrainingRequest = {
        trainingData: "a".repeat(50),
      };
      expect(() => validateInput(validData)).not.toThrow();
    });

    it("should reject missing training data", () => {
      const invalidData: any = {};
      expect(() => validateInput(invalidData)).toThrow(HttpsError);
      try {
        validateInput(invalidData);
      } catch (error: any) {
        expect(error.code).toBe("invalid-argument");
      }
    });

    it("should reject non-string training data", () => {
      const invalidData: any = {trainingData: 123};
      expect(() => validateInput(invalidData)).toThrow(HttpsError);
      try {
        validateInput(invalidData);
      } catch (error: any) {
        expect(error.code).toBe("invalid-argument");
      }
    });

    it("should reject insufficient training data", () => {
      const invalidData: AnalyzeTrainingRequest = {
        trainingData: "a".repeat(49),
      };
      expect(() => validateInput(invalidData)).toThrow(HttpsError);
      try {
        validateInput(invalidData);
      } catch (error: any) {
        expect(error.code).toBe("invalid-argument");
      }
    });

    it("should reject training data that is too large", () => {
      const invalidData: AnalyzeTrainingRequest = {
        trainingData: "a".repeat(200001),
      };
      expect(() => validateInput(invalidData)).toThrow(HttpsError);
      try {
        validateInput(invalidData);
      } catch (error: any) {
        expect(error.code).toBe("invalid-argument");
      }
    });
  });

  describe("shouldResetMonthly", () => {
    it("should reset monthly quota on new month", () => {
      const lastMonth = new Date();
      lastMonth.setMonth(lastMonth.getMonth() - 1);
      const lastMonthTimestamp = Timestamp.fromDate(lastMonth);
      expect(shouldResetMonthly(lastMonthTimestamp)).toBe(true);
    });

    it("should not reset monthly quota in same month", () => {
      const now = Timestamp.now();
      expect(shouldResetMonthly(now)).toBe(false);
    });
  });

  describe("getSystemPrompt", () => {
    it("should return tier 1 prompt for 1 workout", () => {
      const prompt = getSystemPrompt(1, 1);
      expect(prompt).toContain("INITIAL feedback");
      expect(prompt).toContain("Do NOT attempt to identify trends");
      expect(prompt).toContain("overall_assessment");
    });

    it("should return tier 1 prompt for 5 workouts", () => {
      const prompt = getSystemPrompt(5, 3);
      expect(prompt).toContain("INITIAL feedback");
      expect(prompt).toContain("5 workout(s) over 3 week(s)");
    });

    it("should return tier 2 prompt for 6 workouts", () => {
      const prompt = getSystemPrompt(6, 4);
      expect(prompt).toContain("early-stage training");
      expect(prompt).toContain("EMERGING but NOT confirmed");
      expect(prompt).toContain("6 workouts over 4 weeks");
    });

    it("should return tier 2 prompt for 11 workouts", () => {
      const prompt = getSystemPrompt(11, 8);
      expect(prompt).toContain("early-stage training");
      expect(prompt).toContain("11 workouts over 8 weeks");
    });

    it("should return tier 3 prompt for 12 workouts", () => {
      const prompt = getSystemPrompt(12, 12);
      expect(prompt).toContain("training history");
      expect(prompt).toContain("sufficient data for trend analysis");
      expect(prompt).toContain("12 workouts over 12 weeks");
    });

    it("should return tier 3 prompt for 50 workouts", () => {
      const prompt = getSystemPrompt(50, 25);
      expect(prompt).toContain("training history");
      expect(prompt).toContain("50 workouts over 25 weeks");
    });
  });

  describe("decrementQuota", () => {
    let mockDb: any;
    let mockTransaction: any;
    let mockQuotaRef: any;

    beforeEach(() => {
      mockTransaction = {
        get: jest.fn(),
        set: jest.fn(),
      };

      mockQuotaRef = {
        id: "user123",
      };

      mockDb = {
        collection: jest.fn(() => ({
          doc: jest.fn(() => mockQuotaRef),
        })),
        runTransaction: jest.fn(async (callback) => callback(mockTransaction)),
      };
    });

    it("should create initial quota for new user", async () => {
      mockTransaction.get.mockResolvedValue({
        exists: false,
      });

      const result = await decrementQuota("user123", mockDb);

      expect(result.remainingQuotas.monthly).toBe(9);
      expect(mockTransaction.set).toHaveBeenCalledWith(
        mockQuotaRef,
        expect.objectContaining({
          monthlyCount: 9,
        })
      );
    });

    it("should decrement existing quota", async () => {
      mockTransaction.get.mockResolvedValue({
        exists: true,
        data: () => ({
          monthlyCount: 5,
          lastReset: Timestamp.now(),
        }),
      });

      const result = await decrementQuota("user123", mockDb);

      expect(result.remainingQuotas.monthly).toBe(4);
      expect(mockTransaction.set).toHaveBeenCalledWith(
        mockQuotaRef,
        expect.objectContaining({
          monthlyCount: 4,
        })
      );
    });

    it("should throw error when quota is zero", async () => {
      mockTransaction.get.mockResolvedValue({
        exists: true,
        data: () => ({
          monthlyCount: 0,
          lastReset: Timestamp.now(),
        }),
      });

      await expect(decrementQuota("user123", mockDb)).rejects.toThrow(HttpsError);
      await expect(decrementQuota("user123", mockDb)).rejects.toMatchObject({
        code: "resource-exhausted",
      });

      expect(mockTransaction.set).not.toHaveBeenCalled();
    });

    it("should reset monthly quota on new month", async () => {
      const lastMonth = new Date();
      lastMonth.setMonth(lastMonth.getMonth() - 1);

      mockTransaction.get.mockResolvedValue({
        exists: true,
        data: () => ({
          monthlyCount: 0,
          lastReset: Timestamp.fromDate(lastMonth),
        }),
      });

      const result = await decrementQuota("user123", mockDb);

      expect(result.remainingQuotas.monthly).toBe(9);
      expect(mockTransaction.set).toHaveBeenCalledWith(
        mockQuotaRef,
        expect.objectContaining({
          monthlyCount: 9,
        })
      );
    });
  });

  describe("refundQuota", () => {
    let mockDb: any;
    let mockTransaction: any;
    let mockQuotaRef: any;

    beforeEach(() => {
      mockTransaction = {
        get: jest.fn(),
        update: jest.fn(),
      };

      mockQuotaRef = {
        id: "user123",
      };

      mockDb = {
        collection: jest.fn(() => ({
          doc: jest.fn(() => mockQuotaRef),
        })),
        runTransaction: jest.fn(async (callback) => callback(mockTransaction)),
      };
    });

    it("should refund quota when document exists", async () => {
      mockTransaction.get.mockResolvedValue({
        exists: true,
        data: () => ({
          monthlyCount: 5,
        }),
      });

      await refundQuota("user123", mockDb);

      expect(mockTransaction.update).toHaveBeenCalledWith(mockQuotaRef, {
        monthlyCount: 6,
      });
    });

    it("should not refund beyond quota limit", async () => {
      mockTransaction.get.mockResolvedValue({
        exists: true,
        data: () => ({
          monthlyCount: 10,
        }),
      });

      await refundQuota("user123", mockDb);

      expect(mockTransaction.update).toHaveBeenCalledWith(mockQuotaRef, {
        monthlyCount: 10,
      });
    });

    it("should do nothing when document does not exist", async () => {
      mockTransaction.get.mockResolvedValue({
        exists: false,
      });

      await refundQuota("user123", mockDb);

      expect(mockTransaction.update).not.toHaveBeenCalled();
    });

    it("should handle edge case of zero quota", async () => {
      mockTransaction.get.mockResolvedValue({
        exists: true,
        data: () => ({
          monthlyCount: 0,
        }),
      });

      await refundQuota("user123", mockDb);

      expect(mockTransaction.update).toHaveBeenCalledWith(mockQuotaRef, {
        monthlyCount: 1,
      });
    });
  });

  describe("callOpenAI", () => {
    const mockCreate = jest.fn();

    beforeEach(() => {
      const OpenAI = require("openai");
      OpenAI.mockImplementation(() => ({
        chat: {
          completions: {
            create: mockCreate,
          },
        },
      }));
      mockCreate.mockClear();
    });

    it("should use correct model", async () => {
      mockCreate.mockResolvedValue({
        choices: [{
          message: {
            content: JSON.stringify({
              overall_assessment: "Good",
              key_insights: [],
              recommendations: [],
              warnings: [],
            }),
          },
        }],
      });

      await callOpenAI("test data", "test-key");

      expect(mockCreate).toHaveBeenCalledWith(
        expect.objectContaining({
          model: "gpt-5-mini",
        })
      );
    });

    it("should throw error on empty response", async () => {
      mockCreate.mockResolvedValue({
        choices: [{
          message: {},
        }],
      });

      await expect(callOpenAI("test data", "test-key")).rejects.toThrow(
        "No content in OpenAI response"
      );
    });

    it("should handle OpenAI API errors", async () => {
      mockCreate.mockRejectedValue(new Error("API rate limit exceeded"));

      await expect(callOpenAI("test data", "test-key")).rejects.toThrow(
        "API rate limit exceeded"
      );
    });

    it("should parse JSON response correctly", async () => {
      const mockResponse = {
        overall_assessment: "Excellent progress",
        key_insights: [{category: "VOLUME", message: "Good volume", severity: "SUCCESS"}],
        recommendations: ["Keep it up"],
        warnings: [],
      };

      mockCreate.mockResolvedValue({
        choices: [{
          message: {
            content: JSON.stringify(mockResponse),
          },
        }],
      });

      const result = await callOpenAI("test data", "test-key");

      expect(result).toEqual(mockResponse);
    });
  });
});