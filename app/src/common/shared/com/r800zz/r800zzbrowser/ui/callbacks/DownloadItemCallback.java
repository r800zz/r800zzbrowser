package com.r800zz.r800zzbrowser.ui.callbacks;

import android.view.View;

import androidx.annotation.NonNull;

import com.r800zz.r800zzbrowser.downloads.Download;

public interface DownloadItemCallback {
    void onClick(@NonNull View view, @NonNull Download item);
    void onDelete(@NonNull View view, @NonNull Download item);
    void onMore(@NonNull View view, @NonNull Download item);
}
