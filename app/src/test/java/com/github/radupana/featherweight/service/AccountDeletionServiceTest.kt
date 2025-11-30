package com.github.radupana.featherweight.service

import android.text.TextUtils
import android.util.Log
import com.github.radupana.featherweight.data.ExerciseLogDao
import com.github.radupana.featherweight.data.ExerciseSwapHistoryDao
import com.github.radupana.featherweight.data.FeatherweightDatabase
import com.github.radupana.featherweight.data.GlobalExerciseProgressDao
import com.github.radupana.featherweight.data.ParseRequestDao
import com.github.radupana.featherweight.data.PersonalRecordDao
import com.github.radupana.featherweight.data.ProgrammeExerciseTrackingDao
import com.github.radupana.featherweight.data.SetLogDao
import com.github.radupana.featherweight.data.WorkoutDao
import com.github.radupana.featherweight.data.profile.ExerciseMaxTrackingDao
import com.github.radupana.featherweight.data.programme.ProgrammeDao
import com.github.radupana.featherweight.manager.AuthenticationManager
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.FirebaseUserMetadata
import com.google.firebase.auth.GoogleAuthProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AccountDeletionServiceTest {
    private lateinit var database: FeatherweightDatabase
    private lateinit var authManager: AuthenticationManager
    private lateinit var firebaseAuth: FirebaseAuthService
    private lateinit var service: AccountDeletionService

    private lateinit var workoutDao: WorkoutDao
    private lateinit var exerciseLogDao: ExerciseLogDao
    private lateinit var setLogDao: SetLogDao
    private lateinit var programmeDao: ProgrammeDao
    private lateinit var oneRMDao: ExerciseMaxTrackingDao
    private lateinit var personalRecordDao: PersonalRecordDao
    private lateinit var exerciseSwapHistoryDao: ExerciseSwapHistoryDao
    private lateinit var programmeExerciseTrackingDao: ProgrammeExerciseTrackingDao
    private lateinit var globalExerciseProgressDao: GlobalExerciseProgressDao
    private lateinit var parseRequestDao: ParseRequestDao

    @Before
    fun setup() {
        // Mock Android Log
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0

        // Mock TextUtils
        mockkStatic(TextUtils::class)
        every { TextUtils.isEmpty(any()) } answers {
            val str = arg<CharSequence?>(0)
            str == null || str.isEmpty()
        }

        database = mockk()
        authManager = mockk()
        firebaseAuth = mockk()

        // Mock all DAOs
        workoutDao = mockk(relaxed = true)
        exerciseLogDao = mockk(relaxed = true)
        setLogDao = mockk(relaxed = true)
        programmeDao = mockk(relaxed = true)
        oneRMDao = mockk(relaxed = true)
        personalRecordDao = mockk(relaxed = true)
        exerciseSwapHistoryDao = mockk(relaxed = true)
        programmeExerciseTrackingDao = mockk(relaxed = true)
        globalExerciseProgressDao = mockk(relaxed = true)
        parseRequestDao = mockk(relaxed = true)

        every { database.workoutDao() } returns workoutDao
        every { database.exerciseLogDao() } returns exerciseLogDao
        every { database.setLogDao() } returns setLogDao
        every { database.programmeDao() } returns programmeDao
        every { database.exerciseMaxTrackingDao() } returns oneRMDao
        every { database.personalRecordDao() } returns personalRecordDao
        every { database.exerciseSwapHistoryDao() } returns exerciseSwapHistoryDao
        every { database.programmeExerciseTrackingDao() } returns programmeExerciseTrackingDao
        every { database.globalExerciseProgressDao() } returns globalExerciseProgressDao
        every { database.parseRequestDao() } returns parseRequestDao
        every { database.exerciseDao() } returns mockk(relaxed = true)
        every { database.userExerciseUsageDao() } returns mockk(relaxed = true)

        service = AccountDeletionService(database, authManager, firebaseAuth)
    }

    @Test
    fun `deleteAccount returns error when no user is logged in`() =
        runBlocking {
            // Given
            every { authManager.getCurrentUserId() } returns null

            // When
            val result = service.deleteAccount()

            // Then
            assertTrue(result is AccountDeletionService.DeletionResult.Error)
            assertEquals("No user logged in", result.message)
        }

    @Test
    fun `deleteAccount requires reauthentication when Firebase throws exception`() =
        runBlocking {
            // Given
            val userId = "test-user-id"
            val firebaseUser = mockk<FirebaseUser>(relaxed = true)
            val firebaseAuthInstance = mockk<FirebaseAuth>(relaxed = true)

            every { authManager.getCurrentUserId() } returns userId
            every { firebaseAuthInstance.currentUser } returns firebaseUser
            every { firebaseAuth.getAuthProvider() } returns "password"

            mockkStatic(FirebaseAuth::class)
            every { FirebaseAuth.getInstance() } returns firebaseAuthInstance

            mockkStatic("kotlinx.coroutines.tasks.TasksKt")
            coEvery { firebaseUser.delete().await() } throws FirebaseAuthRecentLoginRequiredException("Recent login required", "ERROR_REQUIRES_RECENT_LOGIN")

            // When
            val result = service.deleteAccount()

            // Then
            assertTrue(result is AccountDeletionService.DeletionResult.RequiresReauthentication)
            assertEquals("password", result.authProvider)
        }

    @Test
    fun `deleteAccount successfully deletes all user data`() =
        runBlocking {
            // Given
            val userId = "test-user-id"
            val firebaseUser = mockk<FirebaseUser>(relaxed = true)
            val firebaseAuthInstance = mockk<FirebaseAuth>(relaxed = true)
            val userMetadata = mockk<FirebaseUserMetadata>(relaxed = true)

            every { authManager.getCurrentUserId() } returns userId
            every { authManager.clearUserData() } returns Unit
            every { firebaseAuthInstance.currentUser } returns firebaseUser
            every { firebaseAuth.getAuthProvider() } returns "password"

            // Mock the metadata to indicate recent sign-in
            every { firebaseUser.metadata } returns userMetadata
            every { userMetadata.lastSignInTimestamp } returns System.currentTimeMillis()

            mockkStatic(FirebaseAuth::class)
            every { FirebaseAuth.getInstance() } returns firebaseAuthInstance

            mockkStatic("kotlinx.coroutines.tasks.TasksKt")
            coEvery { firebaseUser.delete().await() } returns mockk()

            // When
            val result = service.deleteAccount()

            // Then
            assertTrue(result is AccountDeletionService.DeletionResult.Success)

            // Verify all data deletion methods were called
            coVerify { setLogDao.deleteAllForUser(userId) }
            coVerify { exerciseLogDao.deleteAllForUser(userId) }
            coVerify { workoutDao.deleteAllForUser(userId) }
            coVerify { programmeDao.deleteAllForUser(userId) }
            coVerify { oneRMDao.deleteAllForUser(userId) }
            coVerify { personalRecordDao.deleteAllForUser(userId) }
            coVerify { exerciseSwapHistoryDao.deleteAllForUser(userId) }
            coVerify { programmeExerciseTrackingDao.deleteAllForUser(userId) }
            coVerify { globalExerciseProgressDao.deleteAllForUser(userId) }
            coVerify { parseRequestDao.deleteAllForUser(userId) }
            verify { authManager.clearUserData() }
        }

    @Test
    fun `createEmailCredential creates correct credential`() {
        // Given
        val email = "test@example.com"
        val password = "password123"

        mockkStatic(EmailAuthProvider::class)
        val expectedCredential = mockk<AuthCredential>()
        every { EmailAuthProvider.getCredential(email, password) } returns expectedCredential

        // When
        val credential = service.createEmailCredential(email, password)

        // Then
        assertEquals(expectedCredential, credential)
    }

    @Test
    fun `createGoogleCredential creates correct credential`() {
        // Given
        val idToken = "google-id-token"

        mockkStatic(GoogleAuthProvider::class)
        val expectedCredential = mockk<AuthCredential>()
        every { GoogleAuthProvider.getCredential(idToken, null) } returns expectedCredential

        // When
        val credential = service.createGoogleCredential(idToken)

        // Then
        assertEquals(expectedCredential, credential)
    }

    @Test
    fun `deleteAccountWithReauthentication succeeds after reauthentication`() =
        runBlocking {
            // Given
            val userId = "test-user-id"
            val firebaseUser = mockk<FirebaseUser>(relaxed = true)
            val firebaseAuthInstance = mockk<FirebaseAuth>(relaxed = true)
            val credential = mockk<AuthCredential>()
            val userMetadata = mockk<FirebaseUserMetadata>(relaxed = true)

            every { authManager.getCurrentUserId() } returns userId
            every { authManager.clearUserData() } returns Unit
            every { firebaseAuthInstance.currentUser } returns firebaseUser
            every { firebaseAuth.getAuthProvider() } returns "password"

            // Mock the metadata to indicate recent sign-in (after reauthentication)
            every { firebaseUser.metadata } returns userMetadata
            every { userMetadata.lastSignInTimestamp } returns System.currentTimeMillis()

            mockkStatic(FirebaseAuth::class)
            every { FirebaseAuth.getInstance() } returns firebaseAuthInstance

            mockkStatic("kotlinx.coroutines.tasks.TasksKt")
            coEvery { firebaseUser.reauthenticate(credential).await() } returns mockk()
            coEvery { firebaseUser.delete().await() } returns mockk()

            // When
            val result = service.deleteAccountWithReauthentication(credential)

            // Then
            assertTrue(result is AccountDeletionService.DeletionResult.Success)
            coVerify { firebaseUser.reauthenticate(credential).await() }
        }

    @Test
    fun `deleteAccountWithReauthentication returns error when user not logged in`() =
        runBlocking {
            // Given
            val firebaseAuthInstance = mockk<FirebaseAuth>(relaxed = true)
            val credential = mockk<AuthCredential>()

            every { firebaseAuthInstance.currentUser } returns null

            mockkStatic(FirebaseAuth::class)
            every { FirebaseAuth.getInstance() } returns firebaseAuthInstance

            // When
            val result = service.deleteAccountWithReauthentication(credential)

            // Then
            assertTrue(result is AccountDeletionService.DeletionResult.Error)
            assertEquals("No user logged in", result.message)
        }
}
