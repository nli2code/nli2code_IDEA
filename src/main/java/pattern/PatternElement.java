package pattern;

public class PatternElement {
    public String element;

    public PatternElement(String element,int postfix) {
        this.element = element.replaceAll("_","_" + postfix);
    }
    public PatternElement(String element) {
        this.element = element;
    }
}
