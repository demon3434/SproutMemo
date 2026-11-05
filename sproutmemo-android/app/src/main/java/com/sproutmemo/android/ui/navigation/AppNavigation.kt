package com.sproutmemo.android.ui.navigation

import android.app.Activity
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.sproutmemo.android.ui.auth.AuthState
import com.sproutmemo.android.ui.auth.AuthViewModel
import com.sproutmemo.android.ui.auth.LoginScreen
import com.sproutmemo.android.ui.book.BookManageScreen
import com.sproutmemo.android.ui.book.BookManageViewModel
import com.sproutmemo.android.ui.dashboard.DashboardScreen
import com.sproutmemo.android.ui.dashboard.DashboardViewModel
import com.sproutmemo.android.ui.note.NoteCreateScreen
import com.sproutmemo.android.ui.note.NoteCreateViewModel
import com.sproutmemo.android.ui.note.NoteListScreen
import com.sproutmemo.android.ui.note.NoteListViewModel
import com.sproutmemo.android.ui.note.NoteEditScreen
import com.sproutmemo.android.ui.note.NoteEditViewModel
import com.sproutmemo.android.data.remote.ApiClient
import kotlinx.coroutines.launch
import com.sproutmemo.android.ui.parent.ChildrenManageScreen
import com.sproutmemo.android.ui.parent.ChildrenManageViewModel
import com.sproutmemo.android.ui.review.DailyReviewScreen
import com.sproutmemo.android.ui.review.DrawReviewScreen
import com.sproutmemo.android.ui.review.ReviewHistoryScreen
import com.sproutmemo.android.ui.review.ReviewPracticeScreen
import com.sproutmemo.android.ui.review.ReviewViewModel
import com.sproutmemo.android.ui.review.NotePreviewScreen
import com.sproutmemo.android.ui.settings.SettingsScreen
import com.sproutmemo.android.ui.settings.SettingsViewModel
import com.sproutmemo.android.ui.shell.AppShell
import com.sproutmemo.android.ui.shell.Screen

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun AppNavigation(navController: NavHostController) {
    val context = LocalContext.current
    val activity = context as? Activity

    val windowSizeClass = if (activity != null) calculateWindowSizeClass(activity) else null
    val isTabletLandscape = windowSizeClass?.widthSizeClass == WindowWidthSizeClass.Expanded

    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.authState.collectAsState()

    when (val state = authState) {
        is AuthState.Success -> {
            val userRole = state.role
            val userId = state.userId
            val sessionKey = "$userId-$userRole"

            val dashboardViewModel: DashboardViewModel = viewModel(key = sessionKey)
            val noteCreateViewModel: NoteCreateViewModel = viewModel(key = sessionKey)
            val noteListViewModel: NoteListViewModel = viewModel(key = sessionKey)
            val reviewViewModel: ReviewViewModel = viewModel(key = sessionKey)
            val bookManageViewModel: BookManageViewModel = viewModel(key = sessionKey)
            val childrenManageViewModel: ChildrenManageViewModel = viewModel(key = sessionKey)
            val settingsViewModel: SettingsViewModel = viewModel(key = sessionKey)

            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Dashboard.route
            val currentScreen = remember(currentRoute) {
                if (currentRoute.startsWith("note_edit")) Screen.NoteEdit
                else if (currentRoute.startsWith("review_practice")) Screen.ReviewPractice
                else when (currentRoute) {
                    Screen.Dashboard.route -> Screen.Dashboard
                    Screen.NoteCreate.route -> Screen.NoteCreate
                    Screen.NoteList.route -> Screen.NoteList
                    Screen.DailyReview.route -> Screen.DailyReview
                    Screen.DrawReview.route -> Screen.DrawReview
                    Screen.ReviewHistory.route -> Screen.ReviewHistory
                    Screen.BookManage.route -> Screen.BookManage
                    Screen.ChildrenManage.route -> Screen.ChildrenManage
                    Screen.Settings.route -> Screen.Settings
                    else -> Screen.Dashboard
                }
            }

            var noteEditBackHandler by remember { mutableStateOf<(() -> Unit)?>(null) }
            var settingsBackHandler by remember { mutableStateOf<(() -> Unit)?>(null) }

            AppShell(
                userRole = userRole,
                currentScreen = currentScreen,
                onScreenSelect = { screen ->
                    if (screen == Screen.Settings) {
                        navController.navigate(screen.route) {
                            launchSingleTop = true
                        }
                    } else {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.id) {
                                inclusive = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                onLogout = { authViewModel.logout() },
                isTabletLandscape = isTabletLandscape,
                onBackPressed = when (currentScreen) {
                    Screen.NoteEdit -> {
                        {
                            noteEditBackHandler?.invoke() ?: navController.popBackStack()
                        }
                    }
                    Screen.Settings -> {
                        {
                            settingsBackHandler?.invoke() ?: navController.popBackStack()
                        }
                    }
                    Screen.ReviewHistory -> {
                        {
                            navController.popBackStack()
                        }
                    }
                    else -> null
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.ReviewHistory.route)
                }
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Dashboard.route,
                    enterTransition = {
                        slideInHorizontally(animationSpec = tween(300)) { it }
                    },
                    exitTransition = {
                        slideOutHorizontally(animationSpec = tween(300)) { -it }
                    },
                    popEnterTransition = {
                        slideInHorizontally(animationSpec = tween(300)) { -it }
                    },
                    popExitTransition = {
                        slideOutHorizontally(animationSpec = tween(300)) { it }
                    }
                ) {
                    composable(Screen.Dashboard.route) {
                        DashboardScreen(viewModel = dashboardViewModel)
                    }
                    composable(Screen.NoteCreate.route) {
                        NoteCreateScreen(
                            viewModel = noteCreateViewModel,
                            onNoteCreated = {
                                navController.navigate(Screen.NoteList.route) {
                                    popUpTo(Screen.Dashboard.route)
                                }
                            }
                        )
                    }
                    composable(Screen.NoteList.route) {
                        NoteListScreen(
                            viewModel = noteListViewModel,
                            userRole = userRole,
                            isTabletLandscape = isTabletLandscape,
                            onEditNote = { noteId ->
                                navController.navigate("note_edit/$noteId")
                            }
                        )
                    }
                    composable("note_edit/{noteId}") { backStackEntry ->
                        val noteId = backStackEntry.arguments?.getString("noteId") ?: ""
                        val noteEditViewModel: NoteEditViewModel = viewModel(key = "edit-$noteId")
                        val scope = rememberCoroutineScope()
                        val editContext = LocalContext.current
                        LaunchedEffect(noteId) {
                            noteEditViewModel.loadNote(noteId)
                        }
                        NoteEditScreen(
                            viewModel = noteEditViewModel,
                            onNoteUpdated = {
                                noteListViewModel.fetchNotes()
                                noteListViewModel.selectedNoteDetail.value?.let { currentNote ->
                                    if (currentNote.id == noteId) {
                                        scope.launch {
                                            try {
                                                val res = ApiClient.getService(editContext).getNoteDetail(noteId)
                                                if (res.isSuccessful && res.body()?.code == 200) {
                                                    noteListViewModel.selectedNoteDetail.value = res.body()?.data
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                    }
                                }
                                navController.popBackStack()
                            },
                            onCancel = {
                                navController.popBackStack()
                            },
                            onRegisterBackHandler = { handler ->
                                noteEditBackHandler = handler
                            }
                        )
                    }
                    composable(Screen.DailyReview.route) {
                        DailyReviewScreen(
                            viewModel = reviewViewModel,
                            userRole = userRole
                        )
                    }
                    composable(Screen.DrawReview.route) {
                        DrawReviewScreen(
                            viewModel = reviewViewModel,
                            onStartPractice = { sessionId ->
                                navController.navigate("review_practice/$sessionId")
                            }
                        )
                    }
                    composable(Screen.ReviewHistory.route) {
                        ReviewHistoryScreen(
                            viewModel = reviewViewModel,
                            userRole = userRole,
                            isTabletLandscape = isTabletLandscape,
                            onNavigateToNote = { noteId ->
                                navController.navigate("note_preview/$noteId")
                            }
                        )
                    }
                    composable("review_practice/{sessionId}") { backStackEntry ->
                        val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
                        ReviewPracticeScreen(
                            viewModel = reviewViewModel,
                            sessionId = sessionId,
                            isTabletLandscape = isTabletLandscape,
                            onBack = {
                                navController.popBackStack()
                            }
                        )
                    }
                    composable("note_preview/{noteId}") { backStackEntry ->
                        val noteId = backStackEntry.arguments?.getString("noteId") ?: ""
                        NotePreviewScreen(
                            noteId = noteId,
                            userRole = userRole,
                            onBack = {
                                navController.popBackStack()
                            }
                        )
                    }
                    composable(Screen.BookManage.route) {
                        BookManageScreen(viewModel = bookManageViewModel)
                    }
                    composable(Screen.ChildrenManage.route) {
                        ChildrenManageScreen(viewModel = childrenManageViewModel)
                    }
                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onLogout = { authViewModel.logout() },
                            onRegisterBackHandler = { settingsBackHandler = it }
                        )
                    }
                }
            }
        }

        else -> {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = { role ->
                    // Navigation will auto recompose to Success state
                }
            )
        }
    }
}

@Composable
private fun semestersLoaded(viewModel: NoteListViewModel): Boolean {
    val semesters by viewModel.semesters.collectAsState()
    return semesters.isNotEmpty()
}
