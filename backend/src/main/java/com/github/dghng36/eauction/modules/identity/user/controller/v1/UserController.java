package com.github.dghng36.eauction.modules.identity.user.controller.v1;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.github.dghng36.eauction.core.base.ApiResponse;
import com.github.dghng36.eauction.core.base.PageResponse;
import com.github.dghng36.eauction.infra.config.security.annotation.AuthInfo;
import com.github.dghng36.eauction.infra.config.security.annotation.AuthInfoType;
import com.github.dghng36.eauction.modules.identity.user.dto.request.ChangePasswordRequest;
import com.github.dghng36.eauction.modules.identity.user.dto.request.RegisterRequest;
import com.github.dghng36.eauction.modules.identity.user.dto.request.SearchPublicUsersRequest;
import com.github.dghng36.eauction.modules.identity.user.dto.request.UpdateMyProfileRequest;
import com.github.dghng36.eauction.modules.identity.user.dto.response.UserResponse;
import com.github.dghng36.eauction.modules.identity.user.service.UserService;
import com.github.dghng36.eauction.modules.media.dto.request.MediaFileUploadRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;



@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserController {
    UserService userService;
    
    @PostMapping("/register")
    ResponseEntity<ApiResponse<UserResponse>> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        log.info("Retrieved register request from user with username: [{}]", registerRequest.getUsername());

        UserResponse userResp = userService.registerUser(registerRequest);

        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/api/v1/users/{id}")
            .buildAndExpand(userResp.getId())
            .toUri();
        
        return ResponseEntity
            .created(location)
            .body(ApiResponse.success("User register successfully", userResp));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @GetMapping("/me")
    ResponseEntity<ApiResponse<UserResponse>> getMyProfile(
        @AuthInfo(info = AuthInfoType.ID) String userId
    ) { 
        UserResponse userResp = userService.getMyProfile(userId);

        return ResponseEntity
            .ok(ApiResponse.success("User profile retrieved successfully", userResp));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @PatchMapping("/me")
    ResponseEntity<ApiResponse<Void>> updateMyProfile(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @Valid @RequestBody UpdateMyProfileRequest updateMyProfileRequest
    ) {
        userService.updateMyProfile(userId, updateMyProfileRequest);

        return ResponseEntity
            .ok(ApiResponse.success("User profile updated successfully"));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @GetMapping
    @Validated
    ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getPublicUsers(
        @RequestParam(defaultValue = "0") 
        @Min(value = 0, message = "Page index must not be less than 0") int page,

        @RequestParam(defaultValue = "10") 
        @Min(value = 1, message = "Page size must not be less than 1") 
        @Max(value = 50, message = "Page size must not be greater than 50")
        int size,

        @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        PageResponse<UserResponse> userPageResp = userService.getPublicUsers(
            page, size, 
            sortBy, sortDirection
        );

        return ResponseEntity
            .ok(ApiResponse.success("All public users retrieved successfully", userPageResp));
    } 

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @PostMapping("/search")
    @Validated
    ResponseEntity<ApiResponse<PageResponse<UserResponse>>> searchPublicUsers(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @RequestParam(defaultValue = "0") 
        @Min(value = 0, message = "Page index must not be less than 0") 
        int page,
        
        @RequestParam(defaultValue = "10") 
        @Min(value = 1, message = "Page size must not be less than 1") 
        @Max(value = 50, message = "Page size must not be greater than 50") 
        int size,
        
        @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection,
        @RequestBody SearchPublicUsersRequest searchPublicUsersRequest
    ) {
        PageResponse<UserResponse> userPageResp = userService.searchPublicUsers(
            searchPublicUsersRequest, 
            page, size,
            sortBy, sortDirection
        );

        
        return ResponseEntity
            .ok(ApiResponse.success("Public users search successfully", userPageResp));
    }
    
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @GetMapping("/{username}")
    ResponseEntity<ApiResponse<UserResponse>> getPublicUserProfile(@PathVariable String username) {
        UserResponse userResp = userService.getUserProfile(username);

        return ResponseEntity
            .ok(ApiResponse.success("Other user profile retrieved successfully", userResp));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @PatchMapping("/me/password")
    ResponseEntity<ApiResponse<Void>> updatePassword(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @Valid @RequestBody ChangePasswordRequest changePasswordRequest
    ) {
        log.info("User with ID [{}] is attempting to update password", userId);

        userService.updatePassword(userId, changePasswordRequest);

        return ResponseEntity
            .ok(ApiResponse.success("User's password updated successfully"));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    @PostMapping("/me/avatar")
    ResponseEntity<ApiResponse<Void>> updateAvatar(
        @AuthInfo(info = AuthInfoType.ID) String userId,
        @RequestBody MediaFileUploadRequest mediaFileRequest
    ) {
        log.info("User with ID [{}] is attempting to update avatar", userId);

        userService.updateAvatar(userId, mediaFileRequest);
        
        return ResponseEntity
            .ok(ApiResponse.success("User's avatar updated successfully"));
    }
    
}
