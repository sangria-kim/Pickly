package com.cola.pickly.presentation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cola.pickly.R
import com.cola.pickly.core.model.WeekId
import com.cola.pickly.feature.archive.ArchiveScreen
import com.cola.pickly.feature.organize.OrganizeScreen
import com.cola.pickly.feature.organize.OrganizeViewModel
import com.cola.pickly.feature.organize.components.BulkActionBar
import com.cola.pickly.feature.settings.SettingsScreen
import com.cola.pickly.core.model.PhotoSelectionState
import com.cola.pickly.feature.weekly.WeeklyListScreen
import com.cola.pickly.feature.weekly.WeeklyListViewModel
import com.cola.pickly.core.ui.theme.BackgroundWhite
import com.cola.pickly.core.ui.theme.BottomNavSelected
import com.cola.pickly.core.ui.theme.BottomNavUnselected

sealed class MainTab(
    val route: String,
    @StringRes val labelResId: Int,
    val icon: ImageVector
) {
    object Organize : MainTab("organize", R.string.nav_organize, Icons.Default.CheckCircle)
    object Archive : MainTab("archive", R.string.archive_title, Icons.Default.DateRange)
    object Settings : MainTab("settings", R.string.nav_settings, Icons.Default.Settings)

    companion object {
        val tabs = listOf(Organize, Archive, Settings)
    }
}

/**
 * Bottom Navigation Bar Composable
 * Normal Mode에서 Bottom Area에 표시되는 네비게이션 바
 */
@Composable
fun PicklyBottomNavigation(
    navController: NavHostController,
    tabs: List<MainTab> = MainTab.tabs
) {
    NavigationBar(
        modifier = Modifier.height(104.dp),
        containerColor = BackgroundWhite,
        contentColor = BottomNavUnselected
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        tabs.forEach { tab ->
            val isSelected = currentRoute == tab.route
            NavigationBarItem(
                icon = { Icon(tab.icon, contentDescription = stringResource(tab.labelResId)) },
                label = { Text(stringResource(tab.labelResId)) },
                selected = isSelected,
                onClick = {
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = BottomNavSelected,
                    selectedTextColor = BottomNavSelected,
                    unselectedIconColor = BottomNavUnselected,
                    unselectedTextColor = BottomNavUnselected,
                    indicatorColor = Color.Transparent // 선택 시 배경색(Indicator) 제거
                )
            )
        }
    }
}

@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    weeklyListViewModel: WeeklyListViewModel,
    onNavigateToDetail: (WeekId) -> Unit,
    onNavigateToFolderSelect: () -> Unit,
    onNavigateToPhotoDetail: (String, Long, Map<Long, PhotoSelectionState>) -> Unit,
    selectedFolder: Pair<String, String>? = null,
    selectionUpdates: Map<Long, PhotoSelectionState>? = null
) {
    val navController = rememberNavController()
    
    // OrganizeViewModel을 한 번만 생성하여 OrganizeScreen과 ArchiveScreen이 같은 인스턴스를 공유하도록 함
    val organizeViewModel: OrganizeViewModel = hiltViewModel()
    
    // Multi Select Mode 상태 관리
    var isMultiSelectMode by remember { mutableStateOf(false) }
    
    // Bulk Action 콜백 상태 관리
    var onShareClick: (() -> Unit)? by remember { mutableStateOf(null) }
    var onMoveClick: (() -> Unit)? by remember { mutableStateOf(null) }
    var onCopyClick: (() -> Unit)? by remember { mutableStateOf(null) }
    var onDeleteClick: (() -> Unit)? by remember { mutableStateOf(null) }

    Scaffold(
        containerColor = BackgroundWhite, // 전체 배경색 흰색 적용
        // Bottom Area: Normal Mode일 때 Bottom Navigation Bar, Multi Select Mode일 때 Bulk Action Bar
        bottomBar = {
            if (isMultiSelectMode) {
                // Multi Select Mode: Bulk Action Bar 표시
                BulkActionBar(
                    onShareClick = { onShareClick?.invoke() },
                    onMoveClick = { onMoveClick?.invoke() },
                    onCopyClick = { onCopyClick?.invoke() },
                    onDeleteClick = { onDeleteClick?.invoke() }
                )
            } else {
                // Normal Mode: Bottom Navigation Bar 표시
                PicklyBottomNavigation(navController = navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MainTab.Organize.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(MainTab.Organize.route) {
                OrganizeScreen(
                    viewModel = organizeViewModel,
                    onNavigateToPhotoDetail = onNavigateToPhotoDetail,
                    selectedFolder = selectedFolder,
                    selectionUpdates = selectionUpdates,
                    onMultiSelectModeChanged = { isMultiSelect ->
                        isMultiSelectMode = isMultiSelect
                    },
                    onShareClick = { action ->
                        onShareClick = action
                    },
                    onMoveClick = { action ->
                        onMoveClick = action
                    },
                    onCopyClick = { action ->
                        onCopyClick = action
                    },
                    onDeleteClick = { action ->
                        onDeleteClick = action
                    }
                )
            }
            composable(MainTab.Archive.route) {
                val globalSelectionMap by organizeViewModel.globalSelectionMap.collectAsStateWithLifecycle()
                ArchiveScreen(
                    globalSelectionMap = globalSelectionMap,
                    onNavigateToPhotoDetail = onNavigateToPhotoDetail,
                    onNavigateToOrganize = {
                        // Tab 1 (정리하기)로 이동
                        navController.navigate(MainTab.Organize.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(MainTab.Settings.route) {
                SettingsScreen()
            }
        }
    }
}

@Composable
fun PlaceholderScreen(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = text)
    }
}
