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

import com.kbeacon.kbeaconlib.KBAdvPackage.KBAdvType;
import com.kbeacon.kbeaconlib.KBCfgPackage.KBCfgBase;
import com.kbeacon.kbeaconlib.KBCfgPackage.KBCfgCommon;
import com.kbeacon.kbeaconlib.KBCfgPackage.KBCfgIBeacon;
import com.kbeacon.kbeaconlib.KBCfgPackage.KBCfgType;
import com.kbeacon.kbeaconlib.KBConnectionEvent;
import com.kbeacon.kbeaconlib.KBException;
import com.kbeacon.kbeaconlib.KBeacon;
import com.kbeacon.kbeaconlib.KBeaconsMgr;

import java.util.ArrayList;

public class DevicePannelActivity extends AppBaseActivity implements View.OnClickListener, KBeacon.ConnStateDelegate{

    public final static String DEVICE_MAC_ADDRESS = "DEVICE_MAC_ADDRESS";
    private final static String LOG_TAG = "DevicePannel";

    public final static String DEFAULT_PASSWORD = "0000000000000000";   //16 zero ascii

    private KBeaconsMgr mBeaconMgr;
    private String mDeviceAddress;
    private KBeacon mBeacon;

    //uiview
    private TextView mBeaconType;
    private TextView mBeaconModel;
    private EditText mEditBeaconUUID;
    private EditText mEditBeaconMajor;
    private EditText mEditBeaconMinor;
    private EditText mEditBeaconAdvPeriod;
    private EditText mEditBeaconPassword;
    private EditText mEditBeaconTxPower;
    private EditText mEditBeaconName;
    private Button mDownloadButton;
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
        }
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
                Integer newAdvPeriod = Integer.valueOf(strAdvPeriod);
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

            //device name
            String strDeviceName = mEditBeaconName.getText().toString();
            if (!strDeviceName.equals(oldCommonCfg.getName())) {
                newCommomCfg.setName(strDeviceName);
            }

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
                        if (error.errorCode == KBException.KBEvtCfgNoParamaters)
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
        }catch (Exception excpt)
        {
            toastShow("config data is invalid");
            excpt.printStackTrace();
        }
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
