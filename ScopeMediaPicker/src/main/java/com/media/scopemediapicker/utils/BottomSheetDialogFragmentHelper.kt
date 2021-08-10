package com.media.scopemediapicker.utils

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class BottomSheetDialogFragmentHelper<B : ViewDataBinding> : BottomSheetDialogFragment() {

    var viewCreatedCallback: ((binding: B, dialogFragment: BottomSheetDialogFragment) -> Unit)? =
        null
    var onStartCallback: ((bottomSheetDialogFragment: BottomSheetDialogFragment) -> Unit)? = null
    var onCreateCallback: ((bottomSheetDialogFragment: BottomSheetDialogFragment) -> Unit)? = null

    companion object {
        fun <B : ViewDataBinding> with(
            layout: Int,
            isAnimationRequire: Boolean = true,
            isCancellable: Boolean = true,
            isCancellableOnTouchOutSide: Boolean = true,
            onStartCallback: ((bottomSheetDialogFragment: BottomSheetDialogFragment) -> Unit)? = null,
            onCreateCallback: ((bottomSheetDialogFragment: BottomSheetDialogFragment) -> Unit)? = null,
            viewCreatedCallback: (binding: B, dialogFragment: BottomSheetDialogFragment) -> Unit
        ): androidx.fragment.app.DialogFragment {
            val dialog = BottomSheetDialogFragmentHelper<B>().apply {
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
            dialog.onCreateCallback = onCreateCallback
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

        viewCreatedCallback?.invoke(DataBindingUtil.bind(view)!!, this)
        if (viewCreatedCallback == null) {
            dismiss()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setStyle(DialogFragment.STYLE_NORMAL, R.style.BottomSheetDialogThemeNoFloating)
        onCreateCallback?.invoke(this)
    }

    override fun onStart() {
        super.onStart()
        if (view?.parent != null)
            (view?.parent as View).setBackgroundColor(Color.TRANSPARENT)
        onStartCallback?.invoke(this)
    }
}