package com.blackbutton.fast.tool.secure.utils

import android.content.Context
import android.net.ConnectivityManager
import com.blackbutton.fast.tool.secure.bean.ProfileBean
import com.blackbutton.fast.tool.secure.constant.Constant
import com.example.testdemo.utils.KLog
import com.google.gson.reflect.TypeToken
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader

object NetworkPing {
    private val mmkv by lazy {
        //启用mmkv的多进程功能
        MMKV.mmkvWithID("Spin", MMKV.MULTI_PROCESS_MODE)
    }
    private val job = Job()

    val scope = CoroutineScope(job)

    private val TAG = "PING-SJSX:"
    private var flag = true

    /**
     * pingIP
     */
    fun pingIP(ip: String?): String {
        val command = "ping -c 1 -W 1500 $ip"
        val proc = Runtime.getRuntime().exec(command)
        val reader = BufferedReader(InputStreamReader(proc.inputStream))
        var time = ""
        flag = false
        when (proc.waitFor()) {
            0 -> {
                val result = StringBuilder()
                while (true) {
                    val line = reader.readLine() ?: break
                    result.append(line).append("\n")
                }
                result.toString().let {
                    time = it.substring(it.indexOf("time=") + 5)
                }
            }
            else -> {
                // 只要是没有ping通,肯定是有原因
                // 网络权限,ip地址,命令有误 等
            }
        }
        flag = true
        return time
    }

    fun ping2(
        ip: String = "www.baidu.com",
        whileTime: Long = 1500,
        pingMessage: (String) -> Unit = { _ -> },
        pingSuccess: (Boolean) -> Unit
    ) {
        scope.launch {
            flag = false
            val command = "ping -c 1 -W 1 $ip"
            while (true) {
                // 每[whileTime]s去 ping一次地址
                delay(whileTime)
                val proc = withContext(Dispatchers.IO) {
                    Runtime.getRuntime().exec(command)
                }
                val reader = BufferedReader(InputStreamReader(proc.inputStream))
                when (withContext(Dispatchers.IO) {
                    proc.waitFor()
                }) {
                    0 -> {
                        // 等价 pingSuccess(true)
                        pingSuccess.invoke(true)
                        val result = StringBuilder()
                        while (true) {
                            val line =
                                withContext(Dispatchers.IO) {
                                    reader.readLine()
                                } ?: break
                            result.append(line).append("\n")
                        }
                        result.toString().let {
                            pingMessage(it.substring(it.indexOf("time=") + 5))
                            flag = true

                        }

                    }
                    else -> {
                        // 只要是没有ping通,肯定是有原因
                        // 网络权限,ip地址,命令有误 等
                        pingSuccess.invoke(false)
                        pingCancle()
                    }
                }
            }

        }
    }
    // 关闭当前的协程
    fun pingCancle() {
        scope.cancel()
    }

    /**
     * 找到最佳ip
     */
    @Synchronized
    fun findTheBestIp(): ProfileBean.SafeLocation {
        val profileBean: ProfileBean =
            if (Utils.isNullOrEmpty(mmkv.decodeString(Constant.PROFILE_DATA))) {
                getProfileJsonData()
            } else {
                JsonUtil.fromJson(
                    mmkv.decodeString(Constant.PROFILE_DATA),
                    object : TypeToken<ProfileBean?>() {}.type
                )
            }
        profileBean.safeLocation?.shuffled()?.take(1)?.forEach {
            it.bestServer =true
            return it
        }
        profileBean.safeLocation!![0].bestServer = true
        return profileBean.safeLocation[0]
    }
    /**
     *
     */
    /**
     * @return 解析json文件
     */
    fun getProfileJsonData(): ProfileBean {
        return JsonUtil.fromJson(
            ResourceUtils.readStringFromAssert("serviceJson.json"),
            object : TypeToken<ProfileBean?>() {}.type
        )
    }

    @Throws(Exception::class)
    fun ping1(ipAddress: String): String? {
        var line: String? = null
        try {
            val pro = Runtime.getRuntime().exec("ping -c 1 -w 1 $ipAddress")
            val buf = BufferedReader(
                InputStreamReader(
                    pro.inputStream
                )
            )
            while (buf.readLine().also { line = it } != null) {
                return line
            }
        } catch (ex: Exception) {
            println(ex.message)
        }
        return ""
    }

    /**
     * check NetworkAvailable
     * @param context
     * @return
     */
    fun isNetworkAvailable(context: Context?): Boolean {
        val manager = context!!.applicationContext.getSystemService(
            Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val info = manager.activeNetworkInfo
        return !(null == info || !info.isAvailable)
    }
}