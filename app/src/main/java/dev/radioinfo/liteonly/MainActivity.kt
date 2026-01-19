package ru.vellit.gsm2g

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ru.vellit.gsm2g.databinding.ActivityMainBinding
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val monitorHandler = Handler(Looper.getMainLooper())
    private var lastPromptPackage: String? = null
    private val promptTimestamps = mutableMapOf<String, Long>()
    private var notificationsRequested = false
    private val whitelistPrefs by lazy {
        getSharedPreferences("whitelist_prefs", Context.MODE_PRIVATE)
    }

    private val channelId = "radioinfo_prompt"
    private val actionReturn = "dev.radioinfo.liteonly.ACTION_RETURN"
    private val actionStay = "dev.radioinfo.liteonly.ACTION_STAY"
    private val promptCooldownMs = 5 * 60_000L

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        updateNotificationPermissionUi(granted)
    }

    private val defaultApps by lazy {
        listOf(
            WhitelistEntry("vk", "ВКонтакте", "com.vkontakte.android"),
            WhitelistEntry("ok", "Одноклассники", "ru.odnoklassniki"),
            WhitelistEntry("mailru", "Почта Mail.ru", "ru.mail.mailapp"),
            WhitelistEntry("max", "MAX (VK мессенджер)", "com.vk.im"),
            WhitelistEntry("dzen", "Дзен", "ru.yandex.zen"),
            WhitelistEntry("kinopoisk", "Кинопоиск", "ru.kinopoisk.android"),
            WhitelistEntry("vkvideo", "VK Видео", "com.vk.video"),
            WhitelistEntry("rutube", "RUTUBE", "ru.rutube.app"),
            WhitelistEntry("premier", "PREMIER", "ru.amediateka.android"),
            WhitelistEntry("okko", "Okko", "ru.more.play"),
            WhitelistEntry("ivi", "IVI", "ru.ivi.client"),
            WhitelistEntry("yappy", "Yappy", "ru.yappy.app"),
            WhitelistEntry("yandex_search", "Яндекс", "ru.yandex.searchplugin"),
            WhitelistEntry("yandex_go", "Яндекс Go", "ru.yandex.taxi"),
            WhitelistEntry("yandex_eda", "Яндекс Еда", "ru.yandex.eda"),
            WhitelistEntry("yandex_lavka", "Яндекс Лавка", "ru.yandex.express"),
            WhitelistEntry("yandex_music", "Яндекс Музыка", "ru.yandex.music"),
            WhitelistEntry("wildberries", "Wildberries", "ru.wildberries.market"),
            WhitelistEntry("ozon", "Ozon", "ru.ozon.app.android"),
            WhitelistEntry("avito", "Авито", "com.avito.android"),
            WhitelistEntry("vkusvill", "Вкусвилл", "ru.vkusvill.app"),
            WhitelistEntry("ashan", "Ашан", "ru.auchan"),
            WhitelistEntry("spar", "Спар", "ru.spar"),
            WhitelistEntry("metro", "Metro", "ru.metro.cashcarry"),
            WhitelistEntry("petrovich", "Петрович", "ru.petrovich"),
            WhitelistEntry("interfax", "Interfax", "ru.interfax"),
            WhitelistEntry("magnit", "Магнит", "ru.magnit.twa"),
            WhitelistEntry("mts", "Мой МТС", "ru.mts.mymts"),
            WhitelistEntry("beeline", "Мой Билайн", "ru.beeline.mybeeline"),
            WhitelistEntry("megafon", "МегаФон", "ru.megafon.mlk"),
            WhitelistEntry("tele2", "Мой Tele2", "ru.tele2.mytele2"),
            WhitelistEntry("rtk", "Мой Ростелеком", "ru.rt.service"),
            WhitelistEntry("sbermobile", "Сбермобайл", "ru.sberbankmobile"),
            WhitelistEntry("tmobile", "T-Mobile", "com.tmobile.pr.mytmobile"),
            WhitelistEntry("alfabank", "Альфа-Банк", "ru.alfabank.mobile.android"),
            WhitelistEntry("mirpay", "Mir Pay / СБП", "ru.nspk.sbp"),
            WhitelistEntry("gis2", "2ГИС", "ru.dublgis.dgismobile"),
            WhitelistEntry("rzd", "РЖД Пассажирам", "ru.fpc.mobile"),
            WhitelistEntry("tutu", "Tutu.ru", "ru.tutu.android"),
            WhitelistEntry("aeroflot", "Аэрофлот", "ru.aeroflot.app"),
            WhitelistEntry("pobeda", "Победа", "com.pobeda.passenger"),
            WhitelistEntry("maxim", "Такси Максим", "com.taxsee.taxsee"),
            WhitelistEntry("gosuslugi", "Госуслуги", "ru.gosuslugi.lk"),
            WhitelistEntry("russianpost", "Почта России", "ru.russianpost.client"),
            WhitelistEntry("znak", "Честный ЗНАК", "ru.crpt.znak"),
            WhitelistEntry("domklik", "Домклик", "ru.sberbank.dcl"),
            WhitelistEntry("hh", "HeadHunter", "ru.hh.android"),
            WhitelistEntry("gismeteo", "Gismeteo", "com.gismeteo.client"),
            WhitelistEntry("radioplayer", "Радиоплеер", "com.radioplayer")
        )
    }

    private val radioInfoIntents = listOf(
        Intent(Intent.ACTION_VIEW).apply {
            component = ComponentName(
                "com.android.phone",
                "com.android.phone.settings.RadioInfo"
            )
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        },
        Intent(Intent.ACTION_VIEW).apply {
            component = ComponentName(
                "com.android.settings",
                "com.android.settings.RadioInfo"
            )
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.descriptionText.text = HtmlCompat.fromHtml(
            getString(R.string.app_description, defaultModeSuggestion()),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        binding.retryButton.setOnClickListener { openRadioInfo(manual = true) }
        binding.dialerButton.setOnClickListener { openDialerCode() }
        binding.settingsButton.setOnClickListener { openSystemSettings() }
        binding.usagePermissionButton.setOnClickListener { openUsageAccessSettings() }
        binding.notificationsPermissionButton.setOnClickListener { requestNotificationPermission() }
        binding.openRadioInfoButton.setOnClickListener { openRadioInfo(manual = true) }
        binding.privacyLink.setOnClickListener { openPrivacyPolicy() }

        binding.statusText.text = getString(R.string.status_idle)

        setupWhitelist()
        createNotificationChannel()
        maybePromptInitialPermissions()
        ensureNotificationPermission(autoRequest = true)
        startMonitoringForeground()
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        ensureNotificationPermission()
    }

    override fun onDestroy() {
        stopMonitoringForeground()
        super.onDestroy()
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            actionReturn -> {
                intent.getStringExtra("package")?.let { pkg ->
                    promptTimestamps.remove(pkg)
                    lastPromptPackage = null
                }
                cancelNotification()
                openRadioInfo(manual = true)
            }
            actionStay -> cancelNotification()
        }
    }

    private fun openRadioInfo(manual: Boolean) {
        for (intent in radioInfoIntents) {
            try {
                startActivity(intent)
                binding.statusText.text = getString(R.string.status_opening)
                binding.fallbackContainer.isVisible = false
                return
            } catch (_: ActivityNotFoundException) {
                // Ignore and try next target.
            } catch (_: SecurityException) {
                // Manufacturer may block access; try next target.
            }
        }

        binding.statusText.text = getString(R.string.status_unavailable)
        binding.fallbackContainer.isVisible = true

        if (manual) {
            Toast.makeText(this, R.string.status_unavailable_toast, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openDialerCode() {
        val dialCode = Uri.encode("*#*#4636#*#*")
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$dialCode"))

        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.dialer_not_found, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openSystemSettings() {
        val intents = listOf(
            Intent(Settings.ACTION_WIRELESS_SETTINGS),
            Intent(Settings.ACTION_SETTINGS)
        )

        for (intent in intents) {
            try {
                startActivity(intent)
                return
            } catch (_: ActivityNotFoundException) {
                // Try the next fallback.
            }
        }

        Toast.makeText(this, R.string.settings_not_found, Toast.LENGTH_SHORT).show()
    }

    private fun setupWhitelist() {
        binding.openWhitelistButton.setOnClickListener {
            if (!hasUsageAccess()) {
                showUsagePermissionDialog {
                    if (hasUsageAccess()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission()) {
                            showNotificationsPermissionDialog {
                                if (hasNotificationPermission()) showWhitelistDialog()
                            }
                        } else {
                            showWhitelistDialog()
                        }
                    }
                }
                return@setOnClickListener
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission()) {
                showNotificationsPermissionDialog {
                    if (hasNotificationPermission()) showWhitelistDialog()
                }
            } else {
                showWhitelistDialog()
            }
        }
    }

    private fun loadWhitelist(): List<WhitelistEntry> {
        val initialized = whitelistPrefs.getBoolean("whitelist_initialized", false)
        val raw = whitelistPrefs.getString("entries", null)
        if (!initialized || raw.isNullOrEmpty()) {
            saveWhitelist(defaultApps)
            return defaultApps
        }

        val array = JSONArray(raw)
        val result = mutableListOf<WhitelistEntry>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            result.add(
                WhitelistEntry(
                    id = obj.optString("id"),
                    label = obj.optString("label"),
                    packageName = obj.optString("packageName")
                )
            )
        }
        return result
    }

    private fun saveWhitelist(list: List<WhitelistEntry>) {
        val array = JSONArray()
        list.forEach { entry ->
            val obj = JSONObject().apply {
                put("id", entry.id)
                put("label", entry.label)
                put("packageName", entry.packageName)
            }
            array.put(obj)
        }
        whitelistPrefs.edit {
            putString("entries", array.toString())
            putBoolean("whitelist_initialized", true)
        }
    }

    private fun deleteEntry(entry: WhitelistEntry, onUpdated: ((List<WhitelistEntry>) -> Unit)? = null) {
        val updated = loadWhitelist().filterNot { it.id == entry.id }
        removeIgnored(entry.packageName)
        saveWhitelist(updated)
        onUpdated?.invoke(updated)
    }

    private fun resetWhitelist(onUpdated: ((List<WhitelistEntry>) -> Unit)? = null) {
        saveWhitelist(defaultApps)
        saveIgnoredPackages(emptySet())
        onUpdated?.invoke(defaultApps)
    }

    private fun confirmResetWhitelist(onUpdated: (List<WhitelistEntry>) -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(R.string.reset_confirm_title)
            .setMessage(R.string.reset_confirm_message)
            .setPositiveButton(R.string.reset_confirm_action) { _, _ -> resetWhitelist(onUpdated) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun loadIgnoredPackages(): MutableSet<String> {
        val raw = whitelistPrefs.getString("ignored_packages", "[]") ?: "[]"
        val array = JSONArray(raw)
        val set = mutableSetOf<String>()
        for (i in 0 until array.length()) {
            set.add(array.optString(i))
        }
        return set
    }

    private fun saveIgnoredPackages(set: Set<String>) {
        val array = JSONArray()
        set.forEach { array.put(it) }
        whitelistPrefs.edit { putString("ignored_packages", array.toString()) }
    }

    private fun removeIgnored(packageName: String) {
        val set = loadIgnoredPackages()
        if (set.remove(packageName)) {
            saveIgnoredPackages(set)
        }
    }

    private fun showWhitelistDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_whitelist_manage, null)
        val recycler = dialogView.findViewById<RecyclerView>(R.id.whitelistDialogRecycler)
        val empty = dialogView.findViewById<android.widget.TextView>(R.id.whitelistDialogEmpty)
        val addButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.whitelistDialogAddButton)
        val resetButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.whitelistDialogResetButton)

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.whitelist_title)
            .setView(dialogView)
            .setNegativeButton(R.string.close, null)
            .create()

        lateinit var adapter: WhitelistAdapter
        adapter = WhitelistAdapter(
            onEdit = { entry ->
                showAppPicker(entry) { updated ->
                    adapter.submit(updated)
                    empty.isVisible = updated.isEmpty()
                }
            },
            onDelete = { entry ->
                deleteEntry(entry) { updated ->
                    adapter.submit(updated)
                    empty.isVisible = updated.isEmpty()
                }
            }
        )

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        fun refresh() {
            val list = loadWhitelist()
            adapter.submit(list)
            empty.isVisible = list.isEmpty()
        }

        addButton.setOnClickListener {
            showAppPicker(null) { updated ->
                adapter.submit(updated)
                empty.isVisible = updated.isEmpty()
            }
        }

        resetButton.setOnClickListener {
            confirmResetWhitelist { updated ->
                adapter.submit(updated)
                empty.isVisible = updated.isEmpty()
            }
        }

        refresh()
        dialog.show()
    }

    private fun isIgnored(packageName: String): Boolean {
        return loadIgnoredPackages().contains(packageName)
    }

    private fun showAppPicker(existing: WhitelistEntry?, onUpdated: ((List<WhitelistEntry>) -> Unit)? = null) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_app_picker, null)
        val searchInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.appSearchInput)
        val recycler = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.appPickerRecycler)

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.add_entry_title_picker)
            .setView(dialogView)
            .setNegativeButton(R.string.cancel, null)
            .create()

        var allApps: List<AppInfo> = emptyList()
        val adapter = AppPickerAdapter { app ->
            val current = loadWhitelist().toMutableList()
            val updatedEntry = existing?.copy(
                label = app.label,
                packageName = app.packageName
            ) ?: WhitelistEntry(
                id = UUID.randomUUID().toString(),
                label = app.label,
                packageName = app.packageName
            )

            val newList = current
                .filterNot { it.id == updatedEntry.id || it.packageName == app.packageName }
                .plus(updatedEntry)
            removeIgnored(app.packageName)
            saveWhitelist(newList)
            onUpdated?.invoke(newList)
            dialog.dismiss()
        }

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter
        adapter.submit(allApps)

        searchInput.addTextChangedListener { text ->
            val query = text?.toString()?.trim()?.lowercase().orEmpty()
            val filtered = if (query.isEmpty()) {
                allApps
            } else {
                allApps.filter { it.label.lowercase().contains(query) || it.packageName.lowercase().contains(query) }
            }
            adapter.submit(filtered)
        }

        dialog.show()

        thread {
            val pm = packageManager
            val launcherIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PackageManager.MATCH_ALL
            } else {
                0
            }
            val loaded = pm.queryIntentActivities(launcherIntent, flags)
                .map { info ->
                    AppInfo(
                        label = info.loadLabel(pm)?.toString().takeIf { !it.isNullOrBlank() } ?: info.activityInfo.packageName,
                        packageName = info.activityInfo.packageName,
                        icon = info.loadIcon(pm)
                    )
                }
                .distinctBy { it.packageName }
                .sortedBy { it.label.lowercase() }

            runOnUiThread {
                if (loaded.isEmpty()) {
                    Toast.makeText(this, R.string.settings_not_found, Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    return@runOnUiThread
                }
                allApps = loaded
                adapter.submit(loaded)
            }
        }

    }

    private fun hasUsageAccess(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = appOps.checkOpNoThrow(
            "android:get_usage_stats",
            android.os.Process.myUid(),
            packageName
        )
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    private fun openUsageAccessSettings() {
        val directIntent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        }
        try {
            startActivity(directIntent)
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
    }

    private fun maybePromptInitialPermissions() {
        val prompted = whitelistPrefs.getBoolean("initial_permissions_prompted", false)
        if (prompted) return
        whitelistPrefs.edit { putBoolean("initial_permissions_prompted", true) }

        val needsUsage = !hasUsageAccess()
        val needsNotifications = !hasNotificationPermission()

        if (needsUsage) {
            showUsagePermissionDialog {
                if (!hasNotificationPermission()) {
                    showNotificationsPermissionDialog()
                }
            }
        } else if (needsNotifications) {
            showNotificationsPermissionDialog()
        }
    }

    private fun showUsagePermissionDialog(onDismiss: (() -> Unit)? = null) {
        AlertDialog.Builder(this)
            .setTitle(R.string.usage_permission_title)
            .setMessage(R.string.usage_permission_needed)
            .setPositiveButton(R.string.grant_usage_permission) { _, _ ->
                openUsageAccessSettings()
            }
            .setNegativeButton(R.string.cancel, null)
            .setOnDismissListener { onDismiss?.invoke() }
            .show()
    }

    private fun showNotificationsPermissionDialog(onDismiss: (() -> Unit)? = null) {
        AlertDialog.Builder(this)
            .setTitle(R.string.notifications_permission_title)
            .setMessage(R.string.notifications_permission_needed)
            .setPositiveButton(R.string.grant_notifications) { _, _ ->
                requestNotificationPermission()
            }
            .setNegativeButton(R.string.cancel, null)
            .setOnDismissListener { onDismiss?.invoke() }
            .show()
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            NotificationManagerCompat.from(this).areNotificationsEnabled() ||
                androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun ensureNotificationPermission(autoRequest: Boolean = false) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val enabled = hasNotificationPermission()
            binding.notificationsPermissionGroup.isVisible = !enabled

            if (!enabled && autoRequest && !notificationsRequested) {
                notificationsRequested = true
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            binding.notificationsPermissionGroup.isVisible = false
        }
    }

    private fun updateNotificationPermissionUi(granted: Boolean) {
        val enabled = granted || hasNotificationPermission()
        binding.notificationsPermissionGroup.isVisible =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !enabled
    }

    private fun defaultModeSuggestion(): String {
        val manufacturer = Build.MANUFACTURER?.lowercase().orEmpty()
        val brand = Build.BRAND?.lowercase().orEmpty()
        val model = Build.MODEL?.lowercase().orEmpty()

        fun matches(vararg keys: String): Boolean {
            return keys.any { key ->
                manufacturer.contains(key) || brand.contains(key)
            }
        }

        val nrAuto = "NR/LTE/GSM/WCDMA (авто)"
        val lteAuto = "LTE/GSM/WCDMA (авто)"
        val globalMode = "NR/LTE/CDMA/EvDo/GSM/WCDMA (global)"

        return when {
            matches("motorola", "moto") -> nrAuto
            matches("oneplus", "oppo", "realme", "nothing", "vivo", "iqoo", "honor", "huawei", "sony") -> nrAuto
            matches("pixel", "google") -> {
                if (model.contains("4a") || model.contains("3") || model.contains("4 ")) lteAuto else nrAuto
            }
            matches("samsung") -> {
                when {
                    model.contains("5g") || model.contains("s2") || model.contains("s1") -> nrAuto
                    brand.contains("usa") || brand.contains("vzw") -> globalMode
                    else -> lteAuto
                }
            }
            matches("xiaomi", "redmi", "poco", "mi") -> if (model.contains("5g")) nrAuto else lteAuto
            matches("tecno", "infinix") -> if (model.contains("5g")) nrAuto else lteAuto
            matches("oppo") -> nrAuto
            else -> nrAuto
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            openNotificationSettings()
        }
    }

    private fun openNotificationSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        }

        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.settings_not_found, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openPrivacyPolicy() {
        val url = getString(R.string.privacy_policy_url)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, url, Toast.LENGTH_SHORT).show()
        }
    }

    private val monitorRunnable = object : Runnable {
        override fun run() {
            monitorForeground()
            monitorHandler.postDelayed(this, 4000)
        }
    }

    private fun startMonitoringForeground() {
        monitorHandler.removeCallbacks(monitorRunnable)
        monitorHandler.post(monitorRunnable)
    }

    private fun stopMonitoringForeground() {
        monitorHandler.removeCallbacks(monitorRunnable)
    }

    private fun monitorForeground() {
        if (!hasUsageAccess()) {
            binding.usagePermissionGroup.isVisible = true
            cancelNotification()
            return
        } else {
            binding.usagePermissionGroup.isVisible = false
        }

        val currentPackage = getTopPackage() ?: return
        if (isIgnored(currentPackage)) {
            cancelNotification()
            return
        }
        val entries = loadWhitelist()
        val match = entries.firstOrNull { entry ->
            entry.packageName == currentPackage
        }

        if (match != null && lastPromptPackage != currentPackage) {
            val now = System.currentTimeMillis()
            val lastTime = promptTimestamps[currentPackage] ?: 0L
            if (now - lastTime < promptCooldownMs) {
                return
            }
            promptTimestamps[currentPackage] = now
            lastPromptPackage = currentPackage
            showReturnNotification(match)
        } else if (match == null) {
            lastPromptPackage = null
            cancelNotification()
        }
    }

    private fun getTopPackage(): String? {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val start = end - 10_000
        val events = usm.queryEvents(start, end)
        var lastPackage: String? = null
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastPackage = event.packageName
            }
        }
        return lastPackage
    }

    private fun showReturnNotification(entry: WhitelistEntry) {
        val nm = NotificationManagerCompat.from(this)
        val returnIntent = PendingIntent.getActivity(
            this,
            1,
            Intent(this, MainActivity::class.java).setAction(actionReturn).apply {
                putExtra("package", entry.packageName)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag()
        )

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag()
        )

        val stayIntent = PendingIntent.getBroadcast(
            this,
            2,
            Intent(this, NotificationActionReceiver::class.java).apply {
                action = actionStay
                putExtra("package", entry.packageName)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag()
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_settings)
            .setContentTitle(getString(R.string.return_prompt_title))
            .setContentText(getString(R.string.return_prompt_message, entry.label))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .addAction(0, getString(R.string.return_to_full), returnIntent)
            .addAction(0, getString(R.string.stay), stayIntent)
            .build()

        nm.notify(1010, notification)
    }

    private fun cancelNotification() {
        NotificationManagerCompat.from(this).cancel(1010)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "RadioInfo reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun mutableFlag(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
    }

    data class WhitelistEntry(
        val id: String,
        val label: String,
        val packageName: String
    )

    data class AppInfo(
        val label: String,
        val packageName: String,
        val icon: android.graphics.drawable.Drawable
    )

    private class WhitelistAdapter(
        private val onEdit: (WhitelistEntry) -> Unit,
        private val onDelete: (WhitelistEntry) -> Unit
    ) : RecyclerView.Adapter<WhitelistAdapter.ViewHolder>() {

        private val items = mutableListOf<WhitelistEntry>()

        fun submit(newItems: List<WhitelistEntry>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_whitelist_entry, parent, false)
            return ViewHolder(view, onEdit, onDelete)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        class ViewHolder(
            itemView: android.view.View,
            private val onEdit: (WhitelistEntry) -> Unit,
            private val onDelete: (WhitelistEntry) -> Unit
        ) : RecyclerView.ViewHolder(itemView) {

            private val title = itemView.findViewById<android.widget.TextView>(R.id.entryTitle)
            private val subtitle = itemView.findViewById<android.widget.TextView>(R.id.entrySubtitle)
            private val edit = itemView.findViewById<com.google.android.material.button.MaterialButton>(R.id.entryEdit)
            private val delete = itemView.findViewById<com.google.android.material.button.MaterialButton>(R.id.entryDelete)

            fun bind(entry: WhitelistEntry) {
                title.text = entry.label
                val context = itemView.context
                subtitle.text = context.getString(R.string.entry_app_package, entry.packageName)

                edit.setOnClickListener { onEdit(entry) }
                delete.setOnClickListener { onDelete(entry) }
            }
        }
    }

    private class AppPickerAdapter(
        private val onPick: (AppInfo) -> Unit
    ) : RecyclerView.Adapter<AppPickerAdapter.ViewHolder>() {

        private val items = mutableListOf<AppInfo>()

        fun submit(newItems: List<AppInfo>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_app_picker, parent, false)
            return ViewHolder(view, onPick)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        class ViewHolder(
            itemView: android.view.View,
            private val onPick: (AppInfo) -> Unit
        ) : RecyclerView.ViewHolder(itemView) {

            private val icon = itemView.findViewById<android.widget.ImageView>(R.id.appIcon)
            private val label = itemView.findViewById<android.widget.TextView>(R.id.appLabel)
            private val pkg = itemView.findViewById<android.widget.TextView>(R.id.appPackage)

            fun bind(item: AppInfo) {
                icon.setImageDrawable(item.icon)
                label.text = item.label
                pkg.text = item.packageName
                itemView.setOnClickListener { onPick(item) }
            }
        }
    }
}
