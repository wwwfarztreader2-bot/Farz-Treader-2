package com.example.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

enum class UserRole(val key: String, val label: String) {
    OWNER("OWNER", "Owner"),
    KARYAWAN("KARYAWAN", "Karyawan")
}

enum class PaymentMethod(val label: String) {
    TUNAI("Tunai"),
    TRANSFER("Transfer")
}

enum class PulsaStatus(val label: String) {
    BERHASIL("Berhasil"),
    PENDING("Pending"),
    DIBATALKAN("Dibatalkan")
}

enum class ReportPeriod(val label: String) {
    HARIAN("Harian"),
    MINGGUAN("Mingguan"),
    BULANAN("Bulanan"),
    RENTANG("Rentang Tanggal")
}

enum class AttendanceType(val key: String, val label: String) {
    HADIR("HADIR", "Hadir"),
    SAKIT("SAKIT", "Sakit"),
    IZIN("IZIN", "Izin");

    companion object {
        fun fromKey(key: String?): AttendanceType =
            entries.firstOrNull { it.key == key } ?: HADIR
    }
}

@Entity(tableName = "role_credentials")
data class RoleCredentialEntity(
    @PrimaryKey val role: String,
    val passwordHash: String,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "employees")
data class EmployeeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val position: String = "Kasir & Operasional",
    val isActive: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val categoryId: Long,
    val categoryName: String,
    val costPrice: Long,
    val sellPrice: Long,
    val stock: Int,
    val minStock: Int = 5,
    val isActive: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "cashier_transactions",
    indices = [Index(value = ["transactionNumber"], unique = true)]
)
data class CashierTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionNumber: String,
    val employeeId: Long,
    val employeeName: String,
    val paymentMethod: String,
    val totalAmount: Long,
    val totalCost: Long,
    val totalProfit: Long,
    val cashReceived: Long,
    val changeAmount: Long,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "cashier_transaction_items",
    foreignKeys = [
        ForeignKey(
            entity = CashierTransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["transactionId"])]
)
data class CashierTransactionItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: Long,
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val costPrice: Long,
    val sellPrice: Long,
    val subtotal: Long,
    val profit: Long
)

data class CashierTransactionWithItems(
    @Embedded val transaction: CashierTransactionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "transactionId"
    )
    val items: List<CashierTransactionItemEntity>
)

@Entity(
    tableName = "pulsa_transactions",
    indices = [Index(value = ["transactionNumber"], unique = true)]
)
data class PulsaTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionNumber: String,
    val employeeId: Long,
    val employeeName: String,
    val serviceName: String,
    val phoneNumber: String,
    val nominal: Long,
    val costPrice: Long,
    val adminFee: Long,
    val totalPayment: Long,
    val paymentMethod: String,
    val status: String,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "attendance_records",
    indices = [Index(value = ["attendanceCode"], unique = true)]
)
data class AttendanceRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val attendanceCode: String,
    val employeeId: Long,
    val employeeName: String,
    val dateKey: String,
    val dayName: String,
    val dateFormatted: String,
    val checkInTimeFormatted: String,
    val checkInMillis: Long,
    val checkInPhotoPath: String,
    val checkInAddress: String,
    val checkInLat: Double,
    val checkInLng: Double,
    val checkOutTimeFormatted: String? = null,
    val checkOutMillis: Long? = null,
    val checkOutPhotoPath: String? = null,
    val checkOutAddress: String? = null,
    val checkOutLat: Double? = null,
    val checkOutLng: Double? = null,
    val attendanceType: String = AttendanceType.HADIR.key,
    val notes: String = ""
)

@Entity(tableName = "sequence_counters")
data class SequenceCounterEntity(
    @PrimaryKey val key: String,
    val nextValue: Long
)

@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val actorRole: String,
    val actorName: String,
    val actionType: String,
    val description: String,
    val referenceCode: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)
