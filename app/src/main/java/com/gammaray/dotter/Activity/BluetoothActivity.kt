package com.gammaray.dotter.Activity

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.appcompat.app.AppCompatActivity
import com.gammaray.dotter.ConnectedThread
import com.gammaray.dotter.R
import kotlinx.android.synthetic.main.activity_bluetooth2.*
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.Thread.sleep
import java.security.SecureRandom
import kotlin.random.Random

@SuppressLint("SetTextI18n")
class BluetoothActivity  : AppCompatActivity(){
    private  val  TAG: String = BluetoothActivity::class.java.simpleName

    private var sent=0
    private val sb=java.lang.StringBuilder()

    private var  mBluetoothStatus: TextView? = null
    private var  mListPairedDevicesBtn: Button? = null
    private var  mDevicesListView: ListView? = null

    private var  mBTAdapter: BluetoothAdapter? = null
    private var  mPairedDevices: Set<BluetoothDevice>? = null
    private var  mBTArrayAdapter: ArrayAdapter<String>? = null
    private var  mHandler : Handler? = null
    private var  mConnectedThread : ConnectedThread? = null
    private var  mBTSocket: BluetoothSocket? = null

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

        mHandler = object : Handler(Looper.getMainLooper()){
            override  fun handleMessage(msg: Message){
                if (msg.what == CONNECTING_STATUS){
                    if (msg.arg1 == 1)
                        mBluetoothStatus!!.text = "Connected to Device: " + msg.obj
                    else
                        mBluetoothStatus!!.text="Connection Failed"
                }
                else if(msg.what == MESSAGE_READ ) {
                    val message = String(msg.obj as ByteArray, 0, msg.arg1);
                    Log.e("MessageReceived",message)
                }
            }
        }
        if (mBTArrayAdapter == null){
            mBluetoothStatus!!.text="Status: Bluetooth not found"
            Toast.makeText(applicationContext, "Bluetooth device not found!", Toast.LENGTH_SHORT).show()
        } else {
            var isThreadDead=true
            sendData.setOnClickListener {
                progressBar.visibility=View.VISIBLE
                progressPercent.visibility=View.VISIBLE
                stopData.visibility=View.VISIBLE

                progressBar.progress=0
                progressPercent.text="0%"
                Thread {
                    if(isThreadDead) {
                        isThreadDead = false
                        val path: String? = intent.getStringExtra(FILE_PATH)
                        try {
                            val image: Bitmap = BitmapFactory.decodeFile(path)
                            val data = convert(image)
                            val width = image.width/8
                            val height = image.height

                            if (mConnectedThread != null && data?.isEmpty() == false) {
                                doHandShake('a', width)
                                for (i in 0 until height) {
                                    if (isThreadDead)
                                        break
                                    waitUntilReady()
                                    for (j in 0 until width)
                                        mConnectedThread!!.write(data[width * i + j])
                                    stats(i, height)
                                }
                            }
                        } catch (e: NullPointerException) {
                            e.printStackTrace()
                        } catch (e: FileNotFoundException) {
                            e.printStackTrace()
                        }
                        isThreadDead = true
                    }
                    updateUI()
                }.start()
            }
            stopData.setOnClickListener {
                isThreadDead=true
                updateUI()
            }
            mListPairedDevicesBtn?.setOnClickListener {
                if(mBTAdapter?.isEnabled == false)
                    mBTAdapter?.enable()
                listPairedDevices() }
        }
    }

    private fun stats(i: Int, height: Int){
        runOnUiThread {
            sent = (100 * (i + 1) / height)
            sb.append(sent)
            sb.append("%")
            progressPercent.text = sb.toString()
            sb.clear()
            progressBar.progress = sent
        }
    }

    private fun updateUI(){
        sleep(1000)
        runOnUiThread{
            if(mBTSocket?.isConnected==true)
                mBTSocket?.close()
            mBluetoothStatus!!.text = "not connected"
            stopData.visibility=View.GONE
        }
    }
    private fun doHandShake(value: Char, width: Int){   //does the handshake and sends the width
        mConnectedThread!!.write(value.toInt())
        sleep(100)
        if(mConnectedThread?.read()!=value.toInt()){
            runOnUiThread{MainActivity.toast("handshake failed")}
            finish()
        }
        mConnectedThread!!.write(width) //sending the width of image
    }
    private fun waitUntilReady(){
        var ready=false
        while(!ready){              //wait until POSITIVE_ACKNOWLEDGEMENT is not received
            ready=mConnectedThread?.read()=='k'.toInt()
            sleep(200)
        }
    }
    override fun onPause() {
        super.onPause()
        if(mBTSocket?.isConnected==true)
            mBTSocket?.close()
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
            mBTAdapter?.enable()
            sleep(1000)
            listPairedDevices()
        }
    }
    private val mDeviceClickListener: OnItemClickListener = object : OnItemClickListener{
        override  fun onItemClick(parent: AdapterView<*>?, view: View, position: Int, id: Long){
            if (!mBTAdapter!!.isEnabled){
                Toast.makeText(baseContext, "Bluetooth not on", Toast.LENGTH_SHORT).show()
                return
            }
            mBluetoothStatus!!.text = "Connecting..."
            // Get the device MAC address, which is the last 17 chars in the View
            val info: String = (view as TextView).text.toString()
            val  address: String = info.substring(info.length - 17)
            val  name: String = info.substring(0, info.length - 17)

            // Spawn a new thread to avoid blocking the GUI one
            object : java.lang.Thread(){
                override  fun run(){
                    var  fail = false
                    val device: BluetoothDevice = mBTAdapter!!.getRemoteDevice(address)
                    try {
                        mBTSocket = createBluetoothSocket(device)
                    }catch (e: IOException){
                        fail = true
                        Toast.makeText(baseContext, "Socket creation failed", Toast.LENGTH_SHORT).show()
                    }
                    try {
                        mBTSocket!!.connect()
                    }catch (e: IOException){
                        try {
                            fail = true
                            mBTSocket!!.close()
                            mHandler!!.obtainMessage(CONNECTING_STATUS, -1, -1)
                                .sendToTarget()
                        }catch (e2: IOException){
                            Toast.makeText(baseContext, "Socket creation failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                    if (!fail){
                        mConnectedThread = ConnectedThread((mBTSocket)!!, (mHandler)!!)
                        mConnectedThread!!.start()
                        mHandler!!.obtainMessage(CONNECTING_STATUS, 1, -1, name)
                            .sendToTarget()
                        runOnUiThread{
                            sendData.visibility=View.VISIBLE
                        }
                    }
                }
            }.start()
        }
    }
    @Throws(IOException::class) private   fun createBluetoothSocket(device: BluetoothDevice): BluetoothSocket {
        try {
            val  m: java.lang.reflect.Method = device.javaClass.getMethod("createInsecureRfcommSocketToServiceRecord", java.util.UUID::class.java)
            return m.invoke(device, BT_MODULE_UUID) as BluetoothSocket
        }catch (e: java.lang.Exception){
            Log.e(TAG, "Could not create Insecure RFComm Connection", e)
        }
        return device.createRfcommSocketToServiceRecord(BT_MODULE_UUID)
    }
    @kotlin.ExperimentalUnsignedTypes
    private fun convert(image: Bitmap):ArrayList<Int>? {
        val width=image.width
        val height=image.height
        if(width%8!=0){
            MainActivity.toast( "width of image exceeds by ${width % 8} pixel!!")
            return null
        }
        val output=ArrayList<Int>()
        val binary=StringBuilder()
        var col:Int
        for (row in 0 until height) {
            col=width-1
            while(col>=7) {
                for(i in col downTo col-7){
                    if(image.getPixel(i, row) ==-16777216)
                        binary.append("1")
                    else
                        binary.append("0")
                }   //end for

                output.add(binary.toString().toUByte(2).toInt())
                binary.clear()
                col -=8
            }        //end while
        }

        return output
    }
    companion object  {
        private val BT_MODULE_UUID: java.util.UUID = java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // "random" unique identifier
        private const val REQUEST_ENABLE_BT: Int = 1 // used to identify adding bluetooth names
        const val MESSAGE_READ: Int = 2 // used in bluetooth handler to identify message update
        private const val CONNECTING_STATUS: Int = 3 // used in bluetooth handler to identify message status
        const val FILE_PATH="com.gammaray.dotter.file_path"
    }
}