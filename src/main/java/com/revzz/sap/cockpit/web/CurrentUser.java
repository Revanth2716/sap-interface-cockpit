package com.revzz.sap.cockpit.web;

import javax.servlet.http.HttpSession;

/** Reads the signed-in user from the session. */
public final class CurrentUser {

    public static final String SESSION_ATTRIBUTE = "PORTAL_USER";

    private CurrentUser() {
    }

    public static String username(HttpSession session) {
        if (session == null) {
            return "anonymous";
        }
        Object value = session.getAttribute(SESSION_ATTRIBUTE);
        return value == null ? "anonymous" : value.toString();
    }

    public static String role(HttpSession session) {
        if (session == null) {
            return "";
        }
        Object value = session.getAttribute(SESSION_ATTRIBUTE);
        return value == null ? "" : value.toString();
    }
}
