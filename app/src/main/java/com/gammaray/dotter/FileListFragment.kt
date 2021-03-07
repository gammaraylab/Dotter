package com.gammaray.dotter

import android.content.Context
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.gammaray.dotter.Adapter.FilesRecyclerAdapter
import com.gammaray.dotter.FileUtils.FileModel
import com.gammaray.dotter.FileUtils.fileModelsFromFiles
import com.gammaray.dotter.FileUtils.getFilesFromPath
import kotlinx.android.synthetic.main.fragment_file_list.*


class FileListFragment : Fragment() {
    private lateinit var filesAdapter: FilesRecyclerAdapter
    private lateinit var path:String
    private lateinit var callBack:OnItemClickListener
    private lateinit var fileChangeBroadcastReceiver: FileChangedBroadcastReceiver

    interface OnItemClickListener{
        fun onClick(fileModel: FileModel)
    }

    companion object{
        private const val ARG_PATH="com.gammaray.dotter.fileslist.path"
        fun build(block:Builder.()->Unit)=Builder().apply(block).build()
    }

    class Builder{
        var path=""
        fun build():FileListFragment{
            val fragment=FileListFragment()
            val args=Bundle()
            args.putString(ARG_PATH,path)
            fragment.arguments=args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val filePath = arguments?.getString(ARG_PATH)
        if (filePath == null) {
            Toast.makeText(context, "Path should not be null!", Toast.LENGTH_SHORT).show()
            return
        }
        path = filePath
        fileChangeBroadcastReceiver=FileChangedBroadcastReceiver(path){
            updateDate()
        }
    }

    override fun onResume() {
        super.onResume()
        context?.registerReceiver(fileChangeBroadcastReceiver,IntentFilter(FileChangedBroadcastReceiver.EXTRA_PATH))
    }

    override fun onPause() {
        super.onPause()
        context?.unregisterReceiver(fileChangeBroadcastReceiver)
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_file_list,container,false)
    }
    override fun onAttach(context: Context) {
        super.onAttach(context)
        try{
            callBack=context as OnItemClickListener
        }catch (e: Exception){
            throw Exception("$context should implement FileListFragment.OnItemClickListener")
        }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }
    private fun initViews() {
        filesRecyclerView.layoutManager = LinearLayoutManager(context)
        filesAdapter = FilesRecyclerAdapter()
        filesRecyclerView.adapter = filesAdapter
        filesAdapter.onItemClickListener={
            callBack.onClick(it)
        }

        updateDate()
    }
    private fun updateDate() {
        val list= getFilesFromPath(path) ?: return
        val files = fileModelsFromFiles(list)
        if (files.isEmpty())
            emptyFolderLayout.visibility = View.VISIBLE
        else
            emptyFolderLayout.visibility = View.INVISIBLE

        filesAdapter.updateData(files)
    }
}