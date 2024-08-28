package com.admin.keyboard.swipe;

import androidx.annotation.RequiresApi;

import com.admin.keyboard.KeyboardActivity;
import com.admin.keyboard.kbd.Keyboard;
import com.admin.keyboard.spellcheck.HunspellChecker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SwipeTyping {

    private char onPress, onRelease;
    private HunspellChecker hunspellChecker;

    private static final String[] KEYBOARD_LAYOUT = {"qwertyuiop", "asdfghjkl", "zxcvbnm"};
    private static List<String> dictionary = new LinkedList<>();
    private Trie dict = new Trie();

    public SwipeTyping(HunspellChecker hunspellChecker) {
        this.hunspellChecker = hunspellChecker;

        File destinationDirectory = new File(KeyboardActivity.getInstance().getFilesDir(), "dictionaries");
        copyFileFromAssets("dictionaries/en/words.txt", new File(destinationDirectory, "words.txt"));
        String path = new File(destinationDirectory, "words.txt").getAbsolutePath();
        dictionary = readDictionaryFromFile(path);
        buildTrieWithDictionary();
    }

    public void setOnPress(char onPress) {
        this.onPress = onPress;
    }

    public void setOnRelease(char onRelease) {
        this.onRelease = onRelease;
    }

        public List<String> findProbableWords(List<Character> swipes) {
        char[] charArray = new char[swipes.size()];
        for (int i = 0; i < swipes.size(); i++) {
            charArray[i] = swipes.get(i);
        }

        List<String> permutations = new LinkedList<>();
        generatePermutations("", new String(charArray), permutations);

        return filterSuggestions(permutations);
    }

    private void generatePermutations(String prefix, String remaining, List<String> permutations) {
        int n = remaining.length();
        if (prefix.startsWith(String.valueOf(onPress)) && prefix.endsWith(String.valueOf(onRelease))) {
            permutations.add(prefix);
        }

        for (int i = 0; i < n; i++) {
            generatePermutations(prefix + remaining.charAt(i),
                    remaining.substring(i + 1), permutations);
        }
    }

    private List<String> filterSuggestions(List<String> candidates) {
        List<String> filteredSuggestions = new LinkedList<>();

        for (String candidate : candidates) {
            if (hunspellChecker.isWord(candidate)) {
                filteredSuggestions.add(candidate);
            }
        }

        return filteredSuggestions;
    }

//    public List<String> findProbableWords(List<Character> swipes) {
//        StringBuilder swipeStringBuilder = new StringBuilder();
//
//        for (Character swipe : swipes) {
//            swipeStringBuilder.append(swipe);
//        }
//
//        String swipeSequence = swipeStringBuilder.toString();
//        return dict.getWordsWithPrefix(swipeSequence);
//    }

    private void buildTrieWithDictionary() {
        File destinationDirectory = new File(KeyboardActivity.getInstance().getFilesDir(), "dictionaries");
        copyFileFromAssets("dictionaries/en/words.txt", new File(destinationDirectory, "words.txt"));
        String path = new File(destinationDirectory, "words.txt").getAbsolutePath();
        List<String> dictionary = readDictionaryFromFile(path);
        buildTrie(dictionary);
    }

    private void buildTrie(List<String> words) {
        for (String word : words) {
            dict.insert(word);
        }
    }

    private List<String> readDictionaryFromFile(String filePath) {
        List<String> words = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                words.add(line.trim());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return words;
    }

    private void copyFileFromAssets(String sourcePath, File destination) {
        try {
            InputStream in = KeyboardActivity.getInstance().getAssets().open(sourcePath);
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


//    private static boolean match(String path, String word) {
//        try {
//            for (char ch : word.toCharArray()) {
//                path = path.split(String.valueOf(ch), 2)[1];
//            }
//            return true;
//        } catch (Exception e) {
//            return false;
//        }
//    }
//
//    private static int getKeyboardRow(char ch) {
//        for (int rowNo = 0; rowNo < KEYBOARD_LAYOUT.length; rowNo++) {
//            if (KEYBOARD_LAYOUT[rowNo].contains(String.valueOf(ch))) {
//                return rowNo;
//            }
//        }
//        return -1; // Character not found in the keyboard layout
//    }
//
//    private static List<Integer> compress(List<Integer> sequence) {
//        List<Integer> retVal = new ArrayList<>();
//        retVal.add(sequence.get(0));
//
//        for (int element : sequence) {
//            if (retVal.get(retVal.size() - 1) != element) {
//                retVal.add(element);
//            }
//        }
//        return retVal;
//    }
//
//    private static int getMinimumWordLength(String path) {
//        List<Integer> rowNumbers = new ArrayList<>();
//        for (char ch : path.toCharArray()) {
//            rowNumbers.add(getKeyboardRow(ch));
//        }
//
//        List<Integer> compressedRowNumbers = compress(rowNumbers);
//        return compressedRowNumbers.size() - 3;
//    }
//
//    @RequiresApi(api = 34)
//    public static List<String> getSuggestions(LinkedList<Character> path) {
//
//        StringBuilder stringBuilder = new StringBuilder(path.size());
//        for (char c : path) {
//            stringBuilder.append(c);
//        }
//
//        String resultString = stringBuilder.toString();
//        String result = resultString.toLowerCase();
//        List<String> suggestions = new ArrayList<>();
//
//        suggestions.addAll(dictionary.stream()
//                .filter(word -> word.charAt(0) == result.charAt(0) && word.charAt(word.length() - 1) == result.charAt(result.length() - 1))
//                .filter(word -> match(result, word))
//                .filter(word -> word.length() > getMinimumWordLength(result))
//                .toList());
//
//        return suggestions;
//    }

}

//    public List<String> findProbableWords(List<Character> swipes) {
//        char[] charArray = new char[swipes.size()];
//        for (int i = 0; i < swipes.size(); i++) {
//            charArray[i] = swipes.get(i);
//        }
//
//        List<String> permutations = new LinkedList<>();
//        generatePermutations("", new String(charArray), permutations);
//
//        return filterSuggestions(permutations);
//    }
//
//    private void generatePermutations(String prefix, String remaining, List<String> permutations) {
//        int n = remaining.length();
//        if (prefix.startsWith(String.valueOf(onPress)) && prefix.endsWith(String.valueOf(onRelease))) {
//            permutations.add(prefix);
//        }
//
//        for (int i = 0; i < n; i++) {
//            generatePermutations(prefix + remaining.charAt(i),
//                    remaining.substring(i + 1), permutations);
//        }
//    }
//
//    private List<String> filterSuggestions(List<String> candidates) {
//        List<String> filteredSuggestions = new LinkedList<>();
//
//        for (String candidate : candidates) {
//            if (hunspellChecker.isWord(candidate)) {
//                filteredSuggestions.add(candidate);
//            }
//        }
//
//        return filteredSuggestions;
//    }

//    private Trie dict = new Trie();
//
//
//    public List<String> findProbableWords(List<Character> swipes) {
//        StringBuilder swipeStringBuilder = new StringBuilder();
//
//        for (Character swipe : swipes) {
//            swipeStringBuilder.append(swipe);
//        }
//
//        String swipeSequence = swipeStringBuilder.toString();
//        return dict.getWordsWithPrefix(swipeSequence);
//    }
//
//    class TrieNode {
//        Map<Character, TrieNode> children;
//        boolean isEndOfWord;
//
//        public TrieNode() {
//            this.children = new HashMap<>();
//            this.isEndOfWord = false;
//        }
//    }
//
//    public class Trie {
//        private TrieNode root;
//
//        public Trie() {
//            this.root = new TrieNode();
//        }
//
//        public void insert(String word) {
//            TrieNode current = root;
//
//            for (char ch : word.toCharArray()) {
//                current.children.putIfAbsent(ch, new TrieNode());
//                current = current.children.get(ch);
//            }
//
//            current.isEndOfWord = true;
//        }
//
//        public boolean search(String word) {
//            TrieNode node = searchNode(word);
//            return node != null && node.isEndOfWord;
//        }
//
//        public boolean startsWith(String prefix) {
//            TrieNode node = searchNode(prefix);
//            return node != null;
//        }
//
//        private TrieNode searchNode(String prefix) {
//            TrieNode current = root;
//
//            for (char ch : prefix.toCharArray()) {
//                if (!current.children.containsKey(ch)) {
//                    return null;
//                }
//                current = current.children.get(ch);
//            }
//
//            return current;
//        }
//
//        // Get all words with a given prefix
//        public java.util.List<String> getWordsWithPrefix(String prefix) {
//            TrieNode node = searchNode(prefix);
//            java.util.List<String> result = new java.util.ArrayList<>();
//
//            if (node != null) {
//                collectWords(node, new StringBuilder(prefix), result);
//            }
//
//            return result;
//        }
//
//        private void collectWords(TrieNode node, StringBuilder currentWord, java.util.List<String> result) {
//            if (node.isEndOfWord) {
//                result.add(currentWord.toString());
//            }
//
//            for (Map.Entry<Character, TrieNode> entry : node.children.entrySet()) {
//                collectWords(entry.getValue(), currentWord.append(entry.getKey()), result);
//                currentWord.deleteCharAt(currentWord.length() - 1);
//            }
//        }
//    }
//
//    private void buildTrieWithDictionary() {
//        File destinationDirectory = new File(this.getFilesDir(), "dictionaries");
//        copyFileFromAssets("dictionaries/en/words.txt", new File(destinationDirectory, "words.txt"));
//        String path = new File(destinationDirectory, "words.txt").getAbsolutePath();
//        List<String> dictionary = readDictionaryFromFile(path);
//        buildTrie(dictionary);
//    }
//
//    private void buildTrie(List<String> words) {
//        for (String word : words) {
//            dict.insert(word);
//        }
//    }
//
//    private List<String> readDictionaryFromFile(String filePath) {
//        List<String> words = new ArrayList<>();
//
//        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
//            String line;
//            while ((line = br.readLine()) != null) {
//                words.add(line.trim());
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        return words;
//    }
//
//    private void copyFileFromAssets(String sourcePath, File destination) {
//        try {
//            InputStream in = this.getAssets().open(sourcePath);
//            OutputStream out = new FileOutputStream(destination);
//
//            byte[] buffer = new byte[1024];
//            int read;
//            while ((read = in.read(buffer)) != -1) {
//                out.write(buffer, 0, read);
//            }
//
//            in.close();
//            out.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//


//    private static final String[] KEYBOARD_LAYOUT = {"qwertyuiop", "asdfghjkl", "zxcvbnm"};
//    private static List<String> dictionary = new LinkedList<>();
//
//        @RequiresApi(api = 34)
//        private void handleSwipeTyping() {
//        if(currMode != MODE.EMOJI) {
//            List<Character> adjustedSwipedKeys = new LinkedList<>(swipedKeys);
//            if (currMode == MODE.CAPS && !keyboardView.getCapsLock()) {
//                onRelease = Character.toLowerCase(onRelease);
//                for (int i = 0; i < adjustedSwipedKeys.size(); i++) {
//                    if (i != 0 && Character.isUpperCase(adjustedSwipedKeys.get(i))) {
//                        char lowercaseChar = Character.toLowerCase(adjustedSwipedKeys.get(i));
//                        adjustedSwipedKeys.set(i, lowercaseChar);
//                    }
//                    System.out.println("handleSwipeTyping key: " + adjustedSwipedKeys.get(i));
//                }
//            }
//            for (char key : adjustedSwipedKeys) {
//                System.out.println("handleSwipeTyping key: " + key);
//            }
//            List<String> probableWords = getSuggestions(new LinkedList<Character>(adjustedSwipedKeys));
//            if (!probableWords.isEmpty()) {
//                currentWord = probableWords.get(0);
//                for (int i = 0; i < probableWords.size(); i++) {
//                    System.out.println("handleSwipeTyping word: " + probableWords.get(i));
//                }
//                setSwipeSuggestions(probableWords);
//                switchToLowerCaseKeyboard();
//                prevWordSwiped = true;
//            }
//        }
//    }
//
//
//    private void buildDictionary() {
//        File destinationDirectory = new File(this.getFilesDir(), "dictionaries");
//        copyFileFromAssets("dictionaries/en/words.txt", new File(destinationDirectory, "words.txt"));
//        String path = new File(destinationDirectory, "words.txt").getAbsolutePath();
//        dictionary = readDictionaryFromFile(path);
//    }
//
//    private List<String> readDictionaryFromFile(String filePath) {
//        List<String> words = new ArrayList<>();
//
//        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
//            String line;
//            while ((line = br.readLine()) != null) {
//                words.add(line.trim());
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        return words;
//    }
//
//    private void copyFileFromAssets(String sourcePath, File destination) {
//        try {
//            InputStream in = this.getAssets().open(sourcePath);
//            OutputStream out = new FileOutputStream(destination);
//
//            byte[] buffer = new byte[1024];
//            int read;
//            while ((read = in.read(buffer)) != -1) {
//                out.write(buffer, 0, read);
//            }
//
//            in.close();
//            out.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//
//    private static boolean match(String path, String word) {
//        try {
//            for (char ch : word.toCharArray()) {
//                path = path.split(String.valueOf(ch), 2)[1];
//            }
//            return true;
//        } catch (Exception e) {
//            return false;
//        }
//    }
//
//    private static int getKeyboardRow(char ch) {
//        for (int rowNo = 0; rowNo < KEYBOARD_LAYOUT.length; rowNo++) {
//            if (KEYBOARD_LAYOUT[rowNo].contains(String.valueOf(ch))) {
//                return rowNo;
//            }
//        }
//        return -1; // Character not found in the keyboard layout
//    }
//
//    private static List<Integer> compress(List<Integer> sequence) {
//        List<Integer> retVal = new ArrayList<>();
//        retVal.add(sequence.get(0));
//
//        for (int element : sequence) {
//            if (retVal.get(retVal.size() - 1) != element) {
//                retVal.add(element);
//            }
//        }
//        return retVal;
//    }
//
//    private static int getMinimumWordLength(String path) {
//        List<Integer> rowNumbers = new ArrayList<>();
//        for (char ch : path.toCharArray()) {
//            rowNumbers.add(getKeyboardRow(ch));
//        }
//
//        List<Integer> compressedRowNumbers = compress(rowNumbers);
//        return compressedRowNumbers.size() - 3;
//    }
//
//    @RequiresApi(api = 34)
//    private static List<String> getSuggestions(LinkedList<Character> path) {
//
//        StringBuilder stringBuilder = new StringBuilder(path.size());
//        for (char c : path) {
//            stringBuilder.append(c);
//        }
//
//        String resultString = stringBuilder.toString();
//        List<String> suggestions = new ArrayList<>();
//
//        suggestions.addAll(dictionary.stream()
//                .filter(word -> word.charAt(0) == resultString.charAt(0) && word.charAt(word.length() - 1) == resultString.charAt(resultString.length() - 1))
//                .filter(word -> match(resultString, word))
//                .filter(word -> word.length() > getMinimumWordLength(resultString))
//                .toList());
//
//        return suggestions;
//    }
