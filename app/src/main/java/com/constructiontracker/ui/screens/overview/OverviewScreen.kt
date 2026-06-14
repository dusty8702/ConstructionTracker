package com.constructiontracker.ui.screens.overview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.constructiontracker.data.database.entities.PaymentEntity
import com.constructiontracker.data.database.entities.PurchaseEntity
import com.constructiontracker.ui.theme.ContractorColors
import com.constructiontracker.utils.formatCurrency
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(viewModel: OverviewViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedSummary by remember { mutableStateOf<ContractorSummary?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (selectedSummary != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedSummary = null },
            sheetState = sheetState
        ) {
            ContractorDetailSheet(
                summary = selectedSummary!!,
                purchases = uiState.purchases
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Construction Tracker") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item { GrandTotalCard(uiState) }
                item {
                    Text(
                        text = "Contractors",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                itemsIndexed(uiState.contractorSummaries) { index, summary ->
                    ContractorCard(
                        summary = summary,
                        color = ContractorColors.getOrElse(index) { MaterialTheme.colorScheme.primary },
                        onClick = { selectedSummary = summary }
                    )
                }
                item { MaterialsTotalCard(uiState.totalPurchases) }
            }
        }
    }
}

@Composable
private fun ContractorDetailSheet(
    summary: ContractorSummary,
    purchases: List<PurchaseEntity>
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header
        item {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                Text(
                    text = summary.contractor.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (summary.contractor.contractType == "FIXED") "Fixed Contract" else "Open Ended",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Total Paid: ${formatCurrency(summary.totalPaid)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (summary.contractor.contractType == "FIXED" && summary.contractor.contractAmount > 0) {
                    val remaining = summary.contractor.contractAmount - summary.totalPaid
                    Text(
                        text = "Remaining: ${formatCurrency(remaining.coerceAtLeast(0.0))}  of  ${formatCurrency(summary.contractor.contractAmount)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            HorizontalDivider()
        }

        // Payments section
        item {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Payments  (${summary.payments.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (summary.payments.isEmpty()) {
            item {
                Text(
                    text = "No payments recorded.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        } else {
            items(summary.payments.size) { i ->
                PaymentDetailRow(payment = summary.payments[i], dateFormat = dateFormat)
            }
        }

        // Purchases section
        item {
            Spacer(Modifier.height(4.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Purchases  (${purchases.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        if (purchases.isEmpty()) {
            item {
                Text(
                    text = "No purchases recorded.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        } else {
            items(purchases.size) { i ->
                PurchaseDetailRow(purchase = purchases[i], dateFormat = dateFormat)
            }
        }
    }
}

@Composable
private fun PaymentDetailRow(payment: PaymentEntity, dateFormat: SimpleDateFormat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Filled.Receipt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 2.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = dateFormat.format(Date(payment.date)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = formatCurrency(payment.amount),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (payment.workDescription.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(text = payment.workDescription, style = MaterialTheme.typography.bodySmall)
                }
                if (payment.bankReference.isNotBlank()) {
                    Text(
                        text = "Ref: ${payment.bankReference}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                Text(
                    text = if (payment.receiptReceived) "Receipt: Yes" else "Receipt: No",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (payment.receiptReceived) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
private fun PurchaseDetailRow(purchase: PurchaseEntity, dateFormat: SimpleDateFormat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Filled.ReceiptLong,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 2.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = purchase.itemName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatCurrency(purchase.amount),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Text(
                    text = dateFormat.format(Date(purchase.date)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = purchase.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                if (purchase.shopName.isNotBlank()) {
                    Text(text = purchase.shopName, style = MaterialTheme.typography.bodySmall)
                }
                if (purchase.notes.isNotBlank()) {
                    Text(
                        text = purchase.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                Text(
                    text = if (purchase.receiptReceived) "Receipt: Yes" else "Receipt: No",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (purchase.receiptReceived) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
private fun GrandTotalCard(state: OverviewUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Project Total",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = formatCurrency(state.grandTotal),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(
                        text = "Contractors",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                    Text(
                        text = formatCurrency(state.totalPayments),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Materials",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                    Text(
                        text = formatCurrency(state.totalPurchases),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun ContractorCard(summary: ContractorSummary, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = summary.contractor.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (summary.contractor.contractType == "FIXED") "Fixed Contract" else "Open Ended",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Text(
                    text = formatCurrency(summary.totalPaid),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            if (summary.contractor.contractType == "FIXED" && summary.contractor.contractAmount > 0) {
                val progress = (summary.totalPaid / summary.contractor.contractAmount)
                    .coerceIn(0.0, 1.0).toFloat()
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = "Paid ${String.format("%.1f", progress * 100)}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "of ${formatCurrency(summary.contractor.contractAmount)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = color,
                    trackColor = color.copy(alpha = 0.2f)
                )
            }
        }
    }
}

@Composable
private fun MaterialsTotalCard(totalPurchases: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Materials & Purchases",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "All categories",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                )
            }
            Text(
                text = formatCurrency(totalPurchases),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}
