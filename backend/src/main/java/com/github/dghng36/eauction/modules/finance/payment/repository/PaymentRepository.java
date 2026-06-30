package com.github.dghng36.eauction.modules.finance.payment.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.github.dghng36.eauction.modules.finance.payment.model.Payment;


public interface PaymentRepository extends MongoRepository<Payment, String> {
    Page<Payment> findAllByUserId(String userId, Pageable pageable);

    Optional<Payment> findByIdAndUserId(String id, String userId);

    Optional<Payment> findByPaymentCode(String paymentCode);
}
