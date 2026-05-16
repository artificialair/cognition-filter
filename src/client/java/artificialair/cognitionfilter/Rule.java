package artificialair.cognitionfilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.minecraft.text.Style;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

public class Rule {
    public String phrase;
    public String replacement;
    public boolean enabled = true;
    public boolean caseSensitive = false;
    public boolean useRegex = false;

    private transient String processedPhrase;  // these are kinda placeholdery but eventually they'll do more
    private transient String processedReplacement;  
    private transient Pattern compiledPattern;

    private transient List<StyledCharacter> parsedPhrase = Collections.emptyList();
    private transient List<StyledCharacter> parsedReplacement = Collections.emptyList();

    public Rule() {}

    public Rule(String phrase, String replacement) {
        this.phrase = phrase;
        this.replacement = replacement;
    }

    void prepare() {
        processedPhrase      = phrase.replace("&&", "§");
        processedReplacement = replacement.replace("&&", "§");

        if (!useRegex) {
            int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
            compiledPattern = Pattern.compile(Pattern.quote(processedPhrase), flags);
        } else {
            try {
                int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
                compiledPattern = Pattern.compile(processedPhrase, flags);
            } catch (PatternSyntaxException e) {
                compiledPattern = null;  // warn about this later  so it doesnt     crash
            }
        }

        parsedPhrase      = parseFormattedString(processedPhrase);
        parsedReplacement = parseFormattedString(processedReplacement);

    }

    public String apply(String input) {
        if (compiledPattern == null || input == null || input.isEmpty()) {
            return input;
        }
        String replace = useRegex ? processedReplacement : Matcher.quoteReplacement(processedReplacement);

        return compiledPattern.matcher(input).replaceAll(replace);
    }

    public Pattern getCompiledPattern() {
        return compiledPattern;
    }

    public String getProcessedReplacement() {
        return processedReplacement;
    }

    public List<StyledCharacter> getParsedPhrase() { 
        return parsedPhrase; 
    }

    public List<StyledCharacter> getParsedReplacement() { 
        return parsedReplacement; 
    }

    // everything below this comment should probably be in another file, eh
    static List<StyledCharacter> parseFormattedString(String text) {
        if (text == null || text.isEmpty()) return Collections.emptyList();
        List<StyledCharacter> result = new ArrayList<>();
        Style currentStyle = Style.EMPTY;
        boolean withParent = true;
 
        for (int i = 0; i < text.length(); ) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                char code = text.charAt(i + 1);
                Formatting fmt = Formatting.byCode(code);
                if (fmt != null) {
                    currentStyle = applyFormatting(currentStyle, fmt);
                    withParent = false;
                }
                i += 2;
            } else {
                int cp = text.codePointAt(i);
                result.add(new StyledCharacter(cp, currentStyle, withParent));
                i += Character.charCount(cp);
            }
        }
        return result;
    }

    private static Style applyFormatting(Style style, Formatting fmt) {
        if (fmt == Formatting.RESET) return Style.EMPTY;
        if (fmt.isColor()) return Style.EMPTY.withColor(TextColor.fromFormatting(fmt));
        return switch (fmt) {
            case BOLD          -> style.withBold(true);
            case ITALIC        -> style.withItalic(true);
            case UNDERLINE     -> style.withUnderline(true);
            case STRIKETHROUGH -> style.withStrikethrough(true);
            case OBFUSCATED    -> style.withObfuscated(true);
            default            -> style;
        };
    }
}