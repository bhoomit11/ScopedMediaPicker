package com.media.scopemediapicker.utils

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class BottomSheetDialogFragmentHelperView : BottomSheetDialogFragment() {

    var viewCreatedCallback: ((binding: View, dialogFragment: BottomSheetDialogFragment) -> Unit)? =
        null
    var onStartCallback: ((bottomSheetDialogFragment: BottomSheetDialogFragment) -> Unit)? = null

    companion object {
        fun with(
            layout: Int,
            isAnimationRequire: Boolean = true,
            isCancellable: Boolean = true,
            isCancellableOnTouchOutSide: Boolean = true,
            onStartCallback: ((bottomSheetDialogFragment: BottomSheetDialogFragment) -> Unit)? = null,
            viewCreatedCallback: (binding: View, dialogFragment: BottomSheetDialogFragment) -> Unit

        ): androidx.fragment.app.DialogFragment {
            val dialog = BottomSheetDialogFragmentHelperView().apply {
                arguments = Bundle().apply {
                    putInt("RES", layout)
                    putBoolean("IS_CANCELLABLE", isCancellable)
                    putBoolean("IS_CANCELLABLE_ON_TOUCH_OUTSIDE", isCancellableOnTouchOutSide)
                    putBoolean("IS_ANIMATION_REQUIRE", isAnimationRequire)
                }
            }
            dialog.isCancelable = isCancellable
            dialog.viewCreatedCallback = viewCreatedCallback
            dialog.onStartCallback = onStartCallback
            return dialog
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(arguments?.getInt("RES")!!, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewCreatedCallback?.invoke(view, this)
        if (viewCreatedCallback == null) {
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        if (view?.parent != null)
            (view?.parent as View).setBackgroundColor(Color.TRANSPARENT)
        onStartCallback?.invoke(this)
    }
}