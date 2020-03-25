package jp.ac.u_tokyo.t.seo.station.app;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.MergeCursor;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.AlarmClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.Context.MODE_PRIVATE;

/**
 * @author Seo-4d696b75
 * @version 2018/12/27.
 */

class OverlayNotification extends BroadcastReceiver implements View.OnClickListener,
        PositionPredictor.StationPredictCallback, View.OnLongClickListener, View.OnTouchListener{

    private final String KEY_TIMER_ICON_POSITION = "timer_position_y";

    OverlayNotification(Context context, StationService service){
        setContext(context);
        mContext = context;
        mService = service;
        LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mWindowManager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        mPowerManager = (PowerManager)context.getSystemService(Context.POWER_SERVICE);

        final int layerType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;


        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                0, 0, layerType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );
        layoutParams.gravity = Gravity.TOP | Gravity.START;
        layoutParams.screenBrightness = -1;
        mIcon = inflater.inflate(R.layout.overlay_icon, null, false);
        mIcon.setVisibility(View.GONE);
        mIcon.setOnClickListener(this);
        mWindowManager.addView(mIcon, layoutParams);

        layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                0, 0, layerType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.TRANSLUCENT
        );
        mBackScreen = inflater.inflate(R.layout.overlay_screen, null, false);
        mBackScreen.setVisibility(View.GONE);
        mWindowManager.addView(mBackScreen, layoutParams);
        mBackScreen.setOnClickListener(this);

        layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                0, 0, layerType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );
        layoutParams.gravity = Gravity.TOP | Gravity.START;
        layoutParams.screenBrightness = -1;
        mNotification = inflater.inflate(R.layout.overlap_layer, null, false);
        mNotification.setVisibility(View.GONE);
        mNotification.setOnClickListener(this);
        mWindowManager.addView(mNotification, layoutParams);

        mPrediction = inflater.inflate(R.layout.overlay_guide, null, false);
        mPrediction.setVisibility(View.GONE);
        mPrediction.setOnClickListener(this);
        mWindowManager.addView(mPrediction, layoutParams);

        //mSpeed = (TextView)mPrediction.findViewById(R.id.textOverlaySpeed);
        mPredictionLine = (TextView)mPrediction.findViewById(R.id.textOverlayCurrentLine);
        mPredictionDistance = new TextView[3];
        mPredictionDistance[0] = (TextView)mPrediction.findViewById(R.id.textOverlayDistance1);
        mPredictionDistance[1] = (TextView)mPrediction.findViewById(R.id.textOverlayDistance2);
        mPredictionDistance[2] = (TextView)mPrediction.findViewById(R.id.textOverlayDistanceFade);
        mPredictionStation = new TextView[4];
        mPredictionStation[0] = (TextView)mPrediction.findViewById(R.id.textOverlayStationCurrent);
        mPredictionStation[1] = (TextView)mPrediction.findViewById(R.id.textOverlayStationNext1);
        mPredictionStation[2] = (TextView)mPrediction.findViewById(R.id.textOverlayStationNext2);
        mPredictionStation[3] = (TextView)mPrediction.findViewById(R.id.textOverlayStationFade);
        mPredictionMarker = new View[4];
        mPredictionMarker[3] = mPrediction.findViewById(R.id.viewStationMarkerFade);
        mPredictionMarker[0] = mPrediction.findViewById(R.id.viewStationMarkerCurrent);
        mPredictionMarker[1] = mPrediction.findViewById(R.id.viewStationMarkerNext1);
        mPredictionMarker[2] = mPrediction.findViewById(R.id.viewStationMarkerNext2);
        Resources resources = mService.getResources();
        mMarkerNormal = ResourcesCompat.getDrawable(resources, R.drawable.station_marker_normal, mService.getTheme());
        mMarkerAccent = ResourcesCompat.getDrawable(resources, R.drawable.station_marker_accent, mService.getTheme());


        //mPredictionTime = (TextView)mPrediction.findViewById(R.id.textOverlayRemainTime);
        mPredictionLineSelect = mPrediction.findViewById(R.id.buttonOverlaySelectLine);
        mPredictionLineSelect.setOnClickListener(this);
        mPredictionExit = (Button)mPrediction.findViewById(R.id.buttonOverlayStopPrediction);
        mPredictionExit.setOnClickListener(this);
        mPredictionContainer = mPrediction.findViewById(R.id.containerPredictionStations);
        mPredictionWait = mPrediction.findViewById(R.id.containerPredictionWait);
        mPredictionLayer = mPrediction.findViewById(R.id.layoutPredictionContent);

        mLayerContainer = mNotification.findViewById(R.id.layoutContent);
        mContentContainer = mNotification.findViewById(R.id.containerDetails);

        mStationName = (TextView)mNotification.findViewById(R.id.stationNameOverlay);
        mStationLines = (TextView)mNotification.findViewById(R.id.textStationLines);
        mStationDistance = (TextView)mNotification.findViewById(R.id.textStationDistance);
        mTimes = (TextView)mNotification.findViewById(R.id.textTime);
        mPrefecture = (TextView)mNotification.findViewById(R.id.textStationPrefecture);

        mNotification.setVisibility(View.GONE);
        mContentContainer.setVisibility(View.VISIBLE);


        mTimeNow = context.getString(R.string.notification_time_now);
        mTimeSec = context.getString(R.string.notification_time_sec);
        mTimeMin = context.getString(R.string.notification_time_min);
        mForceNotify = mService.isNotifyForce();
        mKeepNotification = mService.isKeepNotification();
        setBrightness(mService.getBrightness());
        mDisplayPrefecture = mService.isNotifyPrefecture();

        mMainHandler = mService.getMainHandler();

    }

    private Context mContext;
    private StationService mService;
    private WindowManager mWindowManager;
    private PowerManager mPowerManager;
    private View mIcon;
    private View mNotification;
    private View mBackScreen, mNightScreen;
    private View mPrediction;
    private Drawable mBlackDrawable;

    private View mContentContainer;
    private View mLayerContainer;

    private View mPredictionLayer;

    private TextView mStationDistance, mStationLines, mTimes;
    private TextView mStationName;
    private TextView mPrefecture;

    private Animation mAnimAppear, mAnimDisappear;
    private Animation mAnimOpen, mAnimClose;
    private Animation mAnimExpand, mAnimShrink;
    private Animation mRunningAnimation;
    private AnimatorSet mRunningAnimator;

    private TextView mPredictionLine;
    private TextView[] mPredictionDistance;
    private TextView[] mPredictionStation;
    private View[] mPredictionMarker;
    private Drawable mMarkerNormal, mMarkerAccent;
    private View mPredictionContainer, mPredictionWait;
    private Button mPredictionLineSelect;
    private Button mPredictionExit;

    private View mTimerScreen;
    private View mTimerContainer;
    private View mTimerButton;
    private int mTimerPosition = -1;
    private ObjectAnimator mTimerAnimator;
    private WindowManager.LayoutParams mTimerLayoutParams;

    private String mTimeNow, mTimeSec, mTimeMin;
    private long mDetectTime;
    private Station mDisplayedStation, mRequestedStation;

    private Handler mMainHandler;
    private Runnable mTimerCallback, mDurationCallback;
    private Timer mElapseTimer;

    private boolean mKeepNotification;
    private boolean mForceNotify;
    private boolean mScreen = true;
    private boolean mDisplayPrefecture;

    private Location mLastLocation;


    synchronized void requireNotification(@NonNull Station station){
        // called when new station detected, but screen may be turned on / off
        mDetectTime = System.currentTimeMillis();
        if ( mService.isLinePredictionRunning() ){
            mRequestedStation = station;
        }else{
            if ( mScreen ){
                onNotifyStation(station, !mKeepNotification);
            }else if ( mForceNotify ){
                //強制通知モード
                PowerManager.WakeLock wakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "station-found:");
                wakeLock.acquire(10);
                mBackScreen.setVisibility(View.VISIBLE);
                onNotifyStation(station, false);
            }else{
                mRequestedStation = station;
            }
        }
    }

    void setContext(Context context){
        this.mContext = context;
        mAnimAppear = AnimationUtils.loadAnimation(context, R.anim.anim_appear);
        mAnimDisappear = AnimationUtils.loadAnimation(context, R.anim.anim_disappear);
        mAnimOpen = AnimationUtils.loadAnimation(context, R.anim.anim_open);
        mAnimClose = AnimationUtils.loadAnimation(context, R.anim.anim_close);
        mAnimExpand = AnimationUtils.loadAnimation(context, R.anim.anim_expand);
        mAnimShrink = AnimationUtils.loadAnimation(context, R.anim.anim_shrink);

    }


    void release(){
        mWindowManager.removeView(mIcon);
        mWindowManager.removeView(mBackScreen);
        mWindowManager.removeView(mNotification);
        mWindowManager.removeView(mPrediction);
        setNightMood(false);
        setFixedTimer(false, true);
        mWindowManager = null;
        mPowerManager = null;
        mIcon.setOnClickListener(null);
        mNotification.setOnClickListener(null);
        mBackScreen.setOnClickListener(null);
        mNotification = null;
        mPrediction.setOnClickListener(null);
        mPrediction = null;
        mPredictionLineSelect.setOnClickListener(null);
        mPredictionExit.setOnClickListener(null);
        mPredictionExit = null;
        if ( mElapseTimer != null ) mElapseTimer.cancel();
        if ( mDurationCallback != null ){
            mMainHandler.removeCallbacks(mDurationCallback);
            mDurationCallback = null;
        }

        if ( mRunningAnimator != null ){
            mRunningAnimator.cancel();
            mRunningAnimator = null;
        }
        mAnimAppear.cancel();
        mAnimClose.cancel();
        mAnimDisappear.cancel();
        mAnimExpand.cancel();
        mAnimShrink.cancel();
        mAnimOpen.cancel();

        if ( mTimerPosition >= 0 ){
            SharedPreferences.Editor editor = mService.getSharedPreferences(mService.getString(R.string.preference_name), MODE_PRIVATE).edit();
            editor.putInt(KEY_TIMER_ICON_POSITION, mTimerPosition);
            editor.apply();
        }

        mContext = null;
        mService = null;


    }

    void setNotifyMode(boolean force, boolean keep){
        mForceNotify = force;

        boolean old = mKeepNotification;
        mKeepNotification = keep;
        if ( !old && keep ){
            if ( mService.isRunning() && mService.hasExplorerInitialized() ){
                mDisplayedStation = mService.getCurrentStation();
                onNotifyStation(mDisplayedStation, false);
            }
        }else if ( old && !keep ){
            onNotificationRemoved(null);
        }
    }

    void setDisplayPrefecture(boolean value){
        if ( mDisplayPrefecture != value ){
            mDisplayPrefecture = value;
            if ( mDisplayedStation != null ){
                invalidatePrefecture(mDisplayedStation);
            }
        }
    }

    void setBrightness(int value){
        mBlackDrawable = new ColorDrawable((255 - value) << 24);
        if ( mNightScreen == null ){
            mBackScreen.setBackground(mBlackDrawable);
        }else{
            mBackScreen.setBackground(new ColorDrawable(0x00000000));
            mNightScreen.setBackground(mBlackDrawable);
        }
    }

    void setNightMood(boolean enable){
        if ( enable ){
            if ( mNightScreen == null ){
                //initialize view
                int layerType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
                LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                mNightScreen = inflater.inflate(R.layout.overlay_screen, null);
                WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT,
                        0, 0, layerType,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                        PixelFormat.TRANSLUCENT
                );
                mNightScreen.setBackground(mBlackDrawable);
                mBackScreen.setBackground(new ColorDrawable(0x00000000));
                mWindowManager.addView(mNightScreen, params);
                mNightScreen.setVisibility(View.VISIBLE);
            }
        }else{
            if ( mNightScreen != null ){
                mWindowManager.removeView(mNightScreen);
                mNightScreen = null;
                mBackScreen.setBackground(mBlackDrawable);
            }
        }
    }

    void toggleFixedTimer(){
        if ( mTimerAnimator != null ) return;
        if ( mTimerScreen == null ){
            setFixedTimer(true, false);
        }else if ( mTimerContainer.getVisibility() == View.VISIBLE ){
            setFixedTimer(false, false);
        }
    }

    void onTimerStateChanged(boolean running){
        toggleTimerIcon(!running, null);
    }

    private void setFixedTimer(boolean enable, boolean immediately){
        if ( mTimerAnimator != null ){
            if ( immediately ){
                mTimerAnimator.cancel();
                mTimerAnimator = null;
            }else{
                return;
            }
        }
        if ( enable ){
            if ( mTimerScreen == null ){
                int layerType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
                LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                mTimerScreen = inflater.inflate(R.layout.overlay_timer, null);
                if ( mTimerPosition < 0 ){
                    SharedPreferences preference = mService.getSharedPreferences(mService.getString(R.string.preference_name), MODE_PRIVATE);
                    mTimerPosition = preference.getInt(KEY_TIMER_ICON_POSITION, -1);
                    if ( mTimerPosition < 0 ){
                        Display display = mWindowManager.getDefaultDisplay();
                        Point size = new Point();
                        display.getSize(size);
                        mTimerPosition = size.y - mService.getResources().getDimensionPixelSize(R.dimen.timer_icon_offset_default);
                    }
                }

                mTimerLayoutParams = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        0, mTimerPosition, layerType,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                        PixelFormat.TRANSLUCENT
                );
                mTimerLayoutParams.gravity = Gravity.END | Gravity.TOP;
                mTimerContainer = mTimerScreen.findViewById(R.id.timerIconContainer);
                mTimerButton = mTimerScreen.findViewById(R.id.buttonTimer);
                mTimerButton.setOnClickListener(this);
                mTimerButton.setOnLongClickListener(this);
                mTimerButton.setOnTouchListener(this);
                //mTimerIcon.addOnLayoutChangeListener(this);
                mWindowManager.addView(mTimerScreen, mTimerLayoutParams);
                toggleTimerIcon(true, null);

            }
        }else{
            if ( mTimerScreen != null ){
                if ( immediately || mTimerContainer.getVisibility() != View.VISIBLE ){
                    mWindowManager.removeView(mTimerScreen);
                    mTimerButton.setOnClickListener(null);
                    mTimerButton.setOnLongClickListener(null);
                    mTimerButton.setOnTouchListener(null);
                    mTimerScreen = null;
                    mTimerContainer = null;
                    mTimerButton = null;
                    if ( mTimerCallback != null ){
                        mMainHandler.removeCallbacks(mTimerCallback);
                        mTimerCallback = null;
                    }

                }else{
                    toggleTimerIcon(false, new Runnable(){
                        @Override
                        public void run(){
                            setFixedTimer(false, true);
                        }
                    });
                }
            }

        }
    }

    private boolean mIsTimerButtonClicked;
    private float mTouchY;

    @Override
    public boolean onLongClick(View v){
        if ( mTimerButton != null && v == mTimerButton ){
            mIsTimerButtonClicked = true;
            mTimerPosition = mTimerLayoutParams.y;
        }
        return false;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event){
        if ( mTimerButton != null && v == mTimerButton ){
            switch ( event.getActionMasked() ){
                case MotionEvent.ACTION_DOWN:
                    mIsTimerButtonClicked = false;
                case MotionEvent.ACTION_MOVE:
                    float y = event.getRawY();
                    if ( mIsTimerButtonClicked ){
                        mTimerLayoutParams.y = mTimerPosition + (int)(y - mTouchY);
                        mWindowManager.updateViewLayout(mTimerScreen, mTimerLayoutParams);
                    }else{
                        mTouchY = y;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_HOVER_EXIT:
                    mTimerPosition = mTimerLayoutParams.y;
                    break;
                default:
                    break;

            }
        }
        return v.onTouchEvent(event);
    }

    private void toggleTimerIcon(final boolean show, final Runnable callback){
        if ( mTimerContainer == null ) return;
        if ( show == (mTimerContainer.getVisibility() == View.VISIBLE) ) return;
        final int width = mService.getResources().getDimensionPixelSize(R.dimen.timer_icon_width);
        ObjectAnimator animator = ObjectAnimator.ofFloat(
                mTimerContainer,
                "translationX",
                show ? width : 0,
                show ? 0 : width
        );
        animator.setDuration(300);
        animator.addListener(new Animator.AnimatorListener(){
            @Override
            public void onAnimationStart(Animator animation){
            }

            @Override
            public void onAnimationEnd(Animator animation){
                if ( !show ) mTimerContainer.setVisibility(View.GONE);
                animation.removeAllListeners();
                mTimerAnimator = null;
                if ( callback != null ) callback.run();
            }

            @Override
            public void onAnimationCancel(Animator animation){

            }

            @Override
            public void onAnimationRepeat(Animator animation){

            }
        });
        if ( show ) mTimerContainer.setVisibility(View.VISIBLE);
        mTimerAnimator = animator;
        animator.start();
    }

    @Override
    public void onClick(View v){
        if ( mService == null ) return;
        if ( v == mBackScreen ){
            mBackScreen.setVisibility(View.GONE);
            onNotificationRemoved(null);
        }else if ( v == mNotification ){
            if ( mKeepNotification ){
                toggleNotification();
            }else{
                onNotificationRemoved(null);
            }
        }else if ( v == mIcon ){
            if ( mService.isLinePredictionRunning() ){
                togglePrediction();
            }else{
                if ( mKeepNotification ){
                    toggleNotification();
                }else{
                    onNotificationRemoved(null);
                }
            }
        }else if ( v == mPrediction ){
            togglePrediction();
        }else if ( v == mPredictionLineSelect ){
            Intent intent = new Intent(mService, MainActivity.class)
                    .putExtra(MainActivity.KEY_PREDICTION_LINE, true);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            mService.startActivity(intent);
        }else if ( v == mPredictionExit ){
            mService.stopLinePrediction(true);
        }else if ( v == mTimerButton ){
            if ( mIsTimerButtonClicked ) return;

            mService.setTimer();
        }
    }


    private void toggleNotification(){
        if ( mNotification.getVisibility() == View.VISIBLE ){
            mAnimClose.setAnimationListener(new Animation.AnimationListener(){
                @Override
                public void onAnimationStart(Animation animation){

                }

                @Override
                public void onAnimationEnd(Animation animation){
                    mNotification.setVisibility(View.GONE);
                    animation.setAnimationListener(null);
                }

                @Override
                public void onAnimationRepeat(Animation animation){

                }
            });
            mContentContainer.startAnimation(mAnimClose);
            mIcon.setVisibility(View.VISIBLE);
        }else{
            mNotification.setVisibility(View.VISIBLE);
            mAnimOpen.setAnimationListener(new Animation.AnimationListener(){
                @Override
                public void onAnimationStart(Animation animation){
                    mIcon.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationEnd(Animation animation){
                    animation.setAnimationListener(null);
                }

                @Override
                public void onAnimationRepeat(Animation animation){

                }
            });
            mContentContainer.setVisibility(View.VISIBLE);
            mContentContainer.startAnimation(mAnimOpen);
        }
    }

    void onLocationUpdate(Location location){
        if ( location == null ) return;
        mLastLocation = location;

        if ( mDisplayedStation != null && mStationDistance.getVisibility() == View.VISIBLE ){
            double distance = mService.mDisplayRuler.measureDistance(mDisplayedStation, mLastLocation.getLongitude(), mLastLocation.getLatitude());
            // update contents of views shown on the screen, if any
            mStationDistance.setText(DistanceRuler.formatDistance(distance));
        }
    }

    private void invalidatePrefecture(@NonNull Station station){
        final boolean showPrefecture = mDisplayPrefecture;
        if ( showPrefecture != (mPrefecture.getVisibility() == View.VISIBLE) ){
            mPrefecture.setVisibility(showPrefecture ? View.VISIBLE : View.GONE);
        }
        if ( showPrefecture ){
            mPrefecture.setText(mService.getPrefecture(station.prefecture));
        }
    }


    private void onNotifyStation(@NonNull final Station station, boolean timer){
        // show notification-typed view overlaid at the top of screen
        mDisplayedStation = station;


        // invalidate contents of views
        invalidatePrefecture(station);
        mStationName.setText(station.name);
        final String lines = mDisplayedStation.getLinesName();
        mStationLines.setText(lines);
        onLocationUpdate(mLastLocation);

        // animation attached
        mContentContainer.setVisibility(View.VISIBLE);
        mNotification.setVisibility(View.VISIBLE);
        mLayerContainer.setVisibility(View.VISIBLE);
        mIcon.setVisibility(View.GONE);
        //if ( mStationary ){
        //    mContentContainer.startAnimation(mAnimOpen);
        //}else{
        mLayerContainer.startAnimation(mAnimAppear);
        //}
        if ( mElapseTimer != null ){
            mElapseTimer.cancel();
        }
        mElapseTimer = new Timer();
        mElapseTimer.schedule(new TimeMessageTask(), 0, 1000);

        if ( timer ){
            //通常モード　画面on時にオーバーレイ
            if ( mDurationCallback != null ){
                mMainHandler.removeCallbacks(mDurationCallback);
            }
            mDurationCallback = new Runnable(){
                @Override
                public void run(){
                    mDurationCallback = null;
                    onNotificationRemoved(station);
                }
            };
            mMainHandler.postDelayed(mDurationCallback, 5000);

        }
    }

    synchronized void onNotificationRemoved(@Nullable Station station){
        // removing notification may be required after
        // (1) notification has gone yet
        // (2) notification for another station is now on screen
        // because called from timer
        if ( mDisplayedStation == null || (station != null && !mDisplayedStation.equals(station)) ){
            return;
        }
        if ( mNotification.getVisibility() != View.VISIBLE && mIcon.getVisibility() != View.VISIBLE )
            return;


        if ( mNotification.getVisibility() == View.VISIBLE ){
            mAnimDisappear.setAnimationListener(new Animation.AnimationListener(){
                @Override
                public void onAnimationStart(Animation animation){

                }

                @Override
                public void onAnimationEnd(Animation animation){
                    if ( mNotification != null ){
                        mNotification.setVisibility(View.GONE);
                        animation.setAnimationListener(null);
                    }
                }

                @Override
                public void onAnimationRepeat(Animation animation){

                }
            });
            mLayerContainer.startAnimation(mAnimDisappear);
        }else{
            mIcon.setVisibility(View.GONE);
        }
        mDisplayedStation = null;
        mElapseTimer.cancel();
        mElapseTimer = null;
    }


    void onPredictionStart(@NonNull Line line){
        // show notification for prediction
        if ( mRunningAnimation != null ){
            mRunningAnimation.cancel();
        } else if ( mPrediction.getVisibility() == View.VISIBLE ){
            return;
        }
        mNotification.setVisibility(View.GONE);
        mIcon.setVisibility(View.GONE);

        mPredictionLine.setText(line.mName);
        mPredictionDistance[0].setText("");
        mPredictionDistance[1].setText("");
        mPredictionDistance[2].setText("");
        mPredictionStation[0].setText(mService.getCurrentStation().name);
        mPredictionStation[1].setText("");
        mPredictionStation[2].setText("");
        mPredictionStation[3].setText("");


        mPredictionWait.setVisibility(View.VISIBLE);
        mPredictionContainer.setVisibility(View.INVISIBLE);
        mPrediction.setVisibility(View.VISIBLE);
        mAnimAppear.setAnimationListener(new Animation.AnimationListener(){
            @Override
            public void onAnimationStart(Animation animation){

            }

            @Override
            public void onAnimationEnd(Animation animation){
                mRunningAnimation = null;
                animation.setAnimationListener(null);
            }

            @Override
            public void onAnimationRepeat(Animation animation){

            }
        });
        mPredictionLayer.startAnimation(mAnimAppear);
        mRunningAnimation = mAnimAppear;
        mDisplayedStation = mService.getCurrentStation();
    }

    void onPredictionStop(){
        if ( mPredictionLayer.getVisibility() != View.VISIBLE ) return;
        if ( mRunningAnimation != null ){
            mRunningAnimation.cancel();
        }

        mAnimDisappear.setAnimationListener(new Animation.AnimationListener(){
            @Override
            public void onAnimationStart(Animation animation){

            }

            @Override
            public void onAnimationEnd(Animation animation){
                if ( mPrediction != null ){
                    mPrediction.setVisibility(View.GONE);
                    animation.setAnimationListener(null);
                }
                mRunningAnimation = null;
            }

            @Override
            public void onAnimationRepeat(Animation animation){

            }
        });
        mPredictionLayer.startAnimation(mAnimDisappear);
        mRunningAnimation = mAnimDisappear;
    }

    private void togglePrediction(){
        if ( mPrediction.getVisibility() == View.VISIBLE ){
            mAnimShrink.setAnimationListener(new Animation.AnimationListener(){
                @Override
                public void onAnimationStart(Animation animation){

                }

                @Override
                public void onAnimationEnd(Animation animation){
                    if ( mPrediction != null ){
                        mPrediction.setVisibility(View.GONE);
                        animation.setAnimationListener(null);
                    }
                }

                @Override
                public void onAnimationRepeat(Animation animation){

                }
            });
            mPredictionLayer.startAnimation(mAnimShrink);
            mIcon.setVisibility(View.VISIBLE);
        }else{
            mAnimExpand.setAnimationListener(new Animation.AnimationListener(){
                @Override
                public void onAnimationStart(Animation animation){
                }

                @Override
                public void onAnimationEnd(Animation animation){
                    if ( mPrediction != null ){
                        mIcon.setVisibility(View.GONE);
                        animation.setAnimationListener(null);
                    }

                }

                @Override
                public void onAnimationRepeat(Animation animation){

                }
            });
            mPrediction.setVisibility(View.VISIBLE);
            mPredictionLayer.startAnimation(mAnimExpand);
        }
    }


    private void setTimeMessage(){
        int time = (int)((System.currentTimeMillis() - mDetectTime) / 1000);
        String timeMes = time < 10 ? mTimeNow : (time < 60 ? mTimeSec : String.valueOf(time / 60) + mTimeMin);
        mTimes.setText(timeMes);
    }


    private void invalidatePrediction(PositionPredictor.PredictionResult result){
        if ( mService.hasExplorerInitialized() ){
            mPredictionStation[0].setText(mService.getCurrentStation().name);
        }else{
            mPredictionStation[0].setText("");
        }
        if ( result.size > 0 ){
            mPredictionStation[1].setText(result.getStation(0).name);
            mPredictionDistance[0].setText(DistanceRuler.formatDistance(result.getDistance(0)));
        }else{
            mPredictionStation[1].setText("");
            mPredictionDistance[0].setText("");
        }
        if ( result.size > 1 ){
            mPredictionStation[2].setText(result.getStation(1).name);
            mPredictionDistance[1].setText(DistanceRuler.formatDistance(result.getDistance(1)));
        }else{
            mPredictionStation[2].setText("");
            mPredictionDistance[1].setText("");
        }
    }

    @Override
    public void onApproachStations(final @NonNull PositionPredictor.PredictionResult result){
        if ( mPredictionWait.getVisibility() == View.VISIBLE && result.size > 0 ){
            mPredictionWait.setVisibility(View.GONE);
            mPredictionContainer.setVisibility(View.VISIBLE);
        }else if ( mPredictionWait.getVisibility() != View.VISIBLE && result.size == 0 ){
            mPredictionWait.setVisibility(View.VISIBLE);
            mPredictionContainer.setVisibility(View.INVISIBLE);
        }
        if ( mRequestedStation == null ){
            invalidatePrediction(result);
        }else{
            final Station station = mRequestedStation;
            mDisplayedStation = station;
            mRequestedStation = null;
            if ( !mPredictionStation[1].getText().equals(station.name) || mPrediction.getVisibility() != View.VISIBLE ){
                invalidatePrediction(result);
                return;
            }
            final float[] distanceX = new float[]{
                    mPredictionDistance[0].getX(),
                    mPredictionDistance[1].getX(),
                    mPredictionDistance[2].getX()
            };
            final float[] distanceY = new float[]{
                    mPredictionDistance[0].getY(),
                    mPredictionDistance[1].getY(),
                    mPredictionDistance[2].getY()
            };
            final float[] nameX = new float[]{
                    mPredictionStation[0].getX(),
                    mPredictionStation[1].getX(),
                    mPredictionStation[2].getX(),
                    mPredictionStation[3].getX()
            };
            final float[] nameY = new float[]{
                    mPredictionStation[0].getY(),
                    mPredictionStation[1].getY(),
                    mPredictionStation[2].getY(),
                    mPredictionStation[3].getY()
            };
            final float[] markerX = new float[]{
                    mPredictionMarker[0].getX(),
                    mPredictionMarker[1].getX(),
                    mPredictionMarker[2].getX(),
                    mPredictionMarker[3].getX()
            };
            final float[] markerY = new float[]{
                    mPredictionMarker[0].getY(),
                    mPredictionMarker[1].getY(),
                    mPredictionMarker[2].getY(),
                    mPredictionMarker[3].getY()
            };
            final float[] scale = new float[]{
                    1f,
                    (float)mPredictionMarker[0].getWidth() / mPredictionMarker[1].getWidth(),
                    (float)mPredictionMarker[1].getWidth() / mPredictionMarker[2].getWidth(),
                    (float)mPredictionMarker[2].getWidth() / mPredictionMarker[3].getWidth()
            };

            AnimatorSet animatorSet = new AnimatorSet();
            if ( result.size > 1 ){
                mPredictionDistance[2].setText(DistanceRuler.formatDistance(result.getDistance(1)));
                mPredictionStation[3].setText(result.getStation(1).name);
            }
            mPredictionDistance[0].setPivotX(0);
            mPredictionDistance[0].setPivotY(mPredictionDistance[0].getHeight());
            mPredictionDistance[2].setPivotX(mPredictionDistance[2].getWidth());
            mPredictionDistance[2].setPivotY(0);
            mPredictionStation[0].setPivotX(0);
            mPredictionStation[0].setPivotY(mPredictionStation[0].getHeight());
            mPredictionStation[3].setPivotX(mPredictionStation[3].getWidth());
            mPredictionStation[3].setPivotY(0);
            animatorSet.playTogether(
                    ObjectAnimator.ofPropertyValuesHolder(
                            mPredictionDistance[0],
                            PropertyValuesHolder.ofFloat("scaleY", 1f, 0.5f),
                            PropertyValuesHolder.ofFloat("scaleX", 1f, 0.5f),
                            PropertyValuesHolder.ofFloat("alpha", 1f, 0.2f)
                    ),
                    ObjectAnimator.ofPropertyValuesHolder(
                            mPredictionDistance[1],
                            PropertyValuesHolder.ofFloat("translationX", 0f, distanceX[0] - distanceX[1]),
                            PropertyValuesHolder.ofFloat("translationY", 0f, distanceY[0] - distanceY[1])
                    ),
                    ObjectAnimator.ofPropertyValuesHolder(
                            mPredictionDistance[2],
                            PropertyValuesHolder.ofFloat("scaleY", 0.5f, 1f),
                            PropertyValuesHolder.ofFloat("scaleX", 0.5f, 1f),
                            PropertyValuesHolder.ofFloat("alpha", 0.2f, 1f)
                    ),
                    ObjectAnimator.ofPropertyValuesHolder(
                            mPredictionStation[0],
                            PropertyValuesHolder.ofFloat("scaleY", 1f, 0.5f),
                            PropertyValuesHolder.ofFloat("scaleX", 1f, 0.5f),
                            PropertyValuesHolder.ofFloat("alpha", 1f, 0.2f)
                    ),
                    ObjectAnimator.ofPropertyValuesHolder(
                            mPredictionStation[1],
                            PropertyValuesHolder.ofFloat("translationX", 0f, nameX[0] - nameX[1]),
                            PropertyValuesHolder.ofFloat("translationY", 0f, nameY[0] - nameY[1])
                    ),
                    ObjectAnimator.ofPropertyValuesHolder(
                            mPredictionStation[2],
                            PropertyValuesHolder.ofFloat("translationX", 0f, nameX[1] - nameX[2]),
                            PropertyValuesHolder.ofFloat("translationY", 0f, nameY[1] - nameY[2])
                    ),
                    ObjectAnimator.ofPropertyValuesHolder(
                            mPredictionStation[3],
                            PropertyValuesHolder.ofFloat("scaleY", 0.5f, 1f),
                            PropertyValuesHolder.ofFloat("scaleX", 0.5f, 1f),
                            PropertyValuesHolder.ofFloat("alpha", 0.2f, 1f)
                    ),
                    ObjectAnimator.ofPropertyValuesHolder(
                            mPredictionMarker[3],
                            PropertyValuesHolder.ofFloat("translationX", 0f, markerX[2] - markerX[3]),
                            PropertyValuesHolder.ofFloat("translationY", 0f, markerY[2] - markerY[3]),
                            PropertyValuesHolder.ofFloat("scaleX", 0.1f, scale[3]),
                            PropertyValuesHolder.ofFloat("scaleY", 0.1f, scale[3]),
                            PropertyValuesHolder.ofFloat("alpha", 0f, 1f)
                    ),
                    ObjectAnimator.ofPropertyValuesHolder(
                            mPredictionMarker[2],
                            PropertyValuesHolder.ofFloat("translationX", 0f, markerX[1] - markerX[2]),
                            PropertyValuesHolder.ofFloat("translationY", 0f, markerY[1] - markerY[2]),
                            PropertyValuesHolder.ofFloat("scaleX", 1f, scale[2]),
                            PropertyValuesHolder.ofFloat("scaleY", 1f, scale[2])
                    ),
                    ObjectAnimator.ofPropertyValuesHolder(
                            mPredictionMarker[1],
                            PropertyValuesHolder.ofFloat("translationX", 0f, markerX[0] - markerX[1]),
                            PropertyValuesHolder.ofFloat("translationY", 0f, markerY[0] - markerY[1]),
                            PropertyValuesHolder.ofFloat("scaleX", 1f, scale[1]),
                            PropertyValuesHolder.ofFloat("scaleY", 1f, scale[1])
                    ),
                    ObjectAnimator.ofPropertyValuesHolder(
                            mPredictionMarker[0],
                            PropertyValuesHolder.ofFloat("translationX", 0f, markerX[0] - markerX[1]),
                            PropertyValuesHolder.ofFloat("translationY", 0f, markerY[0] - markerY[1])
                    )
            );
            animatorSet.setDuration(500);
            animatorSet.addListener(new Animator.AnimatorListener(){
                @Override
                public void onAnimationStart(Animator animation){

                }

                @Override
                public void onAnimationEnd(Animator animation){
                    animation.removeAllListeners();
                    for ( int i = 0; i < 3; i++ ){
                        View view = mPredictionDistance[i];
                        view.setY(distanceY[i]);
                        view.setX(distanceX[i]);
                        view.setAlpha(1f);
                        view.setScaleX(1f);
                        view.setScaleY(1f);
                    }
                    for ( int i = 0; i < 4; i++ ){
                        View view = mPredictionStation[i];
                        view.setY(nameY[i]);
                        view.setX(nameX[i]);
                        view.setAlpha(1f);
                        view.setScaleX(1f);
                        view.setScaleY(1f);
                    }
                    for ( int i = 0; i < 4; i++ ){
                        View view = mPredictionMarker[i];
                        view.setY(markerY[i]);
                        view.setX(markerX[i]);
                        view.setAlpha(1f);
                        view.setScaleX(1f);
                        view.setScaleY(1f);
                    }
                    mPredictionDistance[2].setVisibility(View.INVISIBLE);
                    mPredictionStation[3].setVisibility(View.INVISIBLE);
                    mPredictionMarker[3].setVisibility(View.INVISIBLE);
                    invalidatePrediction(result);
                    mRunningAnimator = null;
                }

                @Override
                public void onAnimationCancel(Animator animation){

                }

                @Override
                public void onAnimationRepeat(Animator animation){

                }
            });
            animatorSet.start();
            mRunningAnimator = animatorSet;
        }
    }


    private class TimeMessageTask extends TimerTask{

        @Override
        public void run(){
            mTimes.post(new Runnable(){
                @Override
                public void run(){
                    setTimeMessage();
                }
            });
        }
    }

    @Override
    public void onReceive(Context context, Intent intent){
        String action = intent.getAction();
        if ( action != null ){
            synchronized( this ){
                if ( action.equals(Intent.ACTION_SCREEN_ON) ){
                    mScreen = true;
                    if ( mRequestedStation != null ){
                        final Station station = mRequestedStation;
                        mRequestedStation = null;
                        onNotifyStation(station, !mKeepNotification);
                    }
                }else if ( action.equals(Intent.ACTION_SCREEN_OFF) ){
                    mScreen = false;
                }
            }
        }
    }


}
