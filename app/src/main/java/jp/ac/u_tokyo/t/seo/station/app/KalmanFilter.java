package jp.ac.u_tokyo.t.seo.station.app;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

/**
 * @author Seo-4d696b75
 * @version 2020/06/28.
 */
class KalmanFilter {


    /**
     https://ja.wikipedia.org/wiki/%E8%B5%B7%E5%8B%95%E5%8A%A0%E9%80%9F%E5%BA%A6
     電車の起動加速度はせいぜい5.0km/h/s
     標準正規分布の累積確率分布関数で97.5%点は1.97
     標準偏差2.5km/h/sぐらい ~ 1.0m/s^2 のオーダー
     */
    private final double SIGMA = 1.0;

    static class Sample implements Cloneable {

        /**
         * 初期化した状態オブジェクトを返す
         * @param time 時刻
         * @param pos 観測現在位置
         * @param speed 観測現在速度, 観測値欠損の場合は0（適当な値）
         * @param variance 位置・速度の観測値の平均分散>=0、値が大きいほど観測精度が低いと解釈される
         * @return
         */
        static Sample initialize(long time, double pos, double speed, double variance){
            variance = Math.max(variance, 0);
            Sample state = new Sample(time, pos, speed, 0);

            state.state = MatrixUtils.createRealMatrix(new double[][]{{pos}, {speed}, {0}});
            state.p = MatrixUtils.createRealMatrix(new double[][]{{variance,0,0},{0,variance,0},{0,0,variance}});
            return state;
        }


        private Sample(long time, double pos, double speed, double acceleration){
            this.time = time;
            this.pos = pos;
            this.speed = speed;
            this.acceleration = acceleration;
        }

        final long time;
        final double pos, speed, acceleration;
        private RealMatrix state, p;

        public Sample clone() throws CloneNotSupportedException {
            Sample other = (Sample)super.clone();
            other.state = state.copy();
            other.p = p.copy();
            return other;
        }

    }

    KalmanFilter(){
        mH = MatrixUtils.createRealMatrix(new double[][]{{1,0,0}});
    }

    private final RealMatrix mH;

    Sample update(double pos, double err, long time, Sample latest){
        double delta = (time - latest.time) / 1000.0;
        RealMatrix f = MatrixUtils.createRealMatrix(new double[][]{{1,delta,delta*delta/2},{0,1,delta},{0,0,1}});
        RealMatrix g = MatrixUtils.createRealMatrix(new double[][]{{delta*delta*delta/6},{delta*delta/2},{delta}});
        //estimate
        RealMatrix estimatedState = f.multiply(latest.state);
        RealMatrix p = f.multiply(latest.p).multiply(f.transpose()).add(g.multiply(g.transpose()).scalarMultiply(SIGMA*SIGMA));
        //update
        double e = pos - mH.multiply(estimatedState).getEntry(0,0);
        double s = err*err + mH.multiply(p).multiply(mH.transpose()).getEntry(0,0);
        RealMatrix k = p.multiply(mH.transpose()).scalarMultiply(1.0 / s);
        RealMatrix nextState = estimatedState.add(k.scalarMultiply(e));
        RealMatrix nextP = MatrixUtils.createRealIdentityMatrix(3).subtract(k.multiply(mH)).multiply(p);
        Sample next = new Sample(
                time,
                nextState.getEntry(0,0),
                nextState.getEntry(1,0),
                nextState.getEntry(2,0)
        );
        next.state = nextState;
        next.p = nextP;
        return next;
    }


}
