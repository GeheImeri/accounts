package com.accounts.app.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

/** 数据访问门面：UI 只依赖本类，不直接触碰 DAO */
class Repository(private val db: AppDatabase) {

    val categories: Flow<List<Category>> = db.categoryDao().observeAll()
    val accounts: Flow<List<Account>> = db.accountDao().observeAll()
    val transactions: Flow<List<Transaction>> = db.transactionDao().observeAll()
    val transfers: Flow<List<Transfer>> = db.transferDao().observeAll()
    val templates: Flow<List<Template>> = db.templateDao().observeAll()

    suspend fun addTransaction(type: String, amountCents: Long, categoryId: Long,
                               accountId: Long, occurredAtMillis: Long, note: String): Long {
        return db.transactionDao().insert(
            Transaction(
                type = type,
                amountCents = amountCents,
                categoryId = categoryId,
                accountId = accountId,
                occurredAtMillis = occurredAtMillis,
                note = note.trim()
            )
        )
    }

    suspend fun deleteTransaction(transaction: Transaction) =
        db.transactionDao().delete(transaction)

    suspend fun updateTransaction(transaction: Transaction) =
        db.transactionDao().update(transaction)

    suspend fun addCategory(name: String, color: Long, kind: String, pinned: Boolean): Long {
        // 排序 = 当前 kind 最大排序 + 1；新分类默认不进首页（pinned 由分类管理切换）
        val order = db.categoryDao().maxSortOrder(kind) + 1
        return db.categoryDao().insert(
            Category(name = name, color = color, kind = kind, sortOrder = order, enabled = true, pinned = pinned)
        )
    }

    suspend fun setCategoryPinned(id: Long, pinned: Boolean) {
        val c = db.categoryDao().findByIdOnce(id) ?: return
        db.categoryDao().update(c.copy(pinned = pinned))
    }

    suspend fun addAccount(name: String, color: Long, kind: String = "other"): Long =
        db.accountDao().insert(Account(name = name, color = color, kind = kind))

    suspend fun addTransfer(fromId: Long, toId: Long, amountCents: Long, note: String) {
        db.transferDao().insert(
            Transfer(fromAccountId = fromId, toAccountId = toId,
                     amountCents = amountCents, occurredAtMillis = System.currentTimeMillis(), note = note)
        )
    }

    // ===== 模板 =====
    suspend fun addTemplate(name: String, amountCents: Long, categoryId: Long, kind: String) {
        db.templateDao().insert(
            Template(name = name, amountCents = amountCents, categoryId = categoryId,
                     kind = kind, sortOrder = (db.templateDao().countOnce() + 1))
        )
    }

    suspend fun deleteTemplate(template: Template) = db.templateDao().delete(template)

    // ===== 分类排序（长按拖动排序的简化实现：上移/下移） =====
    suspend fun reorderCategories(kind: String, ids: List<Long>) {
        val current = db.categoryDao().listByKindOnce(kind).associateBy { it.id }
        ids.forEachIndexed { index, id ->
            val cat = current[id] ?: return@forEachIndexed
            if (cat.sortOrder != index + 1) {
                db.categoryDao().update(cat.copy(sortOrder = index + 1))
            }
        }
    }

    companion object {
        fun build(context: Context): Repository = Repository(AppDatabase.get(context))
    }
}
