/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.provision;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

/**
 * Application that sets the provisioned bit, like SetupWizard does.
 */
public class DefaultActivity extends Activity {
    private static final String TAG = "DefaultActivity";
    private Button mStatementButton;
    private boolean mShouldShowStatement;
    private boolean mIsCCSASupport;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mShouldShowStatement = getResources().getBoolean(R.bool.allow_show_operator_statement);
        mIsCCSASupport = getResources().getBoolean(R.bool.support_ccsa_feature);
        Log.d(TAG,"mShouldShowStatement is" + mShouldShowStatement+ ", mIsCCSASupportmIsCCSASupport is" + mIsCCSASupport);
        if (mShouldShowStatement || mIsCCSASupport) {
            if (UserHandle.myUserId() != UserHandle.USER_OWNER) {
                overridePendingTransition(0, 0);
                setProvision();
                Log.d("DefaultActivity", "not owner, finish");
                return;
            }
            overridePendingTransition(0, 0);
            //Showing CCSA statement has priority over showing CMCC statement
            if (mIsCCSASupport) {
                setContentView(R.layout.activity_ccsa);
            }else {
                setContentView(R.layout.activity_main);
            }
            mStatementButton = (Button) findViewById(R.id.confirm);
            mStatementButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "On button click and quit");
                    if (mIsCCSASupport) {
                        showPermission();
                    }
                    setProvision();
                }
            });
        } else {
            overridePendingTransition(0, 0);
            setProvision();
        }

    }

    private void setProvision() {
        // Add a persistent setting to allow other apps to know the device has been provisioned.
        Settings.Global.putInt(getContentResolver(), Settings.Global.DEVICE_PROVISIONED, 1);
        Settings.Secure.putInt(getContentResolver(), Settings.Secure.USER_SETUP_COMPLETE, 1);

        // remove this activity from the package manager.
        PackageManager pm = getPackageManager();
        ComponentName name = new ComponentName(this, DefaultActivity.class);
        pm.setComponentEnabledSetting(name, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);

        // terminate the activity.
        finish();
    }

    private void showPermission() {
        // open permission activity
        try {
            Intent intent = new Intent();
            intent.setAction("android.intent.action.CCSA");
            intent.addCategory("android.intent.category.CCSA");
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "do not found app", 0).show();
        }
    }
    @Override
    public void onBackPressed() {
        if (!mShouldShowStatement && !mIsCCSASupport) {
            super.onBackPressed();
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_CALL:
                if (!mShouldShowStatement && !mIsCCSASupport) {
                    return super.dispatchKeyEvent(event);
                } else {
                    return true;
                }
            case KeyEvent.KEYCODE_SEARCH:
                return true;
            default:
                break;
        }
        return super.dispatchKeyEvent(event);
    }
}
