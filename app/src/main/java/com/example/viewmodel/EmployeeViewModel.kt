package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AttendanceRecordEntity
import com.example.data.local.AttendanceType
import com.example.data.local.CartSubmissionItem
import com.example.data.local.CashierTransactionWithItems
import com.example.data.local.CategoryEntity
import com.example.data.local.EmployeeEntity
import com.example.data.local.PaymentMethod
import com.example.data.local.ProductEntity
import com.example.data.local.PulsaStatus
import com.example.data.local.PulsaTransactionEntity
import com.example.data.local.UserRole
import com.example.data.repository.FarnadRepository
import com.example.util.FormatUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CartLineItem(
    val product: ProductEntity,
    val quantity: Int
) {
    val subtotal: Long get() = product.sellPrice * quantity
}

data class EmployeeWorkspaceUiState(
    val activeEmployees: List<EmployeeEntity> = emptyList(),
    val selectedEmployee: EmployeeEntity? = null,
    val todayAttendanceForSelected: AttendanceRecordEntity? = null,
    val recentAttendance: List<AttendanceRecordEntity> = emptyList(),
    val activeProducts: List<ProductEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedCategoryId: Long? = null,
    val cartItems: List<CartLineItem> = emptyList(),
    val cartTotal: Long = 0L,
    val paymentMethod: PaymentMethod = PaymentMethod.TUNAI,
    val cashReceivedInput: String = "",
    val changeAmount: Long = 0L,
    val lastCompletedReceipt: CashierTransactionWithItems? = null,
    val todayPulsaHistory: List<PulsaTransactionEntity> = emptyList(),
    val isSubmitting: Boolean = false,
    val feedbackMessage: String? = null,
    val isErrorFeedback: Boolean = false
)

private data class EmployeeLocalInputs(
    val searchQuery: String = "",
    val selectedCategoryId: Long? = null,
    val cart: Map<Long, Int> = emptyMap(),
    val paymentMethod: PaymentMethod = PaymentMethod.TUNAI,
    val cashReceivedInput: String = "",
    val lastCompletedReceipt: CashierTransactionWithItems? = null,
    val isSubmitting: Boolean = false,
    val feedbackMessage: String? = null,
    val isErrorFeedback: Boolean = false
)

private data class EmployeeDbData(
    val employees: List<EmployeeEntity>,
    val selectedEmployeeId: Long?,
    val attendance: List<AttendanceRecordEntity>,
    val products: List<ProductEntity>,
    val categories: List<CategoryEntity>
)

class EmployeeViewModel(private val repository: FarnadRepository) : ViewModel() {

    private val localInputs = MutableStateFlow(EmployeeLocalInputs())

    private val dbStream = combine(
        repository.activeEmployees,
        repository.selectedEmployeeIdFlow,
        repository.attendanceRecords,
        repository.activeProducts,
        repository.categories
    ) { employees, selectedId, attendance, products, categories ->
        EmployeeDbData(employees, selectedId, attendance, products, categories)
    }

