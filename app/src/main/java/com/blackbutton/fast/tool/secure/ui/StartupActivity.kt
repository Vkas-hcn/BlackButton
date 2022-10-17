package com.blackbutton.fast.tool.secure.ui

import android.os.Bundle
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

import java.util.*
import android.content.Intent
import com.blackbutton.fast.tool.secure.MainActivity
import com.blackbutton.fast.tool.secure.utils.StatusBarUtils
import com.github.shadowsocks.R
import com.jeremyliao.liveeventbus.LiveEventBus

/**
 * Startup Page
 */
class StartupActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StatusBarUtils.translucent(this)
        setContentView(R.layout.activity_startup)
        supportActionBar?.hide()
        initView()
    }
    private fun initView() {
        val timer = Timer()
        val timerTask: TimerTask = HomeTimerTask()
        timer.schedule(timerTask, 2000)
        LiveEventBus
            .get("JUMP_PAGE", Boolean::class.java)
            .observeForever {
                jumpPage()
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
        startActivity(intent)
    }
    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}