package com.compassrosetech.ccs.android.test;

import com.squirrel.android.cache.ICache;
import com.squirrel.android.cache.IDataMapper;
import com.squirrel.android.cache.implementation.ObservableCacheDispatcher;
import com.squirrel.android.model.Track;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Date;

import kotlin.Function0;
import rx.Observable;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by Alexey Dmitriev <mr.alex.dmitriev@gmail.com> on 15.03.2015.
 */
/*@Config(emulateSdk = 18)
@RunWith(CustomTestRunner.class)*/
public class CacheDispatcherTest {

    private static final String SOME_KEY = "someKey";

    @Mock
    private IDataMapper mockDataMapper;

    @Mock
    private ICache mockCache;

    @Mock
    private Function0<?> mockLoader;

    @Mock
    private Track mockTrack;

    private ObservableCacheDispatcher cacheDispatcher;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        cacheDispatcher = new ObservableCacheDispatcher(mockCache, mockDataMapper);
    }

    @Test
    public void should_use_loader_on_cache_miss() {
        given(mockLoader.invoke()).willReturn(mockTrack);
        given(mockDataMapper.toBytes(any())).willReturn(new byte[1]);

        Observable<Object> observable = cacheDispatcher.get(SOME_KEY, Track.class, mockLoader, new Date());
        Track result = (Track) observable.toBlocking().first();

        verify(mockLoader).invoke();
        verify(mockCache, times(2)).get(SOME_KEY);
        verify(mockCache).put(eq(SOME_KEY), any(ICache.Entry.class));

        assertThat(result, is(equalTo(mockTrack)));
        //verify(mockDataMapper).fromBytes()
    }

    @Test
    public void should_get_existing_entry_from_cache() {
        given(mockCache.get(SOME_KEY)).willReturn(new ICache.Entry(new byte[1]));
        given(mockDataMapper.toBytes(any())).willReturn(new byte[1]);
        given(mockDataMapper.fromBytes(any(byte[].class), eq(Track.class))).willReturn(mockTrack);

        Observable<Object> observable = cacheDispatcher.get(SOME_KEY, Track.class, mockLoader, new Date());
        Track result = (Track) observable.toBlocking().first();

        verify(mockLoader, never()).invoke();
        verify(mockCache).get(SOME_KEY);
        verify(mockCache, never()).put(anyString(), any(ICache.Entry.class));
        verify(mockDataMapper).fromBytes(any(byte[].class), eq(Track.class));

        assertThat(result, is(equalTo(mockTrack)));
        //verify(mockDataMapper).fromBytes()
    }
}