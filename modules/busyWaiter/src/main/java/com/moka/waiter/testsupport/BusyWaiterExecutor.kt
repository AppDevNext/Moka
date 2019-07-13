package com.moka.waiter.testsupport

import com.moka.waiter.BusyWaiter
import com.moka.waiter.BusyWaiter.Category.NETWORK
import org.slf4j.LoggerFactory
import java.lang.Thread.currentThread
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors.newFixedThreadPool

/**
 * This is an implementation of the Executor interface that will track operations
 * using the BusyWaiter. I.e. when this executor is executing something, BusyWaiter
 * will appear to reflect the operation in progress.
 *
 *
 * Espresso will wait until there are not active tasks on this executor.
 */
class BusyWaiterExecutor(private val busyWaiter: BusyWaiter, private val mExecutorToNotifyFinishOperationsOn: Executor) : Executor {

    private val delegate: ExecutorService = newFixedThreadPool(4)

    override fun execute(command: Runnable) {
        val trackerObj = Any()
        log.info("Starting {} on thread {}", command, currentThread())
        busyWaiter.busyWith(trackerObj, NETWORK)
        delegate.execute {
            try {
                command.run()
            } finally {
                // This allows you to finish on another queue ( e.g. the main looper on android )
                mExecutorToNotifyFinishOperationsOn.execute {
                    log.info("Finishing {} on thread {}", command, currentThread())
                    busyWaiter.completed(trackerObj)
                }
            }
        }
    }

    companion object {

        private val log = LoggerFactory.getLogger(BusyWaiterExecutor::class.java)
    }
}
