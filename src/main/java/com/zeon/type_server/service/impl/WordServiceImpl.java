package com.zeon.type_server.service.impl;

import com.zeon.type_server.service.WordService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

// WordServiceImpl.java
@Service
public class WordServiceImpl implements WordService {

    private static final String[] WORD_POOL = {
            "cat", "dog", "tree", "sun", "moon", "star", "fish", "bird",
            "book", "house", "car", "phone", "computer", "keyboard",
            "monitor", "window", "door", "water", "fire", "earth",
            "swift", "java", "python", "rust", "react", "vue", "angular",
            "happy", "sad", "fast", "slow", "big", "small", "hot", "cold"
    };

    private static final Random RANDOM = new Random();

    @Override
    public String generateWordSet(int wordCount) {
        return generateWordList(wordCount)
                .stream()
                .collect(Collectors.joining(" "));
    }

    @Override
    public List<String> generateWordList(int wordCount) {
        List<String> words = new ArrayList<>();
        for (int i = 0; i < wordCount; i++) {
            words.add(WORD_POOL[RANDOM.nextInt(WORD_POOL.length)]);
        }
        return words;
    }

    @Override
    public boolean validateWord(String word) {
        return Arrays.asList(WORD_POOL).contains(word);
    }
}
