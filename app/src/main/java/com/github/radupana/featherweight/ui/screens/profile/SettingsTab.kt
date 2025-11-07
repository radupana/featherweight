package com.github.radupana.featherweight.ui.screens.profile

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.github.radupana.featherweight.model.WeightUnit
import com.github.radupana.featherweight.ui.components.AccountSection
import com.github.radupana.featherweight.ui.components.SyncSection
import com.github.radupana.featherweight.viewmodel.AccountInfo
import com.github.radupana.featherweight.viewmodel.SyncUiState

@Composable
fun SettingsTab(
    currentWeightUnit: WeightUnit,
    onWeightUnitSelected: (WeightUnit) -> Unit,
    accountInfo: AccountInfo?,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    onSignIn: () -> Unit = {},
    onSendVerificationEmail: () -> Unit,
    onChangePassword: (String, String) -> Unit,
    onDeleteAccount: () -> Unit,
    syncState: SyncUiState,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AccountSection(
            accountInfo = accountInfo,
            onSignOut = onSignOut,
            onSignIn = onSignIn,
            onSendVerificationEmail = onSendVerificationEmail,
            onChangePassword = onChangePassword,
            onDeleteAccount = onDeleteAccount,
        )

        SyncSection(
            syncState = syncState,
        )

        WeightUnitSelector(
            currentUnit = currentWeightUnit,
            onUnitSelected = onWeightUnitSelected,
        )

        LegalLinksSection()
    }
}

@Composable
private fun WeightUnitSelector(
    currentUnit: WeightUnit,
    onUnitSelected: (WeightUnit) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Weight Unit",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Column(Modifier.selectableGroup()) {
                WeightUnit.entries.forEach { unit ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (unit == currentUnit),
                                onClick = { onUnitSelected(unit) },
                                role = Role.RadioButton,
                            ).padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = (unit == currentUnit),
                            onClick = null,
                        )
                        Text(
                            text =
                                when (unit) {
                                    WeightUnit.KG -> "Kilograms (kg)"
                                    WeightUnit.LBS -> "Pounds (lbs)"
                                },
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegalLinksSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Legal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            LegalLinkItem(
                title = "Privacy Policy",
                onClick = {
                    val intent =
                        Intent(
                            Intent.ACTION_VIEW,
                            "https://featherweight-app.web.app/privacy.html".toUri(),
                        )
                    context.startActivity(intent)
                },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            LegalLinkItem(
                title = "Terms of Service",
                onClick = {
                    val intent =
                        Intent(
                            Intent.ACTION_VIEW,
                            "https://featherweight-app.web.app/terms.html".toUri(),
                        )
                    context.startActivity(intent)
                },
            )
        }
    }
}

@Composable
private fun LegalLinkItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
