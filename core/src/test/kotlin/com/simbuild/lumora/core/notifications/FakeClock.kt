package com.simbuild.lumora.core.notifications

/** Virtual clock that doubles as a [Scheduler]; time only moves via [advanceBy]. */
class FakeClock : Scheduler, TimeSource {
    private class Task(val dueAt: Long, val action: () -> Unit) {
        var cancelled = false
    }

    private var now = 0L
    private val tasks = mutableListOf<Task>()

    override fun nowMillis(): Long = now

    override fun schedule(delayMillis: Long, action: () -> Unit): Cancellable {
        val task = Task(now + delayMillis, action)
        tasks += task
        return Cancellable { task.cancelled = true }
    }

    val pendingCount: Int get() = tasks.count { !it.cancelled }

    fun advanceBy(millis: Long) {
        val target = now + millis
        while (true) {
            val next = tasks.filter { !it.cancelled && it.dueAt <= target }.minByOrNull { it.dueAt } ?: break
            now = next.dueAt
            tasks.remove(next)
            next.action()
        }
        now = target
    }

    /** Runs a task even though it was cancelled, to prove stale callbacks are harmless. */
    fun fireCancelledTasksAnyway() {
        tasks.filter { it.cancelled }.forEach { it.action() }
    }
}
