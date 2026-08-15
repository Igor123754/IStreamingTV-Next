package com.igor.istreamingtv.data.livetv

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * XMLTV EPG parser — STREAMING (XmlPullParser), bez držanja celog XML-a u RAM-u.
 * ✅ Auto-detekt encodinga, prozor 24h, max 40 programa po kanalu → bezbedno za 2GB.
 */
object EpgParser {

    private val F1 = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US)
    private val F2 = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)

    private const val MAX_PER_CHANNEL = 40

    fun parse(input: InputStream): Map<String, List<EpgProgram>> {
        val out = mutableMapOf<String, MutableList<EpgProgram>>()
        val now = System.currentTimeMillis()
        val min = now - 3 * 3_600_000L
        val max = now + 24 * 3_600_000L

        try {
            val p = Xml.newPullParser()
            p.setInput(input, null) // ✅ auto-detekt encodinga (UTF-8, ISO-8859-1...)

            var channel: String? = null
            var start = 0L; var stop = 0L
            var valid = false
            var title = ""; var desc: String? = null
            var icon: String? = null; var category: String? = null
            var text: StringBuilder? = null

            var ev = p.eventType
            while (ev != XmlPullParser.END_DOCUMENT) {
                when (ev) {
                    XmlPullParser.START_TAG -> when (p.name) {
                        "programme" -> {
                            channel = p.getAttributeValue(null, "channel")
                            start = parseTime(p.getAttributeValue(null, "start")) ?: 0L
                            stop = parseTime(p.getAttributeValue(null, "stop")) ?: 0L
                            valid = start in 1..max && stop > min && channel != null
                            title = ""; desc = null; icon = null; category = null
                        }
                        "title", "desc", "description", "category" -> text = StringBuilder()
                        "icon" -> if (valid && icon == null) {
                            icon = p.getAttributeValue(null, "src")
                        }
                    }
                    XmlPullParser.TEXT -> text?.append(p.text)
                    XmlPullParser.END_TAG -> when (p.name) {
                        "title" -> { title = text.toString().trim(); text = null }
                        "desc", "description" -> { desc = text.toString().trim(); text = null }
                        "category" -> { category = text.toString().trim(); text = null }
                        "programme" -> {
                            if (valid && title.isNotBlank()) {
                                val list = out.getOrPut(channel!!) { mutableListOf() }
                                if (list.size < MAX_PER_CHANNEL) {
                                    list.add(EpgProgram(channel!!, title, desc, icon, start, stop, category))
                                }
                            }
                            valid = false
                        }
                    }
                }
                ev = p.next()
            }
        } catch (_: Exception) {
        }

        // Sortiraj po vremenu (za tačan "sada" program)
        out.values.forEach { it.sortBy { p -> p.startMs } }
        return out
    }

    private fun parseTime(s: String?): Long? {
        if (s.isNullOrBlank()) return null
        val t = s.trim()
        return try {
            F1.parse(t)?.time
        } catch (_: Exception) {
            try { F2.parse(t)?.time } catch (_: Exception) { null }
        }
    }
}
