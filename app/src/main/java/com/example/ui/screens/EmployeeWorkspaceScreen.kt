package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.local.AttendanceType
import com.example.data.local.EmployeeEntity
import com.example.hardware.CameraCaptureOverlay
import com.example.hardware.LocationFetchResult
import com.example.hardware.LocationHelper
import com.example.ui.components.AppIcons
import com.example.ui.components.AttendanceResultCard
import com.example.ui.components.FarnadLogoBadge
import com.example.ui.theme.BrandRed
import com.example.ui.theme.BrandRedDark
import com.example.ui.theme.CardDark
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedContainer
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberContainer
import com.example.viewmodel.EmployeeViewModel
import com.example.viewmodel.EmployeeWorkspaceUiState
import kotlinx.coroutines.launch
import java.io.File

enum class EmployeeTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    ABSENSI("Absensi", AppIcons.CameraAltFilled, AppIcons.CameraAltOutlined),
    KASIR("Kasir", AppIcons.PointOfSaleFilled, AppIcons.PointOfSaleOutlined),
    PULSA("Pulsa & Digital", AppIcons.PhoneAndroidFilled, AppIcons.PhoneAndroidOutlined)
}

private data class PendingAttendanceSession(
    val isCheckOut: Boolean,
    val employee: EmployeeEntity,
    val lat: Double,
    val lng: Double,
    val address: String
)

