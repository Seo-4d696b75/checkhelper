package jp.ac.u_tokyo.t.seo.station.app;

import android.Manifest;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.Settings;
import android.service.media.MediaBrowserService;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.util.LinkedList;
import java.util.List;

import jp.ac.u_tokyo.t.seo.customdialog.CustomDialog;
import jp.ac.u_tokyo.t.seo.customdialog.FragmentCompatActivity;


public class MainActivity extends FragmentCompatActivity implements ServiceConnection,
        DataDialog.DataUpdateResult, CustomDialog.OnClickListener{

    private StationService mService;
    private boolean mHasRequestService;

    private final String KEY_PERMISSION_CHECK = "permission_check";
    private final String KEY_FRAGMENT_ADD = "fragment_add";
    private final String KEY_FRAGMENT_PANES = "fragment_panes";

    static final String KEY_PREDICTION_LINE = "select_line_for_prediction";

    private boolean mHasPermissionChecked;
    private boolean mHasFragmentAdded;

    private final String TRANSACTION_TAG_MAP = "transaction2map";

    final int PERMISSION_REQUEST_OVERLAY = 39;
    final int PERMISSION_REQUEST_SETTING = 38;
    final int PERMISSION_REQUEST_LOCATION = 37;
    final int PERMISSION_REQUEST_STORAGE = 36;

    private boolean mHasTwoPanes = false;
    private boolean mHadTwoPanes = false;
    private boolean mHasLargeScreen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        mHasTwoPanes = getResources().getBoolean(R.bool.has_two_panes);
        mHasLargeScreen = getResources().getBoolean(R.bool.has_large_screen);

        if ( savedInstanceState != null ){
            mHasPermissionChecked = savedInstanceState.getBoolean(KEY_PERMISSION_CHECK, false);
            mHasFragmentAdded = savedInstanceState.getBoolean(KEY_FRAGMENT_ADD, false);
            mHadTwoPanes = savedInstanceState.getBoolean(KEY_FRAGMENT_PANES, false);
        }else{
            mHasPermissionChecked = false;
            mHasFragmentAdded = false;
            mHadTwoPanes = mHasTwoPanes;
        }


        if ( mHasLargeScreen ){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }else{
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        setContentView(R.layout.main_activity);

    }

    @Override
    protected void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_PERMISSION_CHECK, mHasPermissionChecked);
        outState.putBoolean(KEY_FRAGMENT_ADD, mHasFragmentAdded);
        outState.putBoolean(KEY_FRAGMENT_PANES, mHasTwoPanes);
    }

    @Override
    protected void onStop(){
        super.onStop();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if ( mService != null ){
            unbindService(this);
            mService = null;
        }
        mListeners.clear();
        mListeners = null;
    }

    @Override
    protected void onResume(){
        super.onResume();
        checkPermission();
        /*
        以下のフローで初期化する
            onResume()
            checkPermission()
            checkService()
            onServiceChecked()
        途中で他のアクティビティを呼び出す必要があるので、そこから戻ってきたとき
        onResume()から辿りなおせるように設計する
         */
    }

    @Override
    protected void onPause(){
        super.onPause();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder){
        if ( binder instanceof StationService.StationServiceBinder ){
            mService = ((StationService.StationServiceBinder)binder).getService();
            mService.onActivityBound(this);
            for ( OnServiceConnectedListener listener : mListeners ){
                listener.onServiceConnected(mService);
            }
            mListeners.clear();
            checkService();
        }
    }

    private void checkService(){
        if ( mService.needNotificationSetting() ){
            String channel = mService.getNotificationChannelID();
            DialogFragment fragment = StationNotification.NotificationSettingDialog.getInstance(channel);
            fragment.show(getCompatFragmentManager(), StationNotification.NotificationSettingDialog.DIALOG_TAG);
            return;
        }
        if ( !mService.hasVersionChecked() ){
            mService.checkDataVersion(new StationService.DataVersionResult(){
                @Override
                public void onUpToDate(){
                    onServiceChecked();
                }

                @Override
                public void onLatestFound(long version, String url, String size){
                    if ( mService != null ){
                        DataDialog dialog = DataDialog.getInstance(version, url, size, !mService.isDataInitialized());
                        dialog.show(getCompatFragmentManager(), null);
                    }
                }

                @Override
                public void onCheckFailure(){
                    onServiceChecked();
                }
            });
        }
        if ( mService.isDataInitialized() ) onServiceChecked();
    }

    private void onServiceChecked(){
        if ( mService == null || !mService.isDataInitialized() ){
            Toast.makeText(getApplicationContext(), getString(R.string.data_version_fail), Toast.LENGTH_SHORT).show();
            onFinishApp();
            return;
        }


        if ( !mHasFragmentAdded ){
            mHasFragmentAdded = true;
            Fragment fragment = new MainFragment();
            FragmentTransaction transaction = getCompatFragmentManager().beginTransaction();
            if ( mHasTwoPanes ){
                transaction.add(R.id.containerMap, StationMapFragment.getInstance(), "map_fragment");
            }
            transaction.add(R.id.containerMain, fragment, "main_fragment");
            transaction.commit();
        }else if ( mHasTwoPanes != mHadTwoPanes ){
            FragmentManager manager = getCompatFragmentManager();
            FragmentTransaction transaction = manager.beginTransaction();
            if ( mHasTwoPanes ){
                if ( manager.findFragmentById(R.id.containerMap) == null ){
                    transaction.add(R.id.containerMap, StationMapFragment.getInstance());
                }
                if ( manager.findFragmentById(R.id.containerMain) == null ){
                    transaction.add(R.id.containerMain, new MainFragment());
                }
            }else{
                transaction.remove(manager.findFragmentById(R.id.containerMap));
            }
            transaction.commit();
        }


        Intent intent = getIntent();
        if ( intent != null ){
            if ( intent.getBooleanExtra(KEY_PREDICTION_LINE, false) ){
                // only once
                intent.putExtra(KEY_PREDICTION_LINE, false);
                LineDialog dialog = LineDialog.getInstance(true);
                dialog.show(getCompatFragmentManager(), "select_line");
            }
        }
    }


    private void checkPermission(){
        if ( !mHasPermissionChecked ){

            // API level >= 23 request permission at runtime
            if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ){
                if ( !Settings.canDrawOverlays(this) ){
                    Toast.makeText(getApplicationContext(), getText(R.string.permission_overlay_message), Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, PERMISSION_REQUEST_OVERLAY);
                    return;
                }else if ( ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ){
                    ActivityCompat.requestPermissions(
                            this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_LOCATION
                    );
                    return;
                } else if ( ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ){
                    ActivityCompat.requestPermissions(
                            this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_STORAGE
                    );
                    return;
                }
            }


            int resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
            if ( resultCode != ConnectionResult.SUCCESS ){
                GoogleApiAvailability.getInstance().getErrorDialog(this, resultCode, 0).show();
                return;
            }

            // start service after all permissions have been granted
            Intent intent = new Intent(MainActivity.this, StationService.class);
            if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ){
                // Activityが非可視の場合でstartService()するとクラッシュする
                startForegroundService(intent);
            }else{
                startService(intent);
            }


            mHasPermissionChecked = true;
        }

        if ( !mHasRequestService ){
            mHasRequestService = true;
            Intent intent = new Intent(MainActivity.this, StationService.class);
            bindService(intent, this, BIND_AUTO_CREATE);
        }else if ( mService != null ){
            checkService();
        }

    }

    @Override
    public void onUpdateRequired(boolean required, long version, String path){
        if ( required ){
            DataDialog dialog = DataDialog.getInstance(version, path);
            dialog.show(getCompatFragmentManager(), null);
        }else{
            onServiceChecked();
        }
    }

    @Override
    public void onDateUpdate(long version, boolean success){
        if ( success ){
            Toast.makeText(getApplicationContext(), getString(R.string.data_version_success) + version, Toast.LENGTH_SHORT).show();
        }
        onServiceChecked();
    }


    @Override
    public void onServiceDisconnected(ComponentName name){

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent){
        switch ( requestCode ){
            case PERMISSION_REQUEST_OVERLAY:
                if ( Build.VERSION.SDK_INT == Build.VERSION_CODES.O ){
                    // when the user returns back from another activity after granting permission,
                    // a bug found that granted permission not up to date

                    // rebooting app seems to resolve.
                    Toast.makeText(getApplicationContext(), getString(R.string.permission_reboot), Toast.LENGTH_SHORT).show();
                    onFinishApp();
                }else if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this) ){
                    Toast.makeText(this, "OVERLAY_PERMISSION not granted", Toast.LENGTH_SHORT).show();
                    onFinishApp();
                }
                break;
            case PERMISSION_REQUEST_SETTING:
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResult){
        if ( requestCode == PERMISSION_REQUEST_LOCATION ){
            if ( grantResult[0] != PackageManager.PERMISSION_GRANTED ){
                Toast.makeText(this, "LOCATION_PERMISSION not granted", Toast.LENGTH_SHORT).show();
                onFinishApp();
            }
        } else if ( requestCode == PERMISSION_REQUEST_STORAGE ){
            if ( grantResult[0] != PackageManager.PERMISSION_GRANTED ){
                Toast.makeText(this, "storage permission not granted", Toast.LENGTH_SHORT).show();
                onFinishApp();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menu){
        Fragment fragment = null;
        switch ( menu.getItemId() ){
            case R.id.menuLog:
                fragment = new LogFragment();
                break;
            case R.id.menuSetting:
                fragment = new SettingFragment();
                break;
            case android.R.id.home:

            default:
        }
        if ( fragment != null ){
            FragmentManager manager = getCompatFragmentManager();
            FragmentTransaction transaction = manager.beginTransaction();
            transaction.replace(R.id.containerMain, fragment);
            transaction.addToBackStack(null);
            transaction.commit();
            return true;
        }
        return super.onOptionsItemSelected(menu);
    }

    void requestStationOnMap(Station station){
        FragmentManager manager = getCompatFragmentManager();
        if ( mHasTwoPanes ){
            Fragment fragment = manager.findFragmentById(R.id.containerMap);
            if ( fragment instanceof StationMapFragment ){
                ((StationMapFragment)fragment).onStationSelected(station);
            }
        }else{
            if ( manager.findFragmentById(R.id.containerMap) != null ) return;

            FragmentTransaction transaction = manager.beginTransaction();
            transaction.addToBackStack(TRANSACTION_TAG_MAP);
            transaction.commit();

            transaction = manager.beginTransaction();
            transaction.remove(manager.findFragmentById(R.id.containerMain));
            transaction.add(R.id.containerMap, StationMapFragment.getInstance(station));
            transaction.commit();
        }
    }

    void requestShowMap(){


        if ( !mHasTwoPanes ){

            FragmentManager manager = getCompatFragmentManager();
            if ( manager.findFragmentById(R.id.containerMap) != null ) return;

            FragmentTransaction transaction = manager.beginTransaction();
            transaction.addToBackStack(TRANSACTION_TAG_MAP);
            transaction.commit();

            transaction = manager.beginTransaction();
            transaction.remove(manager.findFragmentById(R.id.containerMain));
            transaction.add(R.id.containerMap, StationMapFragment.getInstance());
            transaction.commit();
        }

    }

    void requestLineOnMap(Line line){
        FragmentManager manager = getCompatFragmentManager();
        if ( mHasTwoPanes ){
            Fragment fragment = manager.findFragmentById(R.id.containerMap);
            if ( fragment instanceof StationMapFragment ){
                ((StationMapFragment)fragment).onLineSelected(line);
            }
        }else{
            if ( manager.findFragmentById(R.id.containerMap) != null ) return;

            FragmentTransaction transaction = manager.beginTransaction();
            transaction.addToBackStack(TRANSACTION_TAG_MAP);
            transaction.commit();

            transaction = manager.beginTransaction();
            transaction.remove(manager.findFragmentById(R.id.containerMain));
            transaction.add(R.id.containerMap, StationMapFragment.getInstance(line));
            transaction.commit();
        }
    }

    void onFinishApp(){
        if ( mService != null ){
            mService.stop();
            unbindService(MainActivity.this);
            mService = null;
        }
        stopService(new Intent(MainActivity.this, StationService.class));
        finish();
    }

    @NonNull
    StationService getService(){
        if ( mService == null )
            throw new IllegalStateException("Activity not connected to service yet");
        return mService;
    }

    void getService(OnServiceConnectedListener listener){
        if ( mService != null ){
            listener.onServiceConnected(mService);
        }else if ( mListeners != null ){
            mListeners.add(listener);
        }
    }

    private List<OnServiceConnectedListener> mListeners = new LinkedList<>();

    @Override
    public void onDialogButtonClicked(String tag, CustomDialog dialog, int which){
        if ( tag != null && tag.equals(StationNotification.NotificationSettingDialog.DIALOG_TAG) ){
            Bundle result = dialog.getResult();
            boolean never = result.getBoolean(StationNotification.NotificationSettingDialog.KEY_RESULT_NEVER);
            mService.mShowNotificationSetting = !never;
            if ( which == DialogInterface.BUTTON_POSITIVE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ){
                String channel = result.getString(StationNotification.NotificationSettingDialog.KEY_CHANNEL_ID);
                Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, mService.getPackageName());
                intent.putExtra(Settings.EXTRA_CHANNEL_ID, channel);
                Toast.makeText(getApplicationContext(), getString(R.string.notification_setting_message), Toast.LENGTH_LONG).show();
                startActivity(intent);
            }else if ( which == DialogInterface.BUTTON_NEGATIVE ){
                checkService();
            }
        }
    }

    interface OnServiceConnectedListener{
        void onServiceConnected(StationService service);
    }


    @Override
    protected void onBackStackPop(FragmentManager manager, FragmentManager.BackStackEntry entry){
        String name = entry.getName();
        if ( name != null && name.equals(TRANSACTION_TAG_MAP) ){
            if ( !mHasTwoPanes ){
                FragmentManager activityManager = getCompatFragmentManager();
                FragmentTransaction transaction = activityManager.beginTransaction();
                Fragment map = activityManager.findFragmentById(R.id.containerMap);
                if ( map != null ) transaction.remove(map);
                if ( activityManager.findFragmentById(R.id.containerMain) == null ){
                    transaction.add(R.id.containerMain, new MainFragment());
                }
                transaction.commit();
            }else{
                // try to pop next transaction in backStack, or fin app
                onBackPressed();
            }
        }
    }

}
