package com.gammaray.dotter

import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.os.SystemClock
import com.gammaray.dotter.Activity.BluetoothActivity
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer

class ConnectedThread(mmSocket: BluetoothSocket, private val mHandler: Handler) : Thread() {
    private val mmInStream: InputStream?
    private val mmOutStream: OutputStream?
    override fun run() {
        val buffer= ByteArray(64)
        var bytes: Int // bytes returned from read()
        while (true) {
            try {
                bytes = mmInStream!!.available()
                if (bytes != 0) {
                    SystemClock.sleep(300) //pause and wait for rest of data. Adjust this depending on your sending speed.
                    bytes = mmInStream.available() // how many bytes are ready to be read?
                    bytes = mmInStream.read(buffer, 0, bytes) // record how many bytes we actually read
                    mHandler.obtainMessage(BluetoothActivity.MESSAGE_READ, bytes, -1, buffer).sendToTarget() // Send the obtained bytes to the UI activity
                }
            }catch (e: IOException) {
                e.printStackTrace()
                break
            }catch (e:ArrayIndexOutOfBoundsException){
                e.printStackTrace()
                break
            }
        }
    }
    fun write(int: Int) {
        try {
            mmOutStream!!.write(int)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    fun read():Int{
        try{
            return mmInStream!!.read()
        }catch (e:IOException){
            e.printStackTrace()
        }
        return -1
    }

    init {
        var tmpIn: InputStream? = null
        var tmpOut: OutputStream? = null

        try {
            tmpIn = mmSocket.inputStream
            tmpOut = mmSocket.outputStream
        } catch (e: IOException) {
        }
        mmInStream = tmpIn
        mmOutStream = tmpOut
    }
}