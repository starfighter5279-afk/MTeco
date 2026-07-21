package com.dirt.scheduler;

public interface ScheduledTask {
    void cancel();

    boolean isCancelled();
}