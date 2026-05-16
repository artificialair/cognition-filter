package artificialair.cognitionfilter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import net.minecraft.text.Text;
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
                sChars.add(new StyledCharacter(codePoint, style, true));
                return true;
            });
            
            if (sChars.isEmpty()) return value;
            List<StyledCharacter> result = canReplace[0] ? doReplacements(sChars) : sChars;
            return StyledCharacter.listToText(result).asOrderedText();
        });
    }

    public static List<Text> modifyTooltipLines(List<Text> lines) {
        if (!changeWords || lines.isEmpty()) return lines;

        List<StyledCharacter> joined = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) joined.add(new StyledCharacter('\n', Style.EMPTY, true));
            lines.get(i).visit((style, text) -> {
                for (StyledCharacter sc : Rule.parseFormattedString(text))
                    joined.add(sc.withParentStyle(style));
                return Optional.empty();
            }, Style.EMPTY);
        }

        List<StyledCharacter> result = doReplacements(joined, CognitionFilterConfig.getMultiLineRules());
        List<Text> output = new ArrayList<>();
        List<StyledCharacter> current = new ArrayList<>();
        for (StyledCharacter sc : result) {
            if (sc.codePoint() == '\n') {
                output.add(StyledCharacter.listToText(current));
                current = new ArrayList<>();
            } else {
                current.add(sc);
            }
        }
        output.add(StyledCharacter.listToText(current));
        return output;
    }

    private static List<StyledCharacter> doReplacements(List<StyledCharacter> characters) {
        return doReplacements(characters, CognitionFilterConfig.getRules());
    }
    
    private static List<StyledCharacter> doReplacements(List<StyledCharacter> characters, List<Rule> rules) {
        List<StyledCharacter> working = characters;

        for (Rule rule : rules) {
            if (!rule.enabled) continue;

            Pattern p = rule.getCompiledPattern();
            if (p == null) continue;

            StringBuilder plain = new StringBuilder();
            List<Integer> charToScIdx = new ArrayList<>();
            for (int i = 0; i < working.size(); i++) {
                int cp = working.get(i).codePoint();
                charToScIdx.add(i);
                if (Character.charCount(cp) == 2) charToScIdx.add(i);
                plain.appendCodePoint(cp);
            }
            charToScIdx.add(working.size());

            Matcher matcher = p.matcher(plain);
            List<StyledCharacter> output = new ArrayList<>();
            int lastScEnd = 0;
            boolean anyMatch = false;

            while (matcher.find()) {
                int scStart = charToScIdx.get(matcher.start());
                int scEnd = charToScIdx.get(matcher.end());

                output.addAll(working.subList(lastScEnd, scStart));

                String resolved = resolveReplacement(matcher, rule.getProcessedReplacement());
                Style parentStyle = scStart < working.size() ? working.get(scStart).style() : Style.EMPTY;
                for (StyledCharacter rc : Rule.parseFormattedString(resolved)) {
                    output.add(rc.withParentStyle(parentStyle));
                }

                lastScEnd = scEnd;
                anyMatch = true;
            }

            if (anyMatch) {
                output.addAll(working.subList(lastScEnd, working.size()));
                working = output;
            }
        }
        return working;
    }

    // mess for $1 style replacements
    private static String resolveReplacement(Matcher matcher, String template) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < template.length()) {
            char c = template.charAt(i);
            if (c == '\\' && i + 1 < template.length()) {
                sb.append(template.charAt(i + 1));
                i += 2;
            } else if (c == '$' && i + 1 < template.length() && Character.isDigit(template.charAt(i + 1))) {
                int groupNum = 0;
                i++;
                while (i < template.length() && Character.isDigit(template.charAt(i))) {
                    groupNum = groupNum * 10 + (template.charAt(i) - '0');
                    i++;
                }
                String g = matcher.group(groupNum);
                if (g != null) sb.append(g);
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }
}