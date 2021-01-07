package jp.seo.station.ekisagasu.utils


/**
 * @author Seo-4d696b75
 * @version 2020/12/16.
 */
class SingletonHolder<T, E>(
    val creator: (E) -> T
) {
    @Volatile
    private var instance: T? = null

    fun get(arg: E): T {
        synchronized(this) {
            return instance ?: let {
                val i = creator(arg)
                instance = i
                return i
            }
        }
    }
}
