package com.seo4d696b75.android.ekisagasu.data.cache

import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@ActivityRetainedScoped
internal class MemoryCacheStore @Inject constructor() {
    private val map = mutableMapOf<String, MutableStateFlow<*>>()

    @Suppress("unchecked_cast")
    inline operator fun <reified T : Any> get(
        key: String?,
    ): MutableStateFlow<CacheState<T>> = synchronized(this) {
        val internalKey = "${T::class.java.name}@${key ?: "default"}"
        map.getOrPut(internalKey) {
            MutableStateFlow(CacheState(value = null, isDirty = false, isLoading = false))
        } as MutableStateFlow<CacheState<T>>
    }
}
