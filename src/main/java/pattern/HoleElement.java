package pattern;


public class HoleElement extends PatternElement {
    public String type;
    public String info;

    public HoleElement(String type,String info,int postfix) {
        super("HOLE",postfix);
        this.type = type;
        this.info = info;
    }
}
