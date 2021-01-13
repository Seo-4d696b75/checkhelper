package jp.seo.station.ekisagasu.utils

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.view.View

/**
 * @author Seo-4d696b75
 * @version 2021/01/13.
 */
class AnimationHolder<E : View>(
    val view: E,
    private val x: Float,
    private val y: Float
) {

    fun invalidate(expand: Boolean) {
        view.translationX = if (expand) x else 0f
        view.translationY = if (expand) y else 0f
        view.alpha = 1f
        view.scaleX = 1f
        view.scaleY = 1f
    }

    fun animate(expand: Boolean): ObjectAnimator = animate(expand, this.x, this.y)

    fun animate(expand: Boolean, x: Float, y: Float): ObjectAnimator {

        val scaleFrom = if (expand) 0.1f else 1f
        val scaleTo = if (expand) 1f else 0.1f
        val alphaFrom = if (expand) 0f else 1f
        val alphaTo = if (expand) 1f else 0f
        val srcX = if (expand) 0f else x
        val desX = if (expand) x else 0f
        val srcY = if (expand) 0f else y
        val desY = if (expand) y else 0f
        return ObjectAnimator.ofPropertyValuesHolder(
            view,
            PropertyValuesHolder.ofFloat("alpha", alphaFrom, alphaTo),
            PropertyValuesHolder.ofFloat("translationX", srcX, desX),
            PropertyValuesHolder.ofFloat("translationY", srcY, desY),
            PropertyValuesHolder.ofFloat("scaleX", scaleFrom, scaleTo),
            PropertyValuesHolder.ofFloat("scaleY", scaleFrom, scaleTo)
        )
    }


    fun animate(expand: Boolean, x: Float, effect: Boolean): ObjectAnimator {
        return if (effect) {
            val scaleFrom = if (expand) 0.1f else 1f
            val scaleTo = if (expand) 1f else 0.1f
            val alphaFrom = if (expand) 0f else 1f
            val alphaTo = if (expand) 1f else 0f
            val srcX = if (expand) x else 0f
            val desX = if (expand) 0f else x
            ObjectAnimator.ofPropertyValuesHolder(
                view,
                PropertyValuesHolder.ofFloat("alpha", alphaFrom, alphaTo),
                PropertyValuesHolder.ofFloat("translationX", srcX, desX),
                PropertyValuesHolder.ofFloat("scaleX", scaleFrom, scaleTo),
                PropertyValuesHolder.ofFloat("scaleY", scaleFrom, scaleTo)
            )
        } else {
            ObjectAnimator.ofFloat(
                view,
                "translationX",
                if (expand) 0f else x,
                if (expand) x else 0f
            )
        }
    }

    var visibility: Boolean
        get() = view.visibility == View.VISIBLE
        set(value) {
            view.visibility = if (value) View.VISIBLE else View.INVISIBLE
        }

}