    val uiState: StateFlow<EmployeeWorkspaceUiState> = combine(
        dbStream,
        repository.pulsaTransactions,
        localInputs
    ) { db, pulsaList, inputs ->
        val selectedEmployee = db.employees.firstOrNull { it.id == db.selectedEmployeeId }
            ?: db.employees.firstOrNull()

        val todayKey = FormatUtils.formatDateKey(System.currentTimeMillis())
        val todayStart = FormatUtils.startOfDayMillis()
        val todayEnd = FormatUtils.endOfDayMillis()

        val todayAttendance = if (selectedEmployee != null) {
            db.attendance.firstOrNull { it.employeeId == selectedEmployee.id && it.dateKey == todayKey }
        } else null

        val cartItems = inputs.cart.mapNotNull { (productId, qty) ->
            val product = db.products.firstOrNull { it.id == productId } ?: return@mapNotNull null
            val boundedQty = qty.coerceAtMost(product.stock)
            if (boundedQty <= 0) null else CartLineItem(product, boundedQty)
        }
        val cartTotal = cartItems.sumOf { it.subtotal }
        val cashVal = inputs.cashReceivedInput.filter { it.isDigit() }.toLongOrNull() ?: 0L
        val change = if (inputs.paymentMethod == PaymentMethod.TRANSFER) {
            0L
        } else {
            (cashVal - cartTotal).coerceAtLeast(0L)
        }

        val todayPulsa = pulsaList.filter { it.createdAtMillis in todayStart..todayEnd }

        EmployeeWorkspaceUiState(
            activeEmployees = db.employees,
            selectedEmployee = selectedEmployee,
            todayAttendanceForSelected = todayAttendance,
            recentAttendance = db.attendance.take(20),
            activeProducts = db.products,
            categories = db.categories,
            searchQuery = inputs.searchQuery,
            selectedCategoryId = inputs.selectedCategoryId,
            cartItems = cartItems,
            cartTotal = cartTotal,
            paymentMethod = inputs.paymentMethod,
            cashReceivedInput = inputs.cashReceivedInput,
            changeAmount = change,
            lastCompletedReceipt = inputs.lastCompletedReceipt,
            todayPulsaHistory = todayPulsa,
            isSubmitting = inputs.isSubmitting,
            feedbackMessage = inputs.feedbackMessage,
            isErrorFeedback = inputs.isErrorFeedback
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = EmployeeWorkspaceUiState()
    )

    fun selectEmployee(employee: EmployeeEntity) {
        viewModelScope.launch {
            repository.saveSelectedEmployeeId(employee.id)
        }
    }

    fun clearFeedback() {
        localInputs.value = localInputs.value.copy(feedbackMessage = null, isErrorFeedback = false)
    }

    fun showError(message: String) {
        localInputs.value = localInputs.value.copy(feedbackMessage = message, isErrorFeedback = true)
    }

    // --- Attendance ---

    fun submitCheckIn(
        employee: EmployeeEntity,
        photoPath: String,
        address: String,
        latitude: Double,
        longitude: Double
    ) {
        viewModelScope.launch {
            localInputs.value = localInputs.value.copy(isSubmitting = true)
            runCatching {
                repository.checkIn(employee, photoPath, address, latitude, longitude)
            }.onSuccess { record ->
                localInputs.value = localInputs.value.copy(
                    isSubmitting = false,
                    feedbackMessage = "Absensi Masuk berhasil: ${record.attendanceCode} (${record.checkInTimeFormatted})",
                    isErrorFeedback = false
                )
            }.onFailure { err ->
                localInputs.value = localInputs.value.copy(
                    isSubmitting = false,
                    feedbackMessage = err.message ?: "Gagal menyimpan absensi masuk",
                    isErrorFeedback = true
                )
            }
        }
    }

    fun submitCheckOut(
        attendanceRecord: AttendanceRecordEntity,
        employee: EmployeeEntity,
        photoPath: String,
        address: String,
        latitude: Double,
        longitude: Double
    ) {
        viewModelScope.launch {
            localInputs.value = localInputs.value.copy(isSubmitting = true)
            runCatching {
                repository.checkOut(attendanceRecord.id, employee, photoPath, address, latitude, longitude)
            }.onSuccess { record ->
                localInputs.value = localInputs.value.copy(
                    isSubmitting = false,
                    feedbackMessage = "Absensi Pulang berhasil: ${record.attendanceCode} (${record.checkOutTimeFormatted})",
                    isErrorFeedback = false
                )
            }.onFailure { err ->
                localInputs.value = localInputs.value.copy(
                    isSubmitting = false,
                    feedbackMessage = err.message ?: "Gagal menyimpan absensi pulang",
                    isErrorFeedback = true
                )
            }
        }
    }

    fun submitLeaveAttendance(
        employee: EmployeeEntity,
        type: AttendanceType,
        reason: String,
        photoPath: String = "",
        address: String,
        latitude: Double,
        longitude: Double
    ) {
        viewModelScope.launch {
            localInputs.value = localInputs.value.copy(isSubmitting = true)
            runCatching {
                repository.submitLeaveAttendance(
                    employee = employee,
                    type = type,
                    reason = reason,
                    photoPath = photoPath,
                    address = address,
                    lat = latitude,
                    lng = longitude
                )
            }.onSuccess { record ->
                localInputs.value = localInputs.value.copy(
                    isSubmitting = false,
                    feedbackMessage = "Absensi ${type.label} tercatat: ${record.attendanceCode} (${record.checkInTimeFormatted})",
                    isErrorFeedback = false
                )
            }.onFailure { err ->
                localInputs.value = localInputs.value.copy(
                    isSubmitting = false,
                    feedbackMessage = err.message ?: "Gagal menyimpan absensi ${type.label.lowercase()}",
                    isErrorFeedback = true
                )
            }
        }
    }

    // --- Cashier POS ---

    fun setSearchQuery(query: String) {
        localInputs.value = localInputs.value.copy(searchQuery = query)
    }

    fun selectCategory(categoryId: Long?) {
        localInputs.value = localInputs.value.copy(selectedCategoryId = categoryId)
    }

    fun addToCart(product: ProductEntity) {
        if (product.stock <= 0) {
            showError("Stok ${product.name} sedang kosong")
            return
        }
        val currentMap = localInputs.value.cart.toMutableMap()
        val currentQty = currentMap[product.id] ?: 0
        if (currentQty >= product.stock) {
            showError("Jumlah maksimal sesuai stok tersedia (${product.stock})")
            return
        }
        currentMap[product.id] = currentQty + 1
        localInputs.value = localInputs.value.copy(cart = currentMap)
    }

    fun updateCartQty(productId: Long, newQty: Int) {
        val currentMap = localInputs.value.cart.toMutableMap()
        if (newQty <= 0) {
            currentMap.remove(productId)
        } else {
            currentMap[productId] = newQty
        }
        localInputs.value = localInputs.value.copy(cart = currentMap)
    }

    fun clearCart() {
        localInputs.value = localInputs.value.copy(cart = emptyMap(), cashReceivedInput = "")
    }

    fun setPaymentMethod(method: PaymentMethod) {
        localInputs.value = localInputs.value.copy(paymentMethod = method)
    }

    fun setCashReceivedInput(input: String) {
        val digitsOnly = input.filter { it.isDigit() }
        localInputs.value = localInputs.value.copy(cashReceivedInput = digitsOnly)
    }

    fun dismissReceiptDialog() {
        localInputs.value = localInputs.value.copy(lastCompletedReceipt = null)
    }

    fun processCashierTransaction() {
        val state = uiState.value
        val employee = state.selectedEmployee
        if (employee == null) {
            showError("Pilih nama karyawan yang bertugas terlebih dahulu")
            return
        }
        if (state.cartItems.isEmpty()) {
            showError("Keranjang belanja masih kosong")
            return
        }
        val cashVal = if (state.paymentMethod == PaymentMethod.TRANSFER) {
            state.cartTotal
        } else {
            state.cashReceivedInput.filter { it.isDigit() }.toLongOrNull() ?: 0L
        }
        if (cashVal < state.cartTotal) {
            showError("Uang diterima kurang dari total pembayaran (${FormatUtils.formatRupiah(state.cartTotal)})")
            return
        }

        viewModelScope.launch {
            localInputs.value = localInputs.value.copy(isSubmitting = true)
            runCatching {
                val submissionItems = state.cartItems.map {
                    CartSubmissionItem(productId = it.product.id, quantity = it.quantity)
                }
                repository.submitCashierTransaction(
                    employee = employee,
                    cartItems = submissionItems,
                    paymentMethod = state.paymentMethod,
                    cashReceived = cashVal
                )
            }.onSuccess { receipt ->
                localInputs.value = localInputs.value.copy(
                    isSubmitting = false,
                    cart = emptyMap(),
                    cashReceivedInput = "",
                    lastCompletedReceipt = receipt,
                    feedbackMessage = "Transaksi ${receipt.transaction.transactionNumber} berhasil!",
                    isErrorFeedback = false
                )
            }.onFailure { err ->
                localInputs.value = localInputs.value.copy(
                    isSubmitting = false,
                    feedbackMessage = err.message ?: "Gagal memproses transaksi kasir",
                    isErrorFeedback = true
                )
            }
        }
    }

    // --- Pulsa & Digital ---

    fun submitPulsaTransaction(
        serviceName: String,
        phoneNumber: String,
        nominal: Long,
        costPrice: Long,
        adminFee: Long,
        paymentMethod: PaymentMethod,
        status: PulsaStatus,
        onSuccessClear: () -> Unit
    ) {
        val employee = uiState.value.selectedEmployee
        if (employee == null) {
            showError("Pilih nama karyawan terlebih dahulu")
            return
        }
        if (serviceName.isBlank()) {
            showError("Masukkan nama layanan atau operator")
            return
        }
        if (phoneNumber.isBlank()) {
            showError("Masukkan nomor HP / ID pelanggan")
            return
        }
        if (nominal <= 0) {
            showError("Nominal pulsa / layanan harus lebih dari 0")
            return
        }

        viewModelScope.launch {
            localInputs.value = localInputs.value.copy(isSubmitting = true)
            runCatching {
                repository.submitPulsaTransaction(
                    employee = employee,
                    serviceName = serviceName,
                    phoneNumber = phoneNumber,
                    nominal = nominal,
                    costPrice = costPrice.coerceAtLeast(0L),
                    adminFee = adminFee.coerceAtLeast(0L),
                    paymentMethod = paymentMethod,
                    status = status
                )
            }.onSuccess { tx ->
                localInputs.value = localInputs.value.copy(
                    isSubmitting = false,
                    feedbackMessage = "Transaksi pulsa ${tx.transactionNumber} (${tx.status}) tersimpan",
                    isErrorFeedback = false
                )
                onSuccessClear()
            }.onFailure { err ->
                localInputs.value = localInputs.value.copy(
                    isSubmitting = false,
                    feedbackMessage = err.message ?: "Gagal menyimpan transaksi pulsa",
                    isErrorFeedback = true
                )
            }
        }
    }

    fun updatePendingPulsaStatus(pulsaId: Long, newStatus: PulsaStatus) {
        val employee = uiState.value.selectedEmployee ?: return
        viewModelScope.launch {
            runCatching {
                repository.updatePendingPulsaStatus(
                    pulsaId = pulsaId,
                    newStatus = newStatus,
                    actorRole = UserRole.KARYAWAN,
                    actorName = employee.name
                )
            }.onSuccess {
                localInputs.value = localInputs.value.copy(
                    feedbackMessage = "Status transaksi diperbarui menjadi ${newStatus.label}",
                    isErrorFeedback = false
                )
            }.onFailure { err ->
                showError(err.message ?: "Gagal memperbarui status transaksi")
            }
        }
    }

    class Factory(private val repository: FarnadRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EmployeeViewModel(repository) as T
        }
    }
}
