package com.blackbutton.fast.tool.secure

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.os.RemoteException
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceDataStore
import com.airbnb.lottie.LottieAnimationView
import com.blackbutton.fast.tool.secure.bean.ProfileBean
import com.blackbutton.fast.tool.secure.constant.Constant
import com.blackbutton.fast.tool.secure.ui.ResultsActivity
import com.blackbutton.fast.tool.secure.ui.agreement.AgreementWebView
import com.blackbutton.fast.tool.secure.ui.servicelist.ServiceListActivity
import com.blackbutton.fast.tool.secure.utils.DensityUtils
import com.blackbutton.fast.tool.secure.utils.JsonUtil
import com.blackbutton.fast.tool.secure.utils.NetworkPing.findTheBestIp
import com.blackbutton.fast.tool.secure.utils.ResourceUtils
import com.blackbutton.fast.tool.secure.utils.StatusBarUtils
import com.blackbutton.fast.tool.secure.utils.Utils.FlagConversion
import com.blackbutton.fast.tool.secure.widget.SlidingMenu
import com.github.shadowsocks.Core
import com.github.shadowsocks.R
import com.github.shadowsocks.aidl.IShadowsocksService
import com.github.shadowsocks.aidl.ShadowsocksConnection
import com.github.shadowsocks.bg.BaseService
import com.github.shadowsocks.database.Profile
import com.github.shadowsocks.database.ProfileManager
import com.github.shadowsocks.preference.DataStore
import com.github.shadowsocks.preference.OnPreferenceDataStoreChangeListener
import com.github.shadowsocks.utils.Key
import com.github.shadowsocks.utils.StartService
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.gson.reflect.TypeToken
import com.jeremyliao.liveeventbus.LiveEventBus
import com.xuexiang.xutil.XUtil
import com.xuexiang.xutil.common.ClickUtils
import com.xuexiang.xutil.tip.ToastUtils
import java.util.*
import kotlin.concurrent.schedule


