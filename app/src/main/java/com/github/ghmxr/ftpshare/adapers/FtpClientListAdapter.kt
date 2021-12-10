package com.github.ghmxr.ftpshare.adapers

import android.annotation.SuppressLint
import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.github.ghmxr.ftpshare.R
import com.github.ghmxr.ftpshare.data.ClientBean
import com.github.ghmxr.ftpshare.ftpclient.FtpClientManager

class FtpClientListAdapter(private val context: Context, private val c: ItemCallback?) : RecyclerView.Adapter<ViewHolder>() {

    interface ItemCallback {
        fun onItemLongClick(i: Int, b: ClientBean, a: FtpClientListAdapter, h: ViewHolder)
        fun onItemClick(i: Int, b: ClientBean, a: FtpClientListAdapter, h: ViewHolder)
        fun onActionButtonClicked(b: ClientBean, a: FtpClientListAdapter, h: ViewHolder)
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_ftp_client, p0, false))
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(p0: ViewHolder, p1: Int) {
        val clientBean = FtpClientManager.instance.getClientBeanOfId(p0.adapterPosition)
        p0.apply {
            iconStatus.visibility = if (clientBean.status == ClientBean.Status.CONNECTED) View.VISIBLE else View.GONE
            progressBar.visibility = if (clientBean.status == ClientBean.Status.CONNECTING || clientBean.status == ClientBean.Status.DISCONNECTING) View.VISIBLE else View.GONE
            tvAction.visibility = if (clientBean.status == ClientBean.Status.CONNECTING || clientBean.status == ClientBean.Status.DISCONNECTING) View.GONE else View.VISIBLE
            tvName.text = clientBean.nickName
            tvAddress.text = "ftp://${clientBean.host}:${clientBean.port}"
            tvAction.text = context.resources.getString(if (clientBean.status == ClientBean.Status.CONNECTED) R.string.word_disconnect else R.string.word_connect)
            tvAction.setOnClickListener {
                c?.onActionButtonClicked(clientBean, this@FtpClientListAdapter, p0)
            }
            /*p0.progressBar.setOnClickListener{
                c?.onActionButtonClicked(clientBean,this@FtpClientListAdapter,p0)
            }*/
            p0.itemView.setOnLongClickListener {
                c?.onItemLongClick(p0.adapterPosition, clientBean, this@FtpClientListAdapter, p0)
                true
            }
            p0.itemView.setOnClickListener {
                c?.onItemClick(p0.adapterPosition, clientBean, this@FtpClientListAdapter, p0)
            }
        }
    }

    override fun getItemCount(): Int {
        return FtpClientManager.instance.getClientBeanSize()
    }
}

class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
    val iconStatus: ImageView = v.findViewById(R.id.item_server_status)
    val progressBar: ProgressBar = v.findViewById(R.id.item_server_progress)
    val tvName: TextView = v.findViewById(R.id.item_server_name)
    val tvAddress: TextView = v.findViewById(R.id.item_server_address)
    val tvAction: TextView = v.findViewById(R.id.item_server_action)
}