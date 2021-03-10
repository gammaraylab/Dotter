package com.gammaray.dotter.Services

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.*
import android.util.Log
import com.gammaray.dotter.Activity.BluetoothActivity
import com.gammaray.dotter.Activity.BluetoothActivity.Companion.FILE_PATH
import com.gammaray.dotter.Activity.MainActivity
import com.gammaray.dotter.ConnectedThread
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.Thread.sleep

class BluetoothService :Service(){
    private var  mConnectedThread : ConnectedThread? = null
    private var  mBTAdapter:BluetoothAdapter?=null
    private var  mBTSocket: BluetoothSocket? = null

//    private val TAG:String="BluetoothService"
    private val BT_MODULE_UUID: java.util.UUID = java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // "random" unique identifier
    private var address:String?=null
    private var name:String?=null
    private var path: String? = null

    companion object{
        var lineNo:Int=0
        var isThreadDead=true

    }
    @ExperimentalUnsignedTypes
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        address=intent?.getStringExtra(BluetoothActivity.ADDRESS)
        name=intent?.getStringExtra(BluetoothActivity.NAME)
        path=intent?.getStringExtra(FILE_PATH)
        if(path==null)
            stopSelf()
        if(address!=null)
            init(address!!)
        return START_NOT_STICKY
    }
    override fun onCreate() {
        mBTAdapter = BluetoothAdapter.getDefaultAdapter() // get a handle on the bluetooth radio
        super.onCreate()
    }
    private fun createBluetoothSocket(device: BluetoothDevice): BluetoothSocket {
        try {
            val  m: java.lang.reflect.Method = device.javaClass.getMethod("createInsecureRfcommSocketToServiceRecord", java.util.UUID::class.java)
            return m.invoke(device, BT_MODULE_UUID) as BluetoothSocket
        }catch (e: java.lang.Exception){
            log("Could not create Insecure RFComm Connection")
        }
        return device.createRfcommSocketToServiceRecord(BT_MODULE_UUID)
    }
    private fun init(address:String){
        object : java.lang.Thread(){
            override  fun run(){
                var  fail = false
                val device: BluetoothDevice = mBTAdapter!!.getRemoteDevice(address)
                log("init"+device.address)
                try {
                    log("trying to create socket")
                    mBTSocket = createBluetoothSocket(device)
                }catch (e: IOException){
                    fail = true
                    log("Socket creation failed")
                }
                try {
                    log("trying to connect bluetooth socket")
                    mBTSocket!!.connect()
                }catch (e: IOException){
                    try {
                        fail = true
                        log("connection failed, closing socket")
                        mBTSocket!!.close()
                    }catch (e2: IOException){
                        log("could not close Socket")
                    }
                }
                if (!fail){
                    mConnectedThread = ConnectedThread(mBTSocket!!)
                    mConnectedThread!!.start()
                    log("Socket created successfully")
                    sleep(500)
                    printData()
                }
            }
        }.start()
    }
    @ExperimentalUnsignedTypes
    private fun printData(){
        Thread {
            if(isThreadDead) {
                isThreadDead = false
                try {
                    val image: Bitmap = BitmapFactory.decodeFile(path)
                    val data = convert(image)
                    sleep(1000)
                    val width = image.width/8
                    val height = image.height
                    log("height=$height, width=$width")

                    if (mConnectedThread != null && data?.isEmpty() == false) {
                        doHandShake('a', width)
                        for( i in 0 until height) {
                            if (isThreadDead)
                                break
                            waitUntilReady()
                            for (j in 0 until width) {
//                                sleep(0,400)
                                mConnectedThread!!.write(ByteArray(1) { data[width * i + j].toByte() })
                            }
                            log("${(i*100).toDouble()/height.toDouble()}%")
                        }
                    }
                } catch (e: NullPointerException) {
                    e.printStackTrace()
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                } catch (e: IOException){
                    e.printStackTrace()
//                    updateUI()
                }finally {
                    isThreadDead = true
                }
            }
//            updateUI()
        }.start()
    }
    @Throws(IOException::class)
    private fun doHandShake(value: Char, width: Int){   //does the handshake and sends the width
        mConnectedThread!!.write(value.toInt())
        sleep(200)
        val x=mConnectedThread?.read()
        if(x!=value.toInt()){
            log("handshake failed")
            stopSelf()
        }
        mConnectedThread!!.write(width) //sending the width of image
    }
    private fun waitUntilReady(){
        var x:Int?
        while(true){              //wait until POSITIVE_ACKNOWLEDGEMENT is not received
            x=mConnectedThread?.read()
            if(x=='k'.toInt())
                break
            else if(x==-1) {
                MainActivity.toast(" acknowledgement not received, error:$x")
                stopSelf()
            }
            sleep(200)
        }
    }
    @kotlin.ExperimentalUnsignedTypes
    private fun convert(image: Bitmap):ArrayList<UByte>? {
        val width=image.width
        val height=image.height
        if(width%8!=0){
            MainActivity.toast("width of image exceeds by ${width % 8} pixel!!")
            return null
        }
        val output=ArrayList<UByte>()
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

                output.add(binary.toString().toUByte(2))//.toInt())
                binary.clear()
                col -=8
            }        //end while
        }

        return output
    }

    private fun log(message: String, TAG: String = "MESSAGE"){
        Log.e(TAG, message)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        mBTSocket?.close()
        isThreadDead=true
        log("service is destroyed")
        super.onDestroy()
    }

}