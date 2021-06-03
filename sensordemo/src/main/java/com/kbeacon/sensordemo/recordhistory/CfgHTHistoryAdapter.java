
package com.kbeacon.sensordemo.recordhistory;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.kbeacon.sensordemo.R;
import com.kkmcn.kbeaconlib.KBSensorHistoryData.KBHumidityRecord;

import java.text.SimpleDateFormat;

public class CfgHTHistoryAdapter extends BaseAdapter {
    public LayoutInflater inflater;
    private Context mCtx;
    HTSensorDataInterface mSensorData;
    private static final SimpleDateFormat mRecordTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public CfgHTHistoryAdapter(Context ctx) {
        mCtx = ctx;
    }

    public CfgHTHistoryAdapter(Context context, HTSensorDataInterface proxData) {
        this.mSensorData = proxData;
        mCtx = context;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return mSensorData.size();
    }

    @Override
    public Object getItem(int position) {
        return mSensorData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public void updateView()
    {
        this.notifyDataSetChanged();//强制动态刷新数据进而调用getView方法
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = null;
        ViewHolder holder = null;
        if(convertView == null)
        {
            view = inflater.inflate(R.layout.ele_nearby_ht_record, null);
            holder = new ViewHolder();
            holder.textViewUTCTime = (TextView)view.findViewById(R.id.nbUtcRecordTime);
            holder.textViewTemperature = (TextView)view.findViewById(R.id.txtSensorTemperature);
            holder.textViewHumidity = (TextView)view.findViewById(R.id.txtSensorHumidity);
            view.setTag(holder);//为了复用holder
        }else
        {
            view = convertView;
            holder = (ViewHolder) view.getTag();
        }
        KBHumidityRecord nbRecord = mSensorData.get(position);
        String strNearbyUtcTime = mRecordTimeFormat.format(nbRecord.mUtcTime * 1000);
        holder.textViewUTCTime.setText(strNearbyUtcTime);

        String strTemperature =  mCtx.getString(R.string.BEACON_TEMP) + " " + nbRecord.mTemperature + mCtx.getString(R.string.BEACON_TEMP_UINT);
        holder.textViewTemperature.setText(strTemperature);

        String strHumidity = mCtx.getString(R.string.BEACON_HUM) + " " + nbRecord.mHumidity + "%";
        holder.textViewHumidity.setText(strHumidity);
        return view;
    }
    static class ViewHolder
    {
        TextView textViewUTCTime;
        TextView textViewTemperature;
        TextView textViewHumidity;
    }
}

