package com.github.upelsin.rxJavaCacheDispatcher.test;

import com.github.upelsin.rxJavaCacheDispatcher.ICache;
import com.github.upelsin.rxJavaCacheDispatcher.IDataMapper;
import com.github.upelsin.rxJavaCacheDispatcher.ObservableCacheDispatcher;
import com.github.upelsin.rxJavaCacheDispatcher.test.mocks.MockCache;
import com.github.upelsin.rxJavaCacheDispatcher.test.mocks.MockDataMapper;
import kotlin.Function0;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rx.Observable;

import java.util.*;
import java.util.concurrent.CountDownLatch;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Created by Alexey Dmitriev <mr.alex.dmitriev@gmail.com> on 15.03.2015.
 */
//@RunWith(ApplicationTestRunner.class)
public class ObservableCacheDispatcherTest {

    private static final String SOME_KEY = "someKey";

    private static final byte[] SOME_BYTES = new byte[1];
    
    private static class SomeEntry {}

    /** number of threads racing for a single cache key */
    private static final int NUM_THREADS = 30;

    /** number of packs of threads to be executed simultaneously */
    private static final int NUM_DIFFERENT_CACHE_KEYS = 3;

    @Mock
    private IDataMapper mockDataMapper;

    @Mock
    private ICache mockCache;

    @Mock
    private Function0<SomeEntry> mockLoader;

    @Mock
    private SomeEntry mockEntry;

    @Mock
    private Date mockExpiration;

    private MockCache spiedCache;

    private MockDataMapper spiedDataMapper;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        spiedCache = spy(new MockCache());
        spiedDataMapper = spy(new MockDataMapper());
    }

    @Test
    public void should_use_loader_on_cache_miss() {
        given(mockLoader.invoke()).willReturn(mockEntry);
        given(mockDataMapper.toBytes(any())).willReturn(SOME_BYTES);

        ObservableCacheDispatcher cacheDispatcher = new ObservableCacheDispatcher(mockCache, mockDataMapper);
        Observable<SomeEntry> observable = cacheDispatcher.get(SOME_KEY, SomeEntry.class, mockLoader, mockExpiration);
        SomeEntry result = observable.toBlocking().first();

        verify(mockLoader).invoke();
        verify(mockCache, times(2)).get(SOME_KEY);
        verify(mockCache).put(eq(SOME_KEY), any(ICache.Entry.class));
        assertThat(result, is(equalTo(mockEntry)));
    }

    @Test
    public void should_get_existing_entry_from_cache() {
        given(mockCache.get(SOME_KEY)).willReturn(new ICache.Entry(SOME_BYTES));
        given(mockDataMapper.fromBytes(any(byte[].class), eq(SomeEntry.class))).willReturn(mockEntry);

        ObservableCacheDispatcher cacheDispatcher = new ObservableCacheDispatcher(mockCache, mockDataMapper);
        Observable<SomeEntry> observable = cacheDispatcher.get(SOME_KEY, SomeEntry.class, mockLoader, mockExpiration);
        SomeEntry result = observable.toBlocking().first();

        verify(mockLoader, never()).invoke();
        verify(mockCache).get(SOME_KEY);
        verify(mockCache, never()).put(anyString(), any(ICache.Entry.class));
        verify(mockDataMapper).fromBytes(any(byte[].class), eq(SomeEntry.class));
        verify(mockDataMapper, never()).toBytes(anyObject());
        assertThat(result, is(equalTo(mockEntry)));
    }

    @Test
    public void should_use_loader_once_then_get_entry_from_cache_running_multiple_cache_keys()
            throws InterruptedException {

        ObservableCacheDispatcher cacheDispatcher = new ObservableCacheDispatcher(spiedCache, spiedDataMapper);
        CountDownLatch finishLatch = new CountDownLatch(NUM_THREADS * NUM_DIFFERENT_CACHE_KEYS);
        CountDownLatch loopLatch = new CountDownLatch(1);
        Map<String, List<String>> allResults = new HashMap<String, List<String>>();
        Map<String, Function0<String>> allLoaders = new HashMap<String, Function0<String>>();

        for (int i = 0; i < NUM_DIFFERENT_CACHE_KEYS; i++) {
            String entry = "entry" + System.currentTimeMillis();
            Function0<String> loader = mock(Function0.class);
            given(loader.invoke()).willReturn(entry);

            List<String> results = createAndStartThreads(cacheDispatcher, entry, loader, mockExpiration,
                    finishLatch, loopLatch);

            allResults.put(entry, results);
            allLoaders.put(entry, loader);
        }
        loopLatch.countDown();
        finishLatch.await();


        for (int i = 0; i < NUM_DIFFERENT_CACHE_KEYS; i++) {
            String entry = allLoaders.keySet().iterator().next();
            Function0<String> loader = allLoaders.get(entry);
            List<String> results = allResults.get(entry);

            verify(loader).invoke();
            verify(spiedDataMapper).toBytes(entry);
            verify(spiedCache).put(eq(entry), any(ICache.Entry.class));
            //verify spied cache returned non-null entries
            assertThat(results, everyItem(is(entry)));
            assertThat(results.size(), is(NUM_THREADS));
        }
    }

    private List<String> createAndStartThreads(final ObservableCacheDispatcher ocd,
                                               final String cacheKey,
                                               final Function0<String> loader,
                                               final Date expiration,
                                               final CountDownLatch finishLatch,
                                               final CountDownLatch loopLatch) {

        final List<String> results = new ArrayList<String>();
        for (int i = 0; i < NUM_THREADS; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    //synchronized (onNextLock) {
                        await(loopLatch);

                        Observable<String> value = ocd.get(cacheKey, String.class, loader, expiration);
                        String result = value.toBlocking().first();

                        results.add(result);

                        finishLatch.countDown();
                    //}
                }
            }).start();
        }

        return results;
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
