package com.constructiontracker.ui.screens.addpayment

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.constructiontracker.ConstructionTrackerApplication
import com.constructiontracker.data.database.entities.ContractorEntity
import com.constructiontracker.data.database.entities.PaymentEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddPaymentUiState(
    val contractors: List<ContractorEntity> = emptyList(),
    val selectedContractorId: Int? = null,
    val date: Long = System.currentTimeMillis(),
    val amount: String = "",
    val bankReference: String = "",
    val workDescription: String = "",
    val receiptReceived: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

class AddPaymentViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as ConstructionTrackerApplication).repository

    private val _uiState = MutableStateFlow(AddPaymentUiState())
    val uiState: StateFlow<AddPaymentUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllContractors().collect { contractors ->
                _uiState.update { state ->
                    state.copy(
                        contractors = contractors,
                        selectedContractorId = state.selectedContractorId ?: contractors.firstOrNull()?.id
                    )
                }
            }
        }
    }

    fun updateSelectedContractor(id: Int) = _uiState.update { it.copy(selectedContractorId = id) }
    fun updateDate(date: Long) = _uiState.update { it.copy(date = date) }
    fun updateAmount(amount: String) = _uiState.update { it.copy(amount = amount, error = null) }
    fun updateBankReference(ref: String) = _uiState.update { it.copy(bankReference = ref) }
    fun updateWorkDescription(desc: String) = _uiState.update { it.copy(workDescription = desc) }
    fun updateReceiptReceived(received: Boolean) = _uiState.update { it.copy(receiptReceived = received) }
    fun clearSaveSuccess() = _uiState.update { it.copy(saveSuccess = false) }

    fun savePayment() {
        val state = _uiState.value
        val amount = state.amount.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _uiState.update { it.copy(error = "Please enter a valid amount") }
            return
        }
        if (state.selectedContractorId == null) {
            _uiState.update { it.copy(error = "Please select a contractor") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                repository.insertPayment(
                    PaymentEntity(
                        contractorId = state.selectedContractorId,
                        date = state.date,
                        amount = amount,
                        bankReference = state.bankReference,
                        workDescription = state.workDescription,
                        receiptReceived = state.receiptReceived
                    )
                )
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveSuccess = true,
                        amount = "",
                        bankReference = "",
                        workDescription = "",
                        receiptReceived = false,
                        date = System.currentTimeMillis()
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = "Failed to save. Please try again.") }
            }
        }
    }
}
