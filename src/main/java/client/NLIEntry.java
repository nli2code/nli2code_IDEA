package client;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import response.ContextVariable;
import response.NLI;

import java.util.ArrayList;
import java.util.List;

public class NLIEntry extends LookupElement {

    @Getter
    private String functionalFeature;
    @Getter
    private NLI nli;
    @Getter
    private List<ContextVariable> offeredVariable = new ArrayList<>();

    public NLIEntry(NLI nli, List<ContextVariable> contextVariableList){
        this.nli = nli;
        this.functionalFeature = nli.getFunctionalFeature();

        for (ContextVariable cv : contextVariableList){
            if (nli.getType().contains(cv.getQualifiedType())){
                offeredVariable.add(cv);
            }
        }
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
        sb.append("(");
        for (ContextVariable cv : offeredVariable){
            sb.append(cv.getName());
            sb.append(", ");
        }
        if (sb.length()!=1){
            sb.delete(sb.length()-2,sb.length());
        }
        sb.append(")");

        presentation.setTypeText(sb.toString());
    }
}
