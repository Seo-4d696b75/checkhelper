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
import android.widget.EditText;
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
    private static final String KEY_SIZE = "size";

    private static final String KEY_REQUEST = "data_request";
    private static final String REQUEST_UPDATE = "update";
    private static final String REQUEST_INITIALIZE = "initialize";
    private static final String REQUEST_CHECK = "check";
    private static final String REQUEST_MANUAL = "manual";

    interface DataUpdateResult {
        void onUpdateRequired(boolean required, long version, String path);
        void onDateUpdate(long version, boolean success);
    }

    public static DataDialog getInstance(long version, String path){
        Bundle args = new Bundle();
        args.putString(KEY_REQUEST, REQUEST_UPDATE);
        args.putLong(KEY_VERSION, version);
        args.putString(KEY_PATH, path);
        DataDialog dialog = new DataDialog();
        dialog.setArguments(args);
        return dialog;
    }

    public static DataDialog getInstance(long version, String path, String size, boolean initial){
        Bundle args = new Bundle();
        args.putString(KEY_REQUEST, initial ? REQUEST_INITIALIZE : REQUEST_CHECK);
        args.putLong(KEY_VERSION, version);
        args.putString(KEY_PATH, path);
        args.putString(KEY_SIZE, size);
        DataDialog dialog = new DataDialog();
        dialog.setArguments(args);
        return dialog;
    }

    public static DataDialog getInstance(){
        Bundle args = new Bundle();
        args.putString(KEY_REQUEST, REQUEST_MANUAL);
        DataDialog dialog = new DataDialog();
        dialog.setArguments(args);
        return dialog;
    }

    private String mRequest;
    private long mTargetVersion;
    private String mDataPath;
    private String mDataSize;

    private StationService mService;
    private MainActivity mContext;
    private DataUpdateResult mCallback;
    private EditText mURLText;

    @Override
    protected View onInflateView(LayoutInflater inflater, int id){
        Bundle args = getArguments();
        mRequest = args.getString(KEY_REQUEST, "");
        switch ( mRequest ){
            case REQUEST_UPDATE:
                mDataPath = args.getString(KEY_PATH);
                return inflater.inflate(R.layout.dialog_data, null, false);
            case REQUEST_CHECK:
                mTargetVersion = args.getLong(KEY_VERSION);
                mDataSize = args.getString(KEY_SIZE);
                mDataPath = args.getString(KEY_PATH);
                return inflater.inflate(R.layout.dialog_data_version, null, false);
            case REQUEST_INITIALIZE:
                mTargetVersion = args.getLong(KEY_VERSION);
                mDataSize = args.getString(KEY_SIZE);
                mDataPath = args.getString(KEY_PATH);
                return inflater.inflate(R.layout.dialog_data_initial, null, false);
            case REQUEST_MANUAL:
                mTargetVersion = 0;
                mDataSize = "";
                mDataPath = "";
                return inflater.inflate(R.layout.dialog_data_manual, null, false);
                default:

        }
        return null;
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

        switch ( mRequest ){
            case REQUEST_UPDATE:
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

                break;
            case REQUEST_CHECK:
            case REQUEST_INITIALIZE:
                TextView version = (TextView)view.findViewById(R.id.textDialogDataVersion);
                TextView size = (TextView)view.findViewById(R.id.textDialogDataSize);
                version.setText(String.format(Locale.US, getString(R.string.data_update_version_format), mTargetVersion));
                size.setText(String.format(Locale.US, getString(R.string.data_update_size_format), mDataSize));
                break;
            case REQUEST_MANUAL:
                mURLText = (EditText)view.findViewById(R.id.textDataURL);
                /*
                mURLText.setEnabled(false);
                mURLText.setEnabled(true);
                mURLText.setFocusable(true);
                mURLText.setFocusableInTouchMode(true);
                mURLText.setTextIsSelectable(true);
                mURLText.setSelectAllOnFocus(true);*/
                break;
        }
    }

    @Override
    protected boolean onButtonClicked(int which){
        if ( mRequest.equals(REQUEST_UPDATE) ) return true;
        if ( mRequest.equals(REQUEST_MANUAL) && mURLText != null ) mDataPath = mURLText.getText().toString();
        if ( which == DialogInterface.BUTTON_POSITIVE ){
            if ( mCallback != null ) mCallback.onUpdateRequired(true, mTargetVersion, mDataPath);
        }
        if ( which == DialogInterface.BUTTON_NEGATIVE ){
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
