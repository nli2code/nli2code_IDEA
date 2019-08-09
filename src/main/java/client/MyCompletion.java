package client;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MyCompletion extends LookupElement {

    @Getter
    @Setter
    private String myCompletion;

    @Getter
    @Setter
    private List<String> types = null;

    public MyCompletion() {
    }

    @NotNull
    @Override
    public String getLookupString() {
        return myCompletion;
    }

    @Override
    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(getLookupString());
        presentation.setTypeText("100%");
    }

}
