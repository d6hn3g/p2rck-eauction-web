package com.github.dghng36.eauction.core.base;

import java.time.Instant;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;


@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public abstract class BaseEntity {
    @Id
    String id;

    @CreatedDate
    Instant createdAt;

    @Version
    Long version;

    @LastModifiedDate
    Instant updatedAt;

    LocalDateTime deletedAt;
    Boolean isDeleted;
    
}
