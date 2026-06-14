package com.constructiontracker.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.constructiontracker.data.database.entities.PaymentEntity
import com.constructiontracker.data.database.entities.PurchaseEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ExportFormat { PDF, IMAGE }
enum class ExportContent { PAYMENTS_ONLY, PURCHASES_ONLY, BOTH }

private const val DOC_WIDTH = 595
private const val MARGIN = 40

fun generateExportFile(
    context: Context,
    contractorName: String,
    contractType: String,
    totalPaid: Double,
    allPayments: List<PaymentEntity>,
    allPurchases: List<PurchaseEntity>,
    format: ExportFormat,
    content: ExportContent,
    fromDate: Long?,
    toDate: Long?
): File? {
    val endOfDay = toDate?.let { it + 86_399_999L }

    val payments = when (content) {
        ExportContent.PURCHASES_ONLY -> emptyList()
        else -> allPayments.filter { p ->
            (fromDate == null || p.date >= fromDate) &&
            (endOfDay == null || p.date <= endOfDay)
        }.sortedByDescending { it.date }
    }

    val purchases = when (content) {
        ExportContent.PAYMENTS_ONLY -> emptyList()
        else -> allPurchases.filter { p ->
            (fromDate == null || p.date >= fromDate) &&
            (endOfDay == null || p.date <= endOfDay)
        }.sortedByDescending { it.date }
    }

    return when (format) {
        ExportFormat.PDF -> makePdf(context, contractorName, contractType, totalPaid, payments, purchases, fromDate, toDate)
        ExportFormat.IMAGE -> makeImage(context, contractorName, contractType, totalPaid, payments, purchases, fromDate, toDate)
    }
}

private fun makePdf(
    context: Context,
    contractorName: String,
    contractType: String,
    totalPaid: Double,
    payments: List<PaymentEntity>,
    purchases: List<PurchaseEntity>,
    fromDate: Long?,
    toDate: Long?
): File? = runCatching {
    val contentHeight = renderContent(null, contractorName, contractType, totalPaid, payments, purchases, fromDate, toDate)
    val pageHeight = (contentHeight + MARGIN).toInt().coerceAtLeast(200)

    val pdf = PdfDocument()
    val page = pdf.startPage(PdfDocument.PageInfo.Builder(DOC_WIDTH, pageHeight, 1).create())
    renderContent(page.canvas, contractorName, contractType, totalPaid, payments, purchases, fromDate, toDate)
    pdf.finishPage(page)

    val fileName = "${contractorName.replace(" ", "_")}_report_${System.currentTimeMillis()}.pdf"
    val file = File(context.cacheDir, fileName)
    FileOutputStream(file).use { pdf.writeTo(it) }
    pdf.close()
    file
}.getOrNull()

private fun makeImage(
    context: Context,
    contractorName: String,
    contractType: String,
    totalPaid: Double,
    payments: List<PaymentEntity>,
    purchases: List<PurchaseEntity>,
    fromDate: Long?,
    toDate: Long?
): File? = runCatching {
    val scale = 2
    val contentHeight = renderContent(null, contractorName, contractType, totalPaid, payments, purchases, fromDate, toDate)
    val imgHeight = ((contentHeight + MARGIN) * scale).toInt().coerceAtLeast(200)

    val bitmap = Bitmap.createBitmap(DOC_WIDTH * scale, imgHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(Color.WHITE)
    canvas.scale(scale.toFloat(), scale.toFloat())
    renderContent(canvas, contractorName, contractType, totalPaid, payments, purchases, fromDate, toDate)

    val fileName = "${contractorName.replace(" ", "_")}_report_${System.currentTimeMillis()}.png"
    val file = File(context.cacheDir, fileName)
    FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 95, it) }
    file
}.getOrNull()

