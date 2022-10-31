package com.blackbutton.fast.tool.secure.ui

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.blackbutton.fast.tool.secure.constant.Constant
import com.blackbutton.fast.tool.secure.utils.DensityUtils
import com.blackbutton.fast.tool.secure.utils.StatusBarUtils
import com.blackbutton.fast.tool.secure.widget.HorizontalProgressView
import com.example.testdemo.utils.KLog
import com.first.conn.R
import com.google.android.gms.ads.*
import com.google.android.gms.ads.formats.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

class ResultsActivity: AppCompatActivity() {
    private lateinit var frameLayoutTitle: FrameLayout
    private lateinit var blackTitle: ImageView
    private lateinit var imgTitle: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var ivRight: ImageView
    private var connectionStatus:Boolean =false
    private lateinit var imgConnectInfo: ImageView
    private lateinit var tvConnectInfo: TextView
    private lateinit var ad_frame: FrameLayout
    private lateinit var adImg: ImageView
    var currentNativeAd: NativeAd? = null
    private lateinit var mNativeAds: AdLoader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StatusBarUtils.translucent(this)
        StatusBarUtils.setStatusBarLightMode(this)
        setContentView(R.layout.activity_results)
        supportActionBar?.hide()
        initView()
        initNativeAds()
    }

    private fun initView() {
        frameLayoutTitle =findViewById(R.id.bar_results)
        frameLayoutTitle.setPadding(0,
            DensityUtils.px2dp(StatusBarUtils.getStatusBarHeight(this).toFloat())+50,0,0)
        blackTitle = findViewById(R.id.ivBack)
        imgTitle =findViewById(R.id.img_title)
        tvTitle = findViewById(R.id.tv_title)
        ivRight = findViewById(R.id.ivRight)
        imgConnectInfo= findViewById(R.id.img_connect_info)
        tvConnectInfo= findViewById(R.id.tv_connect_info)
        adImg = findViewById(R.id.ad_img)
        ad_frame = findViewById(R.id.ad_frame)
        imgTitle.visibility = View.GONE
        tvTitle.visibility = View.VISIBLE
        ivRight.visibility = View.GONE
        blackTitle.setImageResource(R.mipmap.ic_black)
        blackTitle.setOnClickListener {
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        connectionStatus = intent.getBooleanExtra(Constant.CONNECTION_STATUS,false)
        if(connectionStatus){
            imgConnectInfo.setImageResource(R.mipmap.ic_connected)
            tvConnectInfo.text = getString(R.string.connected_succeeded)
        }else{
            imgConnectInfo.setImageResource(R.mipmap.ic_disconnected)
            tvConnectInfo.text = getString(R.string.disconnected_succeeded)
        }
    }

    /**
     * 初始化原生广告
     */
    private fun initNativeAds() {
        val videoOptions = VideoOptions.Builder()
            .setStartMuted(true)
            .build()

        val adOptions = NativeAdOptions.Builder()
            .setVideoOptions(videoOptions)
            .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_LEFT)
            .build()
        mNativeAds = AdLoader.Builder(this, "ca-app-pub-3940256099942544/2247696110").forNativeAd { nativeAd ->
            ad_frame.visibility = View.VISIBLE
            adImg.visibility = View.GONE
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
                .inflate(R.layout.layout_results_ad, null) as NativeAdView
            populateNativeAdView(nativeAd, adView)
            ad_frame.removeAllViews()
            ad_frame.addView(adView)
        }.withNativeAdOptions(adOptions).withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                val error =
                    """
           domain: ${loadAdError.domain}, code: ${loadAdError.code}, message: ${loadAdError.message}
          """"
                Toast.makeText(
                    this@ResultsActivity, "Failed to load native ad with error $error",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }).build()
        mNativeAds.loadAd(AdRequest.Builder().build())
    }

    private fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
        // Set the media view.
        adView.mediaView = adView.findViewById(R.id.ad_media)
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        adView.iconView = adView.findViewById(R.id.ad_app_icon)
        adView.advertiserView = adView.findViewById(R.id.ad_advertiser)
        (adView.headlineView as TextView).text = nativeAd.headline
        nativeAd.mediaContent?.let { adView.mediaView?.setMediaContent(it)
            adView.mediaView?.setImageScaleType(ImageView.ScaleType.CENTER_CROP)
            adView.mediaView
        }
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
        adView.setNativeAd(nativeAd)
    }

}