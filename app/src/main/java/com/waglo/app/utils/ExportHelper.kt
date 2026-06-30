package com.waglo.app.utils

import android.content.Context
import com.waglo.app.model.GroupLink
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportHelper {

    private val READABLE_FMT = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
    private val FILE_TS_FMT  = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    private fun exportDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), "WAGLO_Exports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun ts() = FILE_TS_FMT.format(Date())

    // ── CSV ──────────────────────────────────────────────────────────────────
    fun exportAsCsv(context: Context, groups: List<GroupLink>): File {
        val file = File(exportDir(context), "waglo_export_${ts()}.csv")
        file.bufferedWriter().use { bw ->
            bw.write("ID,SerialNumber,GroupName,InviteLink,ChatName,MessageDate," +
                     "Category,Source,Language,ConfidenceScore,Status,IsFavorite,Notes,AddedAt")
            bw.newLine()
            groups.forEach { g ->
                val line = listOf(
                    g.id.toString(),
                    csvEscape(g.serialNumber),
                    csvEscape(g.groupName),
                    csvEscape(g.inviteLink),
                    csvEscape(g.chatName),
                    csvEscape(g.messageDate),
                    csvEscape(g.category),
                    csvEscape(g.source),
                    csvEscape(g.language),
                    g.confidenceScore.toString(),
                    csvEscape(g.status),
                    g.isFavorite.toString(),
                    csvEscape(g.notes ?: ""),
                    csvEscape(g.addedAt)
                ).joinToString(",")
                bw.write(line)
                bw.newLine()
            }
        }
        return file
    }

    // ── JSON ─────────────────────────────────────────────────────────────────
    fun exportAsJson(context: Context, groups: List<GroupLink>): File {
        val file = File(exportDir(context), "waglo_export_${ts()}.json")
        val arr = JSONArray()
        groups.forEach { g ->
            arr.put(JSONObject().apply {
                put("id", g.id)
                put("serialNumber", g.serialNumber)
                put("groupName", g.groupName)
                put("inviteLink", g.inviteLink)
                put("chatName", g.chatName)
                put("messageDate", g.messageDate)
                put("category", g.category)
                put("source", g.source)
                put("language", g.language)
                put("confidenceScore", g.confidenceScore)
                put("status", g.status)
                put("isFavorite", g.isFavorite)
                put("notes", g.notes ?: "")
                put("addedAt", g.addedAt)
            })
        }
        file.writeText(arr.toString(2))
        return file
    }

    // ── TXT ──────────────────────────────────────────────────────────────────
    fun exportAsTxt(context: Context, groups: List<GroupLink>): File {
        val file = File(exportDir(context), "waglo_export_${ts()}.txt")
        val sep60 = "=".repeat(60)
        val div60 = "-".repeat(60)
        file.bufferedWriter().use { bw ->
            bw.write("WAGLO Export — ${groups.size} groups"); bw.newLine()
            bw.write("Generated : ${READABLE_FMT.format(Date())}"); bw.newLine()
            bw.write(sep60); bw.newLine()
            groups.forEach { g ->
                bw.write("[${g.category}] ${g.groupName}"); bw.newLine()
                bw.write("  Link    : ${g.inviteLink}"); bw.newLine()
                bw.write("  Source  : ${g.source}  |  Status: ${g.status}"); bw.newLine()
                bw.write("  Score   : ${g.confidenceScore}  |  Fav: ${g.isFavorite}"); bw.newLine()
                if (!g.notes.isNullOrBlank()) {
                    bw.write("  Notes   : ${g.notes}"); bw.newLine()
                }
                bw.write(div60); bw.newLine()
            }
        }
        return file
    }

    // ── HTML ─────────────────────────────────────────────────────────────────
    fun exportAsHtml(context: Context, groups: List<GroupLink>): File {
        val file = File(exportDir(context), "waglo_export_${ts()}.html")
        file.bufferedWriter().use { bw ->
            bw.write(buildHtmlHeader(groups.size))
            groups.forEachIndexed { idx, g ->
                bw.write(buildHtmlRow(idx + 1, g))
            }
            bw.write("</table></body></html>")
        }
        return file
    }

    private fun buildHtmlHeader(total: Int): String = """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<title>WAGLO Export</title>
<style>
body{font-family:sans-serif;margin:20px;background:#f5f5f5}
h1{color:#25D366}
table{width:100%;border-collapse:collapse;background:#fff}
th{background:#25D366;color:#fff;padding:8px;text-align:left}
td{padding:8px;border-bottom:1px solid #ddd}
tr:hover{background:#f0f0f0}
a{color:#075E54;text-decoration:none}
.badge{padding:2px 8px;border-radius:12px;font-size:12px;background:#e0e0e0}
</style>
</head>
<body>
<h1>WAGLO Export</h1>
<p>Generated: ${READABLE_FMT.format(Date())} | Total: $total groups</p>
<table>
<tr><th>#</th><th>Group Name</th><th>Category</th><th>Invite Link</th><th>Status</th><th>Score</th></tr>
""".trimIndent()

    private fun buildHtmlRow(index: Int, g: GroupLink): String =
        "<tr><td>$index</td>" +
        "<td>${htmlEscape(g.groupName)}</td>" +
        "<td><span class=\"badge\">${g.category}</span></td>" +
        "<td><a href=\"${g.inviteLink}\">${htmlEscape(g.inviteLink)}</a></td>" +
        "<td>${g.status}</td>" +
        "<td>${g.confidenceScore}</td></tr>\n"

    // ── Helpers ───────────────────────────────────────────────────────────────
    private fun csvEscape(value: String): String =
        if (value.contains(',') || value.contains('"') || value.contains('\n'))
            "\"${value.replace("\"", "\"\"")}\""
        else value

    private fun htmlEscape(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
