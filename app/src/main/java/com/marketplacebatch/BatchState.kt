package com.marketplacebatch

/**
 * Lifecycle states for the batch coordinator.
 *
 * Kept in its own source file so the state type is always available to
 * every Kotlin source file that references it.
 */
enum class BatchState {
    IDLE,
    STARTING,
    WAITING_UI,
    RUNNING,
    SCANNING,
    REPLYING,
    SKIPPING,
    ACCOUNT_DONE,
    COMPLETE,
    STOPPED,
    ERROR
}
