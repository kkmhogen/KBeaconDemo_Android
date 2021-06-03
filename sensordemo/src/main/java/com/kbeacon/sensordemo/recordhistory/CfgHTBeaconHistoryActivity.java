package com.kbeacon.sensordemo.recordhistory;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;


import com.kbeacon.sensordemo.AppBaseActivity;
import com.kbeacon.sensordemo.R;
import com.kkmcn.kbeaconlib.KBCfgPackage.KBCfgIBeacon;
import com.kkmcn.kbeaconlib.KBCfgPackage.KBCfgType;
import com.kkmcn.kbeaconlib.KBException;
import com.kkmcn.kbeaconlib.KBSensorHistoryData.KBHumidityDataMsg;
import com.kkmcn.kbeaconlib.KBSensorHistoryData.KBHumidityRecord;
import com.kkmcn.kbeaconlib.KBSensorHistoryData.KBSensorDataMsgBase;
import com.kkmcn.kbeaconlib.KBeacon;
import com.kkmcn.kbeaconlib.KBeaconsMgr;
import com.kkmcn.kbeaconlib.UTCTime;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import static com.kkmcn.kbeaconlib.KBSensorHistoryData.KBSensorDataMsgBase.INVALID_DATA_RECORD_POS;

public class CfgHTBeaconHistoryActivity extends AppBaseActivity implements AbsListView.OnScrollListener {

    private String LOG_TAG = "CfgHTBeaconHistoryActivity.";
    public final static int MSG_LOAD_DATA_CMP = 301;
    public final static int MSG_LOAD_NO_MORE_DATA = 302;
    public final static int MSG_READ_DATA_TIMEOUT = 303;
    public final static String DEVICE_MAC_ADDRESS = "DEVICE_MAC_ADDRESS";
    public final static int MAX_READ_TIMEOUT_MS = 10000;


    public static final int MAX_READ_DATA_RCD = 30;

    public LayoutInflater mInflater;
    public ListView mListView;

    KBHumidityDataMsg mSensorDataMsg = new KBHumidityDataMsg();

    CfgHTRecordFileMgr mRecordFileMgr = new CfgHTRecordFileMgr();
    public boolean mIsLoading = false;//表示是否正处于加载状态
    private long mReadDataNextMsgID = 0;
    private boolean mbHasReadDataInfo = false;
    private long mUtcOffset = 0;
    public com.kbeacon.view.PullUpLoadListViewFooter mLoadMoreView;


    public CfgHTHistoryAdapter mRecordAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private KBeacon mBeacon;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        String mMacAddress = intent.getStringExtra(DEVICE_MAC_ADDRESS);
        if (mMacAddress == null) {
            finish();
            return;
        }
        KBeaconsMgr mBluetoothMgr = KBeaconsMgr.sharedBeaconManager(this);
        mBeacon = mBluetoothMgr.getBeacon(mMacAddress);

        setContentView(R.layout.activity_cfg_ht_record_list);

        mInflater = LayoutInflater.from(this);
        mListView = (ListView) findViewById(R.id.lstSensorList);

        //init record list
        mRecordFileMgr.initHistoryRecord(mBeacon.getMac(), this);

        //adapter
        mRecordAdapter = new CfgHTHistoryAdapter(this, mRecordFileMgr);
        mListView.setOnScrollListener(this);
        mListView.setAdapter(mRecordAdapter);
        mbHasReadDataInfo = false;

        //load more data view
        mLoadMoreView =  new com.kbeacon.view.PullUpLoadListViewFooter(this);
        mLoadMoreView.setVisibility(View.GONE);//设置刷新视图默认情况下是不可见的
        mListView.addFooterView(mLoadMoreView);

