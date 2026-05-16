package artificialair.cognitionfilter;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CognitionFilterConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("CognitionFilter");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type RULE_LIST_TYPE = new TypeToken<List<Rule>>() {}.getType();

    private static Path configPath;
    private static final List<Rule> rules = new ArrayList<>();
    private static final List<Rule> multiLineRules = new ArrayList<>();

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
        if (configPath == null || !Files.exists(configPath)) return;
        try (Reader reader = Files.newBufferedReader(configPath)) {
            List<Rule> loaded = GSON.fromJson(reader, RULE_LIST_TYPE);
            rules.clear();
            multiLineRules.clear();
            if (loaded != null) {
                for (Rule rule : loaded) {
                    if (rule.useRegex && rule.phrase != null) {
                        try {
                            Pattern.compile(rule.phrase);
                        } catch (PatternSyntaxException e) {
                            LOGGER.warn("[CognitionFilter] invalid regex \"{}\": {}",
                                rule.phrase, e.getMessage());
                        }
                    }
                    rule.prepare();
                    rules.add(rule);
                    if (rule.phrase != null && rule.phrase.contains("\n")) {
                        multiLineRules.add(rule);
                    }
                }
            }
            LOGGER.info("[CognitionFilter] loaded {} rule(s)", rules.size());
        } catch (IOException e) {
            LOGGER.error("[CognitionFilter] loading broke, tell artificialair about this probably", e);
        }
        CognitionFilterManager.emptyCache();
    }

    public static void save() {
        if (configPath == null) return;
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                GSON.toJson(rules, writer);
            }
        } catch (IOException e) {
            LOGGER.error("[CognitionFilter] saving broke, which is amazing since it isn't even implemented yet", e);
        }
    }

    public static List<Rule> getRules() {
        return rules;
    }
    
    public static List<Rule> getMultiLineRules() {
        return multiLineRules;
    }
}