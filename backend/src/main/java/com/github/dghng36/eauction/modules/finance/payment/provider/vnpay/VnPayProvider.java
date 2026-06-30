package com.github.dghng36.eauction.modules.finance.payment.provider.vnpay;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.core.utils.VnPayUtils;
import com.github.dghng36.eauction.modules.finance.enums.PaymentProvider;
import com.github.dghng36.eauction.modules.finance.payment.dto.response.CreateDepositResponse;
import com.github.dghng36.eauction.modules.finance.payment.model.Payment;
import com.github.dghng36.eauction.modules.finance.payment.provider.IPaymentProvider;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class VnPayProvider implements IPaymentProvider {

    @Value("${app.payment.vnpay.api-version}")
    String vnpVersion = "2.1.0";

    @Value("${app.payment.vnpay.command}")
    String vnpCommand = "pay";

    @Value("${app.payment.vnpay.api-url}")
    String vnpUrl;

    @Value("${app.payment.vnpay.tmn-code}")
    String tmnCode;

    @Value("${app.payment.vnpay.hash-secret}")
    String hashSecret;

    @Value("${app.payment.vnpay.return-url}")
    String returnUrl;

    @Value("${app.payment.vnpay.locale}")
    String locale = "vn";

    @Value("${app.payment.vnpay.currency-code}")
    String currencyCode = "VND";

    @Value("${app.payment.vnpay.ip-address}")
    String ipAddress = "127.0.0.1";

    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.VN_PAY;
    }

    @Override
    public CreateDepositResponse createDepositPayment(
        String userId,
        Payment payment
    ) {
        
        int intAmount = payment.getAmount().multiply(BigDecimal.valueOf(100)).intValueExact(); 
        // Put all params to map
        Map<String, Object> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnpVersion);
        vnp_Params.put("vnp_Command", vnpCommand);
        vnp_Params.put("vnp_TmnCode", tmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(intAmount));
        vnp_Params.put("vnp_CurrCode", currencyCode);

        // Generate vnp_TxnRef
        String vnp_TxnRef = payment.getPaymentCode();

        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", "Deposit to account");
        vnp_Params.put("vnp_OrderType", "other");

        vnp_Params.put("vnp_Locale", locale);
        vnp_Params.put("vnp_ReturnUrl", returnUrl);
        vnp_Params.put("vnp_IpAddr", ipAddress);

        // Build zone date
        ZonedDateTime nowUtc = ZonedDateTime.now(ZoneOffset.UTC);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        vnp_Params.put("vnp_CreateDate", nowUtc.format(formatter));

        // Build hash and url query
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        vnp_Params.forEach((k, v) -> {
            try {
                String encodedKey = URLEncoder.encode(k, StandardCharsets.US_ASCII);
                String encodedValue = URLEncoder.encode(String.valueOf(v), StandardCharsets.US_ASCII);
            
                hashData.append(k).append('=').append(encodedValue).append('&');
                query.append(encodedKey).append('=').append(encodedValue).append('&');
            } catch(Exception e) {
                throw new AppException("Error while encoding URL parameters: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        });

        hashData.setLength(hashData.length() - 1);
        query.setLength(query.length() - 1);

        String vnp_SecureHash = VnPayUtils.hmacSHA512(hashSecret, hashData.toString());
        String paymentUrl = vnpUrl + "?" + query.toString() + "&vnp_SecureHash=" + vnp_SecureHash;

        return CreateDepositResponse.builder()
            .paymentId(payment.getId())
            .paymentCode(payment.getPaymentCode())
            .paymentUrl(paymentUrl)
            .paymentStatus(payment.getStatus().name())
            .createdAt(payment.getCreatedAt())
            .build();
    }

    @Override
    public void executePayout(Payment payment) {
        throw new AppException("VnPay does not support payout", HttpStatus.BAD_REQUEST);
    }

    @Override
    public boolean validateCallback(Map<String, String> params) {
        String vnp_secure_hash = params.get("vnp_SecureHash");
        if (vnp_secure_hash == null) {
            return false;
        }

        return VnPayUtils.validateSignature(hashSecret, params, vnp_secure_hash);
    }
}
