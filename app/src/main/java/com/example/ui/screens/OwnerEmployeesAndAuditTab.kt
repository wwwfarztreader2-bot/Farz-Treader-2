package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import com.example.ui.components.AppIcons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.local.AttendanceType
import com.example.data.local.EmployeeEntity
import com.example.data.local.UserRole
import com.example.ui.components.AttendanceResultCard
import com.example.ui.theme.BrandRed
import com.example.ui.theme.DangerRed
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenContainer
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.WarningAmber
import com.example.util.FormatUtils
import com.example.viewmodel.OwnerUiState
import com.example.viewmodel.OwnerViewModel
import java.io.File

private enum class EmployeeSubTab(val label: String) {
    KARYAWAN("Daftar Karyawan"),
    ABSENSI("Riwayat Absensi")
}

@Composable
fun OwnerEmployeesTab(
    state: OwnerUiState,
    viewModel: OwnerViewModel,
    modifier: Modifier = Modifier
) {
    var activeSubTab by remember { mutableStateOf(EmployeeSubTab.KARYAWAN) }
    var attendanceFilter by remember { mutableStateOf<AttendanceType?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingEmployee by remember { mutableStateOf<EmployeeEntity?>(null) }
    var previewPhotoPath by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            if (activeSubTab == EmployeeSubTab.KARYAWAN) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = BrandRed,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("employee_add_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Karyawan")
                }
            }
        }
    ) { innerPad ->
        LazyColumn(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EmployeeSubTab.entries.forEach { tab ->
                        FilterChip(
                            selected = activeSubTab == tab,
                            onClick = { activeSubTab = tab },
                            label = {
                                val count = if (tab == EmployeeSubTab.KARYAWAN) {
                                    state.allEmployees.size
                                } else {
                                    state.attendanceRecords.size
                                }
                                Text("${tab.label} ($count)")
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandRed,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("emp_subtab_${tab.name.lowercase()}")
                        )
                    }
                }
            }

            if (activeSubTab == EmployeeSubTab.KARYAWAN) {
                if (state.allEmployees.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(AppIcons.GroupFilled, contentDescription = null, modifier = Modifier.size(44.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("Belum ada karyawan terdaftar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Tambahkan karyawan agar mereka dapat melakukan absensi foto & transaksi kasir.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                } else {
                    items(state.allEmployees, key = { it.id }) { emp ->
                        val latestAttendance = state.attendanceRecords.firstOrNull { it.employeeId == emp.id }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(emp.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Surface(
                                            color = if (emp.isActive) ProfitGreenContainer else MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = if (emp.isActive) "Aktif" else "Nonaktif",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (emp.isActive) ProfitGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(emp.position, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (latestAttendance != null) {
                                        Spacer(Modifier.height(4.dp))
                                        val latestType = AttendanceType.fromKey(latestAttendance.attendanceType)
                                        Text(
                                            text = "Kode Terakhir: ${latestAttendance.attendanceCode} • ${latestType.label} (${latestAttendance.dateFormatted} ${latestAttendance.checkInTimeFormatted})",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = JetBrainsMonoFamily,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                }
                                IconButton(onClick = { editingEmployee = emp }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Ubah Nama Karyawan")
                                }
                                IconButton(onClick = { viewModel.toggleEmployeeActive(emp) }) {
                                    Icon(
                                        imageVector = AppIcons.PowerSettingsNew,
                                        contentDescription = if (emp.isActive) "Nonaktifkan" else "Aktifkan",
                                        tint = if (emp.isActive) WarningAmber else ProfitGreen
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = attendanceFilter == null,
                            onClick = { attendanceFilter = null },
                            label = { Text("Semua (${state.attendanceRecords.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandRed,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("owner_attendance_filter_semua")
                        )
                        AttendanceType.entries.forEach { type ->
                            val count = state.attendanceRecords.count {
                                AttendanceType.fromKey(it.attendanceType) == type
                            }
                            FilterChip(
                                selected = attendanceFilter == type,
                                onClick = { attendanceFilter = type },
                                label = { Text("${type.label} ($count)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandRed,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("owner_attendance_filter_${type.name.lowercase()}")
                            )
                        }
                    }
                }
                val filteredAttendance = if (attendanceFilter == null) {
                    state.attendanceRecords
                } else {
                    state.attendanceRecords.filter {
                        AttendanceType.fromKey(it.attendanceType) == attendanceFilter
                    }
                }
                if (filteredAttendance.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (state.attendanceRecords.isEmpty()) {
                                    "Belum ada riwayat absensi karyawan."
                                } else {
                                    "Tidak ada data absensi untuk filter ${attendanceFilter?.label ?: ""}."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                    }
                } else {
                    items(filteredAttendance, key = { it.id }) { rec ->
                        AttendanceResultCard(
                            record = rec,
                            onPhotoClick = { path -> previewPhotoPath = path }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        EmployeeFormDialog(
            title = "Tambah Karyawan",
            initialName = "",
            initialPosition = "Kasir & Operasional",
            onDismiss = { showAddDialog = false },
            onSave = { name, pos ->
                viewModel.addEmployee(name, pos)
                showAddDialog = false
            }
        )
    }

    editingEmployee?.let { emp ->
        EmployeeFormDialog(
            title = "Ubah Data Karyawan",
            initialName = emp.name,
            initialPosition = emp.position,
            onDismiss = { editingEmployee = null },
            onSave = { name, pos ->
                viewModel.renameEmployee(emp, name, pos)
                editingEmployee = null
            }
        )
    }

    previewPhotoPath?.let { path ->
        Dialog(onDismissRequest = { previewPhotoPath = null }) {
            Card(shape = MaterialTheme.shapes.large) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Foto Absensi Karyawan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    AsyncImage(
                        model = File(path),
                        contentDescription = "Foto Absensi Lengkap",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp)
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
}

@Composable
private fun EmployeeFormDialog(
    title: String,
    initialName: String,
    initialPosition: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var position by remember { mutableStateOf(initialPosition) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Lengkap Karyawan") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("employee_name_input")
                )
                OutlinedTextField(
                    value = position,
                    onValueChange = { position = it },
                    label = { Text("Jabatan / Tugas") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) onSave(name, position)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandRed,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("employee_save_button")
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
fun OwnerActivityAndSettingsTab(
    state: OwnerUiState,
    themeMode: ThemeMode,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onUpdatePassword: (UserRole, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("SEMUA") }
    var showPasswordDialogRole by remember { mutableStateOf<UserRole?>(null) }

    val filters = listOf("SEMUA", "KASIR", "PULSA", "ABSENSI", "STOK", "KARYAWAN", "KEAMANAN")
    val filteredLogs = remember(state.activityLogs, selectedFilter) {
        if (selectedFilter == "SEMUA") state.activityLogs
        else state.activityLogs.filter { it.actionType == selectedFilter }
    }

    LazyColumn(
        modifier = modifier.background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Appearance / Dark Mode Selector Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(AppIcons.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Tampilan Aplikasi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Pilih tema Merah Putih (Terang), Mode Gelap, atau ikuti pengaturan perangkat.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = themeMode == mode,
                                onClick = { onSelectThemeMode(mode) },
                                label = { Text(mode.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandRed,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("theme_mode_${mode.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        }

        // Security Password Management Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(AppIcons.LockReset, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Keamanan & Kata Sandi Login",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Kata sandi disimpan aman dalam bentuk hash PBKDF2-HMAC-SHA256. Hanya Owner yang dapat mengganti kredensial.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showPasswordDialogRole = UserRole.OWNER },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("change_owner_password_button")
                        ) {
                            Text("Ubah Sandi Owner")
                        }
                        OutlinedButton(
                            onClick = { showPasswordDialogRole = UserRole.KARYAWAN },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("change_employee_password_button")
                        ) {
                            Text("Ubah Sandi Karyawan")
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Riwayat Aktivitas (${filteredLogs.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { f ->
                    FilterChip(
                        selected = selectedFilter == f,
                        onClick = { selectedFilter = f },
                        label = { Text(f) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandRed,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        if (filteredLogs.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(AppIcons.History, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text("Belum ada riwayat aktivitas pada filter ini.")
                    }
                }
            }
        } else {
            items(filteredLogs, key = { it.id }) { log ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "${log.actionType} • ${log.actorName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Text(
                                text = FormatUtils.formatDateTimeWib(log.createdAtMillis),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = log.description,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }

    showPasswordDialogRole?.let { targetRole ->
        var newPass by remember { mutableStateOf("") }
        var confirmPass by remember { mutableStateOf("") }
        var localErr by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showPasswordDialogRole = null },
            title = { Text("Ubah Kata Sandi ${targetRole.label}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newPass,
                        onValueChange = {
                            newPass = it
                            localErr = null
                        },
                        label = { Text("Kata Sandi Baru (min. 4 karakter)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmPass,
                        onValueChange = {
                            confirmPass = it
                            localErr = null
                        },
                        label = { Text("Konfirmasi Kata Sandi Baru") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (localErr != null) {
                        Text(localErr!!, color = DangerRed, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when {
                            newPass.length < 4 -> localErr = "Kata sandi minimal 4 karakter"
                            newPass != confirmPass -> localErr = "Konfirmasi kata sandi tidak cocok"
                            else -> {
                                onUpdatePassword(targetRole, newPass)
                                showPasswordDialogRole = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandRed,
                        contentColor = Color.White
                    )
                ) {
                    Text("Simpan Sandi")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialogRole = null }) { Text("Batal") }
            }
        )
    }
}
