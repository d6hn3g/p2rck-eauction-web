package com.github.dghng36.eauction.infra.config.websocket;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.github.dghng36.eauction.infra.config.websocket.interceptor.WebSocketAuthInterceptor;
import com.github.dghng36.eauction.infra.config.websocket.resolver.WebSocketAuthInfoArgumentResolver;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final WebSocketAuthInfoArgumentResolver authInfoArgumentResolver;

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    public WebSocketConfig(WebSocketAuthInfoArgumentResolver authInfoArgumentResolver, WebSocketAuthInterceptor webSocketAuthInterceptor) {
        this.authInfoArgumentResolver = authInfoArgumentResolver;
        this.webSocketAuthInterceptor = webSocketAuthInterceptor;
    }

    @Override
    public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
        // Registers the new Jackson message converter to handle JSON payload serialization
        messageConverters.add(new JacksonJsonMessageConverter());
        
        // Return false to let Spring keep registering default converters alongside this one
        // Return true if you ONLY want to use Jackson and disable default converters
        return false;
    } 

    @Override
    public void configureMessageBroker(
        MessageBrokerRegistry registry
    ) {
        // Client subscribe
        registry.enableSimpleBroker(
            "/topic",
            "/queue"
        );

        // Client send
        registry.setApplicationDestinationPrefixes("/app");

        // Send to specific user
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(
        StompEndpointRegistry registry
    ) {
         registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void addArgumentResolvers(
        List<HandlerMethodArgumentResolver> resolvers
    ) {
        resolvers.add(authInfoArgumentResolver);
    }

    @Override
    public void configureClientInboundChannel(
        ChannelRegistration registration
    ) {
        registration.interceptors(webSocketAuthInterceptor);
    }
}
