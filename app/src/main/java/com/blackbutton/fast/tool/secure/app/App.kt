/*******************************************************************************
 *                                                                             *
 *  Copyright (C) 2017 by Max Lv <max.c.lv@gmail.com>                          *
 *  Copyright (C) 2017 by Mygod Studio <contact-shadowsocks-android@mygod.be>  *
 *                                                                             *
 *  This program is free software: you can redistribute it and/or modify       *
 *  it under the terms of the GNU General Public License as published by       *
 *  the Free Software Foundation, either version 3 of the License, or          *
 *  (at your option) any later version.                                        *
 *                                                                             *
 *  This program is distributed in the hope that it will be useful,            *
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              *
 *  GNU General Public License for more details.                               *
 *                                                                             *
 *  You should have received a copy of the GNU General Public License          *
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.       *
 *                                                                             *
 *******************************************************************************/

package com.blackbutton.fast.tool.secure.app

import android.app.Application
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import com.blackbutton.fast.tool.secure.MainActivity
import com.blackbutton.fast.tool.secure.utils.ResUtils
import com.blackbutton.fast.tool.secure.utils.Utils
import com.example.testdemo.utils.KLog
import com.github.shadowsocks.BuildConfig
import com.github.shadowsocks.Core
import com.google.android.gms.ads.MobileAds
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize

import com.jeremyliao.liveeventbus.LiveEventBus
import com.tencent.mmkv.MMKV
import com.xuexiang.xutil.XUtil

class App : Application(), androidx.work.Configuration.Provider by Core {
    override fun onCreate() {
        super.onCreate()
        MMKV.initialize(this)
        if(Utils.isMainProcess(this)){
            MobileAds.initialize(this) {}
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
            Firebase.initialize(this)
            ResUtils.init(this)
            XUtil.init(this)
            LiveEventBus
                .config().supportBroadcast(this)
                .lifecycleObserverAlwaysActive(true)
            //是否开启打印日志
            KLog.init(BuildConfig.DEBUG)
        }
        Core.init(this, MainActivity::class)

    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Core.updateNotificationChannels()
    }

}
