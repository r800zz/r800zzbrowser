package com.r800zz.r800zzbrowser.ui.widgets;

public interface TrayListener {
    default void onPrivateBrowsingClicked() {}
    default void onAddWindowClicked() {}
    default void onTabsClicked() {}
    default void onBookmarksClicked() {}
    default void onDownloadsClicked() {}
}
