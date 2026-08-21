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

public class ChineseZhuyinKeyboard extends BaseKeyboard {
    private CustomKeyboard mKeyboard;
    private static final String nonZhuyinReg = "[^ㄅㄆㄇㄈㄉㄊㄋㄌㄍㄎㄏㄐㄑㄒㄓㄔㄕㄖㄗㄘㄙㄚㄛㄜㄝㄞㄟㄠㄡㄢㄣㄤㄥㄦㄧㄨㄩ˙ˊˇˋˉ]";
    private static HashMap<String, String> mKeyMap;
    private static HashMap<String, String> mKeyCodeMap;

    public ChineseZhuyinKeyboard(Context aContext) {
        super(aContext);
        if (mKeyMap == null) {
            mKeyMap = new HashMap<>();
            mKeyCodeMap = new HashMap<>();
            setupKeyMap();
        }
    }

    @NonNull
    @Override
    public CustomKeyboard getAlphabeticKeyboard() {
        if (mKeyboard == null) {
            mKeyboard = new CustomKeyboard(mContext.getApplicationContext(), R.xml.keyboard_qwerty_zhuyin);
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

        String zhuyin = mKeyMap.get(aCode);
        if (zhuyin != null) {
            sb.append(zhuyin);
        }

        String result = sb.toString();
        String tones = "˙ˊˇˋˉ";
        int toneIndex = -1;
        for (int i = 0; i < tones.length(); i++) {
            int index = result.indexOf(tones.charAt(i));
            if (index >= 0) {
                toneIndex = index;
                break;
            }
        }

        if (toneIndex >= 0) {
            char tone = result.charAt(toneIndex);
            result = result.replace(String.valueOf(tone), "");
            result += tone;
        }

        return result;
    }

    private String replaceNonZhuyin(String aComposingText) {
        if (aComposingText == null) return "";
        aComposingText = aComposingText.replaceAll("\\s"," ");
        return aComposingText.replaceAll(nonZhuyinReg, "");
    }

    @Nullable
    @Override
    public CandidatesResult getCandidates(String aComposingText) {
        aComposingText = replaceNonZhuyin(aComposingText);
        if (TextUtils.isEmpty(aComposingText)) {
            return null;
        }

        CandidatesResult result = new CandidatesResult();
        result.composing = aComposingText;
        result.words = new ArrayList<>();

        String query = aComposingText;
        String tones = "˙ˊˇˋˉ";
        boolean hasTone = false;
        for (int i = 0; i < tones.length(); i++) {
            if (query.contains(String.valueOf(tones.charAt(i)))) {
                hasTone = true;
                break;
            }
        }
        if (!hasTone) {
            query += "ˉ";
        }

        String[] queryCodes = getCodes(query);
        for (String code : queryCodes) {
            List<Words> words = getWordsFromCode(code);
            if (words != null) {
                result.words.addAll(words);
            }
        }

        return result;
    }

    private String[] getCodes(String aZhuyin) {
        if (TextUtils.isEmpty(aZhuyin)) {
            return new String[0];
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < aZhuyin.length(); i++) {
            String code = mKeyCodeMap.get(String.valueOf(aZhuyin.charAt(i)));
            if (code != null) {
                sb.append(code);
            }
        }
        return new String[] { sb.toString() };
    }

    private List<Words> getWordsFromCode(String aCode) {
        return null;
    }

    private void setupKeyMap() {
        addKey("1", "ㄅ"); addKey("2", "ㄉ"); addKey("3", "ˇ"); addKey("4", "ˋ"); addKey("5", "ㄓ");
        addKey("6", "ˊ"); addKey("7", "˙"); addKey("8", "ㄚ"); addKey("9", "ㄞ"); addKey("0", "ㄢ");
        addKey("q", "ㄆ"); addKey("w", "ㄊ"); addKey("e", "ㄍ"); addKey("r", "ㄐ"); addKey("t", "ㄔ");
        addKey("y", "ㄗ"); addKey("u", "ㄧ"); addKey("i", "ㄛ"); addKey("o", "ㄟ"); addKey("p", "ㄣ");
        addKey("a", "ㄇ"); addKey("s", "ㄋ"); addKey("d", "ㄎ"); addKey("f", "ㄑ"); addKey("g", "ㄕ");
        addKey("h", "ㄘ"); addKey("j", "ㄨ"); addKey("k", "ㄜ"); addKey("l", "ㄠ"); addKey(";", "ㄤ");
        addKey("z", "ㄈ"); addKey("x", "ㄌ"); addKey("c", "ㄏ"); addKey("v", "ㄒ"); addKey("b", "ㄖ");
        addKey("n", "ㄙ"); addKey("m", "ㄩ"); addKey(",", "ㄝ"); addKey(".", "ㄡ"); addKey("/", "ㄥ");
        addKey("-", "ㄦ"); addKey(" ", "ˉ");

        addKeyCode("ㄅ", "10", "ㄅ"); addKeyCode("ㄆ", "11", "ㄅ"); addKeyCode("ㄇ", "12", "ㄅ"); addKeyCode("ㄈ", "13", "ㄅ");
        addKeyCode("ㄉ", "14", "ㄅ"); addKeyCode("ㄊ", "15", "ㄅ"); addKeyCode("ㄋ", "16", "ㄅ"); addKeyCode("ㄌ", "17", "ㄅ");
        addKeyCode("ㄍ", "18", "ㄅ"); addKeyCode("ㄎ", "19", "ㄅ"); addKeyCode("ㄏ", "1A", "ㄅ"); addKeyCode("ㄐ", "1B", "ㄅ");
        addKeyCode("ㄑ", "1C", "ㄅ"); addKeyCode("ㄒ", "1D", "ㄅ"); addKeyCode("ㄓ", "1E", "ㄅ"); addKeyCode("ㄔ", "1F", "ㄅ");
        addKeyCode("ㄕ", "1G", "ㄅ"); addKeyCode("ㄖ", "1H", "ㄅ"); addKeyCode("ㄗ", "1I", "ㄅ"); addKeyCode("ㄘ", "1J", "ㄅ");
        addKeyCode("ㄙ", "1K", "ㄅ"); addKeyCode("ㄚ", "20", "ㄅ"); addKeyCode("ㄛ", "21", "ㄅ"); addKeyCode("ㄜ", "22", "ㄅ");
        addKeyCode("ㄝ", "23", "ㄅ"); addKeyCode("ㄞ", "24", "ㄅ"); addKeyCode("ㄟ", "25", "ㄅ"); addKeyCode("ㄠ", "26", "ㄅ");
        addKeyCode("ㄡ", "27", "ㄅ"); addKeyCode("ㄢ", "28", "ㄅ"); addKeyCode("ㄣ", "29", "ㄅ"); addKeyCode("ㄤ", "2A", "ㄅ");
        addKeyCode("ㄥ", "2B", "ㄅ"); addKeyCode("ㄦ", "2C", "ㄅ"); addKeyCode("ㄧ", "31", "ㄅ"); addKeyCode("ㄨ", "32", "ㄅ");
        addKeyCode("ㄩ", "33", "ㄅ"); addKeyCode("ˉ", "1", "ㄅ"); addKeyCode("ˊ", "2", "ㄅ"); addKeyCode("ˇ", "3", "ㄅ");
        addKeyCode("ˋ", "4", "ㄅ"); addKeyCode("˙", "5", "ㄅ");
    }

    private void addKey(String aKey, String aZhuyin) {
        mKeyMap.put(aKey, aZhuyin);
    }

    private void addKeyCode(String aZhuyin, String aCode, String aTone) {
        mKeyCodeMap.put(aZhuyin, aCode);
    }

    @Override
    public String getKeyboardTitle() {
        return StringUtils.getStringByLocale(mContext, R.string.settings_language_traditional_chinese, getLocale());
    }

    @Override
    public Locale getLocale() {
        return Locale.TRADITIONAL_CHINESE;
    }

    @Override
    public String getSpaceKeyText(String aComposingText) {
        return StringUtils.getStringByLocale(mContext, R.string.settings_language_traditional_chinese, getLocale());
    }

    @Override
    public String[] getDomains(String... domains) {
        return super.getDomains(".tw");
    }
}
