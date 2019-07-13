import client.QAClient;
import client.VariableCollector;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class NLIAction extends AnAction {
    public NLIAction() {
        super("Hello");
    }

    public void actionPerformed(AnActionEvent event) {
        DataContext dataContext = event.getDataContext();
        Project project = event.getProject();
        Editor editor = event.getData(CommonDataKeys.EDITOR);
        Document document = editor.getDocument();
        Caret caret = event.getData(CommonDataKeys.CARET);
        PsiFile psiFile = event.getData(CommonDataKeys.PSI_FILE);

        VariableCollector variableCollector = new VariableCollector(caret);
        psiFile.accept(variableCollector);

        List<AnAction> actionList = new ArrayList<>();
        actionList.add(new AnAction("set cell color") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                QAClient qaClient = new QAClient(e,variableCollector.variableInContext);
                qaClient.start("set cell color");
            }
        });
        actionList.add(new AnAction("set hyperlink") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                QAClient qaClient = new QAClient(e,variableCollector.variableInContext);
                qaClient.start("set hyperlink");
            }
        });
        ActionGroup actionGroup = new DefaultActionGroup(actionList);
        ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup("Please select a NLI",actionGroup,event.getDataContext(), JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,true,()->{},10);
        popup.showInBestPositionFor(dataContext);

    }
}