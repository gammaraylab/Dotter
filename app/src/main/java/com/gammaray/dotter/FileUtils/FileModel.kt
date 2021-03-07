package com.gammaray.dotter.FileUtils

import org.apache.commons.io.FilenameUtils
import java.io.File

data class FileModel(
        val path: String,
        val fileType: FileType,
        val name: String,
        val sizeInB: Long,
        val extension: String = "",
        val subFiles: Int = 0,
        val parent:String?=File(path).parent) {
    val sizeInKB:Double=sizeInB.toDouble()/1024
    val sizeInMB=sizeInKB/1024
    val sizeInGB=sizeInMB/1024
}