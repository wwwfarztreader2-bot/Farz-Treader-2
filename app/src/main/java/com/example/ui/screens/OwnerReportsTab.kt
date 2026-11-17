package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.DateRange
import com.example.ui.components.AppIcons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.CashierTransactionWithItems
import com.example.data.local.PulsaStatus
import com.example.data.local.PulsaTransactionEntity
import com.example.data.local.ReportPeriod
import com.example.ui.theme.BrandRed
import com.example.ui.theme.BrandRedDark
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedContainer
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenContainer
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberContainer
import com.example.util.FormatUtils
import com.example.util.ReportExporter
import com.example.viewmodel.OwnerUiState
import com.example.viewmodel.OwnerViewModel

private enum class ReportHistorySection(val label: String) {
    BARANG_TERJUAL("Barang Terjual"),
    KASIR("Riwayat Kasir"),
    PULSA("Riwayat Pulsa")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerReportsTab(
    state: OwnerUiState,
    viewModel: OwnerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val report = state.reportData
    var showDateRangeDialog by remember { mutableStateOf(false) }
    var activeSection by remember { mutableStateOf(ReportHistorySection.BARANG_TERJUAL) }

    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            runCatching {
                val tempPdf = ReportExporter.buildReportPdfFile(context, report)
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    tempPdf.inputStream().use { input -> input.copyTo(out) }
                }
            }.onSuccess {
                Toast.makeText(context, "Laporan PDF berhasil disimpan", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "Gagal menyimpan PDF: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val saveCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            runCatching {
                val csvContent = ReportExporter.buildReportCsvContent(report)
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(csvContent.toByteArray(Charsets.UTF_8))
                }
            }.onSuccess {
                Toast.makeText(context, "Laporan Excel (.csv) berhasil disimpan", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "Gagal menyimpan Excel CSV: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LazyColumn(
        modifier = modifier.background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Period Filter Chips
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Filter Periode Laporan",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = report.periodTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReportPeriod.entries.forEach { period ->
                            FilterChip(
                                selected = state.reportPeriod == period,
                                onClick = {
                                    if (period == ReportPeriod.RENTANG) {
                                        showDateRangeDialog = true
                                    } else {
                                        viewModel.setReportPeriod(period)
                                    }
                                },
                                leadingIcon = if (period == ReportPeriod.RENTANG) {
                                    { Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                label = { Text(period.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandRed,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("report_period_${period.name.lowercase()}")
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    // Export & Print Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val fileName = "Laporan_Farnad_${FormatUtils.formatCompactDateForCode(System.currentTimeMillis())}.pdf"
                                savePdfLauncher.launch(fileName)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandRed,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("report_export_pdf_button")
                        ) {
                            Icon(AppIcons.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("PDF", style = MaterialTheme.typography.labelMedium)
                        }
                        Button(
                            onClick = {
                                val fileName = "Laporan_Farnad_${FormatUtils.formatCompactDateForCode(System.currentTimeMillis())}.csv"
                                saveCsvLauncher.launch(fileName)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ProfitGreen,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("report_export_excel_button")
                        ) {
                            Icon(AppIcons.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Excel", style = MaterialTheme.typography.labelMedium)
                        }
                        OutlinedButton(
                            onClick = {
                                val pdf = ReportExporter.buildReportPdfFile(context, report)
                                ReportExporter.printPdfFile(context, pdf, "Laporan_Farnad_Cellular")
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("report_print_button")
                        ) {
                            Icon(AppIcons.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Cetak", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = {
                            val pdf = ReportExporter.buildReportPdfFile(context, report)
                            ReportExporter.shareFile(context, pdf, "application/pdf", "Bagikan Laporan PDF")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(AppIcons.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Bagikan PDF ke WhatsApp / Email")
                    }
                }
            }
        }

        // Separated Financial Reports (Goods vs Pulsa vs Admin Revenue)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = BrandRedDark),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Ringkasan Keuangan Terpisah",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(10.dp))
                    ReportMoneyRow("Total Transaksi", "${report.totalTransactions} transaksi (${report.cashierCount} kasir, ${report.pulsaSuccessCount} pulsa)", Color.White)
                    ReportMoneyRow("Total Pemasukan Keseluruhan", FormatUtils.formatRupiah(report.totalGrossIncome), Color.White, bold = true)
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Total Keuntungan Usaha",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandRedDark
                                )
                                Text(
                                    text = "Laba Barang + Pendapatan Admin Pulsa",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF4B5563)
                                )
                            }
                            Text(
                                text = FormatUtils.formatRupiah(report.totalNetProfit),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BrandRedDark
                            )
                        }
                    }
                }
            }
        }

        // Two separate cards: 1) Penjualan Barang, 2) Pulsa & Pendapatan Admin
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("1. Penjualan Barang", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                        HorizontalDivider()
                        SmallStatRow("Omzet Barang", FormatUtils.formatRupiah(report.totalGoodsSales))
                        SmallStatRow("Total Modal", FormatUtils.formatRupiah(report.totalGoodsCost))
                        SmallStatRow("Laba Barang", FormatUtils.formatRupiah(report.totalGoodsProfit), ProfitGreen)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("2. Pulsa & Admin", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                        HorizontalDivider()
                        SmallStatRow("Nominal Pulsa", FormatUtils.formatRupiah(report.totalPulsaNominal))
                        SmallStatRow("Admin (Laba)", FormatUtils.formatRupiah(report.totalPulsaAdminFee), ProfitGreen)
                        SmallStatRow("Pending / Batal", "${report.pulsaPendingCount} / ${report.pulsaCanceledCount}")
                    }
                }
            }
        }

        // Section Switcher
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReportHistorySection.entries.forEach { sec ->
                    FilterChip(
                        selected = activeSection == sec,
                        onClick = { activeSection = sec },
                        label = { Text(sec.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandRed,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        when (activeSection) {
            ReportHistorySection.BARANG_TERJUAL -> {
                if (report.soldProducts.isEmpty()) {
                    item {
                        EmptyReportPlaceholder("Belum ada barang terjual pada periode ini.")
                    }
                } else {
                    items(report.soldProducts) { sold ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(sold.productName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text("Terjual: ${sold.quantitySold} unit", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(FormatUtils.formatRupiah(sold.totalRevenue), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text("Laba ${FormatUtils.formatRupiah(sold.totalProfit)}", style = MaterialTheme.typography.labelSmall, color = ProfitGreen, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
            ReportHistorySection.KASIR -> {
                if (report.cashierTransactions.isEmpty()) {
                    item {
                        EmptyReportPlaceholder("Belum ada transaksi kasir pada periode ini.")
                    }
                } else {
                    items(report.cashierTransactions, key = { it.transaction.id }) { entry ->
                        CashierHistoryRowCard(entry)
                    }
                }
            }
            ReportHistorySection.PULSA -> {
                if (report.pulsaTransactions.isEmpty()) {
                    item {
                        EmptyReportPlaceholder("Belum ada transaksi pulsa pada periode ini.")
                    }
                } else {
                    items(report.pulsaTransactions, key = { it.id }) { pulsa ->
                        PulsaHistoryRowCard(pulsa)
                    }
                }
            }
        }
    }

    if (showDateRangeDialog) {
        val pickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = state.customStartMillis,
            initialSelectedEndDateMillis = state.customEndMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDateRangeDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        val s = pickerState.selectedStartDateMillis
                        val e = pickerState.selectedEndDateMillis ?: s
                        if (s != null && e != null) {
                            viewModel.setCustomDateRange(s, e)
                        }
                        showDateRangeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandRed,
                        contentColor = Color.White
                    )
                ) {
                    Text("Terapkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDateRangeDialog = false }) { Text("Batal") }
            }
        ) {
            DateRangePicker(
                state = pickerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
            )
        }
    }
}

@Composable
private fun ReportMoneyRow(label: String, value: String, color: Color, bold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = color.copy(alpha = 0.85f),
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold
        )
    }
}

@Composable
private fun SmallStatRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

@Composable
private fun CashierHistoryRowCard(entry: CashierTransactionWithItems) {
    val tx = entry.transaction
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = tx.transactionNumber,
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = FormatUtils.formatDateTimeWib(tx.createdAtMillis),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Kasir: ${tx.employeeName} • ${tx.paymentMethod}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = entry.items.joinToString(", ") { "${it.productName} x${it.quantity}" },
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total: ${FormatUtils.formatRupiah(tx.totalAmount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Laba: ${FormatUtils.formatRupiah(tx.totalProfit)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = ProfitGreen
                )
            }
        }
    }
}

@Composable
private fun PulsaHistoryRowCard(pulsa: PulsaTransactionEntity) {
    val (badgeBg, badgeFg) = when (pulsa.status) {
        PulsaStatus.BERHASIL.label -> ProfitGreenContainer to ProfitGreen
        PulsaStatus.PENDING.label -> WarningAmberContainer to WarningAmber
        else -> DangerRedContainer to DangerRed
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${pulsa.serviceName} • ${pulsa.phoneNumber}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Surface(color = badgeBg, shape = RoundedCornerShape(6.dp)) {
                    Text(
                        text = pulsa.status,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeFg,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${pulsa.transactionNumber} • ${FormatUtils.formatDateTimeWib(pulsa.createdAtMillis)} • Kasir: ${pulsa.employeeName}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Nominal: ${FormatUtils.formatRupiah(pulsa.nominal)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Admin: ${FormatUtils.formatRupiah(pulsa.adminFee)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = "Bayar: ${FormatUtils.formatRupiah(pulsa.totalPayment)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EmptyReportPlaceholder(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(24.dp)
        )
    }
}
