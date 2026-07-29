package com.github.adriianh.melo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.github.adriianh.melo.ui.AdaptiveScaffold
import com.github.adriianh.melo.ui.search.SearchScreen

@Composable
expect fun InitImageLoader()

@Composable
fun App() {
    InitImageLoader()

    MaterialTheme {
        var selectedTab by remember { mutableStateOf("Search") }

        AdaptiveScaffold(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it }
        ) { tab, paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (tab) {
                    "Home" -> Text("Home Screen", modifier = Modifier.align(Alignment.Center))
                    "Search" -> SearchScreen()
                    "Library" -> Text("Library Screen", modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}
