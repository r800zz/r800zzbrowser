package com.r800zz.r800zzbrowser.browser.api;
import androidx.annotation.AnyThread;

/**
 * Used by a ContentDelegate to indicate what action to take on a slow script event.
 *
 * @see WSession.ContentDelegate#onSlowScript(WSession,String)
 */
@AnyThread
public enum WSlowScriptResponse {
    STOP,
    CONTINUE;
}
