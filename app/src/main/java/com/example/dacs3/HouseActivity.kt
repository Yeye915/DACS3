package com.example.dacs3

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/** Consistent system-bar and keyboard spacing for XML screens, including Android 15. */
abstract class HouseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        updateSystemBarAppearance()
    }

    override fun onPostResume() {
        super.onPostResume()
        updateSystemBarAppearance()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateSystemBarAppearance()
    }

    private fun updateSystemBarAppearance() {
        val light = ColorUtils.calculateLuminance(ContextCompat.getColor(this, R.color.background)) > 0.5
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = light
            isAppearanceLightNavigationBars = light
        }
    }

    override fun setContentView(view: View) {
        super.setContentView(view)
        applyScreenInsets(view)
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        applyScreenInsets(findViewById<ViewGroup>(android.R.id.content).getChildAt(0))
    }

    private fun applyScreenInsets(root: View) {
        val left = root.paddingLeft
        val top = root.paddingTop
        val right = root.paddingRight
        val bottom = root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val keyboard = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.updatePadding(left + bars.left, top + bars.top, right + bars.right,
                bottom + maxOf(bars.bottom, keyboard.bottom))
            WindowInsetsCompat.CONSUMED
        }
        ViewCompat.requestApplyInsets(root)
        root.findViewById<View>(R.id.btnBackAuth)?.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }
}
