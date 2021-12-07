package com.kbeacon.sensordemo;

import android.app.AlertDialog;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.kkmcn.kbeaconlib.KBConnPara;
import com.kkmcn.kbeaconlib.KBSensorNotifyData.KBHumidityNotifyData;
import com.kkmcn.kbeaconlib.KBSensorNotifyData.KBNotifyButtonEvtData;
import com.kkmcn.kbeaconlib.KBSensorNotifyData.KBNotifyDataBase;
import com.kkmcn.kbeaconlib.KBSensorNotifyData.KBNotifyDataType;
import com.kkmcn.kbeaconlib.KBSensorNotifyData.KBNotifyMotionEvtData;
import com.kkmcn.kbeaconlib.UTCTime;
import com.kbeacon.sensordemo.dfulibrary.KBeaconDFUActivity;
import com.kbeacon.sensordemo.recordhistory.CfgHTBeaconHistoryActivity;
import com.kkmcn.kbeaconlib.KBAdvPackage.KBAdvType;
import com.kkmcn.kbeaconlib.KBCfgPackage.KBCfgBase;
import com.kkmcn.kbeaconlib.KBCfgPackage.KBCfgCommon;
import com.kkmcn.kbeaconlib.KBCfgPackage.KBCfgHumidityTrigger;
import com.kkmcn.kbeaconlib.KBCfgPackage.KBCfgSensor;
import com.kkmcn.kbeaconlib.KBCfgPackage.KBCfgTrigger;
import com.kkmcn.kbeaconlib.KBCfgPackage.KBCfgType;
import com.kkmcn.kbeaconlib.KBConnectionEvent;
import com.kkmcn.kbeaconlib.KBException;
import com.kkmcn.kbeaconlib.KBeacon;
import com.kkmcn.kbeaconlib.KBeaconsMgr;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;

public class DevicePannelActivity extends AppBaseActivity implements View.OnClickListener, KBeacon.ConnStateDelegate, KBeacon.NotifyDataDelegate{

    public final static String DEVICE_MAC_ADDRESS = "DEVICE_MAC_ADDRESS";
    private final static String LOG_TAG = "DevicePannel";

    public final static String DEFAULT_PASSWORD = "0000000000000000";   //16 zero ascii
    private static SimpleDateFormat mUtcTimeFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// 日志文件格式

    private KBeaconsMgr mBeaconMgr;
    private String mDeviceAddress;
    private KBeacon mBeacon;

    //uiview
    private TextView mBeaconType, mBeaconStatus;
    private TextView mBeaconModel;
    private Button mEnableTHData2Adv, mEnableTHData2App, mViewTHDataHistory;
    private Button mEnableTHTrigger2Adv, mEnableTHTrigger2App;
    private Button mRingButton, mEnableAxisAdv, mEnableMotionTrigger, mDisableMotionTrigger;
    private Button mEnableBtnTrigger, mDisableBtnTrigger;
    private String mNewPassword;
    SharePreferenceMgr mPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        mDeviceAddress = intent.getStringExtra(DEVICE_MAC_ADDRESS);
        mBeaconMgr = KBeaconsMgr.sharedBeaconManager(this);
        mBeacon = mBeaconMgr.getBeacon(mDeviceAddress);
        if (mBeacon == null){
            toastShow("device is not exist");
            finish();
        }

        mPref = SharePreferenceMgr.shareInstance(this);
        setContentView(R.layout.device_pannel);
        mBeaconStatus = (TextView)findViewById(R.id.connection_states);
        mBeaconType = (TextView) findViewById(R.id.beaconType);
        mBeaconModel = (TextView) findViewById(R.id.beaconModle);

        //send temperature and humidity in advertisement
        mEnableTHData2Adv = (Button) findViewById(R.id.enableTHDataToAdv);
        mEnableTHData2Adv.setOnClickListener(this);

        //send temperature humidity data to app
        mEnableTHData2App = findViewById(R.id.enableTHDataToApp);
        mEnableTHData2App.setOnClickListener(this);

        //view temperature and humidity data history
        mViewTHDataHistory = findViewById(R.id.viewTHDataHistory);
        mViewTHDataHistory.setOnClickListener(this);

        //report trigger to adv
        mEnableTHTrigger2Adv = findViewById(R.id.enableTHChangeTriggerEvtRpt2Adv);
        mEnableTHTrigger2Adv.setOnClickListener(this);

        //enable humidity data report only changed
        mEnableTHTrigger2App = findViewById(R.id.enableTHChangeTriggerEvtRpt2App);
        mEnableTHTrigger2App.setOnClickListener(this);

        //enable axis report to adv
        mEnableAxisAdv = findViewById(R.id.enableAxisAdvertisement);
        mEnableAxisAdv.setOnClickListener(this);

        //enable motion trigger
        mEnableMotionTrigger = findViewById(R.id.enableMotionTrigger);
        mEnableMotionTrigger.setOnClickListener(this);

        mDisableMotionTrigger = findViewById(R.id.disableMotionTrigger);
        mDisableMotionTrigger.setOnClickListener(this);


        mRingButton = (Button) findViewById(R.id.ringDevice);
        mRingButton.setOnClickListener(this);

        mEnableBtnTrigger = (Button) findViewById(R.id.enableBtnTrigger);
        mEnableBtnTrigger.setOnClickListener(this);

        mDisableBtnTrigger = (Button) findViewById(R.id.disableBtnTrigger);
        mDisableBtnTrigger.setOnClickListener(this);


