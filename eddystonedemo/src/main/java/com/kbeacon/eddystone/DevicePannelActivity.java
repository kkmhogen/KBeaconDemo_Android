package com.kbeacon.eddystone;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kkmcn.kbeaconlib.KBAdvPackage.KBAdvType;
import com.kkmcn.kbeaconlib.KBCfgPackage.KBCfgBase;
import com.kkmcn.kbeaconlib.KBCfgPackage.KBCfgCommon;
import com.kkmcn.kbeaconlib.KBCfgPackage.KBCfgEddyUID;
import com.kkmcn.kbeaconlib.KBCfgPackage.KBCfgEddyURL;
import com.kkmcn.kbeaconlib.KBCfgPackage.KBCfgType;
import com.kkmcn.kbeaconlib.KBConnectionEvent;
import com.kkmcn.kbeaconlib.KBException;
import com.kkmcn.kbeaconlib.KBUtility;
import com.kkmcn.kbeaconlib.KBeacon;
import com.kkmcn.kbeaconlib.KBeaconsMgr;

import java.util.ArrayList;
import java.util.HashMap;

public class DevicePannelActivity extends AppBaseActivity implements View.OnClickListener{

    public final static String DEVICE_MAC_ADDRESS = "DEVICE_MAC_ADDRESS";
    private final static String LOG_TAG = "DevicePannel";

    public final static String DEFAULT_PASSWORD = "0000000000000000";   //16 zero ascii

    private KBeaconsMgr mBeaconMgr;
    private String mDeviceAddress;
    private KBeacon mBeacon;

    //uiview
    private CheckBox mCheckBoxURL, mCheckboxUID, mCheckboxTLM;
    private TextView mBeaconModel;
    private TextView mBeaconVersion;
    private EditText mEditEddyURL;
    private EditText mEditEddyNID;
    private EditText mEditEddySID;
    private EditText mEditBeaconAdvPeriod;
    private EditText mEditBeaconTxPower;
    private EditText mEditBeaconName;
    private Button mDownloadButton;
    private Button mCommandButton;
    private LinearLayout mUrlLayout, mUidLayout, mSettingViewLayout;

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

        setContentView(R.layout.device_pannel);
        mSettingViewLayout = (LinearLayout)findViewById(R.id.beaconConnSetting) ;
        mSettingViewLayout.setVisibility(View.GONE);

        mCheckBoxURL = (CheckBox) findViewById(R.id.checkBoxUrl);

