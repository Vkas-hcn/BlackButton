package com.blackbutton.fast.tool.secure.bean

class ProfileBean {
    val safeLocation: MutableList<SafeLocation>? = null
    class SafeLocation {
        var ufo_pwd: String? = null
        var ufo_method: String? = null
        var ufo_port: Int? = null
        var ufo_country: String? = null
        var ufo_city: String? = null
        var ufo_ip: String? = null
        //是否选中
        var cheek_state: Boolean? = false
        //是否是最佳服务器
        var bestServer:Boolean? = false
    }
}