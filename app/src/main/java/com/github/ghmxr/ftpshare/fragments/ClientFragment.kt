package com.github.ghmxr.ftpshare.fragments

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import com.github.ghmxr.ftpshare.MyApplication
import com.github.ghmxr.ftpshare.R
import com.github.ghmxr.ftpshare.activities.EditClientActivity
import com.github.ghmxr.ftpshare.activities.FtpClientActivity
import com.github.ghmxr.ftpshare.adapers.FtpClientListAdapter
import com.github.ghmxr.ftpshare.adapers.ViewHolder
import com.github.ghmxr.ftpshare.data.ClientBean
import com.github.ghmxr.ftpshare.ftpclient.FtpClientManager
import com.github.ghmxr.ftpshare.utils.CommonUtils
import java.util.*


class ClientFragment : Fragment() {

    private var recyclerView: RecyclerView? = null
    private var adapter: FtpClientListAdapter? = null
    private var sharingUris: List<Uri>? = null

    private var noClientView: View? = null

    companion object {
        @JvmStatic
        val ACTION_REFRESH_LIST = "${MyApplication.getGlobalBaseContext().packageName}:refresh_client_list"
    }

    private val refreshListReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            adapter?.notifyDataSetChanged()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context?.let {
            try {
                it.registerReceiver(refreshListReceiver, IntentFilter(ACTION_REFRESH_LIST))
            } catch (e: Exception) {
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return LayoutInflater.from(context).inflate(R.layout.fragment_client, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.client_rv)
        noClientView = view.findViewById(R.id.add_client_att)
        recyclerView?.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        context?.let { it ->
            adapter = FtpClientListAdapter(it, object : FtpClientListAdapter.ItemCallback {
                override fun onItemLongClick(i: Int, b: ClientBean, a: FtpClientListAdapter, h: ViewHolder) {
                    var values: IntArray? = null
                    PopupWindow().apply {
                        contentView = LayoutInflater.from(it).inflate(R.layout.popup_client_item, null, false)
                        setBackgroundDrawable(ColorDrawable())
                        width = CommonUtils.dip2px(it, 100F)
                        height = -2
                        isOutsideTouchable = true
                        contentView?.findViewById<View>(R.id.popup_edit)?.setOnClickListener {
                            Intent(context, EditClientActivity::class.java).apply {
                                if (context is Activity) {
                                    putExtra(EditClientActivity.EXTRA_CLIENT_BEAN_ID, b._id)
                                    startActivityForResult(this, 0)
                                }
                                dismiss()
                            }
                        }
                        contentView?.findViewById<View>(R.id.popup_delete)?.setOnClickListener {
                            AlertDialog.Builder(context!!)
                                    .setTitle(context!!.resources.getString(R.string.client_delete_confirm_head))
                                    .setMessage(String.format(context!!.resources.getString(R.string.client_delete_confirm_content), b.nickName))
                                    .setPositiveButton(context!!.resources.getString(R.string.dialog_button_confirm)) { dialog, which ->
                                        dialog.cancel()
                                        FtpClientManager.instance.deleteClientBean(b)
                                        adapter?.notifyDataSetChanged()
                                        refreshNoClientAtt()
                                    }
                                    .setNegativeButton(context!!.resources.getString(R.string.dialog_button_cancel)) { _, _ ->
                                    }.show()
                            dismiss()
                        }
                        values = CommonUtils.calculatePopWindowPos(h.tvAction, contentView)
                    }.showAtLocation(h.tvAction, Gravity.TOP or Gravity.START, values?.get(0)
                            ?: 0, values?.get(1) ?: 0)
                }

                override fun onItemClick(i: Int, b: ClientBean, a: FtpClientListAdapter, h: ViewHolder) {
                    activity?.let { activity ->
                        startActivityForResult(Intent(activity, FtpClientActivity::class.java).apply {
                            putExtra(EditClientActivity.EXTRA_CLIENT_BEAN_ID, b._id)
                            sharingUris?.let {
                                putParcelableArrayListExtra(FtpClientActivity.EXTRA_UPLOAD_URIS, ArrayList<Uri>().apply {
                                    addAll(it)
                                })
                            }
                        }, 0)
                    }

                }

                override fun onActionButtonClicked(b: ClientBean, a: FtpClientListAdapter, h: ViewHolder) {
                    if (b.status == ClientBean.Status.CONNECTED) {
                        b.disconnect {
                            a.notifyItemChanged(h.adapterPosition)
                        }
                    } else if (b.status == ClientBean.Status.DISCONNECTED) {
                        b.connect { bb, e ->
                            activity?.let {
                                var snackbarContent = resources.getString(R.string.client_login_false)
                                if (e != null) {
                                    snackbarContent = "$snackbarContent:$e"
                                    Snackbar.make(it.findViewById(android.R.id.content), snackbarContent, Snackbar.LENGTH_SHORT).show()
                                } else if (!bb) {
                                    snackbarContent = resources.getString(R.string.client_login_false)
                                    Snackbar.make(it.findViewById(android.R.id.content), snackbarContent, Snackbar.LENGTH_SHORT).show()
                                }
                            }
                            a.notifyDataSetChanged()

                        }
                    } /*else if (b.status == ClientBean.Status.CONNECTING) {
                        b.disconnect {
                            a.notifyItemChanged(h.adapterPosition)
                        }
                    }*/

                    a.notifyItemChanged(h.adapterPosition)

                }
            })
            recyclerView?.adapter = adapter
        }
        refreshNoClientAtt()
    }

    private fun showConnectErrorSnackBar(b: Boolean, e: Exception?) {

    }

    override fun onResume() {
        super.onResume()
        adapter?.notifyDataSetChanged()
        refreshNoClientAtt()
    }

    fun processActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        //adapter?.notifyDataSetChanged()
        //refreshNoClientAtt()
        if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
            sharingUris = null
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        processActivityResult(requestCode, resultCode, data)
    }

    override fun onDestroy() {
        super.onDestroy()
        context?.let {
            try {
                it.unregisterReceiver(refreshListReceiver)
            } catch (e: Exception) {
            }
        }
    }

    fun setSharingUriAndView(uris: List<Uri>) {
        sharingUris = uris
    }

    private fun refreshNoClientAtt() {
        noClientView?.let {
            it.visibility = if (FtpClientManager.instance.getClientBeanSize() > 0) View.GONE else View.VISIBLE
        }
    }
}