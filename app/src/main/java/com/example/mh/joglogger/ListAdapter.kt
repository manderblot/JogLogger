package com.example.mh.joglogger

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.support.annotation.Nullable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CursorAdapter
import android.widget.TextView

class ListAdapter(context: Context,cursor: Cursor?,flg: Int) : CursorAdapter(context,cursor,flg) {
    override fun bindView(view: View?, context: Context?, cursor: Cursor?) {
        val id = cursor?.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID))
        val date = cursor?.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DATE))
        val elapsedTime = cursor?.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ELAPSEDTIME))
        val distance = cursor?.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DISTANCE))
        val speed = cursor?.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_SPEED))
        val address = cursor?.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ADDRESS))

        val tv_id = view?.findViewById<TextView>(R.id._id)
        val tv_date = view?.findViewById<TextView>(R.id.date)
        val tv_elapsed_time = view?.findViewById<TextView>(R.id.elapsed_time)
        val tv_distance = view?.findViewById<TextView>(R.id.distance)
        val tv_speed = view?.findViewById<TextView>(R.id.speed)
        val tv_place = view?.findViewById<TextView>(R.id.address)

        tv_id?.text = id.toString()
        tv_date?.text = date
        tv_elapsed_time?.text = elapsedTime
        tv_distance?.text = String.format("%.2f",distance?.div(1000))
        tv_speed?.text = String.format("%.2f",speed)
        tv_place?.text = address

    }

    @SuppressLint("ServiceCast")
    override fun newView(context: Context?, cursor: Cursor?, parent: ViewGroup?): View {
        val inflater : LayoutInflater = context?.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.row,null)
        return view
    }


}