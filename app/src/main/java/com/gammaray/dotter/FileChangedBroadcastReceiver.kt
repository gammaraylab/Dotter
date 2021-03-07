package com.gammaray.dotter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gammaray.dotter.Activity.MainActivity

class FileChangedBroadcastReceiver(private val path: String, private val onChange: () -> Unit) : BroadcastReceiver() {

    companion object {
        const val EXTRA_PATH = "com.gammaray.dotter.path"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val filePath = intent?.extras?.getString(EXTRA_PATH).toString()
        if (filePath == path)
            onChange.invoke()
        MainActivity.log("onChange")
    }
}