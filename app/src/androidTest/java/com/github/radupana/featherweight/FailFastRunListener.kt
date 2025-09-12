package com.github.radupana.featherweight

import android.util.Log
import org.junit.runner.Description
import org.junit.runner.Result
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunListener

@Suppress("unused") // Used via reflection in CI (see build.yml)
class FailFastRunListener : RunListener() {
    companion object {
        private const val TAG = "FailFastRunListener"
        private var failureCount = 0
        private const val MAX_FAILURES = 1 // Stop after first failure
    }

    override fun testFailure(failure: Failure) {
        super.testFailure(failure)
        failureCount++
        Log.e(TAG, "Test failed ($failureCount/$MAX_FAILURES): ${failure.description}")

        if (failureCount >= MAX_FAILURES) {
            Log.e(TAG, "Maximum failures reached. Stopping test execution.")
            // This will cause the test run to stop
            throw RuntimeException("Stopping test execution after $MAX_FAILURES failure(s)")
        }
    }

    override fun testRunStarted(description: Description) {
        super.testRunStarted(description)
        failureCount = 0
        Log.i(TAG, "Test run started with fail-fast mode (max failures: $MAX_FAILURES)")
    }

    override fun testRunFinished(result: Result) {
        super.testRunFinished(result)
        Log.i(TAG, "Test run finished. Total failures: $failureCount")
    }
}
