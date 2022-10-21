package com.blackbutton.fast.tool.secure.ui

import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.blackbutton.fast.tool.secure.MainActivity
import com.github.shadowsocks.bean.AroundFlowBean
import com.blackbutton.fast.tool.secure.constant.Constant
import com.blackbutton.fast.tool.secure.utils.*
import com.blackbutton.fast.tool.secure.utils.NetworkPing.findTheBestIp
import com.blackbutton.fast.tool.secure.widget.HorizontalProgressView
import com.example.testdemo.utils.KLog
import com.github.shadowsocks.BuildConfig
import com.github.shadowsocks.R
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.gson.reflect.TypeToken
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

/**
 * Startup Page
 */
class StartupActivity : AppCompatActivity(),
    HorizontalProgressView.HorizontalProgressUpdateListener {
    private lateinit var horizontalProgressView: HorizontalProgressView
    private val timer = Timer()
    private val timerTask: TimerTask = HomeTimerTask()
    // 绕流数据
    private lateinit var aroundFlowData: AroundFlowBean
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StatusBarUtils.translucent(this)
        setContentView(R.layout.activity_startup)
        supportActionBar?.hide()
        initView()
    }

    private fun initView() {
        horizontalProgressView = findViewById(R.id.pb_start)
        horizontalProgressView.setProgressViewUpdateListener(this)
        horizontalProgressView.startProgressAnimation()
        aroundFlowData = AroundFlowBean()
        getFirebaseData()

        LiveEventBus
            .get("JUMP_PAGE", Boolean::class.java)
            .observeForever {
                jumpPage()
            }

    }

    /**
     * 获取Firebase数据
     */
    private fun getFirebaseData() {
        timer.schedule(timerTask, 2000)
        if (BuildConfig.DEBUG) {
            return
        }
        val auth = Firebase.remoteConfig
        auth.fetchAndActivate().addOnSuccessListener {
            MmkvUtils.set(Constant.AROUND_FLOW_DATA, auth.getString("aroundFlowData"))
            MmkvUtils.set(Constant.PROFILE_DATA, auth.getString("profileData"))
        }
    }

    /**
     * 延时
     */
    class HomeTimerTask : TimerTask() {
        override fun run() {
            Looper.prepare()
            LiveEventBus.get("JUMP_PAGE").post(true)
            Looper.loop()
        }
    }

    /**
     * 跳转页面
     */
    private fun jumpPage() {
        val intent = Intent(this@StartupActivity, MainActivity::class.java)
        val bestData = findTheBestIp()
        val dataJson = JsonUtil.toJson(bestData)
        intent.putExtra(Constant.BEST_SERVICE_DATA, dataJson)
        startActivity(intent)
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        horizontalProgressView.stopProgressAnimation()
        horizontalProgressView.setProgressViewUpdateListener(null)
    }

    override fun onHorizontalProgressStart(view: View?) {
    }

    override fun onHorizontalProgressUpdate(view: View?, progress: Float) {
    }

    override fun onHorizontalProgressFinished(view: View?) {
    }
}