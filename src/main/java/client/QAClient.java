package client;

import com.intellij.codeInsight.lookup.*;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiType;
import com.intellij.ui.EditorTextField;
import javafx.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.client.RestTemplate;
import pattern.HoleElement;
import pattern.Pattern;
import pattern.PatternElement;
import response.Recommendation;

import java.util.*;

public class QAClient {
    Document document;
    Caret caret;
    Project project;
    DataContext dataContext;
    Editor editor;
    int originalCaretOffset;
    public QAClient(AnActionEvent event, List<Pair<PsiType,String>> variableInContext, Set<String> importedPackages, int importedStmtOffset){
        updateEvent(event);
        this.variableInContext = variableInContext;
        this.importedPackages = importedPackages;
        this.importedStmtOffset = importedStmtOffset;

        this.symbolFQN.put("CellStyle","org.apache.poi.ss.usermodel.CellStyle");
        this.symbolFQN.put("HSSFWorkbook","org.apache.poi.hssf.usermodel.HSSFWorkbook");
        this.symbolFQN.put("IndexedColors","org.apache.poi.ss.usermodel.IndexedColors");
        this.symbolFQN.put("FillPatternType","org.apache.poi.ss.usermodel.FillPatternType");
        this.symbolFQN.put("HSSFCell","org.apache.poi.hssf.usermodel.HSSFCell");

    }
    private void updateEvent(AnActionEvent event){
        this.document = event.getData(CommonDataKeys.EDITOR).getDocument();
        this.caret = event.getData(CommonDataKeys.CARET);
        this.project = event.getProject();
        this.dataContext = event.getDataContext();
        this.editor = event.getData(CommonDataKeys.EDITOR);
        this.originalCaretOffset = caret.getOffset();
    }
    List<Pair<PsiType,String>> variableInContext;
    Set<String> importedPackages;
    int importedStmtOffset;

    public Map<String,String> symbolFQN = new HashMap<>();

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
            "org.apache.poi.ss.usermodel.Workbook",
            "int",
            "java.lang.String",
            "java.lang.String[]",
            "org.apache.poi.ss.usermodel.Cell"
    };

    public String[] setCellColorInfo = {
            "Workbook",
            "Foreground Color",
            "Background Color",
            "Fill pattern",
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
                "BIG_SPOTS",
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
        RestTemplate restTemplate = new RestTemplate();
        Recommendation recommendation = restTemplate.getForObject("http://localhost:8080/recommendation?type=" + hole.type, Recommendation.class);
        if (recommendation != null){
            Set<String> methods = new HashSet<>(recommendation.getRecommendations());
            for (String method : methods){
                validOptions.add(method);
            }
        }

        return validOptions;
    }

    public static void main(String[] args) {
        RestTemplate restTemplate = new RestTemplate();
        Recommendation recommendation = restTemplate.getForObject("http://localhost:8080/recommendation?type=org.apache.poi.ss.usermodel.Cell", Recommendation.class);
        System.out.println(recommendation.getRecommendations().get(0));
    }

    public LookupElement[] getLookupElements(List<String> validOptions){
        MyCompletion[] myCompletions = new MyCompletion[validOptions.size()];
        for (int i=0;i<myCompletions.length;i++){
            myCompletions[i] = new MyCompletion(validOptions.get(i));
        }
        return myCompletions;
    }

    public void startRaiseQuestion(Pattern pattern, String indent){
        for (String packageToImport : pattern.symbolFQN.values()){
            if (!importedPackages.contains(packageToImport)){
                WriteCommandAction.runWriteCommandAction(project,()->{
                    document.insertString(importedStmtOffset,"import " + packageToImport + ";\n");
                });
                originalCaretOffset += packageToImport.length() + 9;
                caret.moveToOffset(originalCaretOffset);
                importedPackages.add(packageToImport);
            }
        }
        WriteCommandAction.runWriteCommandAction(project,()->{
            document.replaceString(caret.getOffset(),caret.getOffset(),pattern.getView(indent));
        });
        raiseQuestion(pattern,indent,"");
    }


    public void raiseQuestion(Pattern pattern, String indent, String prefix){
        int holeId = pattern.getNextHoleId();

        if (holeId != -1){
            HoleElement hole = pattern.getHole(holeId);
            LookupManager lookupManager = LookupManager.getInstance(project);
            int holeOffset = pattern.getCurrentHoleOffset(indent);
            caret.moveToOffset(originalCaretOffset + holeOffset + prefix.length());
            List<String> validOptions = getValidOption(hole);

            LookupEx lookupEx = lookupManager.showLookup(editor,getLookupElements(validOptions),prefix);
            lookupEx.addLookupListener(new LookupListener() {
                @Override
                public void itemSelected(@NotNull LookupEvent event) {
                    MyCompletion myCompletion = (MyCompletion)event.getItem();
                    //int oldViewSize = pattern.getViewSize(indent);
                    if (myCompletion != null){
                        pattern.fill(new PatternElement(myCompletion.getLookupString()));
                    }else{
                        String mannualInput = document.getText(new TextRange(originalCaretOffset + holeOffset,caret.getOffset()));
                        pattern.fill(new PatternElement(mannualInput));
                    }
                    //WriteCommandAction.runWriteCommandAction(project,()->{
                    //    document.replaceString(originalCaretOffset,originalCaretOffset + oldViewSize ,pattern.getView(indent));
                    //});
                    raiseQuestion(pattern,indent,"");
                }

                @Override
                public void lookupCanceled(@NotNull LookupEvent event){
                    String nextPrefix = "";
                    if (caret.getOffset() > originalCaretOffset + holeOffset){
                        nextPrefix = document.getText(new TextRange(originalCaretOffset + holeOffset,caret.getOffset()));
                        WriteCommandAction.runWriteCommandAction(project,()->{
                            //document.deleteString(originalCaretOffset + holeOffset,caret.getOffset());
                        });
                    }else{
                        WriteCommandAction.runWriteCommandAction(project,()->{
                            String pad = "";
                            for (int i=0; i< originalCaretOffset + holeOffset - caret.getOffset(); i++){
                                pad = pad + " ";
                            }
                            document.insertString(caret.getOffset(),pad);
                        });
                        caret.moveToOffset(originalCaretOffset + holeOffset);
                    }
                    raiseQuestion(pattern,indent,nextPrefix);
                }
            });

                //String answer = Messages.showEditableChooseDialog("Please choose an " + pattern.patternView.argsInfo.get(holeId),pattern.patternView.argsInfo.get(holeId),null,validOptions.toArray(new String[]{}),validOptions.get(0),null);
        } else{
            WriteCommandAction.runWriteCommandAction(project,()->{
                document.replaceString(originalCaretOffset,originalCaretOffset + pattern.getViewSize(indent),pattern.getCode(indent));
            });
            caret.moveToOffset(originalCaretOffset + pattern.getCodeSize(indent));
        }
        //int shallGenerate = Messages.show("generate code now?","generate code now?",Messages.getQuestionIcon());
        //if (shallGenerate == Messages.OK){

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
            Pattern pattern = new Pattern(Arrays.asList(setCellColorText),"set cell color",Arrays.asList(setCellColorInfo),Arrays.asList(setCellColorType),setCellColorOptions, symbolFQN);
            startRaiseQuestion(pattern,indent);
        }
    }
}
