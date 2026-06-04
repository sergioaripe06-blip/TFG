package com.sergio.flatshare.core.session;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.google.firebase.auth.FirebaseAuth;

public class FlatShareApp extends Application {
    private int startedActivities = 0;
    private boolean changingConfigurations = false;

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            }

            @Override
            public void onActivityStarted(Activity activity) {
                startedActivities++;
            }

            @Override
            public void onActivityResumed(Activity activity) {
            }

            @Override
            public void onActivityPaused(Activity activity) {
            }

            @Override
            public void onActivityStopped(Activity activity) {
                changingConfigurations = activity.isChangingConfigurations();
                startedActivities = Math.max(0, startedActivities - 1);
                if (startedActivities == 0 && !changingConfigurations) {
                    handleAppBackgrounded();
                }
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
            }
        });
    }

    private void handleAppBackgrounded() {
        if (SessionStore.isRememberMeEnabled(this)) {
            SessionStore.clearReauthRequired(this);
            return;
        }
        SessionStore.setReauthRequired(this, true);
        FirebaseAuth.getInstance().signOut();
    }
}
