package com.github.shadowsocks.bean

class AroundFlowBean {
    //绕流方式
    var around_flow_mode: String? = null
    //黑名单
    var black_list:  List<String> = ArrayList()
    //白名单
    var white_list: List<String> = ArrayList()
}