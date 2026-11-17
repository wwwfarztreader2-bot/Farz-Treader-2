package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.local.ActivityLogEntity
import com.example.data.local.AttendanceRecordEntity
import com.example.data.local.AttendanceType
import com.example.data.local.CartSubmissionItem
import com.example.data.local.CashierTransactionWithItems
import com.example.data.local.CategoryEntity
import com.example.data.local.EmployeeEntity
import com.example.data.local.FarnadDao
import com.example.data.local.FarnadDatabase
import com.example.data.local.PaymentMethod
import com.example.data.local.ProductEntity
import com.example.data.local.PulsaStatus
import com.example.data.local.PulsaTransactionEntity
import com.example.data.local.RoleCredentialEntity
import com.example.data.local.UserRole
import com.example.ui.theme.ThemeMode
import com.example.util.FormatUtils
import com.example.util.PasswordHasher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "farnad_session")

class FarnadRepository(
    private val dao: FarnadDao,
    private val dataStore: DataStore<Preferences>
) {
    constructor(context: Context) : this(
        dao = FarnadDatabase.getInstance(context).farnadDao(),
        dataStore = context.applicationContext.sessionDataStore
    )

    private val initMutex = Mutex()
    @Volatile
    private var initialized = false

    val sessionRoleFlow: Flow<UserRole?> = dataStore.data.map { prefs ->
        when (prefs[KEY_ACTIVE_ROLE]) {
            UserRole.OWNER.key -> UserRole.OWNER
            UserRole.KARYAWAN.key -> UserRole.KARYAWAN
            else -> null
        }
    }

    val selectedEmployeeIdFlow: Flow<Long?> = dataStore.data.map { prefs ->
        prefs[KEY_SELECTED_EMPLOYEE_ID]
    }

    val themeModeFlow: Flow<ThemeMode> = dataStore.data.map { prefs ->
        when (prefs[KEY_THEME_MODE]) {
            ThemeMode.LIGHT.key -> ThemeMode.LIGHT
            ThemeMode.DARK.key -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.key
        }
    }

    val allEmployees: Flow<List<EmployeeEntity>> = dao.observeAllEmployees()
    val activeEmployees: Flow<List<EmployeeEntity>> = dao.observeActiveEmployees()
    val categories: Flow<List<CategoryEntity>> = dao.observeCategories()
    val allProducts: Flow<List<ProductEntity>> = dao.observeAllProducts()
    val activeProducts: Flow<List<ProductEntity>> = dao.observeActiveProducts()
    val cashierTransactions: Flow<List<CashierTransactionWithItems>> = dao.observeCashierTransactions()
    val pulsaTransactions: Flow<List<PulsaTransactionEntity>> = dao.observePulsaTransactions()
    val attendanceRecords: Flow<List<AttendanceRecordEntity>> = dao.observeAttendanceRecords()
    val activityLogs: Flow<List<ActivityLogEntity>> = dao.observeActivityLogs()

    /**
     * Ensures initial salted-hashed credentials and structural product categories exist.
     * Does NOT fabricate fake business transactions or fake attendance.
     */
    suspend fun ensureInitialized() {
        if (initialized) return
        initMutex.withLock {
            if (initialized) return
            if (dao.getCredential(UserRole.OWNER.key) == null) {
                dao.upsertCredential(
                    RoleCredentialEntity(
                        role = UserRole.OWNER.key,
                        passwordHash = PasswordHasher.hashPassword(DEFAULT_OWNER_PASSWORD)
                    )
                )
            }
            if (dao.getCredential(UserRole.KARYAWAN.key) == null) {
                dao.upsertCredential(
                    RoleCredentialEntity(
                        role = UserRole.KARYAWAN.key,
                        passwordHash = PasswordHasher.hashPassword(DEFAULT_EMPLOYEE_PASSWORD)
                    )
                )
            }
            if (dao.countCategories() == 0) {
                DEFAULT_CATEGORIES.forEach { catName ->
                    dao.insertCategory(CategoryEntity(name = catName))
                }
            }
            initialized = true
        }
    }

    suspend fun login(role: UserRole, rawPassword: String): Boolean {
        ensureInitialized()
        val credential = dao.getCredential(role.key) ?: return false
        val valid = PasswordHasher.verifyPassword(rawPassword, credential.passwordHash)
        if (valid) {
            dataStore.edit { prefs ->
                prefs[KEY_ACTIVE_ROLE] = role.key
            }
            dao.insertActivityLog(
                ActivityLogEntity(
                    actorRole = role.label,
                    actorName = role.label,
                    actionType = "LOGIN",
                    description = "${role.label} masuk ke Sistem Manajemen FARNAD CELL"
                )
            )
        }
        return valid
    }

    suspend fun logout() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_ACTIVE_ROLE)
        }
    }

    suspend fun saveSelectedEmployeeId(employeeId: Long) {
        dataStore.edit { prefs ->
            prefs[KEY_SELECTED_EMPLOYEE_ID] = employeeId
        }
    }

    suspend fun updateRolePassword(targetRole: UserRole, newPassword: String) {
        require(newPassword.length >= 4) { "Kata sandi minimal 4 karakter" }
        ensureInitialized()
        dao.upsertCredential(
            RoleCredentialEntity(
                role = targetRole.key,
                passwordHash = PasswordHasher.hashPassword(newPassword),
                updatedAtMillis = System.currentTimeMillis()
            )
        )
        dao.insertActivityLog(
            ActivityLogEntity(
                actorRole = UserRole.OWNER.label,
                actorName = "Owner",
                actionType = "KEAMANAN",
                description = "Owner memperbarui kata sandi akses ${targetRole.label}"
            )
        )
    }

    // --- Employee Management (Owner) ---

    suspend fun addEmployee(name: String, position: String) {
        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "Nama karyawan tidak boleh kosong" }
        val cleanPos = position.trim().ifEmpty { "Kasir & Operasional" }
        dao.insertEmployee(EmployeeEntity(name = cleanName, position = cleanPos, isActive = true))
        dao.insertActivityLog(
            ActivityLogEntity(
                actorRole = UserRole.OWNER.label,
                actorName = "Owner",
                actionType = "KARYAWAN",
                description = "Menambahkan karyawan baru: $cleanName ($cleanPos)"
            )
        )
    }

    suspend fun renameEmployee(employee: EmployeeEntity, newName: String, newPosition: String) {
        val cleanName = newName.trim()
        require(cleanName.isNotEmpty()) { "Nama karyawan tidak boleh kosong" }
        val cleanPos = newPosition.trim().ifEmpty { employee.position }
        dao.updateEmployee(employee.copy(name = cleanName, position = cleanPos))
        dao.insertActivityLog(
            ActivityLogEntity(
                actorRole = UserRole.OWNER.label,
                actorName = "Owner",
                actionType = "KARYAWAN",
                description = "Mengubah data karyawan ${employee.name} menjadi $cleanName ($cleanPos)"
            )
        )
    }

    suspend fun toggleEmployeeActive(employee: EmployeeEntity) {
        val nextActive = !employee.isActive
        dao.updateEmployee(employee.copy(isActive = nextActive))
        val statusLabel = if (nextActive) "mengaktifkan" else "menonaktifkan"
        dao.insertActivityLog(
            ActivityLogEntity(
                actorRole = UserRole.OWNER.label,
                actorName = "Owner",
                actionType = "KARYAWAN",
                description = "Owner $statusLabel karyawan ${employee.name}"
            )
        )
    }

    // --- Category & Inventory Management (Owner) ---

    suspend fun addCategory(name: String) {
        val clean = name.trim()
        require(clean.isNotEmpty()) { "Nama kategori tidak boleh kosong" }
        val existing = dao.observeCategories().first().any { it.name.equals(clean, ignoreCase = true) }
        require(!existing) { "Kategori '$clean' sudah ada" }
        dao.insertCategory(CategoryEntity(name = clean))
    }

    suspend fun addProduct(
        name: String,
        category: CategoryEntity,
        costPrice: Long,
        sellPrice: Long,
        stock: Int,
        minStock: Int
    ) {
        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "Nama barang tidak boleh kosong" }
        require(costPrice >= 0) { "Harga modal tidak valid" }
        require(sellPrice >= 0) { "Harga jual tidak valid" }
        require(stock >= 0) { "Stok tidak boleh negatif" }

        val now = System.currentTimeMillis()
        dao.insertProduct(
            ProductEntity(
                name = cleanName,
                categoryId = category.id,
                categoryName = category.name,
                costPrice = costPrice,
                sellPrice = sellPrice,
                stock = stock,
                minStock = minStock.coerceAtLeast(1),
                isActive = true,
                createdAtMillis = now,
                updatedAtMillis = now
            )
        )
        dao.insertActivityLog(
            ActivityLogEntity(
                actorRole = UserRole.OWNER.label,
                actorName = "Owner",
                actionType = "STOK",
                description = "Menambahkan barang '$cleanName' (${category.name}) Modal ${FormatUtils.formatRupiah(costPrice)}, Jual ${FormatUtils.formatRupiah(sellPrice)}, Stok $stock"
            )
        )
    }

    suspend fun updateProductDetails(
        product: ProductEntity,
        newName: String,
        category: CategoryEntity,
        newCostPrice: Long,
        newSellPrice: Long,
        newMinStock: Int
    ) {
        val cleanName = newName.trim()
        require(cleanName.isNotEmpty()) { "Nama barang tidak boleh kosong" }
        require(newCostPrice >= 0 && newSellPrice >= 0) { "Harga tidak boleh negatif" }
        dao.updateProduct(
            product.copy(
                name = cleanName,
                categoryId = category.id,
                categoryName = category.name,
                costPrice = newCostPrice,
                sellPrice = newSellPrice,
                minStock = newMinStock.coerceAtLeast(1),
                updatedAtMillis = System.currentTimeMillis()
            )
        )
        dao.insertActivityLog(
            ActivityLogEntity(
                actorRole = UserRole.OWNER.label,
                actorName = "Owner",
                actionType = "STOK",
                description = "Mengubah barang '$cleanName': Modal ${FormatUtils.formatRupiah(newCostPrice)}, Jual ${FormatUtils.formatRupiah(newSellPrice)}"
            )
        )
    }

    suspend fun adjustProductStock(product: ProductEntity, newStock: Int, reason: String) {
        require(newStock >= 0) { "Stok tidak boleh kurang dari 0" }
        val delta = newStock - product.stock
        dao.updateProduct(
            product.copy(
                stock = newStock,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
        val deltaLabel = if (delta >= 0) "+$delta" else "$delta"
        dao.insertActivityLog(
            ActivityLogEntity(
                actorRole = UserRole.OWNER.label,
                actorName = "Owner",
                actionType = "STOK",
                description = "Penyesuaian stok '${product.name}': ${product.stock} -> $newStock ($deltaLabel). Catatan: ${reason.ifBlank { "Koreksi stok" }}"
            )
        )
    }

    suspend fun toggleProductActive(product: ProductEntity) {
        val next = !product.isActive
        dao.updateProduct(product.copy(isActive = next, updatedAtMillis = System.currentTimeMillis()))
        val label = if (next) "mengaktifkan" else "menonaktifkan"
        dao.insertActivityLog(
            ActivityLogEntity(
                actorRole = UserRole.OWNER.label,
                actorName = "Owner",
                actionType = "STOK",
                description = "Owner $label barang '${product.name}'"
            )
        )
    }

    suspend fun deleteProduct(product: ProductEntity) {
        dao.deleteProductById(product.id)
        dao.insertActivityLog(
            ActivityLogEntity(
                actorRole = UserRole.OWNER.label,
                actorName = "Owner",
                actionType = "STOK",
                description = "Menghapus barang '${product.name}' dari katalog"
            )
        )
    }

    // --- Employee Attendance ---

    suspend fun checkIn(
        employee: EmployeeEntity,
        photoPath: String,
        address: String,
        lat: Double,
        lng: Double
    ): AttendanceRecordEntity {
        return dao.recordCheckIn(employee, photoPath, address, lat, lng)
    }

    suspend fun checkOut(
        attendanceId: Long,
        employee: EmployeeEntity,
        photoPath: String,
        address: String,
        lat: Double,
        lng: Double
    ): AttendanceRecordEntity {
        return dao.recordCheckOut(attendanceId, employee, photoPath, address, lat, lng)
    }

    suspend fun submitLeaveAttendance(
        employee: EmployeeEntity,
        type: AttendanceType,
        reason: String,
        photoPath: String,
        address: String,
        lat: Double,
        lng: Double
    ): AttendanceRecordEntity {
        return dao.recordLeaveAttendance(employee, type, reason, photoPath, address, lat, lng)
    }

    // --- Cashier & Pulsa ---

    suspend fun submitCashierTransaction(
        employee: EmployeeEntity,
        cartItems: List<CartSubmissionItem>,
        paymentMethod: PaymentMethod,
        cashReceived: Long
    ): CashierTransactionWithItems {
        return dao.completeCashierSale(employee, cartItems, paymentMethod, cashReceived)
    }

    suspend fun submitPulsaTransaction(
        employee: EmployeeEntity,
        serviceName: String,
        phoneNumber: String,
        nominal: Long,
        costPrice: Long,
        adminFee: Long,
        paymentMethod: PaymentMethod,
        status: PulsaStatus
    ): PulsaTransactionEntity {
        return dao.recordPulsaTransaction(
            employee = employee,
            serviceName = serviceName,
            phoneNumber = phoneNumber,
            nominal = nominal,
            costPrice = costPrice,
            adminFee = adminFee,
            paymentMethod = paymentMethod,
            status = status
        )
    }

    suspend fun updatePendingPulsaStatus(
        pulsaId: Long,
        newStatus: PulsaStatus,
        actorRole: UserRole,
        actorName: String
    ) {
        dao.updatePulsaStatusWithAudit(pulsaId, newStatus, actorRole.label, actorName)
    }

    companion object {
        const val DEFAULT_OWNER_PASSWORD = "Farnad.22"
        const val DEFAULT_EMPLOYEE_PASSWORD = "0622"

        private val DEFAULT_CATEGORIES = listOf(
            "Aksesoris HP",
            "Kartu Perdana & Data",
            "Charger & Kabel",
            "Audio & Headset",
            "Case & Tempered Glass",
            "Sparepart & Lainnya"
        )

        private val KEY_ACTIVE_ROLE = stringPreferencesKey("active_role")
        private val KEY_SELECTED_EMPLOYEE_ID = longPreferencesKey("selected_employee_id")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    }
}
