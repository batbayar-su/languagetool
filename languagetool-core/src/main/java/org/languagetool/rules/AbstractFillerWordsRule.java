/* LanguageTool, a natural language style checker 
 * Copyright (C) 2005 Daniel Naber (http://www.danielnaber.de)
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301
 * USA
 */
package org.languagetool.rules;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import org.languagetool.AnalyzedSentence;
import org.languagetool.AnalyzedTokenReadings;
import org.languagetool.UserConfig;
import org.languagetool.rules.Category.Location;

/**
 * A rule that gives Hints on the use of filler words.
 * The Hints are only given when the percentage of filler words per paragraph exceeds the given limit.
 * A limit of 0 shows all used filler words. Direct speech or citation is excluded otherwise. 
 * This rule detects no grammar error but gives stylistic hints (default off).
 * @author Fred Kruse
 * @since 4.2
 */
public abstract class AbstractFillerWordsRule extends TextLevelRule {


  public static final String RULE_ID = "FILLER_WORDS";
  
  private static final int DEFAULT_MIN_PERCENT = 8;
  private static final Pattern NON_WORD_REGEX = Pattern.compile("[.?!…:;,~’'\"„“”»«‚‘›‹()\\[\\]\\-–—*×∗·+÷/=]");
  private static final Pattern OPENING_QUOTES = Pattern.compile("[\"„”»«]");
  private static final Pattern ENDING_QUOTES = Pattern.compile("[\"“»«]");
  private static final boolean DEFAULT_ACTIVATION = false;

  private int minPercent = DEFAULT_MIN_PERCENT;


  /*
   * Override this to detect filler words in the specified language
   */
  protected abstract boolean isFillerWord(String token);
  
  public AbstractFillerWordsRule(ResourceBundle messages, UserConfig userConfig, boolean defaultActive) {
    super(messages);
    super.setCategory(new Category(new CategoryId("CREATIVE_WRITING"), 
        messages.getString("category_creative_writing"), Location.INTERNAL, false));
    if (!defaultActive) {
      setDefaultOff();
    }
    if (userConfig != null) {
      int confPercent = userConfig.getConfigValueByID(getId());
      if(confPercent >= 0) {
        this.minPercent = confPercent;
      }
    }
    setLocQualityIssueType(ITSIssueType.Style);
  }

  public AbstractFillerWordsRule(ResourceBundle messages, UserConfig userConfig) {
    this(messages, userConfig, DEFAULT_ACTIVATION);
  }

  @Override
  public String getDescription() {
    return messages.getString("filler_words_rule_desc");
  }

  @Override
  public String getId() {
    return RULE_ID;
  }

  @Override
  public int getDefaultValue() {
    return minPercent;
  }

  @Override
  public boolean hasConfigurableValue() {
    return true;
  }

  @Override
  public int getMinConfigurableValue() {
    return 0;
  }

  @Override
  public int getMaxConfigurableValue() {
    return 100;
  }

  public String getConfigureText() {
    return messages.getString("filler_words_rule_opt_text");
  }

  public String getMessage() {
    return messages.getString("filler_words_rule_msg");
  }
  
  protected boolean isException(AnalyzedTokenReadings[] tokens, int num) {
    return false;
  }
  
  @Override
  public RuleMatch[] match(List<AnalyzedSentence> sentences) throws IOException {
    List<RuleMatch> ruleMatches = new ArrayList<>();
    String msg = getMessage();
    List<Integer> startPos = new ArrayList<Integer>();
    List<Integer> endPos = new ArrayList<Integer>();
    double percent = 0;
    int pos = 0;
    int wordCount = 0;
    boolean isDirectSpeech = false;
    for(AnalyzedSentence sentence : sentences) {
      AnalyzedTokenReadings[] tokens = sentence.getTokens();
      for(int n = 0; n < tokens.length; n++) {
        AnalyzedTokenReadings token = tokens[n];
        String sToken = token.getToken();
        if(OPENING_QUOTES.matcher(sToken).matches() && n < tokens.length -1 && !tokens[n + 1].isWhitespace()) {
          isDirectSpeech = true;
        }
        else if(ENDING_QUOTES.matcher(sToken).matches() 
            && !tokens[n - 1].isWhitespace() && !tokens[n - 1].isSentenceStart()) {
          isDirectSpeech = false;
        }
        else if((!isDirectSpeech || minPercent == 0) && !token.isWhitespace() && !token.isSentenceStart() 
            && !token.isSentenceEnd() && !NON_WORD_REGEX.matcher(sToken).matches()) {
          wordCount++;
          if(isFillerWord(sToken) && !isException(tokens, n)) {
            startPos.add(token.getStartPos() + pos);
            endPos.add(token.getEndPos() + pos);
          }
        } else if ("\n".equals(sToken) || "\r\n".equals(sToken) || "\n\r".equals(sToken)) {
          if(wordCount > 0) {
            percent = startPos.size() * 100.0 / wordCount;
          } else {
            percent = 0;
          }
          if (percent > minPercent) {
            for (int i = 0; i < startPos.size(); i++) {
              RuleMatch ruleMatch = new RuleMatch(this, startPos.get(i), endPos.get(i), msg);
              ruleMatches.add(ruleMatch);
            }
          }
          wordCount = 0;
          startPos = new ArrayList<Integer>();
          endPos = new ArrayList<Integer>();
        }
      }
      pos += sentence.getText().length();
    }
    if(wordCount > 0) {
      percent = startPos.size() * 100.0 / wordCount;
    } else {
      percent = 0;
    }
    if (percent > minPercent) {
      for (int i = 0; i < startPos.size(); i++) {
        RuleMatch ruleMatch = new RuleMatch(this, startPos.get(i), endPos.get(i), msg);
        ruleMatches.add(ruleMatch);
      }
    }
    return toRuleMatchArray(ruleMatches);
  }

  static class DialogThread extends Thread {
    private final String text;

    DialogThread(String text) {
      this.text = text;
    }

    @Override
    public void run() {
      JOptionPane.showMessageDialog(null, text);
    }
  }

  void showMessage(String txt) {
    DialogThread dt = new DialogThread(txt);
    dt.run();
  }
  
}
