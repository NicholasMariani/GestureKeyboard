package com.admin.keyboard;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.textservice.SuggestionsInfo;
import android.view.textservice.TextInfo;
import android.widget.Button;
import android.widget.FrameLayout;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.RequiresApi;
import androidx.preference.PreferenceManager;

import com.admin.keyboard.kbd.Keyboard;
import com.admin.keyboard.kbd.KeyboardView;
import com.admin.keyboard.spellcheck.HunspellChecker;
import com.admin.keyboard.swipe.SwipeTyping;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class KeyboardActivity extends InputMethodService implements KeyboardView.OnKeyboardActionListener, CandidateClickListener {
    private Keyboard keyboard, keyboardNumRow, keyboardSpecial1, keyboardSpecial2, emojiKeyboard;
    private CustomKeyboardView keyboardView;
    private MODE prevMode, currMode;
    private boolean autoCapitalize, numRow, gestured;
    private String prevWord, currentWord;
    private HunspellChecker hunspellChecker;
    private CandidateView candidateView;
    private SharedPreferences prefs;
    private static KeyboardActivity instance;
    private FrameLayout baseLayout;
    private char onPress, onRelease;
    private SuggestionsInfo suggestionsInfo;
    private boolean candidateSelected, keepSpelling;
    private List<Character> gesture_keys = new LinkedList<>(Arrays.asList('W', 'A', 'C', 'V', 'w', 'a', 'c', 'v'));
    private List<Character> swipedKeys;
    private boolean prevWordSwiped;
    private SwipeTyping swipeTyping;



    public enum MODE {
        CAPS,
        LOWER,
        SPECIAL1,
        SPECIAL2,
        EMOJI
    }

    @Override
    public View onCreateInputView() {
        instance = this;

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        autoCapitalize = prefs.getBoolean("auto_capitalize_preference", true);
        numRow = prefs.getBoolean("number_row_preference", false);

        baseLayout = (FrameLayout) getLayoutInflater().inflate(R.layout.base_layout, null);

        keyboardView = baseLayout.findViewById(R.id.keyboardView);
        keyboard = new Keyboard(this, R.xml.keyboard);
        keyboardNumRow = new Keyboard(this, R.xml.keyboard_numrow);
        keyboardSpecial1 = new Keyboard(this, R.xml.keyboard_special1);
        keyboardSpecial2 = new Keyboard(this, R.xml.keyboard_special2);
        emojiKeyboard = new Keyboard(this, R.xml.emoji_keyboard);
        keyboardView.setOnKeyboardActionListener(this);
        keyboardView.setPreviewEnabled(true);

        switchToUpperCaseKeyboard();
        prevMode = MODE.CAPS;
        keyboardView.setCapsLock(false);

        hunspellChecker = new HunspellChecker(this);
        swipeTyping = new SwipeTyping(hunspellChecker);

        suggestionsInfo = null;
        candidateSelected = false;
        prevWordSwiped = false;

        return initView();
    }

    private View initView() {
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        Display display = windowManager.getDefaultDisplay();
        display.getMetrics(displayMetrics);
        int deviceHeightPixels = displayMetrics.heightPixels;
        int deviceWidthPixels = displayMetrics.widthPixels;

        if(baseLayout.findViewById(R.id.keyboardView) == null) {
            baseLayout = (FrameLayout) getLayoutInflater().inflate(R.layout.base_layout, null);
            keyboardView = baseLayout.findViewById(R.id.keyboardView);
            keyboardView.setOnKeyboardActionListener(this);
            switchToUpperCaseKeyboard();
        }

        View baseContent = baseLayout.findViewById(R.id.baseContent);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                (keyboardView.getKeyboard().getHeight() + (deviceHeightPixels / 14)));
        baseContent.setLayoutParams(layoutParams);

        candidateView = baseLayout.findViewById(R.id.candidateView);
        LinearLayout.LayoutParams candidateLayoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                deviceHeightPixels / 14);
        candidateView.setLayoutParams(candidateLayoutParams);
        candidateView.setCandidateClickListener(this);
        candidateView.setDisplaySize(deviceHeightPixels, deviceWidthPixels);

        return baseLayout;
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        InputConnection ic = getCurrentInputConnection();

        if(ic != null) {
            prevWord = currentWord;
            currentWord = getCurrentWord(ic);

            if (!currentWord.isEmpty()) {
                setSuggestions(ic);
            }
        }

        setInputView(initView());
    }

    public static KeyboardActivity getInstance() {
        if(instance == null)
            instance = new KeyboardActivity();

        return instance;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
    }

    @Override
    public void onCandidateClick(String suggestion, InputConnection ic) {
        if (ic != null) {
            if(handleWordSelect()) {
                handleDeleteKey();
            }

            ic.commitText(suggestion + " ", 1);

            candidateView.clearSuggestions();

            prevWord = "";
            currentWord = "";
            keepSpelling = true;

            swipedKeys = new LinkedList<>();
            prevWordSwiped = false;
        }
    }

    @Override
    public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd);

        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            if(swipedKeys.size() < 2) {
                if (!currentWord.isEmpty() && candidateSelected) {
                    prevWord = currentWord;
                }
                if(currMode != MODE.EMOJI) {
                    currentWord = getCurrentWord(ic);
                    System.out.println("PREV WORD: " + prevWord + " CURRENT WORD: " + currentWord);
                    if (!currentWord.isEmpty()) {
                        setSuggestions(ic);
                    } else {
                        candidateView.clearSuggestions();
                    }
                }
            }
        }
    }

    public void updateNumRowPref() {
        numRow = prefs.getBoolean("number_row_preference", false);
        if (!numRow) {
            keyboardView.setKeyboard(keyboard);
        } else {
            keyboardView.setKeyboard(keyboardNumRow);
        }
        switch (currMode) {
            case CAPS:
                switchToUpperCaseKeyboard();
                break;
            case LOWER:
                switchToLowerCaseKeyboard();
                break;
            case SPECIAL1:
                switchToSpecial1Keyboard();
                break;
            case SPECIAL2:
                switchToSpecial2Keyboard();
                break;
            default:
                break;
        }
        setInputView(initView());
    }

    @Override
    public void onPress(int primaryCode) {
        if(swipedKeys != null) {
            if (swipedKeys.size() > 1 && primaryCode == -5) {
                if (!handleWordSelect()) {
                    candidateView.setFailure("Cannot delete the current word, text is empty!", getCurrentInputConnection());
                } else {
                    handleDeleteKey();
                    prevWordSwiped = false;
                }
            }
        }
        swipedKeys = new LinkedList<>();
        swipedKeys.add((char)primaryCode);
        System.out.println("CURR MODE: " + currMode);
        if(currMode != MODE.EMOJI) {
            performHapticFeedback();
        }
        System.out.println("ON PRESS: " + (char)primaryCode);
        onPress = (char) primaryCode;
    }

    @Override
    public void onSwipe(int primaryCode) {
        System.out.println("ON SWIPE: " + (char)primaryCode);
        if(swipedKeys.isEmpty()) {
            swipedKeys.add((char)primaryCode);
        } else if (swipedKeys.get(swipedKeys.size() - 1) != (char)primaryCode) {
            swipedKeys.add((char) primaryCode);
        }
    }

    @Override
    public void onRelease(int primaryCode) {
        System.out.println("ON RELEASE: " + (char)primaryCode);
        if(gestured) {
            gestured = false;
            prevWordSwiped = false;
        } else {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                switch (primaryCode) {
                    case Keyboard.KEYCODE_SHIFT:
                        handleShiftKey();
                        break;
                    case Keyboard.KEYCODE_MODE_CHANGE:
                        handleModeChangeKey();
                        break;
                    case Keyboard.KEYCODE_DELETE:
                        if(!prevWordSwiped) {
                            handleDeleteKey();
                        }
                        if(currMode != MODE.EMOJI) {
                            prevWord = currentWord;
                            currentWord = getCurrentWord(ic);
                            if (!currentWord.isEmpty()) {
                                setSuggestions(ic);
                            }
                        }
                        break;
                    case Keyboard.KEYCODE_SEND:
                        handleSendKey();
                        break;
                    case Keyboard.KEYCODE_ALT:
                        handleWordSelect();
                        break;
                    case Keyboard.KEYCODE_SPACE:
                        handleSpaceKey(ic);
                        candidateView.clearSuggestions();
                        break;
                    case Keyboard.KEYCODE_PERIOD:
                    case Keyboard.KEYCODE_EXCLAMATION:
                    case Keyboard.KEYCODE_QUESTION:
                        if (currMode != MODE.SPECIAL1 && currMode != MODE.SPECIAL2) {
                            handleSentenceEndingKey(ic, primaryCode);
                        } else {
                            handleNormalKey(ic, primaryCode);
                        }
                        candidateView.clearSuggestions();
                        break;
                    default:
                        candidateSelected = false;
                        prevWord = currentWord;
                        currentWord = getCurrentWord(ic) + (char) primaryCode;
                        keepSpelling = false;
                        if (swipedKeys.size() < 2) {
                            handleNormalKey(ic, primaryCode);
                            setSuggestions(ic);
                        } else {
                            if (Build.VERSION.SDK_INT >= 34) {
                                handleSwipeTyping();
                            }
                        }
                }
            }
        }
    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        onRelease = (char)primaryCode;
        System.out.println("ON KEY: " + onRelease);
        if (onPress == ' ' && onRelease != ' ') {
            gestured = true;
            switch (onRelease) {
                case 'W':
                case 'w':
                    if(!handleWordSelect()) {
                        candidateView.setFailure("Cannot select the current word, text is empty!", getCurrentInputConnection());
                    }
                    break;
                case 'A':
                case 'a':
                    if(!handleSelectAll()) {
                        candidateView.setFailure("Cannot select all, text is empty!", getCurrentInputConnection());
                    }
                    break;
                case 'C':
                case 'c':
                    if(!handleCopy()) {
                        candidateView.setFailure("Nothing to copy!", getCurrentInputConnection());
                    }
                    break;
                case 'X':
                case 'x':
                    if(!handleCut()) {
                        candidateView.setFailure("Nothing to cut!", getCurrentInputConnection());
                    }
                    break;
                case 'V':
                case 'v':
                    if(!handlePaste()) {
                        candidateView.setFailure("Nothing to paste!", getCurrentInputConnection());
                    }
                    break;
                case 'D':
                case 'd':
                    if(!handleWordSelect()) {
                        candidateView.setFailure("Cannot delete the current word, text is empty!", getCurrentInputConnection());
                    } else {
                        handleDeleteKey();
                    }
                    break;
                case 'E':
                case 'e':
                    switchToEmojiKeyboard();
                    break;
                case 'G':
                case 'g':
                    candidateView.setFailure("Gif kbd under construction", getCurrentInputConnection());
                    // set gif keyboard
                    break;
                case (char)-1:
                    switchToUpperCaseKeyboard();
                    keyboardView.setCapsLock(true);
                    break;
                default:
                    candidateView.setFailure("No gesture available for " + onRelease, getCurrentInputConnection());
                    break;
            }
        } else {
            if(swipedKeys.isEmpty()) {
                swipedKeys.add(onRelease);
            } else if (swipedKeys.get(swipedKeys.size() - 1) != onRelease) {
                swipedKeys.add(onRelease);
            }
        }
    }

    private void handleSwipeTyping() {
        if(currMode != MODE.EMOJI) {
            List<Character> adjustedSwipedKeys = new LinkedList<>(swipedKeys);
            if (currMode == MODE.CAPS && !keyboardView.getCapsLock()) {
                onRelease = Character.toLowerCase(onRelease);
                for (int i = 0; i < adjustedSwipedKeys.size(); i++) {
                    if (i != 0 && Character.isUpperCase(adjustedSwipedKeys.get(i))) {
                        char lowercaseChar = Character.toLowerCase(adjustedSwipedKeys.get(i));
                        adjustedSwipedKeys.set(i, lowercaseChar);
                    }
                    System.out.println("handleSwipeTyping key: " + adjustedSwipedKeys.get(i));
                }
            }
            for (char key : adjustedSwipedKeys) {
                System.out.println("handleSwipeTyping key: " + key);
            }
            swipeTyping.setOnPress(onPress);
            swipeTyping.setOnRelease(onRelease);
            List<String> probableWords = null;
            if (android.os.Build.VERSION.SDK_INT >= 34) {
                probableWords = swipeTyping.findProbableWords(new LinkedList<>(adjustedSwipedKeys));
            }
            if (probableWords != null && !probableWords.isEmpty()) {
                if(currMode == MODE.CAPS && !keyboardView.getCapsLock()) {
                    for(int i = 0; i < probableWords.size(); i++) {
                        String word = probableWords.get(i);
                        probableWords.set(i, Character.toUpperCase(word.charAt(0)) + word.substring(1));
                    }
                }
                for(String word : probableWords) {
                    System.out.println("SWIPE SUGGESTION: " + word);
                }
                currentWord = probableWords.get(0);
                setSwipeSuggestions(probableWords);
                switchToLowerCaseKeyboard();
                prevWordSwiped = true;
            }
        }
    }

    private boolean handleWordSelect() {
        InputConnection ic = getCurrentInputConnection();
        ExtractedText extractedText = ic.getExtractedText(new ExtractedTextRequest(), 0);
        CharSequence currentText = extractedText.text;

        if (!TextUtils.isEmpty(currentText)) {
            int start = extractedText.selectionStart;
            int end = extractedText.selectionEnd;
            while (start > 0 && Character.isWhitespace(currentText.charAt(start - 1))) {
                start--;
            }
            while (start > 0 && !Character.isWhitespace(currentText.charAt(start - 1))) {
                start--;
            }
            while (end < currentText.length() && Character.isWhitespace(currentText.charAt(end))) {
                end++;
            }
            while (end < currentText.length() && !Character.isWhitespace(currentText.charAt(end))) {
                end++;
            }
            ic.setSelection(start, end);
            return true;
        }
        return false;
    }

    private boolean handleCopy() {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            CharSequence selectedText = ic.getSelectedText(0);
            if (!TextUtils.isEmpty(selectedText)) {
                ClipData clip = ClipData.newPlainText("label", selectedText);
                clipboardManager.setPrimaryClip(clip);
                return true;
            }
        }
        return false;
    }

    private boolean handleCut() {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            CharSequence selectedText = ic.getSelectedText(0);
            if (!TextUtils.isEmpty(selectedText)) {
                ic.commitText("", 1);
                ClipData clip = ClipData.newPlainText("label", selectedText);
                clipboardManager.setPrimaryClip(clip);
                return true;
            }
        }
        return false;
    }

    private boolean handlePaste() {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            CharSequence clipboardText = clipboardManager.getText();
            if (!TextUtils.isEmpty(clipboardText)) {
                ic.commitText(clipboardText, 1);
                return true;
            }
        }
        return false;
    }

    private boolean handleSelectAll() {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            CharSequence currentText = ic.getExtractedText(new ExtractedTextRequest(), 0).text;

            if (!TextUtils.isEmpty(currentText)) {
                ic.setSelection(0, currentText.length());
                return true;
            }
        }
        return false;
    }

    private boolean containsEmoji() {
        int length = currentWord.length();

        for (int i = 0; i < length; i++) {
            int type = Character.getType(currentWord.charAt(i));
            if (type == Character.SURROGATE || type == Character.OTHER_SYMBOL) {
                return true;
            }
        }

        return false;
    }

    private void setSuggestions(InputConnection ic) {
        if(!currentWord.isEmpty() && !containsEmoji()) {
            suggestionsInfo = hunspellChecker.lookupSuggestions(new TextInfo(currentWord));
            List<String> suggestions = new LinkedList<>();
            for(int i = 0; i < suggestionsInfo.getSuggestionsCount(); i++) {
                suggestions.add(suggestionsInfo.getSuggestionAt(i));
                System.out.println("CURRENT WORD: " + currentWord + " | SUGGESTION " + i + ": " + suggestionsInfo.getSuggestionAt(i));
            }
            String word = candidateSelected ? prevWord : currentWord;
            boolean typo = suggestionsInfo.getSuggestionsAttributes() == SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO;
            candidateView.setSuggestions(typo, word, suggestions, ic);
        }
    }

    private void setSwipeSuggestions(List<String> suggestions) {
        InputConnection ic = getCurrentInputConnection();
        candidateView.setSuggestions(false, currentWord, suggestions, ic);
        CharSequence currentText = ic.getExtractedText(new ExtractedTextRequest(), 0).text;
        if (!TextUtils.isEmpty(currentText) && currentText.charAt(currentText.length() - 1) != ' ') {
            ic.commitText(" ", 1);
        }
        ic.commitText(suggestions.get(0) , 1);
    }

    private String getCurrentWord(InputConnection ic) {
        ExtractedText extractedText = ic.getExtractedText(new ExtractedTextRequest(), 0);
        if(extractedText == null) {
            return "";
        }
        CharSequence currentText = extractedText.text;

        if (TextUtils.isEmpty(currentText)) {
            switchToUpperCaseKeyboard();
            candidateView.clearSuggestions();
            return "";
        }

        int start = extractedText.selectionStart;
        int end = extractedText.selectionEnd;

        // Move the start position to the beginning of the current word
        while (start > 0 && !Character.isWhitespace(currentText.charAt(start - 1))) {
            start--;
        }

        // Move the end position to the end of the current word
        while (end < currentText.length() && !Character.isWhitespace(currentText.charAt(end))) {
            end++;
        }

        currentText.toString().replace("/[\u2190-\u21FF]|[\u2600-\u26FF]|[\u2700-\u27BF]|[\u3000-\u303F]|[\u1F300-\u1F64F]|[\u1F680-\u1F6FF]/g", "");

        // Extract the current word
        return currentText.subSequence(start, end).toString();
    }

    private void handleNormalKey(InputConnection ic, int primaryCode) {
        if (autoCapitalize && Character.isLetter((char) primaryCode)) {
            if (!keyboardView.getCapsLock() && currMode == MODE.CAPS) {
                switchToLowerCaseKeyboard();

            }
        }
        if(prevWordSwiped) {
            ic.commitText(" ", 1);
            prevWordSwiped = false;
        }

        ic.commitText(String.valueOf((char) primaryCode), 1);
    }

    private void switchToLowerCaseKeyboard() {
        if(currMode == MODE.LOWER || currMode == MODE.CAPS) {
            changeCase(MODE.LOWER);
        }
        if (!numRow) {
            keyboardView.setKeyboard(keyboard);
        } else {
            keyboardView.setKeyboard(keyboardNumRow);
        }

        prevMode = currMode;
        currMode = MODE.LOWER;
        keyboardView.invalidateAllKeys();
    }

    private void switchToUpperCaseKeyboard() {
        if(currMode == MODE.LOWER || currMode == MODE.CAPS) {
            changeCase(MODE.CAPS);
        }
        if (!numRow) {
            keyboardView.setKeyboard(keyboard);
        } else {
            keyboardView.setKeyboard(keyboardNumRow);
        }

        prevMode = currMode;
        currMode = MODE.CAPS;
        keyboardView.invalidateAllKeys();
    }

    private void changeCase(MODE mode) {
        List<Keyboard.Key> keys = keyboardView.getKeyboard().getKeys();
        for (Keyboard.Key key : keys) {
            if (key.label != null && !key.label.equals("") && !keyboardView.getActionKeys().contains(key.label) && Character.isLetter(key.label.charAt(0))) {
                key.label = (mode == MODE.CAPS) ? key.label.toString().toUpperCase() : key.label.toString().toLowerCase();
                key.codes[0] = (int) key.label.charAt(0);
            }
        }
    }

    private void switchToSpecial1Keyboard() {
        keyboardView.setKeyboard(keyboardSpecial1);
        prevMode = currMode;
        currMode = MODE.SPECIAL1;
        keyboardView.invalidateAllKeys();
    }

    private void switchToSpecial2Keyboard() {
        keyboardView.setKeyboard(keyboardSpecial2);
        currMode = MODE.SPECIAL2;
        keyboardView.invalidateAllKeys();
    }

    private void switchToEmojiKeyboard() {
        baseLayout = (FrameLayout) getLayoutInflater().inflate(R.layout.emoji_layout, null);
        keyboardView = baseLayout.findViewById(R.id.emojiKeyboardView);
        keyboardView.setOnKeyboardActionListener(this);
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        Display display = windowManager.getDefaultDisplay();
        display.getMetrics(displayMetrics);
        int deviceHeightPixels = displayMetrics.heightPixels;
        int deviceWidthPixels = displayMetrics.widthPixels;


        keyboardView.setKeyboard(emojiKeyboard);
        keyboardView.setPreviewEnabled(false);

//        EmojiHeader emojiHeader = baseLayout.findViewById(R.id.emojiHeader);
//        LinearLayout.LayoutParams headerLayoutParams = new LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.MATCH_PARENT,
//                LinearLayout.LayoutParams.WRAP_CONTENT);
//        emojiHeader.setLayoutParams(headerLayoutParams);

        View baseContent = baseLayout.findViewById(R.id.emojiBaseContent);
//        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
//                RelativeLayout.LayoutParams.MATCH_PARENT,
//                (int) (keyboardView.getKeyboard().getHeight() + (displayMetrics.density * 90)));
//        baseContent.setLayoutParams(layoutParams);

//        View keyboardScroll = baseContent.findViewById(R.id.emojiKeyboardScroll);
//        RelativeLayout.LayoutParams keyboardScrollParams = new RelativeLayout.LayoutParams(
//                RelativeLayout.LayoutParams.MATCH_PARENT, deviceHeightPixels / 4);
//        keyboardScroll.setLayoutParams(keyboardScrollParams);
//
//        View keyboardLayout = baseContent.findViewById(R.id.emojiKeyboardLayout);
//        FrameLayout.LayoutParams keyboardLayoutParams = new FrameLayout.LayoutParams(
//                FrameLayout.LayoutParams.MATCH_PARENT, deviceHeightPixels / 4);
//        keyboardLayout.setLayoutParams(keyboardLayoutParams);

        Button abc = baseContent.findViewById(R.id.ABC);
        abc.setOnClickListener(v -> setInputView(initView()));

        Button delete = baseContent.findViewById(R.id.delete);
        delete.setOnClickListener(v -> performEmojiDelete());

        setInputView(baseLayout);
        keyboardView.invalidateAllKeys();

        currMode = MODE.EMOJI;
    }

    public MODE getCurrentMode() {
        return currMode;
    }

    public void performEmojiDelete() {
        onPress(-5);
        onRelease(-5);
    }

    private void handleSpaceKey(InputConnection ic) {
        if(suggestionsInfo != null) {
            if (suggestionsInfo.getSuggestionsAttributes() == SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO && !keepSpelling) {
                if (handleWordSelect()) {
                    handleDeleteKey();
                    ic.commitText(suggestionsInfo.getSuggestionAt(0), 1);
                    candidateView.clearSuggestions();
                    candidateSelected = true;
                    keepSpelling = false;
                }
            }
        }
        ic.commitText(" ", 1);
    }

    protected boolean isSpaceKeyPressed() {
        // Check if the space key is currently pressed
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            return ic.getExtractedText(new ExtractedTextRequest(), 0).text.toString().contains(" ");
        }
        return false;
    }

    protected void handleSpaceKeySwipe(float velocityX) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            int cursorPosition = ic.getExtractedText(new ExtractedTextRequest(), 0).selectionStart;
            int newPosition = cursorPosition + (int) (velocityX);

            // Limit the newPosition to be within the text range
            int textLength = ic.getTextBeforeCursor(100, 0).length() + ic.getTextAfterCursor(100, 0).length();
            newPosition = Math.max(0, Math.min(newPosition, textLength));

            // Set the new cursor position
            ic.setSelection(newPosition, newPosition);
        }
    }

    private void handleSentenceEndingKey(InputConnection ic, int primaryCode) {
        switch (primaryCode) {
            case Keyboard.KEYCODE_PERIOD:
                handleNormalKey(ic, '.');
                break;
            case Keyboard.KEYCODE_EXCLAMATION:
                handleNormalKey(ic, '!');
                break;
            case Keyboard.KEYCODE_QUESTION:
                handleNormalKey(ic, '?');
                break;
            default:
                handleNormalKey(ic, primaryCode);
        }
        handleSpaceKey(ic);
        switchToUpperCaseKeyboard();
    }

    private void handleShiftKey() {
        switch(currMode) {
            case LOWER:
                switchToUpperCaseKeyboard();
                break;
            case CAPS:
                if(keyboardView.getCapsLock()) {
                    keyboardView.setCapsLock(false);
                    switchToLowerCaseKeyboard();
                } else {
                    keyboardView.setCapsLock(true);
                }
                break;
            case SPECIAL1:
                switchToSpecial2Keyboard();
                break;
            case SPECIAL2:
                switchToSpecial1Keyboard();
                break;
            default:
                break;
        }
    }

    private void handleModeChangeKey() {
        if(currMode == MODE.LOWER || currMode == MODE.CAPS) {
            switchToSpecial1Keyboard();
        } else if(currMode == MODE.EMOJI) {
            setInputView(initView());
        } else {
            if(prevMode == MODE.CAPS)
                switchToUpperCaseKeyboard();
            else {
                switchToLowerCaseKeyboard();
            }
        }
        keyboardView.invalidateAllKeys();
    }

    private void handleDeleteKey() {
        // Delete the character
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            // Check if there is a selected text range
            ExtractedText extractedText = ic.getExtractedText(new ExtractedTextRequest(), 0);
            int selectionStart = extractedText.selectionStart;
            int selectionEnd = extractedText.selectionEnd;

            if (selectionStart != selectionEnd) {
                // Delete the selected text
                ic.commitText("", 1);
            } else {
                // Delete the character to the left
                ic.deleteSurroundingText(1, 0);
            }

            if(candidateSelected && !prevWord.isEmpty() && !keepSpelling) {
                handleWordSelect();
                ic.commitText(prevWord, 1);
                candidateSelected = false;
                keepSpelling = true;
            }
        }
    }

    private void handleSendKey() {
        // Perform action when the "Done" key is pressed
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.performEditorAction(EditorInfo.IME_ACTION_SEND);
        }
    }

    public void performHapticFeedback() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        }
    }
    @Override
    public void onText(CharSequence text) {
        // Handle text input
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.commitText(text, 1);
        }
    }

    @Override
    public void swipeLeft() {
        System.out.println("SWIPE LEFT");
    }

    @Override
    public void swipeRight() {
        System.out.println("SWIPE RIGHT");
    }

    @Override
    public void swipeDown() {
        System.out.println("SWIPE DOWN");
    }

    @Override
    public void swipeUp() {
        System.out.println("SWIPE UP");
    }
}
