package com.constructiontracker.ui.screens.addpurchase

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.constructiontracker.ConstructionTrackerApplication
import com.constructiontracker.data.database.entities.PurchaseEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

val PURCHASE_CATEGORIES = listOf(
    "Electrical",
    "Plumbing & Bathroom",
    "Tiles & Flooring",
    "Ceiling",
    "Fittings & Fixtures",
    "Paint & Finishing",
    "Other"
)

data class AddPurchaseUiState(
    val itemName: String = "",
    val date: Long = System.currentTimeMillis(),
    val amount: String = "",
    val category: String = PURCHASE_CATEGORIES.first(),
    val shopName: String = "",
    val receiptReceived: Boolean = false,
    val notes: String = "",
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

class AddPurchaseViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as ConstructionTrackerApplication).repository

    private val _uiState = MutableStateFlow(AddPurchaseUiState())
    val uiState: StateFlow<AddPurchaseUiState> = _uiState.asStateFlow()

    fun updateItemName(name: String) = _uiState.update { it.copy(itemName = name, error = null) }
    fun updateDate(date: Long) = _uiState.update { it.copy(date = date) }
    fun updateAmount(amount: String) = _uiState.update { it.copy(amount = amount, error = null) }
    fun updateCategory(category: String) = _uiState.update { it.copy(category = category) }
    fun updateShopName(name: String) = _uiState.update { it.copy(shopName = name) }
    fun updateReceiptReceived(received: Boolean) = _uiState.update { it.copy(receiptReceived = received) }
    fun updateNotes(notes: String) = _uiState.update { it.copy(notes = notes) }
    fun clearSaveSuccess() = _uiState.update { it.copy(saveSuccess = false) }

    fun savePurchase() {
        val state = _uiState.value
        if (state.itemName.isBlank()) {
            _uiState.update { it.copy(error = "Please enter an item name") }
            return
        }
        val amount = state.amount.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _uiState.update { it.copy(error = "Please enter a valid amount") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                repository.insertPurchase(
                    PurchaseEntity(
                        itemName = state.itemName,
                        date = state.date,
                        amount = amount,
                        category = state.category,
                        shopName = state.shopName,
                        receiptReceived = state.receiptReceived,
                        notes = state.notes
                    )
                )
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveSuccess = true,
                        itemName = "",
                        amount = "",
                        category = PURCHASE_CATEGORIES.first(),
                        shopName = "",
                        receiptReceived = false,
                        notes = "",
                        date = System.currentTimeMillis()
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = "Failed to save. Please try again.") }
            }
        }
    }
}
