package com.gddg08.metacontroller.hardware;


import android.content.Context;
import android.util.Log;

import com.gddg08.metacontroller.tool.byteCov;
import com.vise.baseble.ViseBle;
import com.vise.baseble.callback.IBleCallback;
import com.vise.baseble.callback.IConnectCallback;
import com.vise.baseble.callback.scan.IScanCallback;
import com.vise.baseble.callback.scan.ScanCallback;
import com.vise.baseble.callback.scan.SingleFilterScanCallback;
import com.vise.baseble.callback.scan.UuidFilterScanCallback;
import com.vise.baseble.common.PropertyType;
import com.vise.baseble.core.BluetoothGattChannel;
import com.vise.baseble.core.DeviceMirror;
import com.vise.baseble.exception.BleException;
import com.vise.baseble.model.BluetoothLeDevice;
import com.vise.baseble.model.BluetoothLeDeviceStore;

import java.time.temporal.Temporal;
import java.util.UUID;

// 智云稳定器的蓝牙控制库
public class CameraGimbal {

    private static final String TAG = "CameraGimbal";
    private final Context mContext;

    private static UUID serviceUUID = UUID.fromString("0000fee9-0000-1000-8000-00805f9b34fb");
    private static UUID writeUUID = UUID.fromString("d44bc439-abfd-45a2-b575-925416129600");
    private static UUID notifyUUID = UUID.fromString("d44bc439-abfd-45a2-b575-925416129601");
    private boolean onMoving;

    public CameraGimbal(Context context) {
        this.mContext = context;
    }

    private DeviceMirror deviceMirror;
    private boolean isActive;

    //设备连接
    public void connect() {

        ViseBle.getInstance().connectByName("Smooth-Q4141", new IConnectCallback() {


            @Override
            public void onConnectSuccess(DeviceMirror dm) {
                Log.d(TAG, "onConnectSuccess: ");
                deviceMirror = dm;
                DeviceInit();
            }

            @Override
            public void onConnectFailure(BleException exception) {
                Log.d(TAG, "onConnectFailure: ");
            }

            @Override
            public void onDisconnect(boolean ia) {
                Log.d(TAG, "onDisconnect: ");
                isActive = ia;
            }
        });


    }

    // 读写通道的初始化
    private void DeviceInit() {
        BluetoothGattChannel bluetoothGattChannel_write = new BluetoothGattChannel.Builder()
                .setBluetoothGatt(deviceMirror.getBluetoothGatt())
                .setPropertyType(PropertyType.PROPERTY_WRITE)
                .setServiceUUID(serviceUUID)
                .setCharacteristicUUID(writeUUID)
                .builder();
        deviceMirror.bindChannel(new IBleCallback() {
            @Override
            public void onSuccess(byte[] data, BluetoothGattChannel bluetoothGattChannel, BluetoothLeDevice bluetoothLeDevice) {
//                Log.d(TAG, "onWrite: " + byteCov.byte2Hex(data));
            }

            @Override
            public void onFailure(BleException exception) {
                Log.d(TAG, "onWriteFailure: " + exception.getDescription());
            }
        }, bluetoothGattChannel_write);

        BluetoothGattChannel bluetoothGattChannel_notify = new BluetoothGattChannel.Builder()
                .setBluetoothGatt(deviceMirror.getBluetoothGatt())
                .setPropertyType(PropertyType.PROPERTY_NOTIFY)
                .setServiceUUID(serviceUUID)
                .setCharacteristicUUID(notifyUUID)
                .builder();
        deviceMirror.bindChannel(new IBleCallback() {
            @Override
            public void onSuccess(byte[] data, BluetoothGattChannel bluetoothGattChannel, BluetoothLeDevice bluetoothLeDevice) {
                Log.d(TAG, "onBindNotify: " + byteCov.byte2Hex(data));
            }

            @Override
            public void onFailure(BleException exception) {
                Log.d(TAG, "onBindNotifyFailure: " + exception.getDescription());
            }
        }, bluetoothGattChannel_notify);
        deviceMirror.registerNotify(false);

        deviceMirror.setNotifyListener(bluetoothGattChannel_notify.getGattInfoKey(), notifyCallBack);

    }

    IBleCallback notifyCallBack = new IBleCallback() {
        @Override
        public void onSuccess(byte[] data, BluetoothGattChannel bluetoothGattChannel, BluetoothLeDevice bluetoothLeDevice) {
            Log.d(TAG, "onNotify: " + byteCov.byte2Hex(data));
            if (data.length>=7)
            if (data[1] == (byte)0xc1 && data[2] == (byte)0x32) {
                if (data[4] == (byte)0x04||data[4] == (byte)0x50||data[4] == (byte)0x00) {
                    onMoving = false;
                    Log.d(TAG, "onNotify=====> Moving Done!");
                }
            }
        }

        @Override
        public void onFailure(BleException exception) {
            Log.d(TAG, "onNotifyFailure: " + exception.getDescription());
        }
    };

    public void sendBytes(byte[] b) {
        if (deviceMirror==null)
            return;
        deviceMirror.writeData(b);
    }

    public void sendCMD(Cmd cmd) {
        sendBytes(cmd.getAll());
    }

    public void sendCMD(int x, int cmd, int val) {
        sendCMD(new Cmd(x, cmd, val));
    }

