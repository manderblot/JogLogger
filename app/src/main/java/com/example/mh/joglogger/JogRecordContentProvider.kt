package com.example.mh.joglogger

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import org.jetbrains.annotations.Nullable
import java.lang.IllegalArgumentException

class JogRecordContentProvider : ContentProvider() {
    private lateinit var mDbHelper: DatabaseHelper

    companion object {
        val JOGRECORD = 10
        val JOGRECORD_ID = 20
        val AUTHORITY = "com.example.mh.joglogger.JogRecordContentProvider"
        val BASE_PATH = "joglogger"
        val CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH)
    }

    private var uriMatcher = UriMatcher(UriMatcher.NO_MATCH)
    init {
        uriMatcher.addURI(AUTHORITY,BASE_PATH,JOGRECORD)
        uriMatcher.addURI(AUTHORITY,BASE_PATH + "/#",JOGRECORD_ID)
    }

    override fun onCreate(): Boolean {
        mDbHelper = DatabaseHelper(context)
        return false
    }

    @Nullable
    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        var queryBuilder = SQLiteQueryBuilder()
        queryBuilder.tables = DatabaseHelper.TABLE_JOGRECORD
        var uriType = uriMatcher.match(uri)
        when(uriType){
            JOGRECORD -> {}
            JOGRECORD_ID -> {
                queryBuilder.appendWhere(DatabaseHelper.COLUMN_ID + "=" + uri.lastPathSegment)
            }
            else -> {
                throw IllegalArgumentException("Unknown URI: " + uri)
            }
        }
        var db = mDbHelper.writableDatabase
        var cursor = queryBuilder.query(db,projection,selection,selectionArgs,null,null,sortOrder)
        return cursor
    }

    @Nullable
    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        var uriType = uriMatcher.match(uri)
        var sqlDB = mDbHelper.writableDatabase
        var id = 0
        when(uriType){
            JOGRECORD -> {
                id = sqlDB.insert(DatabaseHelper.TABLE_JOGRECORD,null,values).toInt()
            }
            else -> {
                throw IllegalArgumentException("Unknown URI: " + uri)
            }
        }
        context.contentResolver.notifyChange(uri, null)
        return Uri.withAppendedPath(uri, id.toString())
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        return 0
    }

    @Nullable
    override fun getType(uri: Uri): String? {
        return null
    }

    @Nullable
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
        return 0
    }
}