package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.UserRole
import com.example.data.repository.FarnadRepository
import com.example.ui.screens.EmployeeWorkspaceScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.OwnerWorkspaceScreen
import com.example.ui.theme.BrandRed
import com.example.ui.theme.FarnadCellularTheme
import com.example.ui.theme.ThemeMode
import com.example.viewmodel.EmployeeViewModel
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.OwnerViewModel
import com.example.viewmodel.SessionUiState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FarnadAppRoot()
        }
    }
}

@Composable
fun FarnadAppRoot() {
    val context = LocalContext.current.applicationContext
    val repository = remember(context) { FarnadRepository(context) }

    val mainViewModel: MainViewModel = viewModel(
        factory = MainViewModel.Factory(repository)
    )
    val sessionState by mainViewModel.sessionState.collectAsStateWithLifecycle()
    val loginState by mainViewModel.loginState.collectAsStateWithLifecycle()
    val themeMode by mainViewModel.themeMode.collectAsStateWithLifecycle()

    val isDarkActive = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    FarnadCellularTheme(themeMode = themeMode) {
        when (val session = sessionState) {
            SessionUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BrandRed)
                }
            }
            SessionUiState.LoggedOut -> {
                LoginScreen(
                    loginState = loginState,
                    isDarkTheme = isDarkActive,
                    onToggleTheme = { mainViewModel.toggleDarkMode(isDarkActive) },
                    onSelectRole = mainViewModel::selectRole,
                    onLoginSubmit = mainViewModel::attemptLogin,
                    onClearError = mainViewModel::clearLoginError
                )
            }
            is SessionUiState.LoggedIn -> {
                when (session.role) {
                    UserRole.OWNER -> {
                        val ownerViewModel: OwnerViewModel = viewModel(
                            key = "owner_vm",
                            factory = OwnerViewModel.Factory(repository)
                        )
                        val ownerState by ownerViewModel.uiState.collectAsStateWithLifecycle()
                        OwnerWorkspaceScreen(
                            state = ownerState,
                            viewModel = ownerViewModel,
                            themeMode = themeMode,
                            isDarkTheme = isDarkActive,
                            onSelectThemeMode = mainViewModel::setThemeMode,
                            onToggleTheme = { mainViewModel.toggleDarkMode(isDarkActive) },
                            onLogout = mainViewModel::logout
                        )
                    }
                    UserRole.KARYAWAN -> {
                        val employeeViewModel: EmployeeViewModel = viewModel(
                            key = "employee_vm",
                            factory = EmployeeViewModel.Factory(repository)
                        )
                        val employeeState by employeeViewModel.uiState.collectAsStateWithLifecycle()
                        EmployeeWorkspaceScreen(
                            state = employeeState,
                            viewModel = employeeViewModel,
                            isDarkTheme = isDarkActive,
                            onToggleTheme = { mainViewModel.toggleDarkMode(isDarkActive) },
                            onLogout = mainViewModel::logout
                        )
                    }
                }
            }
        }
    }
}
