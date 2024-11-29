package com.atvantiq.wfms.ui.dialogs

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.atvantiq.wfms.R

class ProgressCircularDialog : DialogFragment() {
	
	val ARG_TITLE: String = "ARG_TITLE"
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setStyle(DialogFragment.STYLE_NO_FRAME, R.style.Theme_WFMS)
	}
	
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		return inflater.inflate(R.layout.progress_circular_dailog, container, false)
	}
	
	override fun onStart() {
		super.onStart()
		val dialog = dialog
		if (dialog != null) {
			dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
			isCancelable = false
		}
	}
	
	companion object {
		@JvmStatic
		fun newInstance(title: String) =
			ProgressCircularDialog().apply {
				arguments = Bundle().apply {
					putString(ARG_TITLE, title)
				}
			}
	}
	
}
