package artificialair.cognitionfilter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class CognitionFilterConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("CognitionFilter");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type RULE_LIST_TYPE = new TypeToken<List<Rule>>() {}.getType();

    private static Path configPath;
    private static final List<Rule> rules = new ArrayList<>();

    public static class Rule {
        public String phrase;
        public String replacement;
        public boolean enabled = true;
        public boolean caseSensitive = false;
        public boolean useRegex = false;

        private transient String processedPhrase;
        private transient String processedReplacement;
        private transient Pattern compiledPattern;
        private transient boolean valid = true;

        public Rule() {}

        public Rule(String phrase, String replacement) {
            this.phrase = phrase;
            this.replacement = replacement;
        }

        void prepare() {
            processedPhrase      = processSpecialChars(phrase);
            processedReplacement = processSpecialChars(replacement);

            try {
                int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
                processedPhrase = useRegex ? processedPhrase : Pattern.quote(processedPhrase);
                processedReplacement = useRegex ? processedReplacement : Matcher.quoteReplacement(processedReplacement);
                compiledPattern = Pattern.compile(processedPhrase, flags);
            } catch (PatternSyntaxException e) {
                valid = false;
            }
        }

        /**
         * Applies this rule to the given input string.
         *
         * Plain strings: Pattern.quote() escapes the phrase so characters like
         * ".", "(", "*" are treated as literals. Matcher.quoteReplacement() does
         * the same for the replacement side so "$1" isn't read as a back-reference.
         *
         * Regex strings: both sides are used as-is, giving full regex power
         * including capture groups in the replacement (e.g. "$1").
         */
        public String apply(String input) {
            if (!valid || compiledPattern == null || input == null || input.isEmpty()) {
                return input;
            }

            if (useRegex) {
                return compiledPattern.matcher(input).replaceAll(replacement);
            } else {
                return input.replaceAll(processedPhrase, processedReplacement);
            }
        }
    }

    private static String processSpecialChars(String s) {  // i might want more of these later? (newline is weird, !!)
        if (s == null) return null;
        return s.replace("&&", "§");
    }

    public static void init(Path configDir) {
        configPath = configDir.resolve("cognitionfilter.json");
        if (!Files.exists(configPath)) {
            rules.add(new Rule("example phrase", "example replacement"));
            save();
            rules.clear();
        } else {
            load();
        }
    }

    public static void load() {
        CognitionFilterManager.emptyCache();
        if (configPath == null || !Files.exists(configPath)) return;
        try (Reader reader = Files.newBufferedReader(configPath)) {
            List<Rule> loaded = GSON.fromJson(reader, RULE_LIST_TYPE);
            rules.clear();
            if (loaded != null) {
                for (Rule rule : loaded) {
                    if (rule.useRegex && rule.phrase != null) {
                        try {
                            Pattern.compile(rule.phrase);
                        } catch (PatternSyntaxException e) {
                            LOGGER.warn("CognitionFilter: invalid regex \"{}\": {}",
                                    rule.phrase, e.getMessage());
                        }
                    }
                    rule.prepare();
                    rules.add(rule);
                }
            }
            LOGGER.info("CognitionFilter: loaded {} rule(s)", rules.size());
        } catch (IOException e) {
            LOGGER.error("CognitionFilter: failed to load config", e);
        }
    }

    public static void save() {
        if (configPath == null) return;
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                GSON.toJson(rules, writer);
            }
        } catch (IOException e) {
            LOGGER.error("CognitionFilter: failed to save config", e);
        }
    }

    public static List<Rule> getRules() {
        return rules;
    }
}