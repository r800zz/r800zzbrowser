package com.r800zz.r800zzbrowser.ui.delegates;

import android.view.View;

import androidx.annotation.NonNull;

import com.r800zz.r800zzbrowser.ui.widgets.Windows;

public interface LibraryNavigationDelegate {
    default void onButtonClick(Windows.ContentType contentType) {}
    default void onClose(@NonNull View view) {}
    default void onBack(@NonNull View view) {}
}
