package client;

import com.intellij.psi.JavaRecursiveElementWalkingVisitor;
import com.intellij.psi.PsiImportStatement;
import com.intellij.psi.PsiPackageStatement;

import java.util.HashSet;
import java.util.Set;


public class ImportCollector extends JavaRecursiveElementWalkingVisitor {

    public Set<String> importedPackages = new HashSet<>();
    public int importedStmtOffset = 0;

    @Override
    public void visitPackageStatement(PsiPackageStatement statement) {
        super.visitPackageStatement(statement);
        importedStmtOffset = statement.getTextOffset() + statement.getTextLength() + 1;
    }

    @Override
    public void visitImportStatement(PsiImportStatement statement) {
        super.visitImportStatement(statement);
        importedPackages.add(statement.getQualifiedName());
        importedStmtOffset = statement.getTextOffset() + statement.getTextLength() + 1;
    }
}
