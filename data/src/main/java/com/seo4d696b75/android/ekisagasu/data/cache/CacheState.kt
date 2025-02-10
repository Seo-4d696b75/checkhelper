package com.seo4d696b75.android.ekisagasu.data.cache

internal data class CacheState<T : Any>(
    val value: T?,
    val isDirty: Boolean,
    val isLoading: Boolean,
) {
    val shouldRefresh: Boolean
        get() = (value == null || isDirty) && !isLoading
}
