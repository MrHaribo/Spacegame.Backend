package Spacegame.WorldService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class NameGeneration
{
    //Ispired by:
    //http://www.dialectcreator.com/

    private static List<Character> consonants = new ArrayList<>(Arrays.asList('b', 'c', 'd', 'f', 'g', 'h', 'j', 'k', 'l', 'm', 'n', 'p', 'q', 'r', 's', 't', 'v', 'w', 'x', 'y', 'z'));
    private static List<Character>  vowels = new ArrayList<>(Arrays.asList('a', 'e', 'i', 'o', 'u' ));

    private static String[] syllables = {"a", "ab", "ad", "af", "ag", "ah", "ak", "al", "am", "an", "ang", "ap", "ar", "as", "ash", "at", "ath", "av", "aw", "ay", "az",
        "ba", "be", "bi", "bla", "ble", "bli", "blo", "blu", "bo", "bra", "bre", "bri", "bro", "bru", "bu", "ce", "cla", "cle", "cli", "clo", "clu", "cra",
        "cro", "cru", "da", "de", "di", "do", "dra", "dre", "dri", "dro", "dru", "du", "e", "eb", "ed", "ef", "eg", "eh", "ek", "el", "em",
        "en", "eng", "ep", "er", "es", "esh", "et", "eth", "ev", "ew", "ey", "ez", "fa", "fe", "fi", "fla", "fle", "fli", "flo",
        "flu", "fo", "fra", "fre", "fri", "fro", "fru", "fu", "ga", "ge", "gi", "gla", "gle", "gli", "glo", "glu", "go", "gra", "gre", "gri", "gro",
        "gru", "gu", "ha", "he", "hi", "ho", "hu", "i", "ib", "id",	"if", "ig", "ih", "ik", "il", "im", "in", "ing", "ip", "ir", "is",  "ish",
        "it", "ith", "iv", "iw", "iy", "iz", "ka", "ke", "ki", "kla", "kle", "kli", "klo", "klu", "ko", "kra", "kre", "kri", "kro",
        "kru", "ku", "la", "le", "li", "lo", "lu", "ly", "ma", "me", "mi", "mo", "mu", "na", "ne", "ni", "no", "nu", "o",
        "ob", "od", "of", "og", "oh", "ok", "ol", "om", "on", "ong", "op", "or", "os", "osh", "ot", "oth", "ov", "owoy",
        "oz", "pa", "pe", "pi", "pla", "ple", "pli", "plo", "plu", "po", "pra", "pre", "pri", "pro", "pru", "pu", "ra", "re", "ri",
        "ro", "ru", "sa", "se", "sha", "she", "shi", "sho", "shu", "si", "sla", "sle", "sli", "slo", "slu", "so", "su", "ta", "te", "tha",
        "the", "thi", "tho", "thra", "thre", "thri", "thro", "thru", "thu", "thy", "ti", "to", "tra", "tre", "tri", "tro", "tru", "tu", "u","ub",
        "ud", "uf", "ug", "uh", "uk", "ul", "um", "un", "ung", "up", "ur", "us", "ush", "ut", "uth", "uv", "uw", "uy", "uz",
        "va", "ve", "vi", "vla", "vle", "vli", "vlo", "vlu", "vo", "vra", "vre", "vri", "vro", "vru", "vu", "wa", "we", "wi", "wo", "wra", "wre",
        "wri", "wro", "wru", "wu", "ya", "ye", "yi", "yl", "yo", "yth", "yu", "za", "ze", "zi", "zla", "zle", "zli", "zlo", "zlu",
        "zo", "zra", "zre", "zri", "zro", "zru", "zu" };

    private static int minSyllables = 2;
    private static int maxSyllables = 4;
    
    private static float consonantInjectionProbability = 0.5f;
    private static float consonantAppendProbability = 0.0f;

    private static float vowelInjectionProbability = 0.5f;
    private static float vowelAppendProbability = 0.5f;

    public static String RandomWordCapital(long seed)
    {
        String word = RandomWord(seed);
        return word.substring(0, 1).toUpperCase() + word.substring(1);
    }

    public static String RandomWord(long seed)
    {
        Random wordRandom = new Random(seed);

        int numSyllables = wordRandom.nextInt(maxSyllables - minSyllables) + minSyllables;

        int idx = wordRandom.nextInt(syllables.length);
        String currentSyllable = syllables[idx];
        String word = currentSyllable;

        for (int i = 1; i < numSyllables; i++)
        {
            idx = wordRandom.nextInt(syllables.length);
            String nextSyllable = syllables[idx];

            char lastChar = currentSyllable.charAt(currentSyllable.length() - 1);
            char nextChar = nextSyllable.charAt(0);

            boolean lastIsVowel = vowels.contains(lastChar);
            boolean nextIsVowel = vowels.contains(nextChar);

            // insert vowel
            if (!lastIsVowel && !nextIsVowel)
            {
                float insetVowel = wordRandom.nextFloat();
                if (insetVowel < vowelInjectionProbability)
                    word += vowels.get(wordRandom.nextInt(vowels.size()));
            }

            // insert consonant
            if (lastIsVowel && nextIsVowel)
            {
                float insetConsonant = wordRandom.nextFloat();
                if (insetConsonant < consonantInjectionProbability)
                    word += consonants.get(wordRandom.nextInt(consonants.size()));
            }

            word += nextSyllable;
            currentSyllable = nextSyllable;
        }

        // append consonant
        boolean lastCharIsVowel = vowels.contains(word.charAt(word.length() - 1));
        float appendConsonant = wordRandom.nextFloat();
        if (lastCharIsVowel && appendConsonant < consonantAppendProbability)
        {
            word += consonants.get(wordRandom.nextInt(consonants.size()));
        }
        else
        {
            float appendVowel = wordRandom.nextFloat();
            if (appendVowel < vowelAppendProbability)
                word += vowels.get(wordRandom.nextInt(vowels.size()));
        }

        return word;
    }
}