class MainActivity : AppCompatActivity(), ShadowsocksConnection.Callback,
    OnPreferenceDataStoreChangeListener, ClickUtils.OnClick2ExitListener {
    companion object {
        var stateListener: ((BaseService.State) -> Unit)? = null
    }

    private lateinit var frameLayoutTitle: FrameLayout
    private lateinit var rightTitle: ImageView
    private lateinit var navigation: ImageView
    private lateinit var imgSwitch: LottieAnimationView
    private lateinit var txtConnect: TextView
    private lateinit var timer: Chronometer
    private lateinit var imgCountry: ImageView
    private lateinit var tvLocation: TextView
    private lateinit var mAdView: AdView
    private lateinit var slidingMenu: SlidingMenu
    private lateinit var laHomeMenu: RelativeLayout
    private lateinit var tvContact: TextView
    private lateinit var tvAgreement: TextView
    private lateinit var tvShare: TextView
    private lateinit var radioGroup: RadioGroup
    private var mInterstitialAd: InterstitialAd? = null
    private lateinit var checkSafeLocation: ProfileBean.SafeLocation
    var state = BaseService.State.Idle
    private val connection = ShadowsocksConnection(true)
    private var rangeTime = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StatusBarUtils.translucent(this)
        StatusBarUtils.setStatusBarLightMode(this)
        setContentView(R.layout.activity_main)
        initAd()
        initView()
        clickEvent()
        timerSet()
        initConnectionServer()
        initLiveBus()
    }

    private val connect = registerForActivityResult(StartService()) {
        if (it) {
            ToastUtils.toast(R.string.insufficient_permissions)
        }
    }

    private fun initLiveBus() {
        LiveEventBus
            .get(Constant.SERVER_INFORMATION, ProfileBean.SafeLocation::class.java)
            .observeForever {
                updateServer(it)
            }
    }

    private fun initAd() {
        val adRequest = AdRequest.Builder().build()
        mAdView = findViewById(R.id.adView)
        mAdView.loadAd(adRequest)

        InterstitialAd.load(
            this,
            "ca-app-pub-3940256099942544/1033173712",
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    adError.toString().let { Log.d("TAG", "===$it") }
                    mInterstitialAd = null
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d("TAG", "Ad was loaded.")
                    mInterstitialAd = interstitialAd
                }
            })
        mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdClicked() {
                // Called when a click is recorded for an ad.
                Log.d("TAG", "Ad was clicked.")
            }

            override fun onAdDismissedFullScreenContent() {
                // Called when ad is dismissed.
                Log.d("TAG", "Ad dismissed fullscreen content.")
                mInterstitialAd = null
            }


            override fun onAdImpression() {
                // Called when an impression is recorded for an ad.
                Log.d("TAG", "Ad recorded an impression.")
            }

            override fun onAdShowedFullScreenContent() {
                // Called when ad is shown.
                Log.d("TAG", "Ad showed fullscreen content.")
            }
        }

    }

    private fun initView() {
        frameLayoutTitle = findViewById(R.id.main_title)
        frameLayoutTitle.setPadding(
            0,
            DensityUtils.px2dp(StatusBarUtils.getStatusBarHeight(this).toFloat()) + 10, 0, 0
        )
        timer = findViewById(R.id.timer)
        imgSwitch = findViewById(R.id.img_switch)
        txtConnect = findViewById(R.id.txt_connect)
        imgCountry = findViewById(R.id.img_country)
        tvLocation = findViewById(R.id.tv_location)
        radioGroup = findViewById(R.id.radio_group)
        rightTitle = frameLayoutTitle.findViewById(R.id.ivRight)
        navigation = frameLayoutTitle.findViewById(R.id.ivBack)
        slidingMenu = findViewById(R.id.slidingMenu)
        laHomeMenu = findViewById(R.id.la_home_menu)
        tvContact = laHomeMenu.findViewById(R.id.tv_contact)
        tvAgreement = laHomeMenu.findViewById(R.id.tv_agreement)
        tvShare = laHomeMenu.findViewById(R.id.tv_share)
    }

    /**
     * 点击事件
     */
    private fun clickEvent() {
        navigation.setOnClickListener {
            slidingMenu.open()
        }
        tvContact.setOnClickListener {
            val uri = Uri.parse("mailto:vkas@qq.com")
            val intent = Intent(Intent.ACTION_SENDTO, uri)
            intent.putExtra(Intent.EXTRA_SUBJECT, "") // 主题
            intent.putExtra(Intent.EXTRA_TEXT, "") // 正文
            startActivity(Intent.createChooser(intent, "Please select mail application"))
        }
        tvAgreement.setOnClickListener {
            val intent = Intent(this@MainActivity, AgreementWebView::class.java)
            startActivity(intent)
        }
        tvShare.setOnClickListener {
            val intent = Intent()
            intent.action = Intent.ACTION_SEND
            intent.putExtra(Intent.EXTRA_TEXT, Constant.SHARE_ADDRESS + this.packageName)
            intent.type = "text/plain"
            startActivity(intent)
        }
        rightTitle.setOnClickListener {
            val intent = Intent(this@MainActivity, ServiceListActivity::class.java)
            startActivity(intent)
        }
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            imgSwitch.playAnimation()
            Timer().schedule(1000) {
                startVpn()
            }
        }
    }

    /**
     * 计时器设置
     */
    private fun timerSet() {
        timer.base = SystemClock.elapsedRealtime()
        val hour = ((SystemClock.elapsedRealtime() - timer.base) / 1000 / 60)
        timer.format = "0$hour:%s"
    }


    /**
     * 初始连接服务器
     */
    private fun initConnectionServer() {
        changeState(BaseService.State.Idle, animate = false)
        connection.connect(this, this)
        DataStore.publicStore.registerChangeListener(this)
        ProfileManager.getProfile(DataStore.profileId).let {
            val bestData = findTheBestIp()
            tvLocation.text = bestData.ufo_country + "-" + bestData.ufo_city
            imgCountry.setImageResource(FlagConversion(bestData.ufo_country))
            if (it != null) {
                ProfileManager.updateProfile(setServerData(it, bestData))
            } else {
                val profile = Profile()
                ProfileManager.createProfile(setServerData(profile, bestData))
            }
        }
        DataStore.profileId = 1L
    }

    /**
     * 设置服务器数据
     */
    private fun setServerData(profile: Profile, bestData: ProfileBean.SafeLocation): Profile {
        profile.name = bestData.ufo_country + "-" + bestData.ufo_city
        profile.host = bestData.ufo_ip.toString()
        profile.remotePort = bestData.ufo_port!!
        profile.password = bestData.ufo_pwd!!
        profile.method = bestData.ufo_method!!
        return profile
    }

    /**
     * 更新服务器
     */
    private fun updateServer(safeLocation: ProfileBean.SafeLocation) {
        if (state.name == "Connected") {
            ToastUtils.toast(R.string.disconnect_tips)
            return
        }
        checkSafeLocation = ProfileBean.SafeLocation()
        checkSafeLocation = safeLocation
        checkSafeLocation.ufo_country.let {

        }
        if (checkSafeLocation.bestServer==true) {
            tvLocation.text = Constant.FASTER_SERVER
            imgCountry.setImageResource(FlagConversion(Constant.FASTER_SERVER))

        } else {
            tvLocation.text = checkSafeLocation.ufo_country + "-" + checkSafeLocation.ufo_city
            imgCountry.setImageResource(FlagConversion(checkSafeLocation.ufo_country))
        }
        ProfileManager.getProfile(DataStore.profileId).let {
            if (it != null) {
                setServerData(it, safeLocation)
                ProfileManager.updateProfile(it)
            } else {
                ProfileManager.createProfile(Profile())
            }
        }
        DataStore.profileId = 1L
        startVpn()
    }

    /**
     * 启动VPN
     */
    private fun startVpn() {
        if (state.canStop) {
            Core.stopService()
            Looper.prepare()
            jumpToTheResultPage(false)
            Looper.loop()
        } else {
            connect.launch(null)
        }
    }

