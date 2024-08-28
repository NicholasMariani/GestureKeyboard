package com.admin.keyboard.spellcheck;


import android.content.Context;
import android.util.Log;
import android.view.textservice.SuggestionsInfo;
import android.view.textservice.TextInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * HunspellCheckerService is a prototype of Android SpellCheckerService using Hunspell and JNI.
 * Unfortunately the prototype proved that Hunspell suggestions generation is too slow
 * to be used on Android smartphones. It takes up to few seconds to generate suggestions for a longer word.
 */
public class HunspellChecker {
    private static final String TAG = HunspellChecker.class.getSimpleName();
    private static final boolean DBG = true;
    private String mLocale;
    private Context context;
    private Hunspell hunspell;
    private String dicPath;
    public HunspellChecker(Context context) {
        this.context = context;
        File destinationDirectory = new File(context.getFilesDir(), "dictionaries");

        // Create the destination directory if it doesn't exist
        if (!destinationDirectory.exists()) {
            destinationDirectory.mkdirs();
        }

        // Copy the files from assets to internal storage
        copyFileFromAssets("dictionaries/en/en.aff", new File(destinationDirectory, "en.aff"));
        copyFileFromAssets("dictionaries/en/en.dic", new File(destinationDirectory, "en.dic"));

        // Now, you can use the paths in the internal storage
        String affPath = new File(destinationDirectory, "en.aff").getAbsolutePath();
        dicPath = new File(destinationDirectory, "en.dic").getAbsolutePath();

        mLocale = "en";

        hunspell = new Hunspell();
        hunspell.create(affPath, dicPath);
    }

    private void copyFileFromAssets(String sourcePath, File destination) {
        try {
            InputStream in = context.getAssets().open(sourcePath);
            OutputStream out = new FileOutputStream(destination);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }

            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public SuggestionsInfo lookupSuggestions(TextInfo textInfo) {
        Log.d(TAG, "lookupSuggestions: " + textInfo.getText());
        final String input = textInfo.getText();
        final int hunspellResult = hunspell.spell(input);
        String[] suggestions = hunspell.getSuggestions(input);
        int attr = -1;

        System.out.println(hunspellResult);
        switch (hunspellResult) {
            case SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY:
                attr = SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY;
                break;
            case SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO:
                attr = SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO;
                break;
            case SuggestionsInfo.RESULT_ATTR_HAS_RECOMMENDED_SUGGESTIONS:
                attr = SuggestionsInfo.RESULT_ATTR_HAS_RECOMMENDED_SUGGESTIONS;
                break;
            default:
                attr = SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO;
                break;
        }

        return new SuggestionsInfo(attr, suggestions);
    }

    public boolean isWord(String word) {
        switch (hunspell.spell(word)) {
            case SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY:
                return true;
            case SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO:
            default:
                return false;
        }
    }

    public String getDicPath() {
        return dicPath;
    }
}