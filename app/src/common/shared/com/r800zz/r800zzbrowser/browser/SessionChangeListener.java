package com.r800zz.r800zzbrowser.browser;

import com.r800zz.r800zzbrowser.browser.api.WSession;
import com.r800zz.r800zzbrowser.browser.engine.Session;

public interface SessionChangeListener {
    default void onSessionAdded(Session aSession) {}
    default void onSessionOpened(Session aSession) {}
    default void onSessionClosed(Session aSession) {}
    default void onSessionRemoved(String aId) {}
    default void onSessionStateChanged(Session aSession, boolean aActive) {}
    default void onCurrentSessionChange(WSession aOldSession, WSession aSession) {}
    default void onStackSession(Session aSession) {}
    default void onUnstackSession(Session aSession, Session aParent) {}
}
