package com.accounts.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
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

    @Query("SELECT * FROM categories WHERE kind = :kind AND enabled = 1 ORDER BY sortOrder")
    suspend fun listByKindOnce(kind: String): List<Category>

    @Query("SELECT * FROM categories")
    suspend fun allOnce(): List<Category>

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE enabled = 1 ORDER BY sortOrder")
    fun observeAll(): Flow<List<Account>>

    @Insert
    suspend fun insert(account: Account): Long

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM accounts")
    suspend fun maxSortOrder(): Int

    @Update
    suspend fun update(account: Account)

    @Query("UPDATE accounts SET enabled = 0 WHERE id = :id")
    suspend fun disable(id: Long)

    @Query("SELECT * FROM accounts")
    suspend fun allOnce(): List<Account>

    @Query("DELETE FROM accounts")
    suspend fun deleteAll()
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

    @Query("SELECT * FROM transactions")
    suspend fun allOnce(): List<Transaction>

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}

@Dao
interface TransferDao {
    @Query("SELECT * FROM transfers ORDER BY occurredAtMillis DESC")
    fun observeAll(): Flow<List<Transfer>>

    @Insert
    suspend fun insert(transfer: Transfer): Long

    @Query("SELECT * FROM transfers")
    suspend fun allOnce(): List<Transfer>

    @Query("DELETE FROM transfers")
    suspend fun deleteAll()
}

@Dao
interface TemplateDao {
    @Query("SELECT * FROM templates ORDER BY sortOrder, id")
    fun observeAll(): Flow<List<Template>>

    @Query("SELECT COUNT(*) FROM templates")
    suspend fun countOnce(): Int

    @Query("SELECT * FROM templates")
    suspend fun allOnce(): List<Template>

    @Query("DELETE FROM templates")
    suspend fun deleteAll()

    @Insert
    suspend fun insert(template: Template): Long

    @Delete
    suspend fun delete(template: Template)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets ORDER BY sortOrder, id")
    fun observeAll(): Flow<List<Budget>>

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM budgets")
    suspend fun maxSortOrder(): Int

    @Query("SELECT * FROM budgets")
    suspend fun allOnce(): List<Budget>

    @Query("DELETE FROM budgets")
    suspend fun deleteAll()

    @Insert
    suspend fun insert(budget: Budget): Long

    @Update
    suspend fun update(budget: Budget)

    @Delete
    suspend fun delete(budget: Budget)
}
