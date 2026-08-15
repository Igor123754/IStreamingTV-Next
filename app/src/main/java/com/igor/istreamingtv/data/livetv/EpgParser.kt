package com.igor.istreamingtv.data.livetv

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * XMLTV EPG parser — STREAMING (XmlPullParser), bez držanja celog XML-a u RAM-u.
 * Čuva samo programe u opsegu [sada − 6h, sada + 48h] → pogodno za 2GB uređaje.
 */
object EpgParser {

    private val TIME_FORMAT = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US)

    fun parse(input: InputStream): Map<String, List<EpgProgram>> {
        val out = mutableMapOf<String, MutableList<EpgProgram>>()
        val now = System.currentTimeMillis()
        val min = now - 6 * 3_600_000L
        val max = now + 48 * 3_600_000L

        try {
            val p = Xml.newPullParser()
            p.setInput(input, "UTF-8")

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
                                out.getOrPut(channel!!) { mutableListOf() }.add(
                                    EpgProgram(channel!!, title, desc, icon, start, stop, category)
                                )
                            }
                            valid = false
                        }
                    }
                }
                ev = p.next()
            }
        } catch (_: Exception) {
        }
        return out
    }

    private fun parseTime(s: String?): Long? = try {
        s?.let { TIME_FORMAT.parse(it.trim())?.time }
    } catch (_: Exception) { null }
}
