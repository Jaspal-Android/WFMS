package com.atvantiq.wfms.ui.dialogs

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.atvantiq.wfms.R

class ProgressDialog : DialogFragment() {
	val ARG_TITLE: String = "ARG_TITLE"
	
	
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		return inflater.inflate(R.layout.progress_dialog, container, false)
	}
	
	override fun onStart() {
		super.onStart()
		val dialog = dialog
		if (dialog != null) {
			/*val width = ViewGroup.LayoutParams.MATCH_PARENT
			val height = ViewGroup.LayoutParams.MATCH_PARENT
			dialog.window?.setLayout(width, height)*/
			dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
			isCancelable = false
		}
	}
	
	companion object {
		@JvmStatic
		fun newInstance(title: String) =
			ProgressDialog().apply {
				arguments = Bundle().apply {
					putString(ARG_TITLE, title)
				}
			}
	}
	
}