// Returns the final y position (content height including top margin). Draws when canvas != null.
private fun renderContent(
    canvas: Canvas?,
    contractorName: String,
    contractType: String,
    totalPaid: Double,
    payments: List<PaymentEntity>,
    purchases: List<PurchaseEntity>,
    fromDate: Long?,
    toDate: Long?
): Float {
    val ml = MARGIN.toFloat()
    val mr = (DOC_WIDTH - MARGIN).toFloat()
    val df = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    fun paint(
        size: Float,
        bold: Boolean = false,
        clr: Int = Color.BLACK,
        align: Paint.Align = Paint.Align.LEFT
    ) = Paint().apply {
        textSize = size
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        color = clr
        textAlign = align
        isAntiAlias = true
    }

    val pTitle  = paint(18f, bold = true)
    val pSec    = paint(13f, bold = true)
    val pBody   = paint(10f)
    val pMeta   = paint(9f, clr = Color.GRAY)
    val pAmtPay = paint(10f, bold = true, clr = Color.parseColor("#1565C0"), align = Paint.Align.RIGHT)
    val pAmtPur = paint(10f, bold = true, clr = Color.parseColor("#2E7D32"), align = Paint.Align.RIGHT)
    val pTotPay = paint(11f, bold = true, clr = Color.parseColor("#1565C0"), align = Paint.Align.RIGHT)
    val pTotPur = paint(11f, bold = true, clr = Color.parseColor("#2E7D32"), align = Paint.Align.RIGHT)
    val pGrand  = paint(13f, bold = true, align = Paint.Align.RIGHT)
    val pLine   = Paint().apply { color = Color.LTGRAY; strokeWidth = 0.5f; isAntiAlias = true }

    var y = MARGIN.toFloat()

    // Draw text with top of glyph at current y
    fun dt(p: Paint, s: String, x: Float = ml) { canvas?.drawText(s, x, y - p.ascent(), p) }
    // Advance y by text height + gap
    fun nl(h: Float, gap: Float = 4f) { y += h + gap }
    // Horizontal rule
    fun hr(gap: Float = 6f) { canvas?.drawLine(ml, y, mr, y, pLine); y += gap }

    // Title row (title left, generation date right, same baseline)
    dt(pTitle, "CONTRACTOR REPORT")
    pMeta.textAlign = Paint.Align.RIGHT
    canvas?.drawText("Generated: ${df.format(Date())}", mr, y - pMeta.ascent(), pMeta)
    pMeta.textAlign = Paint.Align.LEFT
    nl(pTitle.textSize, 10f)

    // Info block
    dt(pBody, "Contractor: $contractorName");                                          nl(pBody.textSize, 3f)
    dt(pBody, "Contract Type: ${if (contractType == "FIXED") "Fixed Contract" else "Open Ended"}"); nl(pBody.textSize, 3f)
    dt(pBody, "Total Paid: ${formatCurrency(totalPaid)}");                             nl(pBody.textSize, 3f)

    val periodText = when {
        fromDate != null && toDate != null -> "Period: ${df.format(Date(fromDate))} – ${df.format(Date(toDate))}"
        fromDate != null                   -> "Period: From ${df.format(Date(fromDate))}"
        toDate   != null                   -> "Period: Up to ${df.format(Date(toDate))}"
        else                               -> "Period: All Records"
    }
    dt(pBody, periodText); nl(pBody.textSize, 12f)
    hr()

    // Payments section
    if (payments.isNotEmpty()) {
        dt(pSec, "PAYMENTS  (${payments.size} record${if (payments.size != 1) "s" else ""})"); nl(pSec.textSize, 8f)
        hr()
        for (pmt in payments) {
            dt(pBody, df.format(Date(pmt.date)))
            dt(pAmtPay, formatCurrency(pmt.amount), x = mr)
            nl(pBody.textSize, 2f)
            if (pmt.workDescription.isNotBlank()) {
                dt(pBody, pmt.workDescription.take(70)); nl(pBody.textSize, 2f)
            }
            if (pmt.bankReference.isNotBlank()) dt(pMeta, "Ref: ${pmt.bankReference}")
            pMeta.textAlign = Paint.Align.RIGHT
            dt(pMeta, if (pmt.receiptReceived) "Receipt: Yes" else "Receipt: No", x = mr)
            pMeta.textAlign = Paint.Align.LEFT
            nl(pMeta.textSize, 2f)
            hr(6f)
        }
        dt(pTotPay, "Payments Total: ${formatCurrency(payments.sumOf { it.amount })}", x = mr)
        nl(pTotPay.textSize, 16f)
    }

    // Purchases section
    if (purchases.isNotEmpty()) {
        dt(pSec, "PURCHASES  (${purchases.size} record${if (purchases.size != 1) "s" else ""})"); nl(pSec.textSize, 8f)
        hr()
        for (pur in purchases) {
            dt(pBody, df.format(Date(pur.date)))
            dt(pAmtPur, formatCurrency(pur.amount), x = mr)
            nl(pBody.textSize, 2f)
            dt(pBody, "${pur.itemName}  ·  ${pur.category}".take(70)); nl(pBody.textSize, 2f)
            if (pur.shopName.isNotBlank()) dt(pMeta, pur.shopName)
            pMeta.textAlign = Paint.Align.RIGHT
            dt(pMeta, if (pur.receiptReceived) "Receipt: Yes" else "Receipt: No", x = mr)
            pMeta.textAlign = Paint.Align.LEFT
            if (pur.notes.isNotBlank()) {
                nl(pMeta.textSize, 2f); dt(pMeta, pur.notes.take(70))
            }
            nl(pMeta.textSize, 2f)
            hr(6f)
        }
        dt(pTotPur, "Purchases Total: ${formatCurrency(purchases.sumOf { it.amount })}", x = mr)
        nl(pTotPur.textSize, 16f)
    }

    // Grand total
    if (payments.isNotEmpty() || purchases.isNotEmpty()) {
        hr()
        val gt = payments.sumOf { it.amount } + purchases.sumOf { it.amount }
        dt(pGrand, "GRAND TOTAL: ${formatCurrency(gt)}", x = mr)
        nl(pGrand.textSize, 20f)
    }

    return y
}
