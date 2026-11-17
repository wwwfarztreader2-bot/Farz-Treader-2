package com.example

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AttendanceType
import com.example.data.local.EmployeeEntity
import com.example.data.local.FarnadDatabase
import com.example.ui.components.AppIcons
import com.example.util.FormatUtils
import com.example.util.PasswordHasher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FarnadBusinessLogicTest {

    @Test
    fun passwordHasher_hashesAndVerifiesOwnerAndEmployeeCredentials() {
        val ownerHash1 = PasswordHasher.hashPassword("Farnad.22")
        val ownerHash2 = PasswordHasher.hashPassword("Farnad.22")
        assertNotEquals(ownerHash1, ownerHash2) // Salted per hash
        assertFalse(ownerHash1.contains("Farnad.22"))
        assertTrue(PasswordHasher.verifyPassword("Farnad.22", ownerHash1))
        assertFalse(PasswordHasher.verifyPassword("wrong", ownerHash1))

        val empHash = PasswordHasher.hashPassword("0622")
        assertFalse(empHash.contains("0622"))
        assertTrue(PasswordHasher.verifyPassword("0622", empHash))
        assertFalse(PasswordHasher.verifyPassword("0000", empHash))
    }

    @Test
    fun formatUtils_formatsAttendanceCodesAndRupiahCorrectly() {
        assertEquals("FNC0001", FormatUtils.formatAttendanceCode(1))
        assertEquals("FNC0002", FormatUtils.formatAttendanceCode(2))
        assertEquals("FNC0125", FormatUtils.formatAttendanceCode(125))
        assertEquals("Rp50.000", FormatUtils.formatRupiah(50_000L))
        assertEquals("Rp2.000", FormatUtils.formatRupiah(2_000L))
    }

    @Test
    fun attendanceType_resolvesKnownAndFallbackKeys() {
        assertEquals(AttendanceType.HADIR, AttendanceType.fromKey("HADIR"))
        assertEquals(AttendanceType.SAKIT, AttendanceType.fromKey("SAKIT"))
        assertEquals(AttendanceType.IZIN, AttendanceType.fromKey("IZIN"))
        assertEquals(AttendanceType.HADIR, AttendanceType.fromKey(null))
        assertEquals(AttendanceType.HADIR, AttendanceType.fromKey("UNKNOWN"))
    }

    @Test
    fun appIcons_allCustomVectorsParseCleanly() {
        val vectors = listOf(
            AppIcons.AccessTime,
            AppIcons.AddShoppingCart,
            AppIcons.AdminPanelSettings,
            AppIcons.AssessmentFilled,
            AppIcons.AssessmentOutlined,
            AppIcons.Badge,
            AppIcons.CameraAltFilled,
            AppIcons.CameraAltOutlined,
            AppIcons.Cameraswitch,
            AppIcons.Category,
            AppIcons.DarkMode,
            AppIcons.LightMode,
            AppIcons.DashboardFilled,
            AppIcons.DashboardOutlined,
            AppIcons.DeleteOutline,
            AppIcons.Download,
            AppIcons.EventNote,
            AppIcons.Exposure,
            AppIcons.GroupFilled,
            AppIcons.GroupOutlined,
            AppIcons.History,
            AppIcons.Inventory2Filled,
            AppIcons.Inventory2Outlined,
            AppIcons.LockReset,
            AppIcons.Logout,
            AppIcons.MedicalServices,
            AppIcons.MonetizationOn,
            AppIcons.Palette,
            AppIcons.PhoneAndroidFilled,
            AppIcons.PhoneAndroidOutlined,
            AppIcons.PictureAsPdf,
            AppIcons.PointOfSaleFilled,
            AppIcons.PointOfSaleOutlined,
            AppIcons.PowerSettingsNew,
            AppIcons.Print,
            AppIcons.ProductionQuantityLimits,
            AppIcons.ReceiptLong,
            AppIcons.Remove,
            AppIcons.ReportProblem,
            AppIcons.ShoppingBag,
            AppIcons.Storefront,
            AppIcons.TableChart,
            AppIcons.Visibility,
            AppIcons.VisibilityOff
        )
        assertEquals(44, vectors.size)
        vectors.forEach { vector ->
            assertNotNull(vector)
            assertTrue(vector.name.isNotBlank())
        }
    }

    @Test
    fun dao_recordLeaveAttendance_createsSequentialCodesAndEnforcesValidation() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FarnadDatabase::class.java
        ).allowMainThreadQueries().build()

        try {
            val dao = db.farnadDao()
            val emp1Id = dao.insertEmployee(EmployeeEntity(name = "Rina"))
            val emp2Id = dao.insertEmployee(EmployeeEntity(name = "Budi"))
            val emp1 = dao.getEmployeeById(emp1Id)!!
            val emp2 = dao.getEmployeeById(emp2Id)!!

            // 1. Blank reason rejected
            val blankResult = runCatching {
                dao.recordLeaveAttendance(
                    employee = emp1,
                    type = AttendanceType.SAKIT,
                    reason = "   ",
                    photoPath = "",
                    address = "Rumah",
                    lat = 0.0,
                    lng = 0.0
                )
            }
            assertTrue(blankResult.isFailure)

            // 2. HADIR type rejected for leave endpoint
            val wrongTypeResult = runCatching {
                dao.recordLeaveAttendance(
                    employee = emp1,
                    type = AttendanceType.HADIR,
                    reason = "Masuk",
                    photoPath = "",
                    address = "Toko",
                    lat = 0.0,
                    lng = 0.0
                )
            }
            assertTrue(wrongTypeResult.isFailure)

            // 3. Employee 1 submits Sakit -> FNC0001
            val sickRecord = dao.recordLeaveAttendance(
                employee = emp1,
                type = AttendanceType.SAKIT,
                reason = "  Demam tinggi  ",
                photoPath = "",
                address = "Pengajuan sakit/izin (tanpa lokasi GPS)",
                lat = 0.0,
                lng = 0.0
            )
            assertEquals("FNC0001", sickRecord.attendanceCode)
            assertEquals(AttendanceType.SAKIT.key, sickRecord.attendanceType)
            assertEquals("Demam tinggi", sickRecord.notes)

            // 4. Employee 2 submits Izin -> FNC0002
            val leaveRecord = dao.recordLeaveAttendance(
                employee = emp2,
                type = AttendanceType.IZIN,
                reason = "Urusan keluarga",
                photoPath = "/tmp/surat_izin.jpg",
                address = "Jl. Merdeka No. 10",
                lat = -6.2,
                lng = 106.8
            )
            assertEquals("FNC0002", leaveRecord.attendanceCode)
            assertEquals(AttendanceType.IZIN.key, leaveRecord.attendanceType)
            assertEquals("Urusan keluarga", leaveRecord.notes)
            assertEquals("/tmp/surat_izin.jpg", leaveRecord.checkInPhotoPath)

            // 5. Same-day duplicate check-in or leave is blocked for emp1
            val dupLeave = runCatching {
                dao.recordLeaveAttendance(
                    employee = emp1,
                    type = AttendanceType.IZIN,
                    reason = "Izin tambahan",
                    photoPath = "",
                    address = "",
                    lat = 0.0,
                    lng = 0.0
                )
            }
            assertTrue(dupLeave.isFailure)

            val dupCheckIn = runCatching {
                dao.recordCheckIn(
                    employee = emp1,
                    photoPath = "/tmp/cam.jpg",
                    address = "Toko",
                    lat = -6.2,
                    lng = 106.8
                )
            }
            assertTrue(dupCheckIn.isFailure)

            // 6. Check-out is rejected on a SAKIT/IZIN record
            val checkOutOnSick = runCatching {
                dao.recordCheckOut(
                    attendanceId = sickRecord.id,
                    employee = emp1,
                    photoPath = "/tmp/out.jpg",
                    address = "Toko",
                    lat = -6.2,
                    lng = 106.8
                )
            }
            assertTrue(checkOutOnSick.isFailure)

            // 7. Activity logs recorded for both Sakit & Izin
            val logs = dao.observeActivityLogs().first()
            assertEquals(2, logs.size)
            assertTrue(logs.all { it.actionType == "ABSENSI" })
            assertTrue(logs.any { it.referenceCode == "FNC0001" && it.description.contains("Sakit") && it.description.contains("Demam tinggi") })
            assertTrue(logs.any { it.referenceCode == "FNC0002" && it.description.contains("Izin") && it.description.contains("Urusan keluarga") })
        } finally {
            db.close()
        }
    }
}
