package com.royal.reserve.bank.api.gateway.filter;

/**
 * Names of the trusted identity headers propagated by the API gateway to downstream services.
 */
public final class AuthHeaders {

    /**
     * Header carrying the authenticated subject (JWT {@code sub} claim).
     */
    public static final String SUBJECT = "X-Auth-Subject";

    /**
     * Header carrying the comma-separated permissions of the authenticated caller.
     */
    public static final String PERMISSIONS = "X-Auth-Permissions";

    /**
     * Permission that grants administrative access to every bank account.
     */
    public static final String ADMIN_PERMISSION = "accounts:admin";

    private AuthHeaders() {
    }
}
