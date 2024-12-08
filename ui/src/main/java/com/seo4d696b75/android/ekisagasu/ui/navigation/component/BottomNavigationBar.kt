package com.seo4d696b75.android.ekisagasu.ui.navigation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessStarted
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.seo4d696b75.android.ekisagasu.ui.navigation.NavigationTab
import com.seo4d696b75.android.ekisagasu.ui.navigation.findTab
import com.seo4d696b75.android.ekisagasu.ui.navigation.toRoute

@Composable
fun BottomNavigationBar(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .background(NavigationBarDefaults.containerColor)
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)
            ),
    ) {
        val backStackEntry by navController.currentBackStackEntryFlow.collectAsStateWithLifecycle(null)
        val currentTab by remember {
            derivedStateOf { backStackEntry?.toRoute()?.findTab() }
        }
        NavigationTab.entries.forEach { tab ->
            val selected = tab == currentTab
            NavigationBarItem(
                selected = selected,
                icon = {
                    Icon(
                        imageVector = tab.icon(selected),
                        contentDescription = tab.label(),
                    )
                },
                label = {
                    Text(
                        text = tab.label(),
                        fontWeight = if (selected) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Normal
                        },
                    )
                },
                onClick = dropUnlessStarted {
                    navController.navigate(tab) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }
    }
}
