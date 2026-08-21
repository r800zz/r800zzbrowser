package com.r800zz.r800zzbrowser.ui.callbacks;

import android.view.View;

import androidx.annotation.NonNull;

import com.r800zz.r800zzbrowser.downloads.Download;

public interface DownloadsCallback {
    default void onDeleteDownloads(@NonNull View view) {}
    default void onShowContextMenu(@NonNull View view, Download item, boolean isLastVisibleItem) {}
    default void onHideContextMenu(@NonNull View view) {}
    default void onShowSortingContextMenu(@NonNull View view) {}
}
