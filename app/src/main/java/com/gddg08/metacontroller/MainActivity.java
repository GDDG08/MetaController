package com.gddg08.metacontroller;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.gddg08.metacontroller.hardware.CameraGimbal;
import com.gddg08.metacontroller.hardware.InnerSensor;
import com.gddg08.metacontroller.hardware.MetaCTRLer;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.Toast;

import static com.gddg08.metacontroller.tool.byteCov.*;
import static com.gddg08.metacontroller.tool.myLog.logD;
import static com.gddg08.metacontroller.tool.myLog.setEnableLogOut;

import com.gddg08.metacontroller.connect.BLESPPUtils;
import com.gddg08.metacontroller.connect.DeviceHandle;
import com.gddg08.metacontroller.connect.DeviceList;
import com.gddg08.metacontroller.data.Logger;
import com.gddg08.metacontroller.tool.FileUtils;
import com.gddg08.metacontroller.ui.main.PlaceholderFragment;
import com.gddg08.metacontroller.ui.main.SectionsPagerAdapter;
import com.gddg08.metacontroller.databinding.ActivityMainBinding;
import com.gddg08.metacontroller.view.DeviceDialog;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity implements BLESPPUtils.OnBluetoothAction {

    @SuppressLint("StaticFieldLeak")
    public static BLESPPUtils mBLESPPUtils;
    private DeviceDialog mDeviceDialogCtrl;
    public static ArrayList<BluetoothDevice> mDevicesList = new ArrayList<>();

    boolean first_flag = true;
    public static final String PREFS_NAME = "com.gddg08.metacontroller.color";

    //    public ContentUpdate mContentupdate = new ContentUpdate();
    private static final int UPDATE = 0;

    public static Logger mLogger= new Logger();

    public static InnerSensor mInnerSensor;
    public static MetaCTRLer mMetaCTRLer;
    public static CameraGimbal mCameraGimbal;

//    @SuppressLint("HandlerLeak")
//    private Handler handler = new Handler() {
//        @Override
//        public void handleMessage(Message msg) {
//            DeviceList.targetDevices.get(msg.what).onUIUpdate();
//        }
//    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        com.gddg08.metacontroller.databinding.ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        ViewPager viewPager = binding.viewPager;
        viewPager.setAdapter(sectionsPagerAdapter);
        TabLayout tabs = binding.tabs;
        tabs.setupWithViewPager(viewPager);

        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                //Toast.makeText(MainActivity.this,tab.getPosition()+"",Toast.LENGTH_LONG).show();
                switch (tab.getPosition()) {
                    case PlaceholderFragment.Page_Info - 1:
                        for (DeviceHandle dh : DeviceList.targetDevices)
                            if (dh.dAdapter != null)
                                dh.dAdapter.onHold = false;
                        break;
                    case PlaceholderFragment.Page_Tools - 1:
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case PlaceholderFragment.Page_Info - 1:
                        for (DeviceHandle dh : DeviceList.targetDevices)
                            if (dh.dAdapter != null)
                                dh.dAdapter.onHold = true;
                        break;
                    case PlaceholderFragment.Page_Tools - 1:
                        break;
                }
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case PlaceholderFragment.Page_Info - 1:
                        for (DeviceHandle dh : DeviceList.targetDevices)
                            if (dh.dAdapter != null)
                                dh.dAdapter.onHold = false;
                        break;
                    case PlaceholderFragment.Page_Tools - 1:
                        break;
                }
            }
        });
        FloatingActionButton fab = binding.fab;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PlaceholderFragment.switch2.setChecked(false);
                mDeviceDialogCtrl.show();
                checkGPS();
            }
        });

        initPermissions();
        mBLESPPUtils = new BLESPPUtils(MainActivity.this, this);
        setEnableLogOut();
