package artificialair.cognitionfilter;

import java.util.List;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.Style;

// today i learned about records (think of all the lines you could save!)
public record StyledCharacter (int codePoint, Style style, boolean withParent) {
    public StyledCharacter withParentStyle(Style parentStyle) {
        if (!withParent) return this;  // used to make &&r actually reset the formatting (skyhanni does NOT do this)
        return new StyledCharacter(codePoint, style.withParent(parentStyle), false);
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
