package com.gammaray.dotter.Services

import android.app.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.graphics.get
import com.gammaray.dotter.Activity.BluetoothActivity
import com.gammaray.dotter.Activity.BluetoothActivity.Companion.HEIGHT
import com.gammaray.dotter.Activity.BluetoothActivity.Companion.UPDATE_CONVERSION_STATUS
import com.gammaray.dotter.Activity.BluetoothActivity.Companion.UPDATE_ERROR
import com.gammaray.dotter.Activity.BluetoothActivity.Companion.UPDATE_FLAG
import com.gammaray.dotter.Activity.BluetoothActivity.Companion.UPDATE_HANDSHAKE_INFO
import com.gammaray.dotter.Activity.BluetoothActivity.Companion.UPDATE_SENT
import com.gammaray.dotter.Activity.BluetoothActivity.Companion.UPDATE_BLUETOOTH_STATUS
import com.gammaray.dotter.Activity.BluetoothActivity.Companion.UPDATE_PRINTING_STATUS
import com.gammaray.dotter.Activity.MainActivity
import com.gammaray.dotter.ConnectedThread
import com.gammaray.dotter.R
import java.io.FileNotFoundException
import java.io.IOException

class PrintService : Service() {
    private var address:String?=null
    private var filePath:String?=null
    private var name:String?=null

    private var delayRight:Int=0
    private var delayLeft:Int=0
    private var delayWrite:Int=0

    private var mBTAdapter: BluetoothAdapter? = null
    private var mBTSocket: BluetoothSocket? = null
    private var mConnectedThread : ConnectedThread? = null
    private var notification:Notification?=null
    private var notificationBuilder:NotificationCompat.Builder?=null
    private var notificationManager:NotificationManager?=null

    private var totalSent=0.0
    private var totalTimeLeft=0
    private var minLeft=0
    private var secLeft=0
    private var rate=0

    private val updateBroadcastIntent=Intent(UPDATE_FLAG)

    @ExperimentalUnsignedTypes
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(intent!!.action== ACTION_STOP_PRINTING && intent.getBooleanExtra(EXTRA_NOTIFICATION_ID,false))
            endTask()

        filePath=intent.getStringExtra(BluetoothActivity.FILE_PATH)
        address=intent.getStringExtra(BluetoothActivity.ADDRESS)
        name= intent.getStringExtra(BluetoothActivity.NAME)

        delayRight=intent.getIntExtra(BluetoothActivity.DELAY_RIGHT,30)
        delayLeft=intent.getIntExtra(BluetoothActivity.DELAY_LEFT,18)
        delayWrite=intent.getIntExtra(BluetoothActivity.DELAY_WRITE,110)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createNotificationChannel(channelID,"dotter background service")
        notificationManager=getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        val stopPrintingIntent=Intent(this,PrintService::class.java).apply {
            action=ACTION_STOP_PRINTING
            putExtra(EXTRA_NOTIFICATION_ID,true)
        }
        val stopServicePendingIntent=PendingIntent.getService(this,12,stopPrintingIntent,0)

        notificationBuilder = NotificationCompat.Builder(this, channelID)
        notificationBuilder!!
                .setContentTitle(notificationTitle)
                .setContentText("calculating time...")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setProgress(100,0,false)
                .addAction(R.drawable.ic_stop_service,"Stop printing",stopServicePendingIntent)
        notification=notificationBuilder?.build()
        startForeground(1, notification)

