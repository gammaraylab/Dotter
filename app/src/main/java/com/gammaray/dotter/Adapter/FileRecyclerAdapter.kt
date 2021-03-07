package com.gammaray.dotter.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gammaray.dotter.FileUtils.FileModel
import com.gammaray.dotter.FileUtils.FileType
import com.gammaray.dotter.R
import kotlinx.android.synthetic.main.item_recycler_file.view.*
import java.lang.StringBuilder
import java.util.*


class FilesRecyclerAdapter : RecyclerView.Adapter<FilesRecyclerAdapter.ViewHolder>() {

    var onItemClickListener: ((FileModel) -> Unit)? = null

    var filesList = listOf<FileModel>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recycler_file, parent, false)
        return ViewHolder(view)
    }
    override fun getItemCount() = filesList.size
    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bindView(position)
    fun updateData(filesList: List<FileModel>) {
        this.filesList = filesList
        notifyDataSetChanged()
    }
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        init {
            itemView.setOnClickListener(this)
        }
        override fun onClick(v: View?) {
            onItemClickListener?.invoke(filesList[adapterPosition])
        }
        fun bindView(position: Int) {
            val fileModel = filesList[position]
            itemView.nameTextView.text = fileModel.name
            val sb=StringBuilder()

            if (fileModel.fileType == FileType.FOLDER) {
                itemView.thumbnailImageview.setImageResource(R.drawable.ic_folder_grey)
                itemView.folderTextView.visibility = View.VISIBLE
                itemView.totalSizeTextView.visibility = View.GONE
                sb.clear()
                sb.append("(")
                sb.append(fileModel.subFiles)
                sb.append(" files)")
                itemView.folderTextView.text = sb
            } else {
                when(fileModel.extension.toLowerCase(Locale.ROOT)){
                    "txt"->itemView.thumbnailImageview.setImageResource(R.drawable.ic_text_file)
                    "pdf"->itemView.thumbnailImageview.setImageResource(R.drawable.ic_pdf_file)
                    "jpg"->itemView.thumbnailImageview.setImageResource(R.drawable.ic_image_file)
                    "jpeg"->itemView.thumbnailImageview.setImageResource(R.drawable.ic_image_file)
                    "png"->itemView.thumbnailImageview.setImageResource(R.drawable.ic_image_file)
                    "mp4"->itemView.thumbnailImageview.setImageResource(R.drawable.ic_video_file)
                    "mkv"->itemView.thumbnailImageview.setImageResource(R.drawable.ic_video_file)
                    "avi"->itemView.thumbnailImageview.setImageResource(R.drawable.ic_video_file)
                    "mp3"->itemView.thumbnailImageview.setImageResource(R.drawable.ic_music_file)
                    "m4a"->itemView.thumbnailImageview.setImageResource(R.drawable.ic_music_file)
                    "doc"->itemView.thumbnailImageview.setImageResource(R.drawable.ic_doc_file)
                    "docx"->itemView.thumbnailImageview.setImageResource(R.drawable.ic_docx_file)
                    "html"->itemView.thumbnailImageview.setImageResource(R.drawable.ic_html_file)
                    "htm"->itemView.thumbnailImageview.setImageResource(R.drawable.ic_html_file)
                    "bmp"->itemView.thumbnailImageview.setImageResource(R.drawable.ic_bitmap_file)

                    else->itemView.thumbnailImageview.setImageResource(R.drawable.ic_unknown_file)
                }

                itemView.folderTextView.visibility = View.GONE
                itemView.totalSizeTextView.visibility = View.VISIBLE
                sb.clear()

                if(fileModel.sizeInGB>=1) {
                    sb.append(String.format("%.2f", fileModel.sizeInGB))
                    sb.append(" GB")
                }
                else if(fileModel.sizeInMB>=1) {
                    sb.append(String.format("%.2f", fileModel.sizeInMB))
                    sb.append(" MB")
                }
                else if(fileModel.sizeInKB>=1) {
                    sb.append(String.format("%.2f", fileModel.sizeInKB))
                    sb.append(" KB")
                }
                else {
                    sb.append(fileModel.sizeInB)
                    sb.append(" Byte")
                }

                itemView.totalSizeTextView.text = sb
            }
        }
    }
}