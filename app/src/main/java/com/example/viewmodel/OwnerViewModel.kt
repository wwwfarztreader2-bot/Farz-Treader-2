package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ActivityLogEntity
import com.example.data.local.AttendanceRecordEntity
import com.example.data.local.AttendanceType
import com.example.data.local.CashierTransactionWithItems
import com.example.data.local.CategoryEntity
import com.example.data.local.EmployeeEntity
import com.example.data.local.ProductEntity
import com.example.data.local.PulsaStatus
import com.example.data.local.PulsaTransactionEntity
import com.example.data.local.ReportPeriod
import com.example.data.local.UserRole
import com.example.data.repository.FarnadRepository
import com.example.util.FinancialReportData
import com.example.util.FormatUtils
import com.example.util.SoldProductSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

enum class ChartRange(val label: String) {
    HARIAN("Harian"),
    MINGGUAN("Mingguan"),
    BULANAN("Bulanan")
}

data class SalesChartPoint(
    val label: String,
    val goodsSales: Long,
    val pulsaAdminRevenue: Long
)

data class OwnerUiState(
    val isLoading: Boolean = true,
    val todaySalesTotal: Long = 0L,
    val todayTransactionsCount: Int = 0,
    val todayCashierCount: Int = 0,
    val todayGoodsRevenue: Long = 0L,
    val todayGoodsProfit: Long = 0L,
    val todayPulsaCount: Int = 0,
    val todayPulsaNominal: Long = 0L,
    val todayPulsaAdminFee: Long = 0L,
    val lowStockProducts: List<ProductEntity> = emptyList(),
    val outOfStockProducts: List<ProductEntity> = emptyList(),
    val todayCheckedInCount: Int = 0,
    val todaySickCount: Int = 0,
    val todayLeaveCount: Int = 0,
    val activeEmployeesCount: Int = 0,
    val selectedChartRange: ChartRange = ChartRange.HARIAN,
    val chartPoints: List<SalesChartPoint> = emptyList(),
    val allProducts: List<ProductEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val allEmployees: List<EmployeeEntity> = emptyList(),
    val attendanceRecords: List<AttendanceRecordEntity> = emptyList(),
    val activityLogs: List<ActivityLogEntity> = emptyList(),
    val reportPeriod: ReportPeriod = ReportPeriod.HARIAN,
    val customStartMillis: Long = FormatUtils.daysAgoStartMillis(6),
    val customEndMillis: Long = FormatUtils.endOfDayMillis(),
    val reportData: FinancialReportData = emptyReport(),
    val feedbackMessage: String? = null,
    val isErrorFeedback: Boolean = false
)

private fun emptyReport() = FinancialReportData(
    periodTitle = "Hari Ini",
    totalTransactions = 0,
    cashierCount = 0,
    pulsaSuccessCount = 0,
    pulsaPendingCount = 0,
    pulsaCanceledCount = 0,
    totalGoodsSales = 0L,
    totalGoodsCost = 0L,
    totalGoodsProfit = 0L,
    totalPulsaNominal = 0L,
    totalPulsaAdminFee = 0L,
    totalPulsaPayment = 0L,
    totalGrossIncome = 0L,
    totalNetProfit = 0L,
    soldProducts = emptyList(),
    cashierTransactions = emptyList(),
    pulsaTransactions = emptyList()
)

private data class FilterState(
    val chartRange: ChartRange = ChartRange.HARIAN,
    val reportPeriod: ReportPeriod = ReportPeriod.HARIAN,
    val customStartMillis: Long = FormatUtils.daysAgoStartMillis(6),
    val customEndMillis: Long = FormatUtils.endOfDayMillis(),
    val feedbackMessage: String? = null,
    val isErrorFeedback: Boolean = false
)

private data class CoreStreams(
    val products: List<ProductEntity>,
    val categories: List<CategoryEntity>,
    val employees: List<EmployeeEntity>,
    val cashierTx: List<CashierTransactionWithItems>,
    val pulsaTx: List<PulsaTransactionEntity>
)

