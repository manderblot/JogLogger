package com.example.mh.joglogger

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment

class WifiConfirmDialogFragment : DialogFragment() {
    private val ARG_TITLE = "title"
    private val ARG_MESSAGE = "message"

    private var mTitle = 0
    private var mMessage = 0

    open fun newInstance(title: Int,message: Int): WifiConfirmDialogFragment {
        var fragment = WifiConfirmDialogFragment()
        var args = Bundle()
        args.putInt(ARG_TITLE,title)
        args.putInt(ARG_MESSAGE,message)
        fragment.arguments = args
        return fragment
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if(arguments != null){
            mTitle = arguments!!.getInt(ARG_TITLE)
            mMessage = arguments!!.getInt(ARG_MESSAGE)
        }
        return AlertDialog.Builder(activity)
            .setTitle(mTitle)
            .setMessage(mMessage)
            .setNegativeButton(R.string.alert_dialog_no,
                DialogInterface.OnClickListener { dialog , which ->  })
            .setPositiveButton(R.string.alert_dialog_yes, DialogInterface.OnClickListener() {
                    dialogInterface: DialogInterface, i: Int ->
                (activity as MapsActivity).wifiOff()
            })
            .create()
    }
}