package com.gammaray.dotter.Activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.gammaray.dotter.Adapter.BreadcrumbsRecyclerAdapter
import com.gammaray.dotter.FileListFragment
import com.gammaray.dotter.FileUtils.FileModel
import com.gammaray.dotter.FileUtils.FileType
import com.gammaray.dotter.FileUtils.launchFileIntent
import com.gammaray.dotter.R
import com.gammaray.dotter.Services.BackStackManager
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), FileListFragment.OnItemClickListener {

    private val backStackManager= BackStackManager()
    private lateinit var breadcrumbsRecyclerAdapter: BreadcrumbsRecyclerAdapter
    private val READ_REQUEST_CODE=453

    companion object {
        private lateinit var instance:Context

        fun log(message:String)=Log.e("LOGGER",message)
        fun toast( message:String, context:Context= instance, length:Int=Toast.LENGTH_SHORT)=Toast.makeText(context,message,length).show()
    }
    init{
        instance =this
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if(savedInstanceState==null){
            val fileListFragment= FileListFragment.build {
                path = Environment.getExternalStorageDirectory().absolutePath
            }
            supportFragmentManager.beginTransaction()
                .add(R.id.container,fileListFragment)
                .addToBackStack(Environment.getExternalStorageDirectory().absolutePath)
                .commit()
        }

        checkPermissions()
        initViews()
        initBackStack()
    }
    override fun onClick(fileModel: FileModel) {
        if(fileModel.fileType== FileType.FOLDER)
            addFileFragment(fileModel)
        else
            launchFileIntent(fileModel)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        backStackManager.popFromStack()
        if (supportFragmentManager.backStackEntryCount == 0)
            finish()
    }

    private fun addFileFragment(fileModel: FileModel){
        val fileListFragment= FileListFragment.build {
            path = fileModel.path
        }
        backStackManager.addToStack(fileModel)

        val fragmentTransaction=supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.container, fileListFragment)
        fragmentTransaction.addToBackStack(fileModel.path)
        fragmentTransaction.commit()
    }
    private fun initBackStack(){
        backStackManager.onStackChangeListener={
            updateAdapterData(it)
        }
        backStackManager.addToStack(fileModel = FileModel(Environment.getExternalStorageDirectory().absolutePath, FileType.FOLDER,"Internal storage",0))
    }
    private fun initViews(){
        recyclerViewBreadcrumbs.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        breadcrumbsRecyclerAdapter = BreadcrumbsRecyclerAdapter()
        recyclerViewBreadcrumbs.adapter = breadcrumbsRecyclerAdapter
        breadcrumbsRecyclerAdapter.onItemClickListener = {
            supportFragmentManager.popBackStack(it.path, 2);
            backStackManager.popFromStackTill(it)
        }
    }
    private fun updateAdapterData(files:List<FileModel>){
        breadcrumbsRecyclerAdapter.updateData(files)
        if(files.isNotEmpty())
            recyclerViewBreadcrumbs.smoothScrollToPosition(files.size-1)
    }
    private fun checkPermissions() {
        val permissionRead =
                ContextCompat.checkSelfPermission(this, "android.permission.READ_EXTERNAL_STORAGE")
        val permissionWrite =
                ContextCompat.checkSelfPermission(this, "android.permission.WRITE_EXTERNAL_STORAGE")
        if (permissionRead == 0 && permissionWrite == 0)
            return
        requestPermission()
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(
                        "android.permission.READ_EXTERNAL_STORAGE",
                        "android.permission.WRITE_EXTERNAL_STORAGE"
                ), READ_REQUEST_CODE)
    }

     override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK)
            Log.i("TAG","${data?.data}")

    }

    override fun onRequestPermissionsResult(requestCode: Int, permission: Array<String>,grantResults: IntArray) = when {
        requestCode != READ_REQUEST_CODE -> super.onRequestPermissionsResult(requestCode, permission, grantResults)
        grantResults[0] != 0 -> requestPermission()
        else -> {
            finish()
            startActivity(intent)
        }
    }

}