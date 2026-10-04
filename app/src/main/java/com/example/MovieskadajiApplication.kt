package com.example

import android.app.Application
import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import java.io.File

/**
 * MovieskadajiApplication
 *
 * Custom application class to guarantee environment stability,
 * pre-initialize Chromium cache structures across all user/data paths
 * to prevent missing directory errors (simple_file_enumerator opendir & simple_index_file),
 * and coordinate app-wide services.
 */
class MovieskadajiApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        ensureWebViewDirectories(this)
        startCacheDirectoryGuard(this)
    }

    companion object {
        private const val TAG = "MovieskadajiApp"
        private val appScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        /**
         * Ensures a clean directory exists and purges any invalid files (e.g. .keep)
         * that would break Chromium's SimpleIndexFile parser (line 613 GetEntryHashFromFileName).
         * Chromium expects SimpleCache directories to be either clean/empty or contain
         * 16-hex-digit hash files only.
         */
        private fun ensureCleanDirectory(parent: File, relPath: String) {
            try {
                val dir = File(parent, relPath)
                if (dir.exists() && !dir.isDirectory) {
                    dir.delete()
                }
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                dir.setReadable(true, false)
                dir.setWritable(true, false)
                dir.setExecutable(true, false)

                // Remove any non-cache files like .keep that fail SimpleIndexFile parsing
                dir.listFiles()?.forEach { file ->
                    if (file.name.startsWith(".") || file.name == ".keep" || file.name.length < 8) {
                        try { file.delete() } catch (_: Exception) {}
                    }
                }
            } catch (_: Exception) {}
        }

        /**
         * Pre-creates all directory hierarchies expected by Chromium's Simple Cache and Code Cache
         * engine (e.g. Code Cache/js, Code Cache/wasm, HTTP Cache).
         * This prevents ENOENT (2) "No such file or directory" and
         * "Could not reconstruct index from disk" errors during WebView and browser engine startup.
         */
        fun ensureWebViewDirectories(context: Context) {
            try {
                val pkg = context.packageName
                val dataDir = context.applicationInfo.dataDir

                val cacheRoots = setOfNotNull(
                    context.cacheDir,
                    context.codeCacheDir,
                    File("/data/user/0/$pkg/cache"),
                    File("/data/data/$pkg/cache"),
                    if (dataDir != null) File(dataDir, "cache") else null
                )

                val cacheSubpaths = listOf(
                    "WebView",
                    "WebView/Default",
                    "WebView/Default/HTTP Cache",
                    "WebView/Default/HTTP Cache/Code Cache",
                    "WebView/Default/HTTP Cache/Code Cache/wasm",
                    "WebView/Default/HTTP Cache/Code Cache/js",
                    "WebView/Default/Code Cache",
                    "WebView/Default/Code Cache/wasm",
                    "WebView/Default/Code Cache/js",
                    "WebView/Default/GPUCache",
                    "WebView/Default/Service Worker",
                    "WebView/Default/Service Worker/CacheStorage",
                    "WebView/Default/Service Worker/ScriptCache",
                    "WebView/Crashpad"
                )

                for (root in cacheRoots) {
                    for (sub in cacheSubpaths) {
                        ensureCleanDirectory(root, sub)
                    }
                }

                val dataRoots = setOfNotNull(
                    context.dataDir,
                    File("/data/user/0/$pkg"),
                    File("/data/data/$pkg"),
                    if (dataDir != null) File(dataDir) else null
                )

                val dataSubpaths = listOf(
                    "app_webview",
                    "app_webview/Default",
                    "app_webview/Default/HTTP Cache",
                    "app_webview/Default/HTTP Cache/Code Cache",
                    "app_webview/Default/HTTP Cache/Code Cache/wasm",
                    "app_webview/Default/HTTP Cache/Code Cache/js",
                    "app_webview/Default/Code Cache",
                    "app_webview/Default/Code Cache/wasm",
                    "app_webview/Default/Code Cache/js",
                    "app_webview/Default/GPUCache"
                )

                for (root in dataRoots) {
                    for (sub in dataSubpaths) {
                        ensureCleanDirectory(root, sub)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "WebView cache directories setup notice: ${e.message}")
            }
        }

        /**
         * Actively guards directory presence during the initial cold-start window (first 4 seconds)
         * to handle any asynchronous wipe/purge by Chromium subsystems.
         */
        private fun startCacheDirectoryGuard(context: Context) {
            appScope.launch {
                for (i in 0..40) {
                    ensureWebViewDirectories(context)
                    delay(100L)
                }
            }
        }
    }
}