//    /**
//     * 启动插页广告
//     */
//    private fun startInterstitial() {
//
//    }

    override fun onServiceDisconnected() = changeState(BaseService.State.Idle)
    override fun onBinderDied() {
        connection.disconnect(this)
        connection.connect(this, this)
    }

    private fun changeState(
        state: BaseService.State,
        msg: String? = null,
        animate: Boolean = true
    ) {
        Log.i("TAG", "changeState: --->$state---msg=$msg")
        setConnectionStatusText(state.name)
        this.state = state
        stateListener?.invoke(state)
    }

    /**
     * 设置连接状态文本
     */
    private fun setConnectionStatusText(state: String) {
        when (state) {
            "Connecting" -> {
                txtConnect.text = "Connecting..."
            }
            "Connected" -> {
                if (rangeTime != 0) {
                    timer.base = timer.base + (SystemClock.elapsedRealtime() - rangeTime);
                } else {
                    timer.base = SystemClock.elapsedRealtime();
                }
                timer.start()
                txtConnect.text = "Connected"
                jumpToTheResultPage(true)
            }
            "Stopping" -> {
                txtConnect.text = "Stopping"
            }
            "Stopped" -> {
                timer.stop()
                rangeTime = SystemClock.elapsedRealtime().toInt()
                txtConnect.text = "Connect"
            }
            else -> {
                txtConnect.text = "Configuring"
            }
        }

    }

    /**
     * 跳转结果页
     */
    private fun jumpToTheResultPage(flag: Boolean) {
        imgSwitch.pauseAnimation()
        val intent = Intent(this@MainActivity, ResultsActivity::class.java)
        intent.putExtra(Constant.CONNECTION_STATUS, flag)
        startActivity(intent)
    }

    override fun stateChanged(state: BaseService.State, profileName: String?, msg: String?) =
        changeState(state, msg)

    override fun onServiceConnected(service: IShadowsocksService) = changeState(
        try {
            BaseService.State.values()[service.state]
        } catch (_: RemoteException) {
            BaseService.State.Idle
        }
    )

    override fun onPreferenceDataStoreChanged(store: PreferenceDataStore, key: String) {
        when (key) {
            Key.serviceMode -> {
                connection.disconnect(this)
                connection.connect(this, this)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        connection.bandwidthTimeout = 500
    }

    override fun onStop() {
        connection.bandwidthTimeout = 0
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        DataStore.publicStore.unregisterChangeListener(this)
        connection.disconnect(this)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            ClickUtils.exitBy2Click(2000, this)
        }
        return true
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onRetry() {
        ToastUtils.toast(R.string.exit_procedure)
    }

    override fun onExit() {
        XUtil.get().exitApp()
    }
}