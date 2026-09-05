package com.accounts.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE kind = :kind AND enabled = 1 ORDER BY sortOrder")
    fun observeByKind(kind: String): Flow<List<Category>>

    @Query("SELECT * FROM categories ORDER BY kind, sortOrder")
    fun observeAll(): Flow<List<Category>>

    @Insert
    suspend fun insert(category: Category): Long

    @Update
    suspend fun update(category: Category)

    /** 停用（不真删） */
    @Query("UPDATE categories SET enabled = 0 WHERE id = :id")
    suspend fun disable(id: Long)

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM categories WHERE kind = :kind")
    suspend fun maxSortOrder(kind: String): Int

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun findByIdOnce(id: Long): Category?
}

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE enabled = 1 ORDER BY sortOrder")
    fun observeAll(): Flow<List<Account>>

    @Insert
    suspend fun insert(account: Account): Long

    @Update
    suspend fun update(account: Account)

    @Query("UPDATE accounts SET enabled = 0 WHERE id = :id")
    suspend fun disable(id: Long)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY occurredAtMillis DESC, id DESC")
    fun observeAll(): Flow<List<Transaction>>

    @Insert
    suspend fun insert(transaction: Transaction): Long

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)
}

@Dao
interface TransferDao {
    @Query("SELECT * FROM transfers ORDER BY occurredAtMillis DESC")
    fun observeAll(): Flow<List<Transfer>>

    @Insert
    suspend fun insert(transfer: Transfer): Long
}
