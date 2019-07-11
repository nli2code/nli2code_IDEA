package QAClient;

import com.intellij.openapi.editor.Caret;
import com.intellij.psi.*;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;


public class VariableCollector extends JavaRecursiveElementWalkingVisitor {
    private Caret caret;
    public VariableCollector(Caret caret){
        this.caret = caret;
    }

    private boolean methodDataValid = false;
    private int methodStart;
    private int methodEnd;
    private boolean classDataValid = false;
    private int classStart;
    private int classEnd;
    public List<Pair<PsiType,String>> variableInContext = new ArrayList<>();

    @Override
    public void visitClass(PsiClass aClass) {
        if (aClass.getTextRange().contains(caret.getOffset())){
            classDataValid = true;
            classStart = aClass.getTextRange().getStartOffset();
            classEnd = aClass.getTextRange().getEndOffset();
        }
        super.visitClass(aClass);
    }

    @Override
    public void visitMethod(PsiMethod method) {
        if (method.getTextRange().contains(caret.getOffset())) {
            methodDataValid = true;
            methodStart = method.getTextRange().getStartOffset();
            methodEnd = method.getTextRange().getEndOffset();

            for (PsiParameter parameter : method.getParameterList().getParameters()) {
                variableInContext.add(new Pair<>(parameter.getType(), parameter.getName()));
            }
        }
        super.visitMethod(method);
    }


    @Override
    public void visitLocalVariable(PsiLocalVariable variable) {
        if (methodDataValid && variable.getTextOffset() > methodStart && variable.getTextOffset() < methodEnd){
            variableInContext.add(new Pair<>(variable.getType(),variable.getName()));
        }
        super.visitLocalVariable(variable);
    }

    @Override
    public void visitField(PsiField field) {
        if (classDataValid && field.getTextOffset() > classStart && field.getTextOffset() < classEnd){
            variableInContext.add(new Pair<>(field.getType(),field.getName()));
        }
        super.visitField(field);
    }

}
