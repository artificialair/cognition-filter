package artificialair.cognitionfilter;

import net.minecraft.text.CharacterVisitor;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;

import java.util.Optional;

public class CognitionFilterManager {
    public static String modifyString(String value) {
        if (value == null || value.isEmpty()) return value;
        for (CognitionFilterConfig.Rule rule : CognitionFilterConfig.getRules()) {
            if (!rule.enabled) continue;
            value = rule.apply(value);
        }
        return value;
    }

    public static StringVisitable modifyFormattedText(StringVisitable value) {
        MutableText result = Text.empty();
        value.visit((Style style, String text) -> {
            result.append(Text.literal(modifyString(text)).setStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        return result;
    }

    public static OrderedText modifyOrderedText(OrderedText value) {
        MutableText result = Text.empty();
        final StringBuilder currentText = new StringBuilder();
        final Style[] currentStyle = new Style[1];

        value.accept(new CharacterVisitor() {
            @Override
            public boolean accept(int index, Style style, int codePoint) {
                if (currentStyle[0] == null) {
                    currentStyle[0] = style;
                }
                if (!style.equals(currentStyle[0])) {
                    result.append(Text.literal(modifyString(currentText.toString())).setStyle(currentStyle[0]));
                    currentText.setLength(0);
                    currentStyle[0] = style;
                }
                currentText.appendCodePoint(codePoint);
                return true;
            }
        });

        if (!currentText.isEmpty()) {
            result.append(Text.literal(modifyString(currentText.toString())).setStyle(currentStyle[0]));
        }

        return result.asOrderedText();
    }
}