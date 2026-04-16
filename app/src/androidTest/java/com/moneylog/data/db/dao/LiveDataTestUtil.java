package com.moneylog.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * LiveData 값을 동기적으로 추출하는 테스트 유틸리티.
 * InstantTaskExecutorRule과 함께 사용한다.
 */
public class LiveDataTestUtil {

    public static <T> T getValue(LiveData<T> liveData) throws InterruptedException {
        final Object[] data = new Object[1];
        final CountDownLatch latch = new CountDownLatch(1);

        Observer<T> observer = new Observer<T>() {
            @Override
            public void onChanged(T t) {
                data[0] = t;
                latch.countDown();
                liveData.removeObserver(this);
            }
        };

        liveData.observeForever(observer);
        if (!latch.await(2, TimeUnit.SECONDS)) {
            liveData.removeObserver(observer);
            throw new RuntimeException("LiveData value was never set within 2 seconds.");
        }

        @SuppressWarnings("unchecked")
        T result = (T) data[0];
        return result;
    }
}
