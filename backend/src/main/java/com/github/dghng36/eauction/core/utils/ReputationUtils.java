package com.github.dghng36.eauction.core.utils;

import org.springframework.http.HttpStatus;

import com.github.dghng36.eauction.core.exception.AppException;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class ReputationUtils {
    public static void validateReputationScore(double score) {
        if (Double.isNaN(score) || Double.isInfinite(score)) {
            throw new AppException("Reputation score must be a valid number.", HttpStatus.BAD_REQUEST);
        }

        if (score < ConstantsUtils.ReputationConstants.MIN_REPUTATION_SCORE || 
            score > ConstantsUtils.ReputationConstants.MAX_REPUTATION_SCORE) {
            throw new AppException(String.format("Reputation score must be between %.2f and %.2f.", 
                ConstantsUtils.ReputationConstants.MIN_REPUTATION_SCORE, 
                ConstantsUtils.ReputationConstants.MAX_REPUTATION_SCORE), HttpStatus.BAD_REQUEST);
        }
    }

    public static double clampReputation(double score) {
        return Math.max(ConstantsUtils.ReputationConstants.MIN_REPUTATION_SCORE, 
            Math.min(ConstantsUtils.ReputationConstants.MAX_REPUTATION_SCORE, score));
    }

    public static boolean isCreatedAuctionRoom(double score) {
        return score >= ConstantsUtils.ReputationConstants.MIN_CREATED_AUCTION_REPUTATION;
    }

    public static boolean isParticipatedAuctionRoom(double score) {
        return score >= ConstantsUtils.ReputationConstants.MIN_PARTICIPATED_AUCTION_REPUTATION;
    }

}
