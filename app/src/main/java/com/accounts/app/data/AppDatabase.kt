package com.accounts.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 本地数据库（唯一数据源，App 无网络权限）。
 * 预置数据：支出分类 10（前 8 个 pinned = 首页固定 8 格）、收入分类 6、默认账户 4、示例模板 2。
 * v2：新增 templates 表（快捷模板）。
 * v3：新增 budgets 表（单行预算配置，默认未启用）。
 */
@Database(
    entities = [Category::class, Account::class, Transaction::class, Transfer::class,
        Template::class, Budget::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun transferDao(): TransferDao
    abstract fun templateDao(): TemplateDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "jianji.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
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
                seedTemplates(db)
                db.execSQL(
                    "INSERT OR IGNORE INTO budgets(id,amountCents,period) VALUES(1,0,'month')"
                )
            }
        }

        private fun seedTemplates(db: SupportSQLiteDatabase) {
            db.execSQL(
                "INSERT INTO templates(name,amountCents,categoryId,kind,sortOrder) " +
                    "SELECT '早餐',800,id,'expense',1 FROM categories WHERE name='餐饮' LIMIT 1"
            )
            db.execSQL(
                "INSERT INTO templates(name,amountCents,categoryId,kind,sortOrder) " +
                    "SELECT '地铁',600,id,'expense',2 FROM categories WHERE name='交通' LIMIT 1"
            )
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS templates (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "name TEXT NOT NULL, amountCents INTEGER NOT NULL, " +
                        "categoryId INTEGER NOT NULL, kind TEXT NOT NULL, sortOrder INTEGER NOT NULL DEFAULT 0)"
                )
                seedTemplates(db)
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS budgets (" +
                        "id INTEGER NOT NULL PRIMARY KEY, " +
                        "amountCents INTEGER NOT NULL DEFAULT 0, " +
                        "period TEXT NOT NULL DEFAULT 'month')"
                )
                db.execSQL(
                    "INSERT OR IGNORE INTO budgets(id,amountCents,period) VALUES(1,0,'month')"
                )
            }
        }
    }
}
