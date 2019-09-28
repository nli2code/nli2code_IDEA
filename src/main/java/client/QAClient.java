package client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.codeInsight.lookup.*;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.panel.PanelBuilder;
import com.intellij.openapi.ui.panel.PanelGridBuilder;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiType;
import javafx.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.client.RestTemplate;
import pattern.HoleElement;
import pattern.Pattern;
import pattern.PatternElement;
import response.ContextVariable;
import response.NLI;
import response.Recommendation;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.*;

public class QAClient {
    Document document;
    Caret caret;
    Project project;
    DataContext dataContext;
    Editor editor;
    int originalCaretOffset;
    RangeHighlighter functionalFeatureHighlighter;
    String NLI_PROVIDER_URL;

    public QAClient(AnActionEvent event, List<ContextVariable> variableInContext, Set<String> importedPackages, int importedStmtOffset) {
        updateEvent(event);
        this.importedPackages = importedPackages;
        this.importedStmtOffset = importedStmtOffset;
        this.variableInContext = new HashSet<>(variableInContext);

        Properties properties = new Properties();
        try {
            properties.load(this.getClass().getResourceAsStream("/application.properties"));
            this.NLI_PROVIDER_URL = properties.getProperty("NLI_PROVIDER_URL");
        } catch (IOException e) {
            e.printStackTrace();
            this.NLI_PROVIDER_URL = "http://localhost:8080";
        }

        if(patternMap == null){
            patternMap = new HashMap<>();

            RestTemplate restTemplate = new RestTemplate();
            NLI[] NLIs = restTemplate.getForObject(NLI_PROVIDER_URL + "/NLI", NLI[].class);

            for (NLI nli : NLIs){
                System.out.println(nli.getFunctionalFeature());
                patternMap.put(nli.getFunctionalFeature(),nli);
            }
        }

        for (NLI nli : patternMap.values()){
            NLIMenu.add(new NLIEntry(nli,this.variableInContext));
        }
    }

    private void updateEvent(AnActionEvent event) {
        this.document = event.getData(CommonDataKeys.EDITOR).getDocument();
        this.caret = event.getData(CommonDataKeys.CARET);
        this.project = event.getProject();
        this.dataContext = event.getDataContext();
        this.editor = event.getData(CommonDataKeys.EDITOR);
        this.originalCaretOffset = caret.getOffset();
    }

    Set<ContextVariable> variableInContext = new HashSet<>();
    Set<String> importedPackages;
    int importedStmtOffset;
    static HashMap<String,NLI> patternMap;
    List<NLIEntry> NLIMenu = new ArrayList<>();



