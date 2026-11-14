package com.oblonian.stracker.collect

import com.oblonian.stracker.domain.Motion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** What the phone currently thinks the owner is doing; shared between the collectors. */
object CollectorState {
    private val _motion = MutableStateFlow(Motion.UNKNOWN)
    val motion: StateFlow<Motion> = _motion

    fun update(motion: Motion) {
        _motion.value = motion
    }
}
