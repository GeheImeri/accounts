package com.accounts.app.ui

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.accounts.app.data.Account
import com.accounts.app.data.Budget
import com.accounts.app.data.Category
import com.accounts.app.data.Repository
import com.accounts.app.data.Template
import com.accounts.app.data.Transaction
import com.accounts.app.data.Transfer
import com.accounts.app.util.DataIO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 单例视图模型：所有页面共享同一份数据流 */
class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repository.build(app)
    private val prefs = app.getSharedPreferences("accounts_prefs", 0)

    /** 默认账户（设置页可改，支出与收入记账均优先使用，并持久化到本机）。 */
    val defaultAccountId = MutableStateFlow(prefs.getLong("default_account_id", 0L))

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

    val budgets: StateFlow<List<Budget>> =
        repo.budgets.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun updateRecord(transaction: Transaction, amountCents: Long, note: String,
                     categoryId: Long, accountId: Long) {
        viewModelScope.launch {
            repo.updateTransaction(
                transaction.copy(
                    amountCents = amountCents,
                    note = note.trim(),
                    categoryId = categoryId,
                    accountId = accountId
                )
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

    fun addBudget(name: String, amountCents: Long, period: String) {
        viewModelScope.launch { repo.addBudget(name, amountCents, period) }
    }

    fun updateBudget(budget: Budget) {
        viewModelScope.launch { repo.updateBudget(budget) }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch { repo.deleteBudget(budget) }
    }

    // ===== 分类管理 =====
    fun togglePinned(category: Category) {
        viewModelScope.launch { repo.setCategoryPinned(category.id, !category.pinned) }
    }

    fun addCategory(name: String, icon: String, color: Long, kind: String, pinned: Boolean) {
        viewModelScope.launch { repo.addCategory(name, icon, color, kind, pinned) }
    }

    fun updateCategory(category: Category, name: String, icon: String, color: Long) {
        viewModelScope.launch { repo.updateCategory(category, name, icon, color) }
    }

    fun disableCategory(category: Category) {
        viewModelScope.launch { repo.disableCategory(category) }
    }

    fun reorderCategories(kind: String, ids: List<Long>) {
        viewModelScope.launch { repo.reorderCategories(kind, ids) }
    }

    // ===== 账户管理 =====
    fun addAccount(name: String, icon: String, color: Long, kind: String) {
        viewModelScope.launch { repo.addAccount(name, icon, color, kind) }
    }

    fun updateAccount(account: Account, name: String, icon: String, color: Long, kind: String) {
        viewModelScope.launch { repo.updateAccount(account, name, icon, color, kind) }
    }

    fun addTransfer(fromId: Long, toId: Long, amountCents: Long, note: String) {
        viewModelScope.launch { repo.addTransfer(fromId, toId, amountCents, note) }
    }

    fun setDefaultAccount(id: Long) {
        defaultAccountId.value = id
        prefs.edit().putLong("default_account_id", id).apply()
    }

    // ===== 数据导出 / 备份 / 恢复 =====
    fun exportCsv() {
        viewModelScope.launch {
            val snap = repo.snapshot()
            val ok = DataIO.share(
                getApplication(), "accounts_export.csv", "text/csv",
                DataIO.buildCsv(snap.transactions, snap.categories, snap.accounts)
            )
            toast(if (ok) "CSV 已生成，选择保存位置" else "导出失败")
        }
    }

    fun exportBackup() {
        viewModelScope.launch {
            val ok = DataIO.share(
                getApplication(), "accounts_backup.json", "application/json",
                DataIO.buildBackupJson(repo.snapshot())
            )
            toast(if (ok) "备份文件已生成，请妥善保存" else "备份失败")
        }
    }

    fun restoreBackup(uri: Uri) {
        viewModelScope.launch {
            val content = try {
                getApplication<Application>().contentResolver
                    .openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            } catch (e: Exception) {
                null
            }
            if (content == null) {
                toast("读取文件失败")
                return@launch
            }
            val snap = DataIO.parseBackup(content)
            if (snap == null) {
                toast("备份文件格式不正确")
                return@launch
            }
            repo.restore(snap)
            toast("恢复成功（已覆盖当前数据）")
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show()
    }
}
