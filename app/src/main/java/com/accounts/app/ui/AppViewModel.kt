package com.accounts.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.accounts.app.data.Account
import com.accounts.app.data.Category
import com.accounts.app.data.Repository
import com.accounts.app.data.Template
import com.accounts.app.data.Transaction
import com.accounts.app.data.Transfer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 单例视图模型：所有页面共享同一份数据流 */
class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repository.build(app)

    /** 默认支出账户（设置页可改，记一笔首页默认使用） */
    val defaultAccountId = MutableStateFlow(0L)

    val categories: StateFlow<List<Category>> =
        repo.categories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<Account>> =
        repo.accounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<Transaction>> =
        repo.transactions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transfers: StateFlow<List<Transfer>> =
        repo.transfers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val templates: StateFlow<List<Template>> =
        repo.templates.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ===== 记一笔 =====
    fun addRecord(type: String, amountCents: Long, categoryId: Long, accountId: Long,
                  occurredAtMillis: Long, note: String) {
        if (amountCents <= 0) return
        viewModelScope.launch {
            repo.addTransaction(type, amountCents, categoryId, accountId, occurredAtMillis, note)
        }
    }

    fun deleteRecord(transaction: Transaction) {
        viewModelScope.launch { repo.deleteTransaction(transaction) }
    }

    fun updateRecord(transaction: Transaction, amountCents: Long, note: String, categoryId: Long) {
        viewModelScope.launch {
            repo.updateTransaction(
                transaction.copy(amountCents = amountCents, note = note.trim(), categoryId = categoryId)
            )
        }
    }

    // ===== 模板 =====
    fun addTemplate(name: String, amountCents: Long, categoryId: Long, kind: String) {
        viewModelScope.launch { repo.addTemplate(name, amountCents, categoryId, kind) }
    }

    fun deleteTemplate(template: Template) {
        viewModelScope.launch { repo.deleteTemplate(template) }
    }

    // ===== 分类管理 =====
    fun togglePinned(category: Category) {
        viewModelScope.launch { repo.setCategoryPinned(category.id, !category.pinned) }
    }

    fun addCategory(name: String, color: Long, kind: String, pinned: Boolean) {
        viewModelScope.launch { repo.addCategory(name, color, kind, pinned) }
    }

    fun reorderCategories(kind: String, ids: List<Long>) {
        viewModelScope.launch { repo.reorderCategories(kind, ids) }
    }

    // ===== 账户管理 =====
    fun addAccount(name: String, color: Long, kind: String) {
        viewModelScope.launch { repo.addAccount(name, color, kind) }
    }

    fun addTransfer(fromId: Long, toId: Long, amountCents: Long, note: String) {
        viewModelScope.launch { repo.addTransfer(fromId, toId, amountCents, note) }
    }

    fun setDefaultAccount(id: Long) {
        defaultAccountId.value = id
    }
}
