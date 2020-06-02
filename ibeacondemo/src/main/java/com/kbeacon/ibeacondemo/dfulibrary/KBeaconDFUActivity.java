package com.kbeacon.ibeacondemo.dfulibrary;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.kbeacon.ibeacondemo.AppBaseActivity;
import com.kbeacon.ibeacondemo.R;
import com.kbeacon.kbeaconlib.KBCfgPackage.KBCfgBase;
import com.kbeacon.kbeaconlib.KBCfgPackage.KBCfgCommon;
import com.kbeacon.kbeaconlib.KBCfgPackage.KBCfgType;
import com.kbeacon.kbeaconlib.KBException;
import com.kbeacon.kbeaconlib.KBUtility;
import com.kbeacon.kbeaconlib.KBeacon;
import com.kbeacon.kbeaconlib.KBeaconsMgr;

import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import no.nordicsemi.android.dfu.DfuProgressListener;
import no.nordicsemi.android.dfu.DfuProgressListenerAdapter;
import no.nordicsemi.android.dfu.DfuServiceController;
import no.nordicsemi.android.dfu.DfuServiceInitiator;
import no.nordicsemi.android.dfu.DfuServiceListenerHelper;

public class KBeaconDFUActivity extends AppBaseActivity implements KBeacon.ConnStateDelegate {

    private static String LOG_TAG = "CfgNBDFU";
    public static String DEVICE_MAC_ADDRESS = "DEVICE_MAC_ADDRESS";
    private boolean mInDfuState = false;
    private KBFirmwareDownload firmwareDownload;
    private DfuServiceController controller;
    private TextView mUpdateStatus;
    private ProgressBar mProgressBar;
    private KBeacon.ConnStateDelegate mPrivousDelegation;
    private DfuServiceInitiator starter;
    private ProgressDialog mProgressDialog;
    private KBeacon mBeacon;

