package com.pothole.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record AuthorityResponse(
        @JsonProperty("id") UUID id,
        @JsonProperty("name") String name,
        @JsonProperty("code") String code,
        @JsonProperty("department_type") String departmentType
) {}
