package com.blackbutton.fast.tool.secure.ui

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.blackbutton.fast.tool.secure.app.App
import com.blackbutton.fast.tool.secure.constant.Constant
import com.blackbutton.fast.tool.secure.utils.*
import com.blackbutton.fast.tool.secure.utils.NetworkPing.findTheBestIp
import com.blackbutton.fast.tool.secure.widget.HorizontalProgressView
import com.example.testdemo.utils.KLog
import com.first.conn.BuildConfig
import com.first.conn.R
import com.github.shadowsocks.bean.AroundFlowBean
import com.google.android.gms.ads.AdLoader
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.jeremyliao.liveeventbus.LiveEventBus
import com.xuexiang.xutil.tip.ToastUtils
import java.util.*

/**
 * Startup Page
 */
class StartupActivity : AppCompatActivity(),
    HorizontalProgressView.HorizontalProgressUpdateListener {
    private lateinit var horizontalProgressView: HorizontalProgressView
    private var secondsRemaining: Long = 0L
    private lateinit var mNativeAds: AdLoader

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
    }

    /**
     * 获取Firebase数据
     */
    private fun getFirebaseData() {
        if (BuildConfig.DEBUG) {
            val application = application as? App

            application?.AD_UNIT_ID = "ca-app-pub-3940256099942544/3419835294"

            openAd(10L)
            return
        } else {
            val auth = Firebase.remoteConfig
            auth.fetchAndActivate().addOnSuccessListener {
                ToastUtils.toast("fireBase Connection succeeded")
                MmkvUtils.set(Constant.AROUND_FLOW_DATA, auth.getString("aroundFlowData"))
                MmkvUtils.set(Constant.PROFILE_DATA, auth.getString("profileData"))
            }.addOnCompleteListener {
//                timer.schedule(timerTask, 2000)
                mNativeAds = AdLoader.Builder(this, "ca-app-pub-3940256099942544/2247696110").build()
                LiveEventBus.get(Constant.NATIVE_ADS).post(mNativeAds)

            }
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
        MmkvUtils.set(Constant.BEST_SERVICE_DATA, dataJson)
        startActivity(intent)
    }

    private fun openAd(seconds: Long) {
        val countDownTimer: CountDownTimer = object : CountDownTimer(seconds * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                secondsRemaining = millisUntilFinished / 1000 + 1
            }

            override fun onFinish() {
                secondsRemaining = 0

                val application = application as? App
                if (application == null) {
                    jumpPage()
                    return
                }
                // Show the app open ad.
                application.showAdIfAvailable(
                    this@StartupActivity,
                    object : App.OnShowAdCompleteListener {
                        override fun onShowAdComplete() {
                            jumpPage()
                        }
                    })
            }
        }
        countDownTimer.start()
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