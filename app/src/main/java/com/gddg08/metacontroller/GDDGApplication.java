package com.gddg08.metacontroller;

import android.app.Application;

import com.vise.baseble.ViseBle;
import com.gddg08.metacontroller.tool.CrashHandler;

import org.xutils.x;

public class GDDGApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        CrashHandler crashHandler = CrashHandler.getInstance();
        crashHandler.init(getApplicationContext());
        x.Ext.init(this);

        ViseBle.getInstance().init(this);
    }
}
