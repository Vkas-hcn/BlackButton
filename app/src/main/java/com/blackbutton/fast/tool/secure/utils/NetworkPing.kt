package com.blackbutton.fast.tool.secure.utils

import android.util.Log
import com.blackbutton.fast.tool.secure.bean.ProfileBean
import com.blackbutton.fast.tool.secure.constant.Constant
import com.example.testdemo.utils.KLog
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
object NetworkPing {
    private val job = Job()

    val scope = CoroutineScope(job)

    private val TAG = "PING-SJSX:"

    fun ping(
        profile: ProfileBean) {
        scope.launch {
            while (true) {
                profile.safeLocation.let { it ->
                    it?.map {
                        it.ufo_ip?.let { it1 ->
                          KLog.e("TAG2","==="+pingIP(it1) )
                        }
                    }
                }
            }

        }
    }

    /**
     * pingIP
     */
    private fun pingIP(ip: String):String{
        val command = "ping -c 1 -W 1500 $ip"
        val proc = Runtime.getRuntime().exec(command)
        val reader = BufferedReader(InputStreamReader(proc.inputStream))
        var time=""
        when (proc.waitFor()) {
            0 -> {
                val result = StringBuilder()
                while (true) {
                    val line = reader.readLine() ?: break
                    result.append(line).append("\n")
                }
                result.toString().let {
                     time= it.substring(it.indexOf("time=")+5)
                }
            }
        }
        return time
    }
    // 关闭当前的协程
    fun pingCancle() {
        scope.cancel()
    }
    /**
     * 找到最佳ip
     */
    fun findTheBestIp():ProfileBean.SafeLocation{
        val profileBean: ProfileBean = if (Utils.isNullOrEmpty(MmkvUtils.getStringValue(Constant.PROFILE_DATA))) {
             getProfileJsonData()
        } else {
            JsonUtil.fromJson(
                MmkvUtils.getStringValue(Constant.PROFILE_DATA),
                object : TypeToken<ProfileBean?>() {}.type
            )
        }
        profileBean.safeLocation?.shuffled()?.take(1)?.forEach {
            return it
        }
        return profileBean.safeLocation!![0]
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
}