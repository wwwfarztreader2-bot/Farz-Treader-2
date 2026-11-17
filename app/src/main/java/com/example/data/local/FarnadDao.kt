package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.util.FormatUtils
import kotlinx.coroutines.flow.Flow

data class CartSubmissionItem(
    val productId: Long,
    val quantity: Int
)

@Dao
interface FarnadDao {

    // --- Credentials ---
    @Query("SELECT * FROM role_credentials WHERE role = :role LIMIT 1")
    suspend fun getCredential(role: String): RoleCredentialEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCredential(credential: RoleCredentialEntity)

    // --- Employees ---
    @Query("SELECT * FROM employees ORDER BY isActive DESC, name ASC")
    fun observeAllEmployees(): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees WHERE isActive = 1 ORDER BY name ASC")
    fun observeActiveEmployees(): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees WHERE id = :id LIMIT 1")
    suspend fun getEmployeeById(id: Long): EmployeeEntity?

    @Insert
    suspend fun insertEmployee(employee: EmployeeEntity): Long

    @Update
    suspend fun updateEmployee(employee: EmployeeEntity)

    // --- Categories ---
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun countCategories(): Int

    @Insert
    suspend fun insertCategory(category: CategoryEntity): Long

    // --- Products ---
    @Query("SELECT * FROM products ORDER BY isActive DESC, name ASC")
    fun observeAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY name ASC")
    fun observeActiveProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Long): ProductEntity?

    @Insert
    suspend fun insertProduct(product: ProductEntity): Long

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :productId")
    suspend fun deleteProductById(productId: Long)

    // --- Cashier Transactions ---
    @Transaction
    @Query("SELECT * FROM cashier_transactions ORDER BY createdAtMillis DESC")
    fun observeCashierTransactions(): Flow<List<CashierTransactionWithItems>>

    @Insert
    suspend fun insertCashierTransaction(tx: CashierTransactionEntity): Long

    @Insert
    suspend fun insertCashierTransactionItems(items: List<CashierTransactionItemEntity>)

    // --- Pulsa Transactions ---
    @Query("SELECT * FROM pulsa_transactions ORDER BY createdAtMillis DESC")
    fun observePulsaTransactions(): Flow<List<PulsaTransactionEntity>>

    @Query("SELECT * FROM pulsa_transactions WHERE id = :id LIMIT 1")
    suspend fun getPulsaById(id: Long): PulsaTransactionEntity?

    @Insert
    suspend fun insertPulsaTransaction(tx: PulsaTransactionEntity): Long

    @Update
    suspend fun updatePulsaTransaction(tx: PulsaTransactionEntity)

    // --- Attendance ---
    @Query("SELECT * FROM attendance_records ORDER BY checkInMillis DESC")
    fun observeAttendanceRecords(): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE employeeId = :employeeId AND dateKey = :dateKey ORDER BY checkInMillis DESC LIMIT 1")
    suspend fun getEmployeeAttendanceForDate(employeeId: Long, dateKey: String): AttendanceRecordEntity?

    @Insert
    suspend fun insertAttendance(record: AttendanceRecordEntity): Long

    @Update
    suspend fun updateAttendance(record: AttendanceRecordEntity)

    // --- Sequences ---
    @Query("SELECT * FROM sequence_counters WHERE `key` = :key LIMIT 1")
    suspend fun getCounter(key: String): SequenceCounterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCounter(counter: SequenceCounterEntity)

    // --- Activity Logs ---
    @Query("SELECT * FROM activity_logs ORDER BY createdAtMillis DESC")
    fun observeActivityLogs(): Flow<List<ActivityLogEntity>>

    @Insert
    suspend fun insertActivityLog(log: ActivityLogEntity): Long

    // --- Atomic Transactional Workflows ---

    @Transaction
    suspend fun allocateSequence(key: String): Long {
        val current = getCounter(key)?.nextValue ?: 1L
        upsertCounter(SequenceCounterEntity(key = key, nextValue = current + 1L))
        return current
    }

    @Transaction
    suspend fun completeCashierSale(
        employee: EmployeeEntity,
        cartItems: List<CartSubmissionItem>,
        paymentMethod: PaymentMethod,
        cashReceived: Long
    ): CashierTransactionWithItems {
        require(cartItems.isNotEmpty()) { "Keranjang belanja kosong" }
        val now = System.currentTimeMillis()

        val resolvedItems = mutableListOf<CashierTransactionItemEntity>()
        var totalAmount = 0L
        var totalCost = 0L
        var totalProfit = 0L

        for (item in cartItems) {
            require(item.quantity > 0) { "Jumlah pembelian tidak valid" }
            val product = getProductById(item.productId)
                ?: throw IllegalStateException("Barang tidak ditemukan")
            require(product.isActive) { "${product.name} sudah dinonaktifkan" }
            require(product.stock >= item.quantity) {
                "Stok ${product.name} tidak mencukupi (tersisa ${product.stock})"
            }

            val updatedProduct = product.copy(
                stock = product.stock - item.quantity,
                updatedAtMillis = now
            )
            updateProduct(updatedProduct)

            val subtotal = product.sellPrice * item.quantity
            val itemCost = product.costPrice * item.quantity
            val itemProfit = (product.sellPrice - product.costPrice) * item.quantity

            totalAmount += subtotal
            totalCost += itemCost
            totalProfit += itemProfit

            resolvedItems.add(
                CashierTransactionItemEntity(
                    transactionId = 0L,
                    productId = product.id,
                    productName = product.name,
                    quantity = item.quantity,
                    costPrice = product.costPrice,
                    sellPrice = product.sellPrice,
                    subtotal = subtotal,
                    profit = itemProfit
                )
            )
        }

        val effectiveCash = if (paymentMethod == PaymentMethod.TRANSFER) totalAmount else cashReceived
        require(effectiveCash >= totalAmount) { "Uang pembayaran kurang dari total belanja" }
        val change = effectiveCash - totalAmount

        val seq = allocateSequence("trx_seq")
        val trxNo = "TRX-${FormatUtils.formatCompactDateForCode(now)}-${seq.toString().padStart(4, '0')}"

        val header = CashierTransactionEntity(
            transactionNumber = trxNo,
            employeeId = employee.id,
            employeeName = employee.name,
            paymentMethod = paymentMethod.label,
            totalAmount = totalAmount,
            totalCost = totalCost,
            totalProfit = totalProfit,
            cashReceived = effectiveCash,
            changeAmount = change,
            createdAtMillis = now
        )
        val insertedId = insertCashierTransaction(header)
        val finalItems = resolvedItems.map { it.copy(transactionId = insertedId) }
        insertCashierTransactionItems(finalItems)

        val summaryNames = finalItems.joinToString(", ") { "${it.productName} x${it.quantity}" }
        insertActivityLog(
            ActivityLogEntity(
                actorRole = UserRole.KARYAWAN.label,
                actorName = employee.name,
                actionType = "KASIR",
                description = "Transaksi kasir $trxNo (${paymentMethod.label}): $summaryNames — Total ${FormatUtils.formatRupiah(totalAmount)}",
                referenceCode = trxNo,
                createdAtMillis = now
            )
        )

        return CashierTransactionWithItems(
            transaction = header.copy(id = insertedId),
            items = finalItems
        )
    }

    @Transaction
    suspend fun recordPulsaTransaction(
        employee: EmployeeEntity,
        serviceName: String,
        phoneNumber: String,
        nominal: Long,
        costPrice: Long,
        adminFee: Long,
        paymentMethod: PaymentMethod,
        status: PulsaStatus
    ): PulsaTransactionEntity {
        val now = System.currentTimeMillis()
        val seq = allocateSequence("pls_seq")
        val trxNo = "PLS-${FormatUtils.formatCompactDateForCode(now)}-${seq.toString().padStart(4, '0')}"
        val totalPayment = nominal + adminFee

        val entity = PulsaTransactionEntity(
            transactionNumber = trxNo,
            employeeId = employee.id,
            employeeName = employee.name,
            serviceName = serviceName.trim(),
            phoneNumber = phoneNumber.trim(),
            nominal = nominal,
            costPrice = costPrice,
            adminFee = adminFee,
            totalPayment = totalPayment,
            paymentMethod = paymentMethod.label,
            status = status.label,
            createdAtMillis = now,
            updatedAtMillis = now
        )
        val id = insertPulsaTransaction(entity)

        insertActivityLog(
            ActivityLogEntity(
                actorRole = UserRole.KARYAWAN.label,
                actorName = employee.name,
                actionType = "PULSA",
                description = "Transaksi $serviceName ($phoneNumber) Nominal ${FormatUtils.formatRupiah(nominal)}, Admin ${FormatUtils.formatRupiah(adminFee)}, Status: ${status.label}",
                referenceCode = trxNo,
                createdAtMillis = now
            )
        )
        return entity.copy(id = id)
    }

    @Transaction
    suspend fun updatePulsaStatusWithAudit(
        pulsaId: Long,
        newStatus: PulsaStatus,
        actorRole: String,
        actorName: String
    ) {
        val existing = getPulsaById(pulsaId)
            ?: throw IllegalStateException("Transaksi pulsa tidak ditemukan")
        require(existing.status == PulsaStatus.PENDING.label) {
            "Transaksi berstatus ${existing.status} sudah final dan tidak dapat diubah"
        }
        val now = System.currentTimeMillis()
        updatePulsaTransaction(
            existing.copy(
                status = newStatus.label,
                updatedAtMillis = now
            )
        )
        insertActivityLog(
            ActivityLogEntity(
                actorRole = actorRole,
                actorName = actorName,
                actionType = "PULSA",
                description = "Mengubah status ${existing.transactionNumber} (${existing.serviceName}) dari ${existing.status} menjadi ${newStatus.label}",
                referenceCode = existing.transactionNumber,
                createdAtMillis = now
            )
        )
    }

    @Transaction
    suspend fun recordCheckIn(
        employee: EmployeeEntity,
        photoPath: String,
        address: String,
        lat: Double,
        lng: Double
    ): AttendanceRecordEntity {
        val now = System.currentTimeMillis()
        val dateKey = FormatUtils.formatDateKey(now)
        val existingToday = getEmployeeAttendanceForDate(employee.id, dateKey)
        if (existingToday != null) {
            val typeLabel = AttendanceType.fromKey(existingToday.attendanceType).label
            throw IllegalStateException("${employee.name} sudah memiliki catatan absensi ($typeLabel) hari ini (${existingToday.attendanceCode})")
        }

        val seq = allocateSequence("attendance_fnc")
        val code = FormatUtils.formatAttendanceCode(seq)
        val dayName = FormatUtils.formatDayName(now)
        val dateFormatted = FormatUtils.formatIndonesianDate(now)
        val timeFormatted = FormatUtils.formatTimeWib(now)

        val record = AttendanceRecordEntity(
            attendanceCode = code,
            employeeId = employee.id,
            employeeName = employee.name,
            dateKey = dateKey,
            dayName = dayName,
            dateFormatted = dateFormatted,
            checkInTimeFormatted = timeFormatted,
            checkInMillis = now,
            checkInPhotoPath = photoPath,
            checkInAddress = address,
            checkInLat = lat,
            checkInLng = lng,
            attendanceType = AttendanceType.HADIR.key,
            notes = ""
        )
        val id = insertAttendance(record)

        insertActivityLog(
            ActivityLogEntity(
                actorRole = UserRole.KARYAWAN.label,
                actorName = employee.name,
                actionType = "ABSENSI",
                description = "Absensi Masuk $code pada $dayName, $dateFormatted pukul $timeFormatted di $address",
                referenceCode = code,
                createdAtMillis = now
            )
        )

        return record.copy(id = id)
    }

    @Transaction
    suspend fun recordLeaveAttendance(
        employee: EmployeeEntity,
        type: AttendanceType,
        reason: String,
        photoPath: String,
        address: String,
        lat: Double,
        lng: Double
    ): AttendanceRecordEntity {
        require(type == AttendanceType.SAKIT || type == AttendanceType.IZIN) {
            "Jenis absensi harus Sakit atau Izin"
        }
        val cleanReason = reason.trim()
        require(cleanReason.isNotEmpty()) { "Keterangan / alasan ${type.label.lowercase()} wajib diisi" }

        val now = System.currentTimeMillis()
        val dateKey = FormatUtils.formatDateKey(now)
        val existingToday = getEmployeeAttendanceForDate(employee.id, dateKey)
        if (existingToday != null) {
            val existingLabel = AttendanceType.fromKey(existingToday.attendanceType).label
            throw IllegalStateException("${employee.name} sudah memiliki catatan absensi ($existingLabel) hari ini (${existingToday.attendanceCode})")
        }

        val seq = allocateSequence("attendance_fnc")
        val code = FormatUtils.formatAttendanceCode(seq)
        val dayName = FormatUtils.formatDayName(now)
        val dateFormatted = FormatUtils.formatIndonesianDate(now)
        val timeFormatted = FormatUtils.formatTimeWib(now)

        val record = AttendanceRecordEntity(
            attendanceCode = code,
            employeeId = employee.id,
            employeeName = employee.name,
            dateKey = dateKey,
            dayName = dayName,
            dateFormatted = dateFormatted,
            checkInTimeFormatted = timeFormatted,
            checkInMillis = now,
            checkInPhotoPath = photoPath,
            checkInAddress = address,
            checkInLat = lat,
            checkInLng = lng,
            attendanceType = type.key,
            notes = cleanReason
        )
        val id = insertAttendance(record)

        insertActivityLog(
            ActivityLogEntity(
                actorRole = UserRole.KARYAWAN.label,
                actorName = employee.name,
                actionType = "ABSENSI",
                description = "Absensi ${type.label} $code pada $dayName, $dateFormatted pukul $timeFormatted — Keterangan: $cleanReason",
                referenceCode = code,
                createdAtMillis = now
            )
        )

        return record.copy(id = id)
    }

    @Transaction
    suspend fun recordCheckOut(
        attendanceId: Long,
        employee: EmployeeEntity,
        photoPath: String,
        address: String,
        lat: Double,
        lng: Double
    ): AttendanceRecordEntity {
        val now = System.currentTimeMillis()
        val dateKey = FormatUtils.formatDateKey(now)
        val existing = getEmployeeAttendanceForDate(employee.id, dateKey)
            ?: throw IllegalStateException("Belum ada absensi masuk hari ini untuk ${employee.name}")
        require(existing.id == attendanceId) { "Data absensi tidak cocok" }
        require(existing.attendanceType == AttendanceType.HADIR.key) {
            "Absensi pulang hanya berlaku untuk status Hadir"
        }
        require(existing.checkOutMillis == null) { "${employee.name} sudah melakukan absensi pulang hari ini" }

        val timeFormatted = FormatUtils.formatTimeWib(now)
        val updated = existing.copy(
            checkOutTimeFormatted = timeFormatted,
            checkOutMillis = now,
            checkOutPhotoPath = photoPath,
            checkOutAddress = address,
            checkOutLat = lat,
            checkOutLng = lng
        )
        updateAttendance(updated)

        insertActivityLog(
            ActivityLogEntity(
                actorRole = UserRole.KARYAWAN.label,
                actorName = employee.name,
                actionType = "ABSENSI",
                description = "Absensi Pulang ${existing.attendanceCode} pukul $timeFormatted di $address",
                referenceCode = existing.attendanceCode,
                createdAtMillis = now
            )
        )

        return updated
    }
}
