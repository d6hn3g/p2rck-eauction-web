package com.github.dghng36.eauction.infra.config.security.resolver;

import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.github.dghng36.eauction.core.base.AuthInfoDto;
import com.github.dghng36.eauction.core.exception.AppException;
import com.github.dghng36.eauction.infra.config.security.annotation.AuthInfo;
import com.github.dghng36.eauction.infra.config.security.annotation.AuthInfoType;
import com.github.dghng36.eauction.modules.identity.enums.UserRole;

public class AuthInfoArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthInfo.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, 
        NativeWebRequest webRequest, WebDataBinderFactory binderFactory
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new AppException("User unauthorized", HttpStatus.UNAUTHORIZED);
        }

        Object principal = auth.getPrincipal();
        if (!(principal instanceof  Map<?, ?> map)) {
            throw new AppException("User unauthorized", HttpStatus.UNAUTHORIZED);
        }

        if (map.get("userId") == null ||
            map.get("username") == null ||
            map.get("role") == null
        ) {
            throw new AppException("User unauthorized", HttpStatus.UNAUTHORIZED);
        }

        AuthInfo authInfoAnnotation = parameter.getParameterAnnotation(AuthInfo.class);
        AuthInfoType infoType = authInfoAnnotation != null ? authInfoAnnotation.info() : AuthInfoType.DEFAULT;

        return switch(infoType) {
            case ID -> (String) map.get("userId");
            case USERNAME -> (String) map.get("username");
            case ROLE -> {
                String roleStr = (String) map.get("role");
                yield  roleStr != null ? UserRole.fromString(roleStr).orElse(null) : null;
            }
            case DEFAULT -> AuthInfoDto.builder()
                .id((String) map.get("userId"))
                .username((String) map.get("username"))
                .role(map.get("role") != null ? UserRole.fromString(((String) map.get("role")).replaceFirst("^ROLE_", "")).orElse(null) : null)
                .build();
        };
    }
}
