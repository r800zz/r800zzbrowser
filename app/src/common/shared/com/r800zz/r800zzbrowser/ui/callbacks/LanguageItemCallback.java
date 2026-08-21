package com.r800zz.r800zzbrowser.ui.callbacks;

import android.view.View;

import com.r800zz.r800zzbrowser.ui.adapters.Language;

public interface LanguageItemCallback {
    void onAdd(View view, Language language);
    void onRemove(View view, Language language);
    void onMoveUp(View view, Language language);
    void onMoveDown(View view, Language language);
}
