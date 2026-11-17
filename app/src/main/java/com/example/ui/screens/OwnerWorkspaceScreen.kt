package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import com.example.ui.components.AppIcons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.UserRole
import com.example.ui.components.FarnadLogoBadge
import com.example.ui.components.SalesChartCard
import com.example.ui.components.StatMetricCard
import com.example.ui.components.StockAlertBanner
import com.example.ui.theme.BrandRed
import com.example.ui.theme.BrandRedBright
import com.example.ui.theme.BrandRedDark
import com.example.ui.theme.CardDark
import com.example.ui.theme.CharcoalBrand
import com.example.ui.theme.DangerRed
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.WarningAmber
import com.example.util.FormatUtils
import com.example.viewmodel.OwnerUiState
import com.example.viewmodel.OwnerViewModel

enum class OwnerTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DASHBOARD("Dashboard", AppIcons.DashboardFilled, AppIcons.DashboardOutlined),
    INVENTORY("Barang", AppIcons.Inventory2Filled, AppIcons.Inventory2Outlined),
    REPORTS("Laporan", AppIcons.AssessmentFilled, AppIcons.AssessmentOutlined),
    EMPLOYEES("Karyawan", AppIcons.GroupFilled, AppIcons.GroupOutlined),
    ACTIVITY("Aktivitas", AppIcons.History, AppIcons.History)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerWorkspaceScreen(
    state: OwnerUiState,
    viewModel: OwnerViewModel,
    themeMode: ThemeMode,
    isDarkTheme: Boolean,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onToggleTheme: () -> Unit,
    onLogout: () -> Unit
) {
    var activeTab by rememberSaveable { mutableStateOf(OwnerTab.DASHBOARD) }
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler(enabled = activeTab != OwnerTab.DASHBOARD) {
        activeTab = OwnerTab.DASHBOARD
    }

    LaunchedEffect(state.feedbackMessage) {
        val msg = state.feedbackMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 600.dp

        val sidebarBg = if (isDarkTheme) CardDark else BrandRedDark
        val selectedPillBg = if (isDarkTheme) BrandRed else Color.White
        val selectedPillFg = if (isDarkTheme) Color.White else BrandRedDark

        if (isWide) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Persistent Sidebar for Tablet / Desktop
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
                                    text = "Panel Owner",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandRedDark
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        OwnerTab.entries.forEach { tab ->
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
                                    .testTag("owner_nav_${tab.name.lowercase()}")
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
                                .testTag("owner_theme_toggle_button")
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
                                .testTag("owner_logout_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Keluar")
                        }
                    }
                }

                // Content area
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
                            title = {
                                Text(
                                    text = "Owner • ${activeTab.label}",
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                ) { innerPadding ->
                    OwnerTabBody(
                        activeTab = activeTab,
                        state = state,
                        viewModel = viewModel,
                        themeMode = themeMode,
                        isDarkTheme = isDarkTheme,
                        onSelectThemeMode = onSelectThemeMode,
                        onNavigateTab = { activeTab = it },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        isWide = true
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
                                Surface(
                                    color = BrandRed,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "OWNER",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                IconButton(
                                    onClick = onToggleTheme,
                                    modifier = Modifier.testTag("owner_theme_toggle_button")
                                ) {
                                    Icon(
                                        imageVector = if (isDarkTheme) AppIcons.LightMode else AppIcons.DarkMode,
                                        contentDescription = if (isDarkTheme) "Aktifkan Mode Terang" else "Aktifkan Mode Gelap",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(
                                    onClick = onLogout,
                                    modifier = Modifier.testTag("owner_logout_button")
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
                        OwnerTab.entries.forEach { tab ->
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
                                modifier = Modifier.testTag("owner_nav_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            ) { innerPadding ->
                OwnerTabBody(
                    activeTab = activeTab,
                    state = state,
                    viewModel = viewModel,
                    themeMode = themeMode,
                    isDarkTheme = isDarkTheme,
                    onSelectThemeMode = onSelectThemeMode,
                    onNavigateTab = { activeTab = it },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    isWide = false
                )
            }
        }
    }
}

@Composable
private fun OwnerTabBody(
    activeTab: OwnerTab,
    state: OwnerUiState,
    viewModel: OwnerViewModel,
    themeMode: ThemeMode,
    isDarkTheme: Boolean,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onNavigateTab: (OwnerTab) -> Unit,
    modifier: Modifier = Modifier,
    isWide: Boolean
) {
    if (state.isLoading) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BrandRed)
        }
        return
    }

    when (activeTab) {
        OwnerTab.DASHBOARD -> OwnerDashboardTab(
            state = state,
            isDarkTheme = isDarkTheme,
            onSelectChartRange = viewModel::setChartRange,
            onNavigateTab = onNavigateTab,
            isWide = isWide,
            modifier = modifier
        )
        OwnerTab.INVENTORY -> OwnerInventoryTab(
            state = state,
            viewModel = viewModel,
            modifier = modifier
        )
        OwnerTab.REPORTS -> OwnerReportsTab(
            state = state,
            viewModel = viewModel,
            modifier = modifier
        )
        OwnerTab.EMPLOYEES -> OwnerEmployeesTab(
            state = state,
            viewModel = viewModel,
            modifier = modifier
        )
        OwnerTab.ACTIVITY -> OwnerActivityAndSettingsTab(
            state = state,
            themeMode = themeMode,
            onSelectThemeMode = onSelectThemeMode,
            onUpdatePassword = { role, newPass -> viewModel.updateRolePassword(role, newPass) },
            modifier = modifier
        )
    }
}

@Composable
private fun OwnerDashboardTab(
    state: OwnerUiState,
    isDarkTheme: Boolean,
    onSelectChartRange: (com.example.viewmodel.ChartRange) -> Unit,
    onNavigateTab: (OwnerTab) -> Unit,
    isWide: Boolean,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(
                color = if (isDarkTheme) BrandRedDark else BrandRed,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ringkasan Hari Ini",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.88f),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = FormatUtils.formatIndonesianDate(System.currentTimeMillis()),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Pantau transaksi kasir, admin pulsa, stok, & absensi secara real-time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "Laba Barang + Admin",
                                style = MaterialTheme.typography.labelSmall,
                                color = CharcoalBrand.copy(alpha = 0.72f)
                            )
                            Text(
                                text = FormatUtils.formatRupiah(state.todayGoodsProfit + state.todayPulsaAdminFee),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BrandRedDark
                            )
                        }
                    }
                }
            }
        }

        item {
            StockAlertBanner(
                outOfStockCount = state.outOfStockProducts.size,
                lowStockCount = state.lowStockProducts.size,
                onViewInventory = { onNavigateTab(OwnerTab.INVENTORY) }
            )
        }

        item {
            val cols = if (isWide) 4 else 2
            val primaryAccent = if (isDarkTheme) BrandRedBright else BrandRed
            val cards = listOf(
                MetricSpec(
                    title = "Total Penjualan Hari Ini",
                    value = FormatUtils.formatRupiah(state.todaySalesTotal),
                    subtitle = "Omzet Barang + Admin Pulsa",
                    icon = AppIcons.MonetizationOn,
                    accent = primaryAccent,
                    onClick = { onNavigateTab(OwnerTab.REPORTS) }
                ),
                MetricSpec(
                    title = "Jumlah Transaksi Hari Ini",
                    value = "${state.todayTransactionsCount}",
                    subtitle = "${state.todayCashierCount} Kasir • ${state.todayPulsaCount} Pulsa",
                    icon = AppIcons.ReceiptLong,
                    accent = InfoBlue,
                    onClick = { onNavigateTab(OwnerTab.REPORTS) }
                ),
                MetricSpec(
                    title = "Pendapatan Penjualan Barang",
                    value = FormatUtils.formatRupiah(state.todayGoodsRevenue),
                    subtitle = "Laba: ${FormatUtils.formatRupiah(state.todayGoodsProfit)}",
                    icon = AppIcons.ShoppingBag,
                    accent = ProfitGreen,
                    onClick = { onNavigateTab(OwnerTab.REPORTS) }
                ),
                MetricSpec(
                    title = "Total Transaksi Pulsa",
                    value = "${state.todayPulsaCount} Trx",
                    subtitle = "Nominal: ${FormatUtils.formatRupiah(state.todayPulsaNominal)}",
                    icon = AppIcons.PhoneAndroidFilled,
                    accent = InfoBlue,
                    onClick = { onNavigateTab(OwnerTab.REPORTS) }
                ),
                MetricSpec(
                    title = "Total Biaya Admin Pulsa",
                    value = FormatUtils.formatRupiah(state.todayPulsaAdminFee),
                    subtitle = "Pendapatan admin murni",
                    icon = AppIcons.PointOfSaleFilled,
                    accent = primaryAccent,
                    onClick = { onNavigateTab(OwnerTab.REPORTS) }
                ),
                MetricSpec(
                    title = "Stok Hampir Habis",
                    value = "${state.lowStockProducts.size} Barang",
                    subtitle = "Segera lakukan restok",
                    icon = AppIcons.ReportProblem,
                    accent = WarningAmber,
                    onClick = { onNavigateTab(OwnerTab.INVENTORY) }
                ),
                MetricSpec(
                    title = "Stok Kosong",
                    value = "${state.outOfStockProducts.size} Barang",
                    subtitle = "Tidak dapat dijual di kasir",
                    icon = AppIcons.ProductionQuantityLimits,
                    accent = DangerRed,
                    onClick = { onNavigateTab(OwnerTab.INVENTORY) }
                ),
                MetricSpec(
                    title = "Absensi Karyawan Hari Ini",
                    value = "${state.todayCheckedInCount} / ${state.activeEmployeesCount}",
                    subtitle = "Hadir • ${state.todaySickCount} Sakit • ${state.todayLeaveCount} Izin",
                    icon = AppIcons.Badge,
                    accent = ProfitGreen,
                    onClick = { onNavigateTab(OwnerTab.EMPLOYEES) }
                )
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                cards.chunked(cols).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { card ->
                            StatMetricCard(
                                title = card.title,
                                value = card.value,
                                subtitle = card.subtitle,
                                icon = card.icon,
                                accentColor = card.accent,
                                onClick = card.onClick,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(cols - rowItems.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        item {
            SalesChartCard(
                selectedRange = state.selectedChartRange,
                points = state.chartPoints,
                onSelectRange = onSelectChartRange
            )
        }
    }
}

private data class MetricSpec(
    val title: String,
    val value: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color,
    val onClick: () -> Unit
)
