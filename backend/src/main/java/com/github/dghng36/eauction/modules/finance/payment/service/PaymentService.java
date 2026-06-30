package com.github.dghng36.eauction.modules.finance.payment.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.github.dghng36.eauction.core.base.PageResponse;
import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.core.utils.SortUtils;
import com.github.dghng36.eauction.modules.finance.bankAccount.dto.internal.BankAccountInfo;
import com.github.dghng36.eauction.modules.finance.bankAccount.service.InternalBankAccountService;
import com.github.dghng36.eauction.modules.finance.enums.PaymentProvider;
import com.github.dghng36.eauction.modules.finance.enums.PaymentStatus;
import com.github.dghng36.eauction.modules.finance.enums.PaymentType;
import com.github.dghng36.eauction.modules.finance.payment.dto.request.ApproveWithdrawRequest;
import com.github.dghng36.eauction.modules.finance.payment.dto.request.CreateDepositRequest;
import com.github.dghng36.eauction.modules.finance.payment.dto.request.CreateWithdrawRequest;
import com.github.dghng36.eauction.modules.finance.payment.dto.request.RejectWithdrawRequest;
import com.github.dghng36.eauction.modules.finance.payment.dto.request.SePayWebhookRequest;
import com.github.dghng36.eauction.modules.finance.payment.dto.request.SearchPaymentRequest;
import com.github.dghng36.eauction.modules.finance.payment.dto.response.CreateDepositResponse;
import com.github.dghng36.eauction.modules.finance.payment.dto.response.CreateWithDrawResponse;
import com.github.dghng36.eauction.modules.finance.payment.dto.response.PaymentAdminResponse;
import com.github.dghng36.eauction.modules.finance.payment.dto.response.PaymentResponse;
import com.github.dghng36.eauction.modules.finance.payment.dto.response.PaymentStatusResponse;
import com.github.dghng36.eauction.modules.finance.payment.event.DepositEvent;
import com.github.dghng36.eauction.modules.finance.payment.event.RefundEvent;
import com.github.dghng36.eauction.modules.finance.payment.event.WithdrawApprovedEvent;
import com.github.dghng36.eauction.modules.finance.payment.event.WithdrawCreatedEvent;
import com.github.dghng36.eauction.modules.finance.payment.event.WithdrawRejectedEvent;
import com.github.dghng36.eauction.modules.finance.payment.event.WithdrawSuccessEvent;
import com.github.dghng36.eauction.modules.finance.payment.mapper.PaymentMapper;
import com.github.dghng36.eauction.modules.finance.payment.model.Payment;
import com.github.dghng36.eauction.modules.finance.payment.provider.IPaymentProvider;
import com.github.dghng36.eauction.modules.finance.payment.provider.PaymentFactory;
import com.github.dghng36.eauction.modules.finance.payment.repository.PaymentRepository;
import com.github.dghng36.eauction.modules.finance.wallet.dto.request.DepositUserWalletRequest;
import com.github.dghng36.eauction.modules.finance.wallet.dto.request.HoldBalanceUserWalletRequest;
import com.github.dghng36.eauction.modules.finance.wallet.dto.request.RefundUserWalletRequest;
import com.github.dghng36.eauction.modules.finance.wallet.dto.request.WithdrawUserWalletRequest;
import com.github.dghng36.eauction.modules.finance.wallet.service.InternalWalletService;
import com.github.dghng36.eauction.modules.identity.user.service.InternalUserService;
import com.mongodb.client.result.UpdateResult;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

