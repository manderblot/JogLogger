package com.example.mh.joglogger

import android.app.ListActivity
import android.database.Cursor
import android.os.Bundle
import android.app.LoaderManager
import android.content.CursorLoader
import android.content.Loader
import android.widget.Button

@Suppress("DEPRECATION")
class JogView : ListActivity(),LoaderManager.LoaderCallbacks<Cursor> {
    private val CURSORLOADER_ID = 0
    private lateinit var mAdapter : ListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view)

        val btnView = this.findViewById<Button>(R.id.btnRet)
        btnView.setOnClickListener { finish() }

        var mAdapter = ListAdapter(this,null,0)
        listAdapter = mAdapter

        loaderManager.initLoader(CURSORLOADER_ID,null,this)
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        return CursorLoader(this,JogRecordContentProvider.CONTENT_URI,null,null,null,"_id DESC") as Loader<Cursor>
    }

    override fun onLoadFinished(loader: Loader<Cursor>?, data: Cursor?) {
        mAdapter.swapCursor(data)
    }

    override fun onLoaderReset(loader: Loader<Cursor>?) {
        mAdapter.swapCursor(null)
    }
}