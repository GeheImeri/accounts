package com.accounts.app.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.accounts.app.data.Account
import com.accounts.app.data.Budget
import com.accounts.app.data.Category
import com.accounts.app.data.Template
import com.accounts.app.data.Transaction
import com.accounts.app.data.Transfer
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** 数据导出 / 备份 / 恢复（全本地文件，通过系统分享面板发送） */
object DataIO {

    data class Snapshot(
        val categories: List<Category> = emptyList(),
        val accounts: List<Account> = emptyList(),
        val transactions: List<Transaction> = emptyList(),
        val transfers: List<Transfer> = emptyList(),
        val templates: List<Template> = emptyList(),
        val budgets: List<Budget> = emptyList()
    )

    // ===== 导出 =====

    fun buildCsv(
        txns: List<Transaction>,
        categories: List<Category>,
        accounts: List<Account>
    ): String {
        val catName = categories.associate { it.id to it.name }
        val accName = accounts.associate { it.id to it.name }
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val sb = StringBuilder()
        sb.append("时间,类型,分类,账户,金额(元),备注\n")
        txns.sortedByDescending { it.occurredAtMillis }.forEach { t ->
            val time = Instant.ofEpochMilli(t.occurredAtMillis)
                .atZone(ZoneId.systemDefault()).format(fmt)
            sb.append(csv(time)).append(',')
                .append(csv(if (t.type == "income") "收入" else "支出")).append(',')
                .append(csv(catName[t.categoryId] ?: "")).append(',')
                .append(csv(accName[t.accountId] ?: "")).append(',')
                .append(csv(String.format(java.util.Locale.US, "%.2f", t.amountCents / 100.0))).append(',')
                .append(csv(t.note)).append('\n')
        }
        return sb.toString()
    }

    private fun csv(s: String): String =
        "\"" + s.replace("\"", "\"\"") + "\""

    fun buildBackupJson(s: Snapshot): String {
        val root = JSONObject()
        root.put("app", "accounts")
        root.put("version", 1)
        root.put("categories", JSONArray().also { a ->
            s.categories.forEach { c ->
                a.put(JSONObject()
                    .put("id", c.id).put("name", c.name).put("color", c.color)
                    .put("kind", c.kind).put("sortOrder", c.sortOrder)
                    .put("enabled", c.enabled).put("pinned", c.pinned)
                    .put("icon", c.icon))
            }
        })
        root.put("accounts", JSONArray().also { a ->
            s.accounts.forEach { acc ->
                a.put(JSONObject()
                    .put("id", acc.id).put("name", acc.name).put("kind", acc.kind)
                    .put("color", acc.color).put("initialBalanceCents", acc.initialBalanceCents)
                    .put("sortOrder", acc.sortOrder).put("enabled", acc.enabled)
                    .put("icon", acc.icon))
            }
        })
        root.put("transactions", JSONArray().also { a ->
            s.transactions.forEach { t ->
                a.put(JSONObject()
                    .put("id", t.id).put("type", t.type).put("amountCents", t.amountCents)
                    .put("categoryId", t.categoryId).put("accountId", t.accountId)
                    .put("occurredAtMillis", t.occurredAtMillis).put("note", t.note)
                    .put("createdAtMillis", t.createdAtMillis))
            }
        })
        root.put("transfers", JSONArray().also { a ->
            s.transfers.forEach { tr ->
                a.put(JSONObject()
                    .put("id", tr.id).put("fromAccountId", tr.fromAccountId)
                    .put("toAccountId", tr.toAccountId).put("amountCents", tr.amountCents)
                    .put("occurredAtMillis", tr.occurredAtMillis).put("note", tr.note))
            }
        })
        root.put("templates", JSONArray().also { a ->
            s.templates.forEach { tp ->
                a.put(JSONObject()
                    .put("id", tp.id).put("name", tp.name).put("amountCents", tp.amountCents)
                    .put("categoryId", tp.categoryId).put("kind", tp.kind)
                    .put("sortOrder", tp.sortOrder))
            }
        })
        root.put("budgets", JSONArray().also { a ->
            s.budgets.forEach { b ->
                a.put(JSONObject()
                    .put("id", b.id).put("name", b.name).put("amountCents", b.amountCents)
                    .put("period", b.period).put("sortOrder", b.sortOrder)
                    .put("startAtMillis", b.startAtMillis)
                    .put("endAtMillis", b.endAtMillis))
            }
        })
        return root.toString(2)
    }

    // ===== 分享文件（写缓存目录 + 系统分享面板）=====

    fun share(context: Context, fileName: String, mime: String, content: String): Boolean {
        return try {
            val dir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(dir, fileName)
            // CSV 带 BOM，Excel/WPS 中文不乱码
            val prefix = if (mime.startsWith("text/csv")) "\uFEFF" else ""
            file.writeText(prefix + content, Charsets.UTF_8)
            val uri = FileProvider.getUriForFile(
                context, context.packageName + ".fileprovider", file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "导出到…")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            false
        }
    }

    // ===== 恢复 =====

    fun parseBackup(content: String): Snapshot? {
        return try {
            val root = JSONObject(content)
            fun listLong(name: String, read: (JSONObject) -> Unit) {
                val arr = root.optJSONArray(name) ?: return
                for (i in 0 until arr.length()) read(arr.getJSONObject(i))
            }

            val cats = mutableListOf<Category>()
            listLong("categories") { o ->
                cats.add(Category(o.getLong("id"), o.getString("name"), o.getLong("color"),
                    o.getString("kind"), o.getInt("sortOrder"), o.getBoolean("enabled"),
                    o.getBoolean("pinned"), o.optString("icon")))
            }
            val accs = mutableListOf<Account>()
            listLong("accounts") { o ->
                accs.add(Account(o.getLong("id"), o.getString("name"), o.getString("kind"),
                    o.getLong("color"), o.getLong("initialBalanceCents"), o.getInt("sortOrder"),
                    o.getBoolean("enabled"), o.optString("icon")))
            }
            val txns = mutableListOf<Transaction>()
            listLong("transactions") { o ->
                txns.add(Transaction(o.getLong("id"), o.getString("type"),
                    o.getLong("amountCents"), o.getLong("categoryId"), o.getLong("accountId"),
                    o.getLong("occurredAtMillis"), o.optString("note"),
                    o.optLong("createdAtMillis", 0L)))
            }
            val trs = mutableListOf<Transfer>()
            listLong("transfers") { o ->
                trs.add(Transfer(o.getLong("id"), o.getLong("fromAccountId"),
                    o.getLong("toAccountId"), o.getLong("amountCents"),
                    o.getLong("occurredAtMillis"), o.optString("note")))
            }
            val tpls = mutableListOf<Template>()
            listLong("templates") { o ->
                tpls.add(Template(o.getLong("id"), o.getString("name"),
                    o.getLong("amountCents"), o.getLong("categoryId"),
                    o.getString("kind"), o.getInt("sortOrder")))
            }
            val budgets = mutableListOf<Budget>()
            listLong("budgets") { o ->
                budgets.add(Budget(
                    id = o.getLong("id"),
                    name = o.optString("name", "预算"),
                    amountCents = o.getLong("amountCents"),
                    period = o.getString("period"),
                    sortOrder = o.optInt("sortOrder", 0),
                    startAtMillis = if (o.isNull("startAtMillis")) null else o.optLong("startAtMillis"),
                    endAtMillis = if (o.isNull("endAtMillis")) null else o.optLong("endAtMillis")
                ))
            }
            Snapshot(cats, accs, txns, trs, tpls, budgets)
        } catch (e: Exception) {
            null
        }
    }
}
