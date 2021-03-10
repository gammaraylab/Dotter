package com.gammaray.dotter

import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.os.SystemClock
import com.gammaray.dotter.Activity.BluetoothActivity
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer

class ConnectedThread(mmSocket: BluetoothSocket) : Thread() {
    private val mmInStream: InputStream?
    private val mmOutStream: OutputStream?

    @Throws(IOException::class)
    fun write(int: Int) {
        mmOutStream!!.write(int)
    }
    fun write(bytes: ByteArray) {
        mmOutStream!!.write(bytes)
    }

    @Throws(IOException::class)
    fun read():Int?{
            return mmInStream?.read()
    }

    init {
        var tmpIn: InputStream? = null
        var tmpOut: OutputStream? = null

        try {
            tmpIn = mmSocket.inputStream
            tmpOut = mmSocket.outputStream
        } catch (e: IOException) {
            e.printStackTrace()
        }
        mmInStream = tmpIn
        mmOutStream = tmpOut
    }
}