package com.gencic.bleperipheral;

/**
 * Created by ngencic on 13.9.15..
 * ILogger should be implemented if objects are interested in logging events from BLE communication
 */
public interface ILogger {
    void log(String msg);
}
