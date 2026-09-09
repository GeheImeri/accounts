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
 * v4：budgets 升级为多行（含 name/自动主键/排序），支持多预算卡片。
 * v5：categories 新增 icon 列（首页瓦片 emoji 图标），并按默认分类名回填图标。
 * v6：accounts 新增 icon 列，支持账户图标自定义。
 * v7：budgets 新增自定义时间区间字段。
 */
@Database(
    entities = [Category::class, Account::class, Transaction::class, Transfer::class,
        Template::class, Budget::class],
    version = 7,
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .addCallback(SeedCallback)
                    .build()
                    .also { INSTANCE = it }
            }

        private fun argb(hex: String): Long = ("FF" + hex.removePrefix("#")).toLong(16)

        /** 默认分类图标：name -> emoji（首页瓦片用；空串表示不显示） */
        private val defaultIcons = mapOf(
            "餐饮" to "🍜", "交通" to "🚇", "购物" to "🛍", "居住" to "🏠",
            "日用" to "🧺", "娱乐" to "🎮", "医疗" to "💊", "人情" to "🎁",
            "旅行" to "🧳", "学习" to "📚",
            "工资" to "💰", "奖金" to "🏆", "理财" to "📈", "退款" to "💸",
            "红包" to "🧧", "其他" to "📦"
        )

        private val defaultAccountIcons = mapOf(
            "现金" to "💵", "储蓄卡" to "💳", "支付宝" to "🟦", "微信零钱" to "🟢"
        )

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
                expense.forEachIndexed { _, (name, hex, pinned) ->
                    order++
                    db.execSQL(
                        "INSERT INTO categories(name,color,kind,sortOrder,enabled,pinned,icon) VALUES(?,?,?,?,1,?,?)",
                        arrayOf(name, argb(hex), "expense", order, if (pinned) 1 else 0, defaultIcons[name] ?: "")
                    )
                }
                order = 0
                income.forEachIndexed { _, (name, hex, pinned) ->
                    order++
                    db.execSQL(
                        "INSERT INTO categories(name,color,kind,sortOrder,enabled,pinned,icon) VALUES(?,?,?,?,1,?,?)",
                        arrayOf(name, argb(hex), "income", order, if (pinned) 1 else 0, defaultIcons[name] ?: "")
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
                        "INSERT INTO accounts(name,kind,color,initialBalanceCents,sortOrder,enabled,icon) VALUES(?,?,?,0,?,1,?)",
                        arrayOf(name, kind, argb(hex), i, defaultAccountIcons[name] ?: "")
                    )
                }
                seedTemplates(db)
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

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE budgets_new (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "name TEXT NOT NULL DEFAULT '预算', " +
                        "amountCents INTEGER NOT NULL DEFAULT 0, " +
                        "period TEXT NOT NULL DEFAULT 'month', " +
                        "sortOrder INTEGER NOT NULL DEFAULT 0)"
                )
                db.execSQL(
                    "INSERT INTO budgets_new(name,amountCents,period,sortOrder) " +
                        "SELECT '本月预算',amountCents,period,1 FROM budgets"
                )
                db.execSQL("DROP TABLE budgets")
                db.execSQL("ALTER TABLE budgets_new RENAME TO budgets")
            }
        }

        /** v5：categories 加 icon 列；按默认分类名回填图标（不影响用户已改的自定义分类） */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE categories ADD COLUMN icon TEXT NOT NULL DEFAULT ''")
                defaultIcons.forEach { (name, emoji) ->
                    db.execSQL(
                        "UPDATE categories SET icon = ? WHERE name = ? AND icon = ''",
                        arrayOf(emoji, name)
                    )
                }
            }
        }

        /** v6：账户增加可自定义图标，并为内置账户回填默认图标。 */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE accounts ADD COLUMN icon TEXT NOT NULL DEFAULT ''")
                defaultAccountIcons.forEach { (name, emoji) ->
                    db.execSQL(
                        "UPDATE accounts SET icon = ? WHERE name = ? AND icon = ''",
                        arrayOf(emoji, name)
                    )
                }
            }
        }

        /** v7：预算可选择任意开始日和结束日；旧预算继续按月/年统计。 */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE budgets ADD COLUMN startAtMillis INTEGER")
                db.execSQL("ALTER TABLE budgets ADD COLUMN endAtMillis INTEGER")
            }
        }
    }
}
