package com.kbeacon.ibeacondemo;

import android.app.AlertDialog;
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

import com.kbeacon.kbeaconlib.KBAdvPackage.KBAdvPacketEddyUID;
import com.kbeacon.kbeaconlib.KBAdvPackage.KBAdvType;
import com.kbeacon.kbeaconlib.KBCfgPackage.KBCfgBase;
import com.kbeacon.kbeaconlib.KBCfgPackage.KBCfgCommon;
import com.kbeacon.kbeaconlib.KBCfgPackage.KBCfgEddyUID;
import com.kbeacon.kbeaconlib.KBCfgPackage.KBCfgEddyURL;
import com.kbeacon.kbeaconlib.KBCfgPackage.KBCfgIBeacon;
import com.kbeacon.kbeaconlib.KBCfgPackage.KBCfgTrigger;
import com.kbeacon.kbeaconlib.KBCfgPackage.KBCfgType;
import com.kbeacon.kbeaconlib.KBConnectionEvent;
import com.kbeacon.kbeaconlib.KBException;
import com.kbeacon.kbeaconlib.KBeacon;
import com.kbeacon.kbeaconlib.KBeaconsMgr;

import java.util.ArrayList;
import java.util.HashMap;

public class DevicePannelActivity extends AppBaseActivity implements View.OnClickListener, KBeacon.ConnStateDelegate{

    public final static String DEVICE_MAC_ADDRESS = "DEVICE_MAC_ADDRESS";
    private final static String LOG_TAG = "DevicePannel";

    public final static String DEFAULT_PASSWORD = "0000000000000000";   //16 zero ascii

    private KBeaconsMgr mBeaconMgr;
    private String mDeviceAddress;
    private KBeacon mBeacon;

    //uiview
    private TextView mBeaconType, mBeaconStatus;
    private TextView mBeaconModel;
    private EditText mEditBeaconUUID;
    private EditText mEditBeaconMajor;
    private EditText mEditBeaconMinor;
    private EditText mEditBeaconAdvPeriod;
    private EditText mEditBeaconPassword;
    private EditText mEditBeaconTxPower;
    private EditText mEditBeaconName;
    private Button mDownloadButton, mRingButton, mTriggerButton;
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
        mEditBeaconUUID = (EditText)findViewById(R.id.editIBeaconUUID);
        mEditBeaconMajor = (EditText)findViewById(R.id.editIBeaconMajor);
        mEditBeaconMinor = (EditText)findViewById(R.id.editIBeaconMinor);
        mEditBeaconAdvPeriod = (EditText)findViewById(R.id.editBeaconAdvPeriod);
        mEditBeaconTxPower = (EditText)findViewById(R.id.editBeaconTxPower);
        mEditBeaconName = (EditText)findViewById(R.id.editBeaconname);
        mDownloadButton = (Button) findViewById(R.id.buttonSaveData);
        mEditBeaconPassword = (EditText)findViewById(R.id.editPassword);
        mDownloadButton.setEnabled(false);
        mDownloadButton.setOnClickListener(this);

        mRingButton = (Button) findViewById(R.id.ringDevice);
        mRingButton.setOnClickListener(this);
        mTriggerButton = (Button) findViewById(R.id.enableBtnTrigger);
        mTriggerButton.setOnClickListener(this);
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
            case R.id.buttonSaveData:
                updateViewToDevice();
                break;

            case R.id.ringDevice:
                ringDevice();
                break;

