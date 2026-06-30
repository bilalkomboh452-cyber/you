
package com.waglo.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.waglo.app.utils.PrefsHelper

class WagloApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Apply saved dark mode preference on app start
        val mode = if (PrefsHelper.isDarkMode(this))
            AppCompatDelegate.MODE_NIGHT_YES
        else
            AppCompatDelegate.MODE_NIGHT_NO
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}
