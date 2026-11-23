package roro.stellar.manager.util.update

import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object UpdateUtils {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private const val GITHUB_API_URL =
        "https://api.github.com/repos/Kimger/Stellar/releases/latest"

    private fun parseVersionCode(tagName: String): Int {
        val match = Regex("\\((\\d+)\\)").find(tagName)
        return match?.groupValues?.get(1)?.toIntOrNull() ?: -1
    }

    private fun parseVersionName(tagName: String): String {
        return tagName.removePrefix("v").replace(Regex("\\(\\d+\\)"), "")
    }

    suspend fun checkUpdate(): AppUpdate? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(GITHUB_API_URL)
            .header("Accept", "application/vnd.github+json")
            .build()
        client.newCall(request).execute().use { response ->
            if (response.code == 404) return@withContext null
            if (!response.isSuccessful) throw IOException("GitHub Releases HTTP ${response.code}")

            val release = JSONObject(response.body?.string().orEmpty())
            val tag = release.getString("tag_name")
            val versionCode = parseVersionCode(tag)
            if (versionCode <= 0) throw IOException("Invalid release tag: $tag")
            AppUpdate(
                parseVersionName(tag), versionCode, release.optString("body"),
                findApkDownloadUrl(release.optJSONArray("assets"))
            )
        }
    }

    private fun findApkDownloadUrl(assets: org.json.JSONArray?): String {
        if (assets == null || assets.length() == 0) return ""
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            if (asset.optString("name", "").endsWith(".apk")) {
                return asset.optString("browser_download_url", "")
            }
        }
        return ""
    }
}
