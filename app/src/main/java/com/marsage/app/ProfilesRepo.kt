package com.marsage.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.ZonedDateTime

data class Profile(val name: String, val birthISO: String, val comment: String = "")

class ProfilesRepo(context: Context) {
    private val file = File(context.filesDir, "profiles.json")

    private val defaults = JSONArray("""[
        {"name": "Надиров И. Н.", "birth": "1987-01-20T14:10:00+03:00", "comment": "автор"},
        {"name": "Маленький марсионин", "birth": "2018-10-10T00:00:00.001+03:00", "comment": "рождение в 00:00:00,001"}
    ]""")

    fun load(): List<Profile> {
        val text = try {
            if (file.exists()) file.readText() else {
                file.parentFile?.mkdirs()
                file.writeText(defaults.toString())
                defaults.toString()
            }
        } catch (e: Exception) { defaults.toString() }
        return try {
            val arr = JSONArray(text)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                Profile(o.getString("name"), o.getString("birth"), o.optString("comment"))
            }
        } catch (e: Exception) { emptyList() }
    }

    fun save(profiles: List<Profile>) {
        val arr = JSONArray()
        profiles.forEach {
            arr.put(JSONObject().put("name", it.name).put("birth", it.birthISO).put("comment", it.comment))
        }
        file.writeText(arr.toString(2))
    }

    fun parse(iso: String): ZonedDateTime = ZonedDateTime.parse(iso)
}
