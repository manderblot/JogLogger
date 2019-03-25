package com.example.mh.joglogger

import android.Manifest
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Address
import android.location.Location
import android.location.LocationListener
import android.net.wifi.WifiManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import android.support.v4.app.ActivityCompat
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentActivity
import android.support.v4.content.Loader
import android.support.v4.content.PermissionChecker
import android.util.Log
import android.view.WindowManager
import android.widget.*
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.*

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import java.lang.StringBuilder
import java.text.SimpleDateFormat

class MapsActivity : FragmentActivity(), OnMapReadyCallback,
    GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener,LocationListener,
    android.support.v4.app.LoaderManager.LoaderCallbacks<Address>{
    private lateinit var mMap: GoogleMap
    private lateinit var mGoogleApiClient: GoogleApiClient
    private var fusedLocationClient : FusedLocationProviderClient? = null
    private val locationCallback : LocationCallback by lazy {
        (object : LocationCallback(){
            override fun onLocationResult(p0: LocationResult?) {
                super.onLocationResult(p0)
                p0?.lastLocation?.let { location ->
                    showToast("位置情報の再取得に成功しました")
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(LatLng(location.latitude,location.longitude)))
                }
            }
        })
    }

    private lateinit var mRunList : ArrayList<LatLng>
    private lateinit var mWifi : WifiManager
    private var mWifiOff = false
    private var mStartTimeMills : Long = 0
    private var mMeter : Double = 0.0
    private var mElapsedTime : Double = 0.0
    private var mSpeed : Double = 0.0
    private lateinit var mDbHelper : DatabaseHelper
    private var mStart = false
    private var mFirst = false
    private var mStop = false
    private var mAsked = false
    private lateinit var mChronometer : Chronometer

    override fun onSaveInstanceState(outState : Bundle){
        super.onSaveInstanceState(outState)
	//一覧表示してマップ画面に戻ってくる時にメンバー変数が初期化されることへの対処
        outState.putBoolean("ASKED",mAsked)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
	//メンバー変数リストア
        mAsked = savedInstanceState!!.getBoolean("ASKED")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        mGoogleApiClient = GoogleApiClient.Builder(this)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(LocationServices.API)
            .build()

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment.newInstance().let { supportMapFragment ->
            supportFragmentManager.beginTransaction()
                .replace(R.id.map,supportMapFragment)
                .commit()
            supportMapFragment.getMapAsync{googleMap ->
                mMap = googleMap.apply {
                    this.isIndoorEnabled = false
                }
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LOCATION_TOKYO, DEFAULT_ZOOM_LEVEL))
            }
        }

        mDbHelper = DatabaseHelper(this)
        var tb = findViewById<ToggleButton>(R.id.toggleButton)
        tb.isChecked = false //トグルボタンOFF
        //Toggleのcheckが変更したタイミングで呼び出されるリスナー
        tb.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener {
                compoundButton: CompoundButton, b: Boolean ->
            fun onCheckedChanged(buttonView : CompoundButton,isChecked : Boolean){
                if(isChecked){
                    startChronometer()
                    mStart = true
                    mFirst = true
                    mStop = false
                    mMeter = 0.0
                    mRunList.clear()
                }else{
                    stopChronometer()
                    mStop = true
                    calcSpeed()
                    saveConfirm()
                    mStart = false
                }
            }
        })

    }

    private fun startChronometer(){
        mChronometer = findViewById(R.id.chronometer)
        mChronometer.base = SystemClock.elapsedRealtime() //電源ONからの経過時間をベースに
        mChronometer.start()
        mStartTimeMills = System.currentTimeMillis()
    }

    private fun stopChronometer(){
        mChronometer.stop()
        mElapsedTime = (SystemClock.elapsedRealtime() - mChronometer.base).toDouble()
    }

    override fun onResume() {
        super.onResume()
        if(!mAsked){
            wifiConfirm()
            mAsked = !mAsked
        }
        mGoogleApiClient.connect()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMapLongClickListener{
            var intent: Intent = Intent(this, JogView::class.java)
            startActivity(intent)
        }

        if(ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)){
                AlertDialog.Builder(this)
                    .setTitle("許可が必要です")
                    .setMessage("移動に合わせて地図を動かすためには、ACCESS_FINE_LOCATIONを許可してください")
                    .setPositiveButton("OK",DialogInterface.OnClickListener { dialogInterface: DialogInterface, i: Int ->
                        requestAccessFineLocation()
                    })
                    .setNegativeButton("Cancel",DialogInterface.OnClickListener { dialogInterface: DialogInterface, i: Int ->
                        showToast("GPS機能が使えないので、地図は動きません")
                    })
                    .show()
            }else{
                requestAccessFineLocation()
            }
        }
    }

    private fun requestAccessFineLocation(){
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION.toString()),MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode){
            MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {
                //ユーザーが許可した時許可が必要な機能を改めて実行する
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    showToast("GPS機能が使えないので、地図は動きません")
                }
                return
            }
            MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE -> {
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    saveConfirmDialog()
                }else{
                    showToast("外部へのファイルの保存が許可されなかったので記録できません")
                }
                return
            }
        }
    }

    private fun wifiConfirm(){
        mWifi  = getSystemService(Context.WIFI_SERVICE) as WifiManager

        if(mWifi.isWifiEnabled){
            wifiConfirmDialog()
        }
    }

    private fun wifiConfirmDialog(){
        var newFragment : DialogFragment = WifiConfirmDialogFragment().newInstance(
            R.string.wifi_confirm_dialog_title,R.string.wifi_confirm_dialog_message
        )
        newFragment.show(supportFragmentManager,"dialog")
    }

    open fun wifiOff(){
        mWifi.isWifiEnabled = false
        mWifiOff=true
    }

    override fun onConnected(p0: Bundle?) {
	//Google Playサービスに接続
        if(ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED){
            return
        }
        getUpdateLocation() //位置の更新をリクエスト
    }

    private fun recieveLocation(){
        if(GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) != ConnectionResult.SUCCESS){
            return
        }
        LocationServices.getFusedLocationProviderClient(this).let{client ->
            client.lastLocation.addOnCompleteListener(this){task ->
                if(task.isSuccessful && task.result != null){
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(LatLng(task.result!!.latitude,task.result!!.longitude)))
                }else{
                    getUpdateLocation()
                }
            }
        }
    }

    private fun getUpdateLocation(){
        val request = LocationRequest.create().apply {
            interval = INTERVAL
            numUpdates = 1
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient?.let {client ->
            client.requestLocationUpdates(request,locationCallback,null)
                .addOnCompleteListener{task ->
                    if(task.result == null){
                        showToast("位置情報の取得に失敗しました")
                    }
                }
        }
    }

    override fun onLocationChanged(location: Location){
        Log.d("onLocationChanged","called onLocationChanged")
        if(mStop){
            return
        }
        val cameraPos = CameraPosition.Builder()
            .target(LatLng(location.latitude,location.longitude)).zoom(DEFAULT_ZOOM_LEVEL)
            .bearing(0f).build()
        //地図の中心をlatitude,longtitudeに移動
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPos))

        //マーカー設定
        mMap.clear()
        var latlng = LatLng(location.latitude,location.longitude)
        var options = MarkerOptions()
        options.position(latlng)
        //ランチャーアイコン
        var icon = BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher)
        options.icon(icon)
        mMap.addMarker(options)

        if(mStart){
            if(mFirst){ //１回目の位置取得
                //バックグラウンドで住所を取得
                var args = Bundle()
                args.putDouble("lat",location.latitude)
                args.putDouble("lng",location.longitude)
                //loaderManager?.restartLoader(ADDRESSLOADER_ID,args,this)
                supportLoaderManager.restartLoader(ADDRESSLOADER_ID,args,this)
                //restartLoaderはローダが実行中なら自動的に中断する
                mFirst = !mFirst
            }else{
                drawTrace(latlng)
                sumDistance()
            }
        }
    }

    private fun drawTrace(latLng: LatLng){
        mRunList.add(latLng)
        if(mRunList.size > 2){
            var polyOptions = PolylineOptions()
            for (polyLatLng: LatLng in mRunList){
                polyOptions.add(polyLatLng)
            }
            polyOptions.color(Color.BLUE)
            polyOptions.width(3F)
            polyOptions.geodesic(false)
            mMap.addPolyline(polyOptions)
        }
    }

    private fun sumDistance() {
        if(mRunList.size < 2){
            return
        }

        mMeter = 0.0
        var results = floatArrayOf(3F)
        var i  = 1
        while(i < mRunList.size){
            results[0] = 0F
            Location.distanceBetween(mRunList.get(i-1).latitude,mRunList.get(i-1).longitude,
                mRunList.get(i).latitude,mRunList.get(i).longitude,results)
            mMeter += results[0]
            i++
        }
        var disMeter = mMeter / 1000
        val disText = findViewById<TextView>(R.id.disText)
        disText.text = String.format("%.2f"+" km",disMeter)
    }

    private fun calcSpeed(){
        sumDistance()
        mSpeed = (mMeter/1000) / (mElapsedTime/1000) * 60 * 60
    }

    private fun saveConfirm(){
        if(ActivityCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                AlertDialog.Builder(this)
                    .setTitle("許可が必要です")
                    .setMessage("記録を残すためにはWRITE_EXTERNAL_STORAGEを許可してください")
                    .setPositiveButton("OK",DialogInterface.OnClickListener {
                        dialog: DialogInterface,which: Int ->
                        requestWriteExternalStorage()
                    })
                    .setNegativeButton("Cancel",DialogInterface.OnClickListener { dialogInterface: DialogInterface, i: Int ->
                        showToast("外部ファイルへの保存が許可されなかったので記録できません")
                    })
                    .show()
            }else{
                requestWriteExternalStorage()
            }
        }else{
            saveConfirmDialog()
        }
    }

    private fun requestWriteExternalStorage(){
        ActivityCompat.requestPermissions(this,
            arrayOf<String>(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE)
    }

    private fun saveConfirmDialog(){
        var message = "時間:"
        val disText = findViewById<TextView>(R.id.disText)

        message = message + mChronometer.text + " " +
                "距離" + disText.text + "\n" +
                "時速" + String.format("%.2f" + " km",mSpeed)

        var newFragment = SaveConfirmDialogFragment.newInstance(R.string.save_confirm_dialog_title,message)
        newFragment.show(supportFragmentManager,"dialog")
    }

    override fun onPause() {
        super.onPause()
        if(mGoogleApiClient.isConnected){
            stopLocationUpdates()
        }
        mGoogleApiClient.disconnect()
    }

    override fun onStop() {
        super.onStop()
        if(mWifiOff){
            mWifi.isWifiEnabled = true
        }
        stopLocationUpdates()
    }

    fun stopLocationUpdates(){
        fusedLocationClient?.removeLocationUpdates(locationCallback)
    }

    override fun onConnectionSuspended(p0: Int) {

    }

    override fun onConnectionFailed(p0: ConnectionResult) {

    }

    override fun onCreateLoader(p0: Int, p1: Bundle?): Loader<Address> {
        val lat = p1!!.getDouble("lat")
        val lng = p1.getDouble("lng")
        return AddressTaskLoader(this,lat,lng)
    }

    override fun onLoadFinished(p0: Loader<Address>, p1: Address?) {
        var i = 1
        if(p1 != null){
            var sb = StringBuilder()
            do {
                val item = p1.getAddressLine(i)
                if(item == null){
                    break
                }
                sb.append(item)
            }while(i < p1.maxAddressLineIndex + 1)
            var address = findViewById<TextView>(R.id.address)
            address.text = sb.toString()
        }
    }

    override fun onLoaderReset(p0: Loader<Address>) {

    }

    fun saveJogViaCTP(){
        var strDate = SimpleDateFormat("yyyy/mm/dd").format(mStartTimeMills)
        var txtAddress = findViewById<TextView>(R.id.address)
        var values = ContentValues()

        values.put(DatabaseHelper.COLUMN_DATE,strDate)
        values.put(DatabaseHelper.COLUMN_ELAPSEDTIME,mChronometer.text.toString())
        values.put(DatabaseHelper.COLUMN_DISTANCE,mMeter)
        values.put(DatabaseHelper.COLUMN_SPEED,mSpeed)
        values.put(DatabaseHelper.COLUMN_ADDRESS,txtAddress.text.toString())
        var uri = contentResolver.insert(JogRecordContentProvider.CONTENT_URI,values)
        showToast("データを保存しました")
    }

    fun saveJog(){
        var db = mDbHelper.writableDatabase
        var strDate = SimpleDateFormat("yyyy/mm/dd").format(mStartTimeMills)
        val txtAddress = findViewById<TextView>(R.id.address)

        var values = ContentValues()
        values.put(DatabaseHelper.COLUMN_DATE,strDate)
        values.put(DatabaseHelper.COLUMN_ELAPSEDTIME,mChronometer.text.toString())
        values.put(DatabaseHelper.COLUMN_DISTANCE,mMeter)
        values.put(DatabaseHelper.COLUMN_SPEED,mSpeed)
        values.put(DatabaseHelper.COLUMN_ADDRESS,txtAddress.text.toString())
        try{
            db.insert(DatabaseHelper.TABLE_JOGRECORD,null,values)
        }catch (e : Exception){
            showToast("データの保存に失敗しました")
        }finally {
            db.close()
        }
    }

    private fun showToast(msg : String){
        val error = Toast.makeText(this,msg,Toast.LENGTH_LONG)
        error.show()
    }

    override fun onProviderDisabled(provider: String?) {

    }

    override fun onProviderEnabled(provider: String?) {

    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

    }

    companion object {
        private val MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
        private val MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 2
        private val ADDRESSLOADER_ID = 0
        private val INTERVAL : Long = 500
        private val FASTESTINTERVAL : Long = 16
        private const val DEFAULT_ZOOM_LEVEL = 18f
        private val LOCATION_TOKYO = LatLng(35.681298, 139.766247)
    }
}

