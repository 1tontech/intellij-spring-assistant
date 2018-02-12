package com.miguelfonseca.completely.text.analyze.tokenize;

import gnu.trove.THashSet;

import java.io.InputStream;
import java.util.Collection;
import java.util.Scanner;
import java.util.Set;

public class StopwordsAwareWordTokenizer extends WordTokenizer {

  private Set<String> stopwords = new THashSet<>();

  public StopwordsAwareWordTokenizer() {
    InputStream resourceAsStream = getClass().getResourceAsStream("/stopwords-en.txt");
    try (Scanner scanner = new Scanner(resourceAsStream)) {

      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        if (line == null) {
          continue;
        }
        line = line.trim();
        line = line.toLowerCase();
        if (line.isEmpty()) {
          continue;
        }
        stopwords.add(line);
      }

      scanner.close();
    }
  }

  @Override
  public Collection<String> apply(String... input) {
    Collection<String> tokens = super.apply(input);
    tokens.removeAll(stopwords);
    return tokens;
  }

}
