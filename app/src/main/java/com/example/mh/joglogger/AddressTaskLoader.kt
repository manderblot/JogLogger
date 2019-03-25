package com.example.mh.joglogger

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.support.v4.content.AsyncTaskLoader
import android.util.Log
import java.io.IOException
import java.util.*

class AddressTaskLoader(context: Context,lat: Double,lng: Double) : AsyncTaskLoader<Address>(context) {
    override fun loadInBackground(): Address? {
        var result : Address? = null
        try{
            var results = mGeocoder?.getFromLocation(mLat,mLng,1)
            if(results != null && !results.isEmpty()){
                result = results.get(0)
            }
        }catch (e : IOException){
            Log.e("AddressTaskLoader",e.localizedMessage)
        }
        return result
    }

    companion object {
        private var mGeocoder : Geocoder? = null
        private var mLat : Double = 0.0
        private var mLng : Double = 0.0
    }

    init {
        mGeocoder = Geocoder(context, Locale.getDefault())
        mLat = lat
        mLng = lng
    }

    override fun onStartLoading() {
        forceLoad()
    }
}