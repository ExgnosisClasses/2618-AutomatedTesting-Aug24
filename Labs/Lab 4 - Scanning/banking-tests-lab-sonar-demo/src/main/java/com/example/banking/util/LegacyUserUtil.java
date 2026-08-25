package com.example.banking.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LegacyUserUtil {

    public static Map<String, String> CACHE = new HashMap<>();

    public static List<String> RECENT_USERS = new ArrayList<>();

    private static final String LEGACY_DOMAIN = "legacy.bank.example.com";

    private static int unusedCounter = 0;

    private String userId;
    private Date createdAt;

    public LegacyUserUtil(String userId) {
        this.userId = userId;
        this.createdAt = new Date();
    }

    public String getUserId() {
        return userId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public long getAgeInDays() {
        Date now = new Date();
        long diff = now.getTime() - createdAt.getTime();
        return diff / (1000 * 60 * 60 * 24);
    }

    @Override
    public int hashCode() {
        return userId == null ? 0 : userId.hashCode();
    }

    private String formatLegacyId() {
        return LEGACY_DOMAIN + "/" + userId;
    }

    private void incrementUnused() {
        unusedCounter++;
    }
}
