package artificialair.cognitionfilter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.StringVisitable;

// a lot of this class is taken directly from SkyHanni's kotlin implementation
public class CognitionFilterManager {
    private static final Cache<String, String> stringCache = Caffeine.newBuilder()
        .maximumSize(131072)  // todo: figure out how memory inefficient this actually is
        .expireAfterAccess(5, TimeUnit.MINUTES)
        .build();
    private static final Cache<OrderedText, OrderedText> orderedTextCache = Caffeine.newBuilder()
        .maximumSize(65536)
        .expireAfterAccess(5, TimeUnit.MINUTES)
        .build();
    public static boolean changeWords = true;

    public static void emptyCache() {
        stringCache.invalidateAll();
        orderedTextCache.invalidateAll();
    }

    public static String modifyText(String string) {
        if (!changeWords || string == null || string.isEmpty()) return string;
        return stringCache.get(string, (value) -> {
            for (Rule rule : CognitionFilterConfig.getRules()) {
                if (!rule.enabled) continue;
                value = rule.apply(value);
            }

            return value;
        });
    }

    // i don't think this one needs a cache, it's not used nearly as often as the other two
    public static StringVisitable modifyText(StringVisitable stringVisitable) {
        if (!changeWords) return stringVisitable;
        List<StyledCharacter> sChars = new ArrayList<>();

        stringVisitable.visit((Style style, String text) -> {
            for (StyledCharacter sc : Rule.parseFormattedString(text)) {
                sChars.add(sc.withParentStyle(style));
            }
            return Optional.empty();
        }, Style.EMPTY);

        List<StyledCharacter> result = doReplacements(sChars);
        return StyledCharacter.listToText(result);
    }

    public static OrderedText modifyText(OrderedText orderedText) {
        if (!changeWords || orderedText == null) return orderedText;

        return orderedTextCache.get(orderedText, (value) -> {
            List<StyledCharacter> sChars = new ArrayList<>();
            boolean[] canReplace = {true};  // shit ass bypass for weird nested method error, fix later

            value.accept((int index, Style style, int codePoint) -> {
                if (codePoint == -1) {
                    canReplace[0] = false;
                    return true;
                }
                sChars.add(new StyledCharacter(codePoint, style));
                return true;
            });
            
            if (sChars.isEmpty()) return value;
            List<StyledCharacter> result = canReplace[0] ? doReplacements(sChars) : sChars;
            return StyledCharacter.listToText(result).asOrderedText();
        });
    }

    private static List<StyledCharacter> doReplacements(List<StyledCharacter> characters) {
        List<StyledCharacter> working = characters;

        for (Rule rule : CognitionFilterConfig.getRules()) {
            if (!rule.enabled) continue;
            List<StyledCharacter> phrase = rule.getParsedPhrase();
            if (phrase.isEmpty()) continue;

            StyledCharacter first = phrase.get(0);
            List<StyledCharacter> output = new ArrayList<>(working.size());
            boolean ruleMatched = false;
            int index = 0;
 
            while (index < working.size()) {
                boolean replaced = false;
 
                if (index <= working.size() - phrase.size()) {
                    StyledCharacter current = working.get(index);
                    if (codePointsMatch(current.codePoint(), first.codePoint(), rule.caseSensitive)
                            && stylesAreOverlapping(first.style(), current.style())) {
 
                        int subIndex = 1;
                        while (subIndex < phrase.size()) {
                            StyledCharacter pc = phrase.get(subIndex);
                            StyledCharacter tc = working.get(index + subIndex);
                            if (!codePointsMatch(tc.codePoint(), pc.codePoint(), rule.caseSensitive)
                                    || !stylesAreOverlapping(pc.style(), tc.style())) break;
                            subIndex++;
                        }
 
                        if (subIndex == phrase.size()) {
                            Style parentStyle = working.get(index).style();
                            for (StyledCharacter rc : rule.getParsedReplacement()) {
                                output.add(rc.withParentStyle(parentStyle));
                            }
                            index += subIndex;
                            replaced = true;
                            ruleMatched = true;
                        }
                    }
                }
 
                if (!replaced) {
                    output.add(working.get(index));
                    index++;
                }
            }

            if (ruleMatched) working = output;
        }
        return working;
    }

    private static boolean stylesAreOverlapping(Style pattern, Style text) {
        if (pattern.getColor() != null && !pattern.getColor().equals(text.getColor())) return false;
        if (pattern.isBold()          && !text.isBold())          return false;
        if (pattern.isItalic()        && !text.isItalic())        return false;
        if (pattern.isObfuscated()    && !text.isObfuscated())    return false;
        if (pattern.isUnderlined()    && !text.isUnderlined())    return false;
        if (pattern.isStrikethrough() && !text.isStrikethrough()) return false;
        return true;
    }
    
    private static boolean codePointsMatch(int a, int b, boolean caseSensitive) {
        return caseSensitive ? a == b : Character.toLowerCase(a) == Character.toLowerCase(b);
    }
}