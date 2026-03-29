package com.zeon.type_server.service;

import java.util.*;

// WordService.java
public interface WordService {
    String generateWordSet(int wordCount);
    List<String> generateWordList(int wordCount);
    boolean validateWord(String word); // For future anti-cheat
}
