package ru.wo0t.smarthouse.ui;

import ru.wo0t.smarthouse.R;
import ru.wo0t.smarthouse.SMHZApp;
import ru.wo0t.smarthouse.board.AbstractBoard;
import ru.wo0t.smarthouse.board.Sensor;
import ru.wo0t.smarthouse.board.boardsManager;
import ru.wo0t.smarthouse.common.constants;
import ru.wo0t.smarthouse.util.SystemUiHider;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class ShowPictureFullScreen extends Activity {
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
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */

    private AbstractBoard mBoard = null;
    private Sensor mSensor = null;
    private Sensor.SENSOR_SYSTEM mSystem = Sensor.SENSOR_SYSTEM.CAMES;
    private ProgressDialog mSensorUpdProcessDialog;

    private SystemUiHider mSystemUiHider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_show_picture_full_screen);

        final View controlsView = findViewById(R.id.fullscreen_content_controls);
        final View contentView = findViewById(R.id.fullscreenImageView);

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
        findViewById(R.id.pictureRefresh).setOnTouchListener(mDelayHideTouchListener);

        Intent intent = getIntent();
        int boardId = intent.getIntExtra(boardsManager.BOARD_ID, -1);
        mBoard = ((SMHZApp) getApplication()).getBoardsManager().getBoard(boardId);
        if (mBoard != null) {
            if (!intent.getStringExtra(boardsManager.SENSOR_NAME).isEmpty()) {
                mSensor = mBoard.getSens(intent.getStringExtra(boardsManager.SENSOR_NAME));
            }
        }
        updatePicture();

        ((Button)findViewById(R.id.pictureRefresh)).setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBoard.updateSens(mSensor);
                updateSensDialog();
            }
        });
    }

    private void updatePicture() {
        if (mBoard == null || mSensor == null || mSensor.getVal() == null) return;

        byte[] picData = (byte[])mSensor.getVal();
        Bitmap bMap = BitmapFactory.decodeByteArray(picData, 0, picData.length);

        if (bMap != null) {
            ((ImageView) findViewById(R.id.fullscreenImageView)).setImageBitmap(bMap);
            ((ImageView) findViewById(R.id.fullscreenImageView)).setContentDescription(mSensor.getName());
        }
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

    protected boolean updateSensDialog() {
        if (mSensor == null) return false;

        mSensorUpdProcessDialog = new ProgressDialog(this);
        mSensorUpdProcessDialog.setMessage(getString(R.string.sensUpdating) + "'" + mSensor.getName()+"'");
        mSensorUpdProcessDialog.show();

        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter iff= new IntentFilter(boardsManager.MSG_SENSOR_DATA);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(onNotice, iff);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(onNotice);
    }

    private final BroadcastReceiver onNotice= new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            int boardId = intent.getIntExtra(boardsManager.BOARD_ID, -1);
            Sensor.SENSOR_SYSTEM system = Sensor.SENSOR_SYSTEM.valueOf(intent.getStringExtra(boardsManager.MSG_SYSTEM_NAME));

            if (boardId == mBoard.getBoardId()) {
                switch (intent.getAction()) {
                    case boardsManager.MSG_SENSOR_DATA: {
                        if (mSystem == system) {
                            String sName = intent.getStringExtra(boardsManager.SENSOR_NAME);

                            if ((mSensor != null) && (mSensorUpdProcessDialog != null)) {
                                if (mSensor.getName().equals(sName)) {
                                    mSensorUpdProcessDialog.dismiss();
                                    mSensorUpdProcessDialog = null;
                                    updatePicture();
                                }
                            }
                        }
                    } break;
                }
            }
        }

    };

}
