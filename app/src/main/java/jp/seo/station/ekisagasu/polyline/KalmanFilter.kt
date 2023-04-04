package jp.seo.station.ekisagasu.polyline

import org.apache.commons.math3.linear.MatrixUtils
import org.apache.commons.math3.linear.RealMatrix

/**
 * @author Seo-4d696b75
 * @version 2020/06/28.
 */
class KalmanFilter {

    companion object {

        /**
         * https://ja.wikipedia.org/wiki/%E8%B5%B7%E5%8B%95%E5%8A%A0%E9%80%9F%E5%BA%A6
         * 電車の起動加速度はせいぜい5.0km/h/s
         * 標準正規分布の累積確率分布関数で97.5%点は1.97
         * 標準偏差2.5km/h/sぐらい ~ 1.0m/s^2 のオーダー
         */
        const val SIGMA = 1.0
    }

    class Sample(
        val time: Long,
        val pos: Double,
        val speed: Double,
        val acceleration: Double,
        var state: RealMatrix,
        var p: RealMatrix
    ) : Cloneable {

        @Throws(CloneNotSupportedException::class)
        public override fun clone(): Sample {
            val other = super.clone() as Sample
            other.state = state.copy()
            other.p = p.copy()
            return other
        }

        companion object {
            /**
             * 初期化した状態オブジェクトを返す
             * @param time 時刻
             * @param pos 観測現在位置
             * @param speed 観測現在速度, 観測値欠損の場合は0（適当な値）
             * @param variance 位置・速度の観測値の平均分散>=0、値が大きいほど観測精度が低いと解釈される
             * @return
             */
            fun initialize(time: Long, pos: Double, speed: Double, variance: Double): Sample {
                val v = variance.coerceAtLeast(0.0)
                val state = MatrixUtils.createRealMatrix(
                    arrayOf(
                        doubleArrayOf(pos),
                        doubleArrayOf(speed),
                        doubleArrayOf(0.0)
                    )
                )
                val p = MatrixUtils.createRealMatrix(
                    arrayOf(
                        doubleArrayOf(v, 0.0, 0.0),
                        doubleArrayOf(0.0, v, 0.0),
                        doubleArrayOf(0.0, 0.0, v)
                    )
                )
                return Sample(time, pos, speed, 0.0, state, p)
            }
        }
    }

    private val mH: RealMatrix = MatrixUtils.createRealMatrix(arrayOf(doubleArrayOf(1.0, 0.0, 0.0)))

    fun update(pos: Double, err: Double, time: Long, latest: Sample): Sample {
        val delta = (time - latest.time) / 1000.0
        val f = MatrixUtils.createRealMatrix(
            arrayOf(
                doubleArrayOf(1.0, delta, delta * delta / 2),
                doubleArrayOf(0.0, 1.0, delta),
                doubleArrayOf(0.0, 0.0, 1.0)
            )
        )
        val g = MatrixUtils.createRealMatrix(
            arrayOf(
                doubleArrayOf(delta * delta * delta / 6),
                doubleArrayOf(delta * delta / 2),
                doubleArrayOf(delta)
            )
        )
        // estimate
        val estimatedState = f.multiply(latest.state)
        val p = f.multiply(latest.p).multiply(f.transpose())
            .add(g.multiply(g.transpose()).scalarMultiply(SIGMA * SIGMA))
        // update
        val e = pos - mH.multiply(estimatedState).getEntry(0, 0)
        val s = err * err + mH.multiply(p).multiply(mH.transpose()).getEntry(0, 0)
        val k = p.multiply(mH.transpose()).scalarMultiply(1.0 / s)
        val nextState = estimatedState.add(k.scalarMultiply(e))
        val nextP = MatrixUtils.createRealIdentityMatrix(3).subtract(k.multiply(mH)).multiply(p)
        return Sample(
            time,
            nextState.getEntry(0, 0),
            nextState.getEntry(1, 0),
            nextState.getEntry(2, 0),
            nextState,
            nextP
        )
    }
}
