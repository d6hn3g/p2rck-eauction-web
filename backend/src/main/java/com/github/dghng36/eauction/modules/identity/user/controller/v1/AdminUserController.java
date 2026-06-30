package com.github.dghng36.eauction.modules.identity.user.controller.v1;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.dghng36.eauction.core.base.ApiResponse;
import com.github.dghng36.eauction.core.base.PageResponse;
import com.github.dghng36.eauction.modules.identity.user.dto.request.SearchManagedUsersRequest;
import com.github.dghng36.eauction.modules.identity.user.dto.request.UpdateUserRequest;
import com.github.dghng36.eauction.modules.identity.user.dto.response.UserResponse;
import com.github.dghng36.eauction.modules.identity.user.service.UserService;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/admin/management/users")
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AdminUserController {
    UserService userService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    @Validated
    ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getManagedUsers(
        @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page index must not be less than 0") 
        int page,

        @RequestParam(defaultValue = "10") 
        @Min(value = 1, message = "Page size must not be less than 1") 
        @Max(value = 50, message = "Page size must not be greater than 50") 
        int size,

        @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        PageResponse<UserResponse> userPageResp = userService.getManagedUsers(
            page, size, 
            sortBy, sortDirection
        );

        return ResponseEntity
            .ok(ApiResponse.success("All managed users retrieved successfully", userPageResp));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/search")
    @Validated
    ResponseEntity<ApiResponse<PageResponse<UserResponse>>> searchManagedUsers(
        @RequestParam(defaultValue = "0") 
        @Min(value = 0, message = "Page index must not be less than 0") 
        int page,

        @RequestParam(defaultValue = "10") 
        @Min(value = 1, message = "Page size must not be less than 1") 
        @Max(value = 50, message = "Page size must not be greater than 50") 
        int size,
        
        @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection,
        @RequestBody SearchManagedUsersRequest searchManagedUsersRequest
    ) {
        PageResponse<UserResponse> userPage = userService.searchManagedUsers(
            searchManagedUsersRequest, 
            page, size, 
            sortBy, sortDirection
        );

        
        return ResponseEntity
            .ok(ApiResponse.success("Managed users search successfully", userPage));
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}")
    ResponseEntity<ApiResponse<Void>> updateUser(
        @PathVariable String id, 
        @RequestBody UpdateUserRequest updateUserRequest) {
        log.info("Admin is attempting to update user with ID [{}]", id);

        userService.updateUser(id, updateUserRequest);

        return ResponseEntity
            .ok(ApiResponse.success("User updated successfully"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    ResponseEntity<ApiResponse<Void>> deleteUser(
        @PathVariable String id) {
        log.info("Admin is attempting to delete user with ID [{}]", id);

        userService.deleteUser(id);

        return ResponseEntity
            .ok(ApiResponse.success("User deleted successfully"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/reputation")
    ResponseEntity<ApiResponse<Void>> updateUserReputation(
        @PathVariable String id, 
        @RequestParam double reputationChange
    ) {
        log.info("Admin is attempting to update reputation for user with ID [{}] with change [{}]", id, reputationChange);
        userService.updateUserReputation(id, reputationChange);

        return ResponseEntity
            .ok(ApiResponse.success("User's reputation updated successfully"));
    }
}
