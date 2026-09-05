package com.accounts.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 本地数据库（唯一数据源，App 无网络权限）。
 * 预置数据：支出分类 10（前 8 个 pinned = 首页固定 8 格）、收入分类 6、默认账户 4。
 */
@Database(
    entities = [Category::class, Account::class, Transaction::class, Transfer::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun transferDao(): TransferDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "jianji.db"
                )
                    .addCallback(SeedCallback)
                    .build()
                    .also { INSTANCE = it }
            }

        private fun argb(hex: String): Long = ("FF" + hex.removePrefix("#")).toLong(16)

        private val SeedCallback = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                val expense = listOf(
                    Triple("餐饮", "#FF9F68", true), Triple("交通", "#63A9FF", true),
                    Triple("购物", "#F07BAF", true), Triple("居住", "#8B7CF6", true),
                    Triple("日用", "#6FCF97", true), Triple("娱乐", "#FFB15F", true),
                    Triple("医疗", "#62C6C0", true), Triple("人情", "#FF8FA3", true),
                    Triple("旅行", "#FFC857", false), Triple("学习", "#9FB4FF", false)
                )
                val income = listOf(
                    Triple("工资", "#2FC98A", true), Triple("奖金", "#FFB15F", true),
                    Triple("理财", "#63A9FF", true), Triple("退款", "#62C6C0", true),
                    Triple("红包", "#FF8FA3", true), Triple("其他", "#9A9DB3", true)
                )
                var order = 0
                expense.forEachIndexed { i, (name, hex, pinned) ->
                    order++
                    db.execSQL(
                        "INSERT INTO categories(name,color,kind,sortOrder,enabled,pinned) VALUES(?,?,?,?,1,?)",
                        arrayOf(name, argb(hex), "expense", order, if (pinned) 1 else 0)
                    )
                }
                order = 0
                income.forEachIndexed { i, (name, hex, pinned) ->
                    order++
                    db.execSQL(
                        "INSERT INTO categories(name,color,kind,sortOrder,enabled,pinned) VALUES(?,?,?,?,1,?)",
                        arrayOf(name, argb(hex), "income", order, if (pinned) 1 else 0)
                    )
                }
                val accounts = listOf(
                    Triple("现金", "cash", "#FFB15F"),
                    Triple("储蓄卡", "card", "#8B7CF6"),
                    Triple("支付宝", "ewallet", "#63A9FF"),
                    Triple("微信零钱", "ewallet", "#2FC98A")
                )
                accounts.forEachIndexed { i, (name, kind, hex) ->
                    db.execSQL(
                        "INSERT INTO accounts(name,kind,color,initialBalanceCents,sortOrder,enabled) VALUES(?,?,?,0,?,1)",
                        arrayOf(name, kind, argb(hex), i)
                    )
                }
            }
        }
    }
}
