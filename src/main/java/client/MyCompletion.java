package client;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupItem;
import org.jetbrains.annotations.NotNull;

public class MyCompletion extends LookupElement {

    private String myCompletion;
    public MyCompletion(String myCompletion){
        this.myCompletion = myCompletion;
    }

    @NotNull
    @Override
    public String getLookupString() {
        return myCompletion;
    }
}
