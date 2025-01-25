package com.efedorchenko.timely.fragment.support

import android.app.Dialog
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import androidx.fragment.app.FragmentActivity
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
    }
}