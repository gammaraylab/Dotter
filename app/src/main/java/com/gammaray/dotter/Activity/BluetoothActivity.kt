package com.gammaray.dotter.Activity

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.*
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.appcompat.app.AppCompatActivity
import com.gammaray.dotter.R
import com.gammaray.dotter.Services.PrintService
import kotlinx.android.synthetic.main.activity_bluetooth2.*
import java.lang.Thread.sleep

@SuppressLint("SetTextI18n")
class BluetoothActivity  : AppCompatActivity(){
    private  val  TAG: String = BluetoothActivity::class.java.simpleName

    private var sent:Double=0.0
    private val sb=java.lang.StringBuilder()

    private var  mBluetoothStatus: TextView? = null
    private var  mListPairedDevicesBtn: Button? = null
    private var  mDevicesListView: ListView? = null

    private var  mBTAdapter: BluetoothAdapter? = null
    private var  mPairedDevices: Set<BluetoothDevice>? = null
    private var  mBTArrayAdapter: ArrayAdapter<String>? = null
    private var  mBTSocket: BluetoothSocket? = null
    private var  mIntent:Intent?=null

    @ExperimentalUnsignedTypes
    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth2)

        mBluetoothStatus = findViewById(R.id.bluetooth_status)
        mListPairedDevicesBtn = findViewById(R.id.paired_btn)
        ArrayAdapter<String>(this, android.R.layout.simple_list_item_1).also { mBTArrayAdapter = it }
        mBTAdapter = BluetoothAdapter.getDefaultAdapter() // get a handle on the bluetooth radio
        mDevicesListView = findViewById(R.id.devices_list_view)
        mDevicesListView!!.adapter = mBTArrayAdapter // assign model to view
        mDevicesListView!!.onItemClickListener = mDeviceClickListener
        mIntent=Intent(this,PrintService::class.java)

        registerReceiver(broadCastReceiver, IntentFilter(UPDATE_FLAG))  //broadcast receiver for updating UI

        val image: Bitmap = BitmapFactory.decodeFile(intent.getStringExtra(FILE_PATH))
        bitmapPreview.setImageBitmap(image)
        imageWidthInCM.text="Width=${String.format("%.1f",image.width.toDouble()/42.32)} cm"
        imageHeightInCM.text="Height=${String.format("%.1f",image.height.toDouble()/42.32)} cm"

        if (mBTArrayAdapter == null)
            mBluetoothStatus!!.text="Status: Bluetooth not found"
         else {
            sendData.setOnClickListener {
//                stopData.visibility=View.VISIBLE
                progressPercent.visibility=View.VISIBLE
                progressBar.visibility=View.VISIBLE

                mIntent?.putExtra(DELAY_RIGHT,rightDelay.text.toString().toInt())
                mIntent?.putExtra(DELAY_LEFT,leftDealy.text.toString().toInt())
                mIntent?.putExtra(DELAY_WRITE,writeDelay.text.toString().toInt())

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    this.startForegroundService(mIntent)
                else
                    startService(mIntent)
            }
            stopData.setOnClickListener {
                stopService(mIntent)
                updateUI()
            }
            mListPairedDevicesBtn?.setOnClickListener {
                if(mBTAdapter?.isEnabled == false)
                    mBTAdapter?.enable()
                listPairedDevices() }
        }
    }

    private fun stats(i: Int, height: Int){
        sent = (100.0*i.toDouble() / height.toDouble())
        sb.append(String.format("%.2f",sent))
        sb.append("%")
        progressPercent.text = sb.toString()
        sb.clear()
        progressBar.progress = sent.toInt()
    }

    private fun updateUI(){
        if(mBTSocket?.isConnected==true)
            mBTSocket?.close()
        mBluetoothStatus!!.text = "not connected"
        stopData.visibility=View.GONE

    }
    override fun onPause() {
        super.onPause()
        if(mBTSocket?.isConnected==true)
            mBTSocket?.close()
    }

    override fun onDestroy() {
        unregisterReceiver(broadCastReceiver)
        super.onDestroy()
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, Data: Intent?){
        super.onActivityResult(requestCode, resultCode, Data)
        if (requestCode == REQUEST_ENABLE_BT){
            if (resultCode == RESULT_OK)
                mBluetoothStatus?.text = "Enabled"
            else
                mBluetoothStatus?.text = "Disabled"
        }
    }

    private   fun listPairedDevices(){
        mBTArrayAdapter?.clear()
        mPairedDevices = mBTAdapter?.bondedDevices
        if (mBTAdapter?.isEnabled == true)
            for (  device: BluetoothDevice in mPairedDevices!!)
                mBTArrayAdapter?.add(device.name + "\n" + device.address)
        else {
            Toast.makeText(applicationContext, "Turning bluetooth on", Toast.LENGTH_SHORT).show()
            Thread {
                mBTAdapter?.enable()
                sleep(1000)
                listPairedDevices()
            }.start()
        }
    }
    private val mDeviceClickListener: OnItemClickListener = object : OnItemClickListener{
        override  fun onItemClick(parent: AdapterView<*>?, view: View, position: Int, id: Long){
            if (!mBTAdapter!!.isEnabled){
                Toast.makeText(baseContext, "Bluetooth not on", Toast.LENGTH_SHORT).show()
                return
            }
            // Get the device MAC address, which is the last 17 chars in the View
            val info: String = (view as TextView).text.toString()
            val  address: String = info.substring(info.length - 17)
            val  name: String = info.substring(0, info.length - 17)
            mIntent?.putExtra(FILE_PATH,intent.getStringExtra(FILE_PATH))
            mIntent?.putExtra(ADDRESS,address)
            mIntent?.putExtra(NAME,name)

            sendData.visibility=View.VISIBLE
        }
    }
    private val broadCastReceiver=object:BroadcastReceiver(){
        var sent:Int?=0
        var height:Int?=0
        override fun onReceive(context: Context?, intent: Intent?) {
            mBluetoothStatus!!.text=intent?.getStringExtra(UPDATE_BLUETOOTH_STATUS)
            conversionStatus.text=intent?.getStringExtra(UPDATE_CONVERSION_STATUS)
            printingStatus.text=intent?.getStringExtra(UPDATE_PRINTING_STATUS)
            handshakeInfo.text=intent?.getStringExtra(UPDATE_HANDSHAKE_INFO)
            sent=intent?.getIntExtra(UPDATE_SENT,0)
            height=intent?.getIntExtra(HEIGHT,1)
            stats(sent!!,height!!)
        }
    }
    companion object  {
        val BT_MODULE_UUID: java.util.UUID = java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // "random" unique identifier
        private const val REQUEST_ENABLE_BT: Int = 1 // used to identify adding bluetooth names
        const val FILE_PATH= "com.gammaray.dotter.file_path"
        const val ADDRESS= "com.gammaray.dotter.address"
        const val NAME= "com.gammaray.dotter.name"

        const val UPDATE_FLAG="com.gammaray.dotter.update_flag"
        const val UPDATE_SENT="com.gammaray.dotter.update_sent"
        const val UPDATE_BLUETOOTH_STATUS="com.gammaray.dotter.update_status"
        const val UPDATE_HANDSHAKE_INFO="com.gammaray.dotter.update_handshake_info"
        const val UPDATE_ERROR="com.gammaray.dotter.update_error"       // -1 INVALID WIDTH OF BMP, -2 handshake failed, -3 Insecure RFComm Connection not created, -4 Socket creation failed
        const val UPDATE_CONVERSION_STATUS="com.gammaray.dotter.update_conversion_status"
        const val UPDATE_PRINTING_STATUS="com.gammaray.dotter.update_printing_status"

        const val HEIGHT="com.gammaray.dotter.height"
        const val DELAY_RIGHT="com.gammaray.dotter.delay_right"
        const val DELAY_LEFT="com.gammaray.dotter.delay_left"
        const val DELAY_WRITE="com.gammaray.dotter.delay_write"

    }
}