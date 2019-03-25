package com.example.mh.joglogger

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context,DBNAME,null,DBVERSION) {


    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(CREATE_TABLE_SQL)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {

    }

    companion object {
        val DBNAME = "jogrecord.db"
        val DBVERSION = 1
        val TABLE_JOGRECORD = "jogrecord"
        val COLUMN_ID = "_id"
        val COLUMN_DATE = "date"
        val COLUMN_ELAPSEDTIME = "eltime"
        val COLUMN_DISTANCE = "distance"
        val COLUMN_SPEED = "speed"
        val COLUMN_ADDRESS = "address"
        val CREATE_TABLE_SQL =
            "create table " + TABLE_JOGRECORD + " " +
                    "(" + COLUMN_ID + " integer primary key autoincrement," +
                    COLUMN_DATE + " text not null," +
                    COLUMN_ELAPSEDTIME + " real not null," +
                    COLUMN_DISTANCE + " real not null," +
                    COLUMN_SPEED + " real not null," +
                    COLUMN_ADDRESS + " text null)"
    }
}