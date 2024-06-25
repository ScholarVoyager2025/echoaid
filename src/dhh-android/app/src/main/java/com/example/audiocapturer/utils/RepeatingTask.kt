package com.example.audiocapturer.utils

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class RepeatingTask(
    private val intervalMillis: Long,
    val runAtStart: Boolean = false,
    private val coroutineContext: CoroutineContext = Dispatchers.Default,
    private val task: suspend () -> Unit,
) {
    private var job: Job? = null

    fun start() {
        stop() // 确保在启动前没有正在运行的任务
        job = CoroutineScope(coroutineContext).launch {
            if (runAtStart) task()
            while (isActive) { // 检查协程是否仍然活跃
                delay(intervalMillis)
                task()
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    fun restart() {
        start()
    }
}
