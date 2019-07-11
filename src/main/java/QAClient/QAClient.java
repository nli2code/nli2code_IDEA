package QAClient;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiType;
import javafx.util.Pair;
import pattern.HoleElement;
import pattern.Pattern;
import pattern.PatternElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class QAClient {
    Document document;
    Caret caret;
    Project project;
    DataContext dataContext;
    public QAClient(AnActionEvent event, List<Pair<PsiType,String>> variableInContext){
        updateEvent(event);
        this.variableInContext = variableInContext;
    }
    private void updateEvent(AnActionEvent event){
        this.document = event.getData(CommonDataKeys.EDITOR).getDocument();
        this.caret = event.getData(CommonDataKeys.CARET);
        this.project = event.getProject();
        this.dataContext = event.getDataContext();
    }
    List<Pair<PsiType,String>> variableInContext;

    public String[] setCellColorText = {
            "CellStyle style = ",
            "HOLE",
            ".createCellStyle();\n",
            "style.setFillForegroundColor(IndexedColors.",
            "HOLE",
            ".getIndex());\n",
            "style.setFillBackgroundColor(IndexedColors.",
            "HOLE",
            ".getIndex());\n",
            "style.setFillPattern(FillPatternType.",
            "HOLE",
            ");\n",
            "HOLE",
            ".setCellStyle(style);"
    };


    public String[] setCellColorType = {
            "Workbook",
            "int",
            "java.lang.String",
            "java.lang.String[]",
            "Cell"
    };

    public String[] setCellColorInfo = {
            "Workbook",
            "Foreground Color",
            "Background Color",
            "Fill Pattern",
            "Cell"
    };

    public String[][] recommendedOptions = {
            {
                "new HSSFWorkbook()",
                "new XSSFWorkbook()"
            },
            {
                "RED",
                "GREEN",
                "AQUA"
            },
            {
                "RED",
                "GREEN",
                "AQUA"
            },
            {
                "BIG_SPOT",
                "SOLID_FOREGROUND"
            },
            {
                "new HSSFCell()",
                "new XSSFCell()"
            }
    };

    private List<String> getValidOption(HoleElement hole){
        List<String> validOptions = new ArrayList<>();
        for (String option : hole.options){
            validOptions.add(option);
        }
        for (Pair<PsiType,String> pair : variableInContext){
            //System.out.println(pair.getKey().getCanonicalText() + " : " + pair.getValue());
            if (pair.getKey().getCanonicalText().equals(hole.type)){
                validOptions.add(pair.getValue());
            }
        }
        return validOptions;
    }

    public void raiseQuestion(Pattern pattern, String indent){
        int holeId = pattern.getNextHoleId();
        WriteCommandAction.runWriteCommandAction(project,()->{
            document.replaceString(caret.getOffset(),caret.getOffset(),pattern.getView(indent));
        });
        while (holeId != -1){
            HoleElement hole = pattern.getHole(holeId);
            List<String> validOptions = getValidOption(hole);
            String answer = Messages.showEditableChooseDialog("Please choose an " + pattern.patternView.argsInfo.get(holeId),pattern.patternView.argsInfo.get(holeId),null,validOptions.toArray(new String[]{}),validOptions.get(0),null);
            int oldViewSize = pattern.getViewSize(indent);
            pattern.fill(holeId,new PatternElement(answer));
            holeId = pattern.getNextHoleId();
            WriteCommandAction.runWriteCommandAction(project,()->{
                document.replaceString(caret.getOffset(),caret.getOffset() + oldViewSize,pattern.getView(indent));
            });
        }
        //int shallGenerate = Messages.show("generate code now?","generate code now?",Messages.getQuestionIcon());
        //if (shallGenerate == Messages.OK){
            WriteCommandAction.runWriteCommandAction(project,()->{
                document.replaceString(caret.getOffset(),caret.getOffset() + pattern.getViewSize(indent),pattern.getCode(indent));
            });
            caret.moveToOffset(caret.getOffset() + pattern.getCodeSize(indent));
        //}
    }

    public void start(String funcFeature){
        if (funcFeature.equals("set cell color")){
            int indentNum = caret.getOffset() - document.getLineStartOffset(document.getLineNumber(caret.getOffset()));
            String indent = "";
            for (int i=0;i<indentNum;i++){
                indent += " ";
            }

            List<List<String>> setCellColorOptions = new ArrayList<>();
            for (String[] option : recommendedOptions){
                setCellColorOptions.add(Arrays.asList(option));
            }
            Pattern pattern = new Pattern(Arrays.asList(setCellColorText),"set cell color",Arrays.asList(setCellColorInfo),Arrays.asList(setCellColorType),setCellColorOptions);
            raiseQuestion(pattern,indent);
        }
    }
}
