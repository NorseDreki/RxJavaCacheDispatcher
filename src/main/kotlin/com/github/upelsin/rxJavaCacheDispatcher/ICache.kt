package com.github.upelsin.rxJavaCacheDispatcher

import java.util.Date

/**
 * Created by Alexey Dmitriev <mr.alex.dmitriev@gmail.com> on 26.01.2015.
 */
trait ICache {

    fun get(key: String): Entry?

    fun put(key: String, entry: Entry)

    fun remove(key: String)

    fun clear()

    class Entry(public val data: ByteArray) {

        public fun isHardExpired(): Boolean {
            return false
        }

        public fun isSoftExpired(): Boolean {
            return false
        }

        public fun setExpiration(expiration: Date) {
        }
    }
}
