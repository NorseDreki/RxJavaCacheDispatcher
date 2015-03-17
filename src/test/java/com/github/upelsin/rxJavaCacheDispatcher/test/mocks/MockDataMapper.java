package com.github.upelsin.rxJavaCacheDispatcher.test.mocks;

import com.github.upelsin.rxJavaCacheDispatcher.GsonDataMapper;
import com.google.gson.Gson;

/**
 * Created by Alexey Dmitriev <mr.alex.dmitriev@gmail.com> on 17.03.2015.
 */
public class MockDataMapper extends GsonDataMapper {

    public MockDataMapper() {
        super(new Gson());
    }
}
