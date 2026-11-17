package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import com.example.data.local.CashierTransactionWithItems
import com.example.data.local.PulsaStatus
import com.example.data.local.PulsaTransactionEntity
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

data class SoldProductSummary(
    val productName: String,
    val quantitySold: Int,
    val totalRevenue: Long,
    val totalProfit: Long
)

data class FinancialReportData(
    val periodTitle: String,
    val totalTransactions: Int,
    val cashierCount: Int,
    val pulsaSuccessCount: Int,
    val pulsaPendingCount: Int,
    val pulsaCanceledCount: Int,
    val totalGoodsSales: Long,
    val totalGoodsCost: Long,
    val totalGoodsProfit: Long,
    val totalPulsaNominal: Long,
    val totalPulsaAdminFee: Long,
    val totalPulsaPayment: Long,
    val totalGrossIncome: Long,
    val totalNetProfit: Long,
    val soldProducts: List<SoldProductSummary>,
    val cashierTransactions: List<CashierTransactionWithItems>,
    val pulsaTransactions: List<PulsaTransactionEntity>
)

object ReportExporter {

    fun buildReportPdfFile(context: Context, report: FinancialReportData): File {
        val dir = File(context.filesDir, "exports").apply { mkdirs() }
        val file = File(dir, "Laporan_Farnad_Cellular_${System.currentTimeMillis()}.pdf")

        val pdf = PdfDocument()
        val pageWidth = 595 // A4 points
        val pageHeight = 842
        val margin = 40f

        val titlePaint = Paint().apply {
            color = Color.rgb(13, 37, 79)
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitlePaint = Paint().apply {
            color = Color.rgb(71, 85, 105)
            textSize = 11f
        }
        val sectionPaint = Paint().apply {
            color = Color.rgb(13, 37, 79)
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 10.5f
        }
        val boldBodyPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val linePaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            strokeWidth = 1f
        }

        var pageNumber = 1
        var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        var y = 48f

        fun ensureSpace(needed: Float) {
            if (y + needed > pageHeight - 40f) {
                pdf.finishPage(page)
                pageNumber++
                page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = page.canvas
                y = 48f
            }
        }

        // Header
        canvas.drawText("FARNAD CELL — LAPORAN KEUANGAN", margin, y, titlePaint)
        y += 18f
        canvas.drawText("Periode: ${report.periodTitle}  |  Dicetak: ${FormatUtils.formatDateTimeWib(System.currentTimeMillis())}", margin, y, subtitlePaint)
        y += 12f
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 20f

        // Ringkasan Keuangan
        canvas.drawText("1. RINGKASAN KEUANGAN (TERPISAH)", margin, y, sectionPaint)
        y += 16f
        val summaryLines = listOf(
            "Total Transaksi (Kasir + Pulsa)" to "${report.totalTransactions} transaksi (${report.cashierCount} kasir, ${report.pulsaSuccessCount} pulsa berhasil)",
            "Total Penjualan Barang (Omzet)" to FormatUtils.formatRupiah(report.totalGoodsSales),
            "Total Harga Modal Barang" to FormatUtils.formatRupiah(report.totalGoodsCost),
            "Keuntungan Penjualan Barang (Jual - Modal)" to FormatUtils.formatRupiah(report.totalGoodsProfit),
            "Total Nominal Pulsa Berhasil (Bukan Laba)" to FormatUtils.formatRupiah(report.totalPulsaNominal),
            "Pendapatan Biaya Admin Pulsa (Laba Admin)" to FormatUtils.formatRupiah(report.totalPulsaAdminFee),
            "Total Pemasukan Keseluruhan" to FormatUtils.formatRupiah(report.totalGrossIncome),
            "Total Laba Usaha (Laba Barang + Admin Pulsa)" to FormatUtils.formatRupiah(report.totalNetProfit)
        )
        summaryLines.forEachIndexed { idx, (label, value) ->
            ensureSpace(16f)
            val paint = if (idx >= summaryLines.size - 2) boldBodyPaint else bodyPaint
            canvas.drawText(label, margin, y, paint)
            canvas.drawText(value, 360f, y, paint)
            y += 15f
        }
        y += 12f

        // Daftar Barang Terjual
        ensureSpace(40f)
        canvas.drawText("2. DAFTAR BARANG TERJUAL (${report.soldProducts.size} SKU)", margin, y, sectionPaint)
        y += 16f
        if (report.soldProducts.isEmpty()) {
            canvas.drawText("- Tidak ada penjualan barang pada periode ini.", margin, y, bodyPaint)
            y += 16f
        } else {
            canvas.drawText("Nama Barang", margin, y, boldBodyPaint)
            canvas.drawText("Qty", 310f, y, boldBodyPaint)
            canvas.drawText("Omzet", 365f, y, boldBodyPaint)
            canvas.drawText("Laba", 465f, y, boldBodyPaint)
            y += 13f
            report.soldProducts.forEach { item ->
                ensureSpace(15f)
                canvas.drawText(item.productName.take(40), margin, y, bodyPaint)
                canvas.drawText("${item.quantitySold}", 310f, y, bodyPaint)
                canvas.drawText(FormatUtils.formatRupiah(item.totalRevenue), 365f, y, bodyPaint)
                canvas.drawText(FormatUtils.formatRupiah(item.totalProfit), 465f, y, bodyPaint)
                y += 14f
            }
        }
        y += 12f

        // Riwayat Transaksi Kasir
        ensureSpace(40f)
        canvas.drawText("3. RIWAYAT TRANSAKSI KASIR (${report.cashierTransactions.size})", margin, y, sectionPaint)
        y += 16f
        if (report.cashierTransactions.isEmpty()) {
            canvas.drawText("- Belum ada transaksi kasir pada periode ini.", margin, y, bodyPaint)
            y += 16f
        } else {
            report.cashierTransactions.forEach { entry ->
                ensureSpace(28f)
                val tx = entry.transaction
                canvas.drawText(
                    "${tx.transactionNumber} | ${FormatUtils.formatDateTimeWib(tx.createdAtMillis)} | Kasir: ${tx.employeeName}",
                    margin,
                    y,
                    boldBodyPaint
                )
                y += 13f
                val itemsText = entry.items.joinToString(", ") { "${it.productName} x${it.quantity}" }
                canvas.drawText(
                    "   ${tx.paymentMethod} • Total: ${FormatUtils.formatRupiah(tx.totalAmount)} (Laba: ${FormatUtils.formatRupiah(tx.totalProfit)}) • $itemsText".take(92),
                    margin,
                    y,
                    bodyPaint
                )
                y += 14f
            }
        }
        y += 12f

        // Riwayat Transaksi Pulsa
        ensureSpace(40f)
        canvas.drawText("4. RIWAYAT TRANSAKSI PULSA & LAYANAN DIGITAL (${report.pulsaTransactions.size})", margin, y, sectionPaint)
        y += 16f
        if (report.pulsaTransactions.isEmpty()) {
            canvas.drawText("- Belum ada transaksi pulsa pada periode ini.", margin, y, bodyPaint)
            y += 16f
        } else {
            report.pulsaTransactions.forEach { p ->
                ensureSpace(28f)
                canvas.drawText(
                    "${p.transactionNumber} | ${FormatUtils.formatDateTimeWib(p.createdAtMillis)} | ${p.employeeName} | [${p.status}]",
                    margin,
                    y,
                    boldBodyPaint
                )
                y += 13f
                canvas.drawText(
                    "   ${p.serviceName} (${p.phoneNumber}) • Nominal: ${FormatUtils.formatRupiah(p.nominal)} + Admin: ${FormatUtils.formatRupiah(p.adminFee)} = ${FormatUtils.formatRupiah(p.totalPayment)}",
                    margin,
                    y,
                    bodyPaint
                )
                y += 14f
            }
        }

        pdf.finishPage(page)
        FileOutputStream(file).use { out ->
            pdf.writeTo(out)
        }
        pdf.close()
        return file
    }

