package com.pothole.service.authority;

import com.pothole.model.CivicAuthority;

public record AuthorityResolutionResult(
        CivicAuthority authority,
        String authorityCode,
        boolean isResolved
) {
    public static final String UNKNOWN_AUTHORITY_CODE = "UNKNOWN_AUTHORITY";

    public static AuthorityResolutionResult resolved(CivicAuthority authority) {
        return new AuthorityResolutionResult(authority, authority.getCode(), true);
    }

    public static AuthorityResolutionResult unknown() {
        return new AuthorityResolutionResult(null, UNKNOWN_AUTHORITY_CODE, false);
    }
}
