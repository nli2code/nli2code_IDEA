package pattern;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Pattern {
    public List<PatternElement> elements = new LinkedList<>();
    public List<Integer> holePos = new ArrayList<>();
    public int currentHole = 0;
    public PatternView patternView;
    public Map<String,String> symbolFQN;

    public Pattern(List<String> patternText,String functionalFeature, List<String> holesInfo, List<String> holesType, List<List<String>> holesOptions, Map<String,String> symbolFQN){
        int holeCnt = 0;
        for (int i=0;i<patternText.size();i++){
            String seg = patternText.get(i);
            if (seg.equals("HOLE")){
                holePos.add(i);
                elements.add(new HoleElement(holesType.get(holeCnt),holesOptions.get(holeCnt)));
                holeCnt += 1;
            }else {
                elements.add(new PatternElement(seg));
            }
        }
        patternView = new PatternView(functionalFeature,holesInfo);
        this.symbolFQN = symbolFQN;
    }

    public String getView(String indent){
        return patternView.getText(indent);
    }
    public int getViewSize(String indent){
        return getView(indent).length();
    }
    public int getCurrentHoleOffset(String indent){
        return patternView.getCurrentHoleOffset(indent);
    }

        public String getCode(String indent){
        StringBuilder sb = new StringBuilder();
        for (PatternElement element : elements){
            sb.append(element.element);
            if (element.element.endsWith("\n")){
                sb.append(indent);
            }
        }
        return sb.toString();
    }
    public int getCodeSize(String indent){
        return getCode(indent).length();
    }

    public int getNextHoleId(){
        if (currentHole >= holePos.size()){
            return -1;
        }else{
            return currentHole;
        }
    }

    public HoleElement getHole(int holeId){
        return (HoleElement) elements.get(holePos.get(holeId));
    }

    public void fill(PatternElement expr){
        elements.remove(holePos.get(currentHole).intValue());
        elements.add(holePos.get(currentHole),expr);
        patternView.args.add(expr);
        currentHole += 1;
    }
}
