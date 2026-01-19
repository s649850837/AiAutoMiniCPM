package com.example.aiautominitest.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object AssetUtils {
    private const val TAG = "AssetUtils"

    /**
     * 将 Assets 中的目录拷贝到内部存储
     * @param context Context
     * @param assetDir Assets 中的目录名
     * @param destDir 目标目录（相对于 filesDir）
     * @return 拷贝后的绝对路径，如果失败返回 null
     */
    fun copyAssetFolder(context: Context, assetDir: String, destDir: String = assetDir): String? {
        val filesDir = context.getExternalFilesDir(null) ?: context.filesDir
        val targetDir = File(filesDir, destDir)
        
        try {
            if (!copyAssetsRecursively(context, assetDir, targetDir)) {
                return null
            }
            return targetDir.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy assets", e)
            return null
        }
    }

    private fun copyAssetsRecursively(context: Context, assetPath: String, targetDir: File): Boolean {
        try {
            val assets = context.assets.list(assetPath) ?: return false
            
            if (assets.isEmpty()) {
                // It's a file (or empty dir, but list() returns empty array for file in some versions, 
                // but standard way is to try open it if list is empty or check logic. 
                // Actually list("") returns root files. list("somefile") returns empty. 
                // But list("somedir") returns children.
                // Better approach: try to open as stream. If works, it's a file.
                return copyAssetFile(context, assetPath, targetDir)
            }

            // It's a directory (or file returning empty list? No, list on file returns nothing usually)
            // But we must be careful.
            
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }

            for (asset in assets) {
                // Skip some common junk
                if (asset == "images" || asset == "webkit" || asset == "sounds" || asset == "k2-fsa") continue 
                
                val subAssetPath = if (assetPath.isEmpty()) asset else "$assetPath/$asset"
                val subTargetFile = File(targetDir, asset)
                
                // Recursion
                // We need to know if subAssetPath is a directory or file. 
                // context.assets.list(subAssetPath) will tell us.
                val subAssets = context.assets.list(subAssetPath)
                if (subAssets != null && subAssets.isNotEmpty()) {
                    copyAssetsRecursively(context, subAssetPath, subTargetFile)
                } else {
                    // Could be file or empty dir. Try copy as file.
                    copyAssetFile(context, subAssetPath, subTargetFile)
                }
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error copying $assetPath", e)
            return false
        }
    }

    private fun copyAssetFile(context: Context, assetPath: String, destFile: File): Boolean {
        try {
            val inputStream = context.assets.open(assetPath)
            // Ensure parent exists
            destFile.parentFile?.mkdirs()
            
            val outputStream = FileOutputStream(destFile)
            val buffer = ByteArray(1024)
            var read: Int
            while (inputStream.read(buffer).also { read = it } != -1) {
                outputStream.write(buffer, 0, read)
            }
            inputStream.close()
            outputStream.flush()
            outputStream.close()
            return true
        } catch (e: IOException) {
            // It might be a directory that appeared empty in list()?
            // Log.w(TAG, "Could not open $assetPath as file: ${e.message}")
            return false
        }
    }
}
