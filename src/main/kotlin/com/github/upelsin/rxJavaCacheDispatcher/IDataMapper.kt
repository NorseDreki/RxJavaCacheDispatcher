package com.github.upelsin.rxJavaCacheDispatcher

/**
 * Created by Alexey Dmitriev <mr.alex.dmitriev@gmail.com> on 27.01.2015.
 */
trait IDataMapper {

    fun toBytes(entry: Any): ByteArray

    fun <T> fromBytes(data: ByteArray, clazz: Class<T>): T
}
