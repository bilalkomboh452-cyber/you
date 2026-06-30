
package com.waglo.app.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.google.gson.GsonBuilder
import com.waglo.app.model.GroupLink
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportHelper {

    private val TIMESTAMP_FMT = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    private val READABLE_FMT  = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

    private fun exportDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "WAGLO_Exports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun ts() = TIMESTAMP_FMT.format(Date())

    // -----------------------------------------------------------------------
    // TXT
    // -----------------------------------------------------------------------
    fun exportAsTxt(context: Context, groups: List<GroupLink>): File {
        val file = File(exportDir(context), "waglo_export_${ts()}.txt")
        file.bufferedWriter().use { bw ->
            bw.write("WAGLO Export — ${groups.size} groups")
            bw.newLine(); bw.write("Generated: ${READABLE_FMT.format(Date())}")
            bw.newLine(); bw.write("=" * 60); bw.newLine()
            for ((i, g) in groups.withIndex()) {
                bw.write("${i + 1}. ${g.inviteLink}")
                if (g.groupName.isNotBlank()) bw.write(" [${g.groupName}]")
                if (g.category.isNotBlank())  bw.write(" #${g.category}")
                bw.newLine()
            }
        }
        return file
    }

    // -----------------------------------------------------------------------
    // CSV
    // -----------------------------------------------------------------------
    fun exportAsCsv(context: Context, groups: List<GroupLink>): File {
        val file = File(exportDir(context), "waglo_export_${ts()}.csv")
        file.bufferedWriter().use { bw ->
            bw.write("#,invite_link,group_name,category,source,language,confidence,status,is_favorite,added_at")
            bw.newLine()
            for ((i, g) in groups.withIndex()) {
                bw.write(listOf(
                    i + 1,
                    g.inviteLink.csvEsc(),
                    g.groupName.csvEsc(),
                    g.category,
                    g.source,
                    g.language,
                    String.format(Locale.US, "%.2f", g.confidenceScore),
                    g.status,
                    if (g.isFavorite) "1" else "0",
                    READABLE_FMT.format(Date(g.addedAt))
                ).joinToString(","))
                bw.newLine()
            }
        }
        return file
    }

    private fun String.csvEsc(): String {
        return if (contains(',') || contains('"') || contains('
'))
            ""${replace(""", """"")}"" else this
    }

    // -----------------------------------------------------------------------
    // JSON
    // -----------------------------------------------------------------------
    fun exportAsJson(context: Context, groups: List<GroupLink>): File {
        val file = File(exportDir(context), "waglo_export_${ts()}.json")
        val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
        val payload = mapOf(
            "generated"  to READABLE_FMT.format(Date()),
            "count"      to groups.size,
            "groups"     to groups.map { g -> mapOf(
                "invite_link"      to g.inviteLink,
                "group_name"       to g.groupName,
                "category"         to g.category,
                "source"           to g.source,
                "language"         to g.language,
                "confidence_score" to g.confidenceScore,
                "status"           to g.status,
                "is_favorite"      to g.isFavorite,
                "notes"            to g.notes,
                "discovery_time"   to g.discoveryTime,
                "added_at"         to g.addedAt
            )}
        )
        file.writeText(gson.toJson(payload))
        return file
    }

    // -----------------------------------------------------------------------
    // HTML
    // -----------------------------------------------------------------------
    fun exportAsHtml(context: Context, groups: List<GroupLink>): File {
        val file = File(exportDir(context), "waglo_export_${ts()}.html")
        file.bufferedWriter().use { bw ->
            bw.write("""<!DOCTYPE html><html lang="en"><head><meta charset="UTF-8">
<title>WAGLO Export</title>
<style>body{font-family:sans-serif;margin:20px}table{border-collapse:collapse;width:100%}
th,td{border:1px solid #ccc;padding:8px;text-align:left}th{background:#25d366;color:#fff}
tr:nth-child(even){background:#f9f9f9}a{color:#075e54}</style></head><body>
<h2>WAGLO Export — ${groups.size} groups</h2>
<p>Generated: ${READABLE_FMT.format(Date())}</p>
<table><thead><tr><th>#</th><th>Link</th><th>Group</th><th>Category</th>
<th>Source</th><th>Confidence</th></tr></thead><tbody>""")
            for ((i, g) in groups.withIndex()) {
                bw.write("<tr><td>${i + 1}</td>")
                bw.write("<td><a href="${g.inviteLink.he()}" target="_blank">${g.inviteLink.he()}</a></td>")
                bw.write("<td>${g.groupName.he()}</td><td>${g.category}</td>")
                bw.write("<td>${g.source}</td><td>${String.format(Locale.US, "%.0f%%", g.confidenceScore * 100)}</td></tr>")
            }
            bw.write("</tbody></table></body></html>")
        }
        return file
    }

    private fun String.he() = replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")

    // -----------------------------------------------------------------------
    // Clipboard
    // -----------------------------------------------------------------------
    fun copyToClipboard(context: Context, groups: List<GroupLink>): Boolean {
        return try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val text = groups.joinToString("
") { it.inviteLink }
            cm.setPrimaryClip(ClipData.newPlainText("WhatsApp Links", text))
            true
        } catch (e: Exception) {
            false
        }
    }

    // -----------------------------------------------------------------------
    // Share Intent
    // -----------------------------------------------------------------------
    fun getShareIntent(context: Context, file: File, mimeType: String): Intent {
        val uri: Uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    // -----------------------------------------------------------------------
    // Duplicate-free / filtered helpers
    // -----------------------------------------------------------------------
    fun deduplicatedExport(groups: List<GroupLink>): List<GroupLink> {
        val seen = HashSet<String>(groups.size * 2)
        return groups.filter { seen.add(it.inviteLink.trim().lowercase()) }
    }

    fun filteredExport(
        groups: List<GroupLink>,
        category: String? = null,
        language: String? = null,
        onlyFavorites: Boolean = false
    ): List<GroupLink> {
        return groups.filter { g ->
            (category == null || g.category == category) &&
            (language == null || g.language == language) &&
            (!onlyFavorites || g.isFavorite)
        }
    }
}

private operator fun String.times(n: Int) = repeat(n)
