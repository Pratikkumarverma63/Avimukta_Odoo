package com.brightloop.subscription.security;

import com.brightloop.subscription.domain.entity.Customer;
import com.brightloop.subscription.domain.entity.User;
import com.brightloop.subscription.domain.enums.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static Optional<User> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public static Long getCurrentUserId() {
        return getCurrentUser().map(User::getId).orElse(null);
    }

    public static Role getCurrentUserRole() {
        return getCurrentUser().map(User::getRole).orElse(null);
    }

    public static Long getCurrentCustomerId() {
        return getCurrentUser()
                .map(User::getCustomer)
                .map(Customer::getId)
                .orElse(null);
    }

    public static boolean hasRole(Role role) {
        return getCurrentUserRole() == role;
    }

    public static boolean isAdminOrStaff() {
        Role role = getCurrentUserRole();
        return role == Role.ROLE_ADMIN || role == Role.ROLE_STAFF;
    }
}
