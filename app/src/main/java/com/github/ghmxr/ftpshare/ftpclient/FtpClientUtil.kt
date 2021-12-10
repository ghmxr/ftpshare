package com.github.ghmxr.ftpshare.ftpclient

import android.content.Context
import android.os.Looper
import android.support.v7.app.AlertDialog
import android.util.Log
import android.widget.Toast
import com.github.ghmxr.ftpshare.MyApplication
import com.github.ghmxr.ftpshare.R
import com.github.ghmxr.ftpshare.ui.ProgressDialog
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import org.apache.commons.net.ftp.FTPReply
import java.io.*
import java.nio.charset.Charset

object FtpClientUtil {

    fun listFiles(client: FTPClient, path: String, c: ((Array<FTPFile>?) -> Unit)?) {
        Thread {
            try {
                val list = client.listFiles(path)
                MyApplication.handler.post {
                    c?.invoke(list)
                }
            } catch (e: Exception) {
                MyApplication.handler.post {
                    c?.invoke(null)
                }
            }
        }.start()
    }

    fun obtainAndStartDownloadFtpFilesTask(
            context: Context, client: FTPClient, parentPath: String, ftpFiles: Array<FTPFile>,
            destinationPath: String, callback: ((String?) -> Unit)?,
    ): DownloadFtpFilesTask? {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Log.e("FtpClientUtil", "没有在主线程调用下载逻辑obtainAndStartDownloadFtpFilesTask")
            return null
        }
        val dialog = ProgressDialog(context, context.resources.getString(R.string.dialog_download_title))
        var thread: DownloadFtpFilesTask? = null
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, context.resources.getString(R.string.dialog_button_cancel)) { _, _ ->callback?.invoke(null)
            thread?.flag = true
            dialog.cancel()
        }
        dialog.setContentText(context.resources.getString(R.string.dialog_download_initial))
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        val c = object : DownloadFtpFilesTask.DownloadTaskCallback {
            override fun onFileProgress(resPath: String, dstPath: String) {
                dialog.setProgressIndeterminate(false)
                dialog.setContentText(context.resources.getString(R.string.dialog_download1) + dstPath)
            }

            override fun onProgress(progress: Long, total: Long) {
                dialog.setProgress(progress, total)
            }

            override fun onSpeed(s: Long) {
                dialog.setSpeed(s)
            }

            override fun onCompleted(errorInfo: String) {
                dialog.cancel()
                if (errorInfo.isEmpty()) {
                    Toast.makeText(context, context.resources.getString(R.string.dialog_download_complete), Toast.LENGTH_SHORT).show()
                } else {
                    AlertDialog.Builder(context)
                            .setTitle(context.resources.getString(R.string.dialog_download_error_head2))
                            .setMessage(context.resources.getString(R.string.dialog_download_error_message).format(errorInfo))
                            .setPositiveButton(context.resources.getString(R.string.dialog_button_confirm)) { _, _ -> }
                            .show()
                }
                callback?.invoke(errorInfo)
            }

            override fun onError(e: Exception) {
                dialog.cancel()
                AlertDialog.Builder(context).setTitle(context.resources.getString(R.string.dialog_download_error_head))
                        .setMessage(String.format(context.resources.getString(R.string.dialog_download_error_message), e.toString()))
                        .setPositiveButton(context.resources.getString(R.string.dialog_button_confirm)) { dialog, which -> }
                        .show()
            }
        }
        dialog.show()
        thread = DownloadFtpFilesTask(client, parentPath, ftpFiles, destinationPath, c).apply { start() }
        return thread
    }

    fun obtainAndStartUploadFilesTask(context: Context, client: FTPClient, parentPath: String, fileNames: List<String>, fileInputStreams: List<InputStream>, callback: ((String?) -> Unit)?): UploadFtpFilesTask? {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Log.e("FtpClientUtil", "没有在主线程调用下载逻辑obtainAndStartDownloadFtpFilesTask")
            return null
        }
        val dialog = ProgressDialog(context, context.resources.getString(R.string.dialog_upload_title))
        var thread: UploadFtpFilesTask? = null
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, context.resources.getString(R.string.dialog_button_cancel)) { _, _ ->
            thread?.flag = true
            dialog.cancel()
            callback?.invoke(null)
        }
        dialog.setContentText(context.resources.getString(R.string.dialog_download_initial))
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        val c = object : UploadFtpFilesTask.UploadTaskCallback {
            override fun onFileUploading(fullPath: String) {
                dialog.setProgressIndeterminate(false)
                dialog.setContentText(context.resources.getString(R.string.dialog_upload1) + fullPath)
            }

            override fun onProgress(progress: Long, total: Long) {
                dialog.setProgress(progress, total)
            }

            override fun onSpeed(speed: Long) {
                dialog.setSpeed(speed)
            }

            override fun onComplete(errorInfo: String) {
                dialog.cancel()
                if (errorInfo.isEmpty()) {
                    Toast.makeText(context, context.resources.getString(R.string.dialog_upload_complete), Toast.LENGTH_SHORT).show()
                } else {
                    AlertDialog.Builder(context)
                            .setTitle(context.resources.getString(R.string.dialog_download_error_head2))
                            .setMessage(context.resources.getString(R.string.dialog_upload_error_message).format(errorInfo))
                            .setPositiveButton(context.resources.getString(R.string.dialog_button_confirm)) { _, _ -> }
                            .show()
                }
                callback?.invoke(errorInfo)
            }

            override fun onError(e: Exception) {
                dialog.cancel()
                AlertDialog.Builder(context).setTitle(context.resources.getString(R.string.dialog_download_error_head))
                        .setMessage(String.format(context.resources.getString(R.string.dialog_upload_error_message), e.toString()))
                        .setPositiveButton(context.resources.getString(R.string.dialog_button_confirm)) { dialog, which -> }
                        .show()
                callback?.invoke(e.toString())
            }
        }
        dialog.show()
        thread = UploadFtpFilesTask(client, parentPath, fileNames, fileInputStreams, c).apply { start() }
        return thread
    }

    fun renameFTPFile(client: FTPClient, path: String, f: FTPFile, nf: String, c: ((Boolean, Exception?) -> Unit)?) {
        Thread {
            try {
                client.changeWorkingDirectory(path)
                val b = client.rename(f.name, nf)
                MyApplication.handler.post {
                    c?.invoke(b, null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                MyApplication.handler.post {
                    c?.invoke(false, e)
                }
            }

        }.start()
    }

    /**
     * @param path ends with "/"
     */
    fun deleteFTPFiles(client: FTPClient, path: String, files: Array<FTPFile>, c: ((String) -> Unit)?) {
        Thread {
            val s = StringBuilder()
            try {
                for (f in files) {
                    try {
                        deleteFTPFileOrFolder(client, path, f)
                    } catch (e: Exception) {
                        s.append(e.toString())
                        s.append("\n\n")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                s.append(e.toString())
                s.append("\n\n")
            } finally {
                MyApplication.handler.post {
                    c?.invoke(s.toString())
                }
            }
        }.start()
    }

    fun createFolder(client: FTPClient, path: String, name: String, c: ((Exception?) -> Unit)?) {
        Thread {
            try {
                var e: Exception? = null
                client.changeWorkingDirectory(path)
                val result = client.mkd(/*String(name.toByteArray(Charset.forName("GBK")), Charsets.ISO_8859_1)*/name)
                if (!FTPReply.isPositiveCompletion(result)) {
                    e = Exception("reply code: $result")
                }
                MyApplication.handler.post {
                    c?.invoke(e)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                MyApplication.handler.post {
                    c?.invoke(e)
                }
            }
        }.start()
    }

    private fun deleteFTPFileOrFolder(client: FTPClient, path: String, f: FTPFile) {
        if (f.isDirectory) {
            val childPath = "$path/${f.name}"
            val l = client.listFiles(childPath)
            l?.let {
                for (ff in l) {
                    deleteFTPFileOrFolder(client, childPath, ff)
                }
            }
            client.changeWorkingDirectory(path)
            client.removeDirectory(f.name)
        } else if (f.isFile) {
            client.changeWorkingDirectory(path)
            val reply = client.dele(f.name)
            if (!FTPReply.isPositiveCompletion(reply)) {
                throw RuntimeException("$path/${f.name} delete failed, reply code $reply")
            }
        }
    }

    fun getFileOrFolderSize(path: String, client: FTPClient, f: FTPFile): Long {
        var total = 0L
        if (f.isFile) return f.size
        if (f.isDirectory) {
            val listFiles = client.listFiles(path + "/" + f.name) ?: return total
            for (ff in listFiles) {
                if (ff.isFile) {
                    total += ff.size
                }
                if (ff.isDirectory) {
                    total += getFileOrFolderSize(path + "/" + f.name, client, ff)
                }
            }
        }
        return total
    }

    open class DownloadFtpFilesTask(val client: FTPClient, val parentPath: String, val ftpFiles: Array<FTPFile>, val destinationPath: String, val c: DownloadTaskCallback?) : Thread() {
        open var flag = false
        private var total = 0L
        private var speed = 0L
        private var progress = 0L
        private var lastProgress = 0L
        private var lastSpeedTime = 0L
        val eb = StringBuilder()
        var currentWriting: File? = null

        interface DownloadTaskCallback {
            fun onFileProgress(resPath: String, dstPath: String)
            fun onProgress(progress: Long, total: Long)
            fun onSpeed(s: Long)
            fun onCompleted(errorInfo: String)
            fun onError(e: Exception)
        }

        override fun run() {
            super.run()
            try {
                for (f in ftpFiles) {
                    total += getFileOrFolderSize(parentPath, client, f)
                }
                if (flag) return
                for (f in ftpFiles) {
                    try {
                        downloadFileOrFolder(parentPath, f, "")
                    } catch (e: Exception) {
                        e.printStackTrace()
                        eb.append(e.toString())
                        eb.append("\n\n")
                    }

                }
                if (!flag) MyApplication.handler.post {
                    c?.onCompleted(eb.toString())
                }
            } catch (e: Exception) {
                e.printStackTrace()
                MyApplication.handler.post {
                    c?.onError(e)
                }
            } finally {

            }
        }

        private fun downloadFileOrFolder(parentPath: String, f: FTPFile, relativePath: String) {
            if (flag) return
            if (f.isDirectory) {
                //downloadFileOrFolder(f)
                val listFiles = client.listFiles(parentPath + "/${f.name}")
                listFiles?.let {
                    for (ff in listFiles) {
                        downloadFileOrFolder(parentPath + "/${f.name}", ff, "${relativePath}/${f.name}")
                    }
                }
            } else if (f.isFile) {
                var retrieveFileStream: InputStream? = null
                try {
                    client.setFileType(FTPClient.BINARY_FILE_TYPE)
                    //client.enterLocalPassiveMode()
                    client.changeWorkingDirectory(parentPath)
                    retrieveFileStream = client.retrieveFileStream(f.name)
                    val desFolderPath = destinationPath + relativePath
                    val desPath = "${desFolderPath}/${f.name}"
                    MyApplication.handler.post {
                        c?.onFileProgress("${parentPath}/${f.name}", desPath)
                    }
                    try {
                        val folder = File(desFolderPath)
                        if (!folder.exists()) {
                            folder.mkdirs()
                        }
                    } catch (e: Exception) {

                    }
                    val writeFile = File(desPath)
                    currentWriting = writeFile
                    val o = BufferedOutputStream(FileOutputStream(writeFile))
                    val br = ByteArray(1024)
                    var read: Int
                    while (retrieveFileStream.read(br).also { read = it } != -1) {
                        if (flag) {
                            o.flush()
                            o.close()
                            writeFile.delete()
                            break
                        }
                        o.write(br, 0, read)
                        speed += read
                        progress += read
                        if (progress - lastProgress > 100 * 1024L) {
                            lastProgress = progress
                            MyApplication.handler.post {
                                c?.onProgress(progress, total)
                            }
                        }
                        val current = System.currentTimeMillis()
                        if (current - lastSpeedTime > 1000L) {
                            lastSpeedTime = current
                            val s = speed
                            speed = 0L
                            MyApplication.handler.post {
                                c?.onSpeed(s)
                            }
                        }
                    }
                    if (!flag) {
                        o.flush()
                        o.close()
                    }
                } catch (e: Exception) {
                    try {
                        currentWriting?.delete()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    eb.append(currentWriting)
                    eb.append(":")
                    eb.append(e.toString())
                    eb.append("\n\n")
                    e.printStackTrace()
                } finally {
                    try {
                        if (retrieveFileStream != null) {
                            retrieveFileStream.close()
                            client.completePendingCommand()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    open class UploadFtpFilesTask(val client: FTPClient, val parentPath: String, val fileNames: List<String>, val uploadInputStreams: List<InputStream>, val c: UploadTaskCallback?) : Thread() {
        var flag = false
        var total = 0L
        var progress = 0L
        var speed = 0L
        var speedTime = 0L
        var progressCheck = 0L

        val errorInfo = StringBuilder()

        interface UploadTaskCallback {
            fun onFileUploading(fullPath: String)
            fun onProgress(progress: Long, total: Long)
            fun onSpeed(speed: Long)
            fun onComplete(errorInfo: String)
            fun onError(e: Exception)
        }

        override fun run() {
            super.run()
            try {
                for (input in uploadInputStreams) {
                    try {
                        input.reset()
                    } catch (e: Exception) {
                        total += input.available()
                        continue
                    }
                    while (input.read() != -1) {
                        total++
                    }
                    input.reset()
                }

                for (i in fileNames.indices) {
                    if (flag) break
                    val inputStream = uploadInputStreams[i]
                    MyApplication.handler.post {
                        c?.onFileUploading("$parentPath${if (parentPath.isEmpty()) "/" else ""}${fileNames[i]}")
                    }
                    uploadFileItem(client, parentPath, fileNames[i], inputStream)
                }

                if (!flag) MyApplication.handler.post {
                    c?.onComplete(errorInfo.toString())
                }
            } catch (e: Exception) {
                e.printStackTrace()
                MyApplication.handler.post {
                    c?.onError(e)
                }
            }

        }

        private fun uploadFileItem(client: FTPClient, path: String, fileName: String, inputStream: InputStream) {
            // 设置PassiveMode传输
            if (flag) return
            var out:OutputStream?=null
            try {
                // 设置以二进制流的方式传输
                client.setFileType(FTPClient.BINARY_FILE_TYPE)
                client.changeWorkingDirectory(path)
                out = client.storeFileStream(fileName)
                if(out==null){
                    throw RuntimeException("can not obtain output stream, check permission")
                }
                var i: Int
                val buffer = ByteArray(1024)
                while (inputStream.read(buffer).also { i = it } != -1 && !flag) {
                    out.write(buffer, 0, i)
                    progress += i
                    speed += i
                    if (progress - progressCheck > 100 * 1024) {
                        progressCheck = progress
                        MyApplication.handler.post {
                            c?.onProgress(progress, total)
                        }
                    }
                    val current = System.currentTimeMillis()
                    if (current - speedTime > 1000L) {
                        speedTime = current
                        val s = speed
                        speed = 0L
                        MyApplication.handler.post {
                            c?.onSpeed(s)
                        }
                    }
                }

                if (flag) {
                    client.dele(fileName)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorInfo.append("$path${if (path.isEmpty()) "/" else ""}$fileName:")
                errorInfo.append(e.toString())
                errorInfo.append("\n\n")
            } finally {
                try{
                    out?.flush()
                    out?.close()
                }catch (e:Exception){}
                try {
                    if(out!=null)client.completePendingCommand()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}