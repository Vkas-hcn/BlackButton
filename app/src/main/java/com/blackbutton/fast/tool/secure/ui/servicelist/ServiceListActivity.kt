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
import com.blackbutton.fast.tool.secure.utils.Utils.addTheBestRoute
import com.github.shadowsocks.R
import com.google.gson.reflect.TypeToken
import com.jeremyliao.liveeventbus.LiveEventBus

class ServiceListActivity : AppCompatActivity() {
    private lateinit var frameLayoutTitle: FrameLayout
    private lateinit var blackTitle: ImageView
    private lateinit var imgTitle:ImageView
    private lateinit var tvTitle:TextView
    private lateinit var ivRight:ImageView
    private lateinit var serviceListAdapter: ServiceListAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var profileBean: ProfileBean
    private lateinit var safeLocation: MutableList<ProfileBean.SafeLocation>
    private lateinit var checkSafeLocation: ProfileBean.SafeLocation

    private lateinit var tvConnect:TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_service_list)
        initRecyclerView()
    }

    private fun initRecyclerView() {
        frameLayoutTitle =findViewById(R.id.bar_service_list)
        blackTitle = findViewById(R.id.ivBack)
        imgTitle =findViewById(R.id.img_title)
        tvTitle = findViewById(R.id.tv_title)
        ivRight = findViewById(R.id.ivRight)
        tvConnect = findViewById(R.id.tv_connect)
        recyclerView = findViewById(R.id.rv_service_list)
        imgTitle.visibility = View.GONE
        tvTitle.visibility = View.VISIBLE
        ivRight.visibility = View.GONE
        blackTitle.setImageResource(R.mipmap.ic_black)
        safeLocation = ArrayList()
        profileBean = ProfileBean()
        checkSafeLocation = ProfileBean.SafeLocation()
        profileBean = getMenuJsonData("serviceJson.json")
        safeLocation = profileBean.safeLocation!!
        safeLocation.add(0,addTheBestRoute(safeLocation))
        serviceListAdapter = ServiceListAdapter(safeLocation)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = serviceListAdapter
        serviceListAdapter.setOnItemClickListener { _, _, position ->
            safeLocation.forEachIndexed { index, _ ->
                safeLocation[index].cheek_state = position == index
                if(safeLocation[index].cheek_state == true){
                    checkSafeLocation= safeLocation[index]
                }
            }
            serviceListAdapter.notifyDataSetChanged()
        }
        blackTitle.setOnClickListener {
            finish()
        }
        tvConnect.setOnClickListener {
            LiveEventBus.get(Constant.SERVER_INFORMATION).post(checkSafeLocation)
            finish()
        }
    }

    /**
     * @return 解析json文件
     */
    private fun getMenuJsonData(jsonName: String): ProfileBean {
        Log.i("TAG", "getMenuJsonData: " + readStringFromAssert(jsonName))
        return JsonUtil.fromJson(
            readStringFromAssert(jsonName),
            object : TypeToken<ProfileBean?>() {}.type
        )
    }
}