/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2012-2015 ForgeRock AS. All Rights Reserved
*
* The contents of this file are subject to the terms
* of the Common Development and Distribution License
* (the License). You may not use this file except in
* compliance with the License.
*
* You can obtain a copy of the License at
* http://forgerock.org/license/CDDLv1.0.html
* See the License for the specific language governing
* permission and limitations under the License.
*
* When distributing Covered Code, include this CDDL
* Header Notice in each file and include the License file
* at http://forgerock.org/license/CDDLv1.0.html
* If applicable, add the following below the CDDL Header,
* with the fields enclosed by brackets [] replaced by
* your own identifying information:
* "Portions Copyrighted [year] [name of copyright owner]"
*
*/
package org.forgerock.openidm.sync.impl;

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
        Executor executor = reconContext.getExcecutor();
        if (executor == null) {
            // Execute single threaded
            while (entriesIter.hasNext()) {
                ResultEntry entry = entriesIter.next();
                try {
                    createTask(entry).call();
                } catch (Exception ex) {
                    translateTaskThrowable(ex);
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
            // each time one completes, submit another if there is any more
            for (int processed = 0; processed < submitted; ++processed) {
                Future<Void> future = completionService.take();
                try {
                    // Get any exceptions
                    Void result = future.get();
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    translateTaskThrowable(cause);
                }
                submitNextIfPresent();
            }
        }
    }

    void submitNextIfPresent() throws SynchronizationException {
        reconContext.checkCanceled();
        if (entriesIter.hasNext()) {
            ResultEntry entry = entriesIter.next();
            completionService.submit(createTask(entry));
            ++submitted;
        }
    }

    void translateTaskThrowable(Throwable throwable) throws SynchronizationException {
        if (throwable instanceof SynchronizationException) {
            throw (SynchronizationException) throwable;
        } else {
            throw new SynchronizationException("Exception in executing recon task "
                    + throwable.getMessage(), throwable);
        }
    }

    /**
     * Create the callable task for the given id
     * @param id source or target id
     * @return the task to reconcile the given id
     * @throws SynchronizationException if processing fails
     */
    
    abstract Callable createTask(ResultEntry entry) throws SynchronizationException;

}
