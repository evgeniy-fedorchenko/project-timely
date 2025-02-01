package com.efedorchenko.timely.fragment.support

import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.efedorchenko.timely.R
import com.google.android.material.R.id.design_bottom_sheet
import com.google.android.material.bottomsheet.BottomSheetBehavior

class FragmentUtils {

    companion object {

        fun hideKeyboard(activity: FragmentActivity?) {
            activity?.let {
                val imm = it.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                val view = activity.currentFocus ?: View(activity)
                imm.hideSoftInputFromWindow(view.windowToken, 0)
            }
        }

        fun showLoading(loadingProgressBar: ProgressBar) {
            loadingProgressBar.apply {
                visibility = View.VISIBLE
                alpha = 0f
                animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start()
            }
        }

        fun hideLoading(loadingProgressBar: ProgressBar) {
            loadingProgressBar.visibility = View.GONE
        }

        fun setUpDialogListener(dialog: Dialog?) {
            dialog?.setOnShowListener {
                dialog.findViewById<View>(design_bottom_sheet)?.let {
                    BottomSheetBehavior.from(it).state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
        }

        fun setupButtonAnimationAndClick(
            button: ImageButton,
            context: Context?,
            onClick: () -> Unit,
            mainColor: Int? = null
        ) {
            if (context == null) return
            val blackColor = ContextCompat.getColor(context, R.color.dark_gray)

            val defaultColor = mainColor
                ?: button.imageTintList?.defaultColor
                ?: ContextCompat.getColor(context, R.color.light_gray)

            button.setOnClickListener {
                button.animate().scaleX(0.9f).scaleY(0.9f).setDuration(150).withEndAction {
                    button.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                }.start()

                ValueAnimator.ofArgb(defaultColor, blackColor, defaultColor).apply {
                    duration = 300
                    addUpdateListener { animator ->
                        button.imageTintList = ColorStateList.valueOf(animator.animatedValue as Int)
                    }
                    doOnEnd { onClick() }
                    start()
                }
            }
        }
    }
}