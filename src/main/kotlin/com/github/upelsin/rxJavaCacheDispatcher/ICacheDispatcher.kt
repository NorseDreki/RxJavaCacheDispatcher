package com.github.upelsin.rxJavaCacheDispatcher

import java.util.Date

/**
 * Created by Alexey Dmitriev <mr.alex.dmitriev@gmail.com> on 26.01.2015.
 */
trait ICacheDispatcher {

    fun <T> get(cacheKey: String, clazz: Class<T>, loader: ()-> T, expiration: Date): T

}
