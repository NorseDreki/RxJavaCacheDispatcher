package com.github.upelsin.rxJavaCacheDispatcher

import java.util.Date

import java.util.concurrent.ConcurrentHashMap
import rx.Observable
import java.util.concurrent.locks.ReentrantReadWriteLock
import rx.schedulers.Schedulers

/**
 * An impementation of {@link ICacheDispatcher}.
 *
 * Manages keyed requests so that if data for a particular {@code key} is present in {@code cache} and not expired,
 * it is immediately returned to caller; otherwise data is loaded using {@code loader} provided by client,
 * is saved to {@code cache} and then returned.
 *
 * This class is thread-safe.
 *
 * Created by Alexey Dmitriev <mr.alex.dmitriev@gmail.com> on 26.01.2015.
 */
class ObservableCacheDispatcher(val cache: ICache, val dataMapper: IDataMapper): ICacheDispatcher {

    /** storage for requests which are being loaded at moment */
    val requestsInFlight = ConcurrentHashMap<String, Observable<Any>>()

    val lock = ReentrantReadWriteLock()

    val readLock = lock.readLock()

    val writeLock = lock.writeLock()


    /**
     * Returns data for the given {@code key}. If data is present in {@code cache} and not expired, returns it
     * immediately; otherwise loads data using {@code loader}, saves it {@code cache} and returns to caller.
     *
     * @param key string key which is a unique identifier of data
     * @param clazz class of data
     * @param loader loader used to load data if it's not present in {@code cache}
     * @param expiration date and time in future until cached data remains valid
     *
     * @return {@link Observable} of data
     */
    override fun <T> get(key: String, clazz: Class<T>, loader: () -> T, expiration: Date): Observable<T> {
        readLock.lock()
        try {
            // even if request is removed from requestsInFlight after get() but before if(),
            // the returned Observable will still be valid to obtain data from
            val observable = requestsInFlight.get(key)
            if (observable != null) {
                println("Joining to inFlight from read lock: " + key)
                return observable as Observable<T>
            }

            // If there were no requests in flight, these possibilities exist:
            //   1) no request for this key was even made, and no value is present in cache,
            //      so execution goes to the next block guarded by writeLock (below);
            //
            //   2) another request has just successfully completed, and it is guaranteed there is already value
            //      in cache (writing to cache is guarded by requestsInFlight.get(key) presence;
            //
            //   3) another request has failed or writing to cache has failed so this request goes
            //      through the path (1);
            //
            //   4) another request has reached the block with the writeLock, but hasn't put anything
            //      to requestsInFlight yet -- this request will be waiting in front of writeLock.
            val entry = cache.get(key)
            if (entry != null && !entry.isHardExpired()) {
                println("Cache hit from read lock: " + key)
                return Observable.just(dataMapper.fromBytes(entry.data, clazz))
            }

        } finally {
            readLock.unlock()
        }

        val requestInFlight: Observable<Any>

        writeLock.lock()
        try {
            // since with ReadWriteLock there is no possibility to upgrade readLock to writeLock
            // when needed, there will be threads racing for writeLock, and we need to check
            // the preconditions once again
            val observable = requestsInFlight.get(key)
            if (observable != null) {
                println("Joining to inFlight from write lock: " + key)
                return observable as Observable<T>
            }

            val entry = cache.get(key)
            if (entry != null && !entry.isHardExpired()) {
                println("Cache hit from write lock: " + key)
                return Observable.just(dataMapper.fromBytes(entry.data, clazz))
            }

            println("Putting to inFlight: " + key)
            requestInFlight = Observable.just(loader())//.replay().publish()
            requestsInFlight.put(key, requestInFlight)
            //requestInFlight.connect()

        } finally {
            writeLock.unlock()
        }

        // so the whole point of using writeLock was to create a request atomically,
        // after that it is possible to continue without lock, because the next block of code
        // is guarded by presence of request in requestsInFlight
        println("Subscribing to loader... " + key)
        val result = requestInFlight.toBlocking().first()

        val data = dataMapper.toBytes(result)
        saveToCache(key, data, expiration)

        println("Removing inFlight reference " + key)
        requestsInFlight.remove(key)
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
