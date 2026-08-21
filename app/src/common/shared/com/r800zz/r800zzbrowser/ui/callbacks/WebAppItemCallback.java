package com.r800zz.r800zzbrowser.ui.callbacks;

import android.view.View;

import androidx.annotation.NonNull;

import com.r800zz.r800zzbrowser.ui.adapters.WebApp;

public interface WebAppItemCallback {
    void onClick(@NonNull View view, @NonNull WebApp item);

    void onDelete(@NonNull View view, @NonNull WebApp item);
}
