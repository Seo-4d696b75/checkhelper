package jp.ac.u_tokyo.t.seo.station.app;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import jp.ac.u_tokyo.t.seo.customview.ExpandableTextView;

/**
 * @author Seo-4d696b75
 * @version 2019/04/18.
 */

public class StationNameView extends LinearLayout{

    public StationNameView(Context context){
        this(context, null);
    }

    public StationNameView(Context context, AttributeSet set){
        this(context, set, 0);
    }

    public StationNameView(Context context, AttributeSet set, int defaultAttr){
        super(context, set, defaultAttr);

        final TypedArray array = context.obtainStyledAttributes(set, R.styleable.StationNameView, defaultAttr, 0);
        float nameTextSize = array.getDimensionPixelSize(R.styleable.StationNameView_nameTextSize, 20);
        float phoneticTextSize = array.getDimensionPixelSize(R.styleable.StationNameView_phoneticTextSize, 20);
        int nameTextColor = array.getColor(R.styleable.StationNameView_nameTextColor, Color.BLACK);
        int phoneticTextColor = array.getColor(R.styleable.StationNameView_phoneticTextColor, Color.BLACK);

        array.recycle();


        ViewGroup.LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        //LinearLayout container = new LinearLayout(context);
        //container.setOrientation(LinearLayout.VERTICAL);
        //addView(container, params);

        setOrientation(VERTICAL);

        params = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);


        mNameText = new ExpandableTextView(context);
        mNameText.setTextSize(TypedValue.COMPLEX_UNIT_PX, nameTextSize);
        mNameText.setTextColor(nameTextColor);
        mNameText.setText(" ");
        mNameText.setLines(1);
        addView(mNameText, params);

        mPhoneticText = new ExpandableTextView(context);
        mPhoneticText.setTextSize(TypedValue.COMPLEX_UNIT_PX, phoneticTextSize);
        mPhoneticText.setTextColor(phoneticTextColor);
        mPhoneticText.setText(" ");
        mPhoneticText.setLines(1);
        addView(mPhoneticText, params);

    }
    private TextView mNameText, mPhoneticText;

    public void setStationName(Station station){
        if ( station == null ) return;
        //mNameText.setPadding(0,0,0,0);
        //mPhoneticText.setPadding(0,0,0,0);
        mNameText.setText(station.name);
        mPhoneticText.setText(station.nameKana);

    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b){

        int w1 = mNameText.getMeasuredWidth();
        int w2 = mPhoneticText.getMeasuredWidth();
        int h1 = mNameText.getMeasuredHeight();
        int h2 = mPhoneticText.getMeasuredHeight();
        int w = Math.max(w1, w2);

        mNameText.layout(
                (w-w1)/2,
                0,
                (w-w1)/2 + w1,
                h1
        );
        mPhoneticText.layout(
                (w-w2)/2,
                h1,
                w2 + (w-w2)/2,
                h1 + h2
        );
    }
}
