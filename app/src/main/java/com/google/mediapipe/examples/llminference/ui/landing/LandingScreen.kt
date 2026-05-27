package com.google.mediapipe.examples.llminference.ui.landing

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mediapipe.examples.llminference.R

@Composable
fun LandingRoute(
    onEnterHub: () -> Unit
) {
    LandingScreen(onEnterHub)
}

@Composable
fun LandingScreen(
    onEnterHub: () -> Unit
) {
    val primaryGreen = Color(0xFF1E5631) // Dark medical green from screenshot
    val accentGreen = Color(0xFF2E7D32)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Top Section with Logo and Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.45f)
                .background(
                    color = primaryGreen,
                    shape = RoundedCornerShape(bottomStart = 80.dp, bottomEnd = 80.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                // Main Logo (Medical Cross)
                Icon(
                    imageVector = Icons.Default.MedicalServices,
                    contentDescription = null,
                    modifier = Modifier.size(100.dp),
                    tint = Color(0xFF4DB6AC) // Light blue/green accent
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Apollo Clinical Assist",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "Offline Medical Intelligence Engine",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Bottom Content Section
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Enterprise Offline RAG Client",
                    style = MaterialTheme.typography.titleLarge,
                    color = accentGreen,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Designed for clinical environments with zero cloud connectivity. Patient details remain strictly local for absolute privacy.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Feature List
                FeatureItem(
                    icon = Icons.Default.Shield,
                    text = "HIPAA Compliant (No Data Leaves Device)",
                    iconTint = Color(0xFFF57C00) // Orange
                )
                
                FeatureItem(
                    icon = Icons.Default.Description,
                    text = "Vector Search for Custom Protocols & CSVs",
                    iconTint = Color.Gray
                )
                
                FeatureItem(
                    icon = Icons.Default.FlashOn,
                    text = "Fast, Sub-Second Local Inference",
                    iconTint = Color.Yellow
                )
            }

            // Bottom Button
            Button(
                onClick = onEnterHub,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryGreen)
            ) {
                Text(
                    text = "ENTER CLINICAL HUB",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun FeatureItem(icon: ImageVector, text: String, iconTint: Color) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = iconTint
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.DarkGray
        )
    }
}