    fun buildReceiptPdfFile(context: Context, txWithItems: CashierTransactionWithItems): File {
        val dir = File(context.filesDir, "exports").apply { mkdirs() }
        val file = File(dir, "Struk_${txWithItems.transaction.transactionNumber}.pdf")

        val pdf = PdfDocument()
        val pageWidth = 340
        val estimatedHeight = 280 + (txWithItems.items.size * 28)
        val page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, estimatedHeight, 1).create())
        val canvas = page.canvas

        val headerPaint = Paint().apply {
            color = Color.BLACK
            textSize = 14f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val monoPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            typeface = Typeface.MONOSPACE
        }
        val monoBold = Paint().apply {
            color = Color.BLACK
            textSize = 10.5f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        val tx = txWithItems.transaction
        var y = 32f
        val x = 20f
        canvas.drawText("FARNAD CELL", x, y, headerPaint)
        y += 16f
        canvas.drawText("Struk Pembayaran Resmi", x, y, monoPaint)
        y += 14f
        canvas.drawText("--------------------------------", x, y, monoPaint)
        y += 14f
        canvas.drawText("No     : ${tx.transactionNumber}", x, y, monoPaint)
        y += 14f
        canvas.drawText("Waktu  : ${FormatUtils.formatDateTimeWib(tx.createdAtMillis)}", x, y, monoPaint)
        y += 14f
        canvas.drawText("Kasir  : ${tx.employeeName}", x, y, monoPaint)
        y += 14f
        canvas.drawText("Metode : ${tx.paymentMethod}", x, y, monoPaint)
        y += 14f
        canvas.drawText("--------------------------------", x, y, monoPaint)
        y += 14f

        txWithItems.items.forEach { item ->
            canvas.drawText(item.productName.take(32), x, y, monoBold)
            y += 13f
            canvas.drawText(
                "  ${item.quantity} x ${FormatUtils.formatRupiah(item.sellPrice)} = ${FormatUtils.formatRupiah(item.subtotal)}",
                x,
                y,
                monoPaint
            )
            y += 15f
        }

        canvas.drawText("--------------------------------", x, y, monoPaint)
        y += 16f
        canvas.drawText("TOTAL     : ${FormatUtils.formatRupiah(tx.totalAmount)}", x, y, monoBold)
        y += 15f
        canvas.drawText("DIBAYAR   : ${FormatUtils.formatRupiah(tx.cashReceived)}", x, y, monoPaint)
        y += 15f
        canvas.drawText("KEMBALIAN : ${FormatUtils.formatRupiah(tx.changeAmount)}", x, y, monoBold)
        y += 22f
        canvas.drawText("Terima kasih telah berbelanja!", x, y, monoPaint)

        pdf.finishPage(page)
        FileOutputStream(file).use { pdf.writeTo(it) }
        pdf.close()
        return file
    }

    fun buildReportCsvContent(report: FinancialReportData): String {
        val sb = StringBuilder()
        // UTF-8 BOM for Microsoft Excel compatibility
        sb.append('\uFEFF')
        sb.appendLine("LAPORAN KEUANGAN FARNAD CELL")
        sb.appendLine("Periode;${escapeCsv(report.periodTitle)}")
        sb.appendLine("Dicetak;${escapeCsv(FormatUtils.formatDateTimeWib(System.currentTimeMillis()))}")
        sb.appendLine()
        sb.appendLine("RINGKASAN KEUANGAN")
        sb.appendLine("Kategori;Nilai")
        sb.appendLine("Total Transaksi;${report.totalTransactions}")
        sb.appendLine("Transaksi Kasir;${report.cashierCount}")
        sb.appendLine("Transaksi Pulsa Berhasil;${report.pulsaSuccessCount}")
        sb.appendLine("Total Penjualan Barang (Omzet);${report.totalGoodsSales}")
        sb.appendLine("Total Harga Modal Barang;${report.totalGoodsCost}")
        sb.appendLine("Keuntungan Penjualan Barang;${report.totalGoodsProfit}")
        sb.appendLine("Total Nominal Pulsa Berhasil (Bukan Keuntungan);${report.totalPulsaNominal}")
        sb.appendLine("Pendapatan Biaya Admin Pulsa;${report.totalPulsaAdminFee}")
        sb.appendLine("Total Pemasukan Keseluruhan;${report.totalGrossIncome}")
        sb.appendLine("Total Laba Bersih Usaha;${report.totalNetProfit}")
        sb.appendLine()
        sb.appendLine("DAFTAR BARANG TERJUAL")
        sb.appendLine("Nama Barang;Jumlah Terjual;Total Omzet;Total Laba")
        report.soldProducts.forEach { item ->
            sb.appendLine("${escapeCsv(item.productName)};${item.quantitySold};${item.totalRevenue};${item.totalProfit}")
        }
        sb.appendLine()
        sb.appendLine("RIWAYAT TRANSAKSI KASIR")
        sb.appendLine("No Transaksi;Tanggal;Karyawan;Metode;Total Belanja;Total Modal;Keuntungan;Rincian Barang")
        report.cashierTransactions.forEach { entry ->
            val tx = entry.transaction
            val itemsDesc = entry.items.joinToString(", ") { "${it.productName} (${it.quantity})" }
            sb.appendLine(
                "${tx.transactionNumber};${FormatUtils.formatDateTimeWib(tx.createdAtMillis)};${escapeCsv(tx.employeeName)};${tx.paymentMethod};${tx.totalAmount};${tx.totalCost};${tx.totalProfit};${escapeCsv(itemsDesc)}"
            )
        }
        sb.appendLine()
        sb.appendLine("RIWAYAT TRANSAKSI PULSA & LAYANAN DIGITAL")
        sb.appendLine("No Transaksi;Tanggal;Karyawan;Layanan;Nomor HP;Nominal;Modal;Biaya Admin;Total Bayar;Metode;Status")
        report.pulsaTransactions.forEach { p ->
            sb.appendLine(
                "${p.transactionNumber};${FormatUtils.formatDateTimeWib(p.createdAtMillis)};${escapeCsv(p.employeeName)};${escapeCsv(p.serviceName)};${escapeCsv(p.phoneNumber)};${p.nominal};${p.costPrice};${p.adminFee};${p.totalPayment};${p.paymentMethod};${p.status}"
            )
        }
        return sb.toString()
    }

    fun buildReportCsvFile(context: Context, report: FinancialReportData): File {
        val dir = File(context.filesDir, "exports").apply { mkdirs() }
        val file = File(dir, "Laporan_Farnad_Cellular_${System.currentTimeMillis()}.csv")
        file.writeText(buildReportCsvContent(report), Charsets.UTF_8)
        return file
    }

    fun printPdfFile(context: Context, pdfFile: File, jobName: String) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder(pdfFile.name)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .build()
                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                try {
                    FileInputStream(pdfFile).use { input ->
                        FileOutputStream(destination?.fileDescriptor).use { output ->
                            input.copyTo(output)
                        }
                    }
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.localizedMessage)
                }
            }
        }
        printManager.print(jobName, adapter, null)
    }

    fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }

    private fun escapeCsv(value: String): String {
        val cleaned = value.replace("\"", "\"\"").replace("\n", " ")
        return "\"$cleaned\""
    }
}
