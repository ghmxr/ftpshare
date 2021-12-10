package com.github.ghmxr.ftpshare.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import android.widget.AdapterView.OnItemSelectedListener
import androidx.appcompat.app.AlertDialog
import com.github.ghmxr.ftpshare.R
import com.github.ghmxr.ftpshare.utils.CommonUtils
import com.github.ghmxr.ftpshare.utils.StorageUtil
import java.io.File
import java.util.*

open class FolderSelectorActivity : BaseActivity() {

    companion object {
        @JvmStatic
        val EXTRA_CURRENT_PATH: String = "current_path"

        @JvmStatic
        val EXTRA_SELECTED_PATH: String = "selected_path"
    }

    private var file: File? = null
    private val list: ArrayList<File> = ArrayList()
    private var spinner: Spinner? = null
    private var listView: ListView? = null
    private var adapter: FileListAdapter? = null
    private val positionRecords = Bundle()
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        setContentView(R.layout.layout_dialog_folder_selector)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = resources?.getString(R.string.account_path_dialog_title)
        spinner = findViewById<Spinner>(R.id.folder_storage)
        listView = findViewById<ListView>(R.id.folder_list)
        val path = intent.getStringExtra(EXTRA_CURRENT_PATH)
        if (path == null) {
            Toast.makeText(this, "invalid selected path", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        file = try {
            File(path)
        } catch (e: Exception) {
            e.printStackTrace()
            File(StorageUtil.getMainStoragePath())
        }
        try {
            val storages: List<String>
            storages = if (Build.VERSION.SDK_INT >= 19) {
                StorageUtil.getAvailableStoragePaths(this)
            } else {
                StorageUtil.getAvailableStoragePaths()
            }
            spinner?.setAdapter(ArrayAdapter(this, R.layout.item_spinner_storage, R.id.item_storage_text, storages))
            for (i in storages.indices) {
                if (CommonUtils.isChildPathOfCertainPath(file, File(storages[i]))) {
                    spinner?.setSelection(i)
                    break
                }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        adapter = FileListAdapter()
        listView?.setAdapter(adapter)

        refreshPath(file!!)
        listView?.setOnItemClickListener(OnItemClickListener { parent, view, position, id ->
            if (file != null) positionRecords.putInt(file!!.absolutePath.toLowerCase(), listView?.getFirstVisiblePosition()
                    ?: 0)
            if (position == 0) {
                backtoParent()
                return@OnItemClickListener
            }
            refreshPath(list[position - 1])
        })
        spinner?.setOnItemSelectedListener(object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
                val selected = spinner?.getSelectedItem() as String
                if (!CommonUtils.isChildPathOfCertainPath(file, File(selected))) {
                    refreshPath(File(selected))
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        })
    }

    private fun backtoParent() {
        try {
            val parent_file = file!!.parentFile
            if (parent_file != null) refreshPath(parent_file) else finish()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.action_cancel -> {
                setResult(RESULT_CANCELED)
                finish()
            }
            R.id.action_confirm -> {
                Intent().apply {
                    putExtra(EXTRA_SELECTED_PATH, file?.absolutePath ?: "")
                    setResult(RESULT_OK, this)
                    finish()
                }
            }
            R.id.action_mkdir -> {
                AlertDialog.Builder(this)
                        .setTitle(resources.getString(R.string.word_new_folder))
                        .setView(LayoutInflater.from(this).inflate(R.layout.layout_with_edittext, null))
                        .setPositiveButton(resources.getString(R.string.dialog_button_confirm), null)
                        .setNegativeButton(resources.getString(R.string.dialog_button_cancel)) { _, _ -> }
                        .show().apply {
                            findViewById<EditText>(R.id.dialog_edittext)?.setText(resources.getString(R.string.word_new_folder))
                            getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                                val name = findViewById<EditText>(R.id.dialog_edittext)?.text.toString()
                                if (name.isNullOrBlank() || !CommonUtils.isALegalFileName(name)) {
                                    Toast.makeText(this@FolderSelectorActivity, resources.getString(R.string.dialog_rename_error), Toast.LENGTH_SHORT).show()
                                    return@setOnClickListener
                                }
                                try {
                                    file?.let {
                                        File("${it.absolutePath}/$name").mkdirs()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(this@FolderSelectorActivity, e.toString(), Toast.LENGTH_SHORT).show()
                                }
                                file?.let {
                                    refreshPath(it)
                                }
                                cancel()
                            }
                        }
            }
            android.R.id.home -> {
                backtoParent()
            }
            else -> {

            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        backtoParent()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_folder_selector, menu)
        return super.onCreateOptionsMenu(menu)
    }

    private fun refreshPath(file: File) {
        this.file = file
        list.clear()
        adapter!!.notifyDataSetChanged()
        findViewById<View>(R.id.folder_load).setVisibility(View.VISIBLE)
        Thread {
            synchronized(this@FolderSelectorActivity) {
                val fileArrayList = ArrayList<File>()
                try {
                    val files = file.listFiles()
                    for (currentFile in files) {
                        if (currentFile.isDirectory) fileArrayList.add(currentFile)
                    }
                    Collections.sort(fileArrayList)
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                } finally {
                    handler.post(Runnable {
                        list.clear()
                        list.addAll(fileArrayList)
                        listView!!.adapter = FileListAdapter()
                        (findViewById<View>(R.id.folder_path) as TextView).setText(this@FolderSelectorActivity.file?.getAbsolutePath())
                        findViewById<View>(R.id.folder_load).setVisibility(View.GONE)
                        if (this@FolderSelectorActivity.file != null) {
                            listView!!.setSelection(positionRecords.getInt(this@FolderSelectorActivity.file?.getAbsolutePath()?.toLowerCase()))
                        }
                    })
                }
            }
        }.start()
    }

    private inner class FileListAdapter : BaseAdapter() {
        override fun getCount(): Int {
            return list.size + 1
        }

        override fun getItem(position: Int): Any? {
            return null
        }

        override fun getItemId(position: Int): Long {
            return 0
        }

        override fun getView(position: Int, v: View?, parent: ViewGroup): View {
            var convertView = v
            if (convertView == null) {
                convertView = LayoutInflater.from(this@FolderSelectorActivity).inflate(R.layout.item_folder, parent, false)
            }
            if (position == 0) {
                (convertView?.findViewById<View>(R.id.item_folder_name) as TextView).text = ".."
                return convertView
            }
            (convertView?.findViewById<View>(R.id.item_folder_name) as TextView).setText(list.get(position - 1).getName())
            return convertView
        }
    }
}