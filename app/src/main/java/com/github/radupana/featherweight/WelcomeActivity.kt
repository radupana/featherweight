package com.github.radupana.featherweight

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.radupana.featherweight.di.ServiceLocator
import com.github.radupana.featherweight.ui.theme.FeatherweightTheme

class WelcomeActivity : ComponentActivity() {
    private val authManager by lazy { ServiceLocator.provideAuthenticationManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (!authManager.isFirstLaunch()) {
            navigateToMain()
            return
        }

        setContent {
            FeatherweightTheme {
                WelcomeScreen(
                    onSignInClick = { navigateToSignIn() },
                    onContinueWithoutAccount = { continueWithoutAccount() },
                )
            }
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun navigateToSignIn() {
        startActivity(Intent(this, SignInActivity::class.java))
        finish()
    }

    private fun continueWithoutAccount() {
        authManager.setFirstLaunchComplete()
        authManager.setUnauthenticatedWarningShown()
        navigateToMain()
    }
}

@Composable
fun WelcomeScreen(
    onSignInClick: () -> Unit,
    onContinueWithoutAccount: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Welcome to Featherweight",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Track your workouts and progress",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onSignInClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Sign In or Create Account")
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onContinueWithoutAccount,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Continue Without Account")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "⚠️ Warning: Without an account, your data will be lost if you uninstall the app",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}
