package de.wwu.mulib.search.executors;

import de.wwu.mulib.exceptions.MulibRuntimeException;

import java.util.concurrent.ThreadFactory;

public class ExceptionThrowingThreadFactory implements ThreadFactory {

    private final MultiExecutorsManager owningInstance;

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