    private String mFirmwareFileName;
    private String mFirmwareFileVersion;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cfg_beacon_dfu);

        final Intent intent = getIntent();
        String mMacAddress = intent.getStringExtra(DEVICE_MAC_ADDRESS);
        if (mMacAddress == null) {
            finish();
            return;
        }
        KBeaconsMgr mBluetoothMgr = KBeaconsMgr.sharedBeaconManager(this);
        mBeacon = mBluetoothMgr.getBeacon(mMacAddress);

        mUpdateStatus = (TextView) findViewById(R.id.textStatusDescription);
        mProgressBar = (ProgressBar)findViewById(R.id.progressBar);

        mInDfuState = false;
        firmwareDownload = new KBFirmwareDownload(this);


        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setTitle(getString(R.string.DEVICE_CHECK_UPDATE));
        mProgressDialog.setIndeterminate(false);//设置进度条是否为不明确
        mProgressDialog.setCancelable(false);//设置进度条是否可以按退回键取消
        mProgressDialog.show();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            DfuServiceInitiator.createDfuNotificationChannel(KBeaconDFUActivity.this);
        }

        this.downloadFirmwareInfo();
    }

    private final DfuProgressListener dfuProgressListener = new DfuProgressListenerAdapter() {
        @Override
        public void onDeviceConnecting(final String deviceAddress) {
            mUpdateStatus.setText(R.string.dfu_status_connecting);
        }

        @Override
        public void onDfuProcessStarting(final String deviceAddress) {
            mUpdateStatus.setText(R.string.dfu_status_starting);
        }

        public void onDeviceConnected(@NonNull final String deviceAddress) {
            // empty default implementation
            mUpdateStatus.setText(R.string.UPDATE_CONNECTED);
        }

        public void onDeviceDisconnecting(@NonNull final String deviceAddress) {
            // empty default implementation
            mUpdateStatus.setText(R.string.dfu_status_disconnecting);
        }

        public void onDeviceDisconned(@NonNull final String deviceAddress) {
            // empty default implementation
            mUpdateStatus.setText(R.string.UPDATE_DISCONNECTED);
        }

        @Override
        public void onProgressChanged(@NonNull final String deviceAddress, final int percent,
                                      final float speed, final float avgSpeed,
                                      final int currentPart, final int partsTotal) {
            mProgressBar.setProgress(percent);
            mUpdateStatus.setText(R.string.UPDATE_UPLOADING);
        }

        @Override
        public void onDfuCompleted(@NonNull final String deviceAddress) {
            // empty default implementation
            mUpdateStatus.setText(R.string.UPDATE_COMPLETE);
            mInDfuState = false;
            if (mProgressDialog.isShowing()){
                mProgressDialog.dismiss();
            }
            dfuComplete(getString(R.string.UPDATE_COMPLETE));
        }

        @Override
        public void onDfuAborted(@NonNull final String deviceAddress) {
            // empty default implementation
            mUpdateStatus.setText(R.string.UPDATE_ABORTED);
            mInDfuState = false;
            if (mProgressDialog.isShowing()){
                mProgressDialog.dismiss();
            }
            dfuComplete(getString(R.string.UPDATE_ABORTED));
        }

        @Override
        public void onError(@NonNull final String deviceAddress,
                            final int error, final int errorType, final String message) {
            // empty default implementation
            mUpdateStatus.setText(R.string.UPDATE_ABORTED);
            mInDfuState = false;
            if (mProgressDialog.isShowing()){
                mProgressDialog.dismiss();
            }
            dfuComplete(message);
        }
    };

    private void dfuComplete(String strDesc)
    {
        if (mProgressDialog.isShowing()){
            mProgressDialog.dismiss();
        }

        new AlertDialog.Builder(KBeaconDFUActivity.this)
                .setTitle(R.string.DEVICE_DFU_TITLE)
                .setMessage(strDesc)
                .setPositiveButton(R.string.Dialog_OK, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        KBeaconDFUActivity.this.finish();
                    }
                })
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        DfuServiceListenerHelper.registerProgressListener(this, dfuProgressListener);
    }

    @Override
    protected void onPause() {
        super.onPause();

        DfuServiceListenerHelper.unregisterProgressListener(this, dfuProgressListener);
    }

    public void onConnStateChange(KBeacon beacon, int state, int nReason)
    {
        if (state == KBeacon.KBStateDisconnected)
        {
            if (mInDfuState)
            {
                Log.v(LOG_TAG, "Disconnection for DFU");
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    private void updateFirmware() {
        firmwareDownload.downLoadFile(mFirmwareFileName,
                50 * 1000,
            new KBFirmwareDownload.DownloadFirmwareDataCallback() {
                @Override
                public void onDownloadComplete(boolean bSuccess, File file, KBException error) {
                    if (bSuccess) {
                        mInDfuState = true;

                        starter = new DfuServiceInitiator(KBeaconDFUActivity.this.mBeacon.getMac())
                                .setDeviceName(KBeaconDFUActivity.this.mBeacon.getName())
                                .setKeepBond(false);
                       
                        starter.setPrepareDataObjectDelay(300L);
                        starter.setZip(null, file.getPath());

                        controller = starter.start(KBeaconDFUActivity.this, DFUService.class);
                    } else {
                        if (mProgressDialog.isShowing()){
                            mProgressDialog.dismiss();
                        }

                        mUpdateStatus.setText(R.string.UPDATE_NETWORK_FAIL);
                        dfuComplete(getString(R.string.UPDATE_NETWORK_FAIL));
                    }
                }
            });
    }

    private void makeSureUpdateSelection() {
        String strDesc = String.format(getString(R.string.DFU_FOUND_NEW_VERSION), mFirmwareFileVersion);

        new AlertDialog.Builder(KBeaconDFUActivity.this)
                .setTitle(R.string.DEVICE_DFU_TITLE)
                .setMessage(strDesc)
                .setPositiveButton(R.string.Dialog_OK, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mProgressBar.setProgress(0);
                        mInDfuState = true;

                        //update
                        mPrivousDelegation = mBeacon.getConnStateDelegate();
                        mBeacon.setConnStateDelegate(KBeaconDFUActivity.this);
                        updateFirmware();

                        mProgressDialog.setTitle(getString(R.string.UPDATE_STARTED));
                        mProgressDialog.show();
                    }
                })
                .setNegativeButton(R.string.Dialog_Cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        KBeaconDFUActivity.this.finish();
                    }
                })
                .show();
    }


    private void downloadFirmwareInfo() {
        mProgressDialog.show();

        final KBCfgCommon cfgCommon = (KBCfgCommon) mBeacon.getConfigruationByType(KBCfgType.KBConfigTypeCommon);
        firmwareDownload.downloadFirmwareInfo(cfgCommon.getModel(), 10* 1000, new KBFirmwareDownload.DownloadFirmwareInfoCallback() {
            @Override
            public void onDownloadComplete(boolean bSuccess, HashMap<String, Object> firmwareInfo, KBException error) {
                if (mProgressDialog.isShowing()){
                    mProgressDialog.hide();
                }

                if (bSuccess) {
                    if (firmwareInfo == null)
                    {
                        dfuComplete(getString(R.string.NB_network_cloud_server_error));
                        return;
                    }

                    JSONObject object = (JSONObject)firmwareInfo.get(mBeacon.hardwareVersion());
                    if (object == null){
                        dfuComplete(getString(R.string.NB_network_file_not_exist));
                        return;
                    }
                    HashMap<String, Object> verInfo = new HashMap<>(10);
                    KBCfgBase.JsonObject2HashMap(object, verInfo);
                    mFirmwareFileName = (String)verInfo.get("appFileName");
                    mFirmwareFileVersion = (String)verInfo.get("appVersion");
                    if (mFirmwareFileName == null || mFirmwareFileVersion == null)
                    {
                        dfuComplete(getString(R.string.NB_network_cloud_server_error));
                        return;
                    }

                    String currVerDigital = cfgCommon.getVersion().substring(1);
                    String remoteVerDigital = mFirmwareFileVersion.substring(1);

                    //check version
                    if (Float.valueOf(currVerDigital) < Float.valueOf(remoteVerDigital)) {
                        //disconnect for DFU
                        makeSureUpdateSelection();
                    } else {
                        dfuComplete(getString(R.string.DEVICE_LATEST_VERSION));
                    }
                }
                else{
                    if (error.errorCode == KBFirmwareDownload.ERR_NETWORK_DOWN_FILE_ERROR)
                    {
                        dfuComplete(getString(R.string.UPDATE_NETWORK_FAIL) + error.getMessage());
                    }
                    else
                    {
                        dfuComplete(error.getMessage());
                    }
                }
            }
        });
    }
}
