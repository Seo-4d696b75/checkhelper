package jp.ac.u_tokyo.t.seo.station.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NotificationCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RemoteViews;
import android.widget.Toast;

import jp.ac.u_tokyo.t.seo.customdialog.CustomDialog;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * @author Seo-4d696b75
 * @version 2019/05/15.
 */
class StationNotification{


    private final int NOTIFICATION_TAG = 3939;
    private final String NOTIFICATION_CHANNEL = "station_update";

    private final int BUTTON_ID_EXIT = 0;
    private final int BUTTON_ID_TIMER = 1;

    StationNotification(StationService service){
        this.mService = service;
        mNotificationManager = (NotificationManager)service.getSystemService(NOTIFICATION_SERVICE);


        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ){
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL,
                    mService.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(mService.getString(R.string.notification_channel_description));
            mNotificationManager.createNotificationChannel(channel);
        }

        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ){
            mBuilder = new NotificationCompat.Builder(mService, NOTIFICATION_CHANNEL);
        }else{
            mBuilder = new NotificationCompat.Builder(mService);
        }

        Intent intent = new Intent(mService, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(mService);
        stackBuilder.addNextIntentWithParentStack(intent);
        mBuilder.setContentIntent(stackBuilder.getPendingIntent(39, PendingIntent.FLAG_UPDATE_CURRENT));


        mBuilder.setStyle(new NotificationCompat.DecoratedCustomViewStyle());
        mBuilder.setSmallIcon(R.drawable.notification_icon);
        createView();
        mService.startForeground(NOTIFICATION_TAG, mBuilder.build());
        //mNotificationManager.notify(NOTIFICATION_TAG, mBuilder.build());

    }

    String getNotificationChannelID(){
        return mChannelID;
    }

    boolean checkNotificationChannel(){
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ){
            NotificationChannel current = mNotificationManager.getNotificationChannel(NOTIFICATION_CHANNEL);
            boolean vibrate = current.shouldVibrate();
            Uri sound = current.getSound();
            mChannelID = current.getId();
            if ( vibrate || sound != null ){
                // it seems to be needed that the user edit notification channel setting directly
                return true;
            }
        }
        return false;
    }

    public static class NotificationSettingDialog extends CustomDialog{

        public static NotificationSettingDialog getInstance(String channelId){
            NotificationSettingDialog dialog = new NotificationSettingDialog();
            Bundle args = new Bundle();
            args.putString(KEY_CHANNEL_ID, channelId);
            dialog.setArguments(args);
            return dialog;
        }

        static final String KEY_RESULT_NEVER = "never_call";
        static final String KEY_CHANNEL_ID = "channel_id";
        static final String DIALOG_TAG = "notification_setting";

        private boolean mNeverDisplay = false;
        private CheckBox mCheckBox;
        private String mChannelID;

        @Override
        protected View onInflateView(LayoutInflater inflater, int layoutID){
            return inflater.inflate(R.layout.dialog_notification, null, false);
        }

        @Override
        protected void onCreateContentView (@NonNull View view){
            Bundle args = getArguments();
            mChannelID = args.getString(KEY_CHANNEL_ID);
            mCheckBox = (CheckBox)view.findViewById(R.id.checkboxNotificationSettingNever);
            mCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){
                    mNeverDisplay = isChecked;
                }
            });
        }

        @Override
        protected boolean onButtonClicked(int which){
            return true;
        }

        @Override
        public void onDestroyView(){
            super.onDestroyView();
            mCheckBox.setOnCheckedChangeListener(null);
            mCheckBox = null;
        }

        @Override
        protected void onSaveResult(Bundle result){
            result.putString(KEY_CHANNEL_ID, mChannelID);
            result.putBoolean(KEY_RESULT_NEVER, mNeverDisplay);
        }

    }

    private void createView(){
        mRemoteView = new RemoteViews(mService.getPackageName(), R.layout.notification_main);
        Intent exit = new Intent(mService, StationService.class)
                .putExtra(StationService.REQUEST_KEY, StationService.REQUEST_EXIT)
                .putExtra(StationService.KEY_CLOSE_NOTIFICATION_PANEL, true);
        mRemoteView.setOnClickPendingIntent(R.id.notificationButton1, PendingIntent.getService(mService, BUTTON_ID_EXIT, exit, PendingIntent.FLAG_ONE_SHOT));

        Intent timer = new Intent(mService, StationService.class)
                .putExtra(StationService.REQUEST_KEY, StationService.REQUEST_TIMER)
                .putExtra(StationService.KEY_CLOSE_NOTIFICATION_PANEL, true);
        mRemoteView.setOnClickPendingIntent(R.id.notificationButton2, PendingIntent.getService(mService, BUTTON_ID_TIMER, timer, PendingIntent.FLAG_CANCEL_CURRENT));

        mBuilder.setCustomContentView(mRemoteView);
    }

    void release(){
        mService = null;
        /*if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ){
            mNotificationManager.deleteNotificationChannel(NOTIFICATION_CHANNEL);
        }*/
        mNotificationManager = null;
        mRemoteView = null;
    }

    private StationService mService;
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mBuilder;
    private String mChannelID;

    private RemoteViews mRemoteView;
    private int mUpdateCnt;

    void updateNotification(String title, String message){
        if ( mUpdateCnt++ > 100 ){
            createView();
            mUpdateCnt = 0;
            //https://stackoverflow.com/questions/22789588/how-to-update-notification-with-remoteviews
            // after repeating update of notification, size of mRemoteView.mActions > 1MB
            // TransactionTooLargeException will be thrown
        }

        mRemoteView.setTextViewText(R.id.notification_title, title);
        mRemoteView.setTextViewText(R.id.notification_message, message);

        mNotificationManager.notify(NOTIFICATION_TAG, mBuilder.build());
    }

}