private data class AuditStreams(
    val attendance: List<AttendanceRecordEntity>,
    val logs: List<ActivityLogEntity>
)

class OwnerViewModel(private val repository: FarnadRepository) : ViewModel() {

    private val filterState = MutableStateFlow(FilterState())

    private val coreStreams = combine(
        repository.allProducts,
        repository.categories,
        repository.allEmployees,
        repository.cashierTransactions,
        repository.pulsaTransactions
    ) { products, categories, employees, cashierTx, pulsaTx ->
        CoreStreams(products, categories, employees, cashierTx, pulsaTx)
    }

    private val auditStreams = combine(
        repository.attendanceRecords,
        repository.activityLogs
    ) { attendance, logs ->
        AuditStreams(attendance, logs)
    }

    val uiState: StateFlow<OwnerUiState> = combine(
        coreStreams,
        auditStreams,
        filterState
    ) { core, audit, filter ->
        buildOwnerUiState(core, audit, filter)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = OwnerUiState(isLoading = true)
    )

    fun setChartRange(range: ChartRange) {
        filterState.value = filterState.value.copy(chartRange = range)
    }

    fun setReportPeriod(period: ReportPeriod) {
        filterState.value = filterState.value.copy(reportPeriod = period)
    }

    fun setCustomDateRange(startMillis: Long, endMillis: Long) {
        val safeStart = FormatUtils.startOfDayMillis(minOf(startMillis, endMillis))
        val safeEnd = FormatUtils.endOfDayMillis(maxOf(startMillis, endMillis))
        filterState.value = filterState.value.copy(
            reportPeriod = ReportPeriod.RENTANG,
            customStartMillis = safeStart,
            customEndMillis = safeEnd
        )
    }

    fun clearFeedback() {
        filterState.value = filterState.value.copy(feedbackMessage = null, isErrorFeedback = false)
    }

    private fun postFeedback(message: String, isError: Boolean = false) {
        filterState.value = filterState.value.copy(feedbackMessage = message, isErrorFeedback = isError)
    }

    fun addCategory(name: String) {
        viewModelScope.launch {
            runCatching { repository.addCategory(name) }
                .onSuccess { postFeedback("Kategori '$name' berhasil ditambahkan") }
                .onFailure { postFeedback(it.message ?: "Gagal menambah kategori", true) }
        }
    }

    fun addProduct(
        name: String,
        category: CategoryEntity,
        costPrice: Long,
        sellPrice: Long,
        stock: Int,
        minStock: Int
    ) {
        viewModelScope.launch {
            runCatching {
                repository.addProduct(name, category, costPrice, sellPrice, stock, minStock)
            }.onSuccess {
                postFeedback("Barang '$name' berhasil ditambahkan")
            }.onFailure {
                postFeedback(it.message ?: "Gagal menambahkan barang", true)
            }
        }
    }

    fun updateProduct(
        product: ProductEntity,
        newName: String,
        category: CategoryEntity,
        newCostPrice: Long,
        newSellPrice: Long,
        newMinStock: Int
    ) {
        viewModelScope.launch {
            runCatching {
                repository.updateProductDetails(
                    product = product,
                    newName = newName,
                    category = category,
                    newCostPrice = newCostPrice,
                    newSellPrice = newSellPrice,
                    newMinStock = newMinStock
                )
            }.onSuccess {
                postFeedback("Data barang '$newName' diperbarui")
            }.onFailure {
                postFeedback(it.message ?: "Gagal mengubah barang", true)
            }
        }
    }

    fun adjustStock(product: ProductEntity, newStock: Int, reason: String) {
        viewModelScope.launch {
            runCatching {
                repository.adjustProductStock(product, newStock, reason)
            }.onSuccess {
                postFeedback("Stok '${product.name}' diperbarui menjadi $newStock")
            }.onFailure {
                postFeedback(it.message ?: "Gagal memperbarui stok", true)
            }
        }
    }

