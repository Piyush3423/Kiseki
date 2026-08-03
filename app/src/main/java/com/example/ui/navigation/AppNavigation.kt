package com.example.ui.navigation

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.database.KisekiDatabase
import com.example.data.repository.ActivityTaskRepository
import com.example.viewmodel.ActivityTaskViewModel
import com.example.viewmodel.ActivityTaskViewModelFactory
import com.example.ui.analytics.AnalyticsScreen
import com.example.ui.history.HistoryScreen
import com.example.ui.home.HomeScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.NavBarBg
import com.example.data.repository.UserPreferences
import com.example.data.repository.UserPreferencesRepository
import com.example.viewmodel.SettingsViewModel
import com.example.viewmodel.SettingsViewModelFactory

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import com.example.data.repository.ThemeMode

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import com.example.ui.theme.MotionTokens
import com.example.ui.theme.pressScale

object Routes {
    const val TODAY = "today"
    const val HISTORY = "history"
    const val ANALYTICS = "analytics"
    const val SETTINGS = "settings"
    const val ADD_TASK = "add_task"
    const val TASK_DETAILS = "task_details"
    const val TASK_GROUPS = "task_groups"
    const val TASK_GROUP_DETAILS = "task_group_details"
    const val TEMPLATES = "templates"
}

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    userPreferences: UserPreferences = UserPreferences(),
    preferencesRepository: UserPreferencesRepository? = null,
    initialTaskId: String? = null,
    onHandledInitialTask: () -> Unit = {},
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val database = remember { KisekiDatabase.getDatabase(context) }
    val repository = remember { ActivityTaskRepository(database.activityTaskDao()) }
    val categoryRepository = remember { com.example.data.repository.CategoryRepository(database.categoryDao(), database.activityTaskDao()) }
    val taskGroupRepository = remember { com.example.data.repository.TaskGroupRepository(database.taskGroupDao(), database.activityTaskDao()) }
    val templateRepository = remember { com.example.data.repository.TaskGroupTemplateRepository(database.taskGroupTemplateDao(), database.activityTaskDao(), database.taskGroupDao()) }
    val viewModel: ActivityTaskViewModel = viewModel(factory = ActivityTaskViewModelFactory(repository, categoryRepository, taskGroupRepository, templateRepository, context))

    val prefsRepo = remember(context) { preferencesRepository ?: UserPreferencesRepository(context) }
    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(prefsRepo, context))

    val startRoute = remember(userPreferences.defaultStartScreen) {
        when (userPreferences.defaultStartScreen.lowercase()) {
            "history" -> Routes.HISTORY
            "analytics" -> Routes.ANALYTICS
            "settings" -> Routes.SETTINGS
            else -> Routes.TODAY
        }
    }

    LaunchedEffect(initialTaskId) {
        if (!initialTaskId.isNullOrBlank()) {
            navController.navigate("${Routes.TASK_DETAILS}?taskId=$initialTaskId")
            onHandledInitialTask()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val showBottomBar = currentDestination?.route in listOf(Routes.TODAY, Routes.HISTORY, Routes.ANALYTICS, Routes.SETTINGS)

            if (showBottomBar) {
                val isShadowMonarch = userPreferences.themeMode == ThemeMode.SHADOW_MONARCH

                NavigationBar(
                    containerColor = if (isShadowMonarch) Color(0xFF0C0E1A) else MaterialTheme.colorScheme.surface,
                    tonalElevation = if (isShadowMonarch) 0.dp else 3.dp,
                    modifier = Modifier.border(
                        width = 1.dp,
                        brush = if (isShadowMonarch) {
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF8B5CF6).copy(alpha = 0.5f),
                                    Color(0xFF00E5FF).copy(alpha = 0.5f)
                                )
                            )
                        } else {
                            SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        },
                        shape = RectangleShape
                    )
                ) {
                    val navItemColors = if (isShadowMonarch) {
                        NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF00E5FF),
                            selectedTextColor = Color(0xFF00E5FF),
                            indicatorColor = Color(0xFF281446),
                            unselectedIconColor = Color(0xFF64748B),
                            unselectedTextColor = Color(0xFF64748B)
                        )
                    } else {
                        NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val isTodaySelected = currentDestination?.hierarchy?.any { it.route == Routes.TODAY } == true
                    val isHistorySelected = currentDestination?.hierarchy?.any { it.route == Routes.HISTORY } == true
                    val isAnalyticsSelected = currentDestination?.hierarchy?.any { it.route == Routes.ANALYTICS } == true
                    val isSettingsSelected = currentDestination?.hierarchy?.any { it.route == Routes.SETTINGS } == true

                    val todayScale by animateFloatAsState(
                        targetValue = if (isTodaySelected) 1.08f else 1.0f,
                        animationSpec = MotionTokens.standardTween(),
                        label = "todayScale"
                    )
                    val historyScale by animateFloatAsState(
                        targetValue = if (isHistorySelected) 1.08f else 1.0f,
                        animationSpec = MotionTokens.standardTween(),
                        label = "historyScale"
                    )
                    val analyticsScale by animateFloatAsState(
                        targetValue = if (isAnalyticsSelected) 1.08f else 1.0f,
                        animationSpec = MotionTokens.standardTween(),
                        label = "analyticsScale"
                    )
                    val settingsScale by animateFloatAsState(
                        targetValue = if (isSettingsSelected) 1.08f else 1.0f,
                        animationSpec = MotionTokens.standardTween(),
                        label = "settingsScale"
                    )

                    NavigationBarItem(
                        selected = isTodaySelected,
                        onClick = {
                            navController.navigate(Routes.TODAY) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.pressScale(0.92f)
                            ) {
                                if (isShadowMonarch && isTodaySelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .background(
                                                brush = Brush.radialGradient(
                                                    colors = listOf(
                                                        Color(0xFF00E5FF).copy(alpha = 0.35f),
                                                        Color(0xFF8B5CF6).copy(alpha = 0.15f),
                                                        Color.Transparent
                                                    )
                                                ),
                                                shape = CircleShape
                                            )
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Rounded.Home,
                                    contentDescription = "Home",
                                    modifier = Modifier.graphicsLayer {
                                        scaleX = todayScale
                                        scaleY = todayScale
                                    }
                                )
                            }
                        },
                        label = { Text("Home", fontWeight = if (isTodaySelected) FontWeight.SemiBold else FontWeight.Normal) },
                        colors = navItemColors
                    )
                    NavigationBarItem(
                        selected = isHistorySelected,
                        onClick = {
                            navController.navigate(Routes.HISTORY) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.pressScale(0.92f)
                            ) {
                                if (isShadowMonarch && isHistorySelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .background(
                                                brush = Brush.radialGradient(
                                                    colors = listOf(
                                                        Color(0xFF00E5FF).copy(alpha = 0.35f),
                                                        Color(0xFF8B5CF6).copy(alpha = 0.15f),
                                                        Color.Transparent
                                                    )
                                                ),
                                                shape = CircleShape
                                            )
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Rounded.History,
                                    contentDescription = "History",
                                    modifier = Modifier.graphicsLayer {
                                        scaleX = historyScale
                                        scaleY = historyScale
                                    }
                                )
                            }
                        },
                        label = { Text("History", fontWeight = if (isHistorySelected) FontWeight.SemiBold else FontWeight.Normal) },
                        colors = navItemColors
                    )
                    NavigationBarItem(
                        selected = isAnalyticsSelected,
                        onClick = {
                            navController.navigate(Routes.ANALYTICS) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.pressScale(0.92f)
                            ) {
                                if (isShadowMonarch && isAnalyticsSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .background(
                                                brush = Brush.radialGradient(
                                                    colors = listOf(
                                                        Color(0xFF00E5FF).copy(alpha = 0.35f),
                                                        Color(0xFF8B5CF6).copy(alpha = 0.15f),
                                                        Color.Transparent
                                                    )
                                                ),
                                                shape = CircleShape
                                            )
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Rounded.Analytics,
                                    contentDescription = "Analytics",
                                    modifier = Modifier.graphicsLayer {
                                        scaleX = analyticsScale
                                        scaleY = analyticsScale
                                    }
                                )
                            }
                        },
                        label = { Text("Analytics", fontWeight = if (isAnalyticsSelected) FontWeight.SemiBold else FontWeight.Normal) },
                        colors = navItemColors
                    )
                    NavigationBarItem(
                        selected = isSettingsSelected,
                        onClick = {
                            navController.navigate(Routes.SETTINGS) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.pressScale(0.92f)
                            ) {
                                if (isShadowMonarch && isSettingsSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .background(
                                                brush = Brush.radialGradient(
                                                    colors = listOf(
                                                        Color(0xFF00E5FF).copy(alpha = 0.35f),
                                                        Color(0xFF8B5CF6).copy(alpha = 0.15f),
                                                        Color.Transparent
                                                    )
                                                ),
                                                shape = CircleShape
                                            )
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = "Settings",
                                    modifier = Modifier.graphicsLayer {
                                        scaleX = settingsScale
                                        scaleY = settingsScale
                                    }
                                )
                            }
                        },
                        label = { Text("Settings", fontWeight = if (isSettingsSelected) FontWeight.SemiBold else FontWeight.Normal) },
                        colors = navItemColors
                    )
                }
            }
        }
    ) { innerPadding ->
        fun getTabOrder(route: String?): Int {
            return when (route) {
                Routes.TODAY -> 0
                Routes.HISTORY -> 1
                Routes.ANALYTICS -> 2
                Routes.SETTINGS -> 3
                else -> -1
            }
        }

        NavHost(
            navController = navController,
            startDestination = startRoute,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                val initialOrder = getTabOrder(initialState.destination.route)
                val targetOrder = getTabOrder(targetState.destination.route)
                if (initialOrder != -1 && targetOrder != -1) {
                    if (targetOrder > initialOrder) {
                        slideInHorizontally(MotionTokens.standardTween()) { (it * 0.12f).toInt() } + fadeIn(MotionTokens.standardTween())
                    } else {
                        slideInHorizontally(MotionTokens.standardTween()) { -(it * 0.12f).toInt() } + fadeIn(MotionTokens.standardTween())
                    }
                } else {
                    fadeIn(MotionTokens.standardTween()) + slideInHorizontally(MotionTokens.standardTween()) { (it * 0.08f).toInt() }
                }
            },
            exitTransition = {
                val initialOrder = getTabOrder(initialState.destination.route)
                val targetOrder = getTabOrder(targetState.destination.route)
                if (initialOrder != -1 && targetOrder != -1) {
                    if (targetOrder > initialOrder) {
                        slideOutHorizontally(MotionTokens.standardTween()) { -(it * 0.12f).toInt() } + fadeOut(MotionTokens.standardTween())
                    } else {
                        slideOutHorizontally(MotionTokens.standardTween()) { (it * 0.12f).toInt() } + fadeOut(MotionTokens.standardTween())
                    }
                } else {
                    fadeOut(MotionTokens.standardTween()) + slideOutHorizontally(MotionTokens.standardTween()) { -(it * 0.08f).toInt() }
                }
            },
            popEnterTransition = {
                fadeIn(MotionTokens.standardTween()) + slideInHorizontally(MotionTokens.standardTween()) { -(it * 0.08f).toInt() }
            },
            popExitTransition = {
                fadeOut(MotionTokens.standardTween()) + slideOutHorizontally(MotionTokens.standardTween()) { (it * 0.08f).toInt() }
            }
        ) {
            composable(Routes.TODAY) {
                HomeScreen(
                    viewModel = viewModel,
                    showCompletedOnToday = userPreferences.showCompletedOnToday,
                    startWeekOnMonday = userPreferences.startWeekOnMonday,
                    themeMode = userPreferences.themeMode,
                    onAddTaskClick = { navController.navigate(Routes.ADD_TASK) },
                    onEditTaskClick = { taskId -> navController.navigate("${Routes.ADD_TASK}?taskId=$taskId") },
                    onTaskClick = { taskId -> navController.navigate("${Routes.TASK_DETAILS}?taskId=$taskId") },
                    onNavigateToTemplates = { navController.navigate(Routes.TEMPLATES) }
                )
            }
            composable(Routes.HISTORY) {
                HistoryScreen(
                    viewModel = viewModel,
                    onTaskClick = { taskId -> navController.navigate("${Routes.TASK_DETAILS}?taskId=$taskId") }
                )
            }
            composable(Routes.ANALYTICS) {
                AnalyticsScreen(
                    viewModel = viewModel,
                    themeMode = userPreferences.themeMode
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    contentPadding = innerPadding,
                    onNavigateToTaskGroups = { navController.navigate(Routes.TASK_GROUPS) },
                    onNavigateToTemplates = { navController.navigate(Routes.TEMPLATES) }
                )
            }
            composable(Routes.TEMPLATES) {
                com.example.ui.template.TemplatesScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Routes.TASK_GROUPS) {
                com.example.ui.taskgroup.TaskGroupScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onGroupClick = { groupId -> navController.navigate("${Routes.TASK_GROUP_DETAILS}/$groupId") }
                )
            }
            composable(
                route = "${Routes.TASK_GROUP_DETAILS}/{groupId}",
                arguments = listOf(androidx.navigation.navArgument("groupId") {
                    type = androidx.navigation.NavType.StringType
                    nullable = false
                })
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
                com.example.ui.taskgroup.TaskGroupDetailsScreen(
                    groupId = groupId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onEditTaskClick = { id -> navController.navigate("${Routes.ADD_TASK}?taskId=$id") },
                    themeMode = userPreferences.themeMode
                )
            }
            composable(
                route = "${Routes.TASK_DETAILS}?taskId={taskId}",
                arguments = listOf(androidx.navigation.navArgument("taskId") {
                    type = androidx.navigation.NavType.StringType
                    nullable = false
                })
            ) { backStackEntry ->
                val taskId = backStackEntry.arguments?.getString("taskId") ?: return@composable
                com.example.ui.taskdetails.TaskDetailsScreen(
                    taskId = taskId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onEditTask = { id -> navController.navigate("${Routes.ADD_TASK}?taskId=$id") }
                )
            }
            composable(
                route = "${Routes.ADD_TASK}?taskId={taskId}",
                arguments = listOf(androidx.navigation.navArgument("taskId") {
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                })
            ) { backStackEntry ->
                val taskId = backStackEntry.arguments?.getString("taskId")
                com.example.ui.addtask.AddTaskScreen(
                    onNavigateBack = { navController.popBackStack() },
                    viewModel = viewModel,
                    taskId = taskId,
                    onNavigateToManageGroups = { navController.navigate(Routes.TASK_GROUPS) }
                )
            }
        }
    }
}
