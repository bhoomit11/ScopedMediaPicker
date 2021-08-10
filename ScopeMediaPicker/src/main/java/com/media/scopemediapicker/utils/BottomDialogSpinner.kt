package com.media.scopemediapicker.utils

import android.app.Activity
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.media.scopemediapicker.R
import com.media.scopemediapicker.databinding.DialogBasicListBinding
import com.media.scopemediapicker.databinding.ItemBottomDialogListBinding
import com.simpleadapter.SimpleAdapter

/*
* BottomDialog for selection
* */
class BottomDialogSpinner<M>(val activity: Activity, val list: ArrayList<M>) {
    companion object {
        fun <M> with(activity: Activity, list: ArrayList<M>): BottomDialogSpinner<M> {
            return BottomDialogSpinner(activity, list)
        }
    }

    private var getValueCallback: (value: M) -> String = { value -> value.toString() }
    fun setMap(callback: (value: M) -> String): BottomDialogSpinner<M> {
        getValueCallback = callback
        return this
    }

    private var getSubValueCallback: (subValue: M) -> String = { _ -> "" }
    fun setSubMap(callback: (subValue: M) -> String): BottomDialogSpinner<M> {
        getSubValueCallback = callback
        return this
    }


    private var enableSearch: Boolean = false
    fun setEnableSearch(value: Boolean): BottomDialogSpinner<M> {
        enableSearch = value
        return this
    }

    private var title: String = ""
    fun setTitle(value: String): BottomDialogSpinner<M> {
        title = value
        return this
    }

    private var onValueSelected: (value: M) -> Unit = {}
    fun setOnValueSelectedCallback(callback: (value: M) -> Unit): BottomDialogSpinner<M> {
        onValueSelected = callback
        return this
    }

    var dialogFragment: DialogFragment? = null
    fun build(isCancellable: Boolean = true, isCancellableOnTouchOutSide: Boolean = true): BottomDialogSpinner<M> {

        dialogFragment = BottomSheetDialogFragmentHelper.with<DialogBasicListBinding>(
                R.layout.dialog_basic_list,
                isCancellable = isCancellable,
                isCancellableOnTouchOutSide = isCancellableOnTouchOutSide,
                onStartCallback = {
                    it.dialog?.setOnShowListener { _ ->
                        Handler().postDelayed({
                            val d = it.dialog as BottomSheetDialog
                            val bottomSheet = d.findViewById<FrameLayout>(R.id.design_bottom_sheet)
                            val bottomSheetBehavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet as View)
                            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                        }, 0)
                    }
                }
        ) { it, dialog ->

            it.tvTitle.text = title

            if (title.isNotBlank()) {
                it.tvTitle.text = title
                it.divider.visibility = View.VISIBLE
                it.tvTitle.visibility = View.VISIBLE
            } else {
                it.divider.visibility = View.GONE
                it.tvTitle.visibility = View.GONE
            }
            it.recyclerView.layoutManager = LinearLayoutManager(activity)
            if (enableSearch) {
                it.llSearch.visibility = View.VISIBLE
            } else {
                it.llSearch.visibility = View.GONE
            }
            val adapter = SimpleAdapter.with<M, ItemBottomDialogListBinding>(R.layout.item_bottom_dialog_list) { _, model, binding ->
                binding.tvText.text = getValueCallback(model)

                binding.tvSubText.visibility = View.GONE
                getSubValueCallback(model).ifNotBlank {
                    binding.tvSubText.text = getSubValueCallback(model)
                    binding.tvSubText.visibility = View.VISIBLE
                }

            }
            adapter.setClickableViews({ _: View, model: M, _: Int ->
                onValueSelected(model)
                dialog.dismiss()
            }, R.id.llRoot)

            it.etKeyword.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    adapter.performFilter(s.toString()) { text: String, model: M ->
                        getValueCallback(model).toLowerCase().contains(text.toLowerCase())
                    }
                }

                override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                ) {

                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                }
            })

            adapter.addAll(list)
            it.recyclerView.adapter = adapter

        }
        return this
    }

    fun show(fragmentManager: FragmentManager) {
        dialogFragment?.show(fragmentManager, "dialog")
    }
}