        swipeRefreshLayout = (SwipeRefreshLayout)findViewById(R.id.swipe_container);
        swipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_light, android.R.color.holo_red_light, android.R.color.holo_orange_light, android.R.color.holo_green_light);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                // TODO Auto-generated method stub
                new Handler().postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        swipeRefreshLayout.setRefreshing(true);

                        //remove privous loading
                        mHandler.removeMessages(MSG_READ_DATA_TIMEOUT);

                        if (!mbHasReadDataInfo)
                        {
                            startReadFirstPage();
                        }else{
                            startReadNextRecordPage();
                        }
                    }
                }, 500);
            }
        });

        if (!mbHasReadDataInfo) {
            swipeRefreshLayout.setRefreshing(true);
            startReadFirstPage();
        }
    }

    protected Handler mHandler = new MainActivityHandler();
    class MainActivityHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {

            if (msg.what == MSG_READ_DATA_TIMEOUT)
            {
                mLoadMoreView.setVisibility(View.GONE);
                toastShow(getString(R.string.LOAD_MOVING_DATA_FAILED));
                mIsLoading = false;
                swipeRefreshLayout.setRefreshing(false);
            }
            else if (msg.what == MSG_LOAD_DATA_CMP)
            {
                mLoadMoreView.setVisibility(View.GONE);
                mHandler.removeMessages(MSG_READ_DATA_TIMEOUT);
                mRecordAdapter.updateView();
                mIsLoading = false;
                swipeRefreshLayout.setRefreshing(false);

                toastShow(getString(R.string.load_data_complete, msg.arg1));
            }
            else if (msg.what == MSG_LOAD_NO_MORE_DATA)
            {
                mLoadMoreView.setVisibility(View.GONE);
                mHandler.removeMessages(MSG_READ_DATA_TIMEOUT);
                mRecordAdapter.updateView();
                mIsLoading = false;
                swipeRefreshLayout.setRefreshing(false);

                toastShow(getString(R.string.load_data_complete_no_more_data, msg.arg1));
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.history_menu, menu);
        return true;
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
    }

    private void startReadFirstPage() {
        //set status to loading
        mIsLoading = true;

        mSensorDataMsg.readSensorDataInfo(mBeacon,
                new KBSensorDataMsgBase.ReadSensorCallback()
                {
                    @Override
                    public void onReadComplete(boolean bConfigSuccess, Object obj, KBException error) {

                        if (!bConfigSuccess){
                            toastShow("read data failed");
                            mHandler.sendEmptyMessage(MSG_LOAD_DATA_CMP);
                            return;
                        }

                        KBHumidityDataMsg.ReadHTSensorInfoRsp infRsp = (KBHumidityDataMsg.ReadHTSensorInfoRsp) obj;
                        mUtcOffset = UTCTime.getUTCTimeSeconds() - infRsp.readInfoUtcSeconds;

                        Log.v(LOG_TAG, "Total records in device:" + infRsp.totalRecordNumber);

                        Log.v(LOG_TAG, "Un read records in device:" + infRsp.unreadRecordNumber);
                        mbHasReadDataInfo = true;

                        if (infRsp.unreadRecordNumber == 0)
                        {
                            mHandler.sendEmptyMessage(MSG_LOAD_NO_MORE_DATA);
                        }
                        else
                        {
                            startReadNextRecordPage();
                        }
                    }
                }
        );

        mHandler.sendEmptyMessageDelayed(MSG_READ_DATA_TIMEOUT, MAX_READ_TIMEOUT_MS);
    }


    private void startReadNextRecordPage()
    {
        //set status to loading
        mIsLoading = true;

        mSensorDataMsg.readSensorRecord(mBeacon,
                INVALID_DATA_RECORD_POS,
                KBSensorDataMsgBase.READ_RECORD_NEW_RECORD,
                30,
                new KBSensorDataMsgBase.ReadSensorCallback()
                {
                    @Override
                    public void onReadComplete(boolean bConfigSuccess,  Object obj, KBException error) {

                        if (!bConfigSuccess){
                            toastShow("read data failed");
                            mHandler.sendEmptyMessage(MSG_LOAD_DATA_CMP);
                            return;
                        }

                        KBHumidityDataMsg.ReadHTSensorDataRsp dataRsp = (KBHumidityDataMsg.ReadHTSensorDataRsp) obj;
                        for (KBHumidityRecord record: dataRsp.readDataRspList)
                        {
                            Log.v(LOG_TAG, "record utc time:" + record.mUtcTime);
                            Log.v(LOG_TAG, "record temperature:" + record.mTemperature);
                            Log.v(LOG_TAG, "record humidity:" + record.mHumidity);
                        }

                        if (dataRsp.readDataNextPos == INVALID_DATA_RECORD_POS)
                        {
                            Log.v(LOG_TAG, "Read data complete");
                        }

                        mRecordFileMgr.appendRecord(dataRsp.readDataRspList);
                        int nReadDataNum = dataRsp.readDataRspList.size();
                        mReadDataNextMsgID = dataRsp.readDataNextPos;
                        if (mReadDataNextMsgID == INVALID_DATA_RECORD_POS)
                        {
                            Message msg = mHandler.obtainMessage(MSG_LOAD_NO_MORE_DATA);
                            msg.arg1 = nReadDataNum;
                            mHandler.sendMessage(msg);
                        }else {
                            Log.v(LOG_TAG, "Wait read next data pos:" + mReadDataNextMsgID);
                            Message msg = mHandler.obtainMessage(MSG_LOAD_DATA_CMP);
                            msg.arg1 = nReadDataNum;
                            mHandler.sendMessage(msg);
                        }
                    }
                }
        );

        mHandler.sendEmptyMessageDelayed(MSG_READ_DATA_TIMEOUT, MAX_READ_TIMEOUT_MS);
    }


    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 是否触发按键为back键
        if (keyCode == KeyEvent.KEYCODE_BACK) {
        }
        return true;
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_clear:
            {
                clearAllHistory();
                break;
            }

            case R.id.menu_export:
            {
                String strHistoryContent = mRecordFileMgr.writeHistoryToString();
                if (strHistoryContent == null){
                    toastShow(getString(R.string.no_data_to_export));
                    return true;
                }
                Intent intent = new Intent(Intent.ACTION_SEND);
                KBCfgIBeacon cfgIbeacon = (KBCfgIBeacon)mBeacon.getConfigruationByType(KBCfgType.KBConfigTypeIBeacon);
                String strExportTitle = getString(R.string.EXPORT_HISTORY_DATA_EMAIL_TITLE, mBeacon.getMac());
                intent.putExtra(Intent.EXTRA_SUBJECT, strExportTitle);
                intent.setType("text/plain");
                intent.putExtra(android.content.Intent.EXTRA_TEXT, strHistoryContent);
                startActivity(Intent.createChooser(intent, "send to email"));
                break;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private void clearAllHistory()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.track_clear_history_desc);
        builder.setNegativeButton(R.string.Dialog_Cancel, null);
        builder.setPositiveButton(R.string.Dialog_OK, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mSensorDataMsg.clearSensorRecord(mBeacon,
                        new KBSensorDataMsgBase.ReadSensorCallback()
                        {
                            @Override
                            public void onReadComplete(boolean bConfigSuccess,  Object obj, KBException error) {
                                if (!bConfigSuccess){
                                    toastShow(getString(R.string.track_clear_fail));
                                }
                                else
                                {
                                    mRecordFileMgr.clearHistoryRecord();
                                    mRecordAdapter.notifyDataSetChanged();
                                    toastShow(getString(R.string.track_clear_success));
                                }
                            }
                        }
                );
            }
        });
        builder.show();
    }

}

