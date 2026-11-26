package com.purplevoid.launcher

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.GridView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs

data class AppInfo(val label: String, val pkg: String, val icon: Drawable)

class MainActivity : Activity() {

    private lateinit var bg: LiquidBackground
    private lateinit var drawer: View
    private lateinit var search: EditText
    private lateinit var grid: GridView
    private lateinit var note: EditText
    private lateinit var battery: TextView
    private lateinit var dock: List<ImageView>
    private lateinit var adapter: AppAdapter
    private lateinit var prefs: SharedPreferences
    private lateinit var detector: GestureDetector

    private val allApps = mutableListOf<AppInfo>()
    private val shown = mutableListOf<AppInfo>()
    private val dockPkgs = mutableListOf<String>()
    private var receiverOn = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) = updateBattery(i)
    }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Pantalla completa: el fondo llega hasta detrás de las barras
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
        } else {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }

        setContentView(R.layout.activity_main)
        prefs = getSharedPreferences("void", MODE_PRIVATE)

        bg = findViewById(R.id.bg)
        drawer = findViewById(R.id.drawer)
        search = findViewById(R.id.search)
        grid = findViewById(R.id.grid)
        note = findViewById(R.id.note)
        battery = findViewById(R.id.battery)
        dock = listOf(
            findViewById(R.id.dock0), findViewById(R.id.dock1),
            findViewById(R.id.dock2), findViewById(R.id.dock3)
        )

        findViewById<View>(R.id.content).setOnApplyWindowInsetsListener { v, ins ->
            v.setPadding(0, ins.systemWindowInsetTop, 0, ins.systemWindowInsetBottom)
            ins
        }

        loadApps()
        adapter = AppAdapter()
        grid.adapter = adapter

        grid.setOnItemClickListener { _, _, pos, _ ->
            launch(shown[pos].pkg)
            closeDrawer()
        }
        grid.setOnItemLongClickListener { _, _, pos, _ ->
            pin(shown[pos])
            true
        }

        search.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = filter(s?.toString().orEmpty())
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })

        note.setText(prefs.getString("note", ""))
        note.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                prefs.edit().putString("note", s?.toString().orEmpty()).apply()
            }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })

        findViewById<View>(R.id.homeSearch).setOnClickListener { openDrawer(true) }

        detector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
                if (vy < -1000 && abs(vy) > abs(vx) && drawer.visibility != View.VISIBLE) {
                    openDrawer(false)
                    return true
                }
                return false
            }
        })

        loadDock()
        refreshDock()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        detector.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    override fun onResume() {
        super.onResume()
        bg.start()
        if (!receiverOn) {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val sticky = if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(batteryReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(batteryReceiver, filter)
            }
            receiverOn = true
            sticky?.let { updateBattery(it) }
        }
        // Si se instaló o desinstaló una app, recarga la lista
        if (launcherList().size != allApps.size) {
            loadApps()
            filter(search.text.toString())
            refreshDock()
        }
    }

    override fun onPause() {
        super.onPause()
        bg.stop()
        if (receiverOn) {
            unregisterReceiver(batteryReceiver)
            receiverOn = false
        }
    }

    // ---------- Batería ----------
    private fun updateBattery(i: Intent) {
        val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        if (level >= 0 && scale > 0) {
            battery.text = (if (charging) "⚡ " else "") + (level * 100 / scale) + "%"
        }
    }

    // ---------- Apps ----------
    private fun launcherList() =
        packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0
        ).filter { it.activityInfo.packageName != packageName }

    private fun loadApps() {
        val list = launcherList().map {
            AppInfo(
                it.loadLabel(packageManager).toString(),
                it.activityInfo.packageName,
                it.loadIcon(packageManager)
            )
        }.sortedBy { it.label.lowercase() }
        allApps.clear()
        allApps.addAll(list)
        shown.clear()
        shown.addAll(list)
        if (::adapter.isInitialized) adapter.notifyDataSetChanged()
    }

    private fun filter(q: String) {
        shown.clear()
        shown.addAll(allApps.filter { it.label.contains(q, ignoreCase = true) })
        adapter.notifyDataSetChanged()
    }

    private fun launch(pkg: String) {
        packageManager.getLaunchIntentForPackage(pkg)?.let { startActivity(it) }
    }

    // ---------- Dock ----------
    private fun resolvePkg(i: Intent): String? {
        val p = packageManager.resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName
        return if (p == null || p == "android" || p == packageName) null else p
    }

    private fun loadDock() {
        val saved = prefs.getString("dock", null)
        dockPkgs.clear()
        if (saved == null) {
            listOfNotNull(
                resolvePkg(Intent(Intent.ACTION_DIAL)),
                resolvePkg(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING)),
                resolvePkg(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))),
                resolvePkg(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
            ).distinct().let { dockPkgs.addAll(it) }
            saveDock()
        } else {
            dockPkgs.addAll(saved.split(",").filter { it.isNotBlank() })
        }
    }

    private fun saveDock() {
        prefs.edit().putString("dock", dockPkgs.joinToString(",")).apply()
    }

    private fun refreshDock() {
        for ((i, iv) in dock.withIndex()) {
            val pkg = dockPkgs.getOrNull(i)
            if (pkg == null) {
                iv.setImageDrawable(null)
                iv.setOnClickListener(null)
                continue
            }
            try {
                iv.setImageDrawable(packageManager.getApplicationIcon(pkg))
                iv.setOnClickListener { launch(pkg) }
            } catch (e: PackageManager.NameNotFoundException) {
                iv.setImageDrawable(null)
                iv.setOnClickListener(null)
            }
        }
    }

    private fun pin(app: AppInfo) {
        dockPkgs.remove(app.pkg)
        dockPkgs.add(app.pkg)
        while (dockPkgs.size > 4) dockPkgs.removeAt(0)
        saveDock()
        refreshDock()
        Toast.makeText(this, "${app.label} fijada en el dock", Toast.LENGTH_SHORT).show()
    }

    // ---------- Cajón ----------
    private fun hideKeyboard(v: View) {
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(v.windowToken, 0)
    }

    private fun openDrawer(focusSearch: Boolean) {
        note.clearFocus()
        hideKeyboard(note)
        drawer.alpha = 0f
        drawer.translationY = 300f
        drawer.visibility = View.VISIBLE
        drawer.animate().alpha(1f).translationY(0f).setDuration(220).start()
        if (focusSearch) {
            search.requestFocus()
            drawer.postDelayed({
                (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .showSoftInput(search, InputMethodManager.SHOW_IMPLICIT)
            }, 250)
        }
    }

    private fun closeDrawer() {
        if (drawer.visibility != View.VISIBLE) return
        search.setText("")
        hideKeyboard(search)
        drawer.animate().alpha(0f).translationY(300f).setDuration(180)
            .withEndAction { drawer.visibility = View.GONE }.start()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        closeDrawer() // botón Home
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (drawer.visibility == View.VISIBLE) closeDrawer()
        // Un launcher no sale con Back
    }

    inner class AppAdapter : BaseAdapter() {
        override fun getCount() = shown.size
        override fun getItem(p: Int) = shown[p]
        override fun getItemId(p: Int) = p.toLong()
        override fun getView(p: Int, convert: View?, parent: ViewGroup): View {
            val v = convert ?: LayoutInflater.from(parent.context)
                .inflate(R.layout.item_app, parent, false)
            val app = shown[p]
            v.findViewById<ImageView>(R.id.icon).setImageDrawable(app.icon)
            v.findViewById<TextView>(R.id.label).text = app.label
            return v
        }
    }
}
