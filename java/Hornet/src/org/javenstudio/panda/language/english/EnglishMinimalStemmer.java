package org.javenstudio.panda.language.english;

/**
 * Minimal plural stemmer for English.
 * <p>
 * This stemmer implements the "S-Stemmer" from
 * <i>How Effective Is Suffixing?</i>
 * Donna Harman.
 */
public class EnglishMinimalStemmer {
	
	@SuppressWarnings("fallthrough")
	public int stem(char s[], int len) {
		if (len < 3 || s[len-1] != 's')
			return len;
    
		switch (s[len-2]) {
		case 'u':
		case 's': 
			return len;
			
		case 'e':
			if (len > 3 && s[len-3] == 'i' && s[len-4] != 'a' && s[len-4] != 'e') {
				s[len - 3] = 'y';
				return len - 2;
			}
			
			if (s[len-3] == 'i' || s[len-3] == 'a' || s[len-3] == 'o' || s[len-3] == 'e')
				return len; /** intentional fallthrough */
			
		default: 
			return len - 1;
		}
	}
  
}