    fun toggleProductActive(product: ProductEntity) {
        viewModelScope.launch {
            runCatching { repository.toggleProductActive(product) }
                .onSuccess {
                    val label = if (product.isActive) "dinonaktifkan" else "diaktifkan"
                    postFeedback("Barang '${product.name}' $label")
                }
                .onFailure { postFeedback(it.message ?: "Gagal mengubah status barang", true) }
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            runCatching { repository.deleteProduct(product) }
                .onSuccess { postFeedback("Barang '${product.name}' dihapus") }
                .onFailure { postFeedback(it.message ?: "Gagal menghapus barang", true) }
        }
    }

    fun addEmployee(name: String, position: String) {
        viewModelScope.launch {
            runCatching { repository.addEmployee(name, position) }
                .onSuccess { postFeedback("Karyawan '$name' berhasil ditambahkan") }
                .onFailure { postFeedback(it.message ?: "Gagal menambahkan karyawan", true) }
        }
    }

    fun renameEmployee(employee: EmployeeEntity, newName: String, newPosition: String) {
        viewModelScope.launch {
            runCatching { repository.renameEmployee(employee, newName, newPosition) }
                .onSuccess { postFeedback("Data karyawan diperbarui menjadi '$newName'") }
                .onFailure { postFeedback(it.message ?: "Gagal mengubah nama karyawan", true) }
        }
    }

    fun toggleEmployeeActive(employee: EmployeeEntity) {
        viewModelScope.launch {
            runCatching { repository.toggleEmployeeActive(employee) }
                .onSuccess {
                    val status = if (employee.isActive) "dinonaktifkan" else "diaktifkan"
                    postFeedback("Karyawan '${employee.name}' $status")
                }
                .onFailure { postFeedback(it.message ?: "Gagal mengubah status karyawan", true) }
        }
    }

    fun updateRolePassword(role: UserRole, newPassword: String) {
        viewModelScope.launch {
            runCatching { repository.updateRolePassword(role, newPassword) }
                .onSuccess { postFeedback("Kata sandi ${role.label} berhasil diperbarui") }
                .onFailure { postFeedback(it.message ?: "Gagal mengganti kata sandi", true) }
        }
    }

