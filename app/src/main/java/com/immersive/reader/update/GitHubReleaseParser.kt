package com.immersive.reader.update

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object GitHubReleaseParser {
    fun parse(json: String): GitHubRelease {
        try {
            val root = JSONObject(json)
            val tagName = root.getString("tag_name")
            val assets = root.optJSONArray("assets") ?: JSONArray()
            val apk = selectApk(assets) ?: throw ReleaseParseException("Release has no APK")
            return GitHubRelease(
                tagName = tagName,
                name = root.optString("name").ifBlank { null },
                body = root.optString("body").ifBlank { null },
                htmlUrl = root.optString("html_url"),
                apkDownloadUrl = apk.getString("browser_download_url"),
                apkName = apk.getString("name"),
            )
        } catch (error: ReleaseParseException) {
            throw error
        } catch (error: JSONException) {
            throw ReleaseParseException("Malformed GitHub release JSON", error)
        }
    }

    private fun selectApk(assets: JSONArray): JSONObject? {
        for (index in 0 until assets.length()) {
            val asset = assets.getJSONObject(index)
            val name = asset.optString("name")
            if (name.endsWith(".apk", ignoreCase = true) && !name.contains("debug", ignoreCase = true)) {
                return asset
            }
        }
        return null
    }
}