    public void sendCMDList(Cmd cmd_list[]) {
        new Thread(() -> {
            for (Cmd cmd : cmd_list) {
                sendCMD(cmd);
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    public void LookAt(float[] angles, int timeMs) {
//        if (onMoving){
//            Log.d(TAG, "LookAt===========> wait4Moving");
//            return;
//        }
        char pitch = (char) (angles[1] * 100);
        char pan = (char) (angles[0] * 100);
        char roll = (char) (angles[2] * 100);
        char time = (char) timeMs;
        Cmd cmd_list[] = {
                new Cmd(0x0, 0xc130, 0x0001),
                new Cmd(0x0, 0xc131, 0x0000),
                new Cmd(0x0, 0xc136, time),
                // new Cmd(0x0, 0xc137, 0x0000),
                new Cmd(0x0, 0xc133, pitch),
                // new Cmd(0x0, 0xc134, roll),
                new Cmd(0x0, 0xc135, pan),
                new Cmd(0x0, 0xc131, 0x0001)
        };
        sendCMDList(cmd_list);
//        wait4Moving();
}

    public void LookAt(float[] angles) {
//        if (onMoving){
//            Log.d(TAG, "LookAt===========> wait4Moving");
//            return;
//        }
        char pitch = (char) (angles[1] * 100);
        char pan = (char) (angles[0] * 100);
        char roll = (char) (angles[2] * 100);
        Cmd cmd_list[] = {
                new Cmd(0x0, 0xc130, 0x0001),
                new Cmd(0x0, 0xc131, 0x0000),
                // new Cmd(0x0, 0xc136, time),
                // new Cmd(0x0, 0xc137, 0x0000),
                new Cmd(0x0, 0xc133, pitch),
                 new Cmd(0x0, 0xc134, roll),
                new Cmd(0x0, 0xc135, pan),
                new Cmd(0x0, 0xc131, 0x0001)
        };
        sendCMDList(cmd_list);
//        wait4Moving();
    }

    public void wait4Moving() {
        onMoving = true;
        new Thread(() -> {
            while (onMoving) {
                sendCMD(0x0, 0xc132, 0x00);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

private class Cmd {
    byte[] cmdData = {0, 0, 0, 0, 0, 0, 0};

    Cmd(int x, int cmd, int val) {

        byte[] bArr = {(6 | 0), (byte) (((cmd >> 8) | x) | 0), (byte) (cmd | 0), (byte) ((val >> 8) | 0), (byte) (val | 0)};
        int crc = calcCRC(bArr);
        cmdData[0] = bArr[0];
        cmdData[1] = bArr[1];
        cmdData[2] = bArr[2];
        cmdData[3] = bArr[3];
        cmdData[4] = bArr[4];
        cmdData[5] = (byte) ((crc >> 8) & 0xFF);
        cmdData[6] = (byte) (crc & 0xFF);
    }

    int getPart1() {
        return cmdData[0] & 255;
    }

    int getPart2() {
        return ((cmdData[1] & 255) << 8) | (this.cmdData[2] & 255);
    }

    byte[] getAll() {
        return cmdData;
    }

}

    private static int lookupTable[] = {0, 4129, 8258, 12387, 16516, 20645, 24774, 28903, 33032, 37161, 41290, 45419, 49548, 53677, 57806, 61935, 4657, 528, 12915, 8786, 21173, 17044, 29431, 25302, 37689, 33560, 45947, 41818, 54205, 50076, 62463, 58334, 9314, 13379, 1056, 5121, 25830, 29895, 17572, 21637, 42346, 46411, 34088, 38153, 58862, 62927, 50604, 54669, 13907, 9842, 5649, 1584, 30423, 26358, 22165, 18100, 46939, 42874, 38681, 34616, 63455, 59390, 55197, 51132, 18628, 22757, 26758, 30887, 2112, 6241, 10242, 14371, 51660, 55789, 59790, 63919, 35144, 39273, 43274, 47403, 23285, 19156, 31415, 27286, 6769, 2640, 14899, 10770, 56317, 52188, 64447, 60318, 39801, 35672, 47931, 43802, 27814, 31879, 19684, 23749, 11298, 15363, 3168, 7233, 60846, 64911, 52716, 56781, 44330, 48395, 36200, 40265, 32407, 28342, 24277, 20212, 15891, 11826, 7761, 3696, 65439, 61374, 57309, 53244, 48923, 44858, 40793, 36728, 37256, 33193, 45514, 41451, 53516, 49453, 61774, 57711, 4224, 161, 12482, 8419, 20484, 16421, 28742, 24679, 33721, 37784, 41979, 46042, 49981, 54044, 58239, 62302, 689, 4752, 8947, 13010, 16949, 21012, 25207, 29270, 46570, 42443, 38312, 34185, 62830, 58703, 54572, 50445, 13538, 9411, 5280, 1153, 29798, 25671, 21540, 17413, 42971, 47098, 34713, 38840, 59231, 63358, 50973, 55100, 9939, 14066, 1681, 5808, 26199, 30326, 17941, 22068, 55628, 51565, 63758, 59695, 39368, 35305, 47498, 43435, 22596, 18533, 30726, 26663, 6336, 2273, 14466, 10403, 52093, 56156, 60223, 64286, 35833, 39896, 43963, 48026, 19061, 23124, 27191, 31254, 2801, 6864, 10931, 14994, 64814, 60687, 56684, 52557, 48554, 44427, 40424, 36297, 31782, 27655, 23652, 19525, 15522, 11395, 7392, 3265, 61215, 65342, 53085, 57212, 44955, 49082, 36825, 40952, 28183, 32310, 20053, 24180, 11923, 16050, 3793, 7920};

    private static int calcCRC(byte[] bArr) {
        int i = 0, i2 = 0;
        int length = bArr.length;
        while ((i < length)) {
            int i3 = i2 >> 8;
            i2 = (((i2 << 8) & 65535) ^ lookupTable[(bArr[i] & 255) ^ i3] & 65535);
            i++;
        }
        return i2;
    }
}
