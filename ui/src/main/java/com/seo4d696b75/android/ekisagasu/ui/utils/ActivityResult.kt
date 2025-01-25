package com.seo4d696b75.android.ekisagasu.ui.utils

import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.app.ActivityOptionsCompat

@Composable
fun <I, O> rememberLauncherForActivityResult(
    contract: ActivityResultContract<I, O>,
): CallbackActivityResultLauncher<I, O> {
    val wrapper = remember { CallbackActivityResultLauncher<I, O>() }
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(contract) {
        wrapper.onResult(it)
    }
    wrapper.original = launcher
    return wrapper
}

class CallbackActivityResultLauncher<I, O> {
    lateinit var original: ManagedActivityResultLauncher<I, O>

    var onResult: (O) -> Unit = {}
        private set

    fun launch(
        input: I,
        options: ActivityOptionsCompat? = null,
        onResult: (O) -> Unit,
    ) {
        this.onResult = onResult
        original.launch(input, options)
    }

    val contract: ActivityResultContract<I, O>
        get() = original.contract
}
