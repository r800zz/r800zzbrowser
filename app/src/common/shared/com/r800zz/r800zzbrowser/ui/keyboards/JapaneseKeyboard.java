package com.r800zz.r800zzbrowser.ui.keyboards;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.r800zz.r800zzbrowser.R;
import com.r800zz.r800zzbrowser.input.CustomKeyboard;
import com.r800zz.r800zzbrowser.utils.StringUtils;

import java.util.ArrayList;
import java.util.Locale;

public class JapaneseKeyboard extends BaseKeyboard {
    private CustomKeyboard mKeyboard;

    public JapaneseKeyboard(Context aContext) {
        super(aContext);
    }

    @NonNull
    @Override
    public CustomKeyboard getAlphabeticKeyboard() {
        if (mKeyboard == null) {
            mKeyboard = new CustomKeyboard(mContext.getApplicationContext(), R.xml.keyboard_qwerty_japanese);
        }
        return mKeyboard;
    }

    @Override
    public boolean supportsAutoCompletion() { return true; }

    private final static char[] SYMBOLS = new char[] {
            ' ', '、', '。','!','?','ー'
    };

    @Nullable
    @Override
    public CandidatesResult getCandidates(String aComposingText) {
        if (TextUtils.isEmpty(aComposingText)) {
            return null;
        }
        return null;
    }

    @Override
    public String getKeyboardTitle() {
        return StringUtils.getStringByLocale(mContext, R.string.settings_language_japanese, getLocale());
    }

    @Override
    public Locale getLocale() {
        return Locale.JAPANESE;
    }

    @Override
    public String getSpaceKeyText(String aComposingText) {
        return StringUtils.getStringByLocale(mContext, R.string.settings_language_japanese, getLocale());
    }

    @Override
    public String[] getDomains(String... domains) {
        return super.getDomains(".jp");
    }
}
