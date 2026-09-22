package com.royal.reserve.bank.account.api.util;

/**
 * Names of the trusted identity headers set by the API gateway for downstream services.
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
