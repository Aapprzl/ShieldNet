package com.shieldnet.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shieldnet.ui.theme.*
import com.shieldnet.viewmodel.VpnState

@Composable
fun ShieldToggleButton(
    vpnState: VpnState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isProtected = vpnState == VpnState.CONNECTED
    
    // Infinite transition for pulsing glow animation when connected
    val infiniteTransition = rememberInfiniteTransition(label = "ShieldPulse")
    
    val pulseScale1 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseScale1"
    )
    
    val pulseAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseAlpha1"
    )

    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseScale2"
    )
    
    val pulseAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseAlpha2"
    )

    val buttonColor by animateColorAsState(
        targetValue = when (vpnState) {
            VpnState.CONNECTED -> ShieldGreen
            VpnState.CONNECTING -> WarningAmber
            VpnState.DISCONNECTED -> CyanPrimary
        },
        animationSpec = tween(500),
        label = "ButtonColor"
    )

    val glowColor = when (vpnState) {
        VpnState.CONNECTED -> ShieldGreen
        VpnState.CONNECTING -> WarningAmber
        VpnState.DISCONNECTED -> CyanPrimary
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(240.dp)
    ) {
        // Pulsing background rings
        if (vpnState == VpnState.CONNECTED) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(pulseScale1)
                    .background(glowColor.copy(alpha = pulseAlpha1), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(pulseScale2)
                    .background(glowColor.copy(alpha = pulseAlpha2), CircleShape)
            )
        }

        // Main circular button
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = when (vpnState) {
                            VpnState.CONNECTED -> listOf(ShieldGreen, ShieldGreenDark)
                            VpnState.CONNECTING -> listOf(WarningAmber, Color(0xFFD48A00))
                            VpnState.DISCONNECTED -> listOf(CyanPrimary, CyanDark)
                        }
                    )
                )
                .clickable { onClick() }
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Shield Icon",
                    tint = DarkBackground,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = when (vpnState) {
                        VpnState.CONNECTED -> "ACTIVE"
                        VpnState.CONNECTING -> "STARTING"
                        VpnState.DISCONNECTED -> "START"
                    },
                    color = DarkBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
