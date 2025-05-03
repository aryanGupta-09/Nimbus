package com.example.nimbus.data.worker

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton manager for coordinating background refresh events.
 * This provides a direct communication channel between background workers and UI components.
 */
object BackgroundRefreshManager {
    // Incremented timestamp for each refresh event
    private val _refreshTimestamp = MutableStateFlow(0L)
    val refreshTimestamp: StateFlow<Long> = _refreshTimestamp.asStateFlow()
    
    // Flag indicating whether refresh is in progress
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    /**
     * Notify that a background refresh has started
     */
    fun notifyRefreshStarted() {
        _isRefreshing.value = true
    }
    
    /**
     * Notify that a background refresh has completed.
     * This will trigger UI updates in components observing the refreshTimestamp.
     */
    fun notifyRefreshCompleted() {
        _isRefreshing.value = false
        _refreshTimestamp.value = System.currentTimeMillis()
    }
}