        findViewById(R.id.dfuDevice).setOnClickListener(this);
        findViewById(R.id.readBtnTriggerPara).setOnClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_connect, menu);
        if (mBeacon.getState() == KBeacon.KBStateConnected)
        {
            menu.findItem(R.id.menu_connect).setEnabled(true);
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
            menu.findItem(R.id.menu_connecting).setVisible(false);
            menu.findItem(R.id.menu_connecting).setActionView(null);
            mBeaconStatus.setText("Connected");
        }
        else if (mBeacon.getState() == KBeacon.KBStateConnecting)
        {
            mBeaconStatus.setText("Connecting");
            menu.findItem(R.id.menu_connect).setEnabled(false);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
            menu.findItem(R.id.menu_connecting).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        else
        {
            mBeaconStatus.setText("Disconnected");
            menu.findItem(R.id.menu_connect).setEnabled(true);
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
            menu.findItem(R.id.menu_connecting).setVisible(false);
            menu.findItem(R.id.menu_connecting).setActionView(null);
        }
        return true;
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId()) {

            case R.id.enableAxisAdvertisement:
                enableAdvTypeIncludeAccXYZ();
                break;

            case R.id.enableMotionTrigger:
                enableMotionTriggerToAdv();
                break;

            case R.id.disableMotionTrigger:
                disableMotionTrigger();
                break;

            case R.id.enableTHDataToAdv:
                enableTHRealtimeDataToAdv();
                break;

            case R.id.enableTHDataToApp:
                enableTHRealtimeDataToApp();
                break;

            case R.id.viewTHDataHistory:
                if (mBeacon.isConnected()) {
                    Intent intent = new Intent(this, CfgHTBeaconHistoryActivity.class);
                    intent.putExtra(CfgHTBeaconHistoryActivity.DEVICE_MAC_ADDRESS, mBeacon.getMac());   //field type
                    startActivityForResult(intent, 1);
                }
                break;

            case R.id.enableBtnTrigger:
                //enableButtonTrigger();
                enableBtnTriggerEvtToApp();
                break;

            case R.id.disableBtnTrigger:
                disableButtonTrigger();
                break;

            case R.id.readBtnTriggerPara:
                readButtonTriggerPara();
                break;

            case R.id.enableTHChangeTriggerEvtRpt2Adv:
                enableTHChangeTriggerEvtRpt2Adv();
                break;


            case R.id.enableTHChangeTriggerEvtRpt2App:
                enableTHChangeTriggerEvtRpt2App();
                break;

            case R.id.dfuDevice:
                if (mBeacon.isConnected()) {
                    final Intent intent = new Intent(this, KBeaconDFUActivity.class);
                    intent.putExtra(KBeaconDFUActivity.DEVICE_MAC_ADDRESS, mBeacon.getMac());
                    startActivityForResult(intent, 1);
                }
                break;

            case R.id.ringDevice:
                ringDevice();
                break;
            default:
                break;
        }
    }

    //handle trigger event
    public void onNotifyDataReceived(KBeacon beacon, int nDataType, KBNotifyDataBase sensorData)
    {
        if (nDataType == KBNotifyDataType.NTF_DATA_TYPE_HUMIDITY) {
            KBHumidityNotifyData notifyData = (KBHumidityNotifyData) sensorData;

            float humidity = notifyData.getHumidity();
            float temperature = notifyData.getTemperature();
            long nEventTime = notifyData.getEventUTCTime();

            String strEvtUtcTime;
            strEvtUtcTime = mUtcTimeFmt.format(nEventTime * 1000);
            String strLogInfo = getString(R.string.RECEIVE_NOTIFY_DATA, strEvtUtcTime, temperature, humidity);
            Log.v(LOG_TAG, strLogInfo);
        }
        else if (nDataType == KBNotifyDataType.NTF_DATA_TYPE_BUTTON_EVT)
        {
            KBNotifyButtonEvtData notifyData = (KBNotifyButtonEvtData) sensorData;
            String strLogInfo = String.format("Receive button press event:%d", notifyData.keyNtfEvent);
            Log.v(LOG_TAG, strLogInfo);
        }
        else if (nDataType == KBNotifyDataType.NTF_DATA_TYPE_MOTION_EVT)
        {
            KBNotifyMotionEvtData notifyData = (KBNotifyMotionEvtData) sensorData;
            String strLogInfo = String.format("Receive motion event:%d", notifyData.motionNtfEvent);
            Log.v(LOG_TAG, strLogInfo);
        }
    }

    //Please make sure the app does not enable any trigger's advertisement mode to KBTriggerAdvOnlyMode
    //If the app set some trigger advertisement mode to KBTriggerAdvOnlyMode, then the device only start advertisement when trigger event happened.
    //when this function enabled, then the device will include the realtime temperature and humidity data in advertisement
    public void enableTHRealtimeDataToAdv()
    {
        final KBCfgCommon oldCommonCfg = (KBCfgCommon)mBeacon.getConfigruationByType(KBCfgType.KBConfigTypeCommon);
        final KBCfgSensor oldSensorCfg = (KBCfgSensor)mBeacon.getConfigruationByType(KBCfgType.KBConfigTypeSensor);

        if (!mBeacon.isConnected())
        {
            toastShow("Device is not connected");
            return;
        }

        if (!oldCommonCfg.isSupportHumiditySensor())
        {
            return;
        }

        try {
            //disable temperature trigger, if you enable other trigger, for example, motion trigger, button trigger, please set the trigger adv mode to always adv mode
            //or disable that trigger
            KBCfgHumidityTrigger thTriggerPara = new KBCfgHumidityTrigger();
            thTriggerPara.setTriggerType(KBCfgTrigger.KBTriggerTypeHumidity);
            thTriggerPara.setTriggerAction(KBCfgTrigger.KBTriggerActionOff);

            mEnableTHData2Adv.setEnabled(false);
            this.mBeacon.modifyTrigger(thTriggerPara, new KBeacon.ActionCallback() {
                public void onActionComplete(boolean bConfigSuccess, KBException error) {

                    //enable ksensor advertisement
                    try {
                        ArrayList<KBCfgBase> newCfg = new ArrayList<>(2);
                        if ((oldCommonCfg.getAdvType() & KBAdvType.KBAdvTypeSensor) == 0) {
                            KBCfgCommon newCommonCfg = new KBCfgCommon();
                            newCommonCfg.setAdvType(KBAdvType.KBAdvTypeSensor);
                            newCfg.add(newCommonCfg);
                        }

                        //enable temperature and humidity
                        Integer nOldSensorType = oldSensorCfg.getSensorType();
                        if ((nOldSensorType & KBCfgSensor.KBSensorTypeHumidity) == 0) {
                            KBCfgSensor sensorCfg = new KBCfgSensor();
                            sensorCfg.setSensorType(KBCfgSensor.KBSensorTypeHumidity | nOldSensorType);
                            newCfg.add(sensorCfg);
                        }

                        mBeacon.modifyConfig(newCfg, new KBeacon.ActionCallback() {
                            @Override
                            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                                mEnableTHData2Adv.setEnabled(true);
                                if (bConfigSuccess) {
                                    Log.v(LOG_TAG, "enable humidity advertisement success");
                                }
                            }
                        });
                    }
                    catch (Exception excpt)
                    {
                        excpt.printStackTrace();
                    }
                }
            });
        }
        catch (Exception excpt)
        {
            Log.v(LOG_TAG, "config humidity advertisement failed");
        }
    }



    //please make sure the app does not enable temperature&humidity trigger
    //If the app enable the trigger, the device only report the sensor data to app when trigger event happened.
    //After enable realtime data to app, then the device will periodically send the temperature and humidity data to app whether it was changed or not.
    //require KBeacon firmware version >= 5.22
    public void enableTHRealtimeDataToApp(){
        if (!mBeacon.isConnected()) {
            toastShow("Device is not connected");
            return;
        }

        //check device capability
        int nTriggerCapability = mBeacon.triggerCapability();
        if ((nTriggerCapability & KBCfgTrigger.KBTriggerTypeHumidity) == 0) {
            Log.e(LOG_TAG, "device does not support humidity trigger");
            return;
        }

        //turn off trigger
        try {
            //make sure the trigger was turn off
            KBCfgHumidityTrigger thTriggerPara = new KBCfgHumidityTrigger();
            thTriggerPara.setTriggerType(KBCfgTrigger.KBTriggerTypeHumidity);
            thTriggerPara.setTriggerAction(KBCfgTrigger.KBTriggerActionRptApp);

            //set trigger condition that report temperature and humidity by realtime
            thTriggerPara.setTriggerHtParaMask(KBCfgHumidityTrigger.KBTriggerHTParaMaskRpt2App);

            mEnableTHData2App.setEnabled(false);
            this.mBeacon.modifyTrigger(thTriggerPara, new KBeacon.ActionCallback() {
                public void onActionComplete(boolean bConfigSuccess, KBException error) {
                    mEnableTHData2App.setEnabled(true);
                    if (bConfigSuccess) {
                        Log.v(LOG_TAG, "set temp&humidity trigger event report to app");

                        //subscribe humidity notify
                        if (!mBeacon.isSensorDataSubscribe(KBHumidityNotifyData.class)) {
                            mBeacon.subscribeSensorDataNotify(KBHumidityNotifyData.class, DevicePannelActivity.this, new KBeacon.ActionCallback() {
                                @Override
                                public void onActionComplete(boolean bConfigSuccess, KBException error) {
                                    if (bConfigSuccess) {
                                        Log.v(LOG_TAG, "subscribe temperature and humidity data success");
                                    } else {
                                        Log.v(LOG_TAG, "subscribe temperature and humidity data failed");
                                    }
                                }
                            });
                        }

                    } else {
                        toastShow("enable temp&humidity error:" + error.errorCode);
                    }
                }
            });
        }catch (Exception excpt)
        {
            toastShow("config data is invalid");
            excpt.printStackTrace();
        }
    }

    //The device will start broadcasting when temperature&humidity trigger event happened
    //for example, the humidity > 70% or temperature < 10 or temperature > 50
    public void enableTHChangeTriggerEvtRpt2Adv() {
        if (!mBeacon.isConnected()) {
            toastShow("Device is not connected");
            return;
        }

        //check device capability
        int nTriggerCapability = mBeacon.triggerCapability();
        if ((nTriggerCapability & KBCfgTrigger.KBTriggerTypeHumidity) == 0) {
            Log.e(LOG_TAG, "device does not support humidity trigger");
            return;
        }

        KBCfgHumidityTrigger thTriggerPara = new KBCfgHumidityTrigger();

        try {
            //set trigger type
            thTriggerPara.setTriggerType(KBCfgTrigger.KBTriggerTypeHumidity);

            //set trigger advertisement enable
            thTriggerPara.setTriggerAction(KBCfgTrigger.KBTriggerActionAdv);

            //set trigger adv mode to adv only on trigger
            thTriggerPara.setTriggerAdvMode(KBCfgTrigger.KBTriggerAdvOnlyMode);

            //set trigger condition
            thTriggerPara.setTriggerHtParaMask(KBCfgHumidityTrigger.KBTriggerHTParaMaskTemperatureAbove
                    | KBCfgHumidityTrigger.KBTriggerHTParaMaskTemperatureBelow
                    | KBCfgHumidityTrigger.KBTriggerHTParaMaskHumidityAbove);
            thTriggerPara.setTriggerTemperatureAbove(50);
            thTriggerPara.setTriggerTemperatureBelow(-10);
            thTriggerPara.setTriggerHumidityAbove(70);

            //set trigger adv type
            thTriggerPara.setTriggerAdvType(KBAdvType.KBAdvTypeSensor);

            //set trigger adv duration to 20 seconds
            thTriggerPara.setTriggerAdvTime(20);

            //set the trigger adv interval to 500ms
            thTriggerPara.setTriggerAdvInterval(500f);
        } catch (KBException excpt) {
            excpt.printStackTrace();
            return;
        }

        //enable humidity trigger
        mEnableTHTrigger2Adv.setEnabled(false);
        this.mBeacon.modifyTrigger(thTriggerPara, new KBeacon.ActionCallback() {
            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                mEnableTHTrigger2Adv.setEnabled(true);
                if (bConfigSuccess) {
                    toastShow("enable temp&humidity trigger success");
                } else {
                    toastShow("enable temp&humidity error:" + error.errorCode);
                }
            }
        });
    }

    //the device will send event to app when temperature&humidity trigger event happened
    //for example, the humidity > 50%
    //the app must subscribe the notification event if it want receive the event
    public void enableTHChangeTriggerEvtRpt2App(){
        if (!mBeacon.isConnected()) {
            toastShow("Device is not connected");
            return;
        }

        //check device capability
        int nTriggerCapability = mBeacon.triggerCapability();
        if ((nTriggerCapability & KBCfgTrigger.KBTriggerTypeHumidity) == 0) {
            Log.e(LOG_TAG, "device does not support humidity trigger");
            return;
        }


        try {
            KBCfgHumidityTrigger thTriggerPara = new KBCfgHumidityTrigger();

            //set trigger type
            thTriggerPara.setTriggerType(KBCfgTrigger.KBTriggerTypeHumidity);

            //set trigger event that report to app
            //you can also set the trigger event to both app and advertisement.  (KBCfgTrigger.KBTriggerActionRptApp | KBCfgTrigger.KBTriggerActionAdv)
            thTriggerPara.setTriggerAction(KBCfgTrigger.KBTriggerActionRptApp | KBCfgTrigger.KBTriggerActionAdv);

            //set trigger condition
            thTriggerPara.setTriggerHtParaMask(KBCfgHumidityTrigger.KBTriggerHTParaMaskHumidityAbove);
            thTriggerPara.setTriggerHumidityAbove(70);

            mEnableTHTrigger2App.setEnabled(false);
            this.mBeacon.modifyTrigger(thTriggerPara, new KBeacon.ActionCallback() {
                public void onActionComplete(boolean bConfigSuccess, KBException error) {
                    mEnableTHTrigger2App.setEnabled(true);
                    if (bConfigSuccess) {
                        toastShow("enable temp&humidity trigger success");

                        //subscribe humidity notify
                        if (!mBeacon.isSensorDataSubscribe(KBHumidityNotifyData.class)) {
                            mBeacon.subscribeSensorDataNotify(KBHumidityNotifyData.class, DevicePannelActivity.this, new KBeacon.ActionCallback() {
                                @Override
                                public void onActionComplete(boolean bConfigSuccess, KBException error) {
                                    if (bConfigSuccess) {
                                        Log.v(LOG_TAG, "subscribe temperature and humidity data success");
                                    } else {
                                        Log.v(LOG_TAG, "subscribe temperature and humidity data failed");
                                    }
                                }
                            });
                        }
                    } else {
                        toastShow("enable temp&humidity error:" + error.errorCode);
                    }
                }
            });
        }catch (Exception excpt)
        {
            toastShow("config data is invalid");
            excpt.printStackTrace();
        }
    }


    //The device will start broadcasting when motion trigger event happened
    public void enableMotionTriggerToAdv() {
        if (!mBeacon.isConnected()) {
            toastShow("Device is not connected");
            return;
        }

        //check device capability
        int nTriggerCapability = mBeacon.triggerCapability();
        if ((nTriggerCapability & KBCfgTrigger.KBTriggerTypeMotion) == 0) {
            toastShow("Device does not supported motion sensor");
            return;
        }

        KBCfgTrigger mtionTriggerPara = new KBCfgTrigger();

        try {
            //set trigger type
            mtionTriggerPara.setTriggerType(KBCfgTrigger.KBTriggerTypeMotion);

            //set trigger advertisement enable
            mtionTriggerPara.setTriggerAction(KBCfgTrigger.KBTriggerActionAdv);

            //set trigger adv mode to adv only on trigger
            mtionTriggerPara.setTriggerAdvMode(KBCfgTrigger.KBTriggerAdvOnlyMode);

            //set motion detection sensitive
            mtionTriggerPara.setTriggerPara(10);

            //set trigger adv type
            mtionTriggerPara.setTriggerAdvType(KBAdvType.KBAdvTypeSensor);

            //set trigger adv duration to 20 seconds
            mtionTriggerPara.setTriggerAdvTime(20);

            //set the trigger adv interval to 500ms
            mtionTriggerPara.setTriggerAdvInterval(500f);
        } catch (KBException excpt) {
            excpt.printStackTrace();
            return;
        }

        //enable motion trigger
        mEnableAxisAdv.setEnabled(false);
        this.mBeacon.modifyTrigger(mtionTriggerPara, new KBeacon.ActionCallback() {
            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                mEnableAxisAdv.setEnabled(true);
                if (bConfigSuccess) {
                    toastShow("enable motion trigger success");
                } else {
                    toastShow("enable motion trigger error:" + error.errorCode);
                }
            }
        });
    }

    //Enable motion trigger event to connected app
    //Require the KBeacon firmware version >= 5.20
    public void enableMotionTriggerToApp() {
        if (!mBeacon.isConnected()) {
            toastShow("Device is not connected");
            return;
        }

        //check device capability
        int nTriggerCapability = mBeacon.triggerCapability();
        if ((nTriggerCapability & KBCfgTrigger.KBTriggerTypeMotion) == 0) {
            toastShow("Device does not supported motion sensor");
            return;
        }

        KBCfgTrigger mtionTriggerPara = new KBCfgTrigger();

        try {
            //set trigger type
            mtionTriggerPara.setTriggerType(KBCfgTrigger.KBTriggerTypeMotion);

            //set trigger event that report to connected app
            mtionTriggerPara.setTriggerAction(KBCfgTrigger.KBTriggerActionRptApp);

            //set motion detection sensitive
            mtionTriggerPara.setTriggerPara(3);
        } catch (KBException excpt) {
            excpt.printStackTrace();
            return;
        }

        //enable push button trigger
        mEnableMotionTrigger.setEnabled(false);
        this.mBeacon.modifyTrigger(mtionTriggerPara, new KBeacon.ActionCallback() {
            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                mEnableMotionTrigger.setEnabled(true);
                if (bConfigSuccess) {
                    toastShow("enable push button trigger success");

                    //subscribe humidity notify
                    if (!mBeacon.isSensorDataSubscribe(KBNotifyButtonEvtData.class)) {
                        mBeacon.subscribeSensorDataNotify(KBNotifyMotionEvtData.class, DevicePannelActivity.this, new KBeacon.ActionCallback() {
                            @Override
                            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                                if (bConfigSuccess) {
                                    Log.v(LOG_TAG, "subscribe motion notify success");
                                } else {
                                    Log.v(LOG_TAG, "subscribe motion notify failed");
                                }
                            }
                        });
                    }

                } else {
                    toastShow("enable push button trigger error:" + error.errorCode);
                }
            }
        });
    }

    private void disableMotionTrigger(){
        if (!mBeacon.isConnected()) {
            toastShow("Device is not connected");
            return;
        }

        //check device capability
        int nTriggerCapability = mBeacon.triggerCapability();
        if ((nTriggerCapability & KBCfgTrigger.KBTriggerTypeMotion) == 0) {
            toastShow("Device does not supported motion sensor");
            return;
        }

        KBCfgTrigger motionTriggerPara = new KBCfgTrigger();
        try {
            //set trigger type
            motionTriggerPara.setTriggerType(KBCfgTrigger.KBTriggerTypeMotion);

            //set trigger advertisement enable
            motionTriggerPara.setTriggerAction(KBCfgTrigger.KBTriggerActionOff);
        } catch (KBException excpt) {
            Log.e(LOG_TAG, "Input paramaters invalid");
            return;
        }

        //disable push button trigger
        this.mBeacon.modifyTrigger(motionTriggerPara, new KBeacon.ActionCallback() {
            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                if (bConfigSuccess) {
                    toastShow("disable motion trigger success");
                } else {
                    toastShow("disable motion trigger error:" + error.errorCode);
                }
            }
        });
    }

    //The device will start broadcasting when button trigger event happened
    //for example, double click button
    public void enableButtonTrigger() {
        if (!mBeacon.isConnected()) {
            toastShow("Device is not connected");
            return;
        }

        //check device capability
        int nTriggerCapability = mBeacon.triggerCapability();
        if ((nTriggerCapability & KBCfgTrigger.KBTriggerTypeButton) == 0) {
            toastShow("Device does not supported button sensor");
            return;
        }

        KBCfgTrigger btnTriggerPara = new KBCfgTrigger();

        try {
            //set trigger type
            btnTriggerPara.setTriggerType(KBCfgTrigger.KBTriggerTypeButton);

            //set trigger advertisement enable
            btnTriggerPara.setTriggerAction(KBCfgTrigger.KBTriggerActionAdv);

            //set trigger adv mode to trigger adv only mode, if you want the kbeacon always advertisement,
            //please set the adv mode to KBTriggerAdv2AliveMode
            btnTriggerPara.setTriggerAdvMode(KBCfgTrigger.KBTriggerAdvOnlyMode);

            //set trigger button para, enable single click and double click
            btnTriggerPara.setTriggerPara(KBCfgTrigger.KBTriggerBtnSingleClick | KBCfgTrigger.KBTriggerBtnDoubleClick);

            //set trigger adv type
            btnTriggerPara.setTriggerAdvType(KBAdvType.KBAdvTypeIBeacon);

            //set trigger adv duration to 20 seconds
            btnTriggerPara.setTriggerAdvTime(20);

            //set the trigger adv interval to 500ms
            btnTriggerPara.setTriggerAdvInterval(500f);
        } catch (KBException excpt) {
            excpt.printStackTrace();
            return;
        }

        //enable push button trigger
        mEnableBtnTrigger.setEnabled(false);
        this.mBeacon.modifyTrigger(btnTriggerPara, new KBeacon.ActionCallback() {
            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                mEnableBtnTrigger.setEnabled(true);
                if (bConfigSuccess) {
                    toastShow("enable push button trigger success");
                } else {
                    toastShow("enable push button trgger error:" + error.errorCode);
                }
            }
        });
    }

    //enable button press trigger event to app when KBeacon was connected
    //Requre the KBeacon firmware version >= 5.20
    public void enableBtnTriggerEvtToApp() {
        if (!mBeacon.isConnected()) {
            toastShow("Device is not connected");
            return;
        }

        //check device capability
        int nTriggerCapability = mBeacon.triggerCapability();
        if ((nTriggerCapability & KBCfgTrigger.KBTriggerTypeButton) == 0) {
            Log.e(LOG_TAG, "device does not support button trigger");
            return;
        }

        KBCfgTrigger btnTriggerPara = new KBCfgTrigger();

        try {
            //set trigger type
            btnTriggerPara.setTriggerType(KBCfgTrigger.KBTriggerTypeButton);

            //set trigger event that report to connected app
            btnTriggerPara.setTriggerAction(KBCfgTrigger.KBTriggerActionRptApp);

            //set trigger button para, enable single click and double click
            btnTriggerPara.setTriggerPara(KBCfgTrigger.KBTriggerBtnSingleClick | KBCfgTrigger.KBTriggerBtnDoubleClick);

            //set the trigger adv interval to 500ms
            btnTriggerPara.setTriggerAdvInterval(500f);
        } catch (KBException excpt) {
            excpt.printStackTrace();
            return;
        }

        //enable push button trigger
        mDisableBtnTrigger.setEnabled(false);
        this.mBeacon.modifyTrigger(btnTriggerPara, new KBeacon.ActionCallback() {
            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                mDisableBtnTrigger.setEnabled(true);
                if (bConfigSuccess) {
                    toastShow("enable push button trigger success");

                    //subscribe button notify
                    if (!mBeacon.isSensorDataSubscribe(KBNotifyButtonEvtData.class)) {
                        mBeacon.subscribeSensorDataNotify(KBNotifyButtonEvtData.class, DevicePannelActivity.this, new KBeacon.ActionCallback() {
                            @Override
                            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                                if (bConfigSuccess) {
                                    Log.v(LOG_TAG, "subscribe button notify success");
                                } else {
                                    Log.v(LOG_TAG, "subscribe button notify failed");
                                }
                            }
                        });
                    }

                } else {
                    toastShow("enable push button trgger error:" + error.errorCode);
                }
            }
        });
    }

    //Alarm when button was pressed
    //Requre the KBeacon firmware version >= 5.20
    public void enableBtnTriggerEvtToAlarm() {
        if (!mBeacon.isConnected()) {
            toastShow("Device is not connected");
            return;
        }

        //check device capability
        int nTriggerCapability = mBeacon.triggerCapability();
        if ((nTriggerCapability & KBCfgTrigger.KBTriggerTypeButton) == 0) {
            Log.e(LOG_TAG, "device does not support button trigger");
            return;
        }

        KBCfgTrigger btnTriggerPara = new KBCfgTrigger();

        try {
            //set trigger type
            btnTriggerPara.setTriggerType(KBCfgTrigger.KBTriggerTypeButton);

            //set trigger event that report to connected app
            btnTriggerPara.setTriggerAction(KBCfgTrigger.KBTriggerActionAlert);

            //set trigger button para, enable single click and double click
            btnTriggerPara.setTriggerPara(KBCfgTrigger.KBTriggerBtnSingleClick | KBCfgTrigger.KBTriggerBtnDoubleClick);

            //set the trigger adv interval to 500ms
            btnTriggerPara.setTriggerAdvInterval(500f);
        } catch (KBException excpt) {
            excpt.printStackTrace();
            return;
        }

        //enable push button trigger
        mDisableBtnTrigger.setEnabled(false);
        this.mBeacon.modifyTrigger(btnTriggerPara, new KBeacon.ActionCallback() {
            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                mDisableBtnTrigger.setEnabled(true);
                if (bConfigSuccess) {
                    toastShow("enable push button trigger success");

                    //subscribe button notify
                    if (!mBeacon.isSensorDataSubscribe(KBNotifyButtonEvtData.class)) {
                        mBeacon.subscribeSensorDataNotify(KBNotifyButtonEvtData.class, DevicePannelActivity.this, new KBeacon.ActionCallback() {
                            @Override
                            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                                if (bConfigSuccess) {
                                    Log.v(LOG_TAG, "subscribe button notify success");
                                } else {
                                    Log.v(LOG_TAG, "subscribe button notify failed");
                                }
                            }
                        });
                    }

                } else {
                    toastShow("enable push button trgger error:" + error.errorCode);
                }
            }
        });
    }


    //enable button press trigger event to app when KBeacon was connected
    //if app does not connect to device, then enable button trigger event to advertisement
    //for firmware version >= 5.20
    public void enableBtnTriggerEvtToApp2Adv() {
        if (!mBeacon.isConnected()) {
            toastShow("Device is not connected");
            return;
        }

        //check device capability
        int nTriggerCapability = mBeacon.triggerCapability();
        if ((nTriggerCapability & KBCfgTrigger.KBTriggerTypeButton) == 0) {
            Log.e(LOG_TAG, "device does not support button trigger");
            return;
        }

        KBCfgTrigger btnTriggerPara = new KBCfgTrigger();

        try {
            //set trigger type
            btnTriggerPara.setTriggerType(KBCfgTrigger.KBTriggerTypeButton);

            //set trigger advertisement and app notify
            btnTriggerPara.setTriggerAction(KBCfgTrigger.KBTriggerActionRptApp | KBCfgTrigger.KBTriggerActionAdv);

            //set always advertisement
            btnTriggerPara.setTriggerAdvMode(KBCfgTrigger.KBTriggerAdv2AliveMode);

            //set trigger adv type
            btnTriggerPara.setTriggerAdvType(KBAdvType.KBAdvTypeIBeacon);

            //set trigger button para, enable single click and double click
            btnTriggerPara.setTriggerPara(KBCfgTrigger.KBTriggerBtnSingleClick | KBCfgTrigger.KBTriggerBtnHold);

            //set the trigger adv interval to 500ms
            btnTriggerPara.setTriggerAdvInterval(500f);
        } catch (KBException excpt) {
            excpt.printStackTrace();
            return;
        }

        //enable push button trigger
        mDisableBtnTrigger.setEnabled(false);
        this.mBeacon.modifyTrigger(btnTriggerPara, new KBeacon.ActionCallback() {
            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                mDisableBtnTrigger.setEnabled(true);
                if (bConfigSuccess) {
                    toastShow("enable push button trigger success");

                    //subscribe humidity notify
                    if (!mBeacon.isSensorDataSubscribe(KBNotifyButtonEvtData.class)) {
                        mBeacon.subscribeSensorDataNotify(KBNotifyButtonEvtData.class, DevicePannelActivity.this, new KBeacon.ActionCallback() {
                            @Override
                            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                                if (bConfigSuccess) {
                                    Log.v(LOG_TAG, "subscribe button notify success");
                                } else {
                                    Log.v(LOG_TAG, "subscribe button notify failed");
                                }
                            }
                        });
                    }

                } else {
                    toastShow("enable push button trgger error:" + error.errorCode);
                }
            }
        });
    }

    public void disableButtonTrigger() {
        if (!mBeacon.isConnected()) {
            toastShow("Device is not connected");
            return;
        }

        //check device capability
        int nTriggerCapability = mBeacon.triggerCapability();
        if ((nTriggerCapability & KBCfgTrigger.KBTriggerTypeButton) == 0) {
            toastShow( "device does not support push button trigger");
            return;
        }

        KBCfgTrigger btnTriggerPara = new KBCfgTrigger();

        try {
            //set trigger type
            btnTriggerPara.setTriggerType(KBCfgTrigger.KBTriggerTypeButton);

            //set trigger advertisement enable
            btnTriggerPara.setTriggerAction(KBCfgTrigger.KBTriggerActionOff);
        } catch (KBException excpt) {
            Log.e(LOG_TAG, "Input paramaters invalid");
            return;
        }

        //disable push button trigger
        this.mBeacon.modifyTrigger(btnTriggerPara, new KBeacon.ActionCallback() {
            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                if (bConfigSuccess) {
                    toastShow("disable push button trigger success");
                } else {
                    toastShow("disable push button trigger error:" + error.errorCode);
                }
            }
        });
    }

    public void readButtonTriggerPara() {
        if (!mBeacon.isConnected()) {
            toastShow("Device is not connected");
            return;
        }

        //check device capability
        int nTriggerCapability = mBeacon.triggerCapability();
        if ((nTriggerCapability & KBCfgTrigger.KBTriggerTypeButton) == 0) {
            toastShow("device does not support push button trigger");
            return;
        }

        //enable push button trigger
        this.mBeacon.readTriggerConfig(KBCfgTrigger.KBTriggerTypeButton, new KBeacon.ReadConfigCallback() {
            public void onReadComplete(boolean bConfigSuccess, HashMap<String, Object> paraDicts, KBException error) {
                if (bConfigSuccess) {
                    ArrayList<KBCfgTrigger> btnTriggerCfg = (ArrayList<KBCfgTrigger>)paraDicts.get("trObj");
                    if (btnTriggerCfg != null)
                    {
                        KBCfgTrigger btnCfg = btnTriggerCfg.get(0);

                        Log.v(LOG_TAG, "read trigger type:" + btnCfg.getTriggerType());
                        if (btnCfg.getTriggerAction() > 0)
                        {
                            //button enable mask
                            Integer triggerPara = btnCfg.getTriggerPara();
                            if (triggerPara != null) {
                                if ((triggerPara & KBCfgTrigger.KBTriggerBtnSingleClick) > 0) {
                                    Log.v(LOG_TAG, "Enable single click trigger");
                                }
                                if ((triggerPara & KBCfgTrigger.KBTriggerBtnDoubleClick) > 0) {
                                    Log.v(LOG_TAG, "Enable double click trigger");
                                }
                                if ((triggerPara & KBCfgTrigger.KBTriggerBtnHold) > 0) {
                                    Log.v(LOG_TAG, "Enable hold press trigger");
                                }
                            }

                            //button trigger adv mode
                            if (btnCfg.getTriggerAdvMode()== KBCfgTrigger.KBTriggerAdvOnlyMode)
                            {
                                Log.v(LOG_TAG, "device only advertisement when trigger event happened");
                            }
                            else if (btnCfg.getTriggerAdvMode() == KBCfgTrigger.KBTriggerAdv2AliveMode)
                            {
                                Log.v(LOG_TAG, "device will always advertisement, but the uuid is difference when trigger event happened");
                            }

                            //button trigger adv type
                            Log.v(LOG_TAG, "Button trigger adv type:" + btnCfg.getTriggerAdvType());

                            //button trigger adv duration, unit is sec
                            Log.v(LOG_TAG, "Button trigger adv duration:" + btnCfg.getTriggerAdvTime());

                            //button trigger adv interval, unit is ms
                            Log.v(LOG_TAG, "Button trigger adv interval:" +  btnCfg.getTriggerAdvInterval());
                        }
                        else
                        {
                            Log.v(LOG_TAG, "trigger type:" + btnCfg.getTriggerType() + " is off");
                        }
                    }

                    toastShow("enable push button trigger success");
                } else {
                    toastShow("enable push button trgger error:" + error.errorCode);
                }
            }
        });
    }

    //ring device
    public void ringDevice() {
        if (!mBeacon.isConnected()) {
            toastShow("Device is not connected");
            return;
        }

        KBCfgCommon cfgCommon = (KBCfgCommon)mBeacon.getConfigruationByType(KBCfgType.KBConfigTypeCommon);
        if (!cfgCommon.isSupportBeep())
        {
            toastShow("device does not support ring feature");
            return;
        }

        mRingButton.setEnabled(false);
        HashMap<String, Object> cmdPara = new HashMap<>(5);
        cmdPara.put("msg", "ring");
        cmdPara.put("ringTime", 20000);   //ring times, uint is ms
        cmdPara.put("ringType", 2);  //0x1:beep; 0x2:beep alert only; 0x4: virbrate;
        cmdPara.put("ledOn", 200);   //valid when ringType set to 0x0 or 0x2
        cmdPara.put("ledOff", 1800); //valid when ringType set to 0x0 or 0x2
        mRingButton.setEnabled(false);
        mBeacon.sendCommand(cmdPara, new KBeacon.ActionCallback() {
            @Override
            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                mRingButton.setEnabled(true);
                if (bConfigSuccess)
                {
                    toastShow("send command to beacon success");
                }
                else
                {
                    toastShow("send command to beacon error:" + error.errorCode);
                }
            }
        });
    }

    //The accelerometer can only be in one operating mode at the same time,
    // detected X/Y/Z axis or detect motion, so please disable motion trigger before enable X/Y/Z axis detection
    public void  enableAdvTypeIncludeAccXYZ()
    {
        KBCfgCommon oldCommonCfg = (KBCfgCommon)mBeacon.getConfigruationByType(KBCfgType.KBConfigTypeCommon);
        KBCfgSensor oldSensorCfg = (KBCfgSensor)mBeacon.getConfigruationByType(KBCfgType.KBConfigTypeSensor);

        if (!mBeacon.isConnected())
        {
            toastShow("Device is not connected");
            return;
        }

        if (!oldCommonCfg.isSupportAccSensor())
        {
            toastShow("Device does not supported acc sensor");
            return;
        }

        try {
            //enable ksensor advertisement
            ArrayList<KBCfgBase> newCfg = new ArrayList<>(2);
            if ((oldCommonCfg.getAdvType() & KBAdvType.KBAdvTypeSensor) == 0) {
                KBCfgCommon newCommonCfg = new KBCfgCommon();
                newCommonCfg.setAdvType(KBAdvType.KBAdvTypeSensor);
                newCfg.add(newCommonCfg);
            }

            //enable acc sensor XYZ dection
            Integer nOldSensorType = oldSensorCfg.getSensorType();
            if ((nOldSensorType & KBCfgSensor.KBSensorTypeAcc) == 0) {
                KBCfgSensor sensorCfg = new KBCfgSensor();
                sensorCfg.setSensorType(KBCfgSensor.KBSensorTypeAcc | nOldSensorType);
                newCfg.add(sensorCfg);
            }

            mEnableAxisAdv.setEnabled(false);
            mBeacon.modifyConfig(newCfg, new KBeacon.ActionCallback() {
                @Override
                public void onActionComplete(boolean bConfigSuccess, KBException error) {
                    mEnableAxisAdv.setEnabled(true);
                    if (bConfigSuccess){
                        Log.v(LOG_TAG, "enable axis advertisement success");
                    }else{
                        Log.v(LOG_TAG, "enable axis advertisement failed");
                    }
                }
            });
        }
        catch (Exception excpt)
        {
            Log.v(LOG_TAG, "config acc advertisement failed");
        }
    }


    public void setTHMeasureParameters()
    {
        if (!mBeacon.isConnected())
        {
            toastShow("Device is not connected");
            return;
        }

        KBCfgCommon commonCfg = (KBCfgCommon)mBeacon.getConfigruationByType(KBCfgType.KBConfigTypeCommon);
        if (!commonCfg.isSupportHumiditySensor())
        {
            toastShow("Device does not supported humidity sensor");
            return;
        }


        try {
            KBCfgSensor cfgSensor = new KBCfgSensor();

            //unit is second, set measure temperature and humidity interval
            cfgSensor.setSensorHtMeasureInterval(2);

            //unit is 0.1%, if abs(current humidity - last saved humidity) > 3, then save new record
            cfgSensor.setHumidityChangeThreshold(30);

            //unit is 0.1 Celsius, if abs(current temperature - last saved temperature) > 0.5, then save new record
            cfgSensor.setTemperatureChangeThreshold(5);

            ArrayList<KBCfgBase> cfgList = new ArrayList<>(2);
            cfgList.add(cfgSensor);
            mBeacon.modifyConfig(cfgList, new KBeacon.ActionCallback() {
                @Override
                public void onActionComplete(boolean bConfigSuccess, KBException error) {
                    if (bConfigSuccess)
                    {
                        toastShow("config data to beacon success");
                    }
                    else
                    {
                        toastShow("config failed for error:" + error.errorCode);
                    }
                }
            });
        }catch (Exception excpt)
        {
            toastShow("config data is invalid");
            excpt.printStackTrace();
        }
    }


    private int nDeviceConnState = KBeacon.KBStateDisconnected;

    public void onConnStateChange(KBeacon beacon, int state, int nReason)
    {
        if (state == KBeacon.KBStateConnected)
        {
            Log.v(LOG_TAG, "device has connected");
            invalidateOptionsMenu();

            nDeviceConnState = state;
        }
        else if (state == KBeacon.KBStateConnecting)
        {
            Log.v(LOG_TAG, "device start connecting");
            invalidateOptionsMenu();

            nDeviceConnState = state;
        }
        else if (state == KBeacon.KBStateDisconnecting) {
            Log.e(LOG_TAG, "connection error, now disconnecting");
            nDeviceConnState = state;
            invalidateOptionsMenu();
        }
        else
        {
            if (nDeviceConnState == KBeacon.KBStateConnecting)
            {
                if (nReason == KBConnectionEvent.ConnAuthFail)
                {
                    final EditText inputServer = new EditText(this);
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(getString(R.string.auth_error_title));
                    builder.setView(inputServer);
                    builder.setNegativeButton(R.string.Dialog_Cancel, null);
                    builder.setPositiveButton(R.string.Dialog_OK, null);
                    final AlertDialog alertDialog = builder.create();
                    alertDialog.show();

                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String strNewPassword = inputServer.getText().toString().trim();
                            if (strNewPassword.length() < 8|| strNewPassword.length() > 16)
                            {
                                Toast.makeText(DevicePannelActivity.this,
                                        R.string.connect_error_auth_format,
                                        Toast.LENGTH_SHORT).show();
                            }else {
                                mPref.setPassword(mDeviceAddress, strNewPassword);
                                alertDialog.dismiss();
                            }
                        }
                    });
                }
                else
                {
                    toastShow("connect to device failed, reason:" + nReason);
                }
            }

            Log.e(LOG_TAG, "device has disconnected:" +  nReason);
            invalidateOptionsMenu();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        /*
        if (mBeacon.getState() == KBeacon.KBStateConnected
            || mBeacon.getState() == KBeacon.KBStateConnecting){
            mBeacon.disconnect();
            invalidateOptionsMenu();
        }
        */
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.menu_connect){
            //connect and sync the UTC time to device
            KBConnPara connPara = new KBConnPara();
            connPara.utcTime = UTCTime.getUTCTimeSeconds();
            mBeacon.connectEnhanced(mPref.getPassword(mDeviceAddress),
                    20*1000,
                    connPara,
                    this);
            invalidateOptionsMenu();
        }
        else if(id == R.id.menu_disconnect){
            mBeacon.disconnect();
            invalidateOptionsMenu();
        }
        else if(id == android.R.id.home){
            mBeacon.disconnect();
        }

        return super.onOptionsItemSelected(item);
    }
}
