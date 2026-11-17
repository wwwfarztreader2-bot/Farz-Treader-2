package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.AttendanceRecordEntity
import com.example.data.local.AttendanceType
import com.example.data.local.CashierTransactionWithItems
import com.example.ui.theme.BrandRed
import com.example.ui.theme.BrandRedBright
import com.example.ui.theme.BrandRedDark
import com.example.ui.theme.CharcoalBrand
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedContainer
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenContainer
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberContainer
import com.example.util.FormatUtils
import com.example.util.ReportExporter
import com.example.viewmodel.ChartRange
import com.example.viewmodel.SalesChartPoint
import java.io.File
import java.util.Locale

@Composable
fun FarnadLogoBadge(
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            modifier = Modifier.size(if (compact) 38.dp else 48.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(5.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_farnad_mark),
                    contentDescription = "Logo FARNAD CELL",
                    modifier = Modifier.size(if (compact) 28.dp else 36.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = "FARNAD CELL",
                style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!compact) {
                Text(
                    text = "Sistem Manajemen Usaha",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StatMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color = BrandRed,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        ),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = accentColor.copy(alpha = 0.12f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun StockAlertBanner(
    outOfStockCount: Int,
    lowStockCount: Int,
    onViewInventory: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = outOfStockCount > 0 || lowStockCount > 0) {
        val isCritical = outOfStockCount > 0
        val bg = if (isCritical) DangerRedContainer else WarningAmberContainer
        val fg = if (isCritical) DangerRed else WarningAmber
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = bg),
            border = BorderStroke(1.dp, fg.copy(alpha = 0.5f)),
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = fg.copy(alpha = 0.16f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Peringatan Stok",
                            tint = fg
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Peringatan Stok Otomatis",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = fg
                    )
                    Text(
                        text = buildString {
                            if (outOfStockCount > 0) append("$outOfStockCount barang stok kosong")
                            if (outOfStockCount > 0 && lowStockCount > 0) append(" • ")
                            if (lowStockCount > 0) append("$lowStockCount barang hampir habis")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF1E293B)
                    )
                }
                TextButton(
                    onClick = onViewInventory,
                    modifier = Modifier.testTag("alert_view_inventory_button")
                ) {
                    Text("Kelola", color = fg, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SalesChartCard(
    selectedRange: ChartRange,
    points: List<SalesChartPoint>,
    onSelectRange: (ChartRange) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Grafik Penjualan & Admin Pulsa",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Perbandingan omzet barang dan pendapatan admin",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChartRange.entries.forEach { range ->
                    FilterChip(
                        selected = selectedRange == range,
                        onClick = { onSelectRange(range) },
                        label = { Text(range.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandRed,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("chart_range_${range.name.lowercase()}")
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            val secondaryBarColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
            // Legend
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(BrandRed)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Penjualan Barang", style = MaterialTheme.typography.labelSmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(secondaryBarColor)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Admin Pulsa", style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.height(14.dp))

            val maxValue = (points.maxOfOrNull { maxOf(it.goodsSales, it.pulsaAdminRevenue) } ?: 0L)
                .coerceAtLeast(10_000L)

            if (points.all { it.goodsSales == 0L && it.pulsaAdminRevenue == 0L }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Grafik akan terisi otomatis saat ada transaksi masuk",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    val count = points.size.coerceAtLeast(1)
                    val groupWidth = size.width / count
                    val barWidth = (groupWidth * 0.28f).coerceIn(8.dp.toPx(), 24.dp.toPx())
                    val gap = 4.dp.toPx()
                    val chartHeight = size.height - 8.dp.toPx()

                    // Grid lines
                    for (i in 0..3) {
                        val y = chartHeight * (i / 3f)
                        drawLine(
                            color = Color(0xFFCBD5E1).copy(alpha = 0.5f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    points.forEachIndexed { index, point ->
                        val centerX = groupWidth * index + groupWidth / 2f
                        val goodsH = ((point.goodsSales.toFloat() / maxValue.toFloat()) * chartHeight)
                            .coerceAtLeast(if (point.goodsSales > 0) 6.dp.toPx() else 2.dp.toPx())
                        val adminH = ((point.pulsaAdminRevenue.toFloat() / maxValue.toFloat()) * chartHeight)
                            .coerceAtLeast(if (point.pulsaAdminRevenue > 0) 6.dp.toPx() else 2.dp.toPx())

                        drawRoundRect(
                            color = BrandRed,
                            topLeft = Offset(centerX - barWidth - gap / 2f, chartHeight - goodsH),
                            size = Size(barWidth, goodsH),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                        drawRoundRect(
                            color = secondaryBarColor,
                            topLeft = Offset(centerX + gap / 2f, chartHeight - adminH),
                            size = Size(barWidth, adminH),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    points.forEach { point ->
                        Text(
                            text = point.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AttendanceResultCard(
    record: AttendanceRecordEntity,
    onPhotoClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val type = AttendanceType.fromKey(record.attendanceType)
    val badgeBg = when (type) {
        AttendanceType.SAKIT -> WarningAmberContainer
        AttendanceType.IZIN -> InfoBlue.copy(alpha = 0.14f)
        AttendanceType.HADIR -> if (record.checkOutMillis != null) {
            ProfitGreenContainer
        } else {
            MaterialTheme.colorScheme.primaryContainer
        }
    }
    val badgeFg = when (type) {
        AttendanceType.SAKIT -> WarningAmber
        AttendanceType.IZIN -> InfoBlue
        AttendanceType.HADIR -> if (record.checkOutMillis != null) {
            ProfitGreen
        } else {
            MaterialTheme.colorScheme.onPrimaryContainer
        }
    }
    val badgeLabel = when (type) {
        AttendanceType.SAKIT -> "Sakit"
        AttendanceType.IZIN -> "Izin"
        AttendanceType.HADIR -> if (record.checkOutMillis != null) "Masuk & Pulang" else "Sudah Masuk"
    }
    val borderAccent = when (type) {
        AttendanceType.SAKIT -> WarningAmber.copy(alpha = 0.45f)
        AttendanceType.IZIN -> InfoBlue.copy(alpha = 0.40f)
        AttendanceType.HADIR -> BrandRed.copy(alpha = 0.28f)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, borderAccent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = BrandRed,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "KODE ABSENSI: ${record.attendanceCode}",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = JetBrainsMonoFamily,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = badgeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeFg,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            val hasPhoto = record.checkInPhotoPath.isNotBlank() && File(record.checkInPhotoPath).exists()
            Row(modifier = Modifier.fillMaxWidth()) {
                val photoFile = File(record.checkInPhotoPath)
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        .clickable(enabled = onPhotoClick != null && hasPhoto) {
                            onPhotoClick?.invoke(record.checkInPhotoPath)
                        }
                ) {
                    if (hasPhoto) {
                        AsyncImage(
                            model = photoFile,
                            contentDescription = "Foto Absensi ${record.employeeName}",
                            modifier = Modifier.size(88.dp),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        val placeholderIcon = when (type) {
                            AttendanceType.SAKIT -> AppIcons.MedicalServices
                            AttendanceType.IZIN -> AppIcons.EventNote
                            AttendanceType.HADIR -> AppIcons.Storefront
                        }
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .background(badgeBg.copy(alpha = 0.65f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = placeholderIcon,
                                    contentDescription = badgeLabel,
                                    tint = badgeFg,
                                    modifier = Modifier.size(28.dp)
                                )
                                if (type != AttendanceType.HADIR) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = type.label.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = badgeFg,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    AttendanceFieldRow("NAMA", record.employeeName)
                    AttendanceFieldRow("STATUS", type.label)
                    AttendanceFieldRow("HARI", record.dayName)
                    AttendanceFieldRow("TANGGAL", record.dateFormatted)
                    AttendanceFieldRow("JAM", record.checkInTimeFormatted)
                }
            }

            if (record.notes.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    color = badgeBg.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "KETERANGAN: ${record.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }
            }

            if (record.checkInAddress.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Column {
                        Text(
                            text = "ALAMAT: ${record.checkInAddress}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (record.checkInLat != 0.0 || record.checkInLng != 0.0) {
                            Text(
                                text = String.format(Locale.US, "GPS: %.5f, %.5f", record.checkInLat, record.checkInLng),
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (type == AttendanceType.HADIR && record.checkOutMillis != null) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.AccessTime,
                        contentDescription = null,
                        tint = ProfitGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "ABSENSI PULANG: ${record.checkOutTimeFormatted ?: "-"} • ${record.checkOutAddress ?: "-"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ProfitGreen,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    val outPath = record.checkOutPhotoPath
                    if (outPath != null && File(outPath).exists() && onPhotoClick != null) {
                        TextButton(onClick = { onPhotoClick(outPath) }) {
                            Text("Foto Pulang")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttendanceFieldRow(label: String, value: String) {
    Row {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ReceiptDialog(
    receipt: CashierTransactionWithItems,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val tx = receipt.transaction

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "FARNAD CELL",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = "Struk Transaksi Resmi",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(Modifier.height(10.dp))
                ReceiptLine("No. Transaksi", tx.transactionNumber, mono = true)
                ReceiptLine("Waktu", FormatUtils.formatDateTimeWib(tx.createdAtMillis))
                ReceiptLine("Kasir", tx.employeeName)
                ReceiptLine("Metode", tx.paymentMethod)
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                receipt.items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.productName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${item.quantity} x ${FormatUtils.formatRupiah(item.sellPrice)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = FormatUtils.formatRupiah(item.subtotal),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                ReceiptLine("TOTAL", FormatUtils.formatRupiah(tx.totalAmount), bold = true)
                ReceiptLine("DIBAYAR", FormatUtils.formatRupiah(tx.cashReceived))
                ReceiptLine("KEMBALIAN", FormatUtils.formatRupiah(tx.changeAmount), bold = true)

                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val pdf = ReportExporter.buildReceiptPdfFile(context, receipt)
                            ReportExporter.shareFile(
                                context,
                                pdf,
                                "application/pdf",
                                "Bagikan Struk PDF"
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("receipt_share_pdf_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Simpan/Bagikan")
                    }
                    Button(
                        onClick = {
                            val pdf = ReportExporter.buildReceiptPdfFile(context, receipt)
                            ReportExporter.printPdfFile(context, pdf, "Struk_${tx.transactionNumber}")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandRed,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("receipt_print_button")
                    ) {
                        Icon(AppIcons.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Cetak")
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.End)
                        .testTag("receipt_close_button")
                ) {
                    Text("Tutup")
                }
            }
        }
    }
}

@Composable
private fun ReceiptLine(
    label: String,
    value: String,
    bold: Boolean = false,
    mono: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = if (mono) JetBrainsMonoFamily else MaterialTheme.typography.bodySmall.fontFamily,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium
        )
    }
}
