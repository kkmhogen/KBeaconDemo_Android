package com.kbeacon.sensordemo.recordhistory;
import com.kbeacon.kbeaconlib.KBSensorHistoryData.KBHumidityRecord;

public interface HTSensorDataInterface {

    public int size();

    public KBHumidityRecord get(int nIndex);
}
