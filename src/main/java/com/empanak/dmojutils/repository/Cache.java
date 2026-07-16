package com.empanak.dmojutils.repository;

public interface Cache {
    long lastCleanupTime = 0;
    long cleanupInterval = 60000;
    void cleanupCache();
}
