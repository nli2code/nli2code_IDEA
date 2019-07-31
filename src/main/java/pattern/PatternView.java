package pattern;

import java.util.ArrayList;
import java.util.List;

public class PatternView {
    public String functionalFeature = "";
    public List<String> argsInfo = new ArrayList<>();
    public List<PatternElement> args = new ArrayList<>();

    public PatternView(String functionalFeature, List<String> argsInfo){
        this.functionalFeature = functionalFeature;
        for (String argInfo : argsInfo){
            this.argsInfo.add(argInfo);
        }
    }

    public int getCurrentHoleOffset(String indent){
        if (args.size() == argsInfo.size()){
            return -1;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(functionalFeature);
        sb.append(" {\n");
        for (int i=0;i<argsInfo.size();i++){
            sb.append(indent + "    " + argsInfo.get(i));
            sb.append("  :  ");
            if (args.size()>i){
                sb.append(args.get(i).element);
            } else if (args.size() == i){
                break;
            }
            sb.append("\n");
        }
        return sb.toString().length();
    }

    public String getText(String indent){
        StringBuilder sb = new StringBuilder();
        sb.append(functionalFeature);
        sb.append(" {\n");
        for (int i=0;i<argsInfo.size();i++){
            sb.append(indent + "    " + argsInfo.get(i));
            sb.append("  :  ");
            if (args.size()>i){
                sb.append(args.get(i).element);
            }
            sb.append("\n");
        }
        sb.append(indent + "}");

        return sb.toString();
    }
}
