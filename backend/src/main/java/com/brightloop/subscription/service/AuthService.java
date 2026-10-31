package com.brightloop.subscription.service;

import com.brightloop.subscription.web.dto.*;

import java.util.List;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
    AuthResponse refreshToken(RefreshTokenRequest request);
    UserProfileResponse getCurrentUserProfile();
    UserProfileResponse createStaff(StaffCreateRequest request);
    List<UserProfileResponse> getAllStaff();
}
