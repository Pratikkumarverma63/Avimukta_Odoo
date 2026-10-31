package com.brightloop.subscription.util;

import java.util.regex.Pattern;

public final class PasswordValidator {

    // Must be more than 8 characters (length >= 9), at least one uppercase, one lowercase, one special character
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]");

    private PasswordValidator() {}

    public static boolean isValid(String password) {
        if (password == null || password.length() <= 8) {
            return false;
        }
        if (!UPPERCASE_PATTERN.matcher(password).find()) {
            return false;
        }
        if (!LOWERCASE_PATTERN.matcher(password).find()) {
            return false;
        }
        if (!SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            return false;
        }
        return true;
    }

    public static String getValidationErrorMessage() {
        return "Password must be more than 8 characters, contain at least one uppercase letter, one lowercase letter, and one special character.";
    }
}
