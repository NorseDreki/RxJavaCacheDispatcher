package com.github.upelsin.rxJavaCacheDispatcher

import com.google.gson.Gson
import java.nio.charset.Charset

/**
 * Created by Alexey Dmitriev <mr.alex.dmitriev@gmail.com> on 27.01.2015.
 */
open class GsonDataMapper(val gson: Gson) : IDataMapper {

    override fun toBytes(entry: Any): ByteArray {
        return gson.toJson(entry).getBytes(Charset.forName("UTF-8"))
    }

    override fun <T> fromBytes(data: ByteArray, clazz: Class<T>): T {
        val json = String(data, Charset.forName("UTF-8"))
        return gson.fromJson<T>(json, clazz)
    }
}
