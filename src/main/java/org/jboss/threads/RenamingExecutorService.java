package org.jboss.threads;

import org.wildfly.common.Assert;
import org.wildfly.common.annotation.Nullable;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

final class RenamingExecutorService extends AbstractExecutorService {

    private final ExecutorService delegate;
    @Nullable
    private final Thread.UncaughtExceptionHandler handler;
    private final Supplier<String> nameSupplier;

    RenamingExecutorService(
            ExecutorService delegate,
            @Nullable Thread.UncaughtExceptionHandler handler,
            Supplier<String> nameSupplier) {
        this.delegate = Assert.checkNotNullParam("delegate", delegate);
        this.nameSupplier = Assert.checkNotNullParam("nameSupplier", nameSupplier);
        this.handler = handler;
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        delegate.execute(new RenamingRunnable(command));
    }

    @Override
    public String toString() {
        return "RenamingExecutorService{delegate=" + delegate + '}';
    }

    final class RenamingRunnable implements Runnable {

        private final Runnable command;

        RenamingRunnable(Runnable command) {
            this.command = command;
        }

        @Override
        public void run() {
            final Thread currentThread = Thread.currentThread();
            final String originalName = currentThread.getName();
            currentThread.setName(nameSupplier.get());
            try {
                command.run();
            } catch (Throwable t) {
                getUncaughtExceptionHandler().uncaughtException(currentThread, t);
            } finally {
                currentThread.setName(originalName);
            }
        }

        private Thread.UncaughtExceptionHandler getUncaughtExceptionHandler() {
            if (handler != null) {
                return handler;
            }
            Thread.UncaughtExceptionHandler handler = Thread.currentThread().getUncaughtExceptionHandler();
            return handler == null ? JBossExecutors.loggingExceptionHandler() : handler;
        }
    }
}
