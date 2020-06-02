#KBeacon Android SDK Instruction DOC（English）

----
## 1. Introduction
We provide AAR format SDK library on Github, you can found it in directory:   
./aar-sdk/kbeaconlib-release-xxx.aar

With this SDK, you can scan and configure the KBeacon device. The SDK include follow main class:
* KBeaconsMgr: Global definition, responsible for scanning KBeacon devices advertisment packet, and monitoring the Bluetooth status of the system;

* KBeacon: An instance of a KBeacon device, KBeaconsMgr creates an instance of KBeacon while it found a physical device. Each KBeacon instance has three properties: KBAdvPacketHandler, KBAuthHandler, KBCfgHandler.

* KBAdvPacketHandler: parsing advertisement packet. This attribute is valid during the scan phase.

*	KBAuthHandler: Responsible for the authentication operation with the KBeacon device after the connection is established.

*	KBCfgHandler：Responsible for configuring parameters related to KBeacon devices
![avatar](https://github.com/kkmhogen/KBeaconDemo_Android/blob/master/kbeacon_class_arc.png?raw=true)

**Scanning Stage**

in this stage, KBeaconsMgr will scan and parse the advertisement packet about KBeacon devices, and it will create "KBeacon" instance for every founded devices, developers can get all advertisements data by its allAdvPackets or getAdvPacketByType function.

**Connection Stage**

After a KBeacon connected, developer can make some changes of the device by modifyConfig.


## 2. Android demo
To make your development easier, we have two android demos in github. They are:  
* eddystonedemo: The app can scan KBeacon devices and configure Eddystone URL, TLM, UID related parameters. This SDK are introduced with reference to this demo.
* ibeacondemo: The app can scan KBeacon devices and configure iBeacon related parameters.


## 3. Import SDK to project
### 3.1 Prepare
Development environment:  
Android Studio  
minSdkVersion 21

### 3.2 Import SDK
1. Copy the development kit kbeaconlib-release-xx.aar file into the libs directory. As shown below:  
![avatar](https://github.com/kkmhogen/KBeaconDemo_Android/blob/master/bundlesnapshot.png?raw=true)

2. Add dependencies in build.gradle, Edit "build.gradle" file under the APP project  
a. Add follow line in build.gradle:  
```
repositories {
    flatDir {
        dirs 'libs'   // aar lib directory
    }
}
```
b. add kbeaconlib to dependencies
```
dependencies {
   …
    compile(name:'kbeaconlib-release-v1.0.1', ext:'aar')
}
```

3. Add the Bluetooth permissions and the corresponding component registration under the AndroidManifest.xml file. As follows:  
```
<uses-feature
    android:name="android.hardware.bluetooth_le" android:required="true" />
<uses-permission android:name="android.permission.BLUETOOTH"/>
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```  
For android 10, if you want the app scanning KBeacons in background, please add:  
```
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />

```


## 4. How to use SDK
### 4.1 Scanning device
1. Init KBeaconMgr instance in Activity, also your application should implementation the scanning callback.
```Java
@Override
public void onCreate(Bundle savedInstanceState) {
	//other code...
	//get KBeacon central manager instance
	mBeaconsMgr = KBeaconsMgr.sharedBeaconManager(this);
	if (mBeaconsMgr == null)
	{
	    toastShow("Make sure the phone supports BLE function");
	    return;
	}
	//other code...  
}  
```

2. Request permission:  
In Android-6.0 or later, Bluetooth scanning requires location permissions, so the app should request permission before start scanning like follows:
```Java
//for android6, the app need corse location permission for BLE scanning
if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        != PackageManager.PERMISSION_GRANTED) {
    ActivityCompat.requestPermissions(this,
            new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_COARSE_LOCATION);
}
//for android10, the app need fine location permission for BLE scanning
if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        != PackageManager.PERMISSION_GRANTED) {
    ActivityCompat.requestPermissions(this,
            new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_FINE_LOCATION);
}
```

3. Start scanning
```Java
mBeaconsMgr.delegate = beaconMgrDeletate;
int nStartScan = mBeaconsMgr.startScanning();
if (nStartScan == 0)
{
    Log.v(TAG, "start scan success");
}
else if (nStartScan == KBeaconsMgr.SCAN_ERROR_BLE_NOT_ENABLE) {
    toastShow("BLE function is not enable");
}
else if (nStartScan == KBeaconsMgr.SCAN_ERROR_NO_PERMISSION) {
    toastShow("BLE scanning has no location permission");
}
else
{
    toastShow("BLE scanning unknown error");
}
```

3. Before start scanning, the app should implementation KBeaconMgr delegate for get scanning result
```Java
KBeaconsMgr.KBeaconMgrDelegate beaconMgrDeletate = new KBeaconsMgr.KBeaconMgrDelegate()
    {
        //get advertisement packet during scanning callback
        public void onBeaconDiscovered(KBeacon[] beacons)
        {
            for (KBeacon beacon: beacons)
            {
                //get beacon adv common info
                Log.v(LOG_TAG, "beacon mac:" + beacon.getMac());
                Log.v(LOG_TAG, "beacon name:" + beacon.getName());
                Log.v(LOG_TAG,"beacon rssi:" + beacon.getRssi());

                //get adv packet
                for (KBAdvPacketBase advPacket : beacon.allAdvPackets())
                {
                    switch (advPacket.getAdvType())
                    {
                        case KBAdvType.KBAdvTypeIBeacon:
                        {
                            KBAdvPacketIBeacon advIBeacon = (KBAdvPacketIBeacon)advPacket;
                            Log.v(LOG_TAG,"iBeacon uuid:" + advIBeacon.getUuid());
                            Log.v(LOG_TAG,"iBeacon major:" + advIBeacon.getMajorID());
                            Log.v(LOG_TAG,"iBeacon minor:" + advIBeacon.getMinorID());
                            break;
                        }

                        case KBAdvType.KBAdvTypeEddyTLM:
                        {
                            KBAdvPacketEddyTLM advTLM = (KBAdvPacketEddyTLM)advPacket;
                            Log.v(LOG_TAG,"TLM battery:" + advTLM.getBatteryLevel());
                            Log.v(LOG_TAG,"TLM Temperature:" + advTLM.getTemperature());
                            Log.v(LOG_TAG,"TLM adv count:" + advTLM.getAdvCount());
                            break;
                        }
                    }
                }

                mBeaconsDictory.put(beacon.getMac(), beacon);
            }

            if (mBeaconsDictory.size() > 0) {
                mBeaconsArray = new KBeacon[mBeaconsDictory.size()];
                mBeaconsDictory.values().toArray(mBeaconsArray);
                mDevListAdapter.notifyDataSetChanged();
            }
        }

        public void onCentralBleStateChang(int nNewState)
        {
            if (nNewState == KBeaconsMgr.BLEStatePowerOff)
            {
                Log.e(LOG_TAG, "BLE function is power off");
            }
            else if (nNewState == KBeaconsMgr.BLEStatePowerOn)
            {
                Log.e(LOG_TAG, "BLE function is power on");
            }
        }

        public void onScanFailed(int errorCode)
        {
            Log.e(LOG_TAG, "Start N scan failed：" + errorCode);
            if (mScanFailedContinueNum >= MAX_ERROR_SCAN_NUMBER){
                toastShow("scan encount error, error time:" + mScanFailedContinueNum);
            }
            mScanFailedContinueNum++;
        }
    };
```

4. Handle KSensor data from advertisement packet
```Java
//get advertisement packet during scanning callback
public void onBeaconDiscovered(KBeacon[] beacons)
{
	KBAdvPacketSensor advSensor = (KBAdvPacketSensor)device.getAdvPacketByType(KBAdvType.KBAdvTypeSensor);
	if (advSensor != null)
	{
	   KBAccSensorValue accPos = advSensor.getAccSensor();
	   if (accPos != null) {
	      strAccValue = String.format(Locale.ENGLISH, "x:%d; y:%d; z:%d",
	            accPos.xAis, accPos.yAis, accPos.zAis);
	   }
	}
}
```

5. Clean scanning result and stop scanning  
After start scanning, The KBeaconMgr will buffer all found KBeacon device. If the app wants to remove all buffered KBeacon device, the app can:  
```Java
mBeaconsMgr.clearBeacons();
```
If the app wants to stop scanning:
```Java
mBeaconsMgr. stopScanning();
```

### 4.2 Connect to device
 1. If the app wants to change the device parameters, then it need connect to the device.
 ```Java
mBeacon.connect(password, max_timeout, connectionDelegate);
 ```
* Password: device password, the default password is 0000000000000000
* max_timeout: max connection time, unit is milliseconds.
* connectionDelegate: connection callback.

2. The app can handle connection result by follow:
 ```Java
private KBeacon.ConnStateDelegate connectionDelegate = new KBeacon.ConnStateDelegate()
{
    public void onConnStateChange(KBeacon var1, int state, int nReason)
    {
        if (state == KBeacon.KBStateConnected)
        {
            Log.v(LOG_TAG, "device has connected");
            nDeviceLastState = state;
        }
        else if (state == KBeacon.KBStateConnecting)
        {
            Log.v(LOG_TAG, "device start connecting");
            nDeviceLastState = state;
        }
        else if (state == KBeacon.KBStateDisconnecting)
        {
            Log.v(LOG_TAG, "device start disconnecting");
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
            Log.e(LOG_TAG, "device has disconnected:" +  nReason);
        }
    }
};
 ```

3. Disconnect from the device.
 ```Java
mBeacon.disconnect();
 ```

### 4.3 Configure parameters
#### 4.3.1 Advertisement type
KBeacon devices can support broadcasting multiple type advertisement packets in parallel.  
For example, advertisement period was set to 500ms. Advertisement type was set to “iBeacon + URL + UID + KSensor”, then the device will send advertisement packet like follow.   

|Time(ms)|0|500|1000|1500|2000|2500|3000|3500
|----|----|----|----|----|----|----|----|----
|`Adv type`|KSensor|UID|iBeacon|URL|KSensor|UID|iBeacon|URL


If the advertisement type contains TLM and other types, the KBeacon will send 1 TLM advertisement every 10 advertisement packets by default configuration.
For example: advertisement period was set to 500ms. Advertisement type was set to “URL + TLM”, and then the advertisement packet is like follow

|Time|0|500|1000|1500|2000|2500|3000|3500|4000|4500|5000
|----|----|----|----|----|----|----|----|----|----|----|----
|`Adv type`|URL|URL|URL|URL|URL|URL|URL|URL|URL|TLM|URL


**Notify:**  
  For the advertisement period, Apple has some suggestions that make the device more easily discovered by IOS phones. (The suggest value was: 152.5 ms; 211.25 ms; 318.75 ms; 417.5 ms; 546.25 ms; 760 ms; 852.5 ms; 1022.5 ms; 1285 ms). For more information, please refer to Section 3.5 in "Bluetooth Accessory Design Guidelines for Apple Products". The document link: https://developer.apple.com/accessories/Accessory-Design-Guidelines.pdf.

#### 4.3.2 Get device parameters
After the app connect to KBeacon success. The KBeacon will automatically read current parameters from KBeacon device. so the app can update UI and show the parameters to user after connection setup.  
 ```Java
private KBeacon.ConnStateDelegate connectionDelegate = new KBeacon.ConnStateDelegate()
{
    public void onConnStateChange(KBeacon var1, int state, int nReason)
    {
        if (state == KBeacon.KBStateConnected)
        {
            Log.v(LOG_TAG, "device has connected");

	updateDeviceToView();
        }
    }
};

//update device's configuration to UI
public void updateDeviceToView()
{
    boolean isTLMEnable = false, isUIDEnable = false, isUrlEnable = false;
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
        Log.v(LOG_TAG, "support trigger" + commonCfg.getTrigCapibility());

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

    //get eddystone URL parameters
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

    //other
}
 ```

#### 4.3.3 Update advertisement parameters

After app connects to device success, the app can update parameters of device.

##### 4.3.3.1 Update common parameters
The app can modify the basic parameters of KBeacon through the KBCfgCommon class. The KBCfgCommon has follow parameters:

* name: device name, the device name must <= 18 character

* advType: beacon type, can be setting to iBeacon, KSesnor, Eddy TLM/UID/ etc.,

* advPeriod: advertisement period, the value can be set to 100~10000ms

* txPower: advertisement TX power, unit is dBm.

* autoAdvAfterPowerOn: if autoAdvAfterPowerOn was setting to true, the beacon always advertisement if it has battery. If this value was setting to false, the beacon will power off if long press button for 5 seconds.

* tlmAdvInterval: eddystone TLM advertisement interval. The default value is 10. The KBeacon will send 1 TLM advertisement every 10 advertisement packets

* refPower1Meters: the rx power at 1 meters

* advConnectable: is beacon advertisement can be connectable.  
  **Warning:**   
   if the app set the KBeacon to un-connectable, the app cannot connect to it again if it does not has button. If the device has button, the device can enter connect-able advertisement for 60 seconds when click on the button

* password: device password, the password length must >= 8 character and <= 16 character.  
 **Warning:**   
 Be sure to remember the new password, you won’t be able to connect to the device if you forget the new password.

 Example: Update common parameters
```Java
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
        // Warning: if the app set the KBeacon to un-connectable, the app cannot connect to it if it does not has button.
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
```

##### 4.3.3.2 Update iBeacon parameters
The app can modify the basic parameters of KBeacon through the KBCfgIBeacon class. The KBCfgIBeacon has follow parameters:
uuid: iBeacon uuid
majorID: iBeacon major ID
minorID: iBeacon minor ID
please make sure the KBeacon advertisement type was set to iBeacon.

example: set the KBeacon to broadcasting iBeacon packet
```Java
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
```

##### 4.3.3.3 Update Eddystone parameters
The app can modify the eddystone parameters of KBeacon through the KBCfgEddyURL and KBCfgEddyUID class.  
The KBCfgEddyURL has follow parameters:
* url: eddystone URL address

The KBCfgEddyUID has follow parameters:
* nid: namespace id about UID. It is 10 bytes length hex string value.
* sid: instance id about UID. It is 6 bytes length hex string value.

```Java
//example1: update kbeacon to URL
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

//example2: update kbeacon to UID type
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
```

##### 4.3.3.4 Check if parameters are changed
Sometimes the app needs to configure multiple advertisement parameters at the same time.  
We recommend that the app should check whether the parameter was changed. The app doesn’t need to send the parameter if it’s value was not changed. Reducing the parameters will reduce the modification time.

Example: checking if the parameters was changed, then send new parameters to device.
```Java
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

        //adv period, check if adv period changed
        Integer changeTag = (Integer)mEditBeaconAdvPeriod.getTag();
        if (changeTag > 0)
        {
            String strAdvPeriod = mEditBeaconAdvPeriod.getText().toString();
            if (Utils.isPositiveInteger(strAdvPeriod)) {
                Float newAdvPeriod = Float.valueOf(strAdvPeriod);
                newCommomCfg.setAdvPeriod(newAdvPeriod);
            }
        }

        //tx power
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
        if (!strDeviceName.equals(oldCommonCfg.getName()) && strDeviceName.length() < KBCfgCommon.MAX_NAME_LENGTH)
				{
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
```

#### 4.3.4 Update trigger parameters
 For some KBeacon device that has some motion sensor, push button, etc., the app can set advertisement trigger and the device will advertise when the trigger condition is met. The trigger advertisement has follow parameters:
 * Trigger advertisement Mode: There are two modes of trigger advertisement. One mode is to broadcast only when the trigger is satisfied. The other mode is always broadcasting, and the content of advertisement packet will change when the trigger conditions are met.
 * Trigger parameters: For motion trigger, the parameter is acceleration sensitivity. For button trigger, you can set different trigger event (single click, double click, etc.,).
 *	Trigger advertisement type: The advertisement packet type when trigger event happened. It can be setting to iBeacon, Eddystone or KSensor advertisement.
 *	Trigger advertisement duration: The advertisement duration when trigger event happened.
 *	Trigger advertisement interval: The Bluetooth advertisement interval for trigger advertisement.  You can set a different value from always broadcast.  

 Example 1:  
  &nbsp;&nbsp;Trigger adv mode: setting to broadcast only on trigger event happened  
  &nbsp;&nbsp;Trigger adv type: iBeacon  
  &nbsp;&nbsp;Trigger adv duration: 30 seconds  
	&nbsp;&nbsp;Trigger adv interval: 300ms  
	![avatar](https://github.com/kkmhogen/KBeaconDemo_Android/blob/master/only_adv_when_trigger.png?raw=true)

 Example 2:  
	&nbsp;For some scenario, we need to continuously monitor the KBeacon to ensure that the device was alive, so we set the trigger advertisement mode to always advertisement.   
	&nbsp;We set an larger advertisement interval during alive advertisement and a short advertisement interval when trigger event happened, so we can achieve a balance between power consumption and triggers advertisement be easily detected.  
   &nbsp;&nbsp;Trigger adv mode: setting to Always advertisement  
   &nbsp;&nbsp;Trigger adv type: iBeacon  
   &nbsp;&nbsp;Trigger adv duration: 30 seconds  
 	 &nbsp;&nbsp;Trigger adv interval: 300ms  
	 &nbsp;&nbsp;Always adv interval: 2000ms
 	![avatar](https://github.com/kkmhogen/KBeaconDemo_Android/blob/master/always_adv_with_trigger.png?raw=true)


**Notify:**  
  The SDK will not automatically read trigger configuration after connection setup complete. So the app need read the trigger configuration manual if the app needed. Please reference 4.3.4.1 code for read trigger parameters from device.  

#### 4.3.4.1 Push button trigger
The push button trigger feature is used in some hospitals, nursing homes and other scenarios. When the user encounters some emergency event, they can click the button and the KBeacon device will start broadcast.
The app can configure single click, double-click, triple-click, long-press the button trigger, oor a combination.

**Notify:**  
* By KBeacon's default setting, long press button used to power on and off. Clicking button used to force the KBeacon enter connectable broadcast advertisement. So when you enable the long-press button trigger, the long-press power off function will be disabled. When you turn on the single/double/triple click trigger, the function of clicking to enter connectable broadcast state will also be disabled. After you disable button trigger, the default function about long press or click button will take effect again.
* iBeacon UUID for single click trigger = Always iBeacon UUID + 0x5
* iBeacon UUID for single double trigger = Always iBeacon UUID + 0x6
* iBeacon UUID for single triple trigger = Always iBeacon UUID + 0x7
* iBeacon UUID for single long press trigger = Always iBeacon UUID + 0x8

1. Enable or button trigger feature.  

```Java
//the code was in DevicePannelActivity.java file that in ibeacon demo project
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
```

2. The app can disable the button trigger

```Java
//disable button trigger
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
         Log.e(LOG_TAG, "Input parameters invalid");
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
```

3. The app can read the button current trigger parameters from KBeacon by follow code  

```Java
 //read button trigger information
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

                          //button trigger adv duration, uint is sec
                          Log.v(LOG_TAG, "Button trigger adv duration:" + btnCfg.getTriggerAdvTime());

                          //button trigger adv interval, uint is ms
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
 ```

#### 4.3.4.2 Motion trigger
The KBeacon can start broadcasting when it detects motion. Also the app can setting the sensitivity of motion detection.  
**Notify:**  
* iBeacon UUID for motion Trigger = Always iBeacon UUID + 0x1
* When the KBeacon enable the motion trigger, the Acc feature(X, Y, and Z axis detected function) in the KSensor broadcast will be disabled.


Enabling motion trigger is similar to push button trigger, which will not be described in detail here.

1. Enable or button trigger feature.  

```Java
-(void)enableMotionTrigger
{
    ... same as push button trigger

    //check device capability
    int nTriggerCapability = mBeacon.triggerCapability();
    if ((nTriggerCapability & KBCfgTrigger.KBTriggerTypeMotion) == 0) {
        Log.e(LOG_TAG, "device does not support motion trigger");
        return;
    }

    ... same as push button trigger

    //set trigger type
    btnTriggerPara.setTriggerType(KBCfgTrigger.KBTriggerTypeMotion);

    //set motion trigger sensitivity, the valid range is 2~31. The uint is 16mg.
    btnTriggerPara.setTriggerPara(3);

    ... same as push button trigger
}
```

#### 4.3.5 Send command to device
After app connect to device success, the app can send command to device.  
All command message between app and KBeacon are JSON format. Our SDK provide Hash Map to encapsulate these JSON message.
#### 4.3.5.1 Ring device
 For some KBeacon device that has buzzer function. The app can ring device. For ring command, it has 5 parameters:
 * msg: msg type is 'ring'
 * ringTime: unit is ms. The KBeacon will start flash/alert for 'ringTime' millisecond  when receive this command.
 * ringType: 0x0:led flash only; 0x1:beep alert only; 0x2 both led flash and beep;
 * ledOn: optional parameters, unit is ms. The LED will flash at interval (ledOn + ledOff).  This parameters is valid when ringType set to 0x0 or 0x2.
 * ledOff: optional parameters, unit is ms. the LED will flash at interval (ledOn + ledOff).  This parameters is valid when ringType set to 0x0 or 0x2.

```Java
public void ringDevice() {
        if (!mBeacon.isConnected()) {
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
```

#### 4.3.5.2 Reset configuration to default
 The app can use follow command to reset all configurations to default.
 * msg: message type is 'reset'

```Java
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
```

#### 4.3.6 Error cause in configurations/command
 App may get errors during the configuration. The KBException has follow values.
 * KBException.KBEvtCfgNoParameters: parameters is null
 * KBException.KBEvtCfgBusy: device is busy, please make sure last configuration complete
 * KBException.KBEvtCfgFailed: device return failed.
 * KBException.KBEvtCfgTimeout: configuration timeout
 * KBException.KBEvtCfgInputInvalid: input parameters data not in valid range
 * KBException.KBEvtCfgStateError: device is not in connected state
 * KBException.KBEvtCfgNotSupport: device does not support the parameters

 ```Java
{
    ...other code
    mBeacon.sendCommand(cmdPara, new KBeacon.ActionCallback()
    {
        @Override
        public void onActionComplete(boolean bConfigSuccess, KBException error)
        {
            if (!bConfigSuccess)
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
}
 ```
## 5. DFU
Through the DFU function, you can upgrade the firmware of the device. Our DFU function is based on Nordic's DFU library. In order to let you complete the development quickly, We add the DFU function into ibeacondemo project for your reference. The Demo about DFU includes the following class:
* KBeaconDFUActivity: DFU UI activity.  
* KBFirmwareDownload: Responsible for download the latest firmware from KKM clouds.
* DFUService: This DFU service for DFU option.
* NotificationActivity: During the upgrade, a notification will pop up, click on the notification to enter this activity.
![avatar](https://github.com/kkmhogen/KBeaconDemo_Android/blob/master/kbeacon_dfu_arc.png?raw=true)

### 5.1 Add DFU function to the application.
1. The DFU library need download the latest firmware from KKM cloud server. So you need add follow permission into AndroidManifest.xml
 ```
<uses-permission android:name="android.permission.INTERNET" />
 ```
2. The DFU Demo using nordic DFU library for update. So we need add follow dependency.
 ```
implementation 'no.nordicsemi.android:dfu:1.10.3'
 ```

3. Start DFU activity  
 ```Java
 public void onClick(View v)
{
    switch (v.getId()) {
        case R.id.dfuDevice:
            if (mBeacon.isConnected()) {
                final Intent intent = new Intent(this, KBeaconDFUActivity.class);
                intent.putExtra(KBeaconDFUActivity.DEVICE_MAC_ADDRESS, mBeacon.getMac());
                startActivityForResult(intent, 1);
            }
            break;
        }
}
```
If you want to known more details about getting the Device's latest firmware from KKM cloud, or deploied the latest firmware on you cloud. Please contact KKM sales(sales@kkmcn.com) and she/he will send you a detail document.

 Also for more detail nordic DFU library, please refer to
https://github.com/NordicSemiconductor/Android-DFU-Library

## 6. Special instructions

> 1. AndroidManifest.xml of SDK has declared to access Bluetooth permissions.
> 2. After connecting to the device successfully, we suggest delay 1 second to sending configure data, otherwise the device may not return data normally.
> 3. If you app need running in background, we suggest that sending and receiving data should be executed in the "Service". There will be a certain delay when the device returns data, and you can broadcast data to the "Activity" after receiving in the "Service".

## 7. Change log
* 2020.3.1 v1.23 change the adv period type from integer to float.
* 2020.1.16 v1.22 add button trigger.
* 2019.12.16 v1.21 add android10 permission.
* 2019.10.28 v1.2 add beep function.
* 2019.10.11 v1.1 add KSesnor function.
* 2019.2.1 v1.0 first version.
