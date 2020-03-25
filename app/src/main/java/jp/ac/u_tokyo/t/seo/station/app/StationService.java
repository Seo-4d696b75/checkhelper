package jp.ac.u_tokyo.t.seo.station.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.os.Vibrator;
import android.provider.AlarmClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.widget.Toast;

import com.google.android.gms.common.api.ResolvableApiException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Timer;


/**
 * このアプリの基本機能を提供する. <br>
 * バックグラウンドでも動作する必要がある機能はここから提供される
 *
 * @author Seo-4d696b75
 * @version 2018/03/23.
 */

public class StationService extends Service implements
        GPSService.GPSCallback, StationKdTree.StationUpdateCallback, Line.StationAccessor, Station.LineAccessor, PositionPredictor.StationPredictCallback{

    //ServiceをActivityへバインドするための実装

    private StationServiceBinder mBinder = new StationServiceBinder(this);

    static class StationServiceBinder extends Binder{

        private StationServiceBinder(StationService service){
            this.service = service;
        }

        private StationService service;

        StationService getService(){
            return service;
        }
    }


    //Google Location API　を使って位置情報を取得するオブジェクト
    private GPSService mGPSService;

    void requestLocationUpdate(GPSService.GPSCallback callback, int interval){
        mGPSService.requestGPSUpdate(callback, interval);
    }

    void removeLocationUpdate(GPSService.GPSCallback callback){
        mGPSService.removeGPSUpdate(callback);
    }

    double getLongitude(){
        return mGPSService.getLongitude();
    }

    double getLatitude(){
        return mGPSService.getLatitude();
    }

    /**
     * 現在位置として有効な値が保持されているか確認する.
     *
     * @return 有効値があるならtrue
     */
    boolean hasValidLocation(){
        return mGPSService.hasValidLocation();
    }


    /**
     * {@link StationService}に渡されるログのリスナー
     */
    interface StatusChangeListener{
        /**
         * {@link StationService#log(String)}から呼ばれる
         *
         * @param log サービスに投げられたメッセージ
         */
        void onMessage(LogHolder.ServiceLog log);
    }

    private StatusChangeListener mStatusListener;

    void setOnStatusChangeListener(StatusChangeListener listener){
        mStatusListener = listener;
    }

    private final String KEY_INTERVAL = "interval";
    private final String KEY_RADAR = "radar";
    private final String KEY_DATA_VERSION = "dataVersion";
    private final String KEY_VIBRATE = "vibrate";
    private final String KEY_NOTIFY = "notification";
    private final String KEY_FORCE_NOTIFY = "forceNotify";
    private final String KEY_BRIGHTNESS = "brightness";
    private final String KEY_KEEP_NOTIFICATION = "notification_stationary";
    private final String KEY_NOTIFY_PREFECTURE = "notify_prefecture";
    private final String KEY_NOTIFICATION_SETTING = "notification_channel_setting";
    private final String KEY_VIBRATE_METER = "vibrate_meter";
    private final String KEY_VIBRATE_APPROACH = "vibrate_approach";

    static final String REQUEST_KEY = "service_request";
    static final String REQUEST_EXIT = "exit";
    static final String REQUEST_TIMER = "timer_5min";
    static final String REQUEST_TIMER_FINISH = "timer_finish";
    static final String KEY_CLOSE_NOTIFICATION_PANEL = "close_notification_panel";

    /**
     * {@link StationService}の基本機能の変化をコールする
     */
    interface StationCallback{
        /**
         * 現在位置が変化したとき
         *
         * @param longitude 経度
         * @param latitude  緯度
         */
        void onLocationUpdate(double longitude, double latitude);

        /**
         * 現在の駅が更新されたとき＆{@link StationService#start()}直後に初めて駅を検知したとき
         *
         * @param station {@link StationService#start()}直後に駅の検知に失敗するとnull
         */
        void onStationUpdate(@Nullable Station station);

        /**
         * 現在選択中の路線が変更されたとき
         * {@link StationService#setCurrentLine(Line, boolean)}でユーザが設定したときと、
         * {@link StationService#stop()}で停止するとnullでコールする
         *
         * @param line 選択路線が解除されたときはnull
         */
        void onLineUpdate(@Nullable Line line);

        /**
         * 周辺駅の検索が停止したとき
         *
         * @param mes その時のログ
         */
        void onSearchStop(String mes);

        void onResolutionRequired(ResolvableApiException exception);
    }

    private MainActivity mMainActivity;

    private List<StationCallback> mCallbacks;

    void setCallback(StationCallback callback){
        if ( mCallbacks.contains(callback) ) return;
        mCallbacks.add(callback);
    }

    void removeCallback(StationCallback callback){
        mCallbacks.remove(callback);
    }

    private boolean mDataInitialized = false;

    private int mInterval = 10;
    private int mRadarNum = 10;
    private boolean mIsVibrate = false;
    private boolean mIsNotifyUpdate = true;
    private boolean mForceNotify = true;
    private boolean mKeepNotification = false;
    private long mDataVersion;
    private boolean mHasVersionChecked = false;
    private boolean mIsNotifyPrefecture = false;
    boolean mShowNotificationSetting = false;
    private int mVibrateMeter = 100;
    private boolean mVibrateApproach = false;

    private boolean mRunning = false;
    private OverlayNotification mPopupNotification;
    private StationNotification mNotification;

    private Vibrator mVibrator;

    private boolean mNightMood = false;
    private int mBrightness;

    private boolean mTimerRunning;
    private Timer mTimer;
    private Handler mMainHandler;

    private StationKdTree mExplorer;
    private StationKdTree.StationNode mStationTreeRoot;
    private Date mLatestStationDetected;
    final DistanceRuler mDisplayRuler = DistanceRuler.getInstance(true);
    final DistanceRuler mInternalRuler = DistanceRuler.getInstance(false);
    private LogHolder mLogHolder;

    //private StationPredictor mPredictor;
    private PositionPredictor mPredictor;

    private Line mCurrentLine;
    private String[] mPrefectureName;


    //vibrate when approach next station
    private Station mNextStation;
    private boolean mHasApproached;

    /**
     * 駅データの初期化に成功したか判定.<br>
     * サービス起動時にストレージに保存されたデータの読み込みを試みる
     * データが存在しかつ正しくパースできたら成功
     * {@link #start()}前に確認すること
     *
     * @return 成功してたらtrue
     */
    boolean isDataInitialized(){
        return mDataInitialized;
    }

    /**
     * このサービスが起動してから最新データの確認をしたか
     *
     * @return 確認済みならtrue
     */
    boolean hasVersionChecked(){
        return mHasVersionChecked;
    }

    private CustomExceptionHandler mExceptionHandler;

    private static class CustomExceptionHandler implements Thread.UncaughtExceptionHandler{

        CustomExceptionHandler(Thread.UncaughtExceptionHandler defaultHandler, StationService service){
            mHandler = defaultHandler;
            mService = service;
        }

        private Thread.UncaughtExceptionHandler mHandler;
        private StationService mService;
        private boolean mCrashing = false;


        @Override
        public synchronized void uncaughtException(Thread t, Throwable e){
            if ( mCrashing ) return;
            mCrashing = true;
            try{
               if ( mService != null ) mService.onAppCrash(e);
            } catch( Exception hoge ){
                hoge.printStackTrace();
            } finally {
                Thread.setDefaultUncaughtExceptionHandler(mHandler);
                mHandler = null;
            }
        }

        synchronized void release(){
            mService = null;
            if ( mHandler != null ){
                Thread.setDefaultUncaughtExceptionHandler(mHandler);
                mHandler = null;
            }
        }
    }

    @Override
    public void onCreate(){
        super.onCreate();

        mExceptionHandler = new CustomExceptionHandler(Thread.getDefaultUncaughtExceptionHandler(), this);
        Thread.setDefaultUncaughtExceptionHandler(mExceptionHandler);

        mGPSService = new GPSService(this);

        mLogHolder = new LogHolder();
        mCallbacks = new LinkedList<>();

        mNotification = new StationNotification(this);

        updateNotification(getString(R.string.notification_init_title), getString(R.string.notification_init_text));


        mDataInitialized = checkData();

        //restore user setting
        SharedPreferences preference = getSharedPreferences(getString(R.string.preference_name_backup), MODE_PRIVATE);
        mIsVibrate = preference.getBoolean(KEY_VIBRATE, false);
        mRadarNum = preference.getInt(KEY_RADAR, 12);
        mInterval = preference.getInt(KEY_INTERVAL, 5);
        mIsNotifyUpdate = preference.getBoolean(KEY_NOTIFY, true);
        mForceNotify = preference.getBoolean(KEY_FORCE_NOTIFY, false);
        mKeepNotification = preference.getBoolean(KEY_KEEP_NOTIFICATION, false);
        mIsNotifyPrefecture = preference.getBoolean(KEY_NOTIFY_PREFECTURE, false);
        mVibrateMeter = preference.getInt(KEY_VIBRATE_METER, 100);
        mVibrateApproach = preference.getBoolean(KEY_VIBRATE_APPROACH, false);
        mBrightness = preference.getInt(KEY_BRIGHTNESS, 100);


        preference = getSharedPreferences(getString(R.string.preference_name), MODE_PRIVATE);
        mDataVersion = mDataInitialized ? preference.getLong(KEY_DATA_VERSION, 0) : 0;
        mShowNotificationSetting = preference.getBoolean(KEY_NOTIFICATION_SETTING, true);


        mNightMood = false;


        mVibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
        mTimer = new Timer();
        mMainHandler = new Handler(Looper.getMainLooper());



        log("onCreate > service initialized.");
    }

    private boolean checkData(){
        //check data here
        File file = getFilesDir();
        File stations = new File(file, "stations.json");
        File lines = new File(file, "lines.json");
        if ( lines.exists() && lines.isFile() && lines.canRead() ){
            log("onCreate > line file found : " + lines.getAbsolutePath());
        }else{
            log("onCreate >  line file not found : " + lines.getAbsolutePath());
            return false;
        }
        if ( stations.exists() && stations.isFile() && stations.canRead() ){
            log("onCreate > station file found : " + stations.getAbsolutePath());
        }else{
            log("onCreate >  station file not found : " + stations.getAbsolutePath());
            return false;
        }
        stations = new File(file, "tree");
        lines = new File(file, "lines");
        if ( stations.exists() && stations.isDirectory() && stations.canRead() ){
            File[] files = stations.listFiles();
            if ( files != null && files.length > 0 ){
                log("onCreate > stations data found, segment-size:" + files.length);
            }else{
                log("onCreate > stations dir in empty");
                return false;
            }
        }else{
            log("onCreate > stations dir not found : " + stations.getAbsolutePath());
            return false;
        }
        if ( lines.exists() && lines.isDirectory() && lines.canRead() ){
            File[] files = lines.listFiles();
            if ( files != null && files.length > 0 ){
                log("onCreate > lines data found, size:" + files.length);
            }else{
                log("onCreate > lines dir in empty");
                return false;
            }
        }else{
            log("onCreate > lines dir not found : " + lines.getAbsolutePath());
            return false;
        }


        //station and line pool
        mLineMap = new SparseArray<>();
        mStationMap = new SparseArray<>();

        try{
            lines = new File(file, "lines.json");
            JSONArray array = new JSONArray(readFile(lines));
            for ( int i = 0; i < array.length(); i++ ){
                JSONObject item = array.getJSONObject(i);
                Line line = new Line(item);
                mLineMap.put(line.mCode, line);
            }

            JSONObject data = getStationTreeSegment("root");
            JSONArray list = data.getJSONArray("node_list");
            SparseArray<JSONObject> nodes = new SparseArray<>();
            for ( int i=0 ; i<list.length() ; i++ ){
                JSONObject object = list.getJSONObject(i);
                nodes.put(object.getInt("code"), object);
            }
            mStationTreeRoot = new StationKdTree.StationNode(this, nodes.get(data.getInt("root")), nodes, 0);

            mExplorer = new StationKdTree(this, "main");
        }catch( Exception e ){
            onError("fail to read lines.json", e);

            return false;
        }

        return true;
    }

    interface DataVersionResult{
        void onUpToDate();

        void onLatestFound(long version, String url, String size);

        void onCheckFailure();
    }

    private static class DataVersionTask extends AsyncTask<Void, Void, String>{

        private DataVersionTask(StationService service, DataVersionResult listener){
            this.service = service;
            this.callback = listener;
        }

        StationService service;
        DataVersionResult callback;

        @Override
        protected String doInBackground(Void... param){
            String path = service.getString(R.string.version_url);
            return service.getHttp(path, null);
        }

        @Override
        protected void onPostExecute(String result){
            if ( result == null ){
                callback.onCheckFailure();
                service.onError("version check > Fail to get response from server.");
            }else{
                Log.d("VersionCheck", result);
                try{
                    JSONObject info = new JSONObject(result);
                    long version = info.getLong("version");
                    String size = info.getString("size");
                    String url = info.getString("url");
                    service.mHasVersionChecked = true;
                    if ( version == service.mDataVersion ){
                        service.log("version check > already up to date. " + version);
                        callback.onUpToDate();
                    }else if ( version > service.mDataVersion ){
                        service.log(String.format(Locale.US, "version check > latest found version:%d, size:%s", version, size));
                        callback.onLatestFound(version, url, size);
                    }else{
                        callback.onCheckFailure();
                        service.onError("version check > Version inverted.");
                    }
                }catch( JSONException e ){
                    service.onError("version check > json parse err",e);
                }
            }

            service = null;
            callback = null;
        }

        @Override
        protected void onCancelled(){
            service = null;
            callback = null;
        }
    }

    void checkDataVersion(final DataVersionResult callback){
        new DataVersionTask(this, callback).execute();
    }

    interface DataUpdateListener{
        void onProgress(int progress, String status);

        void onComplete(boolean success);
    }

    interface ProgressListener{
        void onProgress(int length, int allLength);
    }

    private static class DataUpdateTask extends AsyncTask<Void, Void, Boolean>{

        private DataUpdateTask(StationService service, DataUpdateListener listener, String path, long version){
            this.listener = listener;
            this.path = path;
            this.version = version;
            this.service = service;
        }

        DataUpdateListener listener;
        String path;
        long version;

        StationService service;
        int progress;
        String status;

        @Override
        protected Boolean doInBackground(Void... param){
            status = "データをダウンロード...";
            progress = 0;
            publishProgress();
            String str = service.getHttp(path, new ProgressListener(){
                @Override
                public void onProgress(int length, int allLength){
                    progress = (100 * length / allLength);
                    publishProgress();
                }
            });

            try{
                service.log("try to update data. version:" + version);
                status = "データを展開してます...";
                publishProgress();
                JSONObject data = new JSONObject(str);
                long v = data.getLong("version");
                if ( version != v ){
                    service.onError(String.format(Locale.US, "Version mismatch required:%d found:%d", version, v));
                    return false;
                }
                File file = service.getFilesDir();
                //路線一覧データの展開
                JSONArray lines = data.getJSONArray("lines");
                File linesDir = new File(file, "lines");
                //clean
                if ( linesDir.exists() && linesDir.isDirectory() ){
                    status = "古いデータを消去してます...";
                    publishProgress();
                    for ( File item : linesDir.listFiles(new FileFilter(){
                        @Override
                        public boolean accept(File pathname){
                            return pathname.isFile() && pathname.getAbsolutePath().endsWith(".json");
                        }
                    }) ){
                        if ( !item.delete() ){
                            service.onError("fail to clean old data : " + item.getAbsolutePath());
                        }
                    }
                }else{
                    if ( !linesDir.mkdir() ){
                        service.onError("fail to make directory : " + linesDir.getAbsolutePath());
                        return false;
                    }
                }
                status = "路線データを展開してます...";
                for ( int i = 0; i < lines.length(); i++ ){
                    //詳細データは個別ファイルへ
                    //概略データを集めてひとつのファイルへ
                    JSONObject item = lines.getJSONObject(i);
                    int code = item.getInt("code");
                    service.writeFile(new File(linesDir, String.format("%s.json", code)), item.toString());
                    item.remove("station_list");
                    if ( item.has("polyline_list") ){
                        item.remove("polyline_list");
                        item.remove("north");
                        item.remove("south");
                        item.remove("east");
                        item.remove("west");
                    }
                    progress = 100 * i / lines.length();
                    publishProgress();
                }
                service.writeFile(new File(file, "lines.json"), lines.toString());
                service.log("lines > size : " + lines.length());
                //駅一覧データの展開
                //一覧データ(主に検索用)
                status = "駅データを展開してます...";
                JSONArray stations = data.getJSONArray("stations");
                service.writeFile(new File(file, "stations.json"), stations.toString());
                SparseArray<JSONObject> stationMap = new SparseArray<>();
                for ( int i = 0; i < stations.length(); i++ ){
                    JSONObject item = stations.getJSONObject(i);
                    int code = item.getInt("code");
                    stationMap.put(code, item);
                    progress = 100 * i / stations.length();
                    if ( i % 10 == 0 ) publishProgress();
                }
                service.log("stations > size : " + stations.length());
                //範囲ブロックの展開
                status = "駅境界データを展開してます...";
                JSONArray segments = data.getJSONArray("tree_segments");
                File stationsDir = new File(file, "tree");
                //clean
                if ( stationsDir.exists() && stationsDir.isDirectory() ){
                    for ( File item : stationsDir.listFiles(new FileFilter(){
                        @Override
                        public boolean accept(File pathname){
                            return pathname.isFile() && pathname.getAbsolutePath().endsWith(".json");
                        }
                    }) ){
                        if ( !item.delete() ){
                            service.onError("fail to clean old data : " + item.getAbsolutePath());
                        }
                    }
                }else{
                    if ( !stationsDir.mkdir() ){
                        service.onError("fail to make directory : " + stationsDir.getAbsolutePath());
                        return false;
                    }
                }
                for ( int i = 0; i < segments.length(); i++ ){
                    JSONObject segment = segments.getJSONObject(i);
                    String name = segment.getString("name");
                    JSONArray list = segment.getJSONArray("node_list");
                    JSONArray newList = new JSONArray();
                    for ( int k = 0; k < list.length(); k++ ){
                        JSONObject node = list.getJSONObject(k);
                        if ( !node.has("segment") ){
                            int code = node.getInt("code");
                            JSONObject station = stationMap.get(code);
                            if ( station == null ){
                                service.onError("station code not found : " + code + " at " + path);
                                return false;
                            }
                            if ( node.has("right")) station.put("right", node.getInt("right"));
                            if ( node.has("left")) station.put("left", node.getInt("left"));
                            node = station;
                            stationMap.remove(code);
                        }

                        newList.put(node);
                    }
                    segment.put("node_list", newList);
                    service.writeFile(new File(stationsDir, name + ".json"), segment.toString());
                    progress = 100 * i / segments.length();
                    publishProgress();
                }
                service.log("load station block > size : " + segments.length());

                service.log("succeed to update data. version:" + service.mDataVersion);
                service.mDataInitialized = service.checkData();
                if ( service.mDataInitialized ){
                    service.mDataVersion = version;
                    service.saveSetting();
                }
                return service.mDataInitialized;
            }catch( Exception e ){
                service.onError("exception during update", e);
            }

            return false;
        }

        @Override
        protected void onProgressUpdate(Void... values){
            listener.onProgress(progress, status);
        }

        @Override
        protected void onPostExecute(Boolean result){
            listener.onComplete(result);
            service = null;
            listener = null;
        }

        @Override
        protected void onCancelled(){
            service = null;
            listener = null;
        }

    }

    void updateData(final long version, final String path, final DataUpdateListener listener){
        new DataUpdateTask(this, listener, path, version).execute();
    }

    private void writeFile(File file, String data) throws IOException{
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
        writer.write(data);
        writer.close();
    }

    private String getHttp(String path, ProgressListener listener){
        try{
            URL url = new URL(path);
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setConnectTimeout(1000);
            connection.setReadTimeout(10000);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept-Encoding", "identity"); // <--- Add this line
            connection.connect();
            int result = connection.getResponseCode();
            if ( result == 200 ){
                final int size = connection.getContentLength();
                StringBuilder builder = new StringBuilder();
                String encoding = connection.getContentEncoding();
                if ( encoding == null ) encoding = "utf-8";
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), encoding));
                String line = reader.readLine();
                int length = 0;
                while ( line != null ){
                    builder.append(line);
                    length += line.getBytes(encoding).length;
                    line = reader.readLine();
                    if ( line != null ) builder.append('\n');
                    if ( listener != null ){
                        listener.onProgress(length, size);
                    }
                }
                reader.close();
                return builder.toString();
            }else{
                Log.e("HttpGET", "status code = " + result);
            }
        }catch( Exception e ){
            onError("exception during getHttp", e);
        }
        return null;
    }

    private void updateNotification(String title, String text){
        mNotification.updateNotification(title, text);
    }

    void setTimer(){
        if ( mTimerRunning ){
            mPopupNotification.onTimerStateChanged(true);
            Toast.makeText(this, getString(R.string.timer_running_toast), Toast.LENGTH_SHORT).show();
            return;
        }
        Intent timer = new Intent(AlarmClock.ACTION_SET_TIMER)
                .putExtra(AlarmClock.EXTRA_MESSAGE, getString(R.string.timer_message))
                .putExtra(AlarmClock.EXTRA_LENGTH, 300)
                .putExtra(AlarmClock.EXTRA_SKIP_UI, true);
        AlarmManager manager = (AlarmManager)getSystemService(ALARM_SERVICE);
        if ( manager != null && timer.resolveActivity(getPackageManager()) != null ){
            startActivity(timer);
            mPopupNotification.onTimerStateChanged(true);
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MINUTE, 5);
            PendingIntent pendingIntent = PendingIntent.getService(
                    getApplicationContext(), 5,
                    new Intent(getApplicationContext(), StationService.class).putExtra(REQUEST_KEY, REQUEST_TIMER_FINISH),
                    PendingIntent.FLAG_CANCEL_CURRENT
            );
            manager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            mTimerRunning = true;
            Toast.makeText(this, getString(R.string.timer_set_toast), Toast.LENGTH_SHORT).show();
        }else{
            Log.e("timer", "cannot resolve activity.");
            log("timer > cannot resolve activity");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startID){

        //実装上複数回呼ばれる可能性あり
        if ( mPopupNotification == null ){
            log("onStartCommand > service gets started");
            mPopupNotification = new OverlayNotification(this, this);
            registerReceiver(mPopupNotification, new IntentFilter(Intent.ACTION_SCREEN_ON));
            registerReceiver(mPopupNotification, new IntentFilter(Intent.ACTION_SCREEN_OFF));

            updateNotification(getString(R.string.notification_wait_title), getString(R.string.notification_wait_text));

        }


        if ( intent.getBooleanExtra(KEY_CLOSE_NOTIFICATION_PANEL, false) ){
            sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        }

        if ( intent.hasExtra(REQUEST_KEY) ){
            switch ( intent.getStringExtra(REQUEST_KEY) ){
                case REQUEST_EXIT:
                    stop();
                    if ( mMainActivity == null ){
                        stopSelf();
                    }else{
                        mMainActivity.onFinishApp();
                    }
                    break;
                case REQUEST_TIMER:
                    setTimer();
                    break;
                case REQUEST_TIMER_FINISH:
                    mTimerRunning = false;
                    mPopupNotification.onTimerStateChanged(false);
                default:
            }

        }

        return START_STICKY;
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent){
        log("onBind > client requests to bind service");
        return mBinder;
    }

    void onActivityBound(MainActivity activity){
        mMainActivity = activity;
    }

    String getNotificationChannelID(){
        return mNotification.getNotificationChannelID();
    }

    boolean needNotificationSetting(){
        return mShowNotificationSetting && mNotification.checkNotificationChannel();
    }

    @Override
    public boolean onUnbind(Intent intent){
        log("onUnbind > clients unbind this service");
        mMainActivity = null;
        return true;
    }

    @Override
    public void onRebind(Intent intent){
        log("onRebind > client binds this service again");
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        stop();
        mTimer.cancel();
        mTimer = null;
        mMainHandler = null;

        saveSetting();
        writeErrorLog();

        if ( mExplorer != null ){
            mExplorer.release();
            mExplorer = null;
        }
        if ( mStationTreeRoot != null ){
            mStationTreeRoot.release();
            mStationTreeRoot = null;
        }
        mCallbacks.clear();
        if ( mPopupNotification != null ){
            unregisterReceiver(mPopupNotification);
            mPopupNotification.release();
            mPopupNotification = null;
        }

        mGPSService.release();
        mStatusListener = null;

        mNotification.release();
        mNotification = null;


        if ( mPredictor != null ){
            mPredictor.release();
            mPredictor = null;
        }

        mMainActivity = null;
        mBinder = null;

        if ( mExceptionHandler != null ){
            mExceptionHandler.release();
            mExceptionHandler = null;
        }

    }

    void saveSetting(){
        SharedPreferences preferences = getSharedPreferences(getString(R.string.preference_name_backup), MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(KEY_VIBRATE, mIsVibrate);
        editor.putInt(KEY_RADAR, mRadarNum);
        editor.putInt(KEY_INTERVAL, mInterval);
        editor.putBoolean(KEY_NOTIFY, mIsNotifyUpdate);
        editor.putBoolean(KEY_FORCE_NOTIFY, mForceNotify);
        editor.putInt(KEY_BRIGHTNESS, mBrightness);
        editor.putBoolean(KEY_KEEP_NOTIFICATION, mKeepNotification);
        editor.putBoolean(KEY_NOTIFY_PREFECTURE, mIsNotifyPrefecture);
        editor.putInt(KEY_VIBRATE_METER, mVibrateMeter);
        editor.putBoolean(KEY_VIBRATE_APPROACH, mVibrateApproach);
        editor.apply();

        preferences = getSharedPreferences(getString(R.string.preference_name), MODE_PRIVATE);
        editor = preferences.edit();
        editor.putBoolean(KEY_NOTIFICATION_SETTING, mShowNotificationSetting);
        editor.putLong(KEY_DATA_VERSION, mDataVersion);
        editor.apply();

    }

    int getInterval(){
        return mInterval;
    }

    void setInterval(int value, boolean immediate){
        if ( value <= 0 ) value = 1;
        if ( mInterval != value ){
            mInterval = value;
        }
        if ( immediate && mRunning ){
            requestLocationUpdate(this, mInterval);
        }
    }

    void setVibrateMeter(int meter){
        mVibrateMeter = meter;
    }

    int getVibrateMeter(){
        return mVibrateMeter;
    }

    Handler getMainHandler(){
        return mMainHandler;
    }

    long getDataVersion(){
        return mDataVersion;
    }

    boolean isVibrate(){
        return mIsVibrate;
    }

    void setVibrate(boolean value){
        mIsVibrate = value;
    }

    boolean isVibrateApproach(){
        return mVibrateApproach;
    }

    void setVibrateApproach(boolean value){
        mVibrateApproach = value;
    }

    boolean isNotifyForce(){
        return mForceNotify;
    }

    void setForceNotify(boolean force){
        mForceNotify = force;

        mPopupNotification.setNotifyMode(force, mKeepNotification);
    }

    void setKeepNotification(boolean value){
        mKeepNotification = value;
        mPopupNotification.setNotifyMode(mForceNotify, value);
    }

    boolean isKeepNotification(){
        return mKeepNotification;
    }

    int getRadarNum(){
        return mRadarNum;
    }

    void setRadarNum(int k){
        if ( k < 1 ) k = 1;
        if ( k != mRadarNum ){
            mRadarNum = k;
            mExplorer.setSearchProperty(k, 0);
            if ( isRunning() && hasValidLocation() ){
                mExplorer.updateLocation(getLongitude(), getLatitude());
            }
        }
    }

    void setNotify(boolean isNotify){
        mIsNotifyUpdate = isNotify;
    }

    boolean isNotifyUpdate(){
        return mIsNotifyUpdate;
    }

    boolean isNotifyPrefecture(){
        return mIsNotifyPrefecture;
    }

    void setNotifyPrefecture(boolean value){
        mIsNotifyPrefecture = value;
        mPopupNotification.setDisplayPrefecture(value);
    }

    void toggleFixedTimer(){
        mPopupNotification.toggleFixedTimer();
    }

    @Nullable
    Line getCurrentLine(){
        return mCurrentLine;
    }

    boolean isCurrentLineSet(){
        return mCurrentLine != null;
    }

    void setCurrentLine(@Nullable Line line, boolean toast){
        if ( mCurrentLine != null && mCurrentLine.equals(line) ) return;

        stopLinePrediction(toast);
        mCurrentLine = line;
        for ( StationCallback callback : mCallbacks ) callback.onLineUpdate(line);
        if ( toast ){
            if ( line == null ){
                Toast.makeText(this, getString(R.string.line_select_null), Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(this, String.format(Locale.US, getString(R.string.line_select), line.mName), Toast.LENGTH_SHORT).show();
            }
        }


    }

    void startLinePrediction(){
        Line line = getCurrentLine();
        if ( line == null ) return;
        if ( !line.hasDetails() ){
            line = getLine(line.mCode, true);
        }
        if ( !line.hasPolyline() ){
            Toast.makeText(getApplicationContext(), getString(R.string.message_no_polyline), Toast.LENGTH_SHORT).show();
            return;
        }
        mPredictor = new PositionPredictor(this, line);
        mPredictor.setCallback(this);
        mPopupNotification.onPredictionStart(line);
        mHasApproached = true;
        mNextStation = null;
        Toast.makeText(
                getApplicationContext(),
                String.format(Locale.US, getString(R.string.notification_start_prediction), line.mName),
                Toast.LENGTH_LONG
        ).show();
    }

    void stopLinePrediction(boolean toast){
        if ( mPredictor != null ){
            if ( toast ){
                Toast.makeText(
                        getApplicationContext(),
                        String.format(Locale.US, getString(R.string.notification_stop_prediction), mPredictor.target.mName),
                        Toast.LENGTH_LONG
                ).show();
            }
            mPredictor.release();
            mPredictor = null;
            setCurrentLine(null, false);
            mPopupNotification.onPredictionStop();
        }
    }

    boolean isLinePredictionRunning(){
        return mPredictor != null;
    }

    @Override
    public void onLocationUpdate(Location location){
        if ( location != null && mRunning ){
            double longitude = location.getLongitude();
            double latitude = location.getLatitude();
            log(longitude, latitude, null);
            for ( StationCallback callback : mCallbacks ){
                callback.onLocationUpdate(longitude, latitude);
            }
            if ( mPopupNotification != null ){
                mPopupNotification.onLocationUpdate(location);
            }

            mExplorer.updateLocation(longitude, latitude);
            if ( mExplorer.hasInitialized() ){
                Station station = getCurrentStation();
                String distance = DistanceRuler.formatDistance(mDisplayRuler.measureDistance(station, longitude, latitude));
                String date = mLatestStationDetected == null ? "??:??" : new SimpleDateFormat("HH:mm", Locale.US).format(mLatestStationDetected);
                updateNotification(
                        String.format(Locale.US, "%s駅   %s", station.name, date),
                        String.format(Locale.US, "%s  %s", distance, station.getLinesName())
                );


                if ( mPredictor != null ){
                    mPredictor.onLocationUpdate(location, station);
                }

            }

        }
    }

    /* call back from position-predictor */

    @Override
    public void onApproachStations(@NonNull PositionPredictor.PredictionResult result){
        if ( result.size > 0 ){
            Station next = result.getStation(0);
            if ( mNextStation == null || !mNextStation.equals(next) ){
                mNextStation = next;
                mHasApproached = false;
            }else{
                if ( isVibrateApproach() && !mHasApproached && result.getDistance(0) < getVibrateMeter() ){
                    vibrate(StationService.VIBRATE_PATTERN_APPROACH);
                    mHasApproached = true;
                }
            }
        }
        mPopupNotification.onApproachStations(result);
    }


    @Override
    public void onResolutionRequired(ResolvableApiException exception){
        for ( StationCallback callback : mCallbacks ){
            callback.onResolutionRequired(exception);
        }
    }

    @Override
    public void onGPSStop(String mes){
        onError(mes);
    }

    void start(){
        if ( mRunning || !mDataInitialized ){
            return;
        }
        if ( mPopupNotification == null ){
            onError("window not initialized yet.");
            return;
        }
        updateNotification(getString(R.string.notification_start_title), getString(R.string.notification_start_text));
        log("start > getting GPS ready");
        mExplorer.setSearchProperty(mRadarNum, 0);
        mRunning = true;

        log(String.format(Locale.US, "start > start task, minInterval:%ds", mInterval));

        mExplorer.setStationUpdateCallback(this);
        requestLocationUpdate(this, mInterval);


    }

    void stop(){
        if ( stopSearch() ){
            log("stop > client requests task being stopped");
            for ( StationCallback callback : mCallbacks ){
                callback.onSearchStop(getString(R.string.message_stop));
            }
        }
    }

    private boolean stopSearch(){
        if ( mRunning ){
            mRunning = false;
            setCurrentLine(null, false);
            updateNotification(getString(R.string.notification_wait_title), getString(R.string.notification_wait_text));
            removeLocationUpdate(this);
            mExplorer.stop();
            mPopupNotification.onNotificationRemoved(null);
            stopLinePrediction(false);
            Toast.makeText(this, getString(R.string.message_stop), Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    List<Station> getNearStation(){
        return mRunning ? mExplorer.getNearStations() : null;
    }

    @NonNull
    Station getNearStation(int index){
        return mExplorer.getNearStation(index);
    }

    List<Line> getNearLines(int size){
        return mRunning ? mExplorer.getNearLines() : null;
    }

    final static long[] VIBRATE_PATTERN_NORMAL = {0, 500, 100, 100};
    final static long[] VIBRATE_PATTERN_ALERT = {0, 500, 100, 100, 100, 100, 100, 100};
    final static long[] VIBRATE_PATTERN_APPROACH = {0, 100, 100, 100, 100, 100};

    private void vibrate(long[] pattern){
        if ( mIsVibrate && mVibrator.hasVibrator() ){
            mVibrator.vibrate(pattern, -1);
        }
    }


    @Override
    public synchronized void onDetectStation(@Nullable Station station){

        if ( mCurrentLine != null && station != null && !station.isLine(mCurrentLine) ){
            vibrate(VIBRATE_PATTERN_ALERT);
        }else{
            vibrate(VIBRATE_PATTERN_NORMAL);
        }

        mLatestStationDetected = new Date();
        log("detect > " + (station == null ? "null" : station.name));
        log(getLongitude(), getLatitude(), station);

        for ( StationCallback callback : mCallbacks ){
            callback.onStationUpdate(station);
        }


        if ( station == null ){
            updateNotification(getString(R.string.notification_null_title), getString(R.string.notification_null_text));
            log("notifyStation > no station found : not in Japan?");
        }else if ( mIsNotifyUpdate ){
            mPopupNotification.requireNotification(station);
        }
    }

    @Override
    public void onExploreStop(String mes){
        onError(mes);
    }

    @NonNull
    String getPrefecture(int code){
        if ( code < 1 || code > 47 ) return "unknown";
        if ( mPrefectureName == null ){
            //都道府県コード
            try{
                Resources res = getResources();
                InputStream stream = res.openRawResource(R.raw.prefecture);
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
                String[] array = new String[48];
                for ( int i = 1; i <= 47; i++ ){
                    String line = reader.readLine();
                    String[] data = line.split(",");
                    int id = Integer.parseInt(data[0]);
                    if ( id != i || data.length != 2 ){
                        onError("fail to read prefecture : " + line);
                        return "";
                    }
                    array[i] = data[1];
                }
                mPrefectureName = array;
            }catch( Exception e ){
                onError("during getPrefecture", e);
                return "";
            }
        }
        return mPrefectureName[code];
    }

    void setNightMood(boolean enable){
        mPopupNotification.setNightMood(enable);
        mNightMood = enable;
    }

    void setBrightness(int value){
        if ( value < 3 ) value = 3;
        if ( value > 255 ) value = 255;
        mBrightness = value;
        mPopupNotification.setBrightness(value);
    }

    int getBrightness(){
        return mBrightness;
    }

    boolean isNightMood(){
        return mNightMood;
    }

    boolean isRunning(){
        return mRunning;
    }


    void log(String mes){
        Log.d("Log", mes);
        mLogHolder.append(mes, false);
        if ( mStatusListener != null ){
            mStatusListener.onMessage(mLogHolder.getLatest());
        }
    }

    private void log(double lon, double lat, Station station){
        mLogHolder.append(lon, lat, station);
        LogHolder.ServiceLog log = mLogHolder.getLatest();
        Log.d("Log", log.toString());
        if ( mStatusListener != null ){
            mStatusListener.onMessage(log);
        }
    }

    void onError(String mes, Throwable cause){
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        cause.printStackTrace(pw);
        onError(mes + "\n" + sw.toString());
    }

    void onError(final String mes){
        Log.e("Error",mes);
        mLogHolder.append(mes, true);
        if ( mStatusListener != null ){
            mStatusListener.onMessage(mLogHolder.getLatest());
        }
        if ( mMainHandler == null ) return;
        mMainHandler.post(new Runnable(){
            @Override
            public void run(){
                stopSearch();
                for ( StationCallback callback : mCallbacks ){
                    callback.onSearchStop(mes);
                }
            }
        });
    }

    void writeErrorLog(){
        if ( !mLogHolder.hasError()) return;
        List<LogHolder.ServiceLog> list = mLogHolder.getLogList(LogHolder.LOG_SYSTEM);
        StringBuilder builder = new StringBuilder();
        String time = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).format(new Date());
        builder.append(getString(R.string.app_name));
        builder.append("\ntime : ");
        builder.append(time);
        for ( LogHolder.ServiceLog log : list ){
            builder.append("\n");
            builder.append(log.toString());
        }
        String fileName = String.format(Locale.US, "ErrorLog_%s.txt", time);
        try{
            writeFile(new File(getExternalFilesDir(null), fileName), builder.toString());
        }catch( IOException  e){
            throw new AppException("fail to write", e);
        }
    }

    void onAppCrash(Throwable cause) throws IOException {
        cause.printStackTrace();
        List<LogHolder.ServiceLog> list = mLogHolder.getLogList(LogHolder.LOG_SYSTEM);
        StringBuilder builder = new StringBuilder();
        String time = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).format(new Date());
        builder.append(getString(R.string.app_name));
        builder.append("\ntime : ");
        builder.append(time);
        builder.append("\n");
        for ( LogHolder.ServiceLog log : list ){
            builder.append(log.toString());
            builder.append("\n");
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        cause.printStackTrace(pw);
        builder.append(sw.toString());
        String fileName = String.format(Locale.US, "ExceptionLog_%s.txt", time);
        writeFile(new File(getExternalFilesDir(null), fileName), builder.toString());
    }

    LogHolder getLogHolder(){
        return mLogHolder;
    }

    boolean hasExplorerInitialized(){
        return isRunning() && mExplorer.hasInitialized();
    }

    @NonNull
    Station getCurrentStation(){
        if ( !hasExplorerInitialized() ) throw new IllegalStateException("Search not started.");
        return mExplorer.getCurrentStation();
    }


    private SparseArray<Line> mLineMap;
    private SparseArray<Station> mStationMap;
    private SparseArray<StationPoint> mStationPointMap;
    private final Object STATION_ACCESS_LOCK = new Object();

    interface ResultCallback<P>{
        void onGetResult(P result);
    }

    /**
     * Gets line object. Details as polyline and station-list NOT supplied.
     *
     * @param id identifier of line
     * @return line object with base information\
     */
    @Override
    @NonNull
    public Line getLine(int id){
        return getLine(id, false);
    }

    void getLine(final int id, final boolean details, final ResultCallback<Line> callback){
        new Thread(new Runnable(){
            @Override
            public void run(){
                final Line line = getLine(id, details);
                getMainHandler().post(new Runnable(){
                    @Override
                    public void run(){
                        callback.onGetResult(line);
                    }
                });
            }
        }).start();
    }

    @NonNull
    Line getLine(int id, boolean details){
        synchronized( STATION_ACCESS_LOCK ){
            int index = mLineMap.indexOfKey(id);
            if ( index < 0 ){
                throw new AppException("invalid line id:" + id);
            }else{
                Line line = mLineMap.valueAt(index);
                if ( details && !line.hasDetails() ){
                    //set details about the line...
                    File file = new File(getFilesDir(), String.format(Locale.US, "lines/%d.json", line.mCode));
                    try{
                        JSONObject data = new JSONObject(readFile(file));
                        line.setDetails(data, this);
                    }catch( Exception e ){
                        throw new AppException("fail to set details to line:" + id, e);
                    }
                }
                return line;
            }
        }
    }

    @Override
    @NonNull
    public Station[] getStations(int[] code){
        synchronized( STATION_ACCESS_LOCK ){
            Station[] result = new Station[code.length];
            JSONArray array = null;
            for ( int i = 0; i < code.length; i++ ){
                int index = mStationMap.indexOfKey(code[i]);
                if ( index < 0 ){
                    try{
                        if ( array == null ){
                            File file = new File(getFilesDir(), "stations.json");
                            array = new JSONArray(readFile(file));
                        }
                        for ( int j = 0; j < array.length(); j++ ){
                            JSONObject item = array.getJSONObject(j);
                            if ( item.getInt("code") == code[i] ){
                                Station s = new Station(item, this);
                                result[i] = s;
                                mStationMap.put(code[i], s);
                                break;
                            }
                        }
                        if ( result[i] != null ) continue;
                    }catch( Exception e ){
                        throw new AppException("fail to read stations :" + Arrays.toString(code), e);
                    }
                    result[i] = new Station(code[i]);
                }else{
                    result[i] = mStationMap.valueAt(index);
                }
            }
            return result;

        }
    }

    @NonNull
    Station getStation(int code){
        synchronized( STATION_ACCESS_LOCK ){
            int index = mStationMap.indexOfKey(code);
            if ( index < 0 ){
                File file = new File(getFilesDir(), "stations.json");
                try{
                    JSONArray array = new JSONArray(readFile(file));
                    for ( int j = 0; j < array.length(); j++ ){
                        JSONObject item = array.getJSONObject(j);
                        if ( item.getInt("code") == code ){
                            Station s = new Station(item, this);
                            mStationMap.put(code, s);
                            return s;
                        }
                    }
                }catch( Exception e ){
                    throw new AppException("fail to read station : " + code, e);
                }
                return new Station(code);
            }else{
                return mStationMap.valueAt(index);
            }
        }
    }

    @NonNull
    StationPoint getStationPoint(int code){
        synchronized( STATION_ACCESS_LOCK ){
            if ( mStationPointMap == null ){
                int index = mStationMap.indexOfKey(code);
                if ( index < 0 ){
                    File file = new File(getFilesDir(), "stations.json");
                    mStationPointMap = new SparseArray<>();
                    try{
                        JSONArray array = new JSONArray(readFile(file));
                        for ( int j = 0; j < array.length(); j++ ){
                            JSONObject item = array.getJSONObject(j);
                            StationPoint point = new StationPoint(item);
                            mStationPointMap.put(point.code, point);
                        }
                        return getStationPoint(code);
                    }catch( Exception e ){
                        throw new AppException("fail to read station location:" + code, e);
                    }
                }else{
                    Station s = mStationMap.valueAt(index);
                    return new StationPoint(s);
                }
            }else{
                StationPoint point = mStationPointMap.get(code);
                if ( point == null ) throw new RuntimeException();
                return point;
            }
        }
    }

    @NonNull
    StationKdTree.StationNode getStationTreeRoot(){
        return mStationTreeRoot;
    }


    @NonNull
    JSONObject getStationTreeSegment(String name){
        try{
            File file = new File(getFilesDir(), String.format(Locale.US, "tree/%s.json", name));
            if ( file.exists() ){
                JSONObject data = new JSONObject(readFile(file));
                JSONArray list = data.getJSONArray("node_list");
                for ( int i=0 ; i<list.length() ; i++ ){
                    JSONObject object = list.getJSONObject(i);
                    if ( !object.has("segment") ){
                        Station s = new Station(object, this);
                        mStationMap.put(s.code, s);
                    }
                }
                return data;
            }else{
                throw new AppException("station tree segment not found. name:" + name);
            }
        } catch( Exception e ){
            throw new AppException("fail to read station tree segment.", e);
        }
    }

    private String readFile(File file) throws IOException{
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
        StringBuilder builder = new StringBuilder();
        String in = reader.readLine();
        while ( in != null ){
            builder.append(in);
            in = reader.readLine();
        }
        return builder.toString();
    }



    private static class EntryAdapter<E> implements Comparable<EntryAdapter>{

        EntryAdapter(int id, E value){
            mValue = value;
            mCreateTime = SystemClock.uptimeMillis();
            mKey = id;
        }

        final int mKey;
        final E mValue;
        final long mCreateTime;

        boolean mFixed = false;

        @Override
        public int compareTo(@NonNull EntryAdapter o){
            if ( mFixed && o.mFixed ){
                return 0;
            }else if ( mFixed ){
                return 1;
            }else if ( o.mFixed ){
                return -1;
            }else{
                return Long.compare(this.mCreateTime, o.mCreateTime);
            }
        }
    }

}
