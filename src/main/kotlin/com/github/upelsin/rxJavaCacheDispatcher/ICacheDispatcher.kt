package com.github.upelsin.rxJavaCacheDispatcher

import java.util.Date

/**
 * Created by Alexey Dmitriev <mr.alex.dmitriev@gmail.com> on 26.01.2015.
 */
trait ICacheDispatcher {

    fun <T> get(key: String, clazz: Class<T>, loader: ()-> T, expiration: Date): rx.Observable<T>

}