        init()
        return START_NOT_STICKY
    }
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        log("service destroyed()")
        endTask()
    }
    override fun onCreate() {
        mBTAdapter = BluetoothAdapter.getDefaultAdapter() // get a handle on the bluetooth radio
        super.onCreate()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String){
        val chan = NotificationChannel(channelId,
                channelName, NotificationManager.IMPORTANCE_NONE)
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
    }
    private fun log(message:String){
        Log.e("BluetoothService",message)
    }

    private fun doHandShake(value: Char, width: Int){   //does the handshake and sends the width
        var x:Int?=0
        updateBroadcastIntent.putExtra(UPDATE_HANDSHAKE_INFO,"doing Handshake")
        sendBroadcast(updateBroadcastIntent)
        try {
            mConnectedThread!!.write(value.toInt())
            Thread.sleep(400)
            x= mConnectedThread?.read()
        }catch (e: IOException){
            e.printStackTrace()
            onDestroy()
        }
        if(x!=value.toInt()){
            updateBroadcastIntent.putExtra(UPDATE_HANDSHAKE_INFO,"handshake failed, $x received")
            updateBroadcastIntent.putExtra(UPDATE_ERROR,-2)
            sendBroadcast(updateBroadcastIntent)

            onDestroy()
        }
        else{
            updateBroadcastIntent.putExtra(UPDATE_HANDSHAKE_INFO,"handshake succeeded")
            sendBroadcast(updateBroadcastIntent)
        }
        try {
            mConnectedThread!!.write(width) //sending the width of image
            Thread.sleep(5)
            mConnectedThread!!.write(delayRight)
            Thread.sleep(5)
            mConnectedThread!!.write(delayLeft)
            Thread.sleep(5)
            mConnectedThread!!.write(delayWrite)

        }catch (e: IOException){
            e.printStackTrace()
        }
    }

    private fun waitUntilReady(){
        var ready=false
        var x:Int?=0
        try {
            while (!ready) {              //wait until POSITIVE_ACKNOWLEDGEMENT is not received
                x = mConnectedThread?.read()
                ready = x == 'k'.toInt()
                Thread.sleep(200)
            }
        }catch (e:IOException){
            e.printStackTrace()
            onDestroy()
        }
    }
    @ExperimentalUnsignedTypes
    private fun init(){
        updateBroadcastIntent.putExtra(UPDATE_BLUETOOTH_STATUS,"Creating socket")
        sendBroadcast(updateBroadcastIntent)
        var  fail = false
        try {
            val device: BluetoothDevice = mBTAdapter!!.getRemoteDevice(address)
            mBTSocket = createBluetoothSocket(device)
        }catch (e: IOException){
            fail = true
            updateBroadcastIntent.putExtra(UPDATE_BLUETOOTH_STATUS,"Socket creation failed")
            updateBroadcastIntent.putExtra(UPDATE_ERROR,-4)
            sendBroadcast(updateBroadcastIntent)
        }catch (e:IllegalArgumentException){
            e.printStackTrace()
        }
        try {
            mBTSocket!!.connect()
        }catch (e: IOException){
            try {
                fail = true
                mBTSocket!!.close()
            }catch (e2: IOException){
                updateBroadcastIntent.putExtra(UPDATE_BLUETOOTH_STATUS,"Socket creation failed")
                updateBroadcastIntent.putExtra(UPDATE_ERROR,-4)
                sendBroadcast(updateBroadcastIntent)
            }
        }
        if (!fail){
            mConnectedThread = ConnectedThread((mBTSocket)!!)
            mConnectedThread!!.start()
            updateBroadcastIntent.putExtra(UPDATE_BLUETOOTH_STATUS,"connected to $name")
            sendBroadcast(updateBroadcastIntent)
            Thread {
                printDot()
            }.start()
        }
    }
    @Throws(IOException::class)
    private   fun createBluetoothSocket(device: BluetoothDevice): BluetoothSocket {
        try {
            val  m: java.lang.reflect.Method = device.javaClass.getMethod("createInsecureRfcommSocketToServiceRecord", java.util.UUID::class.java)
            return m.invoke(device, BluetoothActivity.BT_MODULE_UUID) as BluetoothSocket
        }catch (e: java.lang.Exception){
            updateBroadcastIntent.putExtra(UPDATE_BLUETOOTH_STATUS,"Could not create Insecure RFComm Connection")
            updateBroadcastIntent.putExtra(UPDATE_ERROR,-3)
            sendBroadcast(updateBroadcastIntent)
            e.printStackTrace()
        }
        return device.createRfcommSocketToServiceRecord(BluetoothActivity.BT_MODULE_UUID)
    }
    @ExperimentalUnsignedTypes
    private fun printDot(){
        Thread {
            try {
                val image: Bitmap = BitmapFactory.decodeFile(filePath)
                val data = convert(image)
                val width = image.width/8
                val height:Int = image.height
                if (mConnectedThread != null && data.isNotEmpty()) {
                    doHandShake('a', width)
                    updateBroadcastIntent.putExtra(UPDATE_PRINTING_STATUS, "Image printing started")
                    sendBroadcast(updateBroadcastIntent)
                    rate=width*8*(delayWrite*10+delayRight*100+delayLeft*100)/1_000_000
                    totalTimeLeft=rate*height
                    minLeft=totalTimeLeft/60
                    secLeft=totalTimeLeft%60

                    notificationBuilder
                            ?.setContentText("${minLeft}M ${secLeft}S left")
                    notification=notificationBuilder?.build()
                    notificationManager?.notify(1,notification)

                    for (i in 0 until height) {
                        waitUntilReady()
                        for (j in 0 until width) {
                            mConnectedThread!!.write(data[width * i + j])
                        }
                        updateUI(i,height)
                    }
                    endTask()
                }
            } catch (e: NullPointerException) {
                e.printStackTrace()
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
                onDestroy()
            }
        }.start()
    }

    @kotlin.ExperimentalUnsignedTypes
    private fun convert(image: Bitmap):ArrayList<Int> {
        val width=image.width
        val height=image.height
        updateBroadcastIntent.putExtra(UPDATE_CONVERSION_STATUS,"converting image to array")
        updateBroadcastIntent.putExtra(HEIGHT,height)
        sendBroadcast(updateBroadcastIntent)
        if(width%8!=0){
            updateBroadcastIntent.putExtra(UPDATE_CONVERSION_STATUS,"image width exceeded by ${width % 8} pixel!")
            updateBroadcastIntent.putExtra(UPDATE_ERROR,-1)
            sendBroadcast(updateBroadcastIntent)
            onDestroy()
        }
        val output=ArrayList<Int>()
        val binary=StringBuilder()
        var col:Int
        for (row in 0 until height) {
            col=width-1
            while(col>=7) {
                for(i in col downTo col-7){
                    if(image[i, row] == -16777216)
                        binary.append("1")
                    else //if(image[i, row] == -1)
                        binary.append("0")
                }   //end for

                output.add(binary.toString().toUByte(2).toInt())
                binary.clear()
                col -=8
            }        //end while
        }
        updateBroadcastIntent.putExtra(UPDATE_CONVERSION_STATUS,"Image converted")
        sendBroadcast(updateBroadcastIntent)

        return output
    }
    private fun endTask(){
        if(mBTSocket?.isConnected==true)
            mBTSocket?.close()
        updateBroadcastIntent.putExtra(UPDATE_PRINTING_STATUS,"Image printing done")
        sendBroadcast(updateBroadcastIntent)
        stopSelf()
    }

    private fun updateUI(sent:Int,height:Int){
        totalSent=(100.0*sent.toDouble() / height.toDouble())
        totalTimeLeft-=rate
        minLeft=totalTimeLeft/60
        secLeft=totalTimeLeft%60
        notificationBuilder
                ?.setContentText("${minLeft}M ${secLeft}S left")
        notification=notificationBuilder?.build()
        notificationManager?.notify(1,notification)
        updateBroadcastIntent.putExtra(UPDATE_SENT,sent)
        sendBroadcast(updateBroadcastIntent)
    }
    companion object{
        const val ACTION_STOP_PRINTING="com.gammaray.dotter.action_stop_printing"
        const val EXTRA_NOTIFICATION_ID="om.gammaray.dotter.printing_service_notification_id"
        private const val channelID="com.gammaray.dotter.printing_service_notification_channel_id"
        private const val notificationTitle="0% printed"
    }
}