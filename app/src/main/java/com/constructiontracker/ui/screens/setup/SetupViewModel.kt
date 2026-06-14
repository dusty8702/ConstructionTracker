package com.constructiontracker.ui.screens.setup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.constructiontracker.ConstructionTrackerApplication
import com.constructiontracker.data.database.entities.ContractorEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ContractorEditState(
    val id: Int,
    val name: String,
    val contractType: String,
    val contractAmount: String,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false
)

data class AddContractorState(
    val name: String = "",
    val contractType: String = "OPEN_ENDED",
    val contractAmount: String = "",
    val contactNumber: String = "",
    val contactNumberError: String? = null,
    val bankAccountNumber: String = "",
    val bankName: String = "",
    val bankBranch: String = "",
    val photoUri: String = "",
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false
)

data class SetupUiState(
    val contractors: List<ContractorEditState> = emptyList(),
    val isLoading: Boolean = true,
    val addContractor: AddContractorState = AddContractorState()
)

class SetupViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as ConstructionTrackerApplication).repository

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllContractors().collect { contractors ->
                _uiState.update { state ->
                    val currentEdits = state.contractors.associateBy { it.id }
                    SetupUiState(
                        contractors = contractors.map { entity ->
                            currentEdits[entity.id] ?: ContractorEditState(
                                id = entity.id,
                                name = entity.name,
                                contractType = entity.contractType,
                                contractAmount = if (entity.contractAmount > 0)
                                    entity.contractAmount.toBigDecimal().stripTrailingZeros().toPlainString()
                                else ""
                            )
                        },
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun updateContractor(id: Int, transform: (ContractorEditState) -> ContractorEditState) {
        _uiState.update { state ->
            state.copy(contractors = state.contractors.map { if (it.id == id) transform(it) else it })
        }
    }

    fun updateName(id: Int, name: String) = updateContractor(id) { it.copy(name = name) }
    fun updateContractType(id: Int, type: String) = updateContractor(id) { it.copy(contractType = type) }
    fun updateContractAmount(id: Int, amount: String) = updateContractor(id) { it.copy(contractAmount = amount) }

    fun saveContractor(id: Int) {
        val edit = _uiState.value.contractors.find { it.id == id } ?: return
        if (edit.name.isBlank()) return
        updateContractor(id) { it.copy(isSaving = true) }
        viewModelScope.launch {
            repository.updateContractor(
                ContractorEntity(
                    id = edit.id,
                    name = edit.name.trim(),
                    contractType = edit.contractType,
                    contractAmount = edit.contractAmount.toDoubleOrNull() ?: 0.0
                )
            )
            updateContractor(id) { it.copy(isSaving = false, saveSuccess = true) }
        }
    }

    fun clearSaveSuccess(id: Int) = updateContractor(id) { it.copy(saveSuccess = false) }

    private fun updateAddState(transform: (AddContractorState) -> AddContractorState) {
        _uiState.update { it.copy(addContractor = transform(it.addContractor)) }
    }

    fun updateNewName(name: String) = updateAddState { it.copy(name = name) }
    fun updateNewContractType(type: String) = updateAddState { it.copy(contractType = type) }
    fun updateNewContractAmount(amount: String) = updateAddState { it.copy(contractAmount = amount) }
    fun updateNewContactNumber(number: String) = updateAddState { it.copy(contactNumber = number, contactNumberError = null) }
    fun updateNewBankAccountNumber(number: String) = updateAddState { it.copy(bankAccountNumber = number) }
    fun updateNewBankName(name: String) = updateAddState { it.copy(bankName = name) }
    fun updateNewBankBranch(branch: String) = updateAddState { it.copy(bankBranch = branch) }
    fun updateNewPhotoUri(uri: String) = updateAddState { it.copy(photoUri = uri) }

    private fun isValidPhoneNumber(phone: String): Boolean {
        val digits = phone.filter { it.isDigit() }
        return digits.length >= 7 && phone.all { it.isDigit() || it in "+- ()" }
    }

    fun addContractor() {
        val state = _uiState.value.addContractor
        if (state.name.isBlank() || state.isSaving) return
        if (state.contactNumber.isNotBlank() && !isValidPhoneNumber(state.contactNumber)) {
            updateAddState { it.copy(contactNumberError = "Enter a valid phone number (min 7 digits)") }
            return
        }
        updateAddState { it.copy(isSaving = true) }
        viewModelScope.launch {
            repository.addContractor(
                name = state.name.trim(),
                contractType = state.contractType,
                contractAmount = state.contractAmount.toDoubleOrNull() ?: 0.0,
                contactNumber = state.contactNumber.trim(),
                bankAccountNumber = state.bankAccountNumber.trim(),
                bankName = state.bankName.trim(),
                bankBranch = state.bankBranch.trim(),
                photoUri = state.photoUri
            )
            updateAddState { AddContractorState(saveSuccess = true) }
        }
    }

    fun clearAddSuccess() = updateAddState { it.copy(saveSuccess = false) }
}
