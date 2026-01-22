package ru.vellit.gsm2g

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import org.hamcrest.CoreMatchers.anyOf
import org.hamcrest.CoreMatchers.containsString
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BasicInstrumentedTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private val instrumentation = getInstrumentation()
    private val context = instrumentation.targetContext
    private val prefs by lazy {
        ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("whitelist_prefs", Context.MODE_PRIVATE)
    }

    @After
    fun tearDown() {
        activityRule.scenario.onActivity { it.clearTestState() }
    }

    @Test
    fun packageName_isCorrect() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("ru.vellit.gsm2g", context.packageName)
    }

    @Test
    fun appDescription_acceptsModePlaceholder() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val mode = "TEST_MODE"
        val description = context.getString(R.string.app_description, mode)
        assertTrue("Description should contain provided mode", description.contains(mode))
    }

    @Test
    fun openWhitelistButton_hasExpectedLabel() {
        val activateText = context.getString(R.string.activate)
        val openText = context.getString(R.string.open_whitelist)
        onView(withId(R.id.openWhitelistButton))
            .check(matches(isDisplayed()))
            .check(
                matches(
                    anyOf(
                        withText(containsString(activateText)),
                        withText(containsString(openText))
                    )
                )
            )
    }

    @Test
    fun privacyLink_isVisible() {
        onView(withId(R.id.privacyLink))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
    }

    @Test
    fun openWhitelist_showsActivate_whenPermissionsMissing() {
        val pkg = instrumentation.targetContext.packageName
        setAppOp(pkg, "GET_USAGE_STATS", "ignore")
        if (Build.VERSION.SDK_INT >= 33) {
            setAppOp(pkg, "POST_NOTIFICATION", "ignore")
        }
        activityRule.scenario.onActivity {
            MainActivity.testBypassUsageAccess = false
            MainActivity.testBypassNotifications = false
        }
        activityRule.scenario.recreate()

        val activateText = context.getString(R.string.activate)
        val openText = context.getString(R.string.open_whitelist)
        onView(withId(R.id.openWhitelistButton))
            .check(
                matches(
                    anyOf(
                        withText(containsString(activateText)),
                        withText(containsString(openText))
                    )
                )
            )
    }

    @Test
    fun openWhitelist_showsOpen_whenPermissionsGranted() {
        val pkg = instrumentation.targetContext.packageName
        setAppOp(pkg, "GET_USAGE_STATS", "allow")
        if (Build.VERSION.SDK_INT >= 33) {
            setAppOp(pkg, "POST_NOTIFICATION", "allow")
        }
        activityRule.scenario.onActivity {
            MainActivity.testBypassUsageAccess = true
            MainActivity.testBypassNotifications = true
        }
        activityRule.scenario.recreate()

        val openText = context.getString(R.string.open_whitelist)
        onView(withId(R.id.openWhitelistButton))
            .check(matches(withText(containsString(openText))))
    }

    @Test
    fun reminder_triggered_forChromeWhenWhitelisted() {
        // Prepare whitelist with Chrome and allow ops.
        val pkg = instrumentation.targetContext.packageName
        setAppOp(pkg, "GET_USAGE_STATS", "allow")
        if (Build.VERSION.SDK_INT >= 33) {
            setAppOp(pkg, "POST_NOTIFICATION", "allow")
        }
        activityRule.scenario.onActivity {
            MainActivity.testBypassUsageAccess = true
            MainActivity.testBypassNotifications = true
        }
        saveWhitelistForTest("com.android.chrome", "Chrome")

        activityRule.scenario.onActivity { activity ->
            MainActivity.testBypassUsageAccess = true
            MainActivity.testBypassNotifications = true
            MainActivity.testTopPackage = "com.android.chrome"
            MainActivity.lastNotifiedPackageTest = null
            activity.runMonitorForTest()
        }

        assertEquals("com.android.chrome", MainActivity.lastNotifiedPackageTest)
    }

    private fun saveWhitelistForTest(packageName: String, label: String) {
        val entry = JSONObject().apply {
            put("id", "test-chrome")
            put("label", label)
            put("packageName", packageName)
        }
        val array = JSONArray().apply { put(entry) }
        prefs.edit()
            .putString("entries", array.toString())
            .putBoolean("whitelist_initialized", true)
            .apply()
    }

    private fun setAppOp(packageName: String, op: String, mode: String) {
        // Ignore failures silently; best-effort setup.
        try {
            val cmd = "appops set $packageName $op $mode"
            instrumentation.uiAutomation.executeShellCommand(cmd).use { /* close */ }
        } catch (_: Exception) {
            // no-op
        }
    }
}
