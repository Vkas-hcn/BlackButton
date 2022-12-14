package com.blackbutton.fast.tool.secure.ui

import android.content.Intent
import android.net.Uri
import android.os.*
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.preference.PreferenceDataStore
import com.airbnb.lottie.LottieAnimationView
import com.blackbutton.fast.tool.secure.ad.AdLoad
import com.blackbutton.fast.tool.secure.bean.ProfileBean
import com.blackbutton.fast.tool.secure.constant.Constant
import com.blackbutton.fast.tool.secure.ui.agreement.AgreementWebView
import com.blackbutton.fast.tool.secure.ui.servicelist.ServiceListActivity
import com.blackbutton.fast.tool.secure.utils.*
import com.blackbutton.fast.tool.secure.utils.Utils.FlagConversion
import com.blackbutton.fast.tool.secure.widget.SlidingMenu
import com.example.testdemo.utils.KLog
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
import com.google.android.gms.ads.*
import com.google.android.gms.ads.formats.NativeAdOptions
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.gson.reflect.TypeToken
import com.jeremyliao.liveeventbus.LiveEventBus
import com.tencent.mmkv.MMKV
import com.xuexiang.xutil.XUtil
import com.xuexiang.xutil.common.ClickUtils
import com.xuexiang.xutil.tip.ToastUtils


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
    private lateinit var radioGroup: LinearLayout
    private lateinit var radioButton0: TextView
    private lateinit var radioButton1: TextView
    private lateinit var clSwitch: ConstraintLayout
    private var mInterstitialAd: InterstitialAd? = null
    private lateinit var mNativeAds: AdLoader.Builder
    private lateinit var adRequest: AdRequest
    var state = BaseService.State.Idle
    private val connection = ShadowsocksConnection(true)
    private var rangeTime = 0f
    private lateinit var bestServiceData: ProfileBean.SafeLocation
    private var isFrontDesk = false
    private val mmkv by lazy {
        //??????mmkv??????????????????
        MMKV.mmkvWithID("Spin", MMKV.MULTI_PROCESS_MODE)
    }
    private lateinit var ad_frame: FrameLayout
    var currentNativeAd: NativeAd? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with(resources.displayMetrics) {
            density = heightPixels / 780.0F
            densityDpi = (160 * density).toInt()
        }
        StatusBarUtils.translucent(this)
        StatusBarUtils.setStatusBarLightMode(this)
        setContentView(R.layout.activity_main)
        initParam()
        initAd()
        initNativeAds()
        initView()
        clickEvent()
        timerSet()
        initConnectionServer()
        initLiveBus()
    }

    private val connect = registerForActivityResult(StartService()) {
        if (it) {
            imgSwitch.pauseAnimation()
            ToastUtils.toast(R.string.insufficient_permissions)
        }
    }

    /**
     * initParam
     */
    private fun initParam() {
        bestServiceData = JsonUtil.fromJson(
            mmkv.decodeString(Constant.BEST_SERVICE_DATA),
            object : TypeToken<ProfileBean.SafeLocation?>() {}.type
        )
    }

    private fun initLiveBus() {
        LiveEventBus
            .get(Constant.SERVER_INFORMATION, ProfileBean.SafeLocation::class.java)
            .observeForever {
                updateServer(it)
            }
        LiveEventBus
            .get(Constant.PLUG_ADVERTISEMENT_CACHE, InterstitialAd::class.java)
            .observeForever {
                mInterstitialAd = it
                plugInAdvertisementCallback()
            }
    }

    private fun initAd() {
        adRequest = AdRequest.Builder().build()
        loadScreenAdvertisement(adRequest)
    }

    /**
     * ?????????????????????
     */
    private fun initNativeAds() {
        ad_frame = findViewById(R.id.ad_frame)
        mNativeAds = AdLoader.Builder(this, "ca-app-pub-3940256099942544/2247696110")
        mNativeAds.forNativeAd { nativeAd ->
            // OnUnifiedNativeAdLoadedListener implementation.
            // If this callback occurs after the activity is destroyed, you must call
            // destroy and return or you may get a memory leak.
            var activityDestroyed = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                activityDestroyed = isDestroyed
            }
            if (activityDestroyed || isFinishing || isChangingConfigurations) {
                nativeAd.destroy()
                return@forNativeAd
            }
            // You must call destroy on old ads when you are done with them,
            // otherwise you will have a memory leak.
            currentNativeAd?.destroy()
            currentNativeAd = nativeAd
            val adView = layoutInflater
                .inflate(R.layout.layout_ad_view, null) as NativeAdView
//            val adView = fl.findViewById<NativeAdView>(R.id.nad_view)
            populateNativeAdView(nativeAd, adView)
            ad_frame.removeAllViews()
            ad_frame.addView(adView)
        }
        val videoOptions = VideoOptions.Builder()
            .setStartMuted(true)
            .build()

        val adOptions = NativeAdOptions.Builder()
            .setVideoOptions(videoOptions)
            .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_LEFT)
            .setMediaAspectRatio(com.google.android.gms.ads.nativead.NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_PORTRAIT)
            .build()

        mNativeAds.withNativeAdOptions(adOptions)
        val adLoader = mNativeAds.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                val error =
                    """
           domain: ${loadAdError.domain}, code: ${loadAdError.code}, message: ${loadAdError.message}
          """"
                Toast.makeText(
                    this@MainActivity, "Failed to load native ad with error $error",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }).build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    private fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
        // Set the media view.
        adView.mediaView = adView.findViewById(R.id.ad_media)

        // Set other ad assets.
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        adView.iconView = adView.findViewById(R.id.ad_app_icon)
        adView.advertiserView = adView.findViewById(R.id.ad_advertiser)
        (adView.headlineView as TextView).text = nativeAd.headline
        nativeAd.mediaContent?.let { adView.mediaView?.setMediaContent(it) }

        // These assets aren't guaranteed to be in every UnifiedNativeAd, so it's important to
        // check before trying to display them.
        if (nativeAd.body == null) {
            adView.bodyView?.visibility = View.INVISIBLE
        } else {
            adView.bodyView?.visibility = View.VISIBLE
            (adView.bodyView as TextView).text = nativeAd.body
        }

        if (nativeAd.callToAction == null) {
            adView.callToActionView?.visibility = View.INVISIBLE
        } else {
            adView.callToActionView?.visibility = View.VISIBLE
            (adView.callToActionView as Button).text = nativeAd.callToAction
        }

        if (nativeAd.icon == null) {
            adView.iconView?.visibility = View.GONE
        } else {
            (adView.iconView as ImageView).setImageDrawable(
                nativeAd.icon?.drawable
            )
            adView.iconView?.visibility = View.VISIBLE
        }

        if (nativeAd.advertiser == null) {
            adView.advertiserView?.visibility = View.INVISIBLE
        } else {
            (adView.advertiserView as TextView).text = nativeAd.advertiser
            adView.advertiserView?.visibility = View.VISIBLE
        }

        // This method tells the Google Mobile Ads SDK that you have finished populating your
        // native ad view with this native ad.
        adView.setNativeAd(nativeAd)

    }

    private fun loadScreenAdvertisement(adRequest: AdRequest) {
        AdLoad.loadScreenAdvertisement(this, "ca-app-pub-3940256099942544/1033173712", adRequest)
    }

    private fun initView() {
        frameLayoutTitle = findViewById(R.id.main_title)
        frameLayoutTitle.setPadding(
            0,
            DensityUtils.px2dp(StatusBarUtils.getStatusBarHeight(this).toFloat()) + 50, 0, 0
        )
        timer = findViewById(R.id.timer)
        imgSwitch = findViewById(R.id.img_switch)
        txtConnect = findViewById(R.id.txt_connect)
        imgCountry = findViewById(R.id.img_country)
        tvLocation = findViewById(R.id.tv_location)
        radioGroup = findViewById(R.id.radio_group)
        radioButton0 = findViewById(R.id.radio_button0)
        radioButton1 = findViewById(R.id.radio_button1)
        rightTitle = frameLayoutTitle.findViewById(R.id.ivRight)
        navigation = frameLayoutTitle.findViewById(R.id.ivBack)
        slidingMenu = findViewById(R.id.slidingMenu)
        laHomeMenu = findViewById(R.id.la_home_menu)
        tvContact = laHomeMenu.findViewById(R.id.tv_contact)
        tvAgreement = laHomeMenu.findViewById(R.id.tv_agreement)
        tvShare = laHomeMenu.findViewById(R.id.tv_share)
        clSwitch = findViewById(R.id.cl_switch)
    }

    /**
     * ????????????
     */
    private fun clickEvent() {
        navigation.setOnClickListener {
            if (!imgSwitch.isAnimating) {
                slidingMenu.open()
            }
        }
        tvContact.setOnClickListener {
            val uri = Uri.parse("mailto:${Constant.MAILBOX_ADDRESS}")
            val intent = Intent(Intent.ACTION_SENDTO, uri)
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
            if (!imgSwitch.isAnimating) {
                val intent = Intent(this@MainActivity, ServiceListActivity::class.java)
                if (state.name == "Connected") {
                    intent.putExtra(Constant.WHETHER_CONNECTED, true)
                } else {
                    intent.putExtra(Constant.WHETHER_CONNECTED, false)
                }
                intent.putExtra(Constant.CURRENT_IP, bestServiceData.bb_ip)
                intent.putExtra(Constant.WHETHER_BEST_SERVER, bestServiceData.bestServer)

                startActivity(intent)
            }
        }
        radioGroup.setOnClickListener {
            vpnSwitch()
        }
        clSwitch.setOnClickListener {
            vpnSwitch()
        }
    }

    /**
     * vpnSwitch
     */
    private fun vpnSwitch() {
        imgSwitch.playAnimation()
        MmkvUtils.set(Constant.SLIDING, true)
        mInterstitialAd?.show(this)
    }

    /**
     * ????????????
     */
    private fun setSwitchStatus() {
        if (state.name == "Connected") {
            radioButton0.setTextColor(getColor(R.color.white))
            radioButton0.background = resources.getDrawable(R.drawable.radio_bg_check)

            radioButton1.setTextColor(getColor(R.color.white))
            radioButton1.background = null
        } else {
            radioButton1.setTextColor(getColor(R.color.white))
            radioButton1.background = resources.getDrawable(R.drawable.radio_bg_check)

            radioButton0.setTextColor(getColor(R.color.white))
            radioButton0.background = null
        }
    }

    /**
     * ???????????????
     */
    private fun timerSet() {
        timer.base = SystemClock.elapsedRealtime()
        val hour = ((SystemClock.elapsedRealtime() - timer.base) / 1000 / 60)
        timer.format = "0$hour:%s"
    }

    /**
     * ??????????????????
     */
    private fun manuallyStartTheService() {
        imgSwitch.playAnimation()
        MmkvUtils.set(Constant.SLIDING, true)
        startVpn()
    }

    /**
     * ?????????????????????
     */
    private fun initConnectionServer() {
        changeState(BaseService.State.Idle, animate = false)
        connection.connect(this, this)
        DataStore.publicStore.registerChangeListener(this)
        ProfileManager.getProfile(DataStore.profileId).let {
            settingsIcon(bestServiceData)
            if (it != null) {
                ProfileManager.updateProfile(setServerData(it, bestServiceData))
            } else {
                val profile = Profile()
                ProfileManager.createProfile(setServerData(profile, bestServiceData))
            }
        }
        DataStore.profileId = 1L
    }


    /**
     * ???????????????
     */
    private fun updateServer(safeLocation: ProfileBean.SafeLocation) {
        settingsIcon(safeLocation)
        bestServiceData = safeLocation
        ProfileManager.getProfile(DataStore.profileId).let {
            if (it != null) {
                setServerData(it, safeLocation)
                ProfileManager.updateProfile(it)
            } else {
                ProfileManager.createProfile(Profile())
            }
        }
        DataStore.profileId = 1L
        isFrontDesk = true
        manuallyStartTheService()
    }

    /**
     * ?????????????????????
     */
    private fun setServerData(profile: Profile, bestData: ProfileBean.SafeLocation): Profile {
        profile.name = bestData.bb_country + "-" + bestData.bb_city
        profile.host = bestData.bb_ip.toString()
        profile.remotePort = bestData.bb_port!!
        profile.password = bestData.bb_pwd!!
        profile.method = bestData.bb_method!!
        return profile
    }

    /**
     * ????????????
     */
    private fun settingsIcon(profileBean: ProfileBean.SafeLocation) {
        if (profileBean.bestServer == true) {
            tvLocation.text = Constant.FASTER_SERVER
            imgCountry.setImageResource(FlagConversion(Constant.FASTER_SERVER))
        } else {
            tvLocation.text = profileBean.bb_country + "-" + profileBean.bb_city
            imgCountry.setImageResource(FlagConversion(profileBean.bb_country))
        }
    }

    /**
     * ??????VPN
     */
    private fun startVpn() {
        if (state.canStop) {
            disConnectToTheVpnService()
        } else {
            connectToTheVpnService()
        }
    }

    /**
     * ??????vpn??????
     */
    private fun disConnectToTheVpnService() {
        Core.stopService()
        jumpToTheResultPage(false)
    }

    /**
     * ??????vpn??????
     */
    private fun connectToTheVpnService() {
        if (NetworkPing.isNetworkAvailable(this)) {
            connect.launch(null)
        } else {
            Looper.prepare()
            imgSwitch.pauseAnimation()
            ToastUtils.toast("The current device has no network")
            Looper.loop()
        }
    }

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
        setSwitchStatus()
        stateListener?.invoke(state)
    }

    /**
     * ????????????????????????
     */
    private fun setConnectionStatusText(state: String) {
        when (state) {
            "Connecting" -> {
                txtConnect.text = "Connecting..."
            }
            "Connected" -> {
                // ????????????
                connectionSucceeded()
                txtConnect.text = "Connected"
            }
            "Stopping" -> {
                txtConnect.text = "Stopping"
            }
            "Stopped" -> {
                connectionStop()
                txtConnect.text = "Connect"
            }
            else -> {
                txtConnect.text = "Configuring"
            }
        }

    }

    /**
     * ????????????
     */
    private fun connectionSucceeded() {
        if (rangeTime != 0f) {
            timer.base = (timer.base + (SystemClock.elapsedRealtime() - rangeTime)).toLong()
        } else {
            timer.base = SystemClock.elapsedRealtime()
        }
        timer.start()
        jumpToTheResultPage(true)
    }

    /**
     * ????????????
     */
    private fun connectionStop() {
        timer.stop()
        //???????????????
        rangeTime = 0f
        timer.base = SystemClock.elapsedRealtime()
    }

    /**
     * ???????????????
     */
    private fun jumpToTheResultPage(flag: Boolean) {
        imgSwitch.pauseAnimation()
        MmkvUtils.set(Constant.SLIDING, false)
        if (!isFrontDesk) {
            setSwitchStatus()
            return
        }
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

    /**
     * ??????????????????
     */
    private fun plugInAdvertisementCallback() {
        mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdClicked() {
                // Called when a click is recorded for an ad.
                KLog.d("TAG", "Ad was clicked.")
            }

            override fun onAdDismissedFullScreenContent() {
                // Called when ad is dismissed.
                isFrontDesk = true
                startVpn()
                mInterstitialAd = null
                loadScreenAdvertisement(adRequest)
            }

            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                // Called when ad fails to show.
                KLog.e("TAG", "Ad failed to show fullscreen content.")
                mInterstitialAd = null
                loadScreenAdvertisement(adRequest)
            }

            override fun onAdImpression() {
                // Called when an impression is recorded for an ad.
                KLog.d("TAG", "Ad recorded an impression.")
            }

            override fun onAdShowedFullScreenContent() {
                // Called when ad is shown.
                KLog.d("TAG", "Ad showed fullscreen content.")
            }
        }
    }

    override fun onStart() {
        super.onStart()
        connection.bandwidthTimeout = 500
        isFrontDesk = true
    }

    override fun onResume() {
        super.onResume()
        isFrontDesk = true
    }

    override fun onStop() {
        connection.bandwidthTimeout = 0
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        DataStore.publicStore.unregisterChangeListener(this)
        connection.disconnect(this)
        currentNativeAd?.destroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            ClickUtils.exitBy2Click(2000, this)
        }
        return true
    }

    override fun onPause() {
        super.onPause()
        isFrontDesk = false
    }

    override fun onRetry() {
        ToastUtils.toast(R.string.exit_procedure)
    }

    override fun onExit() {
        XUtil.get().exitApp()
    }
}