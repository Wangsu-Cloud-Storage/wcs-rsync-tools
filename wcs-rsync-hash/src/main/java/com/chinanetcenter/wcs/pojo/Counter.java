package com.chinanetcenter.wcs.pojo;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by lidl on 15-4-2.
 */
public class Counter {
    private AtomicLong number = new AtomicLong(0L);

    public Counter() {
    }

    public Counter(long number) {
        this.number = new AtomicLong(number);
    }

    public synchronized long getAndIncrement() {
        long temp = number.getAndIncrement();
        return temp;
    }

    public long get() {
        return number.get();
    }

    public synchronized long getAndDecrement() {
        long temp = number.getAndDecrement();
        return temp;
    }
}