    private fun buildOwnerUiState(
        core: CoreStreams,
        audit: AuditStreams,
        filter: FilterState
    ): OwnerUiState {
        val now = System.currentTimeMillis()
        val todayStart = FormatUtils.startOfDayMillis(now)
        val todayEnd = FormatUtils.endOfDayMillis(now)
        val todayKey = FormatUtils.formatDateKey(now)

        val todayCashier = core.cashierTx.filter { it.transaction.createdAtMillis in todayStart..todayEnd }
        val todayPulsa = core.pulsaTx.filter { it.createdAtMillis in todayStart..todayEnd }
        val todayPulsaSuccess = todayPulsa.filter { it.status == PulsaStatus.BERHASIL.label }

        val todayGoodsRevenue = todayCashier.sumOf { it.transaction.totalAmount }
        val todayGoodsProfit = todayCashier.sumOf { it.transaction.totalProfit }
        val todayPulsaNominal = todayPulsaSuccess.sumOf { it.nominal }
        val todayPulsaAdmin = todayPulsaSuccess.sumOf { it.adminFee }

        val activeProducts = core.products.filter { it.isActive }
        val outOfStock = activeProducts.filter { it.stock <= 0 }
        val lowStock = activeProducts.filter { it.stock in 1..it.minStock }

        val todayAttendance = audit.attendance.filter { it.dateKey == todayKey }
        val todayCheckedIn = todayAttendance.count { it.attendanceType == AttendanceType.HADIR.key }
        val todaySick = todayAttendance.count { it.attendanceType == AttendanceType.SAKIT.key }
        val todayLeave = todayAttendance.count { it.attendanceType == AttendanceType.IZIN.key }
        val activeEmployeesCount = core.employees.count { it.isActive }

        val chartPoints = buildChartPoints(
            range = filter.chartRange,
            cashierTx = core.cashierTx,
            pulsaTx = core.pulsaTx,
            nowMillis = now
        )

        val (periodStart, periodEnd, periodTitle) = when (filter.reportPeriod) {
            ReportPeriod.HARIAN -> Triple(todayStart, todayEnd, "Harian (${FormatUtils.formatIndonesianDate(now)})")
            ReportPeriod.MINGGUAN -> {
                val start = FormatUtils.daysAgoStartMillis(6, now)
                Triple(start, todayEnd, "Mingguan (${FormatUtils.formatShortDate(start)} - ${FormatUtils.formatShortDate(now)})")
            }
            ReportPeriod.BULANAN -> {
                val start = FormatUtils.daysAgoStartMillis(29, now)
                Triple(start, todayEnd, "Bulanan (30 Hari Terakhir)")
            }
            ReportPeriod.RENTANG -> Triple(
                filter.customStartMillis,
                filter.customEndMillis,
                "${FormatUtils.formatShortDate(filter.customStartMillis)} - ${FormatUtils.formatShortDate(filter.customEndMillis)}"
            )
        }

        val periodCashier = core.cashierTx.filter { it.transaction.createdAtMillis in periodStart..periodEnd }
        val periodPulsa = core.pulsaTx.filter { it.createdAtMillis in periodStart..periodEnd }
        val periodPulsaSuccess = periodPulsa.filter { it.status == PulsaStatus.BERHASIL.label }
        val periodPulsaPending = periodPulsa.count { it.status == PulsaStatus.PENDING.label }
        val periodPulsaCanceled = periodPulsa.count { it.status == PulsaStatus.DIBATALKAN.label }

        val totalGoodsSales = periodCashier.sumOf { it.transaction.totalAmount }
        val totalGoodsCost = periodCashier.sumOf { it.transaction.totalCost }
        val totalGoodsProfit = periodCashier.sumOf { it.transaction.totalProfit }
        val totalPulsaNominal = periodPulsaSuccess.sumOf { it.nominal }
        val totalPulsaAdminFee = periodPulsaSuccess.sumOf { it.adminFee }
        val totalPulsaPayment = periodPulsaSuccess.sumOf { it.totalPayment }

        val soldProducts = periodCashier
            .flatMap { it.items }
            .groupBy { it.productName }
            .map { (name, items) ->
                SoldProductSummary(
                    productName = name,
                    quantitySold = items.sumOf { it.quantity },
                    totalRevenue = items.sumOf { it.subtotal },
                    totalProfit = items.sumOf { it.profit }
                )
            }
            .sortedByDescending { it.quantitySold }

        val reportData = FinancialReportData(
            periodTitle = periodTitle,
            totalTransactions = periodCashier.size + periodPulsaSuccess.size,
            cashierCount = periodCashier.size,
            pulsaSuccessCount = periodPulsaSuccess.size,
            pulsaPendingCount = periodPulsaPending,
            pulsaCanceledCount = periodPulsaCanceled,
            totalGoodsSales = totalGoodsSales,
            totalGoodsCost = totalGoodsCost,
            totalGoodsProfit = totalGoodsProfit,
            totalPulsaNominal = totalPulsaNominal,
            totalPulsaAdminFee = totalPulsaAdminFee,
            totalPulsaPayment = totalPulsaPayment,
            totalGrossIncome = totalGoodsSales + totalPulsaPayment,
            // Profit = (Goods sell - Goods cost) + Pulsa admin fee ONLY (nominal pulsa is NOT profit)
            totalNetProfit = totalGoodsProfit + totalPulsaAdminFee,
            soldProducts = soldProducts,
            cashierTransactions = periodCashier,
            pulsaTransactions = periodPulsa
        )

        return OwnerUiState(
            isLoading = false,
            todaySalesTotal = todayGoodsRevenue + todayPulsaAdmin,
            todayTransactionsCount = todayCashier.size + todayPulsaSuccess.size,
            todayCashierCount = todayCashier.size,
            todayGoodsRevenue = todayGoodsRevenue,
            todayGoodsProfit = todayGoodsProfit,
            todayPulsaCount = todayPulsaSuccess.size,
            todayPulsaNominal = todayPulsaNominal,
            todayPulsaAdminFee = todayPulsaAdmin,
            lowStockProducts = lowStock,
            outOfStockProducts = outOfStock,
            todayCheckedInCount = todayCheckedIn,
            todaySickCount = todaySick,
            todayLeaveCount = todayLeave,
            activeEmployeesCount = activeEmployeesCount,
            selectedChartRange = filter.chartRange,
            chartPoints = chartPoints,
            allProducts = core.products,
            categories = core.categories,
            allEmployees = core.employees,
            attendanceRecords = audit.attendance,
            activityLogs = audit.logs,
            reportPeriod = filter.reportPeriod,
            customStartMillis = filter.customStartMillis,
            customEndMillis = filter.customEndMillis,
            reportData = reportData,
            feedbackMessage = filter.feedbackMessage,
            isErrorFeedback = filter.isErrorFeedback
        )
    }

