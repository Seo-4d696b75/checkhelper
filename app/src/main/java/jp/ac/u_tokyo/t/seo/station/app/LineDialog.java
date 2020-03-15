package jp.ac.u_tokyo.t.seo.station.app;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import jp.ac.u_tokyo.t.seo.customdialog.CustomDialog;

/**
 * @author Seo-4d696b75
 * @version 2018/04/07.
 */

public class LineDialog extends CustomDialog{

    private StationService mService;

    private final String KEY_LIST_SIZE = "list_size";

    private boolean mStartPrediction;
    static final String KEY_START_PREDICTION = "start_prediction";

    private int mListSize = 10;
    private LineAdapter mAdapter;
    private ListView mListView;
    private Context mContext;

    public static LineDialog getInstance(boolean startPrediction){
        LineDialog dialog = new LineDialog();
        Bundle args = new Bundle();
        args.putBoolean(KEY_START_PREDICTION, startPrediction);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        if ( savedInstanceState != null  ){
            mListSize = savedInstanceState.getInt(KEY_LIST_SIZE, 10);
        }
    }

    @Override
    protected View onInflateView(LayoutInflater inflater, int id){
        return inflater.inflate(R.layout.dialog_line, null, false);
    }

    @Override
    protected void onCreateContentView(@NonNull View view){
        mListView = (ListView)view.findViewById(R.id.listLines);

        Bundle args = getArguments();
        mStartPrediction = args != null &&
                args.getBoolean(KEY_START_PREDICTION, false);

        Activity activity = getActivity();
        if ( activity instanceof MainActivity ){
            mContext = activity;
            ((MainActivity)activity).getService(new MainActivity.OnServiceConnectedListener(){
                @Override
                public void onServiceConnected(StationService service){
                    mService = service;
                    setAdapter();
                }
            });
        }else{
            dismiss();
        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_LIST_SIZE, mListSize);
    }

    private void setAdapter(){
        List<Line> lines = mService.getNearLines(mListSize);
        if ( lines != null ){
            List<LineWrapper> wrappers = new LinkedList<>();
            for ( Line item : lines ) wrappers.add(new LineWrapper(item));
            wrappers.add(new LineWrapper(getString(R.string.select_line_more)));
            mAdapter = new LineAdapter(mContext, wrappers);
            mListView.setAdapter(mAdapter);
            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id){
                    LineWrapper wrapper = mAdapter.getItem(position);
                    if ( wrapper != null ){
                        Line line = wrapper.mLine;
                        if ( line != null ){
                            if ( mStartPrediction ){
                                mService.setCurrentLine(line, false);
                                mService.startLinePrediction();
                            }else{
                                mService.setCurrentLine(line, true);
                            }
                            dismiss();
                        }else{
                            mListSize += 10;
                            setAdapter();
                        }
                    }
                }
            });
        }
    }

    @Override
    protected boolean onButtonClicked(int which){
        if ( which == DialogInterface.BUTTON_NEGATIVE ){
            mService.setCurrentLine(null, true);
        }
        return true;
    }

    @Override
    public void onDestroyView(){
        super.onDestroyView();
        mListView.setAdapter(null);
        mListView.setOnItemClickListener(null);
        mListView = null;
        mAdapter = null;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        mContext = null;
        mService = null;
    }

    private static class LineWrapper {

        private LineWrapper(Line line){
            mLine = line;
            mTitle = line.mName;
        }

        private LineWrapper(String title){
            mLine = null;
            mTitle = title;
        }

        private final Line mLine;
        private final String mTitle;

    }

    private static class LineAdapter extends ArrayAdapter<LineWrapper>{


        private LayoutInflater mInflater;


        private LineAdapter(Context context, List<LineWrapper> lines){
            super(context, 0, lines);
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent){

            if ( convertView == null ){
                convertView = mInflater.inflate(R.layout.cell_line, null, false);
            }
            View view = convertView;
            LineWrapper wrapper = getItem(position);
            if ( wrapper != null ){
                Line line = wrapper.mLine;
                View symbol = view.findViewById(R.id.textCellLineSymbol);
                TextView name = (TextView)view.findViewById(R.id.textCellLineName);
                TextView size = (TextView)view.findViewById(R.id.textCellLineListSize);
                if ( line != null ){
                    name.setText(line.mName);
                    size.setText(String.format(Locale.US, "%d駅", line.mStationSize));
                    GradientDrawable drawable = new GradientDrawable();
                    drawable.setShape(GradientDrawable.RECTANGLE);
                    drawable.setCornerRadius(6f);
                    //be sure not to add alpha channel
                    drawable.setColor(0xFF000000 | line.mColor);
                    symbol.setBackground(drawable);
                    symbol.setVisibility(View.VISIBLE);
                }else{
                    name.setText(wrapper.mTitle);
                    size.setText("");
                    symbol.setVisibility(View.INVISIBLE);
                }
            }
            return view;
        }

    }

}
