package com.github.ghmxr.ftpshare.ftpclient

import android.content.Context
import android.content.SharedPreferences
import com.github.ghmxr.ftpshare.MyApplication
import com.github.ghmxr.ftpshare.data.ClientBean
import org.json.JSONArray
import org.json.JSONObject

class FtpClientManager private constructor() {

    private val list = mutableListOf<ClientBean>()

    companion object {
        @JvmStatic
        val instance: FtpClientManager by lazy {
            FtpClientManager()
        }
    }

    init {
        synchronized(list) {
            list.clear()
            list.addAll(getClientBeans())
        }
    }

    fun getClientBeanSize(): Int = list.size

    fun getClientBeanOfId(id: Int) = synchronized(list) { list[id] }

    fun updateClientBean(id: Int, bean: ClientBean) {
        synchronized(list) {
            val oldBean = list[id]
            oldBean.disconnect {}
            bean._id = id
            list[bean._id] = bean
            flushListToSp()
        }
    }

    fun saveClientBean(bean: ClientBean) {
        synchronized(list) {
            list.add(bean)
            updateIdOfBeans()
            flushListToSp()
        }
    }

    fun deleteClientBean(bean: ClientBean): Boolean {
        synchronized(list) {
            if (!list.remove(bean)) return false
            updateIdOfBeans()
            flushListToSp()
        }
        return true
    }

    private fun updateIdOfBeans() {
        for (i in 0 until list.size) {
            list[i]._id = i
        }
    }

    private fun flushListToSp() {
        val jsonArray = JSONArray()
        for (b in list) {
            jsonArray.put(b.toJsonObject())
        }
        getPreference().edit().putString("clients", jsonArray.toString()).apply()
    }

    private fun getClientBeans(): List<ClientBean> = mutableListOf<ClientBean>().apply {
        val jsonArray = JSONArray(getPreference().getString("clients", "[]"))
        for (i in 0 until jsonArray.length()) {
            add(ClientBean(jsonArray[i] as JSONObject))
        }
    }

    private fun getPreference(): SharedPreferences = MyApplication.getGlobalBaseContext().getSharedPreferences("clientConfig", Context.MODE_PRIVATE)


}