    private fun buildChartPoints(
        range: ChartRange,
        cashierTx: List<CashierTransactionWithItems>,
        pulsaTx: List<PulsaTransactionEntity>,
        nowMillis: Long
    ): List<SalesChartPoint> {
        val wib = TimeZone.getTimeZone("Asia/Jakarta")
        val successPulsa = pulsaTx.filter { it.status == PulsaStatus.BERHASIL.label }

        return when (range) {
            ChartRange.HARIAN -> {
                (6 downTo 0).map { daysAgo ->
                    val start = FormatUtils.daysAgoStartMillis(daysAgo, nowMillis)
                    val end = FormatUtils.endOfDayMillis(start)
                    val label = FormatUtils.formatShortDate(start)
                    val goods = cashierTx.filter { it.transaction.createdAtMillis in start..end }
                        .sumOf { it.transaction.totalAmount }
                    val admin = successPulsa.filter { it.createdAtMillis in start..end }
                        .sumOf { it.adminFee }
                    SalesChartPoint(label, goods, admin)
                }
            }
            ChartRange.MINGGUAN -> {
                (3 downTo 0).map { weeksAgo ->
                    val start = FormatUtils.daysAgoStartMillis((weeksAgo + 1) * 7 - 1, nowMillis)
                    val end = FormatUtils.endOfDayMillis(FormatUtils.daysAgoStartMillis(weeksAgo * 7, nowMillis))
                    val label = "M-${4 - weeksAgo}"
                    val goods = cashierTx.filter { it.transaction.createdAtMillis in start..end }
                        .sumOf { it.transaction.totalAmount }
                    val admin = successPulsa.filter { it.createdAtMillis in start..end }
                        .sumOf { it.adminFee }
                    SalesChartPoint(label, goods, admin)
                }
            }
            ChartRange.BULANAN -> {
                val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
                (5 downTo 0).map { monthsAgo ->
                    val calStart = Calendar.getInstance(wib).apply {
                        timeInMillis = nowMillis
                        add(Calendar.MONTH, -monthsAgo)
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val calEnd = (calStart.clone() as Calendar).apply {
                        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }
                    val start = calStart.timeInMillis
                    val end = calEnd.timeInMillis
                    val label = monthNames[calStart.get(Calendar.MONTH)]
                    val goods = cashierTx.filter { it.transaction.createdAtMillis in start..end }
                        .sumOf { it.transaction.totalAmount }
                    val admin = successPulsa.filter { it.createdAtMillis in start..end }
                        .sumOf { it.adminFee }
                    SalesChartPoint(label, goods, admin)
                }
            }
        }
    }

    class Factory(private val repository: FarnadRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OwnerViewModel(repository) as T
        }
    }
}
