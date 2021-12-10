package com.github.ghmxr.ftpshare.data

import android.content.Intent
import com.github.ghmxr.ftpshare.Constants
import com.github.ghmxr.ftpshare.MyApplication
import com.github.ghmxr.ftpshare.R
import com.github.ghmxr.ftpshare.fragments.ClientFragment
import com.github.ghmxr.ftpshare.utils.StorageUtil
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import org.json.JSONObject

data class ClientBean constructor(var _id: Int, var nickName: String, var userName: String, var password: String, var host: String, var port: Int) {
    enum class Status {
        CONNECTED, CONNECTING, DISCONNECTED, DISCONNECTING
    }

    @Transient
    val client: FTPClient = FTPClient()

    @Transient
    var status: Status = Status.DISCONNECTED

    @Transient
    val connectCallbacks = ArrayList<(Boolean, Exception?) -> Unit>()

    @Transient
    val disconnectCallbacks = ArrayList<(Exception?) -> Unit>()

    /*@Transient
    var isConnecting=false

    @Transient
    var isDisconnecting=false*/
    /*get() {
        field = if (FTPReply.isPositiveCompletion(client.replyCode)) {
            Status.CONNECTED
        } else {
            Status.DISCONNECTED
        }
        return field
    }*/

    var downloadPath = StorageUtil.getMainStoragePath() + "/Download/FTPShare/$nickName"

    var downloadConfirm = true

    var encode: String = Constants.Charset.CHAR_UTF

    var passiveMode = false

    var connectTimeout = 10 //second

    constructor() : this(-1, MyApplication.getGlobalBaseContext().resources.getString(R.string.client_initial_nickname), "", "",
            MyApplication.getGlobalBaseContext().resources.getString(R.string.client_initial_host), 5656)

    constructor(jsonObject: JSONObject) : this(jsonObject.optInt("_id"), jsonObject.optString("nickName"),
            jsonObject.optString("userName"), jsonObject.optString("password"), jsonObject.optString("host"),
            jsonObject.optInt("port")) {
        downloadPath = jsonObject.optString("downloadPath")
        downloadConfirm = jsonObject.optBoolean("downloadConfirm")
        encode = jsonObject.optString("encode")
        passiveMode = jsonObject.optBoolean("passiveMode")
        connectTimeout = jsonObject.optInt("connectTimeout")
    }

    fun connect(callback: ((Boolean, Exception?) -> Unit)?) {
        callback?.let {
            synchronized(connectCallbacks) {
                connectCallbacks.add(it)
            }
        }
        if (status == Status.CONNECTING) {
            return
        }
        status = Status.CONNECTING
        Thread {
            synchronized(client) {
                try {
                    client.controlEncoding = encode // 中文支持
                    client.connectTimeout = connectTimeout * 1000
                    if (passiveMode) client.enterLocalPassiveMode()
                    else client.enterLocalActiveMode()
                    client.connect(host, port)
                    if (FTPReply.isPositiveCompletion(client.replyCode)) {
                        val login = client.login(if (userName.isEmpty()) Constants.FTPConsts.NAME_ANONYMOUS else userName, password)
                        //if(!login)login = client.login(if (userName.isEmpty()) "IUSR" else userName, password)
                        //if(!login)login = client.login(if (userName.isEmpty()) "FTP" else userName, password)
                        //if(!login)login = client.login(if (userName.isEmpty()) "USER" else userName, password)
                        status = if (login) Status.CONNECTED else Status.DISCONNECTED
                        if (login) {
                            /*if(FTPReply.isPositiveCompletion(client.sendCommand("OPTS UTF8", "ON"))){
                                client.controlEncoding = "UTF-8" // 中文支持
                            }else{
                                client.controlEncoding="GBK"
                            }*/
                        }
                        /*MyApplication.handler.post {
                            callback?.invoke(login, null)
                        }*/
                        invokeConnectCallbacksAndClear(login, null)
                    }
                } catch (e: Exception) {
                    status = Status.DISCONNECTED
                    /*MyApplication.handler.post {
                        callback?.invoke(false, e)
                    }*/
                    invokeConnectCallbacksAndClear(false, e)
                }
            }
        }.start()
    }

    fun disconnect(callback: ((Exception?) -> Unit)?) {
        callback?.let {
            synchronized(disconnectCallbacks) {
                disconnectCallbacks.add(it)
            }
        }
        if (status == Status.DISCONNECTING) {
            return
        }
        status = Status.DISCONNECTING
        Thread {
            synchronized(client) {
                try {
                    client.disconnect()
                    status = Status.DISCONNECTED
                    /*MyApplication.handler.post {
                        callback?.invoke(null)
                    }*/
                    invokeDisconnectCallbacksAndClear(null)
                } catch (e: Exception) {
                    status = Status.DISCONNECTED
                    /*MyApplication.handler.post {
                        callback?.invoke(e)
                    }*/
                    invokeDisconnectCallbacksAndClear(e)
                }
            }
        }.start()
    }

    private fun invokeConnectCallbacksAndClear(b: Boolean, e: Exception?) {
        MyApplication.handler.post {
            synchronized(connectCallbacks) {
                for (c in connectCallbacks) {
                    c.invoke(b, e)
                }
                MyApplication.getGlobalBaseContext().sendBroadcast(Intent(ClientFragment.ACTION_REFRESH_LIST))
                connectCallbacks.clear()
            }
        }
    }

    private fun invokeDisconnectCallbacksAndClear(e: Exception?) {
        MyApplication.handler.post {
            synchronized(connectCallbacks) {
                for (c in disconnectCallbacks) {
                    c.invoke(e)
                }
                disconnectCallbacks.clear()
            }
        }
    }

    fun toJsonObject(): JSONObject =
            JSONObject().apply {
                put("userName", userName)
                put("password", password)
                put("host", host)
                put("port", port)
                put("nickName", nickName)
                put("_id", _id)
                put("downloadPath", downloadPath)
                put("downloadConfirm", downloadConfirm)
                put("encode", encode)
                put("passiveMode", passiveMode)
                put("connectTimeout", connectTimeout)
            }


}
