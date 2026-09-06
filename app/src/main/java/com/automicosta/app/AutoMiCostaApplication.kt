package com.automicosta.app

import android.app.Application
import com.automicosta.app.data.AutoMiCostaDatabase

class AutoMiCostaApplication : Application() {
    val database by lazy { AutoMiCostaDatabase.get(this) }
}
