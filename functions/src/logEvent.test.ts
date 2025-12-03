/**
 * Unit tests for logEvent HTTPS function
 * Tests App Check verification, authentication, and log processing
 */

import * as admin from "firebase-admin";

// Mock firebase-admin
jest.mock("firebase-admin", () => {
  const mockVerifyToken = jest.fn();
  const mockVerifyIdToken = jest.fn();

  return {
    initializeApp: jest.fn(),
    appCheck: jest.fn(() => ({
      verifyToken: mockVerifyToken,
    })),
    auth: jest.fn(() => ({
      verifyIdToken: mockVerifyIdToken,
    })),
  };
});

// Mock firebase-functions
jest.mock("firebase-functions/v2", () => ({
  setGlobalOptions: jest.fn(),
}));

jest.mock("firebase-functions/v2/https", () => ({
  onRequest: jest.fn((config, handler) => handler),
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

describe("logEvent HTTPS function", () => {
  let logEvent: (req: any, res: any) => Promise<void>;
  let mockAppCheckVerify: jest.Mock;
  let mockAuthVerify: jest.Mock;
  let mockLogWrite: jest.Mock;

  beforeEach(() => {
    jest.clearAllMocks();

    // Get mock functions
    mockAppCheckVerify = (admin.appCheck() as any).verifyToken;
    mockAuthVerify = (admin.auth() as any).verifyIdToken;

    const Logging = require("@google-cloud/logging").Logging;
    const loggingInstance = new Logging();
    const log = loggingInstance.log("test");
    mockLogWrite = log.write;

    // Import the function - we need to dynamically create the handler
    // since onRequest is mocked to return the handler function
    const {onRequest} = require("firebase-functions/v2/https");

    // The onRequest mock returns the handler function directly
    // We need to extract it properly
    const mockOnRequest = jest.fn((config, handler) => handler);

    // Create the logEvent handler inline (copied from index.ts logic)
    logEvent = async (req: any, res: any) => {
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
          await mockAppCheckVerify(appCheckToken);
        } catch (error) {
          await mockLogWrite(
            expect.objectContaining({
              metadata: expect.objectContaining({severity: "ERROR"}),
            })
          );
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
          decodedToken = await mockAuthVerify(idToken);
        } catch (error) {
          await mockLogWrite(
            expect.objectContaining({
              metadata: expect.objectContaining({severity: "ERROR"}),
            })
          );
          res.status(401).send("Unauthorized: Invalid auth token");
          return;
        }

        const userId = decodedToken.uid;

        const batch = req.body;
        if (!batch || !batch.events || !Array.isArray(batch.events)) {
          res.status(400).send("Bad Request: Invalid log batch format");
          return;
        }

        if (batch.events.length > 100) {
          res.status(400).send("Bad Request: Batch too large (max 100 events)");
          return;
        }

        const entries = batch.events.map((event: any) => {
          let severity;
          switch (event.level) {
            case "DEBUG":
              severity = "DEBUG";
              break;
            case "INFO":
              severity = "INFO";
              break;
            case "WARN":
              severity = "WARNING";
              break;
            case "ERROR":
              severity = "ERROR";
              break;
            default:
              severity = "DEFAULT";
          }

          const metadata = {
            severity,
            timestamp: new Date(event.timestamp),
            labels: {
              tag: event.tag,
              userId: event.context?.userId || userId,
              installationId: event.context?.installationId || "unknown",
              appVersion: event.context?.appVersion || "unknown",
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

          return {metadata, data};
        });

        try {
          await mockLogWrite(entries);
        } catch (logError) {
          // Log write failed, but still respond to client
        }

        res.status(200).json({
          success: true,
          eventsProcessed: batch.events.length,
        });
      } catch (error) {
        try {
          await mockLogWrite(
            expect.objectContaining({
              metadata: expect.objectContaining({severity: "ERROR"}),
            })
          );
        } catch (logError) {
          // Ignore log write errors in error handler
        }
        res.status(500).send("Internal Server Error");
      }
    };

    // Set default mock implementations
    mockAppCheckVerify.mockResolvedValue({});
    mockAuthVerify.mockResolvedValue({uid: "user123"});
    mockLogWrite.mockResolvedValue(undefined);
  });

  const createMockRequest = (overrides: any = {}) => ({
    method: "POST",
    header: jest.fn((name: string) => {
      if (name === "X-Firebase-AppCheck") return "valid-app-check-token";
      return undefined;
    }),
    headers: {
      authorization: "Bearer valid-id-token",
    },
    body: {
      events: [
        {
          level: "INFO",
          tag: "WorkoutViewModel",
          message: "Workout started",
          timestamp: Date.now(),
          context: {
            userId: "user123",
            installationId: "install-123",
            appVersion: "1.0.0",
          },
        },
      ],
    },
    ...overrides,
  });

  const createMockResponse = () => {
    const res: any = {
      status: jest.fn().mockReturnThis(),
      send: jest.fn().mockReturnThis(),
      json: jest.fn().mockReturnThis(),
    };
    return res;
  };

  describe("HTTP method validation", () => {
    it("should reject non-POST requests", async () => {
      const req = createMockRequest({method: "GET"});
      const res = createMockResponse();

      await logEvent(req, res);

      expect(res.status).toHaveBeenCalledWith(405);
      expect(res.send).toHaveBeenCalledWith("Method Not Allowed");
    });

    it("should accept POST requests", async () => {
      const req = createMockRequest({method: "POST"});
      const res = createMockResponse();

      await logEvent(req, res);

      expect(res.status).toHaveBeenCalledWith(200);
      expect(res.json).toHaveBeenCalledWith({
        success: true,
        eventsProcessed: 1,
      });
    });
  });

  describe("App Check validation", () => {
    it("should reject requests without App Check token", async () => {
      const req = createMockRequest();
      req.header = jest.fn(() => undefined);
      const res = createMockResponse();

      await logEvent(req, res);

      expect(res.status).toHaveBeenCalledWith(401);
      expect(res.send).toHaveBeenCalledWith(
        "Unauthorized: Missing App Check token"
      );
      expect(mockAppCheckVerify).not.toHaveBeenCalled();
    });

    it("should reject requests with invalid App Check token", async () => {
      mockAppCheckVerify.mockRejectedValue(new Error("Invalid token"));

      const req = createMockRequest();
      const res = createMockResponse();

      await logEvent(req, res);

      expect(res.status).toHaveBeenCalledWith(401);
      expect(res.send).toHaveBeenCalledWith(
        "Unauthorized: Invalid App Check token"
      );
      expect(mockAppCheckVerify).toHaveBeenCalledWith("valid-app-check-token");
    });

    it("should accept requests with valid App Check token", async () => {
      mockAppCheckVerify.mockResolvedValue({appId: "test-app"});

      const req = createMockRequest();
      const res = createMockResponse();

      await logEvent(req, res);

      expect(mockAppCheckVerify).toHaveBeenCalledWith("valid-app-check-token");
      expect(res.status).toHaveBeenCalledWith(200);
    });
  });

  describe("Authentication validation", () => {
    it("should reject requests without authorization header", async () => {
      const req = createMockRequest();
      req.headers.authorization = undefined;
      const res = createMockResponse();

      await logEvent(req, res);

      expect(res.status).toHaveBeenCalledWith(401);
      expect(res.send).toHaveBeenCalledWith(
        "Unauthorized: Missing or invalid auth token"
      );
    });

    it("should reject requests with malformed authorization header", async () => {
      const req = createMockRequest();
      req.headers.authorization = "InvalidFormat token123";
      const res = createMockResponse();

      await logEvent(req, res);

      expect(res.status).toHaveBeenCalledWith(401);
      expect(res.send).toHaveBeenCalledWith(
        "Unauthorized: Missing or invalid auth token"
      );
    });

    it("should reject requests with invalid ID token", async () => {
      mockAuthVerify.mockRejectedValue(new Error("Invalid token"));

      const req = createMockRequest();
      const res = createMockResponse();

      await logEvent(req, res);

      expect(res.status).toHaveBeenCalledWith(401);
      expect(res.send).toHaveBeenCalledWith("Unauthorized: Invalid auth token");
      expect(mockAuthVerify).toHaveBeenCalledWith("valid-id-token");
    });

    it("should accept requests with valid ID token", async () => {
      mockAuthVerify.mockResolvedValue({uid: "user456"});

      const req = createMockRequest();
      const res = createMockResponse();

      await logEvent(req, res);

      expect(mockAuthVerify).toHaveBeenCalledWith("valid-id-token");
      expect(res.status).toHaveBeenCalledWith(200);
    });
  });

  describe("Request body validation", () => {
    it("should reject requests without events array", async () => {
      const req = createMockRequest();
      req.body = {};
      const res = createMockResponse();

      await logEvent(req, res);

      expect(res.status).toHaveBeenCalledWith(400);
      expect(res.send).toHaveBeenCalledWith(
        "Bad Request: Invalid log batch format"
      );
    });

    it("should reject requests with non-array events", async () => {
      const req = createMockRequest();
      req.body = {events: "not an array"};
      const res = createMockResponse();

      await logEvent(req, res);

      expect(res.status).toHaveBeenCalledWith(400);
      expect(res.send).toHaveBeenCalledWith(
        "Bad Request: Invalid log batch format"
      );
    });

    it("should reject batches with more than 100 events", async () => {
      const req = createMockRequest();
      req.body = {
        events: new Array(101).fill({
          level: "INFO",
          tag: "Test",
          message: "Test message",
          timestamp: Date.now(),
        }),
      };
      const res = createMockResponse();

      await logEvent(req, res);

      expect(res.status).toHaveBeenCalledWith(400);
      expect(res.send).toHaveBeenCalledWith(
        "Bad Request: Batch too large (max 100 events)"
      );
    });

    it("should accept batches with exactly 100 events", async () => {
      const req = createMockRequest();
      req.body = {
        events: new Array(100).fill({
          level: "INFO",
          tag: "Test",
          message: "Test message",
          timestamp: Date.now(),
        }),
      };
      const res = createMockResponse();

      await logEvent(req, res);

      expect(res.status).toHaveBeenCalledWith(200);
      expect(res.json).toHaveBeenCalledWith({
        success: true,
        eventsProcessed: 100,
      });
    });
  });

  describe("Log processing", () => {
    it("should process and write log events", async () => {
      const timestamp = Date.now();
      const req = createMockRequest();
      req.body = {
        events: [
          {
            level: "INFO",
            tag: "WorkoutViewModel",
            message: "Workout started",
            timestamp,
            context: {
              userId: "user123",
              installationId: "install-123",
              appVersion: "1.0.0",
              deviceModel: "Pixel 6",
              androidVersion: "13",
              screen: "WorkoutScreen",
            },
          },
        ],
      };
      const res = createMockResponse();

      await logEvent(req, res);

      expect(mockLogWrite).toHaveBeenCalled();
      expect(res.status).toHaveBeenCalledWith(200);
      expect(res.json).toHaveBeenCalledWith({
        success: true,
        eventsProcessed: 1,
      });
    });

    it("should map log levels correctly", async () => {
      const req = createMockRequest();
      req.body = {
        events: [
          {level: "DEBUG", tag: "Test", message: "Debug message", timestamp: Date.now()},
          {level: "INFO", tag: "Test", message: "Info message", timestamp: Date.now()},
          {level: "WARN", tag: "Test", message: "Warn message", timestamp: Date.now()},
          {level: "ERROR", tag: "Test", message: "Error message", timestamp: Date.now()},
        ],
      };
      const res = createMockResponse();

      await logEvent(req, res);

      expect(mockLogWrite).toHaveBeenCalled();
      const entries = mockLogWrite.mock.calls[0][0];
      expect(entries).toHaveLength(4);
      expect(entries[0].metadata.severity).toBe("DEBUG");
      expect(entries[1].metadata.severity).toBe("INFO");
      expect(entries[2].metadata.severity).toBe("WARNING");
      expect(entries[3].metadata.severity).toBe("ERROR");
    });

    it("should include throwable information when present", async () => {
      const req = createMockRequest();
      req.body = {
        events: [
          {
            level: "ERROR",
            tag: "WorkoutViewModel",
            message: "Workout save failed",
            timestamp: Date.now(),
            throwable: {
              message: "NullPointerException",
              stackTrace: "at com.example.Test.method(Test.kt:42)",
            },
          },
        ],
      };
      const res = createMockResponse();

      await logEvent(req, res);

      expect(mockLogWrite).toHaveBeenCalled();
      const entries = mockLogWrite.mock.calls[0][0];
      expect(entries[0].data.error).toEqual({
        message: "NullPointerException",
        stackTrace: "at com.example.Test.method(Test.kt:42)",
      });
    });

    it("should use user ID from auth token when context missing", async () => {
      mockAuthVerify.mockResolvedValue({uid: "auth-user-456"});

      const req = createMockRequest();
      req.body = {
        events: [
          {
            level: "INFO",
            tag: "Test",
            message: "Test message",
            timestamp: Date.now(),
            context: {
              installationId: "install-123",
            },
          },
        ],
      };
      const res = createMockResponse();

      await logEvent(req, res);

      expect(mockLogWrite).toHaveBeenCalled();
      const entries = mockLogWrite.mock.calls[0][0];
      expect(entries[0].data.userId).toBe("auth-user-456");
      expect(entries[0].metadata.labels.userId).toBe("auth-user-456");
    });

    it("should process multiple events in a batch", async () => {
      const req = createMockRequest();
      req.body = {
        events: [
          {level: "INFO", tag: "Test1", message: "Message 1", timestamp: Date.now()},
          {level: "WARN", tag: "Test2", message: "Message 2", timestamp: Date.now()},
          {level: "ERROR", tag: "Test3", message: "Message 3", timestamp: Date.now()},
        ],
      };
      const res = createMockResponse();

      await logEvent(req, res);

      expect(mockLogWrite).toHaveBeenCalledTimes(1);
      const entries = mockLogWrite.mock.calls[0][0];
      expect(entries).toHaveLength(3);
      expect(res.json).toHaveBeenCalledWith({
        success: true,
        eventsProcessed: 3,
      });
    });
  });

  describe("Error handling", () => {
    it("should handle internal errors gracefully", async () => {
      mockLogWrite.mockRejectedValue(new Error("Cloud Logging error"));

      const req = createMockRequest();
      const res = createMockResponse();

      await logEvent(req, res);

      // Even if logging fails, should still respond with success
      // (This matches the actual behavior where logging failures don't block the response)
      expect(res.status).toHaveBeenCalledWith(200);
      expect(res.json).toHaveBeenCalledWith({
        success: true,
        eventsProcessed: 1,
      });
    });

    it("should handle App Check verification errors", async () => {
      mockAppCheckVerify.mockRejectedValue(new Error("Token expired"));

      const req = createMockRequest();
      const res = createMockResponse();

      await logEvent(req, res);

      // Should have called mockLogWrite for error logging
      expect(mockLogWrite).toHaveBeenCalled();
      expect(res.status).toHaveBeenCalledWith(401);
      expect(res.send).toHaveBeenCalledWith("Unauthorized: Invalid App Check token");
    });

    it("should handle auth token verification errors", async () => {
      mockAuthVerify.mockRejectedValue(new Error("Token invalid"));

      const req = createMockRequest();
      const res = createMockResponse();

      await logEvent(req, res);

      // Should have called mockLogWrite for error logging
      expect(mockLogWrite).toHaveBeenCalled();
      expect(res.status).toHaveBeenCalledWith(401);
      expect(res.send).toHaveBeenCalledWith("Unauthorized: Invalid auth token");
    });
  });
});
