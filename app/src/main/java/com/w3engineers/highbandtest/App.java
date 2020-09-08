package com.w3engineers.highbandtest;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;

/**
 * App's Parent class
 */
public class App extends Application {
    private static Context context;
    private static Activity mActivity = null;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();


        registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
//                MeshLog.v("activity found oncreate " + activity.getLocalClassName());
//                mActivity = activity;
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
//                MeshLog.v("activity found ondestroyed " + activity.getLocalClassName());
//                mActivity = null;
            }

            /** Unused implementation **/
            @Override
            public void onActivityStarted(Activity activity) {
//                MeshLog.v("activity found onstarted " + activity.getLocalClassName());

//                mActivity = activity;
            }

            @Override
            public void onActivityResumed(Activity activity) {
//                MeshLog.v("activity found onresumed " + activity.getLocalClassName());
                mActivity = activity;
            }
            @Override
            public void onActivityPaused(Activity activity) {
//                MeshLog.v("activity found onpaused " + activity.getLocalClassName());
                mActivity = null;
            }

            @Override
            public void onActivityStopped(Activity activity) {
//                MeshLog.v("activity found onstopped " + activity.getLocalClassName());
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
        });
    }

    public static Context getContext(){
        return context;
    }


    public static Activity getCurrentActivity() {
        return mActivity;
    }


}
