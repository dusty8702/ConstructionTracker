package com.constructiontracker.data.repository

import com.constructiontracker.data.database.AppDatabase
import com.constructiontracker.data.database.entities.ContractorEntity
import com.constructiontracker.data.database.entities.PaymentEntity
import com.constructiontracker.data.database.entities.PurchaseEntity
import kotlinx.coroutines.flow.Flow

class ConstructionRepository(database: AppDatabase) {
    private val contractorDao = database.contractorDao()
    private val paymentDao = database.paymentDao()
    private val purchaseDao = database.purchaseDao()

    fun getAllContractors(): Flow<List<ContractorEntity>> = contractorDao.getAllContractors()
    suspend fun addContractor(
        name: String,
        contractType: String,
        contractAmount: Double,
        contactNumber: String = "",
        bankAccountNumber: String = "",
        bankName: String = "",
        bankBranch: String = "",
        photoUri: String = ""
    ) {
        val existing = contractorDao.getAllContractorsOnce()
        val nextId = (existing.maxOfOrNull { it.id } ?: 0) + 1
        contractorDao.insertContractor(
            ContractorEntity(
                id = nextId,
                name = name,
                contractType = contractType,
                contractAmount = contractAmount,
                contactNumber = contactNumber,
                bankAccountNumber = bankAccountNumber,
                bankName = bankName,
                bankBranch = bankBranch,
                photoUri = photoUri
            )
        )
    }

    fun getAllPayments(): Flow<List<PaymentEntity>> = paymentDao.getAllPayments()
    fun getTotalPaid(): Flow<Double> = paymentDao.getTotalPaid()
    suspend fun insertPayment(payment: PaymentEntity) = paymentDao.insertPayment(payment)
    suspend fun deletePayment(payment: PaymentEntity) = paymentDao.deletePayment(payment)

    fun getAllPurchases(): Flow<List<PurchaseEntity>> = purchaseDao.getAllPurchases()
    fun getTotalPurchases(): Flow<Double> = purchaseDao.getTotalPurchases()
    suspend fun insertPurchase(purchase: PurchaseEntity) = purchaseDao.insertPurchase(purchase)
    suspend fun deletePurchase(purchase: PurchaseEntity) = purchaseDao.deletePurchase(purchase)
}
