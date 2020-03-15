package jp.ac.u_tokyo.t.seo.station.app;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.Resources;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.android.gms.common.api.ResolvableApiException;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import jp.ac.u_tokyo.t.seo.customdialog.CompatFragment;
import jp.ac.u_tokyo.t.seo.customview.HorizontalListView;


/**
 * @author Seo-4d696b75
 * @version 2018/04/25.
 */

public class MainFragment extends CompatFragment implements
        StationService.StationCallback, RadarFragment.RadarFragmentCallback, 
        StationFragment.StationFragmentCallback, LineFragment.LineFragmentCallback,
        View.OnLayoutChangeListener{

    private MainActivity mContext;
    private StationService mService;

    private boolean mHasCreated = false;

    static final String TRANSACTION_TAG_INITIAL = "initial_transaction";
    private final String KEY_FRAGMENT_SHOWN = "details_fragment_shown";
    private boolean mIsDetailsFragmentShown;

    private ToggleButton mToggleStart;
    private FloatingButton mButtonFin, mButtonMenu;
    private FloatingButton mButtonMap;
    private FloatingButton mButtonLineSelect, mButtonLineCancel, mButtonLinePredict;
    private FloatingButton mButtonTimer, mButtonTimerFixed;
    private View mButtonContainer;
    private View mButtonScreen;
    private int mButtonWidth, mButtonBigWidth;
    private int mButtonContainerWidth, mButtonContainerHeight;

    private View mMainContainer;
    private View mStationWaitMessage;
    private View mStationSearchMessage;
    private StationNameView mStationName;
    private HorizontalListView mListStationLines;
    private TextView mTextStationPrefecture;
    private TextView mTextDistance;
    private TextView mTextSelectedLine;
    private AnimationView mAnimationView;

    private LineAdapter mAdapter;

    private AnimatorSet mRunningAnimator;

    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        if ( context instanceof MainActivity ){
            mContext = (MainActivity)context;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state){
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(final View view, final Bundle state){

        // When activity is re-created, service may be not connected yet
        mContext.getService(new MainActivity.OnServiceConnectedListener(){
            @Override
            public void onServiceConnected(StationService service){
                mService = service;
                init(view, state);
            }
        });


    }

    private void init(View view, Bundle state){

        if ( mService == null ){
            Log.e("MainFragment", "service not found");
            return;
        }


        mToggleStart = (ToggleButton)view.findViewById(R.id.toggleStart);
        Resources res = getResources();
        mButtonFin = new FloatingButton(
                (Button)view.findViewById(R.id.buttonFinish),
                res.getDimensionPixelSize(R.dimen.float_button_line_select_x),
                res.getDimensionPixelSize(R.dimen.float_button_line_select_y)
        );
        mButtonLineSelect = new FloatingButton(
                (Button)view.findViewById(R.id.buttonLineSelect),
                res.getDimensionPixelSize(R.dimen.float_button_line_select_x),
                res.getDimensionPixelSize(R.dimen.float_button_line_select_y)
        );
        mButtonMap = new FloatingButton(
                (Button)view.findViewById(R.id.buttonMap),
                res.getDimensionPixelSize(R.dimen.float_button_map_x),
                res.getDimensionPixelSize(R.dimen.float_button_map_y)
        );
        mButtonLinePredict = new FloatingButton(
                (Button)view.findViewById(R.id.buttonLinePredict),
                res.getDimensionPixelSize(R.dimen.float_button_line_predict_x),
                res.getDimensionPixelSize(R.dimen.float_button_line_predict_y)
        );
        mStationName = (StationNameView)view.findViewById(R.id.stationNameMain);
        mButtonLineCancel = new FloatingButton(
                (Button)view.findViewById(R.id.buttonLineCancel),
                res.getDimensionPixelSize(R.dimen.float_button_line_cancel_x),
                res.getDimensionPixelSize(R.dimen.float_button_line_cancel_y)
        );

        mListStationLines = (HorizontalListView)view.findViewById(R.id.listStationLines);
        LinearLayoutManager layoutManager = new LinearLayoutManager(mContext.getApplication());
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mListStationLines.setLayoutManager(layoutManager);
        mTextDistance = (TextView)view.findViewById(R.id.textDistance);
        mTextSelectedLine = (TextView)view.findViewById(R.id.textSelectedLine);
        mTextStationPrefecture = (TextView)view.findViewById(R.id.textStationPrefecture);
        mAnimationView = (AnimationView)view.findViewById(R.id.animationView);
        mAnimationView.runAnimation(mService.isRunning());

        mMainContainer = view.findViewById(R.id.containerStation);
        mStationWaitMessage = view.findViewById(R.id.stationWaitMessage);
        mStationSearchMessage = view.findViewById(R.id.stationSearchMessage);

        mToggleStart.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){
                if ( mService.isRunning() != isChecked ){
                    if ( isChecked ){
                        mStationWaitMessage.setVisibility(View.GONE);
                        mStationSearchMessage.setVisibility(View.VISIBLE);
                        mAnimationView.runAnimation(true);
                        mService.start();
                    }else{
                        mAnimationView.runAnimation(false);
                        animationButton(false);
                        mService.stop();
                    }
                }
                animationButton(false);
            }
        });
        mButtonTimer = new FloatingButton(
                (Button)view.findViewById(R.id.buttonTimer),
                res.getDimensionPixelSize(R.dimen.float_button_timer_x),
                res.getDimensionPixelSize(R.dimen.float_button_timer_y)
        );
        mButtonTimerFixed = new FloatingButton(
                (Button)view.findViewById(R.id.buttonTimerFixed),
                res.getDimensionPixelSize(R.dimen.float_button_timer_fixed_x),
                res.getDimensionPixelSize(R.dimen.float_button_timer_fixed_y)
        );
        mButtonMenu = new FloatingButton(
                (Button)view.findViewById(R.id.buttonMenu),
                res.getDimensionPixelSize(R.dimen.float_button_line_select_x),
                res.getDimensionPixelSize(R.dimen.float_button_line_select_y)
        );
        mButtonScreen = view.findViewById(R.id.floatButtonScreen);
        mButtonScreen.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event){
                if ( v == mButtonScreen && event.getActionMasked() == MotionEvent.ACTION_DOWN ){
                    v.setVisibility(View.GONE);
                    animationButton(false);
                }
                return false;
            }
        });
        
        mButtonContainer = view.findViewById(R.id.floatButtonContainer);
        mButtonContainerHeight = mButtonContainer.getHeight();
        mButtonContainerWidth = mButtonContainer.getWidth();
        if ( mButtonContainerWidth * mButtonContainerHeight == 0 ){
            mButtonContainer.addOnLayoutChangeListener(this);
        }
        
        
        mButtonFin.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                mContext.onFinishApp();
            }
        });
        mButtonLineSelect.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if ( mService != null && mService.isRunning() ){
                    DialogFragment dialog = LineDialog.getInstance(false);
                    dialog.show(getCompatFragmentManager(), "line_select");
                }
                animationButton(false);
            }
        });
        mButtonMap.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if ( mContext != null ) mContext.requestShowMap();
                animationButton(false);
            }
        });
        mButtonLineCancel.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if ( mService != null ){
                    mService.setCurrentLine(null, true);
                }
                animationButton(false);
            }
        });
        mButtonLinePredict.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if ( mService != null ){
                    if ( mService.isLinePredictionRunning() ){
                        mService.stopLinePrediction(true);
                    }else{
                        if ( mService.getCurrentLine() == null ){
                            DialogFragment dialog = LineDialog.getInstance(true);
                            dialog.show(getCompatFragmentManager(), "line_predict");
                        }else{
                            mService.startLinePrediction();
                        }
                    }
                }
                animationButton(false);
            }
        });
        mButtonTimer.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if ( mService != null ){
                    mService.setTimer();
                }
                animationButton(false);
            }
        });
        mButtonTimerFixed.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if ( mService != null ) mService.toggleFixedTimer();
                animationButton(false);
            }
        });
        mButtonMenu.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                animationButton(true);
            }
        });

        if ( !mHasCreated ){
            // back stack からpopした場合などは再度呼ばれることがあるので一度のみ実行するように制限
            mHasCreated = true;
            if ( state == null ){
                RadarFragment fragment = RadarFragment.getInstance();
                mIsDetailsFragmentShown = false;
                FragmentManager manager = getCompatFragmentManager();
                FragmentTransaction transaction = manager.beginTransaction();
                transaction.add(R.id.containerSubFragment, fragment, RadarFragment.FRAGMENT_TAG);
                transaction.commit();
            }else{
                mIsDetailsFragmentShown = state.getBoolean(KEY_FRAGMENT_SHOWN);
            }
        }

        mToggleStart.setChecked(mService.isRunning());
        mButtonWidth = getResources().getDimensionPixelSize(R.dimen.float_button_width);
        mButtonBigWidth = getResources().getDimensionPixelSize(R.dimen.float_button_big_width);
        invalidateButton();

        final boolean running = mService.isRunning();
        final boolean searching = !mService.hasExplorerInitialized();
        mMainContainer.setVisibility(running && !searching ? View.VISIBLE : View.INVISIBLE);
        mStationWaitMessage.setVisibility(running ? View.GONE : View.VISIBLE);
        mStationSearchMessage.setVisibility(running && searching ? View.VISIBLE : View.GONE);

        if ( mService.hasExplorerInitialized() ){
            updateInfo(mService.getLongitude(), mService.getLatitude(), mService.getCurrentLine(), mService.getCurrentStation());
        }
        mService.setCallback(this);


    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_FRAGMENT_SHOWN, mIsDetailsFragmentShown);
    }

    @Override
    public void onResume(){
        super.onResume();
        invalidateButton();
    }

    @Override
    public void onPause(){
        super.onPause();
    }

    @Override
    public void onDestroyView(){
        super.onDestroyView();

        if ( mRunningAnimator != null ){
            mRunningAnimator.cancel();
            mRunningAnimator = null;
        }
        if ( mService != null ) mService.removeCallback(this);

        mToggleStart.setOnCheckedChangeListener(null);
        mToggleStart = null;

        mButtonMenu.release();
        mButtonFin.release();
        mButtonLineSelect.release();
        mButtonLineCancel.release();
        mButtonLinePredict.release();
        mButtonTimer.release();
        mButtonTimerFixed.release();
        mButtonMap.release();

        mMainContainer = null;
        mStationWaitMessage = null;
        mStationSearchMessage = null;
        mStationName = null;
        mTextStationPrefecture = null;
        mTextDistance = null;
        mTextSelectedLine = null;
        mAnimationView.runAnimation(false);
        mAnimationView = null;

        mListStationLines.setOnItemClickListener(null);
        mListStationLines.setAdapter(null);
        mListStationLines = null;

        mButtonContainer = null;
        mButtonScreen.setOnTouchListener(null);
        mButtonScreen = null;

        mAdapter = null;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        mContext = null;
        mService = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
        inflater.inflate(R.menu.menu_main, menu);
    }

    @Override
    public void onStart(){
        super.onStart();
        if ( mService != null && mService.hasExplorerInitialized() ){
            if ( mAnimationView != null ) mAnimationView.runAnimation(mService.isRunning());
            onStationUpdate(mService.getCurrentStation());
            updateInfo(mService.getLongitude(), mService.getLatitude(), mService.getCurrentLine(), mService.getCurrentStation());
        }
    }


    @Override
    public void onLocationUpdate(double longitude, double latitude){
        if ( mService.hasExplorerInitialized() ){
            double distance = mService.mDisplayRuler.measureDistance(mService.getCurrentStation(), longitude, latitude);
            mTextDistance.setText(DistanceRuler.formatDistance(distance));
            //updateInfo(longitude, latitude, mService.getCurrentLine(), mService.getCurrentStation());
        }
    }

    @Override
    public void onStationUpdate(@Nullable Station station){
        if ( station != null ){
            if ( mMainContainer.getVisibility() != View.VISIBLE ){
                mStationWaitMessage.setVisibility(View.GONE);
                mStationSearchMessage.setVisibility(View.GONE);
                mMainContainer.setVisibility(View.VISIBLE);
            }
            updateInfo(mService.getLongitude(), mService.getLatitude(), mService.getCurrentLine(), station);
        }
    }

    @Override
    public void onLineUpdate(@Nullable Line line){
        if ( mService.hasExplorerInitialized() ){
            updateInfo(mService.getLongitude(), mService.getLatitude(), line, mService.getCurrentStation());
        }
    }

    @Override
    public void onSearchStop(String mes){
        mToggleStart.setChecked(false);
        animationButton(false);
        mAnimationView.runAnimation(false);
        mMainContainer.setVisibility(View.INVISIBLE);
        mStationWaitMessage.setVisibility(View.VISIBLE);
        mStationSearchMessage.setVisibility(View.GONE);

    }

    @Override
    public void onResolutionRequired(ResolvableApiException exception){
        try{
            exception.startResolutionForResult(mContext, mContext.PERMISSION_REQUEST_SETTING);
        }catch( IntentSender.SendIntentException e ){
            mService.onError("onResolutionRequired", e);
        }
    }

    private void updateInfo(final double longitude, final double latitude, @Nullable final Line line, @NonNull final Station station){

        mService.getMainHandler().post(new Runnable(){
            @Override
            public void run(){
                if ( mService == null ) return;
                mStationName.setStationName(station);
                mTextStationPrefecture.setText(mService.getPrefecture(station.prefecture));
                mTextDistance.setText(String.format(Locale.US, "%.0fm", mService.mDisplayRuler.measureDistance(station, longitude, latitude)));
                mTextSelectedLine.setText(line == null ? getString(R.string.line_none) : line.mName);
                onLocationUpdate(longitude, latitude);

                if ( mAdapter == null || !mAdapter.adaptee.equals(station) ){
                    mAdapter = new LineAdapter(station, mContext.getApplicationContext());
                    mListStationLines.setAdapter(mAdapter);
                    mListStationLines.setOnItemClickListener(new HorizontalListView.OnItemClickListener(){
                        @Override
                        public void onItemClick(View view, int position){
                            if ( mAdapter != null ){
                                onLineSelected(mAdapter.getItem(position));
                            }
                        }
                    });
                }
            }
        });
    }

    private void invalidateButton(){
        // check whether all the views already layout
        // backStackからpopされたとき、Activityごと破棄されたときなど
        // fragment が再生成されるとPropertyAnimatorで弄った値が元に戻ってる？
        // ここで合わせる
        if ( mButtonScreen == null ) return;
        mButtonScreen.setVisibility(View.GONE);
        
        mButtonFin.setVisibility(true);
        mButtonMenu.setVisibility(true);
        mButtonLineSelect.setVisibility(false);
        mButtonLineCancel.setVisibility(false);
        mButtonLinePredict.setVisibility(false);
        mButtonTimer.setVisibility(false);
        mButtonTimerFixed.setVisibility(false);
        mButtonMap.setVisibility(false);


        mButtonFin.invalidate(false);
        mButtonMenu.invalidate(false);
        mButtonLineSelect.invalidate(false);
        mButtonLineCancel.invalidate(false);
        mButtonLinePredict.invalidate(false);
        mButtonTimer.invalidate(false);
        mButtonTimerFixed.invalidate(false);
        mButtonMap.invalidate(false);
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom){
        if ( v == mButtonContainer ){
            mButtonContainerWidth = mButtonContainer.getWidth();
            mButtonContainerHeight = mButtonContainer.getHeight();
            mButtonContainer.removeOnLayoutChangeListener(this);
        }
    }

    private static class FloatingButton {

        FloatingButton(Button button, int x, int y){
            this.button = button;
            this.x = x;
            this.y = y;
        }

        int x, y;
        Button button;

        void invalidate(boolean expand){
            button.setTranslationX(expand ? x : 0f);
            button.setTranslationY(expand ? y : 0f);
            button.setAlpha(1f);
            button.setScaleX(1f);
            button.setScaleY(1f);
        }

        ObjectAnimator animate(boolean expand){
            return animate(expand, this.x, this.y);
        }

        ObjectAnimator animate(boolean expand, float x, float y){
            float scaleFrom = expand ? 0.1f : 1f;
            float scaleTo = expand ? 1f : 0.1f;
            float alphaFrom = expand ? 0f : 1f;
            float alphaTo = expand ? 1f : 0f;
            float srcX = expand ? 0f : x;
            float desX = expand ? x : 0f;
            float srcY = expand ? 0f : y;
            float desY = expand ? y : 0f;
            return ObjectAnimator.ofPropertyValuesHolder(
                    button,
                    PropertyValuesHolder.ofFloat("alpha", alphaFrom, alphaTo),
                    PropertyValuesHolder.ofFloat("translationX", srcX, desX),
                    PropertyValuesHolder.ofFloat("translationY", srcY, desY),
                    PropertyValuesHolder.ofFloat("scaleX", scaleFrom, scaleTo),
                    PropertyValuesHolder.ofFloat("scaleY", scaleFrom, scaleTo)
            );
        }

        ObjectAnimator animate(boolean expand, float x, boolean effect){
            if ( effect ){
                float scaleFrom = expand ? 0.1f : 1f;
                float scaleTo = expand ? 1f : 0.1f;
                float alphaFrom = expand ? 0f : 1f;
                float alphaTo = expand ? 1f : 0f;
                float srcX = expand ? x : 0f;
                float desX = expand ? 0f : x;
                return ObjectAnimator.ofPropertyValuesHolder(
                        button,
                        PropertyValuesHolder.ofFloat("alpha", alphaFrom, alphaTo),
                        PropertyValuesHolder.ofFloat("translationX", srcX, desX),
                        PropertyValuesHolder.ofFloat("scaleX", scaleFrom, scaleTo),
                        PropertyValuesHolder.ofFloat("scaleY", scaleFrom, scaleTo)
                );
            }else{
                return ObjectAnimator.ofFloat(
                        button,
                        "translationX",
                        expand ? 0f : x,
                        expand ? x : 0f
                );
            }
        }

        void release(){
            button.setOnClickListener(null);
            button = null;
        }

        void setOnClickListener(View.OnClickListener listener){
            button.setOnClickListener(listener);
        }

        void setVisibility(boolean visible){
            button.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }

        boolean getVisibility(){
            return button.getVisibility() == View.VISIBLE;
        }


    }

    private void animationButton(final boolean expand){
        final boolean select = mService.hasExplorerInitialized();
        final boolean position = mService.isCurrentLineSet();
        if ( !select && position ) return;
        if ( mButtonContainerHeight * mButtonContainerWidth == 0 ) return;
        if ( mRunningAnimator != null ) return;
        if ( expand == mButtonMap.getVisibility() ) return;

        final int width = mButtonWidth;
        final int bigWidth = mButtonBigWidth;
        final int duration = 300;
        List<Animator> list = new LinkedList<>();
        list.add(mButtonMap.animate(expand));
        list.add(mButtonTimer.animate(expand));
        list.add(mButtonTimerFixed.animate(expand));
        if ( select || (!expand && mButtonLineSelect.getVisibility()) ){
            list.add(mButtonLineSelect.animate(expand));
        }
        if ( position || (!expand && mButtonLineCancel.getVisibility()) ){
            list.add(mButtonLineCancel.animate(expand));
        }
        if ( select || (!expand && mButtonLinePredict.getVisibility()) ){
            list.add(mButtonLinePredict.animate(expand));
        }
        list.add(mButtonFin.animate(expand, width * 2 + bigWidth - mButtonContainerWidth, false));
        list.add(mButtonMenu.animate(!expand, width + bigWidth - mButtonContainerWidth, true));

        AnimatorSet set = new AnimatorSet();


        /*
        int x = Math.round(-mButtonContainer.getTranslationX() / width);

        if ( select && !position && x == 1 ) return;
        if ( select && position && x == 2 ) return;
        if ( !select && x == 0 ) return;

        AnimatorSet set = new AnimatorSet();
        if ( select && !position && x == 0 ){
            ObjectAnimator anim1 = ObjectAnimator.ofFloat(mButtonContainer, "translationX", 0, -width);
            ObjectAnimator anim2 = ObjectAnimator.ofPropertyValuesHolder(
                    mButtonLine,
                    PropertyValuesHolder.ofFloat("alpha", 0, 1),
                    PropertyValuesHolder.ofFloat("translationX", width/2, 0),
                    PropertyValuesHolder.ofFloat("scaleX", 0.1f, 1f),
                    PropertyValuesHolder.ofFloat("scaleY", 0.1f, 1f)
            );
            set.playTogether(anim1, anim2);
            set.setDuration(duration);
        }else if ( !select && x == 1 ){
            ObjectAnimator anim1 = ObjectAnimator.ofFloat(mButtonContainer, "translationX", -width, 0);
            ObjectAnimator anim2 = ObjectAnimator.ofPropertyValuesHolder(
                    mButtonLine,
                    PropertyValuesHolder.ofFloat("alpha", 1, 0),
                    PropertyValuesHolder.ofFloat("translationX", 0, width/2),
                    PropertyValuesHolder.ofFloat("scaleX", 1f, 0.1f),
                    PropertyValuesHolder.ofFloat("scaleY", 1f, 0.1f)
            );
            set.playTogether(anim1, anim2);
            set.setDuration(duration);
        }else if ( select && !position && x == 2 ){
            ObjectAnimator anim1 = ObjectAnimator.ofFloat(mButtonContainer, "translationX", -width*2, -width);
            ObjectAnimator anim2 = ObjectAnimator.ofPropertyValuesHolder(
                    mButtonPosition,
                    PropertyValuesHolder.ofFloat("alpha", 1, 0),
                    PropertyValuesHolder.ofFloat("translationX", -width, -width/2),
                    PropertyValuesHolder.ofFloat("scaleX", 1f, 0.1f),
                    PropertyValuesHolder.ofFloat("scaleY", 1f, 0.1f)
            );
            set.playTogether(anim1, anim2);
            set.setDuration(duration);
        }else if ( !select && x == 2 ){
            ObjectAnimator anim1 = ObjectAnimator.ofFloat(mButtonContainer, "translationX", -width*2, 0);
            ObjectAnimator anim2 = ObjectAnimator.ofPropertyValuesHolder(
                    mButtonLine,
                    PropertyValuesHolder.ofFloat("alpha", 1, 0),
                    PropertyValuesHolder.ofFloat("translationX", 0, width/2),
                    PropertyValuesHolder.ofFloat("scaleX", 1f, 0.1f),
                    PropertyValuesHolder.ofFloat("scaleY", 1f, 0.1f)
            );
            anim2.setInterpolator(new AccelerateInterpolator(1f));
            ObjectAnimator anim3 = ObjectAnimator.ofPropertyValuesHolder(
                    mButtonPosition,
                    PropertyValuesHolder.ofFloat("alpha", 1, 0),
                    PropertyValuesHolder.ofFloat("translationX", -width, width/2),
                    PropertyValuesHolder.ofFloat("scaleX", 1f, 0.1f),
                    PropertyValuesHolder.ofFloat("scaleY", 1f, 0.1f)
            );
            set.playTogether(anim1, anim2, anim3);
            set.setDuration(duration);
        }else if ( select && position ){
            ObjectAnimator anim1 = ObjectAnimator.ofFloat(mButtonContainer, "translationX", -width, -width*2);
            ObjectAnimator anim2 = ObjectAnimator.ofPropertyValuesHolder(
                    mButtonPosition,
                    PropertyValuesHolder.ofFloat("alpha", 0, 1),
                    PropertyValuesHolder.ofFloat("translationX", -width/2, -width),
                    PropertyValuesHolder.ofFloat("scaleX", 0.1f, 1f),
                    PropertyValuesHolder.ofFloat("scaleY", 0.1f, 1f)
            );
            set.playTogether(anim1, anim2);
            set.setDuration(duration);
        }
        set.addListener(new Animator.AnimatorListener(){
            @Override
            public void onAnimationStart(Animator animation){
                if ( select ) mButtonLine.setVisibility(View.VISIBLE);
                if ( position ) mButtonPosition.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation){
                animation.removeAllListeners();
                if ( !select ) mButtonLine.setVisibility(View.INVISIBLE);
                if ( !position ) mButtonPosition.setVisibility(View.INVISIBLE);
                mRunningAnimator = null;
            }

            @Override
            public void onAnimationCancel(Animator animation){

            }

            @Override
            public void onAnimationRepeat(Animator animation){

            }
        });*/
        set.playTogether(list);
        set.setDuration(duration);
        set.addListener(new Animator.AnimatorListener(){
            @Override
            public void onAnimationStart(Animator animation){
                if ( expand ){
                    mButtonMap.setVisibility(true);
                    mButtonTimer.setVisibility(true);
                    mButtonTimerFixed.setVisibility(true);
                    mButtonLineSelect.setVisibility(select);
                    mButtonLineCancel.setVisibility(position);
                    mButtonLinePredict.setVisibility(select);
                    mButtonScreen.setVisibility(View.VISIBLE);
                }else{
                    mButtonMenu.setVisibility(true);
                    mButtonScreen.setVisibility(View.GONE);
                }
            }

            @Override
            public void onAnimationEnd(Animator animation){
                if ( !expand ){
                    mButtonMap.setVisibility(false);
                    mButtonTimer.setVisibility(false);
                    mButtonTimerFixed.setVisibility(false);
                    mButtonLineSelect.setVisibility(false);
                    mButtonLineCancel.setVisibility(false);
                    mButtonLinePredict.setVisibility(false);
                }else{
                    mButtonMenu.setVisibility(false);
                }
                animation.removeAllListeners();
                mRunningAnimator = null;
            }

            @Override
            public void onAnimationCancel(Animator animation){

            }

            @Override
            public void onAnimationRepeat(Animator animation){

            }
        });
        set.start();
        mRunningAnimator = set;
    }



    @Override
    public void getService(MainActivity.OnServiceConnectedListener listener){
        mContext.getService(listener);
    }

    @Override
    protected void onBackStackPop(String name){
        if ( name != null && name.equals(TRANSACTION_TAG_INITIAL) ){
            mIsDetailsFragmentShown = false;
        }
    }

    @Override
    public void onRequestFragmentRemoved(){
        FragmentManager manager = getCompatFragmentManager();
        manager.popBackStack(TRANSACTION_TAG_INITIAL, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        mIsDetailsFragmentShown = false;
        /*
        String targetName = null;
        for ( int i=manager.getBackStackEntryCount()-1 ; i>=0 ; i-- ){
            FragmentManager.BackStackEntry entry = manager.getBackStackEntryAt(i);
            if ( MainActivity.parseTransactionName(entry).equals(TRANSACTION_TAG_INITIAL) ){
                targetName = entry.getName();
            }
        }
        if ( targetName != null ){
            manager.popBackStack(targetName, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            mCurrentFragmentTag = RadarFragment.FRAGMENT_TAG;
        }*/
    }

    private static class LineAdapter extends HorizontalListView.ArrayAdapter<Line>{

        private LineAdapter(Station station, Context context){
            super(Arrays.asList(station.lines));
            adaptee = station;
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        private final Station adaptee;
        private LayoutInflater mInflater;

        @Override
        public View getView(ViewGroup parent){
            return mInflater.inflate(R.layout.cell_line_small, null, false);
        }

        @Override
        public void onBindView(View view, Line data, int position){
            TextView text = (TextView)view.findViewById(R.id.textCellLineName);
            View symbol = view.findViewById(R.id.textCellLineSymbol);
            text.setText(data.mName);GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.RECTANGLE);
            drawable.setCornerRadius(4f);
            //be sure not to add alpha channel
            drawable.setColor(0xFF000000 | data.mColor);
            symbol.setBackground(drawable);
        }
    }

    @Override
    public void onStationSelected(Station station){
        FragmentTransaction transaction = getCompatFragmentManager().beginTransaction();
        StationFragment fragment = StationFragment.getInstanceForMain(station);
        String tag = mIsDetailsFragmentShown ? null : TRANSACTION_TAG_INITIAL;
        mIsDetailsFragmentShown = true;
        transaction.replace(R.id.containerSubFragment, fragment);
        transaction.addToBackStack(tag);
        transaction.commit();
    }

    @Override
    public void onRequestLineOnMap(Line line){
        mContext.requestLineOnMap(line);
    }

    @Override
    public void onRequestPolylineShown(Line line){
        //nothing to do
    }

    @Override
    public void onRequestPolylineRemoved(){
        //nothing to do
    }

    @Override
    public void onRequestVoronoiShown(Station station){
        //nothing to do
    }

    @Override
    public void onRequestRadarShown(double lon, double lat){
        //nothing to do
    }

    @Override
    public void onRequestVoronoiRemoved(){
        //nothing to do
    }

    @Override
    public void onRequestStationOnMap(Station station){
        mContext.requestStationOnMap(station);
    }

    @Override
    public void onLineSelected(Line line){
        FragmentTransaction transaction = getCompatFragmentManager().beginTransaction();
        LineFragment fragment = LineFragment.getInstanceForMain(line);
        String tag = mIsDetailsFragmentShown ? null : TRANSACTION_TAG_INITIAL;
        mIsDetailsFragmentShown = true;
        transaction.replace(R.id.containerSubFragment, fragment);
        transaction.addToBackStack(tag);
        transaction.commit();

    }


}
