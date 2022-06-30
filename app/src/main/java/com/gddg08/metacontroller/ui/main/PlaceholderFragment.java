package com.gddg08.metacontroller.ui.main;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import com.gddg08.metacontroller.MainActivity;
import com.gddg08.metacontroller.connect.BLESPPUtils;
import com.gddg08.metacontroller.connect.DeviceHandle;
import com.gddg08.metacontroller.connect.DeviceList;
import com.gddg08.metacontroller.data.ContentAdapter;
import com.gddg08.metacontroller.databinding.Fragment1Binding;
import com.gddg08.metacontroller.databinding.Fragment2Binding;
import com.gddg08.metacontroller.databinding.FragmentMainBinding;
import com.gddg08.metacontroller.hardware.CameraGimbal;
import com.gddg08.metacontroller.view.MyListView;
import com.gddg08.metacontroller.view.MyScrollView;

import java.util.Timer;
import java.util.TimerTask;

import static com.gddg08.metacontroller.MainActivity.mCameraGimbal;
import static com.gddg08.metacontroller.MainActivity.mInnerSensor;
import static com.gddg08.metacontroller.MainActivity.mLogger;
import static com.gddg08.metacontroller.MainActivity.mMetaCTRLer;
import static com.gddg08.metacontroller.tool.byteCov.*;
import static com.gddg08.metacontroller.tool.myLog.logD;
import static java.lang.Math.abs;

/**
 * A placeholder fragment containing a simple view.
 */
public class PlaceholderFragment extends Fragment {

    private static final String ARG_SECTION_NUMBER = "section_number";

    private PageViewModel pageViewModel;

    private FragmentMainBinding binding;
    public static Fragment1Binding binding1;
    public static Fragment2Binding binding2;

    public static final int Page_Info = 1;
    public static final int Page_Tools = 2;

    public static int radiobutton_selected = 2;
    public static ContentAdapter dAdapter;
    public static MyListView lvd;
    public static TextView textView_fps;
    public static TextView textView_file;
    public static Switch switch1;
    public static Switch switch2;
    private static FragmentActivity mActivity;

    public static PlaceholderFragment newInstance(int index) {
        PlaceholderFragment fragment = new PlaceholderFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_SECTION_NUMBER, index);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pageViewModel = new ViewModelProvider(this).get(PageViewModel.class);
        int index = 1;
        if (getArguments() != null) {
            index = getArguments().getInt(ARG_SECTION_NUMBER);
        }
        pageViewModel.setIndex(index);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        int index = getArguments().getInt(ARG_SECTION_NUMBER);
        View root = null;
        switch (index) {
            case Page_Info:
                binding1 = Fragment1Binding.inflate(inflater, container, false);
                root = binding1.getRoot();

                MyScrollView mainScroll = binding1.mainScroll;
                mainScroll.setScrollListener(new MyScrollView.ScrollListener() {

                    Timer tStop = new Timer();

                    @Override
                    public void onScrollBegin(MyScrollView scrollView) {
                        for (DeviceHandle dh : DeviceList.targetDevices)
                            if (dh.dAdapter != null) {
                                dh.dAdapter.onHold = true;

                                tStop.cancel();
                                tStop = new Timer();
                                tStop.schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        dh.dAdapter.onHold = false;
//                                        Log.d("Scroll", "timer");
                                    }
                                }, 50);
                            }
                    }

                    @Override
                    public void onScrollStop(MyScrollView scrollView) {
                        for (DeviceHandle dh : DeviceList.targetDevices)
                            if (dh.dAdapter != null)
                                dh.dAdapter.onHold = false;
                    }
                });

                switch1 = binding1.switch1;
                switch1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (mLogger.onLogging && isChecked)
                            return;
                        if (mLogger.file != null) {
                            if (isChecked)
                                mLogger.writeHeader();
                            mLogger.onLogging = isChecked;
                        } else {
                            buttonView.setChecked(false);
                            mLogger.init(getActivity());
                        }
                    }
                });
//                textView_fps = binding1.textViewFps;
                textView_file = binding1.textViewFile;
                textView_file.setOnClickListener((v) -> {
                    Toast.makeText(getContext(), mLogger.file + "\n饼：这里应该是点按打开，长按分享", Toast.LENGTH_SHORT).show();
                });

                switch2 = binding1.switch2;
                switch2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        for (DeviceHandle device : DeviceList.targetDevices)
                            if (device.dAdapter != null)
                                device.dAdapter.setOnScope(isChecked, true);
