package com.github.ghmxr.ftpshare.activities

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.support.v4.provider.DocumentFile
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.EditText
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.Toast
import com.github.ghmxr.ftpshare.R
import com.github.ghmxr.ftpshare.adapers.FtpFileListAdapter
import com.github.ghmxr.ftpshare.adapers.ViewHolderFtpFile
import com.github.ghmxr.ftpshare.data.ClientBean
import com.github.ghmxr.ftpshare.ftpclient.FtpClientManager
import com.github.ghmxr.ftpshare.ftpclient.FtpClientUtil
import com.github.ghmxr.ftpshare.utils.CommonUtils
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import java.io.InputStream
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class FtpClientActivity : BaseActivity() {

    companion object {
        @JvmStatic
        val EXTRA_UPLOAD_URIS = "extra_upload_uris"
    }

    private var client: FTPClient? = null

    private var bean: ClientBean? = null
    private var rv: RecyclerView? = null
    private var blankView: View? = null
    private var adapter: FtpFileListAdapter? = null
    private var pg: ProgressBar? = null
    private val pathFolders: LinkedList<String> = LinkedList()
    private var menu: Menu? = null

    private var uploadUris: List<Uri>? = null

    private var isReconnected = false

    private val scrollPositionCache = HashMap<String,Int>()


    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = resources?.getString(R.string.account_path_dialog_title)
        setContentView(R.layout.activity_ftp_client)
        bean = FtpClientManager.instance.getClientBeanOfId(intent.getIntExtra(EditClientActivity.EXTRA_CLIENT_BEAN_ID, -1))
        client = bean?.client
        supportActionBar?.title = bean?.nickName
        if (bean?.status != ClientBean.Status.CONNECTED) {
            makeBeanConnected()
        } else {
            init()
        }
    }

    private fun makeBeanConnected() {
        setProgressVisibility(true)
        bean?.connect { b, e ->
            if (!b) {
                var content = resources.getString(R.string.client_login_false)
                e?.let {
                    content = "$content:$it"
                }
                Toast.makeText(this, content, Toast.LENGTH_SHORT).show()
                finish()
                return@connect
            }
            init()
        }
    }

    override fun onBackPressed() {
        if (!backToLast()) super.onBackPressed()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_ftp_client_file, menu)
        this.menu = menu
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == R.id.action_close) {
            setResult(RESULT_CANCELED)
            finish()
        }
        if (item?.itemId == android.R.id.home) {
            onBackPressed()
        }
        if (item?.itemId == R.id.action_new_folder) {
            createAlertDialogBuilder(resources.getString(R.string.word_new_folder), resources.getString(R.string.word_new_folder), resources.getString(R.string.dialog_new_folder_hint))
                    .show().apply {
                        getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                            val s = this.findViewById<EditText>(R.id.dialog_edittext)?.text.toString()
                            if (s.isEmpty() || s.trim().isEmpty() || !CommonUtils.isALegalFileName(s)) {
                                Toast.makeText(this@FtpClientActivity, resources.getString(R.string.dialog_rename_error), Toast.LENGTH_SHORT).show()
                                return@setOnClickListener
                            }
                            cancel()
                            setProgressVisibility(true)
                            FtpClientUtil.createFolder(client!!, getFullPath(), s) {
                                it?.let {
                                    if(isFinishing)return@let
                                    AlertDialog.Builder(this@FtpClientActivity)
                                            .setTitle(resources.getString(R.string.word_error))
                                            .setMessage(resources.getString(R.string.dialog_new_folder_error).format(s, it.toString()))
                                            .setPositiveButton(resources.getString(R.string.dialog_button_confirm)) { _, _ -> }
                                            .show()
                                }
                                refreshList()
                            }
                        }

                    }
        }
        if (item?.itemId == R.id.action_delete) {
            val deleteList = adapter?.getSelectedItems() ?: ArrayList(0)
            var max = deleteList.size.coerceAtMost(100)
            val b: StringBuilder = StringBuilder()
            for (i in 0 until max) {
                b.append(deleteList[i].name)
                b.append("\n\n")
            }
            AlertDialog.Builder(this).setTitle(resources.getString(R.string.dialog_delete_head))
                    .setMessage(resources.getString(R.string.dialog_delete_message).format(b.toString()))
                    .setPositiveButton(resources.getString(R.string.dialog_button_confirm)) { _, _ ->
                    }
                    .setNegativeButton(resources.getString(R.string.dialog_button_cancel)) { _, _ ->
                    }
                    .show().apply {
                        getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                            cancel()
                            setProgressVisibility(true)
                            FtpClientUtil.deleteFTPFiles(client!!, getFullPath(), deleteList.toTypedArray()) {
                                if (it.isNotEmpty()) {
                                    AlertDialog.Builder(this@FtpClientActivity)
                                            .setTitle(resources.getString(R.string.word_error))
                                            .setMessage(resources.getString(R.string.dialog_delete_message2).format(it))
                                            .setPositiveButton(resources.getString(R.string.dialog_button_confirm)) { _, _ -> }
                                            .show()
                                }
                                refreshList()
                            }
                        }
                    }
        }
        if (item?.itemId == R.id.action_download) {
            downloadSelectedFiles(adapter?.getSelectedItems()?.toTypedArray()
                    ?: ArrayList<FTPFile>().toTypedArray())
        }
        if (item?.itemId == R.id.action_upload) {
            if (uploadUris?.isNotEmpty() == true) {
                val fileNames = ArrayList<String>()
                val inputStreams = ArrayList<InputStream>().apply {
                    uploadUris?.let { uploadUris ->
                        for (uri in uploadUris) {
                            contentResolver.openInputStream(uri)?.let {
                                add(it)
                                val s = uri.lastPathSegment
                                val fileName =/*s?.substring(s.lastIndexOf("/")+1)?:""*/DocumentFile.fromSingleUri(this@FtpClientActivity, uri)?.name
                                        ?: "aaa"
                                fileNames.add(fileName)
                            }
                        }
                    }
                }
                uploadFiles(inputStreams, fileNames)
            } else {
                Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "*/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                    startActivityForResult(this, 0)
                }
            }
        }
        if (item?.itemId == R.id.action_select_all) {
            adapter?.setSelectedState(true)
        }
        if (item?.itemId == R.id.action_select_all_off) {
            adapter?.setSelectedState(false)
        }
        return super.onOptionsItemSelected(item)
    }

    private fun uploadFiles(inputStreams: List<InputStream>, fileNames: List<String>) {
        FtpClientUtil.obtainAndStartUploadFilesTask(this, client!!, if (pathFolders.size > 0) getFullPath() else "", fileNames, inputStreams) {
            refreshList()
            uploadUris = null
            setResult(RESULT_OK)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0 && resultCode == RESULT_OK) {
            data?.data?.let {
                uploadFiles(ArrayList<InputStream>().apply {
                    contentResolver.openInputStream(it)?.let {
                        add(it)
                    }
                }, ArrayList<String>().apply {
                    val name = DocumentFile.fromSingleUri(this@FtpClientActivity, it)?.name
                            ?: "aaaa"
                    add(name)
                })
            }
        }
        if (requestCode == 1 && resultCode == RESULT_OK) {
            data?.getStringExtra(FolderSelectorActivity.EXTRA_SELECTED_PATH)?.let { path ->
                selectedDownloadFtpFiles?.let { files ->
                    FtpClientUtil.obtainAndStartDownloadFtpFilesTask(this@FtpClientActivity, client!!,
                            if (pathFolders.size > 0) getFullPath() else "", files, path) {
                        if (it.isNullOrBlank()) {
                            adapter?.isMultiSelectMode = false
                            setMenuItemsVisibility(false)
                        }
                    }
                }

            }
        }
    }

    private fun setMenuItemsVisibility(isMultiSelectMode: Boolean) {
        menu?.let {
            it.getItem(1).setVisible(!isMultiSelectMode)
            it.getItem(2).setVisible(isMultiSelectMode)
            it.getItem(3).setVisible(isMultiSelectMode)
            it.getItem(4).setVisible(!isMultiSelectMode)
            it.getItem(5).setVisible(isMultiSelectMode)
            it.getItem(6).setVisible(isMultiSelectMode)
        }
    }

    private var selectedDownloadFtpFiles: Array<FTPFile>? = null
    private fun downloadSelectedFiles(files: Array<FTPFile>) {
        selectedDownloadFtpFiles = files
        if (bean?.downloadConfirm == true) {
            /*val dialog = DialogOfFolderSelector(this@FtpClientActivity, bean!!.downloadPath)
            dialog.setOnFolderSelectorDialogConfirmedListener { path ->

            }
            dialog.show()*/
            bean?.let {
                Intent(this, FolderSelectorActivity::class.java).apply {
                    putExtra(FolderSelectorActivity.EXTRA_CURRENT_PATH, it.downloadPath)
                    startActivityForResult(this, 1)
                }
            }

        } else {
            FtpClientUtil.obtainAndStartDownloadFtpFilesTask(this@FtpClientActivity, client!!,
                    if (pathFolders.size > 0) getFullPath() else "", files, bean?.downloadPath
                    ?: "") {
                if (it.isNullOrBlank()) {
                    adapter?.isMultiSelectMode = false
                    setMenuItemsVisibility(false)
                }
            }
        }
    }

    var lm:LinearLayoutManager?=null

    private fun init() {
        uploadUris = intent.getParcelableArrayListExtra(EXTRA_UPLOAD_URIS)
        if (uploadUris?.isNotEmpty() == true) {
            Toast.makeText(this, resources.getString(R.string.share_select_folder), Toast.LENGTH_LONG).show()
        }
        pg = findViewById(R.id.ftp_client_pg)
        rv = findViewById(R.id.ftp_client_rv)
        blankView = findViewById(R.id.client_no_content_att)
        lm=LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        rv?.layoutManager = lm
        adapter = FtpFileListAdapter(this, null, object : FtpFileListAdapter.AdapterCallback {
            override fun onCdClicked() {
                backToLast()
            }

            override fun onMultiSelectModeTurnedOn() {
                setMenuItemsVisibility(true)
            }

            override fun onItemClicked(f: FTPFile?, h: ViewHolderFtpFile) {
                if (f?.isDirectory == true) {
                    cacheFirstItemPosition=lm?.findFirstVisibleItemPosition()?:0
                    scrollPositionCache[getFullPath()] = cacheFirstItemPosition
                    pathFolders.addLast(f.name)
                    refreshList()
                } else if (f?.isFile == true) {
                    onMoreClicked(f, h)
                }
            }

            override fun onMoreClicked(f: FTPFile?, h: ViewHolderFtpFile) {
                var values: IntArray?
                PopupWindow().apply {
                    contentView = LayoutInflater.from(this@FtpClientActivity).inflate(R.layout.popup_ftp_file_item, null, false)
                    setBackgroundDrawable(ColorDrawable())
                    width = CommonUtils.dip2px(this@FtpClientActivity, 100F)
                    height = -2
                    isOutsideTouchable = true
                    values = CommonUtils.calculatePopWindowPos(h.more, contentView)
                    contentView.findViewById<View>(R.id.popup_download).setOnClickListener {
                        downloadSelectedFiles(Array(1) { f!! })
                        this.dismiss()
                    }
                    contentView.findViewById<View>(R.id.popup_rename).setOnClickListener {
                        createAlertDialogBuilder(resources.getString(R.string.dialog_rename_head), f?.name
                                ?: "", resources.getString(R.string.dialog_rename_hint))
                                .show().apply {
                                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                                        val name = findViewById<EditText>(R.id.dialog_edittext)?.text.toString()
                                        if (!CommonUtils.isALegalFileName(name)) {
                                            Toast.makeText(this@FtpClientActivity, resources.getString(R.string.dialog_rename_error), Toast.LENGTH_SHORT).show()
                                            return@setOnClickListener
                                        }
                                        FtpClientUtil.renameFTPFile(client!!, getFullPath(), f!!, name) { b, e ->
                                            cancel()
                                            if (b) {
                                                refreshList()
                                            } else {
                                                AlertDialog.Builder(this@FtpClientActivity)
                                                        .setTitle(resources.getString(R.string.word_error))
                                                        .setMessage(resources.getString(R.string.dialog_rename_error2) + (if ((e?.toString()
                                                                        ?: "").isNotEmpty()) ":" else ""))
                                                        .setPositiveButton(resources.getString(R.string.dialog_button_confirm)) { _, _ -> }
                                                        .show()
                                            }

                                        }
                                    }
                                }
                        dismiss()
                    }

                    contentView.findViewById<View>(R.id.popup_delete).setOnClickListener {
                        AlertDialog.Builder(this@FtpClientActivity)
                                .setTitle(resources.getString(R.string.dialog_delete_head))
                                .setMessage(resources.getString(R.string.dialog_delete_message).format(" ${f?.name ?: ""}"))
                                .setPositiveButton(resources.getString(R.string.dialog_button_confirm)) { _, _ -> }
                                .setNegativeButton(resources.getString(R.string.dialog_button_cancel)) { _, _ -> }
                                .show().apply {
                                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                                        this.cancel()
                                        setProgressVisibility(true)
                                        FtpClientUtil.deleteFTPFiles(client!!, getFullPath(), Array(1) { f!! }) {
                                            if (it.isNotEmpty()) {
                                                AlertDialog.Builder(this@FtpClientActivity)
                                                        .setTitle(resources.getString(R.string.word_error))
                                                        .setMessage(resources.getString(R.string.dialog_delete_message2).format(it))
                                                        .setPositiveButton(resources.getString(R.string.dialog_button_confirm)) { _, _ -> }
                                                        .show()
                                            }
                                            refreshList()
                                        }
                                    }
                                }
                        dismiss()
                    }
                }.showAtLocation(h.more, Gravity.START or Gravity.TOP, values?.get(0)
                        ?: 0, values?.get(1) ?: 0)
            }
        })
        rv?.adapter = adapter
        rv?.addOnScrollListener(object :RecyclerView.OnScrollListener(){
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                //在这里进行第二次滚动（最后的100米！）
                //在这里进行第二次滚动（最后的100米！）
                if (move) {
                    move = false
                    //获取要置顶的项在当前屏幕的位置，mIndex是记录的要置顶项在RecyclerView中的位置
                    val n: Int = cacheFirstItemPosition - (lm?.findFirstVisibleItemPosition()?:0)
                    if (0 <= n && n < rv?.getChildCount()?:0) {
                        //获取要置顶的项顶部离RecyclerView顶部的距离
                        val top: Int = rv?.getChildAt(n)?.getTop()?:0
                        //最后的移动
                        rv?.scrollBy(0, top)
                    }
                }
            }
        })
        setProgressVisibility(false)
        refreshList()
    }

    private fun refreshList() {
        setProgressVisibility(true)
        client?.let {
            FtpClientUtil.listFiles(it, getFullPath()) { l ->
                if (pathFolders.size == 0 && l.isNullOrEmpty() && !isReconnected) {
                    isReconnected = true
                    bean?.disconnect {
                        makeBeanConnected()
                    }
                }
                adapter?.setData(l)
                adapter?.isMultiSelectMode = false
                setMenuItemsVisibility(false)
                setProgressVisibility(false)
                blankView?.visibility = if (l.isNullOrEmpty()) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
                adapter?.showCd = pathFolders.size != 0
                moveToPosition(scrollPositionCache[getFullPath()]?:0)
            }
        }
    }

    var move=false
    var cacheFirstItemPosition=0

    private fun moveToPosition(n: Int) {
        //先从RecyclerView的LayoutManager中获取第一项和最后一项的Position
        val firstItem: Int = lm?.findFirstVisibleItemPosition()?:0
        val lastItem: Int = lm?.findLastVisibleItemPosition()?:0
        //然后区分情况
        if (n <= firstItem) {
            //当要置顶的项在当前显示的第一个项的前面时
            rv?.scrollToPosition(n)
        } else if (n <= lastItem) {
            //当要置顶的项已经在屏幕上显示时
            val top: Int = rv?.getChildAt(n - firstItem)?.getTop()?:0
            rv?.scrollBy(0, top)
        } else {
            //当要置顶的项在当前显示的最后一项的后面时
            rv?.scrollToPosition(n)
            //这里这个变量是用在RecyclerView滚动监听里面的
            move = true
        }
    }

    private fun backToLast(): Boolean {
        if (adapter?.isMultiSelectMode == true) {
            adapter?.isMultiSelectMode = false
            setMenuItemsVisibility(false)
            return true
        }
        if (pathFolders.size > 0) {
            scrollPositionCache.remove(getFullPath())
            pathFolders.removeLast()
            refreshList()
            return true
        }
        return false
    }

    private fun getFullPath(): String {
        val b = StringBuilder("/")
        for (i in 0 until pathFolders.size) {
            b.append(pathFolders[i])
            b.append("/")
        }
        return b.toString()
    }

    private fun setProgressVisibility(b: Boolean) {
        rv?.visibility = if (b) View.GONE else View.VISIBLE
        pg?.visibility = if (b) View.VISIBLE else View.GONE
        blankView?.visibility = View.GONE
    }

    private fun createAlertDialogBuilder(title: String, s: String, h: String) = AlertDialog.Builder(this).apply {
        setTitle(title)
        this.setView(LayoutInflater.from(this@FtpClientActivity).inflate(R.layout.layout_with_edittext, null).apply {
            this.findViewById<EditText>(R.id.dialog_edittext).setText(s)
            this.findViewById<EditText>(R.id.dialog_edittext).hint = h
        })
        setPositiveButton(resources.getString(R.string.dialog_button_confirm), null)
        setNegativeButton(resources.getString(R.string.dialog_button_cancel)) { _, _ -> }
    }
}