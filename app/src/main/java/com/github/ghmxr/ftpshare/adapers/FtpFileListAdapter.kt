package com.github.ghmxr.ftpshare.adapers

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import com.github.ghmxr.ftpshare.R
import com.github.ghmxr.ftpshare.utils.CommonUtils
import org.apache.commons.net.ftp.FTPFile

class FtpFileListAdapter(val context: Context, list: Array<FTPFile>?, private val c: AdapterCallback?) : RecyclerView.Adapter<ViewHolderFtpFile>() {
    private val dataList: ArrayList<FTPFile> = ArrayList()

    interface AdapterCallback {
        fun onCdClicked()
        fun onItemClicked(f: FTPFile?, h: ViewHolderFtpFile)
        fun onMoreClicked(f: FTPFile?, h: ViewHolderFtpFile)
        fun onMultiSelectModeTurnedOn()
    }

    private var currentLongClick: Int = -1

    public var adapterCallback: AdapterCallback? = null

    private var selected: Array<Boolean>? = null

    var isMultiSelectMode: Boolean = false
        set(value) {
            field = value
            if (field) {
                selected = Array(dataList.size) {
                    false
                }
            }
            selected?.set(currentLongClick, true)
            notifyDataSetChanged()
        }

    var showCd: Boolean = true
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    init {
        setData(list)
    }

    public fun setData(a: Array<FTPFile>?) {
        dataList.clear()
        a?.asList()?.let {
            dataList.addAll(it)
        }
        notifyDataSetChanged()
    }

    public fun addData(a: Array<FTPFile>?) {
        a?.asList()?.let {
            dataList.addAll(it)
        }
        notifyDataSetChanged()
    }

    fun getSelectedItems() = ArrayList<FTPFile>().apply {
        selected?.let {
            for (i in it.indices) {
                if (it[i]) this.add(dataList[i])
            }
        }

    }

    fun setSelectedState(b:Boolean){
        selected?.fill(b)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int) = ViewHolderFtpFile(LayoutInflater.from(context).inflate(R.layout.item_ftp_file, p0, false))

    override fun onBindViewHolder(p0: ViewHolderFtpFile, p1: Int) {
        p0.apply {
            if (showCd) {
                if (p0.adapterPosition == 0) {
                    icon.setImageResource(R.drawable.ic_folder)
                    tvName.text = ".."
                    tvDate.text = ""
                    tvSize.text = ""
                    tvSize.visibility = View.GONE
                    cb.visibility = View.GONE
                    p0.itemView.setOnClickListener {
                        c?.onCdClicked()
                    }
                    more.visibility = View.GONE
                } else {
                    bindItem(p0.adapterPosition - 1, p0)
                }

            } else {
                bindItem(p0.adapterPosition, p0)
            }

        }

    }

    private fun bindItem(i: Int, p0: ViewHolderFtpFile) {
        p0.apply {
            val ftpFile: FTPFile = dataList[i]
            icon.setImageResource(if (ftpFile.isFile) R.drawable.ic_file else R.drawable.ic_folder)
            tvName.text = ftpFile.name
            tvDate.text = CommonUtils.getDisplayTimeOfMillis(ftpFile.timestamp?.timeInMillis
                    ?: 0L)
            tvSize.text = Formatter.formatFileSize(context, ftpFile.size)
            tvSize.visibility = if (ftpFile.isDirectory) View.GONE else if (isMultiSelectMode) View.GONE else View.VISIBLE
            cb.visibility = if (isMultiSelectMode) View.VISIBLE else View.GONE
            cb.isChecked = isMultiSelectMode && selected?.get(i) == true
            more.visibility = if (isMultiSelectMode) View.GONE else View.VISIBLE
            itemView.setOnClickListener {
                if (isMultiSelectMode) {
                    selected?.set(i, selected?.get(i) != true)
                    notifyItemChanged(p0.adapterPosition)
                    return@setOnClickListener
                }
                c?.onItemClicked(ftpFile, p0)
            }
            itemView.setOnLongClickListener(object : View.OnLongClickListener {
                override fun onLongClick(v: View?): Boolean {
                    if (isMultiSelectMode) {
                        return true
                    }
                    if (showCd && p0.adapterPosition == 0) {
                        return true
                    }
                    currentLongClick = i
                    isMultiSelectMode = true
                    c?.onMultiSelectModeTurnedOn()
                    return true
                }
            })
            more.setOnClickListener {
                c?.onMoreClicked(ftpFile, p0)
            }
        }


    }

    override fun getItemCount(): Int {
        return if (showCd) 1 + dataList.size else dataList.size
    }
}

class ViewHolderFtpFile(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val icon: ImageView = itemView.findViewById(R.id.item_ftp_file_icon)
    val tvName: TextView = itemView.findViewById(R.id.item_ftp_file_name)
    val tvDate: TextView = itemView.findViewById(R.id.item_ftp_file_info)
    val tvSize: TextView = itemView.findViewById(R.id.item_ftp_file_info2)
    val cb: CheckBox = itemView.findViewById(R.id.item_ftp_file_cb)
    val more: ImageView = itemView.findViewById(R.id.item_ftp_file_more)
}