package com.github.dghng36.eauction.infra.config.db;

import org.springframework.transaction.PlatformTransactionManager;

public interface IDatabaseConfig {
    PlatformTransactionManager transactionManager();
}
