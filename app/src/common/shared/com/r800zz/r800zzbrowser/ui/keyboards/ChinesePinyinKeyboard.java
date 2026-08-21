package com.r800zz.r800zzbrowser.ui.keyboards;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.r800zz.r800zzbrowser.R;
import com.r800zz.r800zzbrowser.input.CustomKeyboard;
import com.r800zz.r800zzbrowser.utils.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ChinesePinyinKeyboard extends BaseKeyboard {
    private CustomKeyboard mKeyboard;
    private static HashMap<String, String[]> mExtraKeyMap;

    public ChinesePinyinKeyboard(Context aContext) {
        super(aContext);
        if (mExtraKeyMap == null) {
            mExtraKeyMap = new HashMap<>();
            setupExtraKeyMap();
        }
    }

    @NonNull
    @Override
    public CustomKeyboard getAlphabeticKeyboard() {
        if (mKeyboard == null) {
            mKeyboard = new CustomKeyboard(mContext.getApplicationContext(), R.xml.keyboard_qwerty_pinyin);
        }
        return mKeyboard;
    }

    @Override
    public boolean supportsAutoCompletion() { return true; }

    @Override
    public boolean usesComposingText() { return true; }

    @Override
    public String getComposingText(String aComposing, String aCode) {
        if (aCode.equals(" ")) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        if (aComposing != null) {
            sb.append(aComposing);
        }

        if (aCode.length() == 1 && Character.isLetter(aCode.charAt(0))) {
            sb.append(aCode.toLowerCase());
        }

        return sb.toString();
    }

    @Nullable
    @Override
    public CandidatesResult getCandidates(String aComposingText) {
        if (TextUtils.isEmpty(aComposingText)) {
            return null;
        }

        CandidatesResult result = new CandidatesResult();
        result.composing = aComposingText;
        result.words = new ArrayList<>();

        List<Words> words = getWordsFromPinyin(aComposingText);
        if (words != null) {
            result.words.addAll(words);
        }

        return result;
    }

    private List<Words> getWordsFromPinyin(String aPinyin) {
        return null;
    }

    private void setupExtraKeyMap() {
        addExtraKeyMap("a", "a", "a|A", "阿|啊");
        addExtraKeyMap("b", "b", "b|B", "不|吧");
    }

    private void addExtraKeyMap(String aKey, String aPinyin, String aLabel, String aWords) {
        mExtraKeyMap.put(aKey, new String[] { aPinyin, aLabel, aWords });
    }

    @Override
    public String getKeyboardTitle() {
        return StringUtils.getStringByLocale(mContext, R.string.settings_language_simplified_chinese, getLocale());
    }

    @Override
    public Locale getLocale() {
        return Locale.CHINESE;
    }

    @Override
    public String getSpaceKeyText(String aComposingText) {
        return StringUtils.getStringByLocale(mContext, R.string.settings_language_simplified_chinese, getLocale());
    }

    @Override
    public String[] getDomains(String... domains) {
        return super.getDomains(".cn");
    }
}
