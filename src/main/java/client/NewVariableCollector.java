package client;

import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import cucumber.api.java.it.Ma;
import javafx.util.Pair;
import response.ContextVariable;
import sun.nio.cs.ext.MacThai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class NewVariableCollector extends JavaRecursiveElementWalkingVisitor {
    private Caret caret;
    private int caretDepth = 0;
    private List<TextRange> blockRanges = new ArrayList<>();

    private void enteringBlock(TextRange textRange){
        blockRanges.add(textRange);
    }

    private void leavingBlock(){
    }

    private int getDepth(int offset){
        for (int i = blockRanges.size() - 1;i>=0;i--){
            if (blockRanges.get(i).contains(offset)){
                return i;
            }
        }
        return -1;
    }

    public void updateContextVariableProperties(){
        caretDepth = blockRanges.size() - 1;
        for (ContextVariable cv : variableInContext){
            cv.setDepthToCaret(caretDepth - cv.getDepthToCaret());
        }

        Collections.sort(variableInContext);
        int currentRank = 0;
        int currentDepth = 0;
        for (ContextVariable cv : variableInContext){
            if (cv.getDepthToCaret() > currentDepth){
                currentDepth = cv.getDepthToCaret();
                currentRank = 0;
            }
            cv.setNthToTheCaret(currentRank);
            currentRank += 1;
        }

        //printAllVariables();
    }

    private void printAllVariables(){
        System.out.println(caretDepth);
        for (ContextVariable cv : variableInContext){
            System.out.println(cv.getQualifiedType() + "  :  " + cv.getName());
            System.out.println(cv.getDepthToCaret() + "  :  " + cv.getNthToTheCaret() + "(" + cv.getDistanceToCaret() + ")");
        }
    }



    public List<ContextVariable> variableInContext = new ArrayList<>();


    public NewVariableCollector(Caret caret) {
        this.caret = caret;
    }

    @Override
    public void visitClass(PsiClass aClass) {
        if (aClass.getTextRange().contains(caret.getOffset())){
            enteringBlock(aClass.getTextRange());
            super.visitClass(aClass);
        }
    }

    @Override
    public void visitMethod(PsiMethod method) {
        if (method.getTextRange().contains(caret.getOffset())){
            enteringBlock(method.getTextRange());
            for (PsiParameter parameter : method.getParameterList().getParameters()) {
                variableInContext.add(new ContextVariable(parameter.getType().getCanonicalText(), parameter.getName(), getDepth(parameter.getTextOffset()), Math.abs(caret.getOffset() - method.getTextOffset())));
            }
            super.visitMethod(method);
        }
    }

    @Override
    public void visitWhileStatement(PsiWhileStatement statement) {
        if (statement.getTextRange().contains(caret.getOffset())){
            enteringBlock(statement.getTextRange());
            super.visitWhileStatement(statement);
        }
    }

    @Override
    public void visitForeachStatement(PsiForeachStatement statement) {
        if (statement.getTextRange().contains(caret.getOffset())){
            enteringBlock(statement.getTextRange());
            super.visitForeachStatement(statement);
        }
    }

    @Override
    public void visitForStatement(PsiForStatement statement) {
        if (statement.getTextRange().contains(caret.getOffset())){
            enteringBlock(statement.getTextRange());
            super.visitForStatement(statement);
        }
    }

    @Override
    public void visitIfStatement(PsiIfStatement statement) {
        if (statement.getTextRange().contains(caret.getOffset())){
            enteringBlock(statement.getTextRange());
            super.visitIfStatement(statement);
        }
    }

    @Override
    public void visitTryStatement(PsiTryStatement statement) {
        if (statement.getTextRange().contains(caret.getOffset())){
            enteringBlock(statement.getTextRange());
            super.visitTryStatement(statement);
        }
    }

    @Override
    public void visitDoWhileStatement(PsiDoWhileStatement statement) {
        if (statement.getTextRange().contains(caret.getOffset())){
            enteringBlock(statement.getTextRange());
            super.visitDoWhileStatement(statement);
        }
    }

    @Override
    public void visitLocalVariable(PsiLocalVariable variable) {
        if (variable.getTextOffset() < caret.getOffset()){
            variableInContext.add(new ContextVariable(variable.getType().getCanonicalText(), variable.getName(), getDepth(variable.getTextOffset()), Math.abs(caret.getOffset() - variable.getTextOffset())));
        }
        super.visitLocalVariable(variable);
    }

    @Override
    public void visitField(PsiField field) {
        variableInContext.add(new ContextVariable(field.getType().getCanonicalText(), field.getName(), getDepth(field.getTextOffset()), Math.abs(caret.getOffset() - field.getTextOffset())));
        super.visitField(field);
    }
}
