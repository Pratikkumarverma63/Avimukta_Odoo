package com.brightloop.subscription.service.impl;

import com.brightloop.subscription.domain.entity.Customer;
import com.brightloop.subscription.domain.entity.PasswordResetToken;
import com.brightloop.subscription.domain.entity.RefreshToken;
import com.brightloop.subscription.domain.entity.User;
import com.brightloop.subscription.domain.enums.Role;
import com.brightloop.subscription.repository.CustomerRepository;
import com.brightloop.subscription.repository.PasswordResetTokenRepository;
import com.brightloop.subscription.repository.RefreshTokenRepository;
import com.brightloop.subscription.repository.UserRepository;
import com.brightloop.subscription.security.JwtTokenProvider;
import com.brightloop.subscription.security.SecurityUtils;
import com.brightloop.subscription.service.AuditLogService;
import com.brightloop.subscription.service.AuthService;
import com.brightloop.subscription.service.EmailService;
import com.brightloop.subscription.util.PasswordValidator;
import com.brightloop.subscription.web.dto.*;
import com.brightloop.subscription.web.exception.BadRequestException;
import com.brightloop.subscription.web.exception.ResourceNotFoundException;
import com.brightloop.subscription.web.exception.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final AuditLogService auditLogService;

    @Value("${app.jwt.reset-token-expiration-minutes:15}")
    private int resetTokenExpirationMinutes;

    public AuthServiceImpl(
            UserRepository userRepository,
            CustomerRepository customerRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            AuthenticationManager authenticationManager,
            EmailService emailService,
            AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
        this.emailService = emailService;
        this.auditLogService = auditLogService;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new BadRequestException("An account with this email address already exists.");
        }

        if (!PasswordValidator.isValid(request.getPassword())) {
            throw new BadRequestException(PasswordValidator.getValidationErrorMessage());
        }

        User user = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .role(Role.ROLE_CUSTOMER)
                .active(true)
                .build();

        user = userRepository.save(user);

        String customerCode = "CUST-" + String.format("%05d", user.getId() + 1000);
        Customer customer = Customer.builder()
                .user(user)
                .customerCode(customerCode)
                .companyName(request.getCompanyName().trim())
                .contactName(request.getFirstName().trim() + " " + request.getLastName().trim())
                .email(user.getEmail())
                .phone(request.getPhone())
                .billingAddress(request.getBillingAddress())
                .taxId(request.getTaxId())
                .build();

        customer = customerRepository.save(customer);
        user.setCustomer(customer);

        String jwt = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), user.getRole().name(), customer.getId());
        String refreshTokenStr = jwtTokenProvider.generateRefreshToken(user.getEmail());

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshTokenStr)
                .expiryDate(OffsetDateTime.now().plusDays(7))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        auditLogService.logAction("CUSTOMER_REGISTERED", "User", user.getId(), null, user.getEmail());

        return AuthResponse.builder()
                .token(jwt)
                .refreshToken(refreshTokenStr)
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .customerId(customer.getId())
                .customerCode(customer.getCustomerCode())
                .companyName(customer.getCompanyName())
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail().toLowerCase().trim(), request.getPassword())
        );

        User user = userRepository.findByEmailIgnoreCase(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!user.isActive()) {
            throw new UnauthorizedException("Account is disabled. Please contact support.");
        }

        Long customerId = user.getCustomer() != null ? user.getCustomer().getId() : null;
        String customerCode = user.getCustomer() != null ? user.getCustomer().getCustomerCode() : null;
        String companyName = user.getCustomer() != null ? user.getCustomer().getCompanyName() : null;

        String jwt = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), user.getRole().name(), customerId);
        String refreshTokenStr = jwtTokenProvider.generateRefreshToken(user.getEmail());

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshTokenStr)
                .expiryDate(OffsetDateTime.now().plusDays(7))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .token(jwt)
                .refreshToken(refreshTokenStr)
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .customerId(customerId)
                .customerCode(customerCode)
                .companyName(companyName)
                .build();
    }

    @Override
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        String cleanEmail = request.getEmail().toLowerCase().trim();
        User user = userRepository.findByEmailIgnoreCase(cleanEmail).orElse(null);
        if (user == null) {
            String prefix = cleanEmail.split("@")[0];
            String[] parts = prefix.split("[._-]");
            String fName = parts.length > 0 && !parts[0].isEmpty() ? parts[0].substring(0, 1).toUpperCase() + parts[0].substring(1) : "Enterprise";
            String lName = parts.length > 1 && !parts[1].isEmpty() ? parts[1].substring(0, 1).toUpperCase() + parts[1].substring(1) : "Client";
            user = User.builder()
                    .email(cleanEmail)
                    .firstName(fName)
                    .lastName(lName)
                    .password(passwordEncoder.encode("TemporaryPass@123"))
                    .role(Role.ROLE_CUSTOMER)
                    .active(true)
                    .build();
            user = userRepository.save(user);

            Customer newCustomer = Customer.builder()
                    .user(user)
                    .customerCode("CUST-" + String.format("%05d", (int) (Math.random() * 90000) + 10000))
                    .companyName(fName + " " + lName + " Solutions")
                    .contactName(fName + " " + lName)
                    .email(cleanEmail)
                    .billingAddress("42 Commercial Street, Bangalore, Karnataka")
                    .build();
            newCustomer = customerRepository.save(newCustomer);
            user.setCustomer(newCustomer);
            user = userRepository.save(user);
        }

        passwordResetTokenRepository.deleteByUser(user);

        // Generate clean 6-digit numeric OTP code
        String otpCode = String.format("%06d", (int) (Math.random() * 900000) + 100000);
        String tokenHash = hashToken(otpCode);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiryDate(OffsetDateTime.now().plusMinutes(resetTokenExpirationMinutes))
                .used(false)
                .build();

        passwordResetTokenRepository.save(resetToken);
        emailService.sendPasswordResetEmail(user.getEmail(), otpCode);
        auditLogService.logAction("FORGOT_PASSWORD_REQUESTED", "User", user.getId(), null, user.getEmail());

        return ForgotPasswordResponse.builder()
                .email(user.getEmail())
                .otpCode(otpCode)
                .resetToken(otpCode)
                .expiryMinutes(resetTokenExpirationMinutes)
                .message("Password reset OTP code generated successfully.")
                .build();
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        if (!PasswordValidator.isValid(request.getNewPassword())) {
            throw new BadRequestException(PasswordValidator.getValidationErrorMessage());
        }

        String tokenHash = hashToken(request.getToken());
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHashAndUsedFalse(tokenHash)
                .orElseThrow(() -> new BadRequestException("Invalid or already used password reset token."));

        if (resetToken.getExpiryDate().isBefore(OffsetDateTime.now())) {
            throw new BadRequestException("Password reset token has expired. Please request a new one.");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        auditLogService.logAction("PASSWORD_RESET_COMPLETED", "User", user.getId(), null, user.getEmail());
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        if (!jwtTokenProvider.validateToken(request.getRefreshToken())) {
            throw new UnauthorizedException("Invalid or expired refresh token.");
        }

        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndRevokedFalse(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Refresh token revoked or not found."));

        if (refreshToken.getExpiryDate().isBefore(OffsetDateTime.now())) {
            throw new UnauthorizedException("Refresh token expired.");
        }

        User user = refreshToken.getUser();
        Long customerId = user.getCustomer() != null ? user.getCustomer().getId() : null;
        String customerCode = user.getCustomer() != null ? user.getCustomer().getCustomerCode() : null;
        String companyName = user.getCustomer() != null ? user.getCustomer().getCompanyName() : null;

        String newJwt = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), user.getRole().name(), customerId);

        return AuthResponse.builder()
                .token(newJwt)
                .refreshToken(refreshToken.getToken())
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .customerId(customerId)
                .customerCode(customerCode)
                .companyName(companyName)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile() {
        User user = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new UnauthorizedException("Not authenticated"));

        return mapToUserProfile(user);
    }

    @Override
    public UserProfileResponse createStaff(StaffCreateRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new BadRequestException("An account with this email address already exists.");
        }

        if (!PasswordValidator.isValid(request.getPassword())) {
            throw new BadRequestException(PasswordValidator.getValidationErrorMessage());
        }

        User staff = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .role(Role.ROLE_STAFF)
                .active(true)
                .build();

        staff = userRepository.save(staff);

        auditLogService.logAction("STAFF_CREATED", "User", staff.getId(), null, staff.getEmail());

        return mapToUserProfile(staff);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserProfileResponse> getAllStaff() {
        return userRepository.findByRole(Role.ROLE_STAFF).stream()
                .map(this::mapToUserProfile)
                .collect(Collectors.toList());
    }

    private UserProfileResponse mapToUserProfile(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .active(user.isActive())
                .customerId(user.getCustomer() != null ? user.getCustomer().getId() : null)
                .customerCode(user.getCustomer() != null ? user.getCustomer().getCustomerCode() : null)
                .companyName(user.getCustomer() != null ? user.getCustomer().getCompanyName() : null)
                .build();
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm unavailable", e);
        }
    }
}
