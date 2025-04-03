package com.efedorchenko.timely.ui.support

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.core.animation.doOnStart
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.findNavController
import com.efedorchenko.timely.R
import com.efedorchenko.timely.TimelyApplication
import com.google.android.material.R.id.design_bottom_sheet
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.CoroutineScope

fun Context.applicationScope(): CoroutineScope {
    return (applicationContext as TimelyApplication).applicationScope
}

fun FragmentActivity.hideKeyboard() {
    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    val view = currentFocus ?: View(this)
    imm.hideSoftInputFromWindow(view.windowToken, 0)
}

fun ProgressBar.show() {
    visibility = View.VISIBLE
    alpha = 0f
    animate().alpha(1f).setDuration(200).start()
}

fun ProgressBar.hide() {
    visibility = View.GONE
}

fun Dialog.setupAsExpandedBottomSheet() {
    setOnShowListener {
        findViewById<View>(design_bottom_sheet)?.let {
            BottomSheetBehavior.from(it).state = BottomSheetBehavior.STATE_EXPANDED
        }
    }
}

/**
 * Выполняется сразу при вызове. Использовать только через листенер
 */
fun ImageButton.animClickListener(onClick: () -> Unit) {
    val context = context ?: return
    val blackColor = ContextCompat.getColor(context, R.color.dark_gray)
    val defaultColor = imageTintList?.defaultColor ?: ContextCompat.getColor(context, R.color.light_gray)

    setOnClickListener {
        val scaleXAnimator = ObjectAnimator.ofFloat(this, "scaleX", 1f, 0.9f, 1f)
            .apply { duration = 150 }

        val scaleYAnimator = ObjectAnimator.ofFloat(this, "scaleY", 1f, 0.9f, 1f)
            .apply { duration = 150 }

        val colorAnimator = ValueAnimator.ofArgb(defaultColor, blackColor, defaultColor).apply {
            duration = 200
            addUpdateListener { imageTintList = ColorStateList.valueOf(it.animatedValue as Int) }
        }

        AnimatorSet().apply {
            playTogether(scaleXAnimator, scaleYAnimator, colorAnimator)
            doOnStart { onClick() }
            start()
        }
    }
}

fun Fragment.navigateForgetting(fragmentId: Int) {
    val navController = findNavController()
    navController.popBackStack(navController.graph.startDestinationId, true)
    navController.navigate(fragmentId)
}