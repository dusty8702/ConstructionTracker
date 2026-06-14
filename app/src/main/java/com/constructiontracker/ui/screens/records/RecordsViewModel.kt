package com.constructiontracker.ui.screens.records

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.constructiontracker.ConstructionTrackerApplication
import com.constructiontracker.data.database.entities.ContractorEntity
import com.constructiontracker.data.database.entities.PaymentEntity
import com.constructiontracker.data.database.entities.PurchaseEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class RecordFilter { ALL, PAYMENTS, PURCHASES }

sealed class RecordItem {
    abstract val date: Long
    abstract val amount: Double

    data class Payment(
        val entity: PaymentEntity,
        val contractorName: String
    ) : RecordItem() {
        override val date get() = entity.date
        override val amount get() = entity.amount
    }

    data class Purchase(val entity: PurchaseEntity) : RecordItem() {
        override val date get() = entity.date
        override val amount get() = entity.amount
    }
}

data class RecordsUiState(
    val records: List<RecordItem> = emptyList(),
    val contractors: List<ContractorEntity> = emptyList(),
    val filter: RecordFilter = RecordFilter.ALL,
    val selectedContractorId: Int? = null,
    val isLoading: Boolean = true
)

class RecordsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as ConstructionTrackerApplication).repository

    private val _filter = MutableStateFlow(RecordFilter.ALL)
    private val _selectedContractorId = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<RecordsUiState> = combine(
        repository.getAllContractors(),
        repository.getAllPayments(),
        repository.getAllPurchases(),
        _filter,
        _selectedContractorId
    ) { contractors, payments, purchases, filter, contractorId ->
        val contractorMap = contractors.associateBy { it.id }
        val paymentRecords = payments.map { payment ->
            RecordItem.Payment(
                entity = payment,
                contractorName = contractorMap[payment.contractorId]?.name ?: "Unknown"
            )
        }
        val purchaseRecords = purchases.map { RecordItem.Purchase(it) }

        val filtered: List<RecordItem> = when (filter) {
            RecordFilter.ALL -> {
                val all = (paymentRecords + purchaseRecords)
                    .sortedByDescending { it.date }
                if (contractorId != null) {
                    all.filter { it !is RecordItem.Payment || it.entity.contractorId == contractorId }
                } else all
            }
            RecordFilter.PAYMENTS -> {
                val filtered = if (contractorId != null) {
                    paymentRecords.filter { it.entity.contractorId == contractorId }
                } else paymentRecords
                filtered.sortedByDescending { it.date }
            }
            RecordFilter.PURCHASES -> purchaseRecords.sortedByDescending { it.date }
        }

        RecordsUiState(
            records = filtered,
            contractors = contractors,
            filter = filter,
            selectedContractorId = contractorId,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RecordsUiState()
    )

    fun setFilter(filter: RecordFilter) {
        _filter.value = filter
        if (filter == RecordFilter.PURCHASES) _selectedContractorId.value = null
    }

    fun setContractorFilter(id: Int?) {
        _selectedContractorId.value = id
    }

    fun deletePayment(item: RecordItem.Payment) {
        viewModelScope.launch { repository.deletePayment(item.entity) }
    }

    fun deletePurchase(item: RecordItem.Purchase) {
        viewModelScope.launch { repository.deletePurchase(item.entity) }
    }
}
