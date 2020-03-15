package jp.ac.u_tokyo.t.seo.station.app;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;

/**
 * @author Seo-4d696b75
 * @version 2019/04/18.
 */

public class AnimationView extends View{

    public AnimationView(Context context){
        this(context, null);
    }

    public AnimationView(Context context, AttributeSet set){
        this(context, set, 0);
    }

    public AnimationView(Context context, AttributeSet set, int defaultAttr){
        super(context, set, defaultAttr);
        getViewTreeObserver().addOnGlobalLayoutListener(LAYOUT_LISTENER);
        mPaint = new Paint();
        mMatrix = new Matrix();
        Resources res = context.getResources();
        mForeground = BitmapFactory.decodeResource(res, R.drawable.launch_icon_old);
        mBackground = BitmapFactory.decodeResource(res, R.drawable.loop_line);
        setWillNotDraw(false);
    }

    private final ViewTreeObserver.OnGlobalLayoutListener LAYOUT_LISTENER = new ViewTreeObserver.OnGlobalLayoutListener(){
        @Override
        public void onGlobalLayout(){
            getViewTreeObserver().removeOnGlobalLayoutListener(LAYOUT_LISTENER);
            mWidth = getWidth();
            mHeight = getHeight();
            mSrc = new Rect(0,0,mForeground.getWidth(),mForeground.getHeight());
            mDes = new Rect(
                    (int)(mWidth * 0.1),
                    (int)(mHeight * 0.13),
                    (int)(mWidth * 0.8),
                    (int)(mHeight * 0.83)
            );
            runAnimation(mRequestRunning);
        }
    };

    private int mWidth, mHeight;
    private long mTime;
    private float mDegree;
    private boolean mRunning;
    private boolean mRequestRunning;
    private boolean mRequestStop;
    private Bitmap mBackground, mForeground;
    private Paint mPaint;
    private Matrix mMatrix;
    private Rect mSrc, mDes;

    public void runAnimation(boolean run){
        if ( mWidth == 0 || mHeight == 0 || mBackground == null || mForeground == null ){
            mRequestRunning = run;
            return;
        }
        if ( mRunning != run ){
            if ( mRunning ){
                mRequestStop = true;
            }else{
                mRunning = true;
                invalidate();
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas){
        if ( mWidth == 0 || mHeight == 0 || mBackground == null || mForeground == null ) return;
        long time = SystemClock.uptimeMillis();
        if ( mRunning && mTime != 0 ){
            float degree = mDegree + (time - mTime) * 0.01f;
            if ( mRequestStop ){
                float step = 360f / 16;
                float threshold = (float)Math.ceil(mDegree/step) * step;
                if ( degree >= threshold ){
                    mRunning = false;
                    mRequestStop = false;
                    mDegree = 0;
                    time = 0;
                }
            }
            if ( degree > 360 ) degree -= 360;
            mDegree = degree;
        }
        mMatrix.reset();
        mMatrix.postRotate(mDegree, mBackground.getWidth()/2f, mBackground.getHeight()/2f);
        mMatrix.postScale((float)mWidth/mBackground.getWidth(), (float)mHeight/mBackground.getHeight());
        //canvas.drawColor(0, PorterDuff.Mode.CLEAR);
        canvas.drawBitmap(mBackground, mMatrix, mPaint);
        canvas.drawBitmap(mForeground, mSrc, mDes, mPaint);

        mTime = time;
        if ( mRunning ) invalidate();
    }

    @Override
    protected void onRestoreInstanceState(Parcelable source){
        SavedState state = (SavedState)source;
        super.onRestoreInstanceState(state.getSuperState());

        runAnimation(state.mRunning);


    }

    @Override
    protected Parcelable onSaveInstanceState(){
        Parcelable superState = super.onSaveInstanceState();
        SavedState state = new SavedState(superState);
        state.mRunning = this.mRunning;
        return state;
    }

    private static class SavedState extends BaseSavedState {

        boolean mRunning;

        SavedState(Parcelable state){
            super(state);
        }


        SavedState(Parcel source){
            super(source);
            boolean[] array = new boolean[1];
            source.readBooleanArray(array);
            mRunning = array[0];
        }

        @Override
        public void writeToParcel(final Parcel out, final int flags) {
            super.writeToParcel(out, flags);
            boolean[] array = new boolean[]{mRunning};
            out.writeBooleanArray(array);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };

    }
}
