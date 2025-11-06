import {Timestamp} from "firebase-admin/firestore";
import {HttpsError} from "firebase-functions/v2/https";
import {
  validateInput,
  getQuotaLimits,
  shouldResetMonthly,
  getSystemPrompt,
  AnalyzeTrainingRequest,
} from "./analyzeTrainingLogic";

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

  describe("getQuotaLimits", () => {
    it("should return correct quota limits", () => {
      const limits = getQuotaLimits();
      expect(limits.monthly).toBe(10);
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
});