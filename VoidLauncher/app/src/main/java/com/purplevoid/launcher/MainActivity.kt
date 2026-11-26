package com.purplevoid.launcher

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
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

data class AppInfo(val label: String, val pkg: String, val icon: Drawable)

class MainActivity : Activity() {

    private lateinit var drawer: View
    private lateinit var search: EditText
    private lateinit var grid: GridView
    private val allApps = mutableListOf<AppInfo>()
    private val shown = mutableListOf<AppInfo>()
    private lateinit var adapter: AppAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawer = findViewById(R.id.drawer)
        search = findViewById(R.id.search)
        grid = findViewById(R.id.grid)

        loadApps()
        adapter = AppAdapter()
        grid.adapter = adapter

        grid.setOnItemClickListener { _, _, pos, _ ->
            packageManager.getLaunchIntentForPackage(shown[pos].pkg)?.let {
                startActivity(it)
                closeDrawer()
            }
        }

        search.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = filter(s?.toString().orEmpty())
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })

        // Desliza hacia arriba en el home para abrir el cajón
        val detector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent) = true
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
                if (vy < -1000) { openDrawer(); return true }
                return false
            }
        })
        findViewById<View>(R.id.home).setOnTouchListener { _, ev -> detector.onTouchEvent(ev) }
    }

    private fun loadApps() {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        packageManager.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .map {
                AppInfo(
                    it.loadLabel(packageManager).toString(),
                    it.activityInfo.packageName,
                    it.loadIcon(packageManager)
                )
            }
            .sortedBy { it.label.lowercase() }
            .let { allApps.addAll(it); shown.addAll(it) }
    }

    private fun filter(q: String) {
        shown.clear()
        shown.addAll(allApps.filter { it.label.contains(q, ignoreCase = true) })
        adapter.notifyDataSetChanged()
    }

    private fun openDrawer() {
        drawer.alpha = 0f
        drawer.translationY = 300f
        drawer.visibility = View.VISIBLE
        drawer.animate().alpha(1f).translationY(0f).setDuration(220).start()
    }

    private fun closeDrawer() {
        if (drawer.visibility != View.VISIBLE) return
        search.setText("")
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(search.windowToken, 0)
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
