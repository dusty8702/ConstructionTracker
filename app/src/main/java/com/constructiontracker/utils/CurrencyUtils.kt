package com.constructiontracker.utils

import java.text.NumberFormat
import java.util.Locale

fun formatCurrency(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("en", "LK"))
    formatter.minimumFractionDigits = 2
    formatter.maximumFractionDigits = 2
    return "Rs. ${formatter.format(amount)}"
}
