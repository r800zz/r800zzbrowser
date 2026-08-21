package com.r800zz.r800zzbrowser.ui.widgets;

import com.r800zz.r800zzbrowser.browser.engine.Session;

import java.util.List;

public interface TabDelegate {
    void onTabAdd();
    void onTabSelect(Session aTab);
    void onTabsClose(List<Session> aTabs);
    void onTabsBookmark(List<Session> aTabs);
    void onTabSync();
}
