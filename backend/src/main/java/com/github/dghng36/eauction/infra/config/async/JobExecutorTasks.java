package com.github.dghng36.eauction.infra.config.async;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class JobExecutorTasks {

    private final Executor jobExecutor;

    public JobExecutorTasks(@Qualifier("jobExecutor") Executor jobExecutor) {
        this.jobExecutor = jobExecutor;
    }

    public Executor getExecutor() {
        return this.jobExecutor;
    }

    public <T> CompletableFuture<T> supplyAsync(Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, jobExecutor);
    }

    public CompletableFuture<Void> runAsync(Runnable task) {
        return CompletableFuture.runAsync(task, jobExecutor);
    }

    public void runAllAndJoin(Runnable... tasks) {
        CompletableFuture.allOf(
            Arrays.stream(tasks).map(this::runAsync).toArray(CompletableFuture[]::new)
        ).join();
    }
}