            case R.id.enableBtnTrigger:
                enableButtonTrigger();
                break;
            default:
                break;
        }
    }

    public void enableButtonTrigger() {
        if (!mBeacon.isConnected()) {
            return;
        }

        //check device capability
        int nTriggerCapability = mBeacon.triggerCapability();
        if ((nTriggerCapability & KBCfgTrigger.KBTriggerTypeButton) == 0) {
            Log.e(LOG_TAG, "device does not support push button trigger");
            return;
        }

        KBCfgTrigger btnTriggerPara = new KBCfgTrigger();

        try {
            //set trigger type
            btnTriggerPara.setTriggerType(KBCfgTrigger.KBTriggerTypeButton);

            //set trigger advertisement enable
            btnTriggerPara.setTriggerAction(KBCfgTrigger.KBTriggerActionAdv);

            //set trigger adv mode to adv only on trigger
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
        mTriggerButton.setEnabled(false);
        this.mBeacon.modifyTrigger(btnTriggerPara, new KBeacon.ActionCallback() {
            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                mTriggerButton.setEnabled(true);
                if (bConfigSuccess) {
                    toastShow("enable push button trigger success");
                } else {
                    toastShow("enable push button trgger error:" + error.errorCode);
                }
            }
        });
    }

    public void disableButtonTrigger() {
        if (!mBeacon.isConnected()) {
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

                        Log.v(LOG_TAG, "trigger type:" + btnCfg.getTriggerType());
                        if (btnCfg.getTriggerAction() > 0)
                        {
                            //button enable mask
                            int nButtonEnableInfo = btnCfg.getTriggerPara();
                            if ((nButtonEnableInfo & KBCfgTrigger.KBTriggerBtnSingleClick) > 0)
                            {
                                Log.v(LOG_TAG, "Enable single click trigger");
                            }
                            if ((nButtonEnableInfo & KBCfgTrigger.KBTriggerBtnDoubleClick) > 0)
                            {
                                Log.v(LOG_TAG, "Enable double click trigger");
                            }
                            if ((nButtonEnableInfo & KBCfgTrigger.KBTriggerBtnHold) > 0)
                            {
                                Log.v(LOG_TAG, "Enable hold press trigger");
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
            return;
        }

        KBCfgCommon cfgCommon = (KBCfgCommon)mBeacon.getConfigruationByType(KBCfgType.KBConfigTypeCommon);
        if (!cfgCommon.isSupportBeep())
        {
            toastShow("device does not support ring feature");
            return;
        }

        mDownloadButton.setEnabled(false);
        HashMap<String, Object> cmdPara = new HashMap<>(5);
        cmdPara.put("msg", "ring");
        cmdPara.put("ringTime", 20000);   //ring times, uint is ms
        cmdPara.put("ringType", 2);  //0x0:led flash only; 0x1:beep alert only; 0x2 led flash and beep alert;
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

    void updateViewToDevice()
    {
        if (!mBeacon.isConnected())
        {
            return;
        }

        //get current configruation
        KBCfgCommon oldCommonCfg = (KBCfgCommon) mBeacon.getConfigruationByType(KBCfgType.KBConfigTypeCommon);
        KBCfgIBeacon oldBeaconCfg = (KBCfgIBeacon) mBeacon.getConfigruationByType(KBCfgType.KBConfigTypeIBeacon);
        KBCfgCommon newCommomCfg = new KBCfgCommon();
        KBCfgIBeacon newBeaconCfg = new KBCfgIBeacon();

        try {
            //check current beacon advertisment type, if current is not iBeacon, then enable it.
            int nAdvType = oldCommonCfg.getAdvType();
            if ((nAdvType & KBAdvType.KBAdvTypeIBeacon) == 0)
            {
                newCommomCfg.setAdvType(KBAdvType.KBAdvTypeIBeacon);
            }

            //adv period, check if user change adv period
            String strAdvPeriod = mEditBeaconAdvPeriod.getText().toString();
            if (Utils.isPositiveInteger(strAdvPeriod)) {
                Float newAdvPeriod = Float.valueOf(strAdvPeriod);
                if (!newAdvPeriod.equals(oldCommonCfg.getAdvPeriod())) {
                    newCommomCfg.setAdvPeriod(newAdvPeriod);
                }
            }

            //tx power, check if user change tx power
            String strTxPower = mEditBeaconTxPower.getText().toString();
            if (Utils.isPositiveInteger(strTxPower) || Utils.isMinusInteger(strTxPower)) {
                Integer newTxPower = Integer.valueOf(strTxPower);
                if (newTxPower > oldCommonCfg.getMaxTxPower() || newTxPower < oldCommonCfg.getMinTxPower()) {
                    toastShow("tx power not valid");
                    return;
                }
                if (!newTxPower.equals(oldCommonCfg.getTxPower())) {
                    newCommomCfg.setTxPower(newTxPower);
                }
            }

            //modify device name
            String strDeviceName = mEditBeaconName.getText().toString();
            if (!strDeviceName.equals(oldCommonCfg.getName())) {
                newCommomCfg.setName(strDeviceName);
            }

            //the password length must >=8 bytes and <= 16 bytes
            //Be sure to remember your new password, if you forget it, you won’t be able to connect to it.
            newCommomCfg.setPassword("123456789");
            String strPassword = mEditBeaconPassword.getText().toString();
            if (strPassword.length() > 0) {
                if (strPassword.length() < 8 || strPassword.length() > 16) {
                    toastShow("password length error");
                    return;
                } else {
                    newCommomCfg.setPassword(strPassword);
                    mNewPassword = strPassword;
                }
            }else{
                mNewPassword = null;
            }

            //iBeacon data
            String uuid = mEditBeaconUUID.getText().toString();
            if (!uuid.equals(oldBeaconCfg.getUuid())){
                newBeaconCfg.setUuid(uuid);
            }

            //iBeacon major id data
            String strMajorID = mEditBeaconMajor.getText().toString();
            if (Utils.isPositiveInteger(strMajorID))
            {
                Integer majorID = Integer.valueOf(strMajorID);
                if (!majorID.equals(oldBeaconCfg.getMajorID())){
                    newBeaconCfg.setMajorID(majorID);
                }
            }

            //iBeacon major id data
            String strMinorID = mEditBeaconMinor.getText().toString();
            if (Utils.isPositiveInteger(strMinorID))
            {
                Integer minorID = Integer.valueOf(strMinorID);
                if (!minorID.equals(oldBeaconCfg.getMinorID())){
                    newBeaconCfg.setMinorID(minorID);
                }
            }

            ArrayList<KBCfgBase> cfgList = new ArrayList<>(2);
            cfgList.add(newCommomCfg);
            cfgList.add(newBeaconCfg);
            mDownloadButton.setEnabled(false);
            mBeacon.modifyConfig(cfgList, new KBeacon.ActionCallback() {
                @Override
                public void onActionComplete(boolean bConfigSuccess, KBException error) {
                    mDownloadButton.setEnabled(true);
                    if (bConfigSuccess)
                    {
                        if (mNewPassword != null) {
                            mPref.setPassword(mDeviceAddress, mNewPassword);
                            mEditBeaconPassword.setText("");
                        }
                        toastShow("config data to beacon success");
                    }
                    else
                    {
                        if (error.errorCode == KBException.KBEvtCfgNoParameters)
                        {
                            toastShow("Config parameters is null, no data need to be sent");
                        }
                        else if (error.errorCode == KBException.KBEvtCfgBusy)
                        {
                            toastShow("Another configruation is not complete");
                        }
                        else if (error.errorCode == KBException.KBEvtCfgFailed)
                        {
                            toastShow("Device return failed");
                        }
                        else if (error.errorCode == KBException.KBEvtCfgTimeout)
                        {
                            toastShow("send parameters to device timeout");
                        }
                        else if (error.errorCode == KBException.KBEvtCfgInputInvalid)
                        {
                            toastShow("Input parameters invalid");
                        }
                        else if (error.errorCode == KBException.KBEvtCfgStateError)
                        {
                            toastShow("Please make sure the device was connected");
                        }
                        else if (error.errorCode == KBException.KBEvtCfgNotSupport)
                        {
                            toastShow("Device does not support the parameters");
                        }
                        else
                        {
                            toastShow("config failed for error:" + error.errorCode);
                        }
                    }
                }
            });
        }catch (Exception excpt)
        {
            toastShow("config data is invalid");
            excpt.printStackTrace();
        }
    }

    public void updateBeaconCommonPara() {
        if (!mBeacon.isConnected()) {
            return;
        }

        //change parameters
        KBCfgCommon newCommomCfg = new KBCfgCommon();
        try {
            //set device name
            newCommomCfg.setName("KBeaconDemo");

            //set advertisement period
            newCommomCfg.setAdvPeriod(1000f);

            //set tx power on
            newCommomCfg.setTxPower(-4);

            //set the device to un-connectable.
            // Warning: if the app set the KBeacon to un-connectable, the app can not connect to it if it does not has button.
            // If the device has button, the device can enter connect-able advertisement for 60 seconds when click on the button
            newCommomCfg.setAdvConnectable(0);

            //set device to always power on
            //the autoAdvAfterPowerOn is enable, the device will not allowed power off by long press button
            newCommomCfg.setAutoAdvAfterPowerOn(0);

        } catch (KBException excpt) {
            toastShow("input data invalid");
            excpt.printStackTrace();
        }

        ArrayList<KBCfgBase> cfgList = new ArrayList<>(1);
        cfgList.add(newCommomCfg);
        mBeacon.modifyConfig(cfgList, new KBeacon.ActionCallback() {
            @Override
            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                mDownloadButton.setEnabled(true);
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
    }

    public void updateIBeaconPara()
    {
        if (!mBeacon.isConnected()) {
            return;
        }

        //change parameters
        KBCfgCommon commonPara = new KBCfgCommon();
        KBCfgIBeacon iBeaconPara = new KBCfgIBeacon();
        try {
            //set adv type to iBeacon
            commonPara.setAdvType(KBAdvType.KBAdvTypeIBeacon);

            //set iBeacon para
            iBeaconPara.setUuid("E2C56DB5-DFFB-48D2-B060-D0F5A71096E0");
            iBeaconPara.setMajorID(645);
            iBeaconPara.setMinorID(741);

        } catch (KBException excpt) {
            toastShow("input data invalid");
            excpt.printStackTrace();
        }

        ArrayList<KBCfgBase> cfgList = new ArrayList<>(2);
        cfgList.add(commonPara);
        cfgList.add(iBeaconPara);
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
    }

    //example: reset all parameters to default
    public void resetParameters() {
        if (!mBeacon.isConnected()) {
            return;
        }

        mDownloadButton.setEnabled(false);
        HashMap<String, Object> cmdPara = new HashMap<>(5);
        cmdPara.put("msg", "reset");
        mRingButton.setEnabled(false);
        mBeacon.sendCommand(cmdPara, new KBeacon.ActionCallback() {
            @Override
            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                mRingButton.setEnabled(true);
                if (bConfigSuccess)
                {
                    //disconnect with device to make sure the new parameters take effect
                    mBeacon.disconnect();
                    toastShow("send reset command to beacon success");
                }
                else
                {
                    toastShow("send reset command to beacon error:" + error.errorCode);
                }
            }
        });
    }

    public void updateDeviceToView()
    {
        KBCfgCommon commonCfg = (KBCfgCommon) mBeacon.getConfigruationByType(KBCfgType.KBConfigTypeCommon);
        if (commonCfg != null) {
            mBeaconType.setText(commonCfg.getAdvTypeString());
            mBeaconModel.setText(commonCfg.getModel());
            mEditBeaconAdvPeriod.setText(String.valueOf(commonCfg.getAdvPeriod()));
            mEditBeaconTxPower.setText(String.valueOf(commonCfg.getTxPower()));
            mEditBeaconName.setText(String.valueOf(commonCfg.getName()));

            KBCfgIBeacon iBeaconCfg = (KBCfgIBeacon)mBeacon.getConfigruationByType(KBCfgType.KBConfigTypeIBeacon);
            if (iBeaconCfg != null) {
                mEditBeaconUUID.setText(iBeaconCfg.getUuid());
                mEditBeaconMajor.setText(String.valueOf(iBeaconCfg.getMajorID()));
                mEditBeaconMinor.setText(String.valueOf(iBeaconCfg.getMinorID()));
            }
        }
    }


    private int nDeviceConnState = KBeacon.KBStateDisconnected;

    public void onConnStateChange(KBeacon beacon, int state, int nReason)
    {
        if (state == KBeacon.KBStateConnected)
        {
            Log.v(LOG_TAG, "device has connected");
            invalidateOptionsMenu();

            mDownloadButton.setEnabled(true);

            updateDeviceToView();

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

            mDownloadButton.setEnabled(false);
            Log.e(LOG_TAG, "device has disconnected:" +  nReason);
            invalidateOptionsMenu();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mBeacon.getState() == KBeacon.KBStateConnected
            || mBeacon.getState() == KBeacon.KBStateConnecting){
            mBeacon.disconnect();
            invalidateOptionsMenu();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.menu_connect){
            mBeacon.connect(mPref.getPassword(mDeviceAddress), 20*1000, this);
            invalidateOptionsMenu();
        }
        else if(id == R.id.menu_disconnect){
            mBeacon.disconnect();
            invalidateOptionsMenu();
        }

        return super.onOptionsItemSelected(item);
    }
}
