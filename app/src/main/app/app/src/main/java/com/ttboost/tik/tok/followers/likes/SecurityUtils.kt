package com.ttboost.tik.tok.followers.likes

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.security.MessageDigest
import kotlin.system.exitProcess

object SecurityUtils {
    private const val VALID_SIGNATURE_SHA256 = "DUMMY_SHA256_REPLACE_ME_LATER"

    fun performSecurityChecks(context: Context) {
        if (isDebuggerAttached() || isEmulator() || isFridaDetected()) {
            exitApp()
        }
        checkAppSignature(context)
    }

    private fun checkAppSignature(context: Context) {
        try {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES
            )
            for (signature in packageInfo.signatures) {
                val md = MessageDigest.getInstance("SHA-256")
                md.update(signature.toByteArray())
                val currentSignature = md.digest().joinToString("") { "%02x".format(it) }
                
                if (VALID_SIGNATURE_SHA256 != "DUMMY_SHA256_REPLACE_ME_LATER" &&
                    !currentSignature.equals(VALID_SIGNATURE_SHA256, ignoreCase = true)) {
                    exitApp()
                }
            }
        } catch (e: Exception) {
            exitApp()
        }
    }

    private fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.lowercase().contains("vbox")
                || Build.FINGERPRINT.lowercase().contains("test-keys")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.lowercase().contains("droid4x")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.HARDWARE.contains("vbox86")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator"))
    }

    private fun isDebuggerAttached(): Boolean {
        return Debug.isDebuggerConnected() || Debug.waitingForDebugger()
    }

    private fun isFridaDetected(): Boolean {
        try {
            val reader = BufferedReader(FileReader("/proc/self/maps"))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line!!.contains("frida") || line!!.contains("xposed")) {
                    reader.close()
                    return true
                }
            }
            reader.close()
        } catch (e: Exception) {
        }
        val fridaServerPaths = arrayOf("/data/local/tmp/frida-server", "/data/local/tmp/re.frida.server")
        for (path in fridaServerPaths) {
            if (File(path).exists()) return true
        }
        return false
    }

    private fun exitApp() {
        android.os.Process.killProcess(android.os.Process.myPid())
        exitProcess(1)
    }
}
