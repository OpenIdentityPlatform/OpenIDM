/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2012-2017 ForgeRock AS.
 */
package org.forgerock.openidm.sync.impl;

import org.forgerock.openidm.sync.SynchronizationException;

import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;


/**
 * Base class to process a reconciliation phase, either on the calling thread, or
 * multi-threaded using an executor.
 *
 * Keeps the executor loaded to a desirable level, rather than filling up
 * its queue with all tasks up front.
 */
public abstract class ReconFeeder {
    
    /**
     * The default feed size.
     */
    protected static int DEFAULT_FEED_SIZE = 1000;
    
    CompletionService<Void> completionService;
    int feedSize = DEFAULT_FEED_SIZE;
    int submitted = 0;

    Iterator<ResultEntry> entriesIter;
    ReconciliationContext reconContext;

    protected ReconFeeder(Iterator<ResultEntry> entriesIter, ReconciliationContext reconContext) {
        this.entriesIter = entriesIter;
        this.reconContext = reconContext;
    }

    public void setFeedSize(int feedSize) {
        this.feedSize = feedSize;
    }

    void execute() throws SynchronizationException, InterruptedException {
        SynchronizationException initialCause = null;
        Executor executor = reconContext.getExcecutor();
        if (executor == null) {
            // Execute single threaded
            while (entriesIter.hasNext()) {
                ResultEntry entry = entriesIter.next();
                try {
                    createTask(entry).call();
                } catch (Exception ex) {
                    throw translateTaskThrowable(ex);
                }
            }
        } else {
            submitted = 0;
            completionService = new ExecutorCompletionService<Void>(executor);

            // Pre-load configured number of items
            for (int i = 0; i < feedSize; ++i) {
                submitNextIfPresent();
            }

            // Check all submitted tasks for exception, and
            // each time one completes, submit a new one
            for (int processed = 0; processed < submitted; ++processed) {
                Future<Void> future = completionService.take();
                try {
                    // Get any exceptions
                    Void result = future.get();
                } catch (ExecutionException ex) {
                    if (initialCause == null) {
                        initialCause = translateTaskThrowable(ex);
                    }
                }
                // Submit new task only if no failure has occurred
                if (initialCause == null) {
                    submitNextIfPresent();
                }
            }
            if (initialCause != null) {
                throw initialCause;
            }
        }
    }

    void submitNextIfPresent() throws SynchronizationException {
        if (entriesIter.hasNext() && !reconContext.isCanceled()) {
            ResultEntry entry = entriesIter.next();
            completionService.submit(createTask(entry));
            ++submitted;
        }
    }

    SynchronizationException translateTaskThrowable(Throwable throwable) {
        Throwable cause = throwable.getCause();
        
        if (cause instanceof SynchronizationException) {
                return (SynchronizationException) cause;
        } else if (cause != null) {
            return new SynchronizationException("Exception in executing recon task: "
                        + cause.getMessage(), cause);
        } else {
            return new SynchronizationException("Exception in executing recon task", throwable);
        }
    }

    /**
     * Create the callable task for the given id
     * @param id source or target id
     * @return the task to reconcile the given id
     * @throws SynchronizationException if processing fails
     */
    
    abstract Callable<Void> createTask(ResultEntry entry) throws SynchronizationException;

}
