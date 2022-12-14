package com.gddg08.metacontroller.data;

import android.util.Log;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import static com.gddg08.metacontroller.tool.byteCov.*;
import static com.gddg08.metacontroller.tool.myLog.logD;

public class Content {

    final public static String typeLi[] = {"BYTE", "uInt8", "uInt16", "uInt32", "Float", "Char", "Int8", "Int16", "Int32"};

    public ArrayList tagList = new ArrayList();
    public ArrayList list = new ArrayList();
    public int dataLen = 0;
    public int byteLen = 0;

    public Content(boolean debug) {
        if (debug)
            CreatContent_T();
    }

    public void CreatContent(byte[] data) {
        int num = (data.length - 1) / 20;
        for (int i = 0; i < num; i++) {
            int len = 19;
            for (int j = 1; j < 19; j++) {
                if (data[i * 20 + 1 + 1 + j] == (byte) 0) {
                    len = j;
                    break;
                }
            }
            byte[] temp = new byte[len];
            System.arraycopy(data, i * 20 + 1 + 1, temp, 0, len);
            String tag = new String(temp);
            int type = data[i * 20 + 1] & 0xFF;
            list.add(new Var(type, tag));
            tagList.add(tag);
            dataLen++;
            byteLen += getDataLen(type);
        }
    }

    private void CreatContent_T() {
        //test
        list.add(new Var(4, "这里是", fl2Byte(1.1f)));
        tagList.add("test1");
        list.add(new Var(4, "Meta Controller", fl2Byte(2.2f)));
        tagList.add("test2");
        list.add(new Var(4, "移动控制平台", fl2Byte(3.3f)));
        tagList.add("test3");
        list.add(new Var(4, "开启控制器", fl2Byte(4.4f)));
        tagList.add("test4");
        list.add(new Var(4, "右下角连接蓝牙", fl2Byte(5.5f)));
        tagList.add("test5");
        list.add(new Var(4, "即可接收分配空间数据", fl2Byte(6.6f)));
        tagList.add("test6");
        list.add(new Var(4, "也可结合手机传感器", fl2Byte(7.7f)));
        tagList.add("test7");
        list.add(new Var(4, "欢迎使用", fl2Byte(8.8f)));
        tagList.add("test8");
        dataLen = 8;

//        dataLen = list.size();
    }

    public boolean Update(byte[] data) {
        if (data.length != byteLen+1){
            logD("ERROR:" + "decode data len: " + data.length + ", exp: " + byteLen);
            return false;
        }
        int cur_pos = 1;//first as device id
        for (int i = 0; i < list.size(); i++) {
            int len = getDataLen(((Var) list.get(i)).type);
            byte[] temp = new byte[len];
            System.arraycopy(data, cur_pos, temp, 0, len);
            ((Var) list.get(i)).setData(temp);
            cur_pos += len;
            logD("RESULT:" + ((Var) list.get(i)).getTag() + "-->" + ((Var) list.get(i)).getStr());
        }
        return true;
    }

    private int getDataLen(int type) {
        switch (type) {
            case 0:
            case 1:
            case 5:
            case 6:
                return 1;
            case 2:
            case 7:
                return 2;
            case 3:
            case 4:
            case 8:
                return 4;
        }
        return 0;
    }
}
