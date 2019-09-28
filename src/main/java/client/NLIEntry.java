package client;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import response.ContextVariable;
import response.NLI;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class NLIEntry extends LookupElement {

    @Getter
    private String functionalFeature;
    @Getter
    private NLI nli;
    @Getter
    private List<ContextVariable> offeredVariable = new ArrayList<>();
    @Getter
    private int score;

    public NLIEntry(NLI nli, Set<ContextVariable> contextVariableList){
        this.nli = nli;
        this.functionalFeature = nli.getFunctionalFeature();

        for (ContextVariable cv : contextVariableList){
            if (nli.getType().contains(cv.getQualifiedType())){
                offeredVariable.add(cv);
            }
        }

        for (String targetType : nli.getType()){
            int minScore = 100;
            for (ContextVariable cv : contextVariableList){
                if (cv.getQualifiedType().equals(targetType)){
                    int fitScore = cv.getDepthToCaret() * 10 + cv.getNthToTheCaret();
                    if (fitScore < minScore){
                        minScore = fitScore;
                    }
                }
            }
            score += minScore;
        }
        score = score / nli.getType().size();
    }

    @NotNull
    @Override
    public String getLookupString() {
        return nli.getFunctionalFeature();
    }

    @Override
    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(getLookupString());

        StringBuilder sb = new StringBuilder();
        /*sb.append("(");
        for (ContextVariable cv : offeredVariable){
            sb.append(cv.getName());
            sb.append(", ");
        }
        if (sb.length()!=1){
            sb.delete(sb.length()-2,sb.length());
        }
        sb.append(")");*/
        sb.append(score);

        presentation.setTypeText(sb.toString());
    }
}
