package jp.ac.u_tokyo.t.seo.station.app;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Locale;

import jp.ac.u_tokyo.t.seo.customdialog.CustomDialog;

/**
 * @author Seo-4d696b75
 * @version 2019/03/12.
 */

public class DataDialog extends CustomDialog{

    private static final String KEY_VERSION = "version";
    private static final String KEY_PATH = "path";
    private static final String KEY_DATA_EMPTY = "empty";
    private static final String KEY_GET = "get";
    private static final String KEY_SIZE = "size";

    interface DataUpdateResult {
        void onUpdateRequired(boolean required, long version, String path);
        void onDateUpdate(long version, boolean success);
    }

    public static DataDialog getInstance(long version, String path){
        Bundle args = new Bundle();
        args.putBoolean(KEY_GET, true);
        args.putLong(KEY_VERSION, version);
        args.putString(KEY_PATH, path);
        DataDialog dialog = new DataDialog();
        dialog.setArguments(args);
        return dialog;
    }

    public static DataDialog getInstance(long version, String path, String size, boolean empty){
        Bundle args = new Bundle();
        args.putBoolean(KEY_GET, false);
        args.putLong(KEY_VERSION, version);
        args.putString(KEY_PATH, path);
        args.putBoolean(KEY_DATA_EMPTY, empty);
        args.putString(KEY_SIZE, size);
        DataDialog dialog = new DataDialog();
        dialog.setArguments(args);
        return dialog;
    }

    private boolean mGetData;
    private long mTargetVersion;
    private String mDataPath;
    private boolean mDataEmpty;
    private String mDataSize;

    private StationService mService;
    private MainActivity mContext;
    private DataUpdateResult mCallback;

    @Override
    protected View onInflateView(LayoutInflater inflater, int id){
        Bundle args = getArguments();
        mGetData = args.getBoolean(KEY_GET);
        mTargetVersion = args.getLong(KEY_VERSION);
        mDataPath = args.getString(KEY_PATH);
        if ( mGetData ){
            return inflater.inflate(R.layout.dialog_data, null, false);
        }else{
            mDataEmpty = args.getBoolean(KEY_DATA_EMPTY);
            mDataSize = args.getString(KEY_SIZE);
            return inflater.inflate(mDataEmpty ? R.layout.dialog_data_initial : R.layout.dialog_data_version, null, false);
        }
    }

    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        Fragment parent = getParentFragment();
        if ( parent instanceof DataUpdateResult ){
            mCallback = (DataUpdateResult) parent;
        }else if ( context instanceof DataUpdateResult ){
            mCallback = (DataUpdateResult) context;
        }
    }

    @Override
    protected void onCreateContentView(@NonNull View view){

        Activity activity = getActivity();
        if ( activity instanceof MainActivity ){
            mContext = (MainActivity)activity;
            mService = mContext.getService();
        }else{
            return;
        }

        if ( mGetData ){
            final TextView status = (TextView)view.findViewById(R.id.textDialogDataStatus);
            final TextView progress = (TextView)view.findViewById(R.id.textDialogDataProgress);
            final ProgressBar bar = (ProgressBar)view.findViewById(R.id.progressBarDialogData);
            progress.setText("0%");
            bar.setProgress(0);
            mService.updateData(mTargetVersion, mDataPath, new StationService.DataUpdateListener(){
                @Override
                public void onProgress(int value, String mes){
                    status.setText(mes);
                    progress.setText(String.format(Locale.US, "%2d%%", value));
                    bar.setProgress(value);
                }

                @Override
                public void onComplete(boolean success){
                    if ( mCallback != null ) mCallback.onDateUpdate(mTargetVersion, success);
                    dismiss();
                }
            });
        }else if ( !mDataEmpty ){
            TextView v = (TextView)view.findViewById(R.id.textDialogDataVersion);
            v.setText(String.format(Locale.US, getString(R.string.data_version_size_format), mTargetVersion, mDataSize));

        }
    }

    @Override
    protected boolean onButtonClicked(int which){
        if ( !mGetData && which == DialogInterface.BUTTON_POSITIVE ){
            if ( mCallback != null ) mCallback.onUpdateRequired(true, mTargetVersion, mDataPath);
        }
        if ( !mGetData && which == DialogInterface.BUTTON_NEGATIVE ){
            if ( mCallback != null ) mCallback.onUpdateRequired(false, mTargetVersion, mDataPath);
        }
        return true;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        mService = null;
        mContext = null;
        mCallback = null;
    }

}
