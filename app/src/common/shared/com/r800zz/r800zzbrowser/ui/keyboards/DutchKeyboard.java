package com.r800zz.r800zzbrowser.ui.keyboards;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.r800zz.r800zzbrowser.R;
import com.r800zz.r800zzbrowser.input.CustomKeyboard;
import com.r800zz.r800zzbrowser.ui.widgets.WidgetPlacement;
import com.r800zz.r800zzbrowser.utils.StringUtils;

import java.util.Locale;

public class DutchKeyboard extends BaseLatinKeyboard {
    private final Locale mLocale;
    private CustomKeyboard mKeyboard;
    private CustomKeyboard mSymbolsKeyboard;

    public DutchKeyboard(Context aContext) {
        super(aContext);
        mLocale = new Locale("nl", "NL");
    }

    @NonNull
    @Override
    public CustomKeyboard getAlphabeticKeyboard() {
        if (mKeyboard == null) {
            mKeyboard = new CustomKeyboard(mContext.getApplicationContext(), R.xml.keyboard_qwerty_dutch);
            loadDatabase();
        }
        return mKeyboard;
    }

    @Nullable
    @Override
    public CustomKeyboard getSymbolsKeyboard() {
        if (mSymbolsKeyboard == null) {
            mSymbolsKeyboard = new CustomKeyboard(mContext.getApplicationContext(), R.xml.keyboard_symbols);
        }
        return mSymbolsKeyboard;
    }

    @Override
    public float getAlphabeticKeyboardWidth() {
        return WidgetPlacement.dpDimension(mContext, R.dimen.keyboard_alphabetic_width);
    }

    @Override
    public String getKeyboardTitle() {
        return StringUtils.getStringByLocale(mContext, R.string.settings_language_dutch, getLocale());
    }

    @Override
    public Locale getLocale() {
        return mLocale;
    }

    @Override
    public String getSpaceKeyText(String aComposingText) {
        return StringUtils.getStringByLocale(mContext, R.string.settings_language_dutch, getLocale());
    }

    @Override
    public String[] getDomains(String... domains) {
        return super.getDomains(".nl");
    }
}
