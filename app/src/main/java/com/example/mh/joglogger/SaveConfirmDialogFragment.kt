package com.example.mh.joglogger

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment

class SaveConfirmDialogFragment : DialogFragment() {
    companion object {
        fun newInstance(save_confirm_dialog_title: Int, message: String): DialogFragment {
            var fragment: SaveConfirmDialogFragment = SaveConfirmDialogFragment()
            var args = Bundle()
            args.putInt(ARG_TITLE,save_confirm_dialog_title)
            args.putString(ARG_MESSAGE,message)
            fragment.arguments = args
            return fragment
        }

        val ARG_TITLE = "title"
        val ARG_MESSAGE = "message"
    }

    private var mTitle : Int = 0
    private lateinit var mMessage : String

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if(arguments != null){
            mTitle = arguments!!.getInt(ARG_TITLE)
            mMessage = arguments!!.getString(ARG_MESSAGE)
        }
        return AlertDialog.Builder(activity)
            .setTitle(mTitle)
            .setMessage(mMessage)
            .setNegativeButton(R.string.alert_dialog_cancel,
                DialogInterface.OnClickListener(){ dialogInterface: DialogInterface, i: Int -> })
            .setPositiveButton(R.string.alert_dialog_ok,
                DialogInterface.OnClickListener() { dialogInterface: DialogInterface, i: Int ->
                    (activity as MapsActivity).saveJogViaCTP()
                })
            .create()
    }
}