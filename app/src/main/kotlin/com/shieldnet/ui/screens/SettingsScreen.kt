package com.shieldnet.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shieldnet.ui.components.GlassCard
import com.shieldnet.ui.theme.*
import com.shieldnet.viewmodel.SettingsViewModel
import com.shieldnet.viewmodel.LogViewModel

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    logViewModel: LogViewModel,
    onBlocklistUpdated: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val autoStart by settingsViewModel.autoStart.collectAsState()
    val dnsServer by settingsViewModel.dnsServer.collectAsState()
    val saveLogs by settingsViewModel.saveLogs.collectAsState()
    val logRetentionDays by settingsViewModel.logRetentionDays.collectAsState()
    val blocklistLastUpdated by settingsViewModel.blocklistLastUpdated.collectAsState()
    val isUpdatingBlocklist by settingsViewModel.isUpdatingBlocklist.collectAsState()
    val updateResult by settingsViewModel.updateResult.collectAsState()

    var showDnsDialog by remember { mutableStateOf(false) }
    var showRetentionDialog by remember { mutableStateOf(false) }

    // Toast alert on blocklist update complete
    LaunchedEffect(updateResult) {
        updateResult?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            settingsViewModel.clearUpdateResult()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineSmall.copy(
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // SECTION: General
        SettingsHeader("GENERAL")
        
        SettingsToggleRow(
            title = "Auto-start on boot",
            subtitle = "Start protection when device boots up",
            icon = Icons.Default.PowerSettingsNew,
            checked = autoStart,
            onCheckedChange = { settingsViewModel.setAutoStart(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION: DNS & Rules
        SettingsHeader("DNS & PROTECTION")

        SettingsClickableRow(
            title = "DNS Upstream Server",
            subtitle = when (dnsServer) {
                "1.1.1.1" -> "Cloudflare (1.1.1.1)"
                "8.8.8.8" -> "Google (8.8.8.8)"
                "9.9.9.9" -> "Quad9 (9.9.9.9)"
                else -> dnsServer
            },
            icon = Icons.Default.Dns,
            onClick = { showDnsDialog = true }
        )

        Spacer(modifier = Modifier.height(12.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Blocklist Database",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Last updated: $blocklistLastUpdated",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }

                if (isUpdatingBlocklist) {
                    CircularProgressIndicator(
                        color = CyanPrimary,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Button(
                        onClick = { 
                            settingsViewModel.updateBlocklistNow { success, _ ->
                                if (success) onBlocklistUpdated()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Update", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION: Logs & Privacy
        SettingsHeader("LOGS & PRIVACY")

        SettingsToggleRow(
            title = "Intercept History",
            subtitle = "Record blocked advertisement domains",
            icon = Icons.Default.History,
            checked = saveLogs,
            onCheckedChange = { settingsViewModel.setSaveLogs(it) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        SettingsClickableRow(
            title = "Auto-delete History",
            subtitle = when (logRetentionDays) {
                0 -> "Never"
                3 -> "After 3 days"
                7 -> "After 7 days"
                30 -> "After 30 days"
                else -> "$logRetentionDays days"
            },
            icon = Icons.Default.AutoDelete,
            enabled = saveLogs,
            onClick = { showRetentionDialog = true }
        )

        Spacer(modifier = Modifier.height(12.dp))

        SettingsClickableRow(
            title = "Clear Interception Logs",
            subtitle = "Instantly clear database entries",
            icon = Icons.Default.DeleteSweep,
            onClick = {
                logViewModel.clearAllLogs()
                Toast.makeText(context, "Interception logs cleared!", Toast.LENGTH_SHORT).show()
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION: About
        SettingsHeader("ABOUT")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Info",
                    tint = TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "ShieldNet v1.0.0",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Local Ad Interception Engine",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }

    // Upstream DNS Selection Dialog
    if (showDnsDialog) {
        val dnsOptions = listOf(
            "1.1.1.1" to "Cloudflare (1.1.1.1)",
            "8.8.8.8" to "Google (8.8.8.8)",
            "9.9.9.9" to "Quad9 (9.9.9.9)"
        )
        AlertDialog(
            onDismissRequest = { showDnsDialog = false },
            title = { Text("Select Upstream DNS", color = TextPrimary) },
            containerColor = DarkSurface,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    dnsOptions.forEach { (ip, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    settingsViewModel.setDnsServer(ip)
                                    showDnsDialog = false
                                }
                                .padding(vertical = 12.dp)
                        ) {
                            RadioButton(
                                selected = dnsServer == ip,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = CyanPrimary)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(label, color = TextPrimary, fontSize = 16.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDnsDialog = false }) {
                    Text("Cancel", color = CyanPrimary)
                }
            }
        )
    }

    // Log Retention Dialog
    if (showRetentionDialog) {
        val retentionOptions = listOf(
            0 to "Never",
            3 to "After 3 days",
            7 to "After 7 days",
            30 to "After 30 days"
        )
        AlertDialog(
            onDismissRequest = { showRetentionDialog = false },
            title = { Text("Select Log Retention", color = TextPrimary) },
            containerColor = DarkSurface,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    retentionOptions.forEach { (days, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    settingsViewModel.setLogRetentionDays(days)
                                    // Trigger cleanup if needed
                                    if (days > 0) logViewModel.deleteOlderThan(days)
                                    showRetentionDialog = false
                                }
                                .padding(vertical = 12.dp)
                        ) {
                            RadioButton(
                                selected = logRetentionDays == days,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = CyanPrimary)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(label, color = TextPrimary, fontSize = 16.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRetentionDialog = false }) {
                    Text("Cancel", color = CyanPrimary)
                }
            }
        )
    }
}

@Composable
fun SettingsHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(
            color = TextTertiary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        ),
        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp, top = 8.dp)
    )
}

@Composable
fun SettingsToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = CyanPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
            
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = DarkBackground,
                    checkedTrackColor = CyanPrimary,
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = DarkSurface
                )
            )
        }
    }
}

@Composable
fun SettingsClickableRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (enabled) CyanPrimary else TextTertiary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = title,
                        color = if (enabled) TextPrimary else TextTertiary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        color = if (enabled) TextSecondary else TextTertiary,
                        fontSize = 13.sp
                    )
                }
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = if (enabled) TextSecondary else TextTertiary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
