package pattern;

import java.util.regex.Matcher;

public class Test {
    public static void main(String[] args) {
        String a = "cell_50 f cell_5 = asas".replaceAll("cell_5","cell_1");
        System.out.println(a);
    }
}