//                        dAdapter.notifyDataSetChanged();
//                        lvd.postInvalidate();
                    }
                });
                binding1.refresh.setOnClickListener((v) -> {
                    for (DeviceHandle deviceHandle : DeviceList.targetDevices)
                        deviceHandle.sendData(i82Byte(0xf1));
                });
                DeviceList.demo(getActivity(), "DEMO");


                break;
            case Page_Tools:
                binding2 = Fragment2Binding.inflate(inflater, container, false);
                root = binding2.getRoot();

                switch_sensor = binding2.switchSensor;
                switch_imu = binding2.switchImu;

                binding2.button2.setOnClickListener((v) -> {
                    mInnerSensor.refreshOffset();
                    mMetaCTRLer.refreshOffset();
                });

                binding2.button3.setOnClickListener((view) -> {
                    byte[] test = new byte[]{0x06, 0x01, 0x06, 0x00, 0x00, 0x09, (byte) 0x91};
                    mCameraGimbal.sendBytes(test);
                });
                binding2.button4.setOnClickListener((view) -> {
//                    mCameraGimbal.sendCMD(1, 6, 0);
//                    mCameraGimbal.LookAt(new float[]{2.14f, -26.67f, 0.0f});
//                    sensor_orientation[0] *= -1;
//                    sensor_orientation[1] *= -1;
                    mCameraGimbal.LookAt(sensor_orientation);
                });
//                new Thread(()->{
//                    while(true) {
//                        if (switch_sensor.isChecked()) {
//                            sensor_orientation[0] *= -1;
//                            sensor_orientation[1] *= -1;
//                            mCameraGimbal.LookAt(sensor_orientation);
//                            try {
//                                Thread.sleep(100);
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                    }
//                }).start();
                mActivity = getActivity();
                break;
            default:
                break;
        }
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    static Switch switch_sensor;
    static boolean sensor_first = true;

    static float[] sensor_orientation = {0, 0, 0};

    public static void onSensorUpdate(float[] orientaion) {

        mActivity.runOnUiThread(() ->
                binding2.textView2.setText("Sensor Data: \nx:" + orientaion[0] + "\ny:" + orientaion[1] + "\nz:" + orientaion[2]));

        if (switch_sensor.isChecked()) {
            if (sensor_first == true) {
                mInnerSensor.refreshOffset();
                sensor_first = false;
            }
            GimbalCTRL(orientaion);
            CameraGimbalCTRL(sensor_orientation, orientaion);
        } else {
            sensor_first = true;
        }
        sensor_orientation = orientaion.clone();
    }

    static Switch switch_imu;
    static boolean imu_first = true;

    static float[] imu_orientation = {0, 0, 0};

    public static void onIMUUpdate(float[] orientaion) {

        if (switch_imu.isChecked()) {
            if (imu_first == true) {
                mMetaCTRLer.refreshOffset();
                imu_first = false;
            }
            GimbalCTRL(orientaion);
            CameraGimbalCTRL(imu_orientation, orientaion);
        } else {
            imu_first = true;
        }
        imu_orientation = orientaion.clone();
    }
    //机器人云台控制
    private static void GimbalCTRL(float[] orientaion) {
        byte[] temp = new byte[9];
        temp[0] = (byte) 0xb6;
        System.arraycopy(fl2Byte(orientaion[0]), 0, temp, 1, 4);
        System.arraycopy(fl2Byte(-orientaion[1]), 0, temp, 5, 4);
        logD("CMD_SET_GIMBAL_ANGLE:-->" + byte2Hex(temp));
        try {
            DeviceList.getDeviceHandle(0).sendData(temp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static boolean onNewMove = false;

    private static void CameraGimbalCTRL(float[] old_ori, float[] new_ori) {
        Log.d("CameraGimbalCTRL", String.valueOf(new_ori[0] - old_ori[0]));
        if ((abs(new_ori[0] - old_ori[0]) < 0.2f || abs(new_ori[1] - old_ori[1]) < 0.2f || abs(new_ori[2] - old_ori[2]) < 0.2f) && onNewMove) {
            Log.d("CameraGimbalCTRL", "smallmove ");

            mCameraGimbal.LookAt(new float[]{-new_ori[0], new_ori[1], new_ori[2]});
            onNewMove = false;
        } else if (abs(new_ori[0] - old_ori[0]) > 0.8 || abs(new_ori[1] - old_ori[1]) > 0.8 || abs(new_ori[2] - old_ori[2]) > 0.8) {
            Log.d("CameraGimbalCTRL", "bigmove ");

            onNewMove = true;
        }

    }
}