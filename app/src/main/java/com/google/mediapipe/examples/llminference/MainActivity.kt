package com.google.mediapipe.examples.llminference

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.mediapipe.examples.llminference.ui.chat.ChatRoute
import com.google.mediapipe.examples.llminference.ui.home.HomeRoute
import com.google.mediapipe.examples.llminference.ui.landing.LandingRoute
import com.google.mediapipe.examples.llminference.ui.loading.LoadingRoute
import com.google.mediapipe.examples.llminference.ui.selection.SelectionRoute
import com.google.mediapipe.examples.llminference.ui.theme.LLMInferenceTheme

const val LANDING_SCREEN = "landing_screen"
const val START_SCREEN = "start_screen"
const val LOAD_SCREEN = "load_screen"
const val CHAT_SCREEN = "chat_screen"
const val HOME_SCREEN = "home_screen"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize PDFBox for PDF parsing
        com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(applicationContext)

        setContent {
            LLMInferenceTheme {
                val navController = rememberNavController()
                val startDestination = intent.getStringExtra("NAVIGATE_TO") ?: LANDING_SCREEN
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    topBar = {
                        // Show AppBar only for non-landing screens
                        if (currentRoute != LANDING_SCREEN) {
                            AppBar(
                                showBackButton = currentRoute != HOME_SCREEN,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(LANDING_SCREEN) {
                            LandingRoute(onEnterHub = {
                                navController.navigate(HOME_SCREEN)
                            })
                        }

                        composable(HOME_SCREEN) {
                            HomeRoute(onStart = {
                                navController.navigate(START_SCREEN)
                            })
                        }

                        composable(START_SCREEN) {
                            SelectionRoute(
                                onModelSelected = {
                                    navController.navigate(LOAD_SCREEN) {
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }

                        composable(LOAD_SCREEN) {
                            LoadingRoute(
                                onModelLoaded = {
                                    navController.navigate(CHAT_SCREEN) {
                                        popUpTo(LOAD_SCREEN) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                },
                                onGoBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable(CHAT_SCREEN) {
                            ChatRoute(
                                onClose = {
                                    navController.navigate(HOME_SCREEN) {
                                        popUpTo(HOME_SCREEN) { inclusive = false }
                                        launchSingleTop = true
                                    }
                                })
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AppBar(
        showBackButton: Boolean = false,
        onBack: () -> Unit = {}
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
            )
            Box(
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Text(
                    text = stringResource(R.string.disclaimer),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
            }
        }
    }
}
