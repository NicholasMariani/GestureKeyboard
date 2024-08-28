package com.admin.keyboard.swipe;


import java.util.Map;

class Trie {
    private TrieNode root;

    Trie() {
        root = new TrieNode();
    }

    void insert(String word) {
        TrieNode current = root;

        for (char l : word.toCharArray()) {
            current = current.getChildren().computeIfAbsent(l, c -> new TrieNode());
        }
        current.setEndOfWord(true);
    }

    boolean delete(String word) {
        return delete(root, word, 0);
    }

    boolean containsNode(String word) {
        TrieNode current = root;

        for (int i = 0; i < word.length(); i++) {
            char ch = word.charAt(i);
            TrieNode node = current.getChildren().get(ch);
            if (node == null) {
                return false;
            }
            current = node;
        }
        return current.isEndOfWord();
    }

    boolean isEmpty() {
        return root == null;
    }

    private boolean delete(TrieNode current, String word, int index) {
        if (index == word.length()) {
            if (!current.isEndOfWord()) {
                return false;
            }
            current.setEndOfWord(false);
            return current.getChildren().isEmpty();
        }
        char ch = word.charAt(index);
        TrieNode node = current.getChildren().get(ch);
        if (node == null) {
            return false;
        }
        boolean shouldDeleteCurrentNode = delete(node, word, index + 1) && !node.isEndOfWord();

        if (shouldDeleteCurrentNode) {
            current.getChildren().remove(ch);
            return current.getChildren().isEmpty();
        }
        return false;
    }

    public boolean search(String word) {
        TrieNode node = searchNode(word);
        return node != null && node.isEndOfWord();
    }

    public boolean startsWith(String prefix) {
        TrieNode node = searchNode(prefix);
        return node != null;
    }

    private TrieNode searchNode(String prefix) {
        TrieNode current = root;

        for (char ch : prefix.toCharArray()) {
            if (!current.getChildren().containsKey(ch)) {
                return null;
            }
            current = current.getChildren().get(ch);
        }

        return current;
    }

    // Get all words with a given prefix
    public java.util.List<String> getWordsWithPrefix(String prefix) {
        TrieNode node = searchNode(prefix);
        java.util.List<String> result = new java.util.ArrayList<>();

        if (node != null) {
            collectWords(node, new StringBuilder(prefix), result);
        }

        return result;
    }

    private void collectWords(TrieNode node, StringBuilder currentWord, java.util.List<String> result) {
        if (node.isEndOfWord()) {
            result.add(currentWord.toString());
        }

        for (Map.Entry<Character, TrieNode> entry : node.getChildren().entrySet()) {
            collectWords(entry.getValue(), currentWord.append(entry.getKey()), result);
            currentWord.deleteCharAt(currentWord.length() - 1);
        }
    }
//
//        public List<String> findWords(char[] orderedSet) {
//            List<String> foundWords = new ArrayList<>();
//            StringBuilder currentWord = new StringBuilder();
//            TrieNode node = root;
//
//            for (char ch : orderedSet) {
//                if (ch != ' ') {
//                    if (node.children.get(ch) != null) {
//                        currentWord.append(ch);
//                        node = node.children.get(ch);
//
//                        if (node.isWord) {
//                            foundWords.add(currentWord.toString());
//                        }
//                    } else {
//                        // Reset Trie traversal
//                        currentWord.setLength(0);
//                        node = root;
//                    }
//                }
//            }
//
//            return foundWords;
//        }
//    }
}

