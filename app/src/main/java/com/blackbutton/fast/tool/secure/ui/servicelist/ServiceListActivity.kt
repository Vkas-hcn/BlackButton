package com.blackbutton.fast.tool.secure.ui.servicelist

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blackbutton.fast.tool.secure.constant.Constant
import com.blackbutton.fast.tool.secure.utils.JsonUtil
import com.blackbutton.fast.tool.secure.utils.ResourceUtils.readStringFromAssert
import com.blackbutton.fast.tool.secure.bean.ProfileBean
import com.blackbutton.fast.tool.secure.utils.DensityUtils.dp2px
import com.blackbutton.fast.tool.secure.utils.DensityUtils.px2dp
import com.blackbutton.fast.tool.secure.utils.MmkvUtils
import com.blackbutton.fast.tool.secure.utils.NetworkPing
import com.blackbutton.fast.tool.secure.utils.StatusBarUtils
import com.blackbutton.fast.tool.secure.utils.Utils.addTheBestRoute
import com.blackbutton.fast.tool.secure.utils.Utils.isNullOrEmpty
import com.example.testdemo.utils.KLog
import com.github.shadowsocks.R
import com.google.gson.reflect.TypeToken
import com.jeremyliao.liveeventbus.LiveEventBus
import com.xuexiang.xutil.tip.ToastUtils

class ServiceListActivity : AppCompatActivity() {
    private lateinit var frameLayoutTitle: FrameLayout
    private lateinit var blackTitle: ImageView
    private lateinit var imgTitle: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var ivRight: ImageView
    private lateinit var serviceListAdapter: ServiceListAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var profileBean: ProfileBean
    private lateinit var safeLocation: MutableList<ProfileBean.SafeLocation>
    private lateinit var checkSafeLocation: ProfileBean.SafeLocation

    //选中IP
    private var selectIp: String? = null

    //whetherConnected
    private var whetherConnected = false
    private lateinit var tvConnect: TextView
    private var whetherBestServer = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StatusBarUtils.translucent(this)
        StatusBarUtils.setStatusBarLightMode(this)
        setContentView(R.layout.activity_service_list)
        initParam()
        initRecyclerView()
    }

    /**
     * initParam
     */
    private fun initParam() {
        selectIp = intent.getStringExtra(Constant.CURRENT_IP)
        whetherConnected = intent.getBooleanExtra(Constant.WHETHER_CONNECTED, false)
        whetherBestServer = intent.getBooleanExtra(Constant.WHETHER_BEST_SERVER, false)
    }

    private fun initRecyclerView() {
        frameLayoutTitle = findViewById(R.id.bar_service_list)
        frameLayoutTitle.setPadding(
            0,
            px2dp(StatusBarUtils.getStatusBarHeight(this).toFloat()) + 50,
            0,
            0
        )
        blackTitle = findViewById(R.id.ivBack)
        imgTitle = findViewById(R.id.img_title)
        tvTitle = findViewById(R.id.tv_title)
        ivRight = findViewById(R.id.ivRight)
        tvConnect = findViewById(R.id.tv_connect)
        recyclerView = findViewById(R.id.rv_service_list)
        imgTitle.visibility = View.GONE
        tvTitle.visibility = View.VISIBLE
        ivRight.visibility = View.GONE
        blackTitle.setImageResource(R.mipmap.ic_black)

        storageServerData()
        if(!whetherBestServer){
            safeLocation.forEach {
                it.cheek_state = it.bb_ip == selectIp
            }
        }
        serviceListAdapter = ServiceListAdapter(safeLocation)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = serviceListAdapter
        clickEvent()

    }

    /**
     * 点击事件
     */
    private fun clickEvent() {
        serviceListAdapter.setOnItemClickListener { _, _, position ->
            safeLocation.forEachIndexed { index, _ ->
                safeLocation[index].cheek_state = position == index
                if (safeLocation[index].cheek_state == true) {
                    checkSafeLocation = safeLocation[index]
                }
            }
            serviceListAdapter.notifyDataSetChanged()
        }
        blackTitle.setOnClickListener {
            finish()
        }
        tvConnect.setOnClickListener {
            if (whetherConnected) {
                ToastUtils.toast(R.string.disconnect_tips)
            } else {
                LiveEventBus.get(Constant.SERVER_INFORMATION).post(checkSafeLocation)
                finish()
            }

        }
    }

    /**
     * 存储服务器数据
     */
    private fun storageServerData() {
        safeLocation = ArrayList()
        profileBean = ProfileBean()
        checkSafeLocation = ProfileBean.SafeLocation()
        profileBean = if (isNullOrEmpty(MmkvUtils.getStringValue(Constant.PROFILE_DATA))) {
            getMenuJsonData("serviceJson.json")
        } else {
            JsonUtil.fromJson(
                MmkvUtils.getStringValue(Constant.PROFILE_DATA),
                object : TypeToken<ProfileBean?>() {}.type
            )
        }
        safeLocation = profileBean.safeLocation!!
        safeLocation.add(0, addTheBestRoute(NetworkPing.findTheBestIp()))
        checkSafeLocation = safeLocation[0]
    }

    /**
     * @return 解析json文件
     */
    private fun getMenuJsonData(jsonName: String): ProfileBean {
        return JsonUtil.fromJson(
            readStringFromAssert(jsonName),
            object : TypeToken<ProfileBean?>() {}.type
        )
    }
}