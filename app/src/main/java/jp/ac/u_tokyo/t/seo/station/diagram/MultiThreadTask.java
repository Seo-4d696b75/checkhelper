package jp.ac.u_tokyo.t.seo.station.diagram;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

/**
 * @author Seo-4d696b75
 * @version 2018/06/29.
 */

public abstract class MultiThreadTask<T,P>{

    private boolean mIsProcessing = false;
    private boolean mIsQueuing = false;
    private Queue<T> mQueue;
    private int mTaskCnt;
    private int mProcessedCnt;
    private int mThreadCnt;
    private long mTime;

    /**
     * 処理の開始を宣言します.
     * ここで処理を開始してから、マルチスレッドで処理したい{@link T}オブジェクトを{@link #enqueue(Object)}へ渡します.
     * 処理すべきオブジェクトがなくなったら{@link #stopEnqueue()}を呼び出します.
     * すると、その段階で待ち行列に加えられたすべてのオブジェクトが処理されてから
     * {@link #onComplete(int, long)}がコールバックされます.
     * @param threadNum 処理を行うスレッドの数 0以下の値は1と解釈します
     */
    public final void start(int threadNum){
        final int threads = threadNum > 0 ? threadNum : 1;
        synchronized ( this ){
            if ( mIsProcessing ){
                return;
            }else{
                mIsProcessing = true;
                mIsQueuing = true;
                mQueue = new LinkedList<>();
                mTaskCnt = 0;
                mProcessedCnt = 0;
                mThreadCnt = 0;
                mTime = System.currentTimeMillis();
            }
        }
        for ( int i=0 ; i<threads ; i++ ){
            startProcess();
        }
    }

    /**
     * 全ての{@link T}オブジェクトの処理が終了したときにコールされます.
     * <strong>NOTE </strong>{@link #stopEnqueue()}でタスクの追加の終了が宣言された段階で
     * 待ち行列に追加されていた全てのオブジェクトが処理されてから呼ばれるので、時間差が発生し得ます.
     * @param cnt 追加されたタスクの数=処理されたタスクの数
     * @param time 要した時間[ms]
     */
    protected void onComplete(int cnt, long time){}

    public final void enqueue(Collection<T> tasks){
        for ( T task : tasks ){
            enqueue(task);
        }
    }

    /**
     * 処理したいタスクを待ち行列に追加します.
     * <strong>NOTE</strong> {@link #start(int)}で処理の開始を宣言してから
     * {@link #stopEnqueue()}でタスクの追加の終了を宣言するまでの間のみ呼び出せます.
     * それ以外の不正なタイミングで呼ぶと例外を投げます.
     * @param task 処理すべきタスク nullは許容されません
     */
    public final synchronized void enqueue(T task){
        if ( mIsProcessing ){
            if ( mIsQueuing ) {
                if (task == null) {
                    throw new NullPointerException("task object does not accept null");
                } else {
                    mQueue.offer(task);
                    mTaskCnt++;
                    onEnqueued(task, mTaskCnt);
                    if (mQueue.size() == 1) {
                        notifyAll();
                    }
                }
            }else{
                throw new IllegalStateException("it has been declared that task queuing completed");
            }
        }else{
            throw new IllegalStateException("task can be enqueued only after processing gets started");
        }
    }

    /**
     * タスクが待ち行列に追加されたときにコールされます.
     * @param task 追加されたタスク NonNull
     * @param cnt これまでに追加されたタスクの数
     */
    protected void onEnqueued(T task, int cnt){}

    /**
     * 処理するタスクの追加の終了を宣言します.
     * {@link #enqueue(Object)}で追加するタスクがなくなったら呼んでください.
     * 待ち行列に残っているタスクを処理し終えた段階で{@link #onComplete(int, long)}がコールバックされます.
     * <strong>NOTE </strong>{@link #start(int)}で処理開始を宣言したあとに一回のみ呼び出せます.
     * それ以外の不正なタイミングで呼ぶと例外を投げます.
     */
    public final synchronized void stopEnqueue(){
        if ( mIsProcessing && mIsQueuing ){
            mIsQueuing = false;
            if ( mQueue.isEmpty() ){
                notifyAll();
            }
        }else{
            throw new IllegalStateException("task queuing stop can be declared once after queuing has been started");
        }
    }

    /**
     * {@link #stopEnqueue()}の操作後、すべてのスレッドでの初期が完了するまで待つ
     */
    public final synchronized void waitForCompletion(){
        stopEnqueue();
        while( mIsProcessing ){
            try{
                wait();
            }catch( InterruptedException e ){
                e.getMessage();
            }
        }
    }

    private synchronized T dequeue(){
        T task = mQueue.poll();
        while ( mIsQueuing || task != null ){
            if ( task != null ){
                return task;
            }
            try{
                wait();
            }catch (InterruptedException e){
                e.printStackTrace();
            }
            task = mQueue.poll();
        }
        return null;
    }

    /**
     * タスクを処理するスレッドオブジェクトのテンプレート
     */
    public abstract class ProcessThread extends Thread{

        @Override
        public final void run(){
            try{
                T task = dequeue();
                while ( task != null ){
                    onProcessed(task, process(task));
                    task = dequeue();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
            onProcessFinish();
        }

        /**
         * 与えれたタスクを処理する
         * @param task not null
         * @return タスクを処理して得たオブジェクト
         */
        protected abstract P process(T task);

    }

    /**
     * 実際のタスクの処理を定義したスレッドオブジェクトを取得します
     * @return 具体的な処理を定義したスレッド
     */
    protected abstract ProcessThread getProcessThread();

    private synchronized void startProcess(){
        mThreadCnt++;
        getProcessThread().start();
    }

    private synchronized void onProcessed(T task, P product){
        mProcessedCnt++;
        onProcessed(task, product, mProcessedCnt);
    }

    /**
     * タスクが処理される度にコールされます
     * @param task 処理されたオブジェクト
     * @param product 処理された結果得られたもの 処理の実装次第ではnull
     * @param cnt これまでに処理されたタスクの数
     */
    protected void onProcessed(T task, P product, int cnt){}

    private synchronized void onProcessFinish(){
        mThreadCnt--;
        if ( mThreadCnt == 0 ){
            mIsProcessing = false;
            onComplete(mProcessedCnt, System.currentTimeMillis() - mTime);
            notifyAll();
        }
    }

}
