/*
 * Copyright (c) 2017 The LineageOS Project
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 * Also add information on how to contact you by electronic and paper mail.
 *
 */

package org.lineageos.flipflap;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.IBinder;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManagerPolicy.WindowManagerFuncs;

public class FlipFlapService extends Service {
    private static final String TAG = "FlipFlap";

    private Context mContext;
    private FlipFlapView mCoverView;
    private WindowManager mWm;
    private int mStartId;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "Creating cover view");

        mWm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        mContext = new ContextThemeWrapper(this, R.style.FlipFlapTheme);

        final Notification notification = new Notification.Builder(this)
                .setContentTitle(getString(R.string.notification_title))
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(Notification.PRIORITY_MIN)
                .setOngoing(true)
                .setShowWhen(false)
                .setLocalOnly(true)
                .build();
        startForeground(1, notification);

        registerReceiver(mReceiver, new IntentFilter(com.android.internal.util.mdroid.content.Intent.ACTION_LID_STATE_CHANGED));
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (com.android.internal.util.mdroid.content.Intent.ACTION_LID_STATE_CHANGED.equals(intent.getAction())) {
                int lidState = intent.getIntExtra(com.android.internal.util.mdroid.content.Intent.EXTRA_LID_STATE, -1);
                if (lidState == WindowManagerFuncs.LID_OPEN) {
                    stopSelf(mStartId);
                }
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroying cover view");
        if (mCoverView != null) {
            mCoverView.setVisibility(View.GONE);
            mWm.removeView(mCoverView);
            mCoverView = null;
        }
        unregisterReceiver(mReceiver);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mStartId = startId;
        if (mCoverView == null) {
            mCoverView = createCoverView();
            if (mCoverView == null) {
                stopSelf(mStartId);
            } else {
                WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.TYPE_BOOT_PROGRESS);
                params.screenBrightness = mCoverView.getScreenBrightness();
                params.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                mWm.addView(mCoverView, params);
            }
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private FlipFlapView createCoverView() {
        int coverStyle = FlipFlapUtils.getCoverStyle(mContext);
        switch (coverStyle) {
            case FlipFlapUtils.COVER_STYLE_DOTCASE: return new DotcaseView(mContext);
            case FlipFlapUtils.COVER_STYLE_CIRCLE: return new CircleView(mContext);
            case FlipFlapUtils.COVER_STYLE_RECTANGULAR: return new RectangularView(mContext);
            case FlipFlapUtils.COVER_STYLE_ICEVIEW: return new IceviewView(mContext);
        }

        // Not possible because of the check, above, matching on the valid covers
        return null;
    }
}