        mCheckBoxURL.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b){
                    mUrlLayout.setVisibility(View.VISIBLE);
                }else{
                    mUrlLayout.setVisibility(View.GONE);
                }
            }
        });

        mCheckboxTLM = (CheckBox) findViewById(R.id.checkBoxTLM);
        mCheckboxUID = (CheckBox) findViewById(R.id.checkBoxUID);
        mCheckboxUID.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b){
                    mUidLayout.setVisibility(View.VISIBLE);
                }else{
                    mUidLayout.setVisibility(View.GONE);
                }
            }
        });

        mBeaconVersion = (TextView) findViewById(R.id.beaconVersion);
        mBeaconModel = (TextView) findViewById(R.id.beaconModle);

        mUrlLayout = (LinearLayout)findViewById(R.id.eddy_url_layout);
        mUrlLayout.setVisibility(View.GONE);
        mEditEddyURL = (EditText)findViewById(R.id.editEddyURL);
        mEditEddyURL.addTextChangedListener(new TextChangeWatcher(mEditEddyURL));


        mUidLayout = (LinearLayout)findViewById(R.id.eddy_uid_layout);
        mUidLayout.setVisibility(View.GONE);
        mEditEddyNID = (EditText)findViewById(R.id.editEddyNid);
        mEditEddyNID.addTextChangedListener(new TextChangeWatcher(mEditEddyNID));
        mEditEddySID = (EditText)findViewById(R.id.editEddySid);
        mEditEddySID.addTextChangedListener(new TextChangeWatcher(mEditEddySID));

        mEditBeaconAdvPeriod = (EditText)findViewById(R.id.editBeaconAdvPeriod);
        mEditBeaconAdvPeriod.addTextChangedListener(new TextChangeWatcher(mEditBeaconAdvPeriod));

        mEditBeaconTxPower = (EditText)findViewById(R.id.editBeaconTxPower);
        mEditBeaconTxPower.addTextChangedListener(new TextChangeWatcher(mEditBeaconTxPower));

        mEditBeaconName = (EditText)findViewById(R.id.editBeaconname);
        mEditBeaconName.addTextChangedListener(new TextChangeWatcher(mEditBeaconName));

        mDownloadButton = (Button) findViewById(R.id.buttonSaveData);
        mDownloadButton.setEnabled(false);
        mDownloadButton.setOnClickListener(this);

        mCommandButton = (Button)findViewById(R.id.buttonCommand);
        mCommandButton.setOnClickListener(this);
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
        }
        else if (mBeacon.getState() == KBeacon.KBStateConnecting)
        {
            menu.findItem(R.id.menu_connect).setEnabled(false);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
            menu.findItem(R.id.menu_connecting).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        else
        {
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
            case R.id.buttonSaveData: {
                updateViewToDevice();
                break;
            }

            case R.id.buttonCommand:{
                ringDevice();

                break;
            }
        }
    }

    public class TextChangeWatcher implements TextWatcher
    {
        private View mEditText;

        TextChangeWatcher(View parent)
        {
            mEditText = parent;
            mEditText.setTag(0);
        }

        public void beforeTextChanged(CharSequence var1, int var2, int var3, int var4)
        {

        }

        public void onTextChanged(CharSequence var1, int var2, int var3, int var4)
        {
            mEditText.setTag(1);
        }

        public void afterTextChanged(Editable var1)
        {

        }

    }

    public void simpleUpdateDeviceTest() {
        if (!mBeacon.isConnected()) {
            return;
        }

        //get current paramaters
        KBCfgCommon newCommomCfg = new KBCfgCommon();
        try {
            newCommomCfg.setAdvPeriod(1000f);
            newCommomCfg.setTxPower(-4);
            newCommomCfg.setName("KBeaconDemo");
        } catch (KBException excpt) {
            toastShow("input data invalid");
            excpt.printStackTrace();
        }

        ArrayList<KBCfgBase> cfgList = new ArrayList<>(1);
        cfgList.add(newCommomCfg);
        mDownloadButton.setEnabled(false);
        mBeacon.modifyConfig(cfgList, new KBeacon.ActionCallback() {
            @Override
            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                mDownloadButton.setEnabled(true);
                if (bConfigSuccess)
                {
                    clearChangeTag();
                    toastShow("config data to beacon success");
                }
                else
                {
                    toastShow("config failed for error:" + error.errorCode);
                }
            }
        });
    }

    //read user input and download to KBeacon device
    void updateViewToDevice()
    {
        if (!mBeacon.isConnected())
        {
            return;
        }

        KBCfgCommon oldCommonCfg = (KBCfgCommon)mBeacon.getConfigruationByType(KBCfgType.KBConfigTypeCommon);
        KBCfgCommon newCommomCfg = new KBCfgCommon();
        KBCfgEddyURL newUrlCfg = new KBCfgEddyURL();
        KBCfgEddyUID newUidCfg = new KBCfgEddyUID();
        try {
            //check if user update advertisement type
            int nAdvType = 0;
            if (mCheckBoxURL.isChecked()){
                nAdvType |= KBAdvType.KBAdvTypeEddyURL;
            }
            if (mCheckboxUID.isChecked()){
                nAdvType |= KBAdvType.KBAdvTypeEddyUID;
            }
            if (mCheckboxTLM.isChecked()){
                nAdvType |= KBAdvType.KBAdvTypeEddyTLM;
            }
            //check if the parameters changed
            if (oldCommonCfg.getAdvType() != nAdvType)
            {
                newCommomCfg.setAdvType(nAdvType);
            }

            //adv period, check if user change adv period
            Integer changeTag = (Integer)mEditBeaconAdvPeriod.getTag();
            if (changeTag > 0)
            {
                String strAdvPeriod = mEditBeaconAdvPeriod.getText().toString();
                if (Utils.isPositiveInteger(strAdvPeriod)) {
                    Float newAdvPeriod = Float.valueOf(strAdvPeriod);
                    newCommomCfg.setAdvPeriod(newAdvPeriod);
                }
            }

            //tx power ,
            changeTag = (Integer)mEditBeaconTxPower.getTag();
            if (changeTag > 0)
            {
                String strTxPower = mEditBeaconTxPower.getText().toString();
                Integer newTxPower = Integer.valueOf(strTxPower);
                if (newTxPower > oldCommonCfg.getMaxTxPower() || newTxPower < oldCommonCfg.getMinTxPower()) {
                    toastShow("tx power not valid");
                    return;
                }
                newCommomCfg.setTxPower(newTxPower);
            }

            //device name
            String strDeviceName = mEditBeaconName.getText().toString();
            if (!strDeviceName.equals(oldCommonCfg.getName()) && strDeviceName.length() < KBCfgCommon.MAX_NAME_LENGTH) {
                newCommomCfg.setName(strDeviceName);
            }

            //uid config
            if (mCheckboxUID.isChecked())
            {
                KBCfgEddyUID oldUidCfg = (KBCfgEddyUID)mBeacon.getConfigruationByType(KBCfgType.KBConfigTypeEddyUID);
                String strNewNID = mEditEddyNID.getText().toString();
                String strNewSID = mEditEddySID.getText().toString();
                if (!strNewNID.equals(oldUidCfg.getNid()) && KBUtility.isHexString(strNewNID)){
                    newUidCfg.setNid(strNewNID);
                }

                if (!strNewSID.equals(oldUidCfg.getSid()) && KBUtility.isHexString(strNewSID)){
                    newUidCfg.setSid(strNewSID);
                }
            }

            //url config
            if (mCheckBoxURL.isChecked())
            {
                KBCfgEddyURL oldUrlCfg = (KBCfgEddyURL)mBeacon.getConfigruationByType(KBCfgType.KBConfigTypeEddyURL);
                String strUrl = mEditEddyURL.getText().toString();
                if (!strUrl.equals(oldUrlCfg.getUrl())){
                    newUrlCfg.setUrl(strUrl);
                }
            }

            //TLM advertisement interval configuration (optional)
            if (mCheckboxTLM.isChecked()){
                //The default TLM advertisement interval is 10. The KBeacon will send 1 TLM advertisement packet every 10 advertisement packets.
                //newCommomCfg.setTLMAdvInterval(8);
            }
        }catch (KBException excpt)
        {
            toastShow("config data is invalid:" + excpt.errorCode);
            excpt.printStackTrace();
        }

        ArrayList<KBCfgBase> cfgList = new ArrayList<>(3);
        cfgList.add(newCommomCfg);
        cfgList.add(newUidCfg);
        cfgList.add(newUrlCfg);
        mDownloadButton.setEnabled(false);
        mBeacon.modifyConfig(cfgList, new KBeacon.ActionCallback() {
            @Override
            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                mDownloadButton.setEnabled(true);
                if (bConfigSuccess)
                {
                    clearChangeTag();
                    toastShow("config data to beacon success");
                }
                else
                {
                    if (error.errorCode == KBException.KBEvtCfgNoParameters)
                    {
                        toastShow("No data need to be config");
                    }
                    else
                    {
                        toastShow("config failed for error:" + error.errorCode);
                    }
                }
            }
        });
    }

    //example update beacon to URL
    public void updateKBeaconToEddyUrl() {
        if (!mBeacon.isConnected()) {
            return;
        }

        //change parameters
        KBCfgCommon commonPara = new KBCfgCommon();
        KBCfgEddyURL urlPara = new KBCfgEddyURL();
        try {
            //set adv type to iBeacon
            commonPara.setAdvType(KBAdvType.KBAdvTypeEddyURL);

            //url para
            urlPara.setUrl("https://www.google.com/");
        } catch (KBException excpt) {
            toastShow("input data invalid");
            excpt.printStackTrace();
        }

        ArrayList<KBCfgBase> cfgList = new ArrayList<>(2);
        cfgList.add(commonPara);
        cfgList.add(urlPara);
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

    //update beacon to UID type
    public void updateKBeaconToEddyUID() {
        if (!mBeacon.isConnected()) {
            return;
        }

        //change parameters
        KBCfgCommon commonPara = new KBCfgCommon();
        KBCfgEddyUID uidPara = new KBCfgEddyUID();
        try {
            //set adv type to iBeacon
            commonPara.setAdvType(KBAdvType.KBAdvTypeEddyUID);

            //set uid para
            uidPara.setNid("0x00010203040506070809");
            uidPara.setSid("0x010203040506");
        } catch (KBException excpt) {
            toastShow("input data invalid");
            excpt.printStackTrace();
        }

        ArrayList<KBCfgBase> cfgList = new ArrayList<>(2);
        cfgList.add(commonPara);
        cfgList.add(uidPara);
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

    public void stopRingDevice() {
        if (!mBeacon.isConnected()) {
            return;
        }

        KBCfgCommon cfgCommon = (KBCfgCommon)mBeacon.getConfigruationByType(KBCfgType.KBConfigTypeCommon);
        if (!cfgCommon.isSupportBeep())
        {
            Log.e(LOG_TAG, "device does not support ring feature");
            return;
        }

        mDownloadButton.setEnabled(false);
        HashMap<String, Object> cmdPara = new HashMap<>(5);
        cmdPara.put("msg", "ring");
        cmdPara.put("ringTime", 20000);   //ring times, uint is ms
        cmdPara.put("ringType", 10);  //0x0:led flash only; 0x1:beep alert only; 0x2 led flash and beep alert;

        mBeacon.sendCommand(cmdPara, new KBeacon.ActionCallback() {
            @Override
            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                mDownloadButton.setEnabled(true);
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

    public void ringDevice() {
        if (!mBeacon.isConnected()) {
            return;
        }

        KBCfgCommon cfgCommon = (KBCfgCommon)mBeacon.getConfigruationByType(KBCfgType.KBConfigTypeCommon);
        if (!cfgCommon.isSupportBeep())
        {
            Log.e(LOG_TAG, "device does not support ring feature");
            return;
        }

        mDownloadButton.setEnabled(false);
        HashMap<String, Object> cmdPara = new HashMap<>(5);
        cmdPara.put("msg", "ring");
        cmdPara.put("ringTime", 20000);   //ring times, uint is ms
        cmdPara.put("ringType", 2);  //0x0:led flash only; 0x1:beep alert only; 0x2 led flash and beep alert;
        cmdPara.put("ledOn", 200);   //valid when ringType set to 0x0 or 0x2
        cmdPara.put("ledOff", 1800); //valid when ringType set to 0x0 or 0x2
        mBeacon.sendCommand(cmdPara, new KBeacon.ActionCallback() {
            @Override
            public void onActionComplete(boolean bConfigSuccess, KBException error) {
                mDownloadButton.setEnabled(true);
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

    //update device's configuration  to UI
    public void updateDeviceToView()
    {
        boolean isTLMEnable, isUIDEnable, isUrlEnable;
        KBCfgCommon commonCfg = (KBCfgCommon) mBeacon.getConfigruationByType(KBCfgType.KBConfigTypeCommon);
        if (commonCfg != null) {

            //print basic capibility
            Log.v(LOG_TAG, "support iBeacon:" + commonCfg.isSupportIBeacon());
            Log.v(LOG_TAG, "support eddy url:" + commonCfg.isSupportEddyURL());
            Log.v(LOG_TAG, "support eddy tlm:" + commonCfg.isSupportEddyTLM());
            Log.v(LOG_TAG, "support eddy uid:" + commonCfg.isSupportEddyUID());
            Log.v(LOG_TAG, "support ksensor:" + commonCfg.isSupportKBSensor());
            Log.v(LOG_TAG, "beacon has button:" + commonCfg.isSupportButton());
            Log.v(LOG_TAG, "beacon can beep:" + commonCfg.isSupportBeep());
            Log.v(LOG_TAG, "support accleration sensor:" + commonCfg.isSupportAccSensor());
            Log.v(LOG_TAG, "support humidify sensor:" + commonCfg.isSupportHumiditySensor());
            Log.v(LOG_TAG, "support max tx power:" + commonCfg.getMaxTxPower());
            Log.v(LOG_TAG, "support min tx power:" + commonCfg.getMinTxPower());

            //get support trigger
            Log.v(LOG_TAG, "support trigger" + commonCfg.getTrigCapability());

            //device model
            mBeaconModel.setText(commonCfg.getModel());

            //device version
            mBeaconVersion.setText(commonCfg.getVersion());

            //current advertisment type
            mEditBeaconName.setText(commonCfg.getAdvTypeString());

            //advertisment period
            mEditBeaconAdvPeriod.setText(String.valueOf(commonCfg.getAdvPeriod()));

            //beacon tx power
            mEditBeaconTxPower.setText(String.valueOf(commonCfg.getTxPower()));

            //beacon name
            mEditBeaconName.setText(String.valueOf(commonCfg.getName()));

            //check if Eddy TLM advertisement enable
            isTLMEnable = ((commonCfg.getAdvType() & KBAdvType.KBAdvTypeEddyTLM) > 0);
            mCheckboxTLM.setChecked(isTLMEnable);

            //check if Eddy UID advertisement enable
            isUIDEnable= ((commonCfg.getAdvType() & KBAdvType.KBAdvTypeEddyUID) > 0);
            mCheckboxUID.setChecked(isUIDEnable);
            mUidLayout.setVisibility(isUIDEnable? View.VISIBLE: View.GONE);

            //check if Eddy URL advertisement enable
            isUrlEnable= ((commonCfg.getAdvType() & KBAdvType.KBAdvTypeEddyURL) > 0);
            mCheckBoxURL.setChecked(isUrlEnable);
            mUrlLayout.setVisibility(isUrlEnable? View.VISIBLE: View.GONE);

            //check if iBeacon advertisment enable
            Log.v(LOG_TAG, "iBeacon advertisment enable:" + ((commonCfg.getAdvType() & KBAdvType.KBAdvTypeIBeacon) > 0));

            //check if KSensor advertisment enable
            Log.v(LOG_TAG, "iBeacon advertisment enable:" + ((commonCfg.getAdvType() & KBAdvType.KBAdvTypeSensor) > 0));

            //check TLM adv interval
            Log.v(LOG_TAG, "TLM adv interval:" + commonCfg.getTLMAdvInterval());
        }

        //get eddystone URL paramaters
        KBCfgEddyURL beaconUrlCfg = (KBCfgEddyURL) mBeacon.getConfigruationByType(KBCfgType.KBConfigTypeEddyURL);
        if (beaconUrlCfg != null) {
            mEditEddyURL.setText(beaconUrlCfg.getUrl());
        }

        //get eddystone UID information
        KBCfgEddyUID beaconUIDCfg = (KBCfgEddyUID) mBeacon.getConfigruationByType(KBCfgType.KBConfigTypeEddyUID);
        if (beaconUIDCfg != null) {
            mEditEddyNID.setText(beaconUIDCfg.getNid());
            mEditEddySID.setText(beaconUIDCfg.getSid());
        }

        clearChangeTag();
    }

    private void clearChangeTag()
    {
        mEditEddySID.setTag(0);
        mEditEddyNID.setTag(0);
        mEditEddyURL.setTag(0);
        mEditBeaconName.setTag(0);
        mEditBeaconAdvPeriod.setTag(0);
        mEditBeaconTxPower.setTag(0);
    }


    private int nDeviceLastState = KBeacon.KBStateDisconnected;



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
            mBeacon.connect(DEFAULT_PASSWORD, 20*1000, connectionDelegate);
            invalidateOptionsMenu();
        }
        else if(id == R.id.menu_disconnect){
            mBeacon.disconnect();
            invalidateOptionsMenu();
        }

        return super.onOptionsItemSelected(item);
    }

    private KBeacon.ConnStateDelegate connectionDelegate = new KBeacon.ConnStateDelegate()
    {
        public void onConnStateChange(KBeacon var1, int state, int nReason)
        {
            if (state == KBeacon.KBStateConnected)
            {
                Log.v(LOG_TAG, "device has connected");
                invalidateOptionsMenu();

                mDownloadButton.setEnabled(true);
                mSettingViewLayout.setVisibility(View.VISIBLE);

                updateDeviceToView();

                nDeviceLastState = state;
            }
            else if (state == KBeacon.KBStateConnecting)
            {
                Log.v(LOG_TAG, "device start connecting");
                invalidateOptionsMenu();

                nDeviceLastState = state;
            }
            else if (state == KBeacon.KBStateDisconnecting)
            {
                Log.v(LOG_TAG, "device start disconnecting");
                invalidateOptionsMenu();

                nDeviceLastState = state;
            }
            else if (state == KBeacon.KBStateDisconnected)
            {
                if (nReason == KBConnectionEvent.ConnAuthFail) {
                    toastShow("password error");
                } else if (nReason == KBConnectionEvent.ConnTimeout) {
                    toastShow("connection timeout");
                } else {
                    toastShow("connection other error, reason:" + nReason);
                }

                nDeviceLastState = state;
                mDownloadButton.setEnabled(false);
                mSettingViewLayout.setVisibility(View.GONE);
                Log.e(LOG_TAG, "device has disconnected:" +  nReason);
                invalidateOptionsMenu();
            }
        }
    };
}
