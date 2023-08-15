package de.wwu.mulib.search.executors;

import de.wwu.mulib.exceptions.MulibRuntimeException;

import java.util.concurrent.ThreadFactory;

/**
 * Simple factory for creating threads that, if an uncaught {@link Throwable} escapes the execution,
 * signals the {@link MulibExecutorManager} a failure.
 */
public class ExceptionThrowingThreadFactory implements ThreadFactory {

    private final MultiExecutorsManager owningInstance;

    /**
     * Create a new instance
     * @param owningInstance The owning executor manager.
     */
    public ExceptionThrowingThreadFactory(MultiExecutorsManager owningInstance) {
        this.owningInstance = owningInstance;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread result = new Thread(r);
        result.setUncaughtExceptionHandler((t, e) -> {
            this.owningInstance.signalFailure(e);
            throw new MulibRuntimeException(e);
        });
        return result;
    }
}
