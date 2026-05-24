package com.ttboost.tik.tok.followers.likes

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.util.regex.Pattern

class MainActivity : AppCompatActivity() {

    private lateinit var etAdbIp: EditText
    private lateinit var etTargetCoin: EditText
    private lateinit var btnOnlyFollow: Button
    private lateinit var btnOnlyLike: Button
    private lateinit var btnTotal: Button
    private lateinit var tvLogs: TextView
    private lateinit var btnEmergencyStop: Button

    private var isRunning = false
    private var jobMode = "TOTAL"
    private var earnedCoins = 0
    private var targetCoins = 1000
    private val mainHandler = Handler(Looper.getMainLooper())

    private val tiktokLoadDelay = 2500L
    private val returnDelay = 800L
    private val pkgName = "com.ttboost.tik.tok.followers.likes"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etAdbIp = findViewById(R.id.etAdbIp)
        etTargetCoin = findViewById(R.id.etTargetCoin)
        btnOnlyFollow = findViewById(R.id.btnOnlyFollow)
        btnOnlyLike = findViewById(R.id.btnOnlyLike)
        btnTotal = findViewById(R.id.btnTotal)
        tvLogs = findViewById(R.id.tvLogs)
        btnEmergencyStop = findViewById(R.id.btnEmergencyStop)

        setupButtons()
    }

    private fun setupButtons() {
        btnOnlyFollow.setOnClickListener {
            jobMode = "FOLLOW"
            log("[*] Đã chọn chế độ: Chỉ Follow", "#00FFFF")
            startAutomation()
        }

        btnOnlyLike.setOnClickListener {
            jobMode = "LIKE"
            log("[*] Đã chọn chế độ: Chỉ Tym", "#00FFFF")
            startAutomation()
        }

        btnTotal.setOnClickListener {
            jobMode = "TOTAL"
            log("[*] Đã chọn chế độ: Tổng Lực", "#00FFFF")
            startAutomation()
        }

        btnEmergencyStop.setOnClickListener {
            isRunning = false
            log("[!] ⏹ DỪNG TOOL KHẨN CẤP THÀNH CÔNG", "#FF0000")
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }

    private fun startAutomation() {
        if (!isRunning) {
            isRunning = true
            targetCoins = etTargetCoin.text.toString().toIntOrNull() ?: 1000
            val adbIp = etAdbIp.text.toString()
            
            log("[+] Kết nối thiết bị qua ADB $adbIp...", "#00CC00")
            Thread { runAutomationLoop() }.start()
        }
    }

    private fun log(message: String, colorHex: String) {
        mainHandler.post {
            tvLogs.append("\n<font color='$colorHex'>$message</font>")
            tvLogs.text = android.text.Html.fromHtml(tvLogs.text.toString(), android.text.Html.FROM_HTML_MODE_LEGACY)
        }
    }

    private fun executeRootCmd(cmd: String): String {
        try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            val isReader = BufferedReader(InputStreamReader(process.inputStream))
            os.writeBytes("$cmd\n")
            os.writeBytes("exit\n")
            os.flush()
            val output = StringBuilder()
            var line: String?
            while (isReader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            process.waitFor()
            return output.toString()
        } catch (e: Exception) {
            return ""
        }
    }

    private fun tapFast(x: Int, y: Int) {
        Runtime.getRuntime().exec(arrayOf("su", "-c", "input tap $x $y &"))
    }

    private fun getScreenXml(): String {
        executeRootCmd("uiautomator dump /sdcard/v.xml")
        return executeRootCmd("cat /sdcard/v.xml")
    }

    private fun parseElementCoords(xmlData: String, keywords: List<String>): Pair<Int, Int>? {
        for (keyword in keywords) {
            val pattern = Pattern.compile("(text|content-desc)=\"[^\"]*\\Q$keyword\\E[^\"]*\".*?bounds=\"\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]\"", Pattern.CASE_INSENSITIVE)
            val matcher = pattern.matcher(xmlData)
            if (matcher.find()) {
                val x1 = matcher.group(2)!!.toInt()
                val y1 = matcher.group(3)!!.toInt()
                val x2 = matcher.group(4)!!.toInt()
                val y2 = matcher.group(5)!!.toInt()
                return Pair((x1 + x2) / 2, (y1 + y2) / 2)
            }
        }
        return null
    }

    private fun runAutomationLoop() {
        while (isRunning && earnedCoins < targetCoins) {
            log("[*] Đang nhận diện nhiệm vụ...", "#FFFFFF")
            var currentJob = ""

            while (isRunning) {
                val xmlData = getScreenXml()

                if (xmlData.contains("completed the task") || xmlData.contains("Error")) {
                    val coords = parseElementCoords(xmlData, listOf("Skip", "Bỏ qua"))
                    if (coords != null) tapFast(coords.first, coords.second)
                    log("[⚠] Phát hiện lỗi, Skip!", "#FFA500")
                    Thread.sleep(1000)
                    continue
                }

                val followCoords = parseElementCoords(xmlData, listOf("Follow +", "Theo dõi +"))
                val likeCoords = parseElementCoords(xmlData, listOf("Like +", "Thích +"))
                
                var jobFound = false
                
                if ((jobMode == "FOLLOW" || jobMode == "TOTAL") && followCoords != null) {
                    tapFast(followCoords.first, followCoords.second)
                    currentJob = "follow"
                    jobFound = true
                } else if ((jobMode == "LIKE" || jobMode == "TOTAL") && likeCoords != null) {
                    tapFast(likeCoords.first, likeCoords.second)
                    currentJob = "like"
                    jobFound = true
                }

                if (jobFound) break else Thread.sleep(500)
            }

            if (!isRunning) break
            Thread.sleep(tiktokLoadDelay)

            if (currentJob == "like") {
                tapFast(540, 960)
                Thread.sleep(80)
                tapFast(540, 960)
                Thread.sleep(1000)
            } else if (currentJob == "follow") {
                val tiktokXml = getScreenXml()
                val coords = parseElementCoords(tiktokXml, listOf("Follow", "Theo dõi", "Follow button"))
                if (coords != null) {
                    tapFast(coords.first, coords.second)
                } else {
                    tapFast(900, 800)
                }
                Thread.sleep(1000)
            }

            if (!isRunning) break
            Runtime.getRuntime().exec(arrayOf("su", "-c", "am start -n $pkgName/.MainActivity"))
            Thread.sleep(returnDelay)
            
            earnedCoins += 10
            log("[] []Earned: $earnedCoins / Target: $targetCoins[] []", "#BF00FF")
        }
        
        if (earnedCoins >= targetCoins) {
            log("[+] ĐÃ HOÀN THÀNH MỤC TIÊU XU!", "#00CC00")
            isRunning = false
        }
    }
}
