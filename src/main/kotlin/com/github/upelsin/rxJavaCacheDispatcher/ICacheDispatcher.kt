package com.squirrel.android.cache

import java.util.Date

/**
 * Created by Alexey Dmitriev <mr.alex.dmitriev@gmail.com> on 26.01.2015.
 */
public trait ICacheDispatcher {

    public fun <T> get(cacheKey: String, clazz: Class<T>, loader: ()-> Any/*ExpensiveLoader<Any>*/, expiration: Date): T

    public trait ExpensiveLoader<T> {

        public fun execute(): T
    }
}
