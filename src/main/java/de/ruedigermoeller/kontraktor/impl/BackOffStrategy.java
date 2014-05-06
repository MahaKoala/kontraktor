package de.ruedigermoeller.kontraktor.impl;

import java.util.concurrent.locks.*;

/**
 * Created by moelrue on 06.05.2014.
 *
 * Kontraktor uses spinlocking. By adjusting the backofstrategy values one can define the tradeoff
 * regarding latency/idle CPU load.
 *
 * if a message queue is empty, first busy spin is used for N iterations, then Thread.yield, then LockSupport.park, then sleep(nanosToPark)
 *
 */
public class BackOffStrategy {

    int yieldCount;
    int parkCount;
    int sleepCount;
    int nanosToPark  = 1000*500; // half milli

    public BackOffStrategy() {
        setCounters(100000,50000,5000);
    }

    /**
     * @param yieldCount - number of busy spins until Thread.yield is used
     * @param parkCount  - number of Thread.yield iterations until parkNanos(1) is used
     * @param sleepCount - number of parkNanos(1) is used until park(nanosToPark) is used. Default for nanosToPark is 0.5 milliseconds
     */
    public BackOffStrategy(int yieldCount, int parkCount, int sleepCount) {
        setCounters(yieldCount,parkCount,sleepCount);
    }

    public void setCounters( int yield, int park, int sleep ) {
        yieldCount = yield;
        parkCount = yield+park;
        sleepCount = yield+park+sleep;
    }

    public int getNanosToPark() {
        return nanosToPark;
    }

    public void setNanosToPark(int nanosToPark) {
        this.nanosToPark = nanosToPark;
    }

    public void yield(int count) {
        if ( count > sleepCount) {
            LockSupport.parkNanos(nanosToPark);
        } else if ( count > parkCount) {
            LockSupport.parkNanos(1);
        } else {
            if ( count > yieldCount)
                Thread.yield();
        }
    }


}