    public LookupElement[] getLookupElements(HoleElement hole) {

        ObjectMapper objectMapper = new ObjectMapper();
        String variableInContextString = "";
        try {
            variableInContextString = objectMapper.writeValueAsString(variableInContext);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        RestTemplate restTemplate = new RestTemplate();
        Recommendation recommendation = restTemplate.postForObject(NLI_PROVIDER_URL + "/recommendation?type=" + hole.type + "&info=" + hole.info, variableInContextString, Recommendation.class);

        List<MyCompletion> myCompletions = new ArrayList<>();
        if (recommendation != null) {
            for (int i = 0; i < recommendation.getEntries().size(); i++) {
                MyCompletion myCompletion = new MyCompletion();
                myCompletion.setMyCompletion(recommendation.getEntries().get(i).getText());
                myCompletion.setTypes(recommendation.getEntries().get(i).getTypeList());
                myCompletion.setScore(recommendation.getEntries().get(i).getScore());
                myCompletions.add(myCompletion);
            }
        }

        MyCompletion[] completionsToReturn = new MyCompletion[myCompletions.size()];
        for (int i = 0; i < myCompletions.size(); i++) {
            completionsToReturn[i] = myCompletions.get(i);
        }

        return completionsToReturn;
    }

    public static void main(String[] args) {
        RestTemplate restTemplate = new RestTemplate();
        Recommendation recommendation = restTemplate.getForObject("http://localhost:8080/recommendation?type=org.apache.poi.ss.usermodel.Cell", Recommendation.class);
        System.out.println(recommendation.getEntries().get(0).getText());
    }

    public void updateImports(Set<String> imports) {
        for (String packageToImport : imports) {
            if (!importedPackages.contains(packageToImport)) {
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    document.insertString(importedStmtOffset, "import " + packageToImport + ";\n");
                });
                originalCaretOffset += packageToImport.length() + 9;
                caret.moveToOffset(originalCaretOffset);
                importedPackages.add(packageToImport);
            }
        }
    }


    public void startRaiseQuestion(Pattern pattern, String indent) {
        updateImports(new HashSet<>(pattern.symbolFQN.values()));

        WriteCommandAction.runWriteCommandAction(project, () -> {
            document.replaceString(caret.getOffset(), caret.getOffset(), pattern.getView(indent));
        });
        TextAttributes highlightAttributes = new TextAttributes();
        highlightAttributes.setForegroundColor(Color.MAGENTA);
        functionalFeatureHighlighter = editor.getMarkupModel().addRangeHighlighter(originalCaretOffset, originalCaretOffset + pattern.getFunctionalFeature().length(), HighlighterLayer.ERROR, highlightAttributes, HighlighterTargetArea.EXACT_RANGE);
        raiseQuestion(pattern, indent, "");
    }


    public void raiseQuestion(Pattern pattern, String indent, String prefix) {
        int holeId = pattern.getNextHoleId();


        PanelBuilder panelBuilder = new PanelGridBuilder();
        JPanel jPanel = panelBuilder.createPanel();
        jPanel.add(new JEditorPane());


        if (holeId != -1) {
            HoleElement hole = pattern.getHole(holeId);
            int holeOffset = pattern.getCurrentHoleOffset(indent);
            caret.moveToOffset(originalCaretOffset + holeOffset + prefix.length());

            TextAttributes highlightAttributes = new TextAttributes();
            highlightAttributes.setForegroundColor(Color.ORANGE);
            //highlightAttributes.setBackgroundColor(Color.LIGHT_GRAY);
            RangeHighlighter rangeHighlighter = editor.getMarkupModel().addRangeHighlighter(caret.getVisualLineStart() + indent.length() + 4, originalCaretOffset + holeOffset, HighlighterLayer.ERROR, highlightAttributes, HighlighterTargetArea.EXACT_RANGE);

            LookupManager lookupManager = LookupManager.getInstance(project);
            LookupEx lookupEx = lookupManager.showLookup(editor, getLookupElements(hole), prefix);
            lookupEx.addLookupListener(new LookupListener() {
                @Override
                public void itemSelected(@NotNull LookupEvent event) {
                    MyCompletion myCompletion = (MyCompletion) event.getItem();
                    //int oldViewSize = pattern.getViewSize(indent);
                    if (myCompletion != null) {
                        if (myCompletion.getTypes() != null) {
                            updateImports(new HashSet<>(myCompletion.getTypes()));
                        }
                        pattern.fill(new PatternElement(myCompletion.getLookupString()));
                    } else {
                        String mannualInput = document.getText(new TextRange(originalCaretOffset + holeOffset, caret.getOffset()));
                        if (mannualInput.endsWith("/")) {
                            mannualInput = mannualInput.substring(0, mannualInput.length() - 1);
                        }
                        pattern.fill(new PatternElement(mannualInput));
                    }
                    //WriteCommandAction.runWriteCommandAction(project,()->{
                    //    document.replaceString(originalCaretOffset,originalCaretOffset + oldViewSize ,pattern.getView(indent));
                    //});

                    editor.getMarkupModel().removeHighlighter(rangeHighlighter);
                    raiseQuestion(pattern, indent, "");
                }

                @Override
                public void lookupCanceled(@NotNull LookupEvent event) {
                    String nextPrefix = "";
                    if (caret.getOffset() > originalCaretOffset + holeOffset) {
                        nextPrefix = document.getText(new TextRange(originalCaretOffset + holeOffset, caret.getOffset()));
                        WriteCommandAction.runWriteCommandAction(project, () -> {
                            //document.deleteString(originalCaretOffset + holeOffset,caret.getOffset());
                        });
                    } else {
                        WriteCommandAction.runWriteCommandAction(project, () -> {
                            String pad = "";
                            for (int i = 0; i < originalCaretOffset + holeOffset - caret.getOffset(); i++) {
                                pad = pad + " ";
                            }
                            document.insertString(caret.getOffset(), pad);
                        });
                        caret.moveToOffset(originalCaretOffset + holeOffset);
                    }
                    editor.getMarkupModel().removeHighlighter(rangeHighlighter);
                    raiseQuestion(pattern, indent, nextPrefix);
                }
            });
            //String answer = Messages.showEditableChooseDialog("Please choose an " + pattern.patternView.argsInfo.get(holeId),pattern.patternView.argsInfo.get(holeId),null,validOptions.toArray(new String[]{}),validOptions.get(0),null);
        } else {
            editor.getMarkupModel().removeHighlighter(functionalFeatureHighlighter);
            pattern.updateViewAndCode();
            WriteCommandAction.runWriteCommandAction(project, () -> {
                document.replaceString(originalCaretOffset, originalCaretOffset + pattern.getViewSize(indent), pattern.getCode(indent));
            });
            caret.moveToOffset(originalCaretOffset + pattern.getCodeSize(indent));
        }
        //int shallGenerate = Messages.show("generate code now?","generate code now?",Messages.getQuestionIcon());
        //if (shallGenerate == Messages.OK){

        //}
    }

    private void sortTheNLIs(NLIEntry[] NLIMenuArray){
        Arrays.sort(NLIMenuArray, Comparator.comparingInt(NLIEntry::getScore));
        //Arrays.sort(NLIMenuArray, Comparator.comparingInt(a -> - a.getOfferedVariable().size()));
    }


    public void show(){

        NLIEntry[] NLIMenuArray = new NLIEntry[NLIMenu.size()];
        NLIMenu.toArray(NLIMenuArray);
        sortTheNLIs(NLIMenuArray);

        LookupManager lookupManager = LookupManager.getInstance(project);
        LookupEx lookupEx = lookupManager.showLookup(editor,NLIMenuArray , "");
        lookupEx.addLookupListener(new LookupListener() {
            @Override
            public void itemSelected(@NotNull LookupEvent event) {
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    document.deleteString(originalCaretOffset,caret.getOffset());
                });
                caret.moveToOffset(originalCaretOffset);

                NLIEntry selectedNLI = (NLIEntry) event.getItem();
                if (selectedNLI!=null){
                    start(selectedNLI.getFunctionalFeature());
                }
            }
        });
    }

    public void start(String funcFeature) {
        int indentNum = caret.getOffset() - document.getLineStartOffset(document.getLineNumber(caret.getOffset()));
        String indent = "";
        for (int i = 0; i < indentNum; i++) {
            indent += " ";
        }

        NLI nli = patternMap.get(funcFeature);
        if (nli!=null){
            System.out.println(nli.getFunctionalFeature());
            startRaiseQuestion(new Pattern(nli),indent);
        }

        /*if (funcFeature.equals("set cell color")) {
            Pattern pattern = new Pattern(Arrays.asList(setCellColorText), "set cell color", Arrays.asList(setCellColorInfo), Arrays.asList(setCellColorType), setCellColorSymbols);
            startRaiseQuestion(pattern, indent);
        }else if (funcFeature.equals("set hyperlink")){
            Pattern pattern = new Pattern(Arrays.asList(setHyperlinkText), "set hyperlink", Arrays.asList(setHyperlinkInfo), Arrays.asList(setHyperlinkType), setHyperlinkSymbols);
            startRaiseQuestion(pattern, indent);
        }*/
    }
}
