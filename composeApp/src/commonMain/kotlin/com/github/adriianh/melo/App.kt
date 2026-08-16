package com.github.adriianh.melo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.github.adriianh.melo.ui.AdaptiveScaffold
import com.github.adriianh.melo.ui.home.HomeScreen
import com.github.adriianh.melo.ui.library.LibraryScreen
import com.github.adriianh.melo.ui.login.LoginDialog
import com.github.adriianh.melo.ui.login.LoginViewModel
import com.github.adriianh.melo.ui.search.SearchScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
expect fun InitImageLoader()

@Composable
fun App() {
    InitImageLoader()

    MaterialTheme {
        var selectedTab by remember { mutableStateOf("Search") }
        var showLoginDialog by remember { mutableStateOf(false) }
        val loginViewModel: LoginViewModel = koinViewModel()

        AdaptiveScaffold(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            onLoginClick = { showLoginDialog = true }
        ) { tab, paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (tab) {
                    "Home" -> HomeScreen()
                    "Search" -> SearchScreen()
                    "Library" -> LibraryScreen(
                        onLoginClick = { showLoginDialog = true }
                    )
                }
            }
        }

        if (showLoginDialog) {
            LoginDialog(
                viewModel = loginViewModel,
                onDismiss = { showLoginDialog = false }
            )
        }
    }
}
