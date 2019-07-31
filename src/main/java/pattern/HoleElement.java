package pattern;

import java.util.List;

public class HoleElement extends PatternElement {
    public String type;
    public List<String> options;

    public HoleElement(String type, List<String> options) {
        super("HOLE");
        this.type = type;
        this.options = options;
    }
}
