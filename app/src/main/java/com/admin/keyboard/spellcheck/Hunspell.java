package com.admin.keyboard.spellcheck;

public class Hunspell {
    static {
        System.loadLibrary("hunspell-jni");
    }
    public native void create(String aff, String dic);
    public native int spell(String word);
    public native String[] getSuggestions(String word);
    public native void cleanup();
}