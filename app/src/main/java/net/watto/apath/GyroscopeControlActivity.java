package net.watto.apath;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.widget.TextView;
import android.widget.Toast;

import net.watto.apath.util.SystemUiHider;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see net.watto.apath.util.SystemUiHider
 */
public class GyroscopeControlActivity extends ControlActivity implements SensorEventListener {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * If set, will toggle the system UI visibility upon interaction. Otherwise,
     * will show the system UI visibility upon interaction.
     */
    private static final boolean TOGGLE_ON_CLICK = true;

    /**
     * The flags to pass to {@link net.watto.apath.util.SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    /**
     * The instance of the {@link net.watto.apath.util.SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;

    private SensorManager sm;
    private Sensor sa, smf;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // check is gyroscope available
        SensorManager sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) == null) {
            Toast.makeText(this, R.string.position_not_available, Toast.LENGTH_LONG).show();
            this.finish();
            return;
        }

        setContentView(R.layout.activity_gyroscope);

        final View controlsView = findViewById(R.id.fullscreen_content_controls);
        final View contentView = findViewById(R.id.control_values);

        // Set up an instance of SystemUiHider to control the system UI for
        // this activity.
        mSystemUiHider = SystemUiHider.getInstance(this, contentView, HIDER_FLAGS);
        mSystemUiHider.setup();
        mSystemUiHider
                .setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
                    // Cached values.
                    int mControlsHeight;
                    int mShortAnimTime;

                    @Override
                    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
                    public void onVisibilityChange(boolean visible) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                            // If the ViewPropertyAnimator API is available
                            // (Honeycomb MR2 and later), use it to animate the
                            // in-layout UI controls at the bottom of the
                            // screen.
                            if (mControlsHeight == 0) {
                                mControlsHeight = controlsView.getHeight();
                            }
                            if (mShortAnimTime == 0) {
                                mShortAnimTime = getResources().getInteger(
                                        android.R.integer.config_shortAnimTime);
                            }
                            controlsView.animate()
                                    .translationY(visible ? 0 : mControlsHeight)
                                    .setDuration(mShortAnimTime);
                        } else {
                            // If the ViewPropertyAnimator APIs aren't
                            // available, simply show or hide the in-layout UI
                            // controls.
                            controlsView.setVisibility(visible ? View.VISIBLE : View.GONE);
                        }

                        if (visible && AUTO_HIDE) {
                            // Schedule a hide().
                            delayedHide(AUTO_HIDE_DELAY_MILLIS);
                        }
                    }
                });

        // Set up the user interaction to manually show or hide the system UI.
        contentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TOGGLE_ON_CLICK) {
                    mSystemUiHider.toggle();
                } else {
                    mSystemUiHider.show();
                }
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.control_exit).setOnTouchListener(mDelayHideTouchListener);

        // sensor
        this.sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sa = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        smf = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    @Override
    public void onResume() {
        super.onResume();
        sm.registerListener(this, sa, SensorManager.SENSOR_DELAY_UI);
        sm.registerListener(this, smf, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onPause() {
        super.onPause();
        sm.unregisterListener(this);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }


    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    Handler mHideHandler = new Handler();
    Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mSystemUiHider.hide();
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    public void finishActivity(View v) {
        finish();
    }

    protected float[] values_sa, values_smf;

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        double tmp;
        switch (sensorEvent.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                values_sa = sensorEvent.values;
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                values_smf = sensorEvent.values;
                break;
        }
        if (values_smf == null || values_sa == null) {
            return;
        }
        float[] mR = new float[9];
        float[] mI = new float[9];
        float[] mV = new float[3];
        SensorManager.getRotationMatrix(mR, mI, values_sa, values_smf);
        SensorManager.getOrientation(mR, mV);
        tmp = Math.max(Math.min(mV[1], .6), -.6);
        int forward = (int)(213 * tmp);
        tmp = Math.max(Math.min(mV[2], .6), -.6);

        int r = forward;
        int l = forward;
        if (tmp > 0) {
            l *= (.6 - tmp) / .6;
        } else if(tmp < 0) {
            r *= (.6 + tmp) / .6;
        }

        r = Math.max(Math.min(127, r + 64), 0);
        l = Math.max(Math.min(127, l + 64), 0);

        TextView tv = (TextView) findViewById(R.id.control_values);
        tv.setText(
                String.format(
                        "Udaljenost:\n%d cm",
                        this.distance
                )
        );
        /*tv.setText(
                String.format(
                        "Z: %.3f\nX: %.3f\nY: %.3f",
                        mV[0],
                        mV[1],
                        mV[2]
                )
        );*/
        command[0] = 'M';
        command[1] = (byte)r;
        command[2] = (byte)l;
        tv = (TextView) findViewById(R.id.control_values_device);
        tv.setText(String.format("M%02X%02X", command[1], command[2]));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
