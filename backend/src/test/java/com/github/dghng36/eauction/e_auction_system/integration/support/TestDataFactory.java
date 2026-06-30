package com.github.dghng36.eauction.e_auction_system.integration.support;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.dghng36.eauction.modules.identity.user.dto.request.RegisterRequest;

public final class TestDataFactory {

    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    private TestDataFactory() {}

    public static RegisterRequest buildRegisterRequest(String prefix) {
        int index = COUNTER.incrementAndGet();
        String username = prefix + index;
        return RegisterRequest.builder()
            .username(username)
            .password("Password123!")
            .email(username + "@integration.test")
            .fullName("Integration User " + index)
            .phoneNumber(String.format("09%08d", index))
            .nationalId("001234567" + (index % 10))
            .idIssueDate(LocalDate.of(2020, 1, 15))
            .idIssuePlace("Ha Noi")
            .address("123 Test Street, Ha Noi")
            .dateOfBirth(LocalDate.of(1995, 6, 15))
            .nationality("Vietnamese")
            .build();
    }

    public static String uniqueMediaCode() {
        return "MC" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }
}
