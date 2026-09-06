package com.accounts.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 分类。设计约束（见需求文档 F4）：
 * - kind: expense / income
 * - 不可真删：删除 = 停用（enabled=false），历史流水不悬空
 * - pinned：是否显示在记账首页固定 8 格中（在分类管理里切换）
 */
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Long,        // ARGB
    val kind: String,       // "expense" | "income"
    val sortOrder: Int = 0,
    val enabled: Boolean = true,
    val pinned: Boolean = false
)

/** 账户：余额 = 初始 + 收入 − 支出 + 转入 − 转出（实时推算，见需求文档 F5） */
@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: String,               // cash / card / ewallet / other
    val color: Long,
    val initialBalanceCents: Long = 0,
    val sortOrder: Int = 0,
    val enabled: Boolean = true
)

/**
 * 收支流水。金额恒为正、以「分」存储（见需求文档 5.4）；
 * 退款等反向场景记为 income + 退款分类，不引入负金额。
 */
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(entity = Category::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = Account::class, parentColumns = ["id"], childColumns = ["accountId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index("occurredAtMillis"), Index("categoryId"), Index("accountId"), Index("type")]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,               // "expense" | "income"
    val amountCents: Long,          // > 0
    val categoryId: Long,
    val accountId: Long,
    val occurredAtMillis: Long,
    val note: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)

/** 转账：独立记录，绝不进入收支统计（见需求文档 5.4 / 6） */
@Entity(
    tableName = "transfers",
    indices = [Index("occurredAtMillis")]
)
data class Transfer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fromAccountId: Long,
    val toAccountId: Long,
    val amountCents: Long,
    val occurredAtMillis: Long,
    val note: String = ""
)

/** 快捷模板（我的模板）：点一下即记一笔；支持 ＋新增 与 长按删除 */
@Entity(tableName = "templates")
data class Template(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amountCents: Long,
    val categoryId: Long,
    val kind: String,          // expense / income
    val sortOrder: Int = 0
)

/** 预算（单行配置：固定 id=1）。amountCents=0 表示未启用 */
@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey val id: Long = 1,
    val amountCents: Long = 0,
    val period: String = "month"   // month / year
)
