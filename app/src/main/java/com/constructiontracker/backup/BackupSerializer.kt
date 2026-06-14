package com.constructiontracker.backup

import com.constructiontracker.data.database.entities.ContractorEntity
import com.constructiontracker.data.database.entities.PaymentEntity
import com.constructiontracker.data.database.entities.PurchaseEntity
import org.json.JSONArray
import org.json.JSONObject

object BackupSerializer {
    fun serialize(
        contractors: List<ContractorEntity>,
        payments: List<PaymentEntity>,
        purchases: List<PurchaseEntity>
    ): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())

        val contractorsArr = JSONArray()
        contractors.forEach { c ->
            contractorsArr.put(JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("contractType", c.contractType)
                put("contractAmount", c.contractAmount)
                put("contactNumber", c.contactNumber)
                put("bankAccountNumber", c.bankAccountNumber)
                put("bankName", c.bankName)
                put("bankBranch", c.bankBranch)
                put("photoUri", c.photoUri)
            })
        }
        root.put("contractors", contractorsArr)

        val paymentsArr = JSONArray()
        payments.forEach { p ->
            paymentsArr.put(JSONObject().apply {
                put("id", p.id)
                put("contractorId", p.contractorId)
                put("date", p.date)
                put("amount", p.amount)
                put("bankReference", p.bankReference)
                put("workDescription", p.workDescription)
                put("receiptReceived", p.receiptReceived)
                put("createdAt", p.createdAt)
            })
        }
        root.put("payments", paymentsArr)

        val purchasesArr = JSONArray()
        purchases.forEach { p ->
            purchasesArr.put(JSONObject().apply {
                put("id", p.id)
                put("itemName", p.itemName)
                put("date", p.date)
                put("amount", p.amount)
                put("category", p.category)
                put("shopName", p.shopName)
                put("receiptReceived", p.receiptReceived)
                put("notes", p.notes)
                put("createdAt", p.createdAt)
            })
        }
        root.put("purchases", purchasesArr)

        return root.toString()
    }
}
