package com.gammaray.dotter.Activity

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.*
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.appcompat.app.AppCompatActivity
import com.gammaray.dotter.ConnectedThread
import com.gammaray.dotter.R
import com.gammaray.dotter.Services.BluetoothService
import kotlinx.android.synthetic.main.activity_bluetooth2.*
import java.io.IOException
import java.lang.Thread.sleep

@SuppressLint("SetTextI18n")
class BluetoothActivity  : AppCompatActivity(){
//    private  val  TAG: String = BluetoothActivity::class.java.simpleName

    private var sent=0
    private val sb=java.lang.StringBuilder()

    private var  mBluetoothStatus: TextView? = null
    private var  mListPairedDevicesBtn: Button? = null
    private var  mDevicesListView: ListView? = null

    private var  mBTAdapter: BluetoothAdapter? = null
    private var  mPairedDevices: Set<BluetoothDevice>? = null
    private var  mBTArrayAdapter: ArrayAdapter<String>? = null
    private var  mConnectedThread : ConnectedThread? = null
    private var  mBTSocket: BluetoothSocket? = null

    private lateinit var mIntent:Intent

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

        mIntent=Intent(this,BluetoothService::class.java)
//        mBluetoothStatus!!.text = "Connected to Device: " + msg.obj
//        mBluetoothStatus!!.text="Connection Failed"
        if (mBTArrayAdapter == null){
            mBluetoothStatus!!.text="Bluetooth not found"
            Toast.makeText(applicationContext, "Bluetooth device not found!", Toast.LENGTH_SHORT).show()
        } else {
            sendData.setOnClickListener {
                progressBar.visibility=View.VISIBLE
                progressPercent.visibility=View.VISIBLE
                stopData.visibility=View.VISIBLE

                progressBar.progress=0
                progressPercent.text="0%"
                val path: String? = intent?.getStringExtra(FILE_PATH)
                mIntent.putExtra(FILE_PATH,path)
                startService(mIntent)
            }
            stopData.setOnClickListener {
                stopService(intent)
                updateUI()
            }
            mListPairedDevicesBtn?.setOnClickListener {
                if(mBTAdapter?.isEnabled == false)
                    mBTAdapter?.enable()
                listPairedDevices() }
        }
    }

    private fun stats(line: Int, height: Int){
        runOnUiThread {
            sent = (100 * (line + 1) / height)
            sb.append(sent)
            sb.append("%")
            progressPercent.text = sb.toString()
            sb.clear()
            progressBar.progress = sent
        }
    }

    private fun updateUI(){
        runOnUiThread{
            if(mBTSocket?.isConnected==true)
                mBTSocket?.close()
            mBluetoothStatus!!.text = "not connected"
            stopData.visibility=View.GONE
        }
    }
    @Throws(IOException::class)
    private fun doHandShake(value: Char, width: Int){   //does the handshake and sends the width
        mConnectedThread!!.write(value.toInt())
        sleep(200)
        val x=mConnectedThread?.read()
        if(x!=value.toInt()){
            runOnUiThread{MainActivity.toast("handshake failed, $x received")}
            updateUI()
            finish()
        }
        else
            runOnUiThread{MainActivity.toast("handshake succeeded")}

        mConnectedThread!!.write(width) //sending the width of image
    }

    private fun waitUntilReady(){
//        MainActivity.toast(" acknowledgement not received, error:$x")
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
            sleep(10)
            mBTAdapter?.enable()
            sleep(1200)
            listPairedDevices()
        }
    }
    private val mDeviceClickListener: OnItemClickListener = object : OnItemClickListener{
        override  fun onItemClick(parent: AdapterView<*>?, view: View, position: Int, id: Long){
            if (!mBTAdapter!!.isEnabled){
                Toast.makeText(baseContext, "Bluetooth not on", Toast.LENGTH_SHORT).show()
                return
            }
//            mBluetoothStatus!!.text = "Connecting..."
            val info: String = (view as TextView).text.toString()
            val  address: String = info.substring(info.length - 17)
            val  name: String = info.substring(0, info.length - 17)

            mIntent.putExtra(ADDRESS,address)
            mIntent.putExtra(NAME,name)

            sendData.visibility=View.VISIBLE
        }
    }
    companion object  {
        private const val REQUEST_ENABLE_BT: Int = 1 // used to identify adding bluetooth names
        const val FILE_PATH="com.gammaray.dotter    .file_path"
        const val ADDRESS="com.gammaray.dotter.address"
        const val NAME="com.gammaray.dotter.name"
    }
}