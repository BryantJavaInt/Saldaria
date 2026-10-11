package com.bryantgiorgini.saldaria.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bryantgiorgini.saldaria.R
import com.bryantgiorgini.saldaria.ui.CompareScreen
import com.bryantgiorgini.saldaria.ui.ScheduleScreen
import com.bryantgiorgini.saldaria.ui.SimulatorScreen
import com.bryantgiorgini.saldaria.ui.theme.customColors
import com.bryantgiorgini.saldaria.viewmodel.SimulatorViewModel

sealed class Screen(val route: String, val titleRes: Int, val topBarTitleRes: Int, val icon: ImageVector, val contentDescRes: Int) {
    object Simulator : Screen("simulator", R.string.nav_simulator, R.string.app_name, Icons.Default.Calculate, R.string.cd_nav_simulator)
    object Schedule : Screen("schedule", R.string.nav_schedule, R.string.title_schedule, Icons.Default.List, R.string.cd_nav_schedule)
    object Compare : Screen("compare", R.string.nav_compare, R.string.nav_compare, Icons.AutoMirrored.Filled.CompareArrows, R.string.cd_nav_compare)
}

val items = listOf(
    Screen.Simulator,
    Screen.Schedule,
    Screen.Compare
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    // Instancia única del ViewModel compartida entre las pantallas de la navegación
    val viewModel: SimulatorViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    // Determinamos el título actual de la TopAppBar
    val currentScreen = items.find { screen -> 
        currentDestination?.hierarchy?.any { it.route == screen.route } == true 
    }
    val currentScreenTitleRes = currentScreen?.topBarTitleRes ?: R.string.app_name

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(currentScreenTitleRes)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                items.forEach { screen ->
                    val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    val itemCd = stringResource(screen.contentDescRes)
                    NavigationBarItem(
                        icon = { 
                            Icon(
                                imageVector = screen.icon, 
                                contentDescription = null // El contentDescription se pone en el Modifier del NavigationBarItem
                            ) 
                        },
                        label = { Text(stringResource(screen.titleRes)) },
                        selected = isSelected,
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.customColors.activeFill
                        ),
                        modifier = Modifier.semantics {
                            contentDescription = itemCd
                        },
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Simulator.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Simulator.route) {
                SimulatorScreen(
                    state = state,
                    onPrincipalChange = viewModel::updatePrincipal,
                    onRateChange = viewModel::updateRate,
                    onYearsChange = viewModel::updateYears,
                    onCalculate = viewModel::calculate,
                    onRetryEcb = viewModel::fetchEcbRate,
                    onUseEcbRate = viewModel::useEcbRateAsTin
                )
            }
            composable(Screen.Schedule.route) {
                ScheduleScreen(state = state)
            }
            composable(Screen.Compare.route) {
                CompareScreen(
                    state = state,
                    onMonthChange = viewModel::updatePrepaymentMonth,
                    onAmountChange = viewModel::updatePrepaymentAmount,
                    onModeChange = viewModel::updatePrepaymentMode,
                    onCalculate = viewModel::calculatePrepayment
                )
            }
        }
    }
}
