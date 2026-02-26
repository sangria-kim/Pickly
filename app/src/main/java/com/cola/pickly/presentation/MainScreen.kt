package com.cola.pickly.presentation

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cola.pickly.R
import com.cola.pickly.core.ui.R as CoreUiR
import com.cola.pickly.feature.archive.ArchiveScreen
import com.cola.pickly.feature.archive.ArchiveViewModel
import com.cola.pickly.feature.organize.OrganizeScreen
import com.cola.pickly.feature.organize.OrganizeViewModel
import com.cola.pickly.core.ui.components.BulkAction
import com.cola.pickly.core.ui.components.BulkActionBar
import com.cola.pickly.feature.settings.SettingsScreen
import com.cola.pickly.core.model.PhotoSelectionState
import com.cola.pickly.core.model.RejectReason
import com.cola.pickly.core.model.ViewerContext
import com.cola.pickly.core.ui.theme.TealAccent
import com.cola.pickly.presentation.legal.PrivacyPolicyScreen
import com.cola.pickly.presentation.legal.OpenSourceLicensesScreen

sealed class MainTab(
    val route: String,
    @StringRes val labelResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Organize : MainTab("organize", R.string.nav_organize, Icons.Filled.PhotoLibrary, Icons.Outlined.PhotoLibrary)
    object Archive : MainTab("archive", R.string.archive_title, Icons.Filled.Inventory2, Icons.Outlined.Inventory2)
    object Settings : MainTab("settings", R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings)

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
    organizeViewModel: OrganizeViewModel,
    tabs: List<MainTab> = MainTab.tabs
) {
    val organizeUiState by organizeViewModel.uiState.collectAsStateWithLifecycle()

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.outline
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        tabs.forEach { tab ->
            val isSelected = currentRoute == tab.route
            NavigationBarItem(
                icon = {
                    Icon(
                        if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = stringResource(tab.labelResId)
                    )
                },
                label = { Text(stringResource(tab.labelResId)) },
                selected = isSelected,
                onClick = {
                    // 분석 중이면 중단 확인 다이얼로그 표시
                    val isAnalyzing = (organizeUiState as? com.cola.pickly.feature.organize.OrganizeUiState.GridReady)?.isAnalyzing ?: false
                    if (isAnalyzing) {
                        organizeViewModel.requestInterruptConfirmation {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    } else {
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = TealAccent,
                    selectedTextColor = TealAccent,
                    unselectedIconColor = MaterialTheme.colorScheme.outline,
                    unselectedTextColor = MaterialTheme.colorScheme.outline,
                    indicatorColor = TealAccent.copy(alpha = 0.12f)
                )
            )
        }
    }
}

@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    onNavigateToPhotoDetail: (String, Long, Map<Long, PhotoSelectionState>, Boolean, ViewerContext, Map<Long, RejectReason>, List<Long>?) -> Unit,
    selectedFolder: Pair<String, String>? = null,
    selectionUpdates: Map<Long, PhotoSelectionState>? = null
) {
    val navController = rememberNavController()

    // OrganizeViewModel을 한 번만 생성하여 OrganizeScreen과 ArchiveScreen이 같은 인스턴스를 공유하도록 함
    val organizeViewModel: OrganizeViewModel = hiltViewModel()

    // 현재 탭 추적
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentTab = navBackStackEntry?.destination?.route

    // Multi Select Mode 상태 관리
    var isOrganizeMultiSelectMode by remember { mutableStateOf(false) }
    var isArchiveMultiSelectMode by remember { mutableStateOf(false) }

    val isMultiSelectMode = isOrganizeMultiSelectMode || isArchiveMultiSelectMode

    // Archive bulk action 콜백
    var onArchiveShareClick: (() -> Unit)? by remember { mutableStateOf(null) }
    var onArchiveMoveClick: (() -> Unit)? by remember { mutableStateOf(null) }
    var onArchiveCopyClick: (() -> Unit)? by remember { mutableStateOf(null) }
    var onArchiveDeleteClick: (() -> Unit)? by remember { mutableStateOf(null) }
    var archiveActionInProgress by remember { mutableStateOf(false) }

    // 탭 전환 시 다중 선택 모드 자동 해제
    LaunchedEffect(currentTab) {
        if (isOrganizeMultiSelectMode) {
            organizeViewModel.exitMultiSelectMode()
        }
        // Archive는 ArchiveViewModel의 exitMultiSelectMode()를 직접 호출할 수 없으므로
        // isArchiveMultiSelectMode 상태를 리셋 (ArchiveScreen의 LaunchedEffect가 감지)
        isArchiveMultiSelectMode = false
    }

    // 종료 다이얼로그 상태 관리
    var showExitDialog by remember { mutableStateOf(false) }
    val activity = LocalContext.current as? Activity

    // Multi Select Mode가 아닐 때만 종료 다이얼로그 표시
    BackHandler(enabled = !isMultiSelectMode) {
        showExitDialog = true
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        // Bottom Area: Normal Mode일 때 Bottom Navigation Bar, Multi Select Mode일 때 Bulk Action Bar
        bottomBar = {
            if (isMultiSelectMode) {
                val actions = when (currentTab) {
                    MainTab.Organize.route -> BulkAction.organizeActions
                    MainTab.Archive.route -> BulkAction.archiveActions
                    else -> emptyList()
                }
                BulkActionBar(
                    actions = actions,
                    onActionClick = { action ->
                        when (action) {
                            is BulkAction.Accept -> organizeViewModel.bulkAcceptSelected()
                            is BulkAction.Unmark -> organizeViewModel.bulkUnmarkSelected()
                            is BulkAction.Reject -> organizeViewModel.bulkRejectSelected()
                            is BulkAction.Share -> onArchiveShareClick?.invoke()
                            is BulkAction.Move -> onArchiveMoveClick?.invoke()
                            is BulkAction.Copy -> onArchiveCopyClick?.invoke()
                            is BulkAction.Delete -> onArchiveDeleteClick?.invoke()
                        }
                    },
                    isActionInProgress = archiveActionInProgress
                )
            } else {
                // Normal Mode: Bottom Navigation Bar 표시
                PicklyBottomNavigation(navController = navController, organizeViewModel = organizeViewModel)
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
                    onNavigateToPhotoDetail = { folderId, photoId, selectionMap, selectedOnly, rejectCandidates, orderedPhotoIds ->
                        onNavigateToPhotoDetail(folderId, photoId, selectionMap, selectedOnly, ViewerContext.SELECT, rejectCandidates, orderedPhotoIds)
                    },
                    selectedFolder = selectedFolder,
                    selectionUpdates = selectionUpdates,
                    onMultiSelectModeChanged = { isMultiSelect ->
                        isOrganizeMultiSelectMode = isMultiSelect
                    }
                )
            }
            composable(MainTab.Archive.route) {
                val archiveViewModel: ArchiveViewModel = hiltViewModel()
                val globalSelectionMap by organizeViewModel.globalSelectionMap.collectAsStateWithLifecycle()
                val archiveIsActionInProgress by archiveViewModel.isActionInProgress.collectAsStateWithLifecycle()

                // Archive의 액션 진행 상태를 MainScreen에 반영
                LaunchedEffect(archiveIsActionInProgress) {
                    archiveActionInProgress = archiveIsActionInProgress
                }

                // Archive bulk action 콜백 등록
                LaunchedEffect(Unit) {
                    onArchiveShareClick = { archiveViewModel.shareSelectedPhotos() }
                    onArchiveMoveClick = { archiveViewModel.requestDestinationForMove() }
                    onArchiveCopyClick = { archiveViewModel.requestDestinationForCopy() }
                    onArchiveDeleteClick = { archiveViewModel.requestDeleteConfirmation() }
                }

                ArchiveScreen(
                    viewModel = archiveViewModel,
                    globalSelectionMap = globalSelectionMap,
                    onNavigateToPhotoDetail = { folderId, photoId, selectionMap, selectedOnly, rejectCandidates, orderedPhotoIds ->
                        onNavigateToPhotoDetail(folderId, photoId, selectionMap, selectedOnly, ViewerContext.ARCHIVE, rejectCandidates, orderedPhotoIds)
                    },
                    onNavigateToOrganize = {
                        // Tab 1 (정리하기)로 이동
                        navController.navigate(MainTab.Organize.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onMultiSelectModeChanged = { isMultiSelect ->
                        isArchiveMultiSelectMode = isMultiSelect
                    }
                )
            }
            composable(MainTab.Settings.route) {
                SettingsScreen(
                    onNavigateToPrivacyPolicy = {
                        navController.navigate("privacy_policy")
                    },
                    onNavigateToOpenSourceLicenses = {
                        navController.navigate("open_source_licenses")
                    }
                )
            }
            composable("privacy_policy") {
                PrivacyPolicyScreen(
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }
            composable("open_source_licenses") {
                OpenSourceLicensesScreen(
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }

    // 종료 확인 다이얼로그
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text(text = stringResource(CoreUiR.string.exit_dialog_stay))
                }
            },
            confirmButton = {
                TextButton(onClick = { activity?.finish() }) {
                    Text(text = stringResource(CoreUiR.string.exit_dialog_exit))
                }
            },
            text = {
                Text(text = stringResource(CoreUiR.string.exit_dialog_message))
            }
        )
    }
}
