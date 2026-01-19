package ru.vellit.gsm2g

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import org.json.JSONArray

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == "dev.radioinfo.liteonly.ACTION_STAY") {
            val targetPackage = intent.getStringExtra("package") ?: return
            rememberIgnored(context, targetPackage)
            NotificationManagerCompat.from(context).cancel(1010)
        }
    }

    private fun rememberIgnored(context: Context, packageName: String) {
        val prefs = context.getSharedPreferences("whitelist_prefs", Context.MODE_PRIVATE)
        val currentRaw = prefs.getString("ignored_packages", "[]") ?: "[]"
        val array = JSONArray(currentRaw)
        val set = mutableSetOf<String>()
        for (i in 0 until array.length()) {
            set.add(array.optString(i))
        }
        if (set.add(packageName)) {
            val newArray = JSONArray()
            set.forEach { newArray.put(it) }
            prefs.edit().putString("ignored_packages", newArray.toString()).apply()
        }
    }

}
