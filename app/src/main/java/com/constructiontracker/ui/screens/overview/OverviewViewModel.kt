package com.constructiontracker.ui.screens.overview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.constructiontracker.ConstructionTrackerApplication
import com.constructiontracker.data.database.entities.ContractorEntity
import com.constructiontracker.data.database.entities.PaymentEntity
import com.constructiontracker.data.database.entities.PurchaseEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ContractorSummary(
    val contractor: ContractorEntity,
    val totalPaid: Double,
    val payments: List<PaymentEntity>
)

data class OverviewUiState(
    val contractorSummaries: List<ContractorSummary> = emptyList(),
    val totalPayments: Double = 0.0,
    val totalPurchases: Double = 0.0,
    val purchases: List<PurchaseEntity> = emptyList(),
    val isLoading: Boolean = true
) {
    val grandTotal: Double get() = totalPayments + totalPurchases
}

class OverviewViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as ConstructionTrackerApplication).repository

    val uiState: StateFlow<OverviewUiState> = combine(
        repository.getAllContractors(),
        repository.getAllPayments(),
        repository.getAllPurchases()
    ) { contractors, payments, purchases ->
        val summaries = contractors.map { contractor ->
            val contractorPayments = payments
                .filter { it.contractorId == contractor.id }
                .sortedByDescending { it.date }
            ContractorSummary(
                contractor = contractor,
                totalPaid = contractorPayments.sumOf { it.amount },
                payments = contractorPayments
            )
        }
        OverviewUiState(
            contractorSummaries = summaries,
            totalPayments = payments.sumOf { it.amount },
            totalPurchases = purchases.sumOf { it.amount },
            purchases = purchases.sortedByDescending { it.date },
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = OverviewUiState()
    )
}
