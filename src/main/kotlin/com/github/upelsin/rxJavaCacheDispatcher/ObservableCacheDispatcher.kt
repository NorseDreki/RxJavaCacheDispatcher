package com.github.upelsin.rxJavaCacheDispatcher

import java.util.Date

import java.util.concurrent.ConcurrentHashMap
import rx.Observable
import java.util.concurrent.locks.ReentrantReadWriteLock
import rx.schedulers.Schedulers

/**
 * Created by Alexey Dmitriev <mr.alex.dmitriev@gmail.com> on 26.01.2015.
 */
class ObservableCacheDispatcher(val cache: ICache, val dataMapper: IDataMapper) {

    val rwl = ReentrantReadWriteLock()
    val inFlight = ConcurrentHashMap<String, Observable<Any>>()
    val readLock = rwl.readLock()
    val writeLock = rwl.writeLock()

    fun <T> get(key: String, clazz: Class<T>, loader: () -> T, expiration: Date): Observable<T> {
        val data: ByteArray

        readLock.lock()
        try {
            // join an in-flight request if any and return an Observable to make
            // a subscription on

            // not matching this "if" guarantees there will be a cached value in cache.
            val observable = inFlight.get(key)
            if (observable != null) {
                println("Joining to inFlight from read lock " + key)
                return observable as Observable<T>
            }

            // guard cache with exclusive lock? no due to guarantee before
            // but there might be an error when writing, so still could be null
            val entry = cache.get(key)
            if (entry != null && !entry.isHardExpired()) {
                data = entry.data
                println("Cache hit from read lock " + key)
                return Observable.just(dataMapper.fromBytes(data, clazz))
            }

        } finally {
            readLock.unlock()
        }

        val o: Observable<Any>
        // here come together threads racing for write lock
        // there was no requests in flight
        // there was no value in cache
        // there was an expired value
        writeLock.lock()
        try {
            // check for preconditions again
            // there might be next block already executing; it's released write lock,
            // so there is a request in
            val observable = inFlight.get(key)
            if (observable != null) {
                println("Joining to inFlight from write lock " + key)
                return observable as Observable<T>
            }
            /*if (inFlight.containsKey(key)) {
                println("Joining to inFlight from write lock")
                return inFlight.get(key)// as Observable<T>
            }*/

            // there should also be a check for cache since value in inFlight means op has completed
            // and value is ready
            // too many threads reading from cache at this point could be a slowdown
            val entry = cache.get(key)
            if (entry != null && !entry.isHardExpired()) {
                println("Cache hit from write lock " + key)
                data = entry.data
                return Observable.just(dataMapper.fromBytes(data, clazz))
            }

            println("Putting to inFlight " + key)
            o = Observable.just(loader())//.replay().publish()
            inFlight.put(key, o)
            //o.connect()

        } finally {
            writeLock.unlock()
        }

        println("Subscribing to loader... " + key)
        // write lock is released for the period of a lengthy loading operation
        // maybe think about just lock downgrade?
        val result = o.subscribeOn(Schedulers.io()).observeOn(Schedulers.newThread()).toBlocking().single()

        // maybe put these two ops under write lock below?
        // there will be no readers of cache when it's being written to
        data = dataMapper.toBytes(result)
        saveToCache(key, data, expiration)

        // actually, this op is atomic, no lock needed
        // ACTUALLY, it has to go through locking to protect if/return check for inFlight
        // maybe just replace that code with get() then check rather than contains()
        /*writeLock.lock()
        try {
            println("Removing write lock")
            inFlight.remove(key)
        } finally {
            writeLock.unlock()
        }*/


        println("Removing inFlight reference " + key)
        inFlight.remove(key)
        println("Removed inFlight reference " + key)

        return Observable.just(result as T)
    }

    private fun saveToCache(key: String, data: ByteArray, expiration: Date) {
        //LOG.d("Putting to cache for the key %s", key)
        val entry = ICache.Entry(data)
        entry.setExpiration(expiration)
        cache.put(key, entry)
    }
}
