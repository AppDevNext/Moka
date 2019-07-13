package com.moka.waiter

import androidx.annotation.GuardedBy
import com.moka.waiter.BusyWaiter.Category.GENERAL
import com.moka.waiter.internal.SetMultiMap
import org.slf4j.LoggerFactory
import java.lang.System.identityHashCode
import java.util.*
import java.util.concurrent.locks.ReentrantLock

/**
 * This allows the app to let Espresso know when it is busy and Espresso should wait.
 * It is not really tied to Espresso and could be used in any case where you need to know if the app is "busy".
 *
 * Call busyWith when you start being "busy"
 * Call completed when you are done and start being "idle".
 *
 * Generally, you should call completed from a finally block.
 *
 * Espresso will wait for the app to be "idle".
 *
 * Proper use of the BusyWaiter will avoid having to "wait" or "sleep" in tests.
 * Be sure not be "busy" langer than necessary, otherwise it will slow down your tests.
 */
class BusyWaiter private constructor(private val completedOnExecutionThread: ExecutionThread) {

    @GuardedBy("lock")
    private val operationsInProgress = SetMultiMap<Category, Any>()
    @GuardedBy("lock")
    private val currentlyTrackedCategories = EnumSet.allOf(Category::class.java)
    @GuardedBy("lock")
    private val noLongerBusyCallbacks = ArrayList<NoLongerBusyCallback>(4)
    private val lock = ReentrantLock()
    private val defaultCategory = GENERAL

    val name: String
        get() {
            lock.lock()
            try {
                return String.format(this.javaClass.simpleName + "@%d with operations: %s", identityHashCode(this), operationsInProgress)
            } finally {
                lock.unlock()
            }
        }

    val isNotBusy: Boolean
        get() {
            lock.lock()
            try {
                for (currentlyTrackedOperation in currentlyTrackedCategories) {
                    if (isBusyWith(currentlyTrackedOperation)) {
                        return false
                    }
                }
                return true
            } finally {
                lock.unlock()
            }
        }

    /**
     * Record the start of an async operation.
     *
     * @param operation An object that identifies the request. Must have a correct equals()/hashCode().
     */
    fun busyWith(operation: Any) {
        busyWith(operation, defaultCategory)
    }

    /**
     * Record the start of an async operation.
     *
     * @param operation An object that identifies the request. Must have a correct equals()/hashCode().
     */
    fun busyWith(operation: Any, category: Category) {
        lock.lock()
        val wasAdded: Boolean
        try {
            wasAdded = operationsInProgress.add(category, operation)
            if (wasAdded) {
                log.info("busyWith -> [{}] was added to active operations", operation)
            }
        } finally {
            lock.unlock()
        }
    }

    fun registerNoLongerBusyCallback(noLongerBusyCallback: NoLongerBusyCallback) {
        lock.lock()
        try {
            noLongerBusyCallbacks.add(noLongerBusyCallback)
        } finally {
            lock.unlock()
        }
    }

    fun payAttentionToCategory(category: Category) {
        lock.lock()
        try {
            currentlyTrackedCategories.add(category)
        } finally {
            lock.unlock()
        }
    }

    fun ignoreCategory(category: Category) {
        lock.lock()
        try {
            currentlyTrackedCategories.remove(category)
        } finally {
            lock.unlock()
        }
    }

    fun completedEverythingInCategory(category: Category) {
        lock.lock()
        try {
            val iterator = operationsInProgress.valuesIterator(category)
            while (iterator.hasNext()) {
                val next = iterator.next()
                completed(next, iterator)
            }
        } finally {
            lock.unlock()
        }
    }

    fun completedEverything() {
        lock.lock()
        try {
            val iterator = operationsInProgress.valuesIterator()
            while (iterator.hasNext()) {
                val next = iterator.next()
                completed(next, iterator)
            }
        } finally {
            lock.unlock()
        }
    }

    fun completedEverythingMatching(matcher: OperationMatcher) {
        lock.lock()
        try {
            val iterator = operationsInProgress.valuesIterator()
            while (iterator.hasNext()) {
                val next = iterator.next()
                if (matcher.matches(next)) {
                    completed(next, iterator)
                }
            }
        } finally {
            lock.unlock()
        }
    }

    /**
     * Marks an operation as complete, using the "completedOnExecutionThread" for this BusyWaiter instance.
     */
    fun postCompleted(operationInProgress: Any) {
        completedOnExecutionThread.execute(object : Runnable {
            override fun run() {
                completed(operationInProgress)
            }

            override fun toString(): String {
                return "completed($operationInProgress)"
            }
        })
    }

    /**
     * Marks an object as completed ( immediately on the current ExecutionThread )
     *
     * @param operationInProgress an object that was passed to busyWith
     */
    fun completed(operationInProgress: Any) {
        completed(operationInProgress, null)
    }

    private fun completed(operation: Any?, iterator: MutableIterator<Any>?) {
        if (operation == null) {
            throw NullPointerException("null can not be `completed` null, operation must be non-null")
        }
        lock.lock()
        val wasRemoved: Boolean
        try {
            if (iterator != null) {
                // if the collection is being iterated,
                // then we HAVE to use the iterator for removal to avoid ConcurrentModificationException
                iterator.remove()
                wasRemoved = true
            } else {
                wasRemoved = operationsInProgress.removeValue(operation)
            }
            if (wasRemoved) {
                log.info("completed -> [{}] was removed from active operations", operation)
            }
            if (wasRemoved && isNotBusy) {
                for (noLongerBusyCallback in noLongerBusyCallbacks) {
                    log.info("All operations are now finished, we are now idle")
                    noLongerBusyCallback.noLongerBusy()
                }
            }
        } finally {
            lock.unlock()
        }
    }

    private fun isBusyWith(currentlyTrackedOperation: Category): Boolean {
        lock.lock()
        try {
            return !operationsInProgress.values(currentlyTrackedOperation).isEmpty()
        } finally {
            lock.unlock()
        }
    }

    fun toVerboseString(): String {
        try {
            lock.lock()
            val operations = operationsInProgress
            val sb = StringBuilder()
                    .append("\n=====================**")
                    .append("\n BusyWaiter Information ")
                    .append("\n=====================**")
            try {
                sb.append("\nTotal Operations:")
                        .append(operations.allValues().size)
                        .append("\nList of operations in progress:")
                        .append("\n===========================*")
                for (category in operations.allKeys()) {
                    sb.append("\nCATEGORY: ======= ").append(category.name).append(" =======")
                    for (operation in operations.values(category)) {
                        sb.append("\n").append(operation.toString())
                    }
                }
            } catch (e: Exception) {
                sb.append(e.message)
                sb.append("\n===* FAILED to get list of progress operations ===*")
            }

            return sb.append("\n===========================*\n").toString()
        } finally {
            lock.unlock()
        }
    }

    enum class Category {
        GENERAL,
        NETWORK,
        DIALOG
    }

    interface NoLongerBusyCallback {
        fun noLongerBusy()
    }

    interface OperationMatcher {
        fun matches(o: Any): Boolean
    }

    interface ExecutionThread {

        fun execute(runnable: Runnable)

        companion object {
            val IMMEDIATE: ExecutionThread = object : ExecutionThread {
                override fun execute(runnable: Runnable) {
                    runnable.run()
                }
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(BusyWaiter::class.java)

        fun withOperationsCompletedOn(completedOnExecutionThread: ExecutionThread): BusyWaiter {
            return BusyWaiter(completedOnExecutionThread)
        }
    }

}
