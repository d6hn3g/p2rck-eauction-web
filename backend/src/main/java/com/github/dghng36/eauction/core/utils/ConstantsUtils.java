package com.github.dghng36.eauction.core.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class ConstantsUtils {
    // Security constants
    public static final class AuthenticationConstants {
        private AuthenticationConstants() {}

        public static final String AUTH_HEADER = "Authorization";
        public static final String TOKEN_PREFIX = "Bearer ";

        public static final String INVALIDATED_EXPIRY_DURATION = "604800";
    }

    // Media file constants
    public static final class MediaFileConstants {
        private MediaFileConstants() {}

        public static final int MAX_MEDIA_FILE_URL = 5;

        public static final long MAX_MEDIA_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5MB
    }

    // Metadata constants
    public static final class MetadataConstants {
        private MetadataConstants() {}

        public static final int MAX_METADATA_SIZE = 10;
        public static final int MAX_VALUE_LENGTH = 255;
    }

    // Reputation constants
    public static final class ReputationConstants {
        private ReputationConstants() {}

        public static final double WELCOME_REPUTATION_BONUS = 50.0;

        public static final double LOGIN_WEEKLY_REPUTATION_BONUS = 2.5;

        public static final double MAX_REPUTATION_SCORE = 100.0;
        public static final double MIN_REPUTATION_SCORE = 0.0;

        public static final double MIN_CREATED_AUCTION_REPUTATION = 20.0;
        public static final double MIN_PARTICIPATED_AUCTION_REPUTATION = 10.0;

        public static final double CREATED_AUCTION_ROOM_REPUTATION_BONUS = 10.0;
        public static final double PARTICIPATED_AUCTION_ROOM_REPUTATION_BONUS = 2.5;

        public static final double WIN_REPUTATION_BONUS = 10.0;
        public static final double LOSS_REPUTATION_PENALTY = 5.0;
    }

    // Identity module constants
    public static final class IdentityConstants {
        private IdentityConstants() {}

        public static final int MIN_USERNAME_LENGTH = 6;
        public static final int MAX_USERNAME_LENGTH = 20;

        public static final int MIN_PASSWORD_LENGTH = 6;
        public static final int MAX_PASSWORD_LENGTH = 100;

        public static final int MIN_PHONE_NUMBER_LENGTH = 10;
        public static final int MAX_PHONE_NUMBER_LENGTH = 15;
    }

    // Auction module constants
    public static final class AuctionConstants {
        private AuctionConstants() {}

        public static final int MIN_PRODUCT_NAME_LENGTH = 6;
        public static final int MAX_PRODUCT_NAME_LENGTH = 100;

        public static final int MIN_PRODUCT_DESCRIPTION_LENGTH = 10;
        public static final int MAX_PRODUCT_DESCRIPTION_LENGTH = 500;

        public static final int MIN_AUCTION_ROOM_TITLE_LENGTH = 6;
        public static final int MAX_AUCTION_ROOM_TITLE_LENGTH = 100;

        public static final int MIN_AUCTION_ROOM_DESCRIPTION_LENGTH = 10;
        public static final int MAX_AUCTION_ROOM_DESCRIPTION_LENGTH = 500;

        public static final int MIN_AUCTION_ROOM_DURATION_MINUTES = 1;
        public static final int MAX_AUCTION_ROOM_DURATION_MINUTES = 10080;

        public static final int MIN_AUCTION_ROOM_EXTENSION_TIME = 1;
        public static final int MAX_AUCTION_ROOM_EXTENSION_TIME = 60;

        public static final int MIN_AUCTION_ROOM_PARTICIPANTS = 1;
        public static final int MAX_AUCTION_ROOM_PARTICIPANTS = 10000;

        public static final int MAX_REASON_LENGTH = 255;
    }

    // Finance module constants
    public static final class FinanceConstants {
        private FinanceConstants() {}

        public static final int MIN_BANK_NAME_LENGTH = 2;
        public static final int MAX_BANK_NAME_LENGTH = 100;

        public static final int MIN_ACCOUNT_NUMBER_LENGTH = 5;
        public static final int MAX_ACCOUNT_NUMBER_LENGTH = 20;

        public static final int MIN_ACCOUNT_HOLDER_NAME_LENGTH = 2;
        public static final int MAX_ACCOUNT_HOLDER_NAME_LENGTH = 100;

        public static final int MAX_DESCRIPTION_LENGTH = 255;

        public static final double MIN_TRANSACTION_AMOUNT = 0.01;
        public static final double MAX_TRANSACTION_AMOUNT = 1_000_000.00;
    }

}
