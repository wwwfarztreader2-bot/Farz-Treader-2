package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        RoleCredentialEntity::class,
        EmployeeEntity::class,
        CategoryEntity::class,
        ProductEntity::class,
        CashierTransactionEntity::class,
        CashierTransactionItemEntity::class,
        PulsaTransactionEntity::class,
        AttendanceRecordEntity::class,
        SequenceCounterEntity::class,
        ActivityLogEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class FarnadDatabase : RoomDatabase() {
    abstract fun farnadDao(): FarnadDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE attendance_records ADD COLUMN attendanceType TEXT NOT NULL DEFAULT 'HADIR'")
                db.execSQL("ALTER TABLE attendance_records ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
            }
        }

        @Volatile
        private var INSTANCE: FarnadDatabase? = null

        fun getInstance(context: Context): FarnadDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FarnadDatabase::class.java,
                    "farnad_cellular_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
