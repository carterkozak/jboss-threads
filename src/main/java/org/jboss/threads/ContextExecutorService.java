/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.threads;

import org.wildfly.common.annotation.Nullable;

import java.io.Closeable;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public final class ContextExecutorService extends AbstractExecutorService {

    private final ExecutorService delegate;
    private final ContextHandler<?> handler;

    public ContextExecutorService(ExecutorService delegate, ContextHandler<?> handler) {
        this.delegate = delegate;
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
        delegate.execute(new ContextRunnable<>(command, handler));
    }

    private static final class ContextRunnable<C> implements Runnable {

        private final Runnable delegate;
        private final ContextHandler<C> handler;
        private final C context;

        ContextRunnable(Runnable delegate, ContextHandler<C> handler) {
            this.delegate = delegate;
            this.handler = handler;
            this.context = handler.collectContext();
        }

        @Override
        public void run() {
            try (ContextCleaner ignored = handler.installContext(context)) {
                delegate.run();
            }
        }
    }

    public interface ContextHandler<C> {
        C collectContext();

        @Nullable
        ContextCleaner installContext(C context);
    }

    public interface ContextCleaner extends Closeable {
        /** Clears any state set by {@link ContextHandler#installContext(Object)}.  */
        @Override
        void close();
    }
}
