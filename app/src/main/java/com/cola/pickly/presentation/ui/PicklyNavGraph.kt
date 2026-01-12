package com.cola.pickly.presentation.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.ui.window.DialogProperties
import com.cola.pickly.core.model.WeekId
import com.cola.pickly.presentation.MainScreen
import com.cola.pickly.presentation.MainViewModel
import com.cola.pickly.presentation.splash.SplashScreen
import com.cola.pickly.core.model.PhotoSelectionState
import com.cola.pickly.presentation.viewer.ViewerUiState
import com.cola.pickly.presentation.viewer.ViewerViewModel
import com.cola.pickly.presentation.viewer.ViewerScreen
import com.cola.pickly.feature.weekly.WeeklyDetailScreen

@Composable
fun PicklyNavGraph(
    mainViewModel: MainViewModel
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(navController = navController, viewModel = mainViewModel) 
        }
        composable("main") { backStackEntry ->
            // S-03에서 전달받은 결과 처리 (폴더 선택)
            val savedStateHandle = backStackEntry.savedStateHandle
            val selectedFolderId = savedStateHandle.get<String>("selected_folder_id")
            val selectedFolderName = savedStateHandle.get<String>("selected_folder_name")
            
            // S-04에서 전달받은 결과 처리 (선택 상태 변경)
            // popBackStack() 후 main composable이 다시 실행될 때마다 selection_updates를 읽도록 함
            // savedStateHandle의 keys를 관찰하여 변경을 감지
            val currentBackStackEntry by navController.currentBackStackEntryAsState()
            var selectionUpdates by remember { mutableStateOf<Map<Long, PhotoSelectionState>?>(null) }
            
            // backStackEntry가 변경될 때마다 (popBackStack 후) selection_updates를 확인
            LaunchedEffect(currentBackStackEntry?.id) {
                val updates = savedStateHandle.get<Map<Long, PhotoSelectionState>>("selection_updates")
                if (updates != null) {
                    selectionUpdates = updates
                    savedStateHandle.remove<Map<Long, PhotoSelectionState>>("selection_updates")
                }
            }

            MainScreen(
                mainViewModel = mainViewModel,
                weeklyListViewModel = hiltViewModel(),
                onNavigateToDetail = { weekId ->
                    navController.navigate("weekly_detail/${weekId}")
                },
                onNavigateToFolderSelect = {
                    // 이제 S-03은 MainScreen(OrganizeScreen) 내부의 Dialog로 처리되므로 
                    // 네비게이션 액션은 필요 없거나, 필요하다면 Dialog를 띄우는 이벤트를 전달해야 합니다.
                    // 현재 MainScreen의 onNavigateToFolderSelect는 OrganizeScreen 내부에서 처리되도록 변경되었으므로
                    // 여기서는 빈 람다 혹은 로깅 정도만 처리하면 됩니다.
                    // (OrganizeScreen 내부에서 showFolderSheet 상태로 처리됨)
                },
                onNavigateToPhotoDetail = { folderId, photoId, selectionMap ->
                    // viewer 라우트로 이동하기 전에 selectionMap을 현재 backStackEntry의 savedStateHandle에 저장
                    // viewer composable에서 previousBackStackEntry의 savedStateHandle을 통해 읽어옴
                    navController.currentBackStackEntry?.savedStateHandle?.set(
                        "initial_selection_map_for_viewer",
                        selectionMap
                    )
                    navController.navigate("viewer/$folderId/$photoId")
                },
                selectedFolder = if (selectedFolderId != null && selectedFolderName != null) {
                    selectedFolderId to selectedFolderName
                } else null,
                selectionUpdates = selectionUpdates
            )
        }
        composable(
            route = "weekly_detail/{weekId}",
            arguments = listOf(navArgument("weekId") { type = NavType.StringType })
        ) { backStackEntry ->
            val weekIdString = backStackEntry.arguments?.getString("weekId") ?: return@composable
            
            val parts = weekIdString.split("-W")
            if (parts.size == 2) {
                val year = parts[0].toIntOrNull() ?: 0
                val weekOfYear = parts[1].toIntOrNull() ?: 0
                val weekId = WeekId(year, weekOfYear)

                WeeklyDetailScreen(
                    viewModel = hiltViewModel(),
                    weekId = weekId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        // FolderSelectScreen 라우트 제거됨 (S-03)

        dialog(
            route = "viewer/{folderId}/{photoId}",
            arguments = listOf(
                navArgument("folderId") { type = NavType.StringType },
                navArgument("photoId") { type = NavType.LongType }
            ),
            dialogProperties = DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) { backStackEntry ->
            // 이전 화면(main)에서 전달된 initial_selection_map을 읽음
            val selectionMapFromPrevious = navController.previousBackStackEntry?.savedStateHandle?.get<Map<Long, PhotoSelectionState>>("initial_selection_map_for_viewer")
            
            val viewerViewModel: ViewerViewModel = hiltViewModel()
            
            // ViewModel 초기화 직후 selectionMap 설정
            if (selectionMapFromPrevious != null) {
                viewerViewModel.initializeSelectionMap(selectionMapFromPrevious)
                // 데이터 소비 후 초기화
                navController.previousBackStackEntry?.savedStateHandle?.remove<Map<Long, PhotoSelectionState>>("initial_selection_map_for_viewer")
            }
            
            val uiState by viewerViewModel.uiState.collectAsStateWithLifecycle()
            
            // 시스템 back key 처리 - onBackClick과 동일한 동작
            when (val state = uiState) {
                is ViewerUiState.Success -> {
                    // 시스템 back key를 처리하는 함수
                    val handleBack: () -> Unit = {
                        // 뒤로가기 시 변경된 선택 상태를 전달
                        navController.previousBackStackEntry?.savedStateHandle?.set("selection_updates", state.selectionMap)
                        navController.popBackStack()
                    }
                    
                    // BackHandler로 시스템 back key 처리
                    BackHandler(onBack = handleBack)
                    
                    ViewerScreen(
                        photos = state.photos,
                        initialIndex = state.initialIndex,
                        selectionMap = state.selectionMap,
                        onBackClick = handleBack,
                        onSelectClick = { photoId ->
                            viewerViewModel.toggleSelection(photoId)
                        },
                        onRejectClick = { photoId ->
                            viewerViewModel.toggleRejection(photoId)
                        }
                    )
                }
                is ViewerUiState.Error -> {
                    // 에러 처리 (예: Toast 메시지 후 종료)
                    // 여기서는 간단히 빈 화면이나 로딩 유지
                }
                ViewerUiState.Loading -> {
                    // 로딩 화면 (ViewerScreen 내부에서 처리하거나 별도 처리)
                }
            }
        }
    }
}