/**
 * All payment are not deleted directly or soft deleted,
 * they are updated with status CANCELED
 */

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PaymentService {
    MongoTemplate mongoTemplate;
    PaymentRepository paymentRepo;

    InternalUserService internalUserService;
    InternalWalletService internalWalletService;
    InternalBankAccountService internalBankAccountService;

    PaymentFactory paymentFactory;

    PaymentMapper paymentMapper;

    ApplicationEventPublisher eventPublisher;

    static final Set<String> ALLOWED_SORT_BY_FIELDS = Set.of(
        "createdAt",
        "updatedAt",
        "amount",
        "status",
        "paymentProvider"
    );

    // Methods for PaymentController
    @Transactional
    public CreateDepositResponse createDeposit(
        String userId,
        CreateDepositRequest createDepositRequest
    ) {
        PaymentProvider provider = PaymentProvider.fromString(createDepositRequest.getPaymentProvider())
            .orElse(null);

        IPaymentProvider paymentProvider = paymentFactory.getPaymentProvider(
            provider
        );

        Payment payment = paymentMapper.toDepositPaymentEntity(
            userId, 
            provider, 
            createDepositRequest.getAmount(), 
            createDepositRequest.getDescription(),
            createDepositRequest.getMetadata()
        );
        Payment savedPayment = paymentRepo.save(payment);

        return paymentProvider.createDepositPayment(
            userId, 
            savedPayment
        );
    }
    
    @Transactional
    public CreateWithDrawResponse createWithdraw(
        String userId,
        CreateWithdrawRequest createWithdrawRequest
    ) {
        PaymentProvider provider = PaymentProvider.fromString(createWithdrawRequest.getPaymentProvider())
            .orElse(null);

        Payment payment = paymentMapper.toWithdrawPaymentEntity(
            userId, 
            provider,
            createWithdrawRequest.getBankAccountId(),
            createWithdrawRequest.getAmount(), 
            createWithdrawRequest.getDescription(),
            createWithdrawRequest.getMetadata()
        );

        Payment savedPayment = paymentRepo.save(payment);

        // Get bank account info of user
        BankAccountInfo bankAccountInfo = internalBankAccountService.getBankAccountInfo(createWithdrawRequest.getBankAccountId());

        log.info("Hold balance for withdraw payment with id: [{}] and code: [{}] of user: [{}] with amount: [{}]",
            savedPayment.getId(), savedPayment.getPaymentCode(), userId, createWithdrawRequest.getAmount()
        );

        internalWalletService.handleHoldBalanceUserWallet(
            userId, 
            HoldBalanceUserWalletRequest.builder()
                .paymentId(savedPayment.getId())
                .paymentCode(savedPayment.getPaymentCode())
                .description(
                    "Hold balance withdraw payment of user: " + userId
                )
                .holdAmount(createWithdrawRequest.getAmount())
                .metadata(Map.of(
                    "bankAccountId", bankAccountInfo.getBankAccountId(),
                    "bankName", bankAccountInfo.getBankName(),
                    "bankCode", bankAccountInfo.getBankCode(),
                    "accountNumber", bankAccountInfo.getAccountNumber(),
                    "accountHolderName", bankAccountInfo.getAccountHolderName()
                ))
                .build()
        );

        // Publish event
        log.info("Publish withdraw created event for payment with id: [{}] and code: [{}] of user: [{}] with amount: [{}]",
            savedPayment.getId(), savedPayment.getPaymentCode(), savedPayment.getUserId(), savedPayment.getAmount()
        );

        publishWithdrawCreatedEvent(
            savedPayment.getId(),
            savedPayment.getPaymentCode(), 
            savedPayment.getUserId(), 
            savedPayment.getAmount(), 
            savedPayment.getDescription(),
            bankAccountInfo.getBankAccountId(),
            bankAccountInfo.getBankName(),
            bankAccountInfo.getAccountNumber(),
            bankAccountInfo.getAccountHolderName()
        );

        log.info("Finished publish withdraw created event for payment with id: [{}] and code: [{}] of user: [{}] with amount: [{}]",
            savedPayment.getId(), savedPayment.getPaymentCode(), savedPayment.getUserId(), savedPayment.getAmount()
        );

        return CreateWithDrawResponse.builder()
            .paymentId(savedPayment.getId())
            .paymentCode(savedPayment.getPaymentCode())
            .amount(savedPayment.getAmount())
            .paymentStatus(savedPayment.getStatus().name())
            .createdAt(savedPayment.getCreatedAt())
            .build();
    }

    // Methods for MyPaymentController
    public PageResponse<PaymentResponse> getMyPayments(
        String userId,
        int page, int size,
        String sortBy, String sortDirection
    ) {
        // Build sort object
        Sort sortBuilt = SortUtils.buildSort(sortBy, sortDirection, ALLOWED_SORT_BY_FIELDS);

        Page<Payment> paymentPage = paymentRepo.findAllByUserId(userId, PageRequest.of(page, size, sortBuilt));

        return PageResponse.<PaymentResponse>builder()
            .currentPage(paymentPage.getTotalElements() == 0 ? 0 : page + 1)
            .pageSize(size)
            .totalPages(paymentPage.getTotalPages())
            .totalElements(paymentPage.getTotalElements())
            .data(paymentMapper.toPaymentResponseList(paymentPage.getContent()))
            .build();
    }

    public PageResponse<PaymentResponse> searchMyPayments(
        String userId, SearchPaymentRequest searchPaymentRequest,
        int page, int size,
        String sortBy, String sortDirection
    ) {
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        // Add criteria for userId
        criteriaList.add(Criteria.where("userId").is(userId));

        // Add criteria for other search fields if needed
        applyCriteria(criteriaList, searchPaymentRequest, false);

        // Execute query
        return executeMyQuery(query, criteriaList, page, size, sortBy, sortDirection);
    }

    public PaymentResponse getMyPayment(
        String userId,
        String paymentId
    ) {
        // Find payment by userId and paymentId
        Payment payment = paymentRepo.findByIdAndUserId(paymentId, userId)
            .orElseThrow(() -> new AppException("Payment not found", HttpStatus.NOT_FOUND));

        return paymentMapper.toPaymentResponse(payment);
    }

    public PaymentStatusResponse getUserPaymentStatus(
        String userId,
        String paymentId
    ) {
        // Find payment by userId and paymentId
        Payment payment = paymentRepo.findByIdAndUserId(paymentId, userId)
            .orElseThrow(() -> new AppException("Payment not found", HttpStatus.NOT_FOUND));

        return paymentMapper.toPaymentStatusResponse(payment);
    }

    public void cancelMyPayment(
        String userId,
        String paymentId
    ) {
        // Find payment by userId and paymentId
        Payment payment = paymentRepo.findByIdAndUserId(paymentId, userId)
            .orElseThrow(() -> new AppException("Payment not found", HttpStatus.NOT_FOUND));

        // Update payment status to CANCELED
        payment.setStatus(PaymentStatus.CANCELLED);

        if (payment.getType().equals(PaymentType.WITHDRAW)) {
            // Refund for user in wallet
            BankAccountInfo bankAccountInfo = internalBankAccountService.getBankAccountInfo(payment.getBankAccountId());
            internalWalletService.handleRefundUserWallet(
                payment.getUserId(), 
                RefundUserWalletRequest.builder()
                    .paymentId(payment.getId())
                    .paymentCode(payment.getPaymentCode())
                    .description(
                        "Refund for canceled withdraw payment of user:" + userId
                    )
                    .refundAmount(payment.getAmount())
                    .metadata(Map.of(
                        "bankAccountId", bankAccountInfo.getBankAccountId(),
                        "bankName", bankAccountInfo.getBankName(),
                        "bankCode", bankAccountInfo.getBankCode(),
                        "accountNumber", bankAccountInfo.getAccountNumber(),
                        "accountHolderName", bankAccountInfo.getAccountHolderName()
                    ))
                    .build()
            );

             // Publish event
            publishRefundEvent(
                payment.getId(),
                payment.getPaymentCode(), 
                payment.getUserId(),
                payment.getAmount(),
                "User canceled the withdraw payment"
            );
        }

        log.info("Cancel payment with id: [{}] and code: [{}] of user: [{}] with amount: [{}]",
            payment.getId(), payment.getPaymentCode(), userId, payment.getAmount()
        );

        paymentRepo.save(payment);
    }

    // Methods for AdminPaymentController
    public PageResponse<PaymentAdminResponse> getAllPayments(
        int page, int size,
        String sortBy, String sortDirection
    ) {
        // Build sort object
        Sort sortBuilt = SortUtils.buildSort(sortBy, sortDirection, ALLOWED_SORT_BY_FIELDS);

        Page<Payment> paymentPage = paymentRepo.findAll(PageRequest.of(page, size, sortBuilt));

        return PageResponse.<PaymentAdminResponse>builder()
            .currentPage(paymentPage.getTotalElements() == 0 ? 0 : page + 1)
            .pageSize(size)
            .totalPages(paymentPage.getTotalPages())
            .totalElements(paymentPage.getTotalElements())
            .data(paymentMapper.toPaymentAdminResponseList(paymentPage.getContent()))
            .build();
    }

    public PageResponse<PaymentAdminResponse> searchAllPayments(
        SearchPaymentRequest searchPaymentRequest,
        int page, int size,
        String sortBy, String sortDirection
    ) {
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        // Add criteria for search fields if needed
        applyCriteria(criteriaList, searchPaymentRequest, true);

        // Execute query
        return executeAdminQuery(query, criteriaList, page, size, sortBy, sortDirection);
    }

    public PaymentAdminResponse getUserPayment(
        String userId,
        String paymentId
    ) {
        // Find payment by userId and paymentId
        Payment payment = paymentRepo.findByIdAndUserId(paymentId, userId)
            .orElseThrow(() -> new AppException("Payment not found", HttpStatus.NOT_FOUND));

        return paymentMapper.toPaymentAdminResponse(payment);
    }

    public void approveWithdrawPayment(
        String paymentId,
        ApproveWithdrawRequest approveWithdrawRequest
    ) {
        Payment payment = paymentRepo.findById(paymentId)
            .orElseThrow(() -> new AppException("Payment not found", HttpStatus.NOT_FOUND));

        // Validate bank account in payment has verified
        internalBankAccountService.validateBankAccountVerified(payment.getBankAccountId());

        if (!payment.getType().equals(PaymentType.WITHDRAW)) {
            log.error("Payment with id: [{}] and code: [{}] of user: [{}] has invalid payment type for approve withdraw: [{}]",
                payment.getId(), payment.getPaymentCode(), payment.getUserId(), payment.getType()
            );

            throw new AppException("Only withdraw payment can be approved", HttpStatus.BAD_REQUEST);
        }

        if (!payment.getStatus().equals(PaymentStatus.PENDING)) {
            log.error("Payment with id: [{}] and code: [{}] of user: [{}] has invalid payment status for approve withdraw: [{}]",
                payment.getId(), payment.getPaymentCode(), payment.getUserId(), payment.getStatus()
            );

            throw new AppException("Only pending payment can be approved", HttpStatus.BAD_REQUEST);
        }

        // Update payment status to APPROVED
        try {
            payment.setStatus(PaymentStatus.PROCESSING);
            paymentRepo.save(payment);

            // Execute payout with provider

           // Publish event
            publishWithdrawApprovedEvent(
                payment.getId(), 
                payment.getPaymentCode(), 
                payment.getUserId(), 
                payment.getAmount(), 
                "Admin approved the withdraw payment and processing payout to user: " + payment.getUserId()
            );

        } catch(Exception e) {
            log.error("Approve withdraw payment failed for payment with id: [{}] and code: [{}] of user: [{}] with error: [{}]",
                payment.getId(), payment.getPaymentCode(), payment.getUserId(), e.getMessage()
            );

            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Payout failed " + e.getMessage());
            paymentRepo.save(payment);

            // Refund user wallet
            BankAccountInfo bankAccountInfo = internalBankAccountService.getBankAccountInfo(payment.getBankAccountId());
            internalWalletService.handleRefundUserWallet(
                payment.getUserId(), 
                RefundUserWalletRequest.builder()
                    .paymentId(payment.getId())
                    .paymentCode(payment.getPaymentCode())
                    .description(
                        "Refund failed for payment of user: " + payment.getUserId()
                    )
                    .metadata(Map.of(
                        "reason", "Payout failed with error: " + e.getMessage(),
                        "bankAccountId", bankAccountInfo.getBankAccountId(),
                        "bankName", bankAccountInfo.getBankName(),
                        "bankCode", bankAccountInfo.getBankCode(),
                        "accountNumber", bankAccountInfo.getAccountNumber(),
                        "accountHolderName", bankAccountInfo.getAccountHolderName()
                    ))
                    .refundAmount(payment.getAmount())
                    .build()
            );

             // Publish event
            publishRefundEvent(
                payment.getId(),
                payment.getPaymentCode(), 
                payment.getUserId(),
                payment.getAmount(),
                "Payout failed with error: " + e.getMessage()
             );

            throw new AppException("Approve withdraw payment failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        log.info("Approve withdraw payment with id: {} and code: [{}] of user: [{}] with amount: [{}]",
            payment.getId(), payment.getPaymentCode(), payment.getUserId(), payment.getAmount()
        );
    }

    public void rejectWithdrawPayment(
        String paymentId,
        RejectWithdrawRequest rejectWithdrawRequest
    ) {
        Payment payment = paymentRepo.findById(paymentId)
            .orElseThrow(() -> new AppException("Payment not found", HttpStatus.NOT_FOUND));

        // Validate bank account in payment has verified
        internalBankAccountService.validateBankAccountVerified(payment.getBankAccountId());
        
        if (!payment.getType().equals(PaymentType.WITHDRAW)) {
            log.error("Payment with id: [{}] and code: [{}] of user: [{}] has invalid payment type for reject withdraw: [{}]",
                payment.getId(), payment.getPaymentCode(), payment.getUserId(), payment.getType()
            );

            throw new AppException("Only withdraw payment can be rejected", HttpStatus.BAD_REQUEST);
        }

        if (!payment.getStatus().equals(PaymentStatus.PENDING)) {
            log.error("Payment with id: [{}] and code: [{}] of user: [{}] has invalid payment status for reject withdraw: [{}]",
                payment.getId(), payment.getPaymentCode(), payment.getUserId(), payment.getStatus()
            );

            throw new AppException("Only pending payment can be rejected", HttpStatus.BAD_REQUEST);
        }

        // Update payment status to FAILED
        payment.setStatus(PaymentStatus.FAILED);
        paymentRepo.save(payment);

        // Refund for user in wallet

        BankAccountInfo bankAccountInfo = internalBankAccountService.getBankAccountInfo(payment.getBankAccountId());
        internalWalletService.handleRefundUserWallet(
            payment.getUserId(), 
            RefundUserWalletRequest.builder()
                .paymentId(payment.getId())
                .paymentCode(payment.getPaymentCode())
                .description("Refund for rejected withdraw payment with code: " + payment.getPaymentCode())
                .metadata(Map.of(
                    "reason", rejectWithdrawRequest.getRejectedReason(),
                    "bankAccountId", bankAccountInfo.getBankAccountId(),
                    "bankName", bankAccountInfo.getBankName(),
                    "accountNumber", bankAccountInfo.getAccountNumber(),
                    "accountHolderName", bankAccountInfo.getAccountHolderName()
                ))
                .refundAmount(payment.getAmount())
                .build()
        );

        // Publish event
        publishWithdrawRejectedEvent(
            payment.getId(),
            payment.getPaymentCode(), 
            payment.getUserId(),
            payment.getAmount(),
            "Admin rejected the withdraw payment with reason: " + rejectWithdrawRequest.getRejectedReason()
        );

        log.info("Reject withdraw payment with id: [{}] and code: [{}] of user: [{}] with amount: [{}] with reason: [{}]",
            payment.getId(), payment.getPaymentCode(), payment.getUserId(), payment.getAmount(), rejectWithdrawRequest.getRejectedReason()
        );
    }

    // Methods for call back
    public void handleSePayWebhook(
        SePayWebhookRequest sePayWebhookRequest
    ) {
        String paymentCode = sePayWebhookRequest.getContent();

        Payment payment = paymentRepo.findByPaymentCode(paymentCode)
            .orElseThrow(() -> new AppException("Payment not found with code: " + paymentCode, HttpStatus.NOT_FOUND));

        if (payment.getStatus().equals(PaymentStatus.SUCCESS) || payment.getStatus().equals(PaymentStatus.FAILED)) {
            return;
        }

        String providerTxd = String.valueOf(sePayWebhookRequest.getId());
        BankAccountInfo bankAccountInfo = internalBankAccountService.getBankAccountInfo(payment.getBankAccountId());

        // Refund user wallet
        switch(payment.getType()) {
            // Handle with withdraw
            case WITHDRAW -> {
                handleSePaytWithdrawPayment(
                    payment, 
                    providerTxd, 
                    bankAccountInfo, 
                    sePayWebhookRequest
                );
            }

            // Handle with deposit
            case DEPOSIT -> {
                handleSePayDepositPayment(
                    payment, 
                    providerTxd, 
                    sePayWebhookRequest
                );
            }   

            // If receive unsupported payment type, mark payment as failed and log error
            default -> {
                markPaymentAsFailed(payment, "Unsupported payment type for SePay webhook: " + payment.getType());
            }
        }

    }

    public Map<String, String> handleVNPayCallback(
        Map<String, String> params
    ) {
        Map<String, String> response = new HashMap<>();

        // Check sum with hash secret
        IPaymentProvider vnProvider = paymentFactory.getPaymentProvider(PaymentProvider.VN_PAY);

        if (!vnProvider.validateCallback(params)) {
            response.put("RspCode", "01");
            response.put("Message", "Invalid signature");
            return response;
        }

        // Get param 
        String paymentCode = params.get("vnp_TxnRef");
        String vnpResponseCode = params.get("vnp_ResponseCode");
        String providerTxId = params.get("vnp_TransactionNo");
        BigDecimal amount = new BigDecimal(params.get("vnp_Amount")).divide(new BigDecimal(100));

        Payment payment = paymentRepo.findByPaymentCode(paymentCode)
            .orElseThrow(() -> new AppException("Payment not found with code: " + paymentCode, HttpStatus.NOT_FOUND));

        if (!payment.getStatus().equals(PaymentStatus.PENDING)) {
            response.put("RspCode", "02");
            response.put("Message", "Payment has been processed already");
            return response;
        }

        if ("00".equals(vnpResponseCode)) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setProviderTransactionId(providerTxId);
            payment.setCallbackData(params);
            paymentRepo.save(payment);

            // Handle wallet for user
            internalWalletService.handleDepositSuccessUserWallet(
                payment.getUserId(), 
                DepositUserWalletRequest.builder()
                    .paymentId(payment.getId())
                    .paymentCode(payment.getPaymentCode())
                    .description("")
                    .metadata(Map.of(
                        "gateway", "VNPay",
                        "transactionDate", params.get("vnp_PayDate")
                    ))
                    .depositAmount(amount)
                    .build()
            );

            response.put("RspCode", "00");
            response.put("Message", "Confirm Success");

            // Publish event
            publishDepositSuccessEvent(
                payment.getId(),
                payment.getPaymentCode(), 
                payment.getUserId(), 
                amount,
                "Payment successful with VNPay callback"
            );

        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Payment failed with response code: " + vnpResponseCode);
            paymentRepo.save(payment);

            // Refund user wallet
            internalWalletService.handleRefundUserWallet(
                payment.getUserId(),
                RefundUserWalletRequest.builder()
                    .paymentId(payment.getId())
                    .paymentCode(payment.getPaymentCode())
                    .description("Refund for failed deposit payment of user: " + payment.getUserId())
                    .metadata(Map.of(
                        "gateway", "VNPay",
                        "transactionDate", params.get("vnp_PayDate")
                    ))
                    .refundAmount(amount)
                    .build()
                );

            response.put("RspCode", vnpResponseCode);
            response.put("Message", "Payment failed");
        }
        return response;
    }

    /**
     * Utility methods:
     * - applyCriteria for build criteria list from search request
     * - executeMyQuery and executeAdminQuery for execute query with criteria list and map to
     * response, these methods will call common method executeCommonQuery to avoid duplicate code for execute query and build page response
     * - getMatchUserIdsByUsername for search user ids with username keyword, used in admin search payment by keyword to search in username field
     * - publish event for publish events after certain action
     */

    // These methods for search
    private void applyCriteria(
        List<Criteria> criteriaList,
        SearchPaymentRequest searchPaymentRequest,
        boolean isAdminSearch
    ) {
        if (StringUtils.hasText(searchPaymentRequest.getSearchQuery())) {
            String regex = Pattern.quote(searchPaymentRequest.getSearchQuery().trim());
            List<Criteria> orCriteriaList = new ArrayList<>();
            
            orCriteriaList.add(Criteria.where("description").regex(regex, "i"));
            orCriteriaList.add(Criteria.where("paymentCode").regex(regex, "i"));

            if (isAdminSearch) {
                Set<String> matchedUserIds = getMatchUserIdsByUsername(searchPaymentRequest.getSearchQuery());
                if (!matchedUserIds.isEmpty()) {
                    orCriteriaList.add(Criteria.where("userId").in(matchedUserIds));
                }
            }
            if (!orCriteriaList.isEmpty()) {
                criteriaList.add(new Criteria().orOperator(orCriteriaList.toArray(Criteria[]::new)));
            }
        }


        if (StringUtils.hasText(searchPaymentRequest.getPaymentProvider())) {
            PaymentProvider provider = PaymentProvider.fromString(searchPaymentRequest.getPaymentProvider())
                .orElse(null);
            if (provider != null) {
                criteriaList.add(Criteria.where("paymentProvider").is(provider.name()));
            }
        }

        if (StringUtils.hasText(searchPaymentRequest.getPaymentType())) {
            PaymentType type = PaymentType.fromString(searchPaymentRequest.getPaymentType())
                .orElse(null);
            if (type != null) {
                criteriaList.add(Criteria.where("type").is(type.name()));
            }
        }

        if (StringUtils.hasText(searchPaymentRequest.getPaymentStatus())) {
            PaymentStatus status = PaymentStatus.fromString(searchPaymentRequest.getPaymentStatus())
                .orElse(null);
            if (status != null) {
                criteriaList.add(Criteria.where("status").is(status.name()));
            }
        }

        if (searchPaymentRequest.getCreatedBefore() != null) {
            criteriaList.add(Criteria.where("createdAt").lte(searchPaymentRequest.getCreatedBefore()));
        }

        if (searchPaymentRequest.getCreatedAfter() != null) {
            criteriaList.add(Criteria.where("createdAt").gte(searchPaymentRequest.getCreatedAfter()));
        }

        if (searchPaymentRequest.getUpdatedBefore() != null) {
            criteriaList.add(Criteria.where("updatedAt").lte(searchPaymentRequest.getUpdatedBefore()));
        }

        if (searchPaymentRequest.getUpdatedAfter() != null) {
            criteriaList.add(Criteria.where("updatedAt").gte(searchPaymentRequest.getUpdatedAfter()));
        }
    }

    private record QueryResult(List<Payment> payments, long totalElements) {}

    private PageResponse<PaymentResponse> executeMyQuery(
        Query baseQuery, List<Criteria> criteriaLits,
        int page, int size,
        String sortBy, String sortDirection
    ) {
        QueryResult queryResult = executeCommonQuery(baseQuery, criteriaLits, page, size, sortBy, sortDirection);

        List<PaymentResponse> paymentResponses = paymentMapper.toPaymentResponseList(queryResult.payments());

        return createPageResponse(page, size, queryResult.totalElements(), paymentResponses);
    }

    private PageResponse<PaymentAdminResponse> executeAdminQuery(
        Query baseQuery, List<Criteria> criteriaLits,
        int page, int size,
        String sortBy, String sortDirection
    ) {
        QueryResult queryResult = executeCommonQuery(baseQuery, criteriaLits, page, size, sortBy, sortDirection);

        List<PaymentAdminResponse> paymentAdminResponses = paymentMapper.toPaymentAdminResponseList(queryResult.payments());

        return createPageResponse(page, size, queryResult.totalElements(), paymentAdminResponses);
    }

    private <T extends PaymentResponse> PageResponse<T> createPageResponse(
        int page, int size, long totalElements, List<T> data
    ) {
        return PageResponse.<T>builder()
            .currentPage(totalElements == 0 ? 0 : page + 1)
            .pageSize(size)
            .totalElements(totalElements)
            .totalPages((int) Math.ceil((double) totalElements / size))
            .data(data)
            .build();
    }

    private QueryResult executeCommonQuery(
        Query baseQuery, List<Criteria> criteriaList,
        int page, int size,
        String sortBy, String sortDirection
    ) {
        if (!criteriaList.isEmpty()) {
            criteriaList.forEach(baseQuery::addCriteria);
        }

        long totalElements = mongoTemplate.count(baseQuery, Payment.class);
        
        // Build sort object
        Sort sortBuilt = SortUtils.buildSort(sortBy, sortDirection, ALLOWED_SORT_BY_FIELDS);
        baseQuery.with(PageRequest.of(page, size, sortBuilt));

        List<Payment> payments = mongoTemplate.find(baseQuery, Payment.class);

        return new QueryResult(payments, totalElements);
    }


    // This methods for helper search
    private Set<String> getMatchUserIdsByUsername(
        String keyword
    ) {
        return internalUserService.searchUserIdsByUsername(keyword).stream()
            .map(String::valueOf)
            .collect(Collectors.toSet());
    }

    // These methods for publish event
    private void publishDepositSuccessEvent(
        String paymentId, 
        String paymentCode, 
        String userId, 
        BigDecimal amount,
        String description
    ) {
        eventPublisher.publishEvent(
            DepositEvent.builder()
                .paymentId(paymentId)
                .paymentCode(paymentCode)
                .userId(userId)
                .amount(amount)
                .description(description)
                .build()
        );
    }

    private void publishWithdrawSuccessEvent(
        String paymentId, 
        String paymentCode, 
        String userId, 
        BigDecimal amount,
        String description,
        String bankAccountId,
        String bankName,
        String accountNumber,
        String accountHolderName
    ) {
        eventPublisher.publishEvent(
            WithdrawSuccessEvent.builder()
                .paymentId(paymentId)
                .paymentCode(paymentCode)
                .userId(userId)
                .amount(amount)
                .description(description)
                .bankAccountId(bankAccountId)
                .bankName(bankName)
                .accountNumber(accountNumber)
                .accountHolderName(accountHolderName)
                .build()
        );
    }

    private void publishWithdrawCreatedEvent(
        String paymentId, 
        String paymentCode, 
        String userId, 
        BigDecimal amount, 
        String description,
        String bankAccountId,
        String bankName,
        String accountNumber,
        String accountHolderName
    ) {
        eventPublisher.publishEvent(
            WithdrawCreatedEvent.builder()
                .paymentId(paymentId)
                .paymentCode(paymentCode)
                .userId(userId)
                .amount(amount)
                .description(description)
                .bankAccountId(bankAccountId)
                .bankName(bankName)
                .accountNumber(accountNumber)
                .accountHolderName(accountHolderName)
                .build()
        );
    }

    private void publishWithdrawApprovedEvent(
        String paymentId,
        String paymentCode,
        String userId,
        BigDecimal amount,
        String description
    ) {
        eventPublisher.publishEvent(
            WithdrawApprovedEvent.builder()
                .paymentId(paymentId)
                .paymentCode(paymentCode)
                .userId(userId)
                .amount(amount)
                .description(description)
                .build()
        );
    }

    private void publishWithdrawRejectedEvent(
        String paymentId,
        String paymentCode,
        String userId,
        BigDecimal amount,
        String rejectedReason
    ) {
        eventPublisher.publishEvent(
            WithdrawRejectedEvent.builder()
                .paymentId(paymentId)
                .paymentCode(paymentCode)
                .userId(userId)
                .amount(amount)
                .description(rejectedReason)
                .build()
        );
    }


    private void publishRefundEvent(
        String paymentId, 
        String paymentCode, 
        String userId, 
        BigDecimal amount, 
        String description

    ) {
        eventPublisher.publishEvent(
            RefundEvent.builder()
                .paymentId(paymentId)
                .paymentCode(paymentCode)
                .userId(userId)
                .amount(amount)
                .description(description)
                .build()
        );
    }

    // These methods for handle SePay webhook
    @Transactional
    private void handleSePaytWithdrawPayment(
        Payment payment,
        String providerTxId,
        BankAccountInfo bankAccountInfo,
        SePayWebhookRequest sePayWebHookRequest
    ) {
        // Check amount in and amount out
        if (sePayWebHookRequest.getAmountOut() != null
            && sePayWebHookRequest.getAmountIn().compareTo(payment.getAmount()) == 0
        ) {
            Query query = new Query(Criteria.where("id").is(payment.getId())
                .and("status").is(PaymentStatus.PROCESSING)
            );

            Update update = new Update()
                .set("status", PaymentStatus.SUCCESS)
                .set("providerTransactionId", String.valueOf(providerTxId));

            UpdateResult updateResult = mongoTemplate.updateFirst(query, update, Payment.class);

            if (updateResult.getModifiedCount() > 0) {
                // handle wallet for user
                internalWalletService.handleWithdrawSuccessUserWallet(
                    payment.getUserId(), 
                    WithdrawUserWalletRequest.builder()
                        .paymentId(payment.getId())
                        .paymentCode(payment.getPaymentCode())
                        .description(
                            "Withdraw user: " + payment.getUserId() + " successfully"
                        )
                        .withdrawAmount(payment.getAmount())
                        .metadata(Map.of(
                            "gateway", sePayWebHookRequest.getGateway(),
                            "transactionDate", sePayWebHookRequest.getTransactionDate()
                        ))
                        .bankName(bankAccountInfo.getBankName())
                        .bankCode(bankAccountInfo.getBankCode())
                        .accountNumber(bankAccountInfo.getAccountNumber())
                        .accountHolderName(bankAccountInfo.getAccountHolderName())    
                        .build()
                );

                // Publish event
                publishWithdrawSuccessEvent(
                    payment.getId(), 
                    payment.getPaymentCode(), 
                    payment.getUserId(), 
                    payment.getAmount(), 
                    "Withdraw user: " + payment.getUserId() + " successfully with SePay",
                    bankAccountInfo.getBankAccountId(),
                    bankAccountInfo.getBankName(),
                    bankAccountInfo.getAccountNumber(),
                    bankAccountInfo.getAccountHolderName()
                );
            } else {
                markPaymentAsFailed(payment, "Failed to update payment status to SUCCESS, maybe because the payment has been processed by another webhook");
            }
        }
    }

    @Transactional
    private void handleSePayDepositPayment(
        Payment payment,
        String providerTxId,
        SePayWebhookRequest sePayWebHookRequest
    ) {
        if (sePayWebHookRequest.getAmountIn() != null
            && sePayWebHookRequest.getAmountIn().compareTo(payment.getAmount()) == 0
        ) {
            Query query = new Query(Criteria.where("id").is(payment.getId())
                .and("status").is(PaymentStatus.PENDING)
            );

            Update update = new Update()
                .set("status", PaymentStatus.SUCCESS)
                .set("providerTransactionId", String.valueOf(providerTxId));

            UpdateResult updateResult = mongoTemplate.updateFirst(query, update, Payment.class);

            if (updateResult.getModifiedCount() > 0) {
                // handle wallet for user
                internalWalletService.handleDepositSuccessUserWallet(
                    payment.getUserId(), 
                    DepositUserWalletRequest.builder()
                        .paymentId(payment.getId())
                        .paymentCode(payment.getPaymentCode())
                        .description(
                            "Deposit user: " + payment.getUserId() + " successfully"
                        )
                        .depositAmount(payment.getAmount())
                        .metadata(Map.of(
                            "gateway", sePayWebHookRequest.getGateway(),
                            "transactionDate", sePayWebHookRequest.getTransactionDate()
                        ))   
                        .build()
                );

                // Publish event
                publishDepositSuccessEvent(
                    payment.getId(), 
                    payment.getPaymentCode(), 
                    payment.getUserId(), 
                    payment.getAmount(), 
                    "Deposit user: " + payment.getUserId() + " successfully with SePay"
                );
            } else {
                markPaymentAsFailed(payment, "Failed to update payment status to SUCCESS, maybe because the payment has been processed by another webhook");
            }
        }
    }

    @Transactional
    private void markPaymentAsFailed(Payment payment, String reason) {
        // Mark payment as failed
        Query query = new Query(Criteria.where("id").is(payment.getId())
            .and("status").in(PaymentStatus.PENDING, PaymentStatus.PROCESSING)
        );

        Update update = new Update()
            .set("status", PaymentStatus.FAILED)
            .set("failureReason", reason);

        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, Payment.class);

        if (updateResult.getModifiedCount() > 0) {
            // Refund user wallet
            internalWalletService.handleRefundUserWallet(
                payment.getUserId(),
                RefundUserWalletRequest.builder()
                    .paymentId(payment.getId())
                    .paymentCode(payment.getPaymentCode())
                    .description("Refund for failed deposit payment with code: " + payment.getPaymentCode())
                    .refundAmount(payment.getAmount())
                    .metadata(Map.of(
                        "reason", reason
                    ))
                    .build()
                );

            // Publish event
            publishRefundEvent(
                payment.getId(),
                payment.getPaymentCode(), 
                payment.getUserId(),
                payment.getAmount(),
                "Payment failed with reason: " + reason
            );
        }
    }
}
