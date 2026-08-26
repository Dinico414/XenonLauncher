package com.xenonware.launcher

import android.content.Intent
import com.xenon.mylibrary.activity.BaseWelcomeActivity
import com.xenonware.launcher.data.SharedPreferenceManager

class WelcomeActivity : BaseWelcomeActivity() {

    private val sharedPreferenceManager by lazy { SharedPreferenceManager(this) }

    override fun getTitleText(): String = getString(R.string.welcome_title)

    override fun getDescriptionText(): String = getString(R.string.welcome_description)

    override fun onWelcomeFinished() {
        sharedPreferenceManager.isFirstLaunch = false
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
