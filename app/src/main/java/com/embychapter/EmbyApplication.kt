package com.embychapter

import android.app.Application

class EmbyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 全局初始化可在此进行
    }
}
