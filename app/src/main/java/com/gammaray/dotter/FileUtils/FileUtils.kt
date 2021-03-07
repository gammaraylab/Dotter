package com.gammaray.dotter.FileUtils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.gammaray.dotter.Activity.BluetoothActivity
import com.gammaray.dotter.Activity.MainActivity
import java.io.File

    fun getFilesFromPath(path:String,showHiddenFiles:Boolean=false,onlyFolders:Boolean=false):List<File>?{
        val file=File(path)
        return file.listFiles()
            ?.filter { showHiddenFiles|| !it.name.startsWith(".") }
            ?.filter { !onlyFolders || it.isDirectory }
            ?.toList()
    }
    fun fileModelsFromFiles(files:List<File>):List<FileModel>{
        return files.map {
            FileModel(it.path, FileType.fileType(it),it.name,it.length(),it.extension,it.listFiles()?.size?:0)
        }
    }
    fun Context.launchFileIntent(fileModel: FileModel) {
        val intent = Intent(this,BluetoothActivity::class.java)
        intent.putExtra(BluetoothActivity.FILE_PATH,fileModel.path)
        startActivity(intent)
    }