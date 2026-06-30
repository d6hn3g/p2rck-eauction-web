package com.github.dghng36.eauction.e_auction_system.unit.support;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;

import java.util.concurrent.CompletableFuture;

import org.mockito.stubbing.Answer;

import com.github.dghng36.eauction.infra.config.async.JobExecutorTasks;

public final class JobExecutorTasksMockHelper {

    private JobExecutorTasksMockHelper() {}

    public static void runSynchronously(JobExecutorTasks jobExecutorTasks) {
        lenient().doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return CompletableFuture.completedFuture(null);
        }).when(jobExecutorTasks).runAsync(any(Runnable.class));

        Answer<Void> runAllAnswer = invocation -> {
            for (Object arg : invocation.getArguments()) {
                ((Runnable) arg).run();
            }
            return null;
        };

        lenient().doAnswer(runAllAnswer).when(jobExecutorTasks).runAllAndJoin(
            any(Runnable.class), any(Runnable.class));
        lenient().doAnswer(runAllAnswer).when(jobExecutorTasks).runAllAndJoin(
            any(Runnable.class), any(Runnable.class), any(Runnable.class));
    }
}
