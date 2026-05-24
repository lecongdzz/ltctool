package com.ttboost.tik.tok.followers.likes

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.util.regex.Pattern

class MainActivity : AppCompatActivity() {

    private lateinit var btnStart: Button
    private lateinit var tvLogs: TextView
    private var isRunning = false
    private val mainHandler = Handler(Looper.getMainLooper())
    
    private var cachedFollowX: Int? = null
    private var cachedFollowY: Int? = null
    
    private val tiktokLoadDelay = 2500L
    private val returnDelay = 800L
    private val pkgName = "com.ttboost.tik.tok.followers.likes"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnStart = findViewById(R.id.btnStart)
        tvLogs = findViewById(R.id.tvLogs)

        btnStart.setOnClickListener {
            if (!isRunning) {
                isRunning = true
                btnStart.text = "ĐANG CHẠY HỆ THỐNG..."
                btnStart.setBackgroundColor(android.graphics.Color.DKGRAY)
                log("[🚀] KHỞI ĐỘNG HỆ THỐNG THÀNH CÔNG! ĐANG ÉP XUNG V3.3...")
                Thread { runAutomationLoop() }.start()
            } else {
                isRunning = false
                btnStart.text = "KHỞI ĐỘNG HỆ THỐNG"
                btnStart.setBackgroundColor(android.graphics.Color.RED)
                log("[!] Đã dừng hệ thống an toàn.")
            }
        }
    }

    private fun log(message: String) {
        mainHandler.post {
            tvLogs.append("\n$message")
            val scrollAmount = tvLogs.layout.getLineTop(tvLogs.lineCount) - tvLogs.height
            if (scrollAmount > 0) {
                tvLogs.scrollTo(0, scrollAmount)
            }
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
        var count = 0
        while (isRunning) {
            count++
            log("[*] Vòng $count: Đang quét cấu trúc TTBoost...")
            var currentJob = ""

            while (isRunning) {
                val xmlData = getScreenXml()

                if (xmlData.contains("completed the task") || xmlData.contains("Error")) {
                    val coords = parseElementCoords(xmlData, listOf("Skip", "Bỏ qua"))
                    if (coords != null) tapFast(coords.first, coords.second)
                    log("-> [Hệ thống] Phát hiện lỗi, tự động Skip!")
                    Thread.sleep(1000)
                    continue
                }

                if (xmlData.contains("OK") || xmlData.contains("Đóng") || xmlData.contains("Close")) {
                    val coords = parseElementCoords(xmlData, listOf("OK", "Đóng", "Close"))
                    if (coords != null) tapFast(coords.first, coords.second)
                    Thread.sleep(500)
                    continue
                }

                val followCoords = parseElementCoords(xmlData, listOf("Follow +", "Theo dõi +"))
                val likeCoords = parseElementCoords(xmlData, listOf("Like +", "Thích +"))
                
                var jobFound = false
                
                if (followCoords != null) {
                    tapFast(followCoords.first, followCoords.second)
                    currentJob = "follow"
                    jobFound = true
                } else if (likeCoords != null) {
                    tapFast(likeCoords.first, likeCoords.second)
                    currentJob = "like"
                    jobFound = true
                }

                if (jobFound) break else Thread.sleep(500)
            }

            if (!isRunning) break
            log("-> Nhận dạng thành công [${currentJob.uppercase()}]. Tiến vào TikTok...")
            Thread.sleep(tiktokLoadDelay)

            if (currentJob == "like") {
                log("-> [Quét Định Vị] Kiểm tra nút Tym trên màn hình TikTok...")
                val tiktokXml = getScreenXml()
                val coords = parseElementCoords(tiktokXml, listOf("Like", "Thích", "Button, Like"))
                if (coords != null) {
                    log("-> [Tọa độ] Tìm thấy nút Tym thực tế tại $coords. Click!")
                    tapFast(coords.first, coords.second)
                } else {
                    log("-> [Dự phòng] Không thấy nút bấm, kích hoạt Double Tap vô ảnh!")
                    tapFast(540, 960)
                    Thread.sleep(80)
                    tapFast(540, 960)
                }
                Thread.sleep(1000)
            } else if (currentJob == "follow") {
                if (cachedFollowX != null && cachedFollowY != null) {
                    log("-> [Trí tuệ BK] Sử dụng tọa độ Follow đã học: $cachedFollowX, $cachedFollowY")
                    tapFast(cachedFollowX!!, cachedFollowY!!)
                } else {
                    log("-> [Học máy] Đang quét tìm tọa độ nút Follow lần đầu...")
                    val tiktokXml = getScreenXml()
                    val coords = parseElementCoords(tiktokXml, listOf("Follow", "Theo dõi", "Follow button"))
                    if (coords != null) {
                        cachedFollowX = coords.first
                        cachedFollowY = coords.second
                        tapFast(cachedFollowX!!, cachedFollowY!!)
                        log("-> [Ghi nhớ] Đã găm tọa độ Follow thành công!")
                    } else {
                        tapFast(900, 800)
                    }
                }
                Thread.sleep(1000)
            }

            if (!isRunning) break
            Runtime.getRuntime().exec(arrayOf("su", "-c", "am start -n $pkgName/.MainActivity"))
            Thread.sleep(returnDelay)
            log("[+] Chu kỳ $count HOÀN THÀNH XUẤT SẮC!")
        }
    }
}