//        mBLESPPUtils.setStopFlag("@\r\n".getBytes());
        if (!mBLESPPUtils.isBluetoothEnable()) mBLESPPUtils.enableBluetooth();
        mBLESPPUtils.onCreate();
        mDeviceDialogCtrl = new DeviceDialog(this, mBLESPPUtils);
        DeviceList.setOnBluetoothAction(this);

        mInnerSensor = new InnerSensor(this);
        mMetaCTRLer = new MetaCTRLer(this);

        mCameraGimbal = new CameraGimbal(this);
        mCameraGimbal.connect();
    }

    public void checkGPS() {
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean enable = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!enable) {
            Toast.makeText(getApplicationContext(), "没开定位服务！\n搜不到设备哟~", Toast.LENGTH_LONG).show();
        }
    }

    private void initPermissions() {
        if (ContextCompat.checkSelfPermission(this, "android.permission-group.LOCATION") != 0) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            "android.permission.ACCESS_FINE_LOCATION",
                            "android.permission.ACCESS_COARSE_LOCATION",
                            "android.permission.ACCESS_WIFI_STATE",
                            "android.permission.WRITE_EXTERNAL_STORAGE",
                            "android.permission.READ_EXTERNAL_STORAGE"
                    },
                    1
            );
        }
        if (Build.VERSION.SDK_INT >= 23) {
            int REQUEST_CODE_PERMISSION_STORAGE = 1;
            String[] permissions = {
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };

            for (String str : permissions) {
                if (this.checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
                    this.requestPermissions(permissions, REQUEST_CODE_PERMISSION_STORAGE);
                    return;
                }
            }
        }
    }

    @Override
    public void onFoundDevice(BluetoothDevice device) {

//        Toast.makeText(MainActivity.this, device.getName(),Toast.LENGTH_LONG).show();

        // 判断是不是重复的
        for (int i = 0; i < mDevicesList.size(); i++) {
            if (mDevicesList.get(i).getAddress().equals(device.getAddress())) return;
        }
        // 添加，下次有就不显示了
        mDevicesList.add(device);
        // 添加条目到 UI 并设置点击事件
        mDeviceDialogCtrl.addDevice(device, new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                BluetoothDevice clickDevice = (BluetoothDevice) v.getTag();
                postShowToast("开始连接:" + clickDevice.getName());
//                mLogTv.setText(mLogTv.getText() + "\n" + "开始连接:" + clickDevice.getName());
//                mBLESPPUtils.connect(clickDevice);
                DeviceList.connect(MainActivity.this, clickDevice.getAddress());
            }
        });
    }

    @Override
    public void onConnectSuccess(BluetoothDevice device, BluetoothSocket socket) {
        logD("DOUBLE" + "连接成功" + device.getName() + device.getAddress());
        postShowToast(device.getName() + "(" + device.getAddress() + ")\n连接成功!", () -> {
//            mDeviceDialogCtrl.dismiss();
            SharedPreferences.Editor info_edit = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE).edit();
            info_edit.putString("devLast_addr", device.getAddress());
            info_edit.putString("devLast_name", device.getName());
            info_edit.apply();

        });
    }

    @Override
    public void onConnectFailed(String deviceMac, String msg) {
        postShowToast("连接失败:" + msg);
        postShowToast(msg, () -> {
            new AlertDialog.Builder(this)
                    .setTitle("是否重连")
                    .setMessage(msg)
                    .setNegativeButton("取消", (view, which) -> {
                        DeviceList.removeDevice(deviceMac);
//                                    DeviceList.targetDevices.remove(DeviceList.getDeviceHandle(deviceMac));
                    })
                    .setPositiveButton("重试", (view, which) -> {
                        DeviceList.getDeviceHandle(deviceMac).connect();
                    })
                    .show();
        });

    }

    @Override
    public void onReceiveBytes(int id, byte[] bytes) {

        logD("Receiving1----->设备" + id );
        switch (bytes[0]) {
            case (byte) 0xff:

                break;
            case (byte) 0xc8:
                mMetaCTRLer.onIMUChanged(DeviceList.getDeviceHandle(id).mContent);
            case (byte) 0x01:
            case (byte) 0x02:
            case (byte) 0x03:
            default:
                mLogger.runOnCall();
//                Message msg = new Message();
//                msg.what = id;
//                handler.sendMessage(msg);
                DeviceList.getDeviceHandle(id).onUIUpdate();

                break;
        }
    }

    @Override
    public void onSendBytes(int id, byte[] bytes) {
        logD("BLE,Sending----->" + byte2Hex(bytes));
    }

    @Override
    public void onFinishFoundDevice() {
        Toast.makeText(this, "搜索已暂停", Toast.LENGTH_SHORT).show();
    }

    public void postShowToast(final String msg) {
        postShowToast(msg, null);
    }

    public void postShowToast(final String msg, final DoSthAfterPost doSthAfterPost) {
        runOnUiThread(() -> {
            Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            if (doSthAfterPost != null) doSthAfterPost.doIt();
        });
    }


    private interface DoSthAfterPost {
        void doIt();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) {
            if (resultCode == Activity.RESULT_OK) {//是否选择，没选择就不会继续
                FileUtils fu = new FileUtils(this);
                Uri uri = data.getData();//得到uri，后面就是将uri转化成file的过程。
                mLogger.file = fu.getFilePathByUri(uri);

                mLogger.start(this);
                Toast.makeText(this, mLogger.file, Toast.LENGTH_SHORT).show();
                SharedPreferences.Editor info_edit = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE).edit();
                info_edit.putString("LogFile", mLogger.file);
                info_edit.apply();

                PlaceholderFragment.switch1.setChecked(true);
                String filename[] = mLogger.file.split("/");
                PlaceholderFragment.textView_file.setText(filename[filename.length - 1]);
            }
        }
    }

    public void web(String url) {
        Intent intent = new Intent();
        intent.setAction("android.intent.action.VIEW");
        Uri content_url = Uri.parse(url);
        intent.setData(content_url);
        startActivity(intent);
    }


    private static Boolean isExit = false;

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
        Timer tExit = null;
        if (isExit == false) {
            isExit = true; // 准备退出
            Toast.makeText(this, "再按一次退出", Toast.LENGTH_SHORT).show();
            tExit = new Timer();
            tExit.schedule(new TimerTask() {
                @Override
                public void run() {
                    isExit = false;
                }
            }, 2000);
        } else {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            startActivity(intent);
        }
    }

    @Override
    protected void onDestroy() {
        mBLESPPUtils.onDestroy();
        mLogger.stop();
        DeviceList.removeAll();
        super.onDestroy();
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        for (DeviceHandle dh : DeviceList.targetDevices)
            if (dh.dAdapter != null)
                if (dh.dAdapter.onScope) {
                    dh.dAdapter.setOnScope(false, false);
                    dh.dAdapter.pauseShow = true;
                }
        mInnerSensor.Pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        for (DeviceHandle dh : DeviceList.targetDevices)
            if (dh.dAdapter != null)
                if (dh.dAdapter.pauseShow) {
                    dh.dAdapter.setOnScope(true, false);
                    dh.dAdapter.pauseShow = false;
                }
        mInnerSensor.Resume();
    }
}