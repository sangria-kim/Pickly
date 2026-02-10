package com.cola.pickly.presentation.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import com.cola.pickly.core.ui.transition.ContainerTransformSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cola.pickly.presentation.MainScreen
import com.cola.pickly.presentation.MainViewModel
import com.cola.pickly.presentation.splash.SplashScreen
import com.cola.pickly.core.model.PhotoSelectionState
import com.cola.pickly.core.model.RejectReason
import com.cola.pickly.core.model.ViewerContext
import com.cola.pickly.presentation.viewer.ViewerUiState
import com.cola.pickly.presentation.viewer.ViewerScreen
import com.cola.pickly.presentation.viewer.ViewerViewModel
import com.cola.pickly.core.data.settings.SettingsRepository
import com.cola.pickly.core.ui.transition.LocalAnimatedVisibilityScope
import com.cola.pickly.core.ui.transition.LocalSharedTransitionScope
import dagger.hilt.android.EntryPointAccessors
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PicklyNavGraphEntryPoint {
    fun settingsRepository(): SettingsRepository
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PicklyNavGraph(
    mainViewModel: MainViewModel
) {
    val context = LocalContext.current
    val entryPoint = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            PicklyNavGraphEntryPoint::class.java
        )
    }
    val settingsRepository = remember { entryPoint.settingsRepository() }
    val navController = rememberNavController()
    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            NavHost(navController = navController, startDestination = "splash") {
                composable("splash") {
                    SplashScreen(navController = navController, viewModel = mainViewModel)
                }
                composable("main") { backStackEntry ->
                    CompositionLocalProvider(LocalAnimatedVisibilityScope provides this@composable) {
                        val savedStateHandle = backStackEntry.savedStateHandle
                        val selectedFolderId = savedStateHandle.get<String>("selected_folder_id")
                        val selectedFolderName = savedStateHandle.get<String>("selected_folder_name")

                        val currentBackStackEntry by navController.currentBackStackEntryAsState()
                        var selectionUpdates by remember { mutableStateOf<Map<Long, PhotoSelectionState>?>(null) }

                        LaunchedEffect(currentBackStackEntry?.id) {
                            val updates = savedStateHandle.get<Map<Long, PhotoSelectionState>>("selection_updates")
                            if (updates != null) {
                                selectionUpdates = updates
                                savedStateHandle.remove<Map<Long, PhotoSelectionState>>("selection_updates")
                            }
                        }

                        MainScreen(
                            mainViewModel = mainViewModel,
                            onNavigateToPhotoDetail = { folderId, photoId, selectionMap, selectedOnly, viewerContext, rejectCandidates ->
                                navController.currentBackStackEntry?.savedStateHandle?.set(
                                    "initial_selection_map_for_viewer",
                                    selectionMap
                                )
                                navController.currentBackStackEntry?.savedStateHandle?.set(
                                    "initial_reject_candidates_for_viewer",
                                    rejectCandidates
                                )
                                navController.navigate("viewer/$folderId/$photoId?selectedOnly=$selectedOnly&context=${viewerContext.name}")
                            },
                            selectedFolder = if (selectedFolderId != null && selectedFolderName != null) {
                                selectedFolderId to selectedFolderName
                            } else null,
                            selectionUpdates = selectionUpdates
                        )
                    }
                }
                composable(
                    route = "viewer/{folderId}/{photoId}?selectedOnly={selectedOnly}&context={context}",
                    arguments = listOf(
                        navArgument("folderId") { type = NavType.StringType },
                        navArgument("photoId") { type = NavType.LongType },
                        navArgument("selectedOnly") {
                            type = NavType.BoolType
                            defaultValue = false
                        },
                        navArgument("context") {
                            type = NavType.StringType
                            defaultValue = ViewerContext.SELECT.name
                        }
                    ),
                    enterTransition = { ContainerTransformSpec.navFadeIn() },
                    exitTransition = { ContainerTransformSpec.navFadeOut() },
                    popEnterTransition = { ContainerTransformSpec.navFadeIn() },
                    popExitTransition = { ContainerTransformSpec.navFadeOut() }
                ) { backStackEntry ->
                    CompositionLocalProvider(LocalAnimatedVisibilityScope provides this@composable) {
                        val contextName = backStackEntry.arguments?.getString("context") ?: ViewerContext.SELECT.name
                        val viewerContext = try {
                            ViewerContext.valueOf(contextName)
                        } catch (e: IllegalArgumentException) {
                            ViewerContext.SELECT
                        }

                        val selectionMapFromPrevious = navController.previousBackStackEntry?.savedStateHandle?.get<Map<Long, PhotoSelectionState>>("initial_selection_map_for_viewer")
                        val rejectCandidatesFromPrevious = navController.previousBackStackEntry?.savedStateHandle?.get<Map<Long, RejectReason>>("initial_reject_candidates_for_viewer")

                        val viewerViewModel: ViewerViewModel = hiltViewModel()

                        if (selectionMapFromPrevious != null) {
                            viewerViewModel.initializeViewerData(
                                selectionMapFromPrevious,
                                rejectCandidatesFromPrevious ?: emptyMap()
                            )
                            navController.previousBackStackEntry?.savedStateHandle?.remove<Map<Long, PhotoSelectionState>>("initial_selection_map_for_viewer")
                            navController.previousBackStackEntry?.savedStateHandle?.remove<Map<Long, RejectReason>>("initial_reject_candidates_for_viewer")
                        }

                        val uiState by viewerViewModel.uiState.collectAsStateWithLifecycle()

                        when (val state = uiState) {
                            is ViewerUiState.Success -> {
                                val coroutineScope = rememberCoroutineScope()
                                var exitRequestId by remember { mutableStateOf(0) }
                                var isClosing by remember { mutableStateOf(false) }

                                val requestClose: () -> Unit = requestClose@{
                                    if (isClosing) return@requestClose
                                    isClosing = true
                                    exitRequestId += 1
                                    coroutineScope.launch {
                                        withFrameNanos { }
                                        navController.previousBackStackEntry?.savedStateHandle?.set("selection_updates", state.selectionMap)
                                        navController.popBackStack()
                                    }
                                }

                                BackHandler(enabled = !isClosing, onBack = requestClose)

                                ViewerScreen(
                                    photos = state.photos,
                                    initialIndex = state.initialIndex,
                                    selectionMap = state.selectionMap,
                                    viewerContext = viewerContext,
                                    exitRequestId = exitRequestId,
                                    onBackClick = requestClose,
                                    onSelectClick = { photoId ->
                                        viewerViewModel.toggleSelection(photoId)
                                    },
                                    onRejectClick = { photoId ->
                                        viewerViewModel.toggleRejection(photoId)
                                    },
                                    settingsRepository = settingsRepository
                                )
                            }
                            is ViewerUiState.Error -> { }
                            ViewerUiState.Loading -> { }
                        }
                    }
                }
            }
        }
    }
}
