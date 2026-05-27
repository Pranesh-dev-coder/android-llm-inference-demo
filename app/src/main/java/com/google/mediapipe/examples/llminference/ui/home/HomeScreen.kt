package com.google.mediapipe.examples.llminference.ui.home

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

data class HomeOption(
    val title: String,
    val icon: ImageVector,
    val route: String? = null
)

@Composable
fun HomeRoute(
    onStart: () -> Unit
) {
    val options = listOf(
        HomeOption("Medical Records", Icons.Default.Description),
        HomeOption("Book Appointment", Icons.Default.Event),
        HomeOption("Prescriptions", Icons.Default.Medication),
        HomeOption("Health Tips", Icons.Default.Lightbulb),
        HomeOption("AI Assistant", Icons.Default.Chat, "ai_assistant")
    )

    HomeScreen(
        options = options,
        onOptionClick = { option ->
            if (option.route == "ai_assistant") {
                onStart()
            }
        }
    )
}

@Composable
fun HomeScreen(
    options: List<HomeOption>,
    onOptionClick: (HomeOption) -> Unit
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome to HealthConnect",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp, top = 16.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(options) { option ->
                HomeCard(option = option) {
                    if (option.route == "ai_assistant") {
                        onOptionClick(option)
                    } else {
                        Toast.makeText(context, "${option.title} is coming soon!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}

@Composable
fun HomeCard(
    option: HomeOption,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = option.title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
