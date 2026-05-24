package com.shieldnet.ui.screens

import android.app.Activity
import android.content.Context
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shieldnet.ui.components.ShieldToggleButton
import com.shieldnet.ui.components.StatCard
import com.shieldnet.ui.theme.*
import com.shieldnet.viewmodel.HomeViewModel
import com.shieldnet.viewmodel.VpnState

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val vpnState by viewModel.vpnState.collectAsState()
    val blockedToday by viewModel.blockedToday.collectAsState()
    val totalBlocked by viewModel.totalBlocked.collectAsState()
    val blocklistSize by viewModel.blocklistSize.collectAsState()
    val context = LocalContext.current

    // VPN Permission Launcher
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.startVpn(context)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "ShieldNet",
            style = MaterialTheme.typography.headlineMedium.copy(
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "Local VPN Ad Blocker",
            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Big animated toggle button
        ShieldToggleButton(
            vpnState = vpnState,
            onClick = {
                if (vpnState == VpnState.CONNECTED) {
                    viewModel.stopVpn(context)
                } else {
                    val intent = VpnService.prepare(context)
                    if (intent != null) {
                        vpnPermissionLauncher.launch(intent)
                    } else {
                        viewModel.startVpn(context)
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(30.dp))

        // Connection status card/text
        Card(
            colors = CardDefaults.cardColors(
                containerColor = DarkSurface.copy(alpha = 0.5f)
            ),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
            ),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = when (vpnState) {
                                VpnState.CONNECTED -> ShieldGreen
                                VpnState.CONNECTING -> WarningAmber
                                VpnState.DISCONNECTED -> WarningRed
                            },
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = when (vpnState) {
                        VpnState.CONNECTED -> "Protected"
                        VpnState.CONNECTING -> "Connecting..."
                        VpnState.DISCONNECTED -> "Unprotected"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = when (vpnState) {
                            VpnState.CONNECTED -> ShieldGreen
                            VpnState.CONNECTING -> WarningAmber
                            VpnState.DISCONNECTED -> WarningRed
                        },
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Stats grid
        Text(
            text = "STATISTICS",
            style = MaterialTheme.typography.titleSmall.copy(
                color = TextTertiary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            ),
            modifier = Modifier
                .align(Alignment.Start)
                .padding(start = 8.dp, bottom = 12.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            StatCard(
                label = "Blocked Today",
                value = blockedToday,
                icon = Icons.Default.Block,
                iconColor = WarningRed
            )
            
            StatCard(
                label = "Total Ads Blocked",
                value = totalBlocked,
                icon = Icons.Default.Security,
                iconColor = ShieldGreen
            )
            
            StatCard(
                label = "Active Rules",
                value = blocklistSize,
                icon = Icons.Default.ListAlt,
                iconColor = CyanPrimary
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}
