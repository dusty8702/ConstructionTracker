package com.constructiontracker.data.database.dao

import androidx.room.*
import com.constructiontracker.data.database.entities.ContractorEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContractorDao {
    @Query("SELECT * FROM contractors ORDER BY id ASC")
    fun getAllContractors(): Flow<List<ContractorEntity>>

    @Query("SELECT * FROM contractors ORDER BY id ASC")
    suspend fun getAllContractorsOnce(): List<ContractorEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContractors(contractors: List<ContractorEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContractor(contractor: ContractorEntity)

    @Update
    suspend fun updateContractor(contractor: ContractorEntity)
}
