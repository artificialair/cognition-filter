package artificialair.cognitionfilter;

import java.util.List;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.Style;

// today i learned about records (think of all the lines you could save!)
public record StyledCharacter (int codePoint, Style style) {
    public StyledCharacter withParentStyle(Style parentStyle) {
        return new StyledCharacter(codePoint, style.withParent(parentStyle));
    }

    public static MutableText listToText(List<StyledCharacter> chars) {
        MutableText result = Text.empty();
        int i = 0;
        while (i < chars.size()) {
            Style sstyle = chars.get(i).style();
            StringBuilder sb = new StringBuilder();
            while (i < chars.size() && chars.get(i).style().equals(sstyle)) {
                sb.appendCodePoint(chars.get(i++).codePoint());
            }
            result.append(Text.literal(sb.toString()).setStyle(sstyle));
        }
        return result;
    }
}