private data class LeaveCameraRequest(
    val employee: EmployeeEntity,
    val type: AttendanceType,
    val onCaptured: (String) -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeWorkspaceScreen(
    state: EmployeeWorkspaceUiState,
    viewModel: EmployeeViewModel,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onLogout: () -> Unit
) {
    var activeTab by rememberSaveable { mutableStateOf(EmployeeTab.ABSENSI) }
    val snackbarHostState = remember { SnackbarHostState() }
    var activeCameraSession by remember { mutableStateOf<PendingAttendanceSession?>(null) }
    var activeLeaveCamera by remember { mutableStateOf<LeaveCameraRequest?>(null) }

    BackHandler(enabled = activeTab != EmployeeTab.ABSENSI && activeCameraSession == null && activeLeaveCamera == null) {
        activeTab = EmployeeTab.ABSENSI
    }

    LaunchedEffect(state.feedbackMessage) {
        val msg = state.feedbackMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 600.dp

        val sidebarBg = if (isDarkTheme) CardDark else BrandRedDark
        val selectedPillBg = if (isDarkTheme) BrandRed else Color.White
        val selectedPillFg = if (isDarkTheme) Color.White else BrandRedDark

        if (isWide) {
            Row(modifier = Modifier.fillMaxSize()) {
                Surface(
                    color = sidebarBg,
                    modifier = Modifier
                        .width(236.dp)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .padding(14.dp)
                    ) {
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_farnad_logo),
                                    contentDescription = "Logo FARNAD CELL",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(34.dp)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Mode Karyawan",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandRedDark
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        EmployeeTab.entries.forEach { tab ->
                            val selected = activeTab == tab
                            NavigationDrawerItem(
                                selected = selected,
                                onClick = { activeTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label, fontWeight = FontWeight.SemiBold) },
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = selectedPillBg,
                                    selectedIconColor = selectedPillFg,
                                    selectedTextColor = selectedPillFg,
                                    unselectedContainerColor = Color.Transparent,
                                    unselectedIconColor = Color.White.copy(alpha = 0.88f),
                                    unselectedTextColor = Color.White.copy(alpha = 0.88f)
                                ),
                                modifier = Modifier
                                    .padding(vertical = 2.dp)
                                    .testTag("employee_nav_${tab.name.lowercase()}")
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.16f))
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = onToggleTheme,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.14f),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("employee_theme_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme) AppIcons.LightMode else AppIcons.DarkMode,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(if (isDarkTheme) "Mode Terang" else "Mode Gelap")
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = onLogout,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.14f),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("employee_logout_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Keluar")
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.weight(1f),
                    snackbarHost = {
                        SnackbarHost(snackbarHostState) { data ->
                            Snackbar(
                                snackbarData = data,
                                containerColor = if (state.isErrorFeedback) DangerRed else BrandRed,
                                contentColor = Color.White
                            )
                        }
                    },
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text("Karyawan • ${activeTab.label}", fontWeight = FontWeight.Bold) },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                ) { innerPadding ->
                    EmployeeTabBody(
                        activeTab = activeTab,
                        state = state,
                        viewModel = viewModel,
                        isWide = true,
                        isDarkTheme = isDarkTheme,
                        onLaunchCameraWithLocation = { session -> activeCameraSession = session },
                        onLaunchLeaveCamera = { req -> activeLeaveCamera = req },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        } else {
            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = {
                    SnackbarHost(snackbarHostState) { data ->
                        Snackbar(
                            snackbarData = data,
                            containerColor = if (state.isErrorFeedback) DangerRed else BrandRed,
                            contentColor = Color.White
                        )
                    }
                },
                topBar = {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FarnadLogoBadge(compact = true)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(color = BrandRed, shape = RoundedCornerShape(8.dp)) {
                                    Text(
                                        text = "KARYAWAN",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                IconButton(
                                    onClick = onToggleTheme,
                                    modifier = Modifier.testTag("employee_theme_toggle_button")
                                ) {
                                    Icon(
                                        imageVector = if (isDarkTheme) AppIcons.LightMode else AppIcons.DarkMode,
                                        contentDescription = if (isDarkTheme) "Aktifkan Mode Terang" else "Aktifkan Mode Gelap",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(
                                    onClick = onLogout,
                                    modifier = Modifier.testTag("employee_logout_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                        contentDescription = "Keluar",
                                        tint = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                        }
                    }
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        EmployeeTab.entries.forEach { tab ->
                            val selected = activeTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { activeTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.tertiary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.testTag("employee_nav_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            ) { innerPadding ->
                EmployeeTabBody(
                    activeTab = activeTab,
                    state = state,
                    viewModel = viewModel,
                    isWide = false,
                    isDarkTheme = isDarkTheme,
                    onLaunchCameraWithLocation = { session -> activeCameraSession = session },
                    onLaunchLeaveCamera = { req -> activeLeaveCamera = req },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }
    }

        // Fullscreen Live CameraX capture overlay (strictly no gallery)
        val cameraSession = activeCameraSession
        if (cameraSession != null) {
            CameraCaptureOverlay(
                employeeName = cameraSession.employee.name,
                modeLabel = if (cameraSession.isCheckOut) "Absensi Pulang" else "Absensi Masuk",
                onPhotoConfirmed = { photoPath ->
                    val todayRecord = state.todayAttendanceForSelected
                    if (cameraSession.isCheckOut && todayRecord != null) {
                        viewModel.submitCheckOut(
                            attendanceRecord = todayRecord,
                            employee = cameraSession.employee,
                            photoPath = photoPath,
                            address = cameraSession.address,
                            latitude = cameraSession.lat,
                            longitude = cameraSession.lng
                        )
                    } else {
                        viewModel.submitCheckIn(
                            employee = cameraSession.employee,
                            photoPath = photoPath,
                            address = cameraSession.address,
                            latitude = cameraSession.lat,
                            longitude = cameraSession.lng
                        )
                    }
                    activeCameraSession = null
                },
                onCancel = { activeCameraSession = null }
            )
        }

        val leaveCam = activeLeaveCamera
        if (leaveCam != null) {
            CameraCaptureOverlay(
                employeeName = leaveCam.employee.name,
                modeLabel = "Bukti ${leaveCam.type.label}",
                onPhotoConfirmed = { photoPath ->
                    leaveCam.onCaptured(photoPath)
                    activeLeaveCamera = null
                },
                onCancel = { activeLeaveCamera = null }
            )
        }
    }
}

@Composable
private fun EmployeeTabBody(
    activeTab: EmployeeTab,
    state: EmployeeWorkspaceUiState,
    viewModel: EmployeeViewModel,
    isWide: Boolean,
    isDarkTheme: Boolean,
    onLaunchCameraWithLocation: (PendingAttendanceSession) -> Unit,
    onLaunchLeaveCamera: (LeaveCameraRequest) -> Unit,
    modifier: Modifier = Modifier
) {
    when (activeTab) {
        EmployeeTab.ABSENSI -> EmployeeAttendanceTab(
            state = state,
            viewModel = viewModel,
            isDarkTheme = isDarkTheme,
            onLaunchCameraWithLocation = onLaunchCameraWithLocation,
            onLaunchLeaveCamera = onLaunchLeaveCamera,
            modifier = modifier
        )
        EmployeeTab.KASIR -> EmployeeCashierTab(
            state = state,
            viewModel = viewModel,
            isWide = isWide,
            modifier = modifier
        )
        EmployeeTab.PULSA -> EmployeePulsaTab(
            state = state,
            viewModel = viewModel,
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeSelectorCard(
    employees: List<EmployeeEntity>,
    selectedEmployee: EmployeeEntity?,
    onSelectEmployee: (EmployeeEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Karyawan Bertugas",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary
            )
            Spacer(Modifier.height(6.dp))
            if (employees.isEmpty()) {
                Surface(
                    color = DangerRedContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Belum ada karyawan aktif. Silakan minta Owner menambahkan karyawan di menu Manajemen Karyawan terlebih dahulu.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DangerRed,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedEmployee?.let { "${it.name} (${it.position})" } ?: "Pilih Nama Karyawan",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                            .testTag("employee_selector_dropdown")
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        employees.forEach { emp ->
                            DropdownMenuItem(
                                text = { Text("${emp.name} — ${emp.position}") },
                                onClick = {
                                    onSelectEmployee(emp)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmployeeAttendanceTab(
    state: EmployeeWorkspaceUiState,
    viewModel: EmployeeViewModel,
    isDarkTheme: Boolean,
    onLaunchCameraWithLocation: (PendingAttendanceSession) -> Unit,
    onLaunchLeaveCamera: (LeaveCameraRequest) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isFetchingGps by remember { mutableStateOf(false) }
    var pendingCheckOutMode by remember { mutableStateOf(false) }
    var permissionWarning by remember { mutableStateOf<String?>(null) }
    var previewPhotoPath by remember { mutableStateOf<String?>(null) }
    var leaveDialogType by remember { mutableStateOf<AttendanceType?>(null) }
    var leaveReason by remember { mutableStateOf("") }
    var leavePhotoPath by remember { mutableStateOf("") }
    var isSubmittingLeaveGps by remember { mutableStateOf(false) }

    fun proceedWithGpsAndCamera(employee: EmployeeEntity, isCheckOut: Boolean) {
        scope.launch {
            isFetchingGps = true
            permissionWarning = null
            when (val gpsResult = LocationHelper.fetchCurrentLocation(context)) {
                is LocationFetchResult.Failure -> {
                    isFetchingGps = false
                    permissionWarning = gpsResult.message
                }
                is LocationFetchResult.Success -> {
                    isFetchingGps = false
                    onLaunchCameraWithLocation(
                        PendingAttendanceSession(
                            isCheckOut = isCheckOut,
                            employee = employee,
                            lat = gpsResult.latitude,
                            lng = gpsResult.longitude,
                            address = gpsResult.address
                        )
                    )
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val camGranted = permissions[Manifest.permission.CAMERA] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val locGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (camGranted && locGranted) {
            val emp = state.selectedEmployee
            if (emp != null) {
                proceedWithGpsAndCamera(emp, pendingCheckOutMode)
            }
        } else {
            permissionWarning = buildString {
                append("Izin Kamera dan Lokasi GPS wajib diaktifkan untuk absensi foto asli. ")
                if (!camGranted) append("Izin Kamera ditolak. ")
                if (!locGranted) append("Izin Lokasi GPS ditolak. ")
                append("Absensi tidak dapat dibuat tanpa kamera & lokasi GPS.")
            }
        }
    }

    fun startAttendanceFlow(isCheckOut: Boolean) {
        val emp = state.selectedEmployee
        if (emp == null) {
            viewModel.showError("Pilih nama karyawan terlebih dahulu")
            return
        }
        pendingCheckOutMode = isCheckOut

        val camGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (camGranted && (fineGranted || coarseGranted)) {
            proceedWithGpsAndCamera(emp, isCheckOut)
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LazyColumn(
        modifier = modifier.background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            EmployeeSelectorCard(
                employees = state.activeEmployees,
                selectedEmployee = state.selectedEmployee,
                onSelectEmployee = viewModel::selectEmployee
            )
        }

        if (permissionWarning != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DangerRedContainer),
                    border = BorderStroke(1.dp, DangerRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Izin / GPS Diperlukan",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = DangerRed
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = permissionWarning!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF450A0A)
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(intent)
                                }
                            ) {
                                Text("Pengaturan Izin")
                            }
                            Button(
                                onClick = { startAttendanceFlow(pendingCheckOutMode) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BrandRed,
                                    contentColor = Color.White
                                )
                            ) {
                                Text("Coba Lagi")
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = if (isDarkTheme) BrandRedDark else BrandRed),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "ABSENSI FOTO REAL-TIME + GPS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Kamera langsung (tanpa galeri), lokasi GPS otomatis, dan kode unik FNC otomatis.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.88f)
                    )
                    Spacer(Modifier.height(14.dp))

                    val todayRec = state.todayAttendanceForSelected
                    if (isFetchingGps || state.isSubmitting) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = if (isFetchingGps) "Mengambil titik lokasi GPS..." else "Menyimpan absensi...",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else if (todayRec == null) {
                        Button(
                            onClick = { startAttendanceFlow(isCheckOut = false) },
                            enabled = state.selectedEmployee != null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = BrandRedDark,
                                disabledContainerColor = Color.White.copy(alpha = 0.5f),
                                disabledContentColor = BrandRedDark.copy(alpha = 0.6f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("attendance_check_in_button")
                        ) {
                            Icon(AppIcons.CameraAltFilled, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "ABSENSI MASUK",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val emp = state.selectedEmployee
                                    if (emp == null) {
                                        viewModel.showError("Pilih nama karyawan terlebih dahulu")
                                    } else {
                                        leaveReason = ""
                                        leavePhotoPath = ""
                                        leaveDialogType = AttendanceType.SAKIT
                                    }
                                },
                                enabled = state.selectedEmployee != null,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = WarningAmberContainer,
                                    contentColor = Color(0xFF92400E),
                                    disabledContainerColor = WarningAmberContainer.copy(alpha = 0.5f),
                                    disabledContentColor = Color(0xFF92400E).copy(alpha = 0.6f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("attendance_sick_button")
                            ) {
                                Icon(AppIcons.MedicalServices, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("ABSEN SAKIT", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    val emp = state.selectedEmployee
                                    if (emp == null) {
                                        viewModel.showError("Pilih nama karyawan terlebih dahulu")
                                    } else {
                                        leaveReason = ""
                                        leavePhotoPath = ""
                                        leaveDialogType = AttendanceType.IZIN
                                    }
                                },
                                enabled = state.selectedEmployee != null,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFDBEAFE),
                                    contentColor = Color(0xFF1E40AF),
                                    disabledContainerColor = Color(0xFFDBEAFE).copy(alpha = 0.5f),
                                    disabledContentColor = Color(0xFF1E40AF).copy(alpha = 0.6f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("attendance_leave_button")
                            ) {
                                Icon(AppIcons.EventNote, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("ABSEN IZIN", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        val todayType = AttendanceType.fromKey(todayRec.attendanceType)
                        if (todayType == AttendanceType.HADIR && todayRec.checkOutMillis == null) {
                            Button(
                                onClick = { startAttendanceFlow(isCheckOut = true) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ProfitGreen,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .testTag("attendance_check_out_button")
                            ) {
                                Icon(AppIcons.Logout, contentDescription = null)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "ABSENSI PULANG",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else if (todayType == AttendanceType.HADIR) {
                            Surface(
                                color = ProfitGreen.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Absensi Masuk & Pulang hari ini sudah tercatat (${todayRec.attendanceCode}).",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        } else {
                            Surface(
                                color = Color.White.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Absensi hari ini tercatat sebagai ${todayType.label} (${todayRec.attendanceCode}).",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        state.todayAttendanceForSelected?.let { todayRec ->
            item {
                Text(
                    text = "Bukti Absensi Hari Ini",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                AttendanceResultCard(
                    record = todayRec,
                    onPhotoClick = { path -> previewPhotoPath = path }
                )
            }
        }

        if (state.recentAttendance.isNotEmpty()) {
            item {
                Text(
                    text = "Riwayat Absensi Terbaru",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(state.recentAttendance, key = { it.id }) { rec ->
                AttendanceResultCard(
                    record = rec,
                    onPhotoClick = { path -> previewPhotoPath = path }
                )
            }
        }
    }

    previewPhotoPath?.let { path ->
        Dialog(onDismissRequest = { previewPhotoPath = null }) {
            Card(shape = MaterialTheme.shapes.large) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Foto Absensi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    AsyncImage(
                        model = File(path),
                        contentDescription = "Foto Absensi Lengkap",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { previewPhotoPath = null },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandRed,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Tutup")
                    }
                }
            }
        }
    }

    val activeLeaveType = leaveDialogType
    val selectedEmp = state.selectedEmployee
    if (activeLeaveType != null && selectedEmp != null) {
        val leaveCamPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                onLaunchLeaveCamera(
                    LeaveCameraRequest(
                        employee = selectedEmp,
                        type = activeLeaveType,
                        onCaptured = { capturedPath -> leavePhotoPath = capturedPath }
                    )
                )
            } else {
                viewModel.showError("Izin kamera ditolak. Foto bukti bersifat opsional untuk Sakit/Izin.")
            }
        }

        val presets = if (activeLeaveType == AttendanceType.SAKIT) {
            listOf("Kurang enak badan / demam", "Istirahat sesuai anjuran dokter", "Periksa ke klinik / RS")
        } else {
            listOf("Keperluan keluarga", "Urusan pribadi mendesak", "Acara keluarga / keagamaan")
        }

        AlertDialog(
            onDismissRequest = {
                if (!isSubmittingLeaveGps) leaveDialogType = null
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (activeLeaveType == AttendanceType.SAKIT) AppIcons.MedicalServices else AppIcons.EventNote,
                        contentDescription = null,
                        tint = if (activeLeaveType == AttendanceType.SAKIT) WarningAmber else InfoBlue
                    )
                    Text(
                        text = "Form Absen ${activeLeaveType.label}",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Karyawan: ${selectedEmp.name} (${selectedEmp.position})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presets.forEach { preset ->
                            FilterChip(
                                selected = leaveReason == preset,
                                onClick = { leaveReason = preset },
                                label = { Text(preset, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandRed,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                    OutlinedTextField(
                        value = leaveReason,
                        onValueChange = { leaveReason = it },
                        label = { Text("Keterangan / Alasan ${activeLeaveType.label}") },
                        placeholder = { Text("Tuliskan alasan singkat...") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("leave_reason_input")
                    )

                    if (leavePhotoPath.isNotBlank() && File(leavePhotoPath).exists()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AsyncImage(
                                model = File(leavePhotoPath),
                                contentDescription = "Foto Bukti ${activeLeaveType.label}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Foto bukti terlampir",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ProfitGreen
                                )
                                TextButton(
                                    onClick = { leavePhotoPath = "" },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Hapus Foto", color = DangerRed)
                                }
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                val camGranted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                                if (camGranted) {
                                    onLaunchLeaveCamera(
                                        LeaveCameraRequest(
                                            employee = selectedEmp,
                                            type = activeLeaveType,
                                            onCaptured = { capturedPath -> leavePhotoPath = capturedPath }
                                        )
                                    )
                                } else {
                                    leaveCamPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("leave_attach_photo_button")
                        ) {
                            Icon(AppIcons.CameraAltOutlined, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Foto Surat / Bukti (Opsional)")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isSubmittingLeaveGps = true
                            val hasLocPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED ||
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED

                            var lat = 0.0
                            var lng = 0.0
                            var address = "Pengajuan ${activeLeaveType.label.lowercase()} (tanpa lokasi GPS)"
                            if (hasLocPermission) {
                                when (val gpsResult = LocationHelper.fetchCurrentLocation(context)) {
                                    is LocationFetchResult.Success -> {
                                        lat = gpsResult.latitude
                                        lng = gpsResult.longitude
                                        address = gpsResult.address
                                    }
                                    is LocationFetchResult.Failure -> Unit
                                }
                            }
                            isSubmittingLeaveGps = false
                            viewModel.submitLeaveAttendance(
                                employee = selectedEmp,
                                type = activeLeaveType,
                                reason = leaveReason,
                                photoPath = leavePhotoPath,
                                address = address,
                                latitude = lat,
                                longitude = lng
                            )
                            leaveDialogType = null
                        }
                    },
                    enabled = leaveReason.isNotBlank() && !isSubmittingLeaveGps && !state.isSubmitting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandRed,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("leave_submit_button")
                ) {
                    if (isSubmittingLeaveGps) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                    } else {
                        Text("Kirim Absen ${activeLeaveType.label}")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { leaveDialogType = null },
                    enabled = !isSubmittingLeaveGps
                ) {
                    Text("Batal")
                }
            }
        )
    }
}
