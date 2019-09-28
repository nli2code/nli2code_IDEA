package pattern;

import response.ContextVariable;
import response.NLI;

import java.util.*;
import java.util.regex.Matcher;

public class Pattern{
    public List<PatternElement> elements = new LinkedList<>();
    public Map<Integer,List<Integer>> holePos = new HashMap<>();
    public int currentHole = 0;
    public PatternView patternView;
    public Map<String, String> symbolFQN;
    public String functionalFeature;
    private static int posfix = 0;


    public void initPattern(List<String> patternText, String functionalFeature, List<String> holesInfo, List<String> holesType, Map<String, String> symbolFQN) {
        posfix += 1;
        for (int i = 0; i < patternText.size(); i++) {
            String seg = patternText.get(i);
            if (seg.startsWith("<HOLE")) {
                int holeId = Integer.parseInt(seg.substring(5,seg.length()-1));
                List<Integer> holeIdPos = holePos.getOrDefault(holeId,new ArrayList<>());
                holeIdPos.add(i);
                holePos.put(holeId,holeIdPos);

                elements.add(new HoleElement(holesType.get(holeId),holesInfo.get(holeId),posfix));
            } else {
                elements.add(new PatternElement(seg,posfix));
            }
        }
        patternView = new PatternView(functionalFeature, holesInfo);
        this.symbolFQN = symbolFQN;
        this.functionalFeature = functionalFeature;
    }

    public Pattern(NLI nli){
        initPattern(nli.getText(),nli.getFunctionalFeature(),nli.getInfo(),nli.getType(),nli.getSymbol());
    }

    public String getFunctionalFeature() {
        return functionalFeature;
    }

    public String getView(String indent) {
        return patternView.getText(indent);
    }

    public int getViewSize(String indent) {
        return getView(indent).length();
    }

    public int getCurrentHoleOffset(String indent) {
        return patternView.getCurrentHoleOffset(indent);
    }

    public void updateViewAndCode(){
        for (PatternElement element : elements) {
            if (element.element.equals("\"") || element.element.startsWith("\"") && !element.element.endsWith("\"")){
                element.element += "\"";
            }
        }
        for (PatternElement element : patternView.args){
            if (element.element.equals("\"") || element.element.startsWith("\"") && !element.element.endsWith("\"")){
                element.element += "\"";
            }
        }
    }


    public String getCode(String indent) {
        StringBuilder sb = new StringBuilder();
        for (PatternElement element : elements) {
            sb.append(element.element);
            if (element.element.endsWith("\n")) {
                sb.append(indent);
            }
        }
        return sb.toString();
    }

    public int getCodeSize(String indent) {
        return getCode(indent).length();
    }

    public int getNextHoleId() {
        if (currentHole >= holePos.size()) {
            return -1;
        } else {
            return currentHole;
        }
    }

    public HoleElement getHole(int holeId) {
        return (HoleElement) elements.get(holePos.get(holeId).get(0));
    }

    //---------------------
    private boolean isVariableName(String name){
        return name.matches("[A-Za-z_](\\w)+");
    }

    private boolean isVariableDefinition(){
        List<Integer> currentHolePositions = holePos.get(currentHole);
        if(currentHolePositions.size() != 1){
            return false;
        }
        int currentHolePosition = currentHolePositions.get(0);
        if (elements.size() <= currentHolePosition + 1 || currentHolePosition == 0){
            return false;
        }
        if (elements.get(currentHolePosition + 1).element.equals(";\n")
                && elements.get(currentHolePosition-1).element.trim().endsWith("=")
                && !elements.get(currentHolePosition-1).element.trim().endsWith("==")){
            return true;
        }
        return false;
    }

    private void replaceName(String name){
        List<Integer> currentHolePositions = holePos.get(currentHole);
        int currentHolePosition = currentHolePositions.get(0);
        Matcher matcher = java.util.regex.Pattern.compile("(\\w+_[0-9]+)").matcher(elements.get(currentHolePosition-1).element);
        assert matcher.find();
        String oldName = matcher.group();
        for (PatternElement patternElement : elements){
            patternElement.element = patternElement.element.replaceAll(oldName,name);
        }
        elements.set(currentHolePosition + 1,new PatternElement(""));
        elements.set(currentHolePosition,new PatternElement(""));
        elements.set(currentHolePosition - 1,new PatternElement(""));
    }
    //---------------------


    public void fill(PatternElement expr) {
        patternView.args.add(expr);
        if(isVariableName(expr.element) && isVariableDefinition()){
            replaceName(expr.element);
        } else{
            for (int holeIdPos : holePos.get(currentHole)){
                elements.remove(holeIdPos);
                elements.add(holeIdPos,expr);
            }
        }
        currentHole += 1;
    }
}
