package ru.runa.gpd.editor;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import ru.runa.gpd.Localization;
import ru.runa.gpd.PropertyNames;
import ru.runa.gpd.editor.clipboard.VariableTransfer;
import ru.runa.gpd.globalsection.GlobalSectionUtils;
import ru.runa.gpd.lang.model.FormNode;
import ru.runa.gpd.lang.model.ProcessDefinition;
import ru.runa.gpd.lang.model.SubprocessDefinition;
import ru.runa.gpd.lang.model.Variable;
import ru.runa.gpd.lang.model.VariableUserType;
import ru.runa.gpd.lang.par.ParContentProvider;
import ru.runa.gpd.ltk.RenameRefactoringWizard;
import ru.runa.gpd.ltk.RenameVariableRefactoring;
import ru.runa.gpd.search.ElementMatch;
import ru.runa.gpd.search.MultiVariableSearchQuery;
import ru.runa.gpd.search.SearchResult;
import ru.runa.gpd.settings.CommonPreferencePage;
import ru.runa.gpd.ui.custom.Dialogs;
import ru.runa.gpd.ui.custom.DragAndDropAdapter;
import ru.runa.gpd.ui.custom.LoggingSelectionAdapter;
import ru.runa.gpd.ui.custom.TableViewerLocalDragAndDropSupport;
import ru.runa.gpd.ui.dialog.ChooseUserTypeDialog;
import ru.runa.gpd.ui.dialog.ChooseVariableDialog;
import ru.runa.gpd.ui.dialog.ErrorDialog;
import ru.runa.gpd.ui.dialog.UpdateVariableNameDialog;
import ru.runa.gpd.ui.wizard.ChooseGlobalVariableWizard;
import ru.runa.gpd.ui.wizard.CompactWizardDialog;
import ru.runa.gpd.ui.wizard.VariableWizard;
import ru.runa.gpd.util.EmbeddedFileUtils;
import ru.runa.gpd.util.VariableUtils;
import ru.runa.gpd.util.VariablesUsageXlsExporter;
import ru.runa.gpd.util.WorkspaceOperations;
import ru.runa.wfe.var.format.FileFormat;

public class VariableEditorPage extends EditorPartBase<Variable> {

    private TableViewer tableViewer;
    private Button searchButton;
    private Button moveUpButton;
    private Button moveDownButton;
    private Button renameButton;
    private Button changeButton;
    private Button deleteButton;
    private Button copyButton;
    private Button importGlobalButton;
    private Button moveToTypeAttributeButton;
    private Button usageReportButton;
    private Button pasteButton;
    private Button makeLocalButton;

    private static Function<Variable, String> joinVariableNamesFunction = new Function<Variable, String>() {

        @Override
        public String apply(Variable variable) {
            return variable.getName();
        }

    };

    public VariableEditorPage(ProcessEditorBase editor) {
        super(editor);
    }

    @Override
    public void createPartControl(Composite parent) {
        SashForm sashForm = createSashForm(parent, SWT.VERTICAL, "DesignerVariableEditorPage.label.variables");

        Composite allVariablesComposite = createSection(sashForm, "DesignerVariableEditorPage.label.all_variables");

        tableViewer = createMainViewer(allVariablesComposite, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.FULL_SELECTION);
        tableViewer.setLabelProvider(new VariableLabelProvider());
        TableViewerLocalDragAndDropSupport.enable(tableViewer, new DragAndDropAdapter<Variable>() {

            @Override
            public void onDropElement(Variable beforeElement, Variable variable) {
                editor.getDefinition().changeChildIndex(variable, beforeElement);
            }
        });

        createTable(tableViewer, new DataViewerComparator<>(new ValueComparator<Variable>() {
            @Override
            public int compare(Variable o1, Variable o2) {
                int result = 0;
                switch (getColumn()) {
                case 0:
                    result = o1.getName().compareTo(o2.getName());
                    break;
                case 1:
                    result = o1.getFormatLabel().compareTo(o2.getFormatLabel());
                    break;
                }
                return result;
            }
        }), new TableColumnDescription("property.name", 200, SWT.LEFT), new TableColumnDescription("Variable.property.format", 200, SWT.LEFT),
                new TableColumnDescription("Variable.property.defaultValue", 200, SWT.LEFT, false),
                new TableColumnDescription("Variable.property.storeType", 200, SWT.LEFT));

        Composite buttonsBar = createActionBar(allVariablesComposite);
        addButton(buttonsBar, "button.create", new CreateVariableSelectionListener(), false);
        renameButton = addButton(buttonsBar, "button.rename", new RenameVariableSelectionListener(), true);
        changeButton = addButton(buttonsBar, "button.change", new ChangeVariableSelectionListener(), true);
        copyButton = addButton(buttonsBar, "button.copy", new CopyVariableSelectionListener(), true);
        pasteButton = addButton(buttonsBar, "button.paste", new PasteVariableSelectionListener(), true);
        searchButton = addButton(buttonsBar, "button.search", new SearchVariableUsageSelectionListener(), true);
        if (CommonPreferencePage.isGlobalObjectsEnabled()) {
            importGlobalButton = addButton(buttonsBar, "button.importGlobal", new ImportGlobalVariableSelectionListener(), true);
            makeLocalButton = addButton(buttonsBar, "button.makeLocal", new MakeLocalVariableListener(), true);
        }
        usageReportButton = addButton(buttonsBar, "button.report", new ReportUsageSelectionListener(), true);
        usageReportButton.setToolTipText(Localization.getString("DesignerVariableEditorPage.report.variablesUsage.tooltip"));
        moveUpButton = addButton(buttonsBar, "button.up", new MoveVariableSelectionListener(true), true);
        moveDownButton = addButton(buttonsBar, "button.down", new MoveVariableSelectionListener(false), true);
        deleteButton = addButton(buttonsBar, "button.delete", new DeleteVariableSelectionListener(), true);
        moveToTypeAttributeButton = addButton(buttonsBar, "button.move", new MoveToTypeAttributeSelectionListener(), true);
        updateViewer();
    }

    @Override
    public void dispose() {
        for (Variable variable : getDefinition().getVariables(false, false)) {
            variable.removePropertyChangeListener(this);
        }
        super.dispose();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String type = evt.getPropertyName();
        if (PropertyNames.PROPERTY_CHILDREN_CHANGED.equals(type)) {
            updateViewer();
        } else if (evt.getSource() instanceof Variable) {
            if (PropertyNames.PROPERTY_NAME.equals(type) || PropertyNames.PROPERTY_FORMAT.equals(type)
                    || PropertyNames.PROPERTY_DEFAULT_VALUE.equals(type) || PropertyNames.PROPERTY_STORE_TYPE.equals(type)) {
                tableViewer.refresh(evt.getSource());
            }
        }
    }

    @Override
    protected void updateUI() {

        List<?> variables = (List<?>) tableViewer.getInput();
        List<?> selected = ((IStructuredSelection) tableViewer.getSelection()).toList();
        boolean isWithoutGlobalVars = isWithoutGlobalVars(selected);
        boolean isGlobalSection = isGlobalSection();
        boolean isUsingGlobals = isUsingGlobals();
        enableAction(searchButton, selected.size() == 1);
        enableAction(changeButton, isWithoutGlobalVars && selected.size() == 1);
        enableAction(moveUpButton, isWithoutGlobalVars && selected.size() == 1 && variables.indexOf(selected.get(0)) > 0);
        enableAction(moveDownButton, isWithoutGlobalVars && selected.size() == 1 && variables.indexOf(selected.get(0)) < variables.size() - 1);
        enableAction(deleteButton, selected.size() > 0);
        enableAction(renameButton, isWithoutGlobalVars && selected.size() == 1);
        enableAction(copyButton, isWithoutGlobalVars && selected.size() > 0);
        enableAction(moveToTypeAttributeButton, isWithoutGlobalVars && selected.size() == 1);
        enableAction(usageReportButton, variables.size() > 0);
        enableAction(pasteButton, isWithoutGlobalVars);
        if (CommonPreferencePage.isGlobalObjectsEnabled()) {
            enableAction(makeLocalButton, !isWithoutGlobalVars && selected.size() == 1 && !isGlobalSection && isUsingGlobals);
            enableAction(importGlobalButton, (selected.size() >= 0 && !isGlobalSection && isUsingGlobals));
        }

    }

    private boolean isWithoutGlobalVars(List<?> variables) {
        return variables.stream().filter(v -> v instanceof Variable).noneMatch(v -> ((Variable) v).isGlobal());
    }

    private void updateViewer() {
        List<Variable> variables = getDefinition().getVariables(false, false);
        tableViewer.setInput(variables);
        for (Variable variable : variables) {
            variable.addPropertyChangeListener(this);
        }
        updateUI();
    }

    private boolean isGlobalSection() {
        if (GlobalSectionUtils.isGlobalSectionName(getDefinition().getName())) {
            return true;
        }
        return false;
    }

    private boolean isUsingGlobals() {
        return getDefinition().isUsingGlobalVars();
    }

    private class MoveVariableSelectionListener extends LoggingSelectionAdapter {
        private final boolean up;

        public MoveVariableSelectionListener(boolean up) {
            this.up = up;
        }

        @Override
        protected void onSelection(SelectionEvent e) throws Exception {
            IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
            Variable variable = (Variable) selection.getFirstElement();
            List<Variable> variables = getDefinition().getVariables(false, false);
            int index = variables.indexOf(variable);
            getDefinition().swapChildren(variable, up ? variables.get(index - 1) : variables.get(index + 1));
            tableViewer.setSelection(selection);
        }
    }

    private class SearchVariableUsageSelectionListener extends LoggingSelectionAdapter {
        @Override
        protected void onSelection(SelectionEvent e) throws Exception {
            List<Variable> result = Lists.newArrayList();
            Variable variable = getSelection();
            result.add(variable);
            if (variable.isComplex()) {
                result.addAll(VariableUtils.expandComplexVariable(variable, variable));
            }
            String searchText = Joiner.on(", ").join(Lists.transform(result, joinVariableNamesFunction));
            MultiVariableSearchQuery query = new MultiVariableSearchQuery(searchText, editor.getDefinitionFile(), getDefinition(), result);
            NewSearchUI.runQueryInBackground(query);
        }
    }

    private class RenameVariableSelectionListener extends LoggingSelectionAdapter {
        @Override
        protected void onSelection(SelectionEvent e) throws Exception {
            IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
            Variable variable = (Variable) selection.getFirstElement();
            UpdateVariableNameDialog dialog = new UpdateVariableNameDialog(variable);
            int result = dialog.open();
            if (result != IDialogConstants.OK_ID) {
                return;
            }

            IResource projectRoot = editor.getDefinitionFile().getParent();
            IDE.saveAllEditors(new IResource[] { projectRoot }, false);

            String newName = dialog.getName();
            String newScriptingName = dialog.getScriptingName();
            RenameVariableRefactoring refactoring = new RenameVariableRefactoring(editor.getDefinitionFile(), editor.getDefinition(), variable,
                    newName, newScriptingName);
            boolean useLtk = refactoring.isUserInteractionNeeded();
            List<IFile> affectedFiles = Lists.newArrayList();
            Change[] changes = refactoring.createChange(null).getChildren();
            for (Change change : changes) {
                if (change.getAffectedObjects() != null) {
                    for (Object o : change.getAffectedObjects()) {
                        if (o instanceof IFile) {
                            affectedFiles.add((IFile) o);
                        }
                    }
                }
            }
            if (useLtk) {
                RenameRefactoringWizard wizard = new RenameRefactoringWizard(refactoring);
                wizard.setDefaultPageTitle(Localization.getString("Refactoring.variable.name"));
                RefactoringWizardOpenOperation operation = new RefactoringWizardOpenOperation(wizard);
                result = operation.run(Display.getCurrent().getActiveShell(), Localization.getString("VariableEditorPage.variable.rename.title"));
                if (result != IDialogConstants.OK_ID) {
                    return;
                }
            }
            // update variables
            variable.setName(newName);
            variable.setScriptingName(newScriptingName);

            IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
            for (IFile file : affectedFiles) {
                IEditorPart editor = page.findEditor(new FileEditorInput(file));
                if (editor != null) {
                    IEditorPart activeEditor = page.getActiveEditor();
                    page.closeEditor(editor, false);
                    IDE.openEditor(page, file);
                    page.activate(activeEditor);
                }
            }
            if (useLtk) {
                IDE.saveAllEditors(new IResource[] { projectRoot }, false);
                for (SubprocessDefinition subprocessDefinition : editor.getDefinition().getEmbeddedSubprocesses().values()) {
                    WorkspaceOperations.saveProcessDefinition(subprocessDefinition);
                }
            }
        }
    }

    private class DeleteVariableSelectionListener extends LoggingSelectionAdapter {
        @SuppressWarnings("unchecked")
        @Override
        protected void onSelection(SelectionEvent e) throws Exception {
            final IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
            final List<Variable> variables = selection.toList();
            final ProcessDefinition definition = getDefinition();
            for (Variable variable : variables) {
                definition.removeGlobalVariableInAllProcesses(variable, definition.getFile().getParent().getParent());
                if (!isGlobalSection()) {
                    delete(variable);
                }
            }
        }

    }

    private IFile getDefaultValueAsFile(Variable variable) {
        if (EmbeddedFileUtils.isFileVariableClassName(variable.getJavaClassName())) {
            String defaultValue = variable.getDefaultValue();
            if (!Strings.isNullOrEmpty(defaultValue) && EmbeddedFileUtils.isProcessFile(defaultValue)) {
                IFile file = EmbeddedFileUtils.getProcessFile(EmbeddedFileUtils.getProcessFileName(defaultValue));
                if (null != file && file.exists()) {
                    return file;
                }
            }
        }

        return null;
    }

    private void delete(Variable variable) {
        List<FormNode> formNodes = new ArrayList<>();
        List<String> confirmationInfo = new ArrayList<>();
        List<Variable> variablesToBeRemoved = Lists.newArrayList();
        variablesToBeRemoved.add(variable);
        if (variable.isComplex()) {
            variablesToBeRemoved.addAll(VariableUtils.expandComplexVariable(variable, variable));
        }
        String searchText = Joiner.on(", ").join(Lists.transform(variablesToBeRemoved, joinVariableNamesFunction));
        MultiVariableSearchQuery query = new MultiVariableSearchQuery(searchText, editor.getDefinitionFile(), getDefinition(), variablesToBeRemoved);
        NewSearchUI.runQueryInForeground(PlatformUI.getWorkbench().getActiveWorkbenchWindow(), query);
        SearchResult searchResult = query.getSearchResult();
        if (searchResult.getMatchCount() > 0) {
            confirmationInfo.add(Localization.getString("UsagesFoundFor", variable.getName()) + ":\n");
            for (Object object : searchResult.getElements()) {
                ElementMatch elementMatch = (ElementMatch) object;
                confirmationInfo.add(" * " + elementMatch.toString(searchResult));
                if (elementMatch.getGraphElement() instanceof FormNode) {
                    formNodes.add((FormNode) elementMatch.getGraphElement());
                }
            }
        }
        if (!confirmationInfo.isEmpty()
                && !Dialogs.confirm(Localization.getString("deletion.allEditorsWillBeSaved") + "\n\n" + Localization.getString("confirm.delete"),
                        Joiner.on("\n").join(confirmationInfo))) {
            return;
        }
        ParContentProvider.rewriteFormValidationsRemoveVariable(formNodes, variable.getName());
        IFile file = getDefaultValueAsFile(variable);
        if (null != file) {
            EmbeddedFileUtils.deleteProcessFile(file);
        }
        getDefinition().removeChild(variable);
        IResource projectRoot = editor.getDefinitionFile().getParent();
        IDE.saveAllEditors(new IResource[] { projectRoot }, false);
    }

    private class CreateVariableSelectionListener extends LoggingSelectionAdapter {
        @Override
        protected void onSelection(SelectionEvent e) throws Exception {
            VariableWizard wizard = new VariableWizard(getDefinition(), null, true, true);
            CompactWizardDialog dialog = new CompactWizardDialog(wizard);
            if (dialog.open() == Window.OK) {
                Variable variable = wizard.getVariable();
                getDefinition().addChild(variable);
                select(variable);
            }
        }
    }

    private class ImportGlobalVariableSelectionListener extends LoggingSelectionAdapter {
        @Override
        protected void onSelection(SelectionEvent e) throws Exception {
            IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
            ChooseGlobalVariableWizard wizard = new ChooseGlobalVariableWizard(getDefinition(), selection);
            CompactWizardDialog dialog = new CompactWizardDialog(wizard);
            if (dialog.open() == Window.OK) {

            }
            updateViewer();
        }
    }

    private class MakeLocalVariableListener extends LoggingSelectionAdapter {
        @Override
        protected void onSelection(SelectionEvent e) throws Exception {
            IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
            Variable variable = (Variable) selection.getFirstElement();
            variable.setGlobal(false);
            updateViewer();
        }
    }

    private class ChangeVariableSelectionListener extends LoggingSelectionAdapter {
        @Override
        protected void onSelection(SelectionEvent e) throws Exception {
            IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
            Variable variable = (Variable) selection.getFirstElement();
            IFile previousFileVariableDefaultValue = getDefaultValueAsFile(variable);
            VariableWizard wizard = new VariableWizard(getDefinition(), variable, false, true);
            CompactWizardDialog dialog = new CompactWizardDialog(wizard);
            if (dialog.open() == Window.OK) {
                variable.setFormat(wizard.getVariable().getFormat());
                variable.setUserType(wizard.getVariable().getUserType());
                variable.setPublicVisibility(wizard.getVariable().isPublicVisibility());
                variable.setEditableInChat(wizard.getVariable().isEditableInChat());
                variable.setDefaultValue(wizard.getVariable().getDefaultValue());
                variable.setStoreType(wizard.getVariable().getStoreType());
                if (previousFileVariableDefaultValue != null && !EmbeddedFileUtils.isFileVariableClassName(variable.getJavaClassName())) {
                    EmbeddedFileUtils.deleteProcessFile(previousFileVariableDefaultValue);
                }
                tableViewer.setSelection(selection);
            }
        }
    }

    private class CopyVariableSelectionListener extends LoggingSelectionAdapter {
        @Override
        protected void onSelection(SelectionEvent e) throws Exception {
            Clipboard clipboard = new Clipboard(getDisplay());
            @SuppressWarnings("unchecked")
            List<Variable> list = ((IStructuredSelection) tableViewer.getSelection()).toList();
            clipboard.setContents(new Object[] { list }, new Transfer[] { VariableTransfer.getInstance() });
        }
    }

    private class PasteVariableSelectionListener extends LoggingSelectionAdapter {
        @Override
        protected void onSelection(SelectionEvent e) throws Exception {
            Clipboard clipboard = new Clipboard(getDisplay());
            @SuppressWarnings("unchecked")
            List<Variable> data = (List<Variable>) clipboard.getContents(VariableTransfer.getInstance(getDefinition()));
            if (data != null) {
                for (Variable variable : data) {
                    boolean nameAllowed = true;
                    Variable newVariable = VariableUtils.getVariableByName(getDefinition(), variable.getName());
                    if (newVariable == null) {
                        newVariable = new Variable(variable);
                    } else {
                        UpdateVariableNameDialog dialog = new UpdateVariableNameDialog(newVariable);
                        nameAllowed = dialog.open() == Window.OK;
                        if (nameAllowed) {
                            newVariable = new Variable(variable);
                            newVariable.setName(dialog.getName());
                            newVariable.setScriptingName(dialog.getScriptingName());
                        }
                    }

                    if (nameAllowed) {
                        getDefinition().addChild(newVariable);
                        if (newVariable.isComplex()) {
                            copyUserTypeRecursive(newVariable.getUserType());
                            newVariable.setUserType(getDefinition().getVariableUserTypeNotNull(newVariable.getUserType().getName()));
                        }
                    }
                }
            }
        }

        private void copyUserTypeRecursive(VariableUserType sourceUserType) {
            if (getDefinition().getVariableUserType(sourceUserType.getName()) == null) {
                VariableUserType userType = sourceUserType.getCopy();
                getDefinition().addVariableUserType(userType);
            }
            for (Variable attribute : sourceUserType.getAttributes()) {
                if (attribute.isComplex()) {
                    copyUserTypeRecursive(attribute.getUserType());
                }
            }
        }
    }

    private class MoveToTypeAttributeSelectionListener extends LoggingSelectionAdapter {

        @Override
        protected void onSelection(SelectionEvent e) throws Exception {
            IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
            Variable variable = (Variable) selection.getFirstElement();
            ChooseUserTypeDialog dialog = new ChooseUserTypeDialog(getDefinition().getVariableUserTypes());
            VariableUserType newType = dialog.openDialog();
            if (newType == null) {
                return;
            }
            moveToUserType(variable, newType);
        }

        private void moveToUserType(Variable variable, VariableUserType newType) throws Exception {
            if (!newType.canUseAsAttribute(variable)) {
                ErrorDialog.open(Localization.getString("VariableTypeEditorPage.error.attribute.move.loop"));
                return;
            }

            List<Variable> variables = editor.getDefinition().getVariables(true, false, newType.getName());
            if (variables.size() == 0) {
                ErrorDialog.open(Localization.getString("VariableTypeEditorPage.error.variable.move.without.substitution.variable"));
                return;
            }

            IResource projectRoot = editor.getDefinitionFile().getParent();
            boolean useLtk = false;
            if (variables.size() > 0) {
                Variable substitutionVariable;
                if (variables.size() > 1) {
                    ChooseVariableDialog variableDialog = new ChooseVariableDialog(variables);
                    substitutionVariable = variableDialog.openDialog();
                } else {
                    substitutionVariable = variables.get(0);
                }
                if (substitutionVariable == null) {
                    return;
                }
                IDE.saveAllEditors(new IResource[] { projectRoot }, false);

                String newName = substitutionVariable.getName() + VariableUserType.DELIM + variable.getName();
                String newScriptingName = substitutionVariable.getScriptingName() + VariableUserType.DELIM + variable.getScriptingName();
                RenameVariableRefactoring refactoring = new RenameVariableRefactoring(editor.getDefinitionFile(), editor.getDefinition(), variable,
                        newName, newScriptingName);
                useLtk = refactoring.isUserInteractionNeeded();
                if (useLtk) {
                    RenameRefactoringWizard wizard = new RenameRefactoringWizard(refactoring);
                    wizard.setDefaultPageTitle(Localization.getString("Refactoring.variable.name"));
                    RefactoringWizardOpenOperation operation = new RefactoringWizardOpenOperation(wizard);
                    int result = operation.run(Display.getCurrent().getActiveShell(),
                            Localization.getString("VariableEditorPage.variable.move.title"));
                    if (result != IDialogConstants.OK_ID) {
                        return;
                    }
                }
            }
            newType.addAttribute(variable);
            getDefinition().removeChild(variable);

            if (useLtk && editor.getDefinition().getEmbeddedSubprocesses().size() > 0) {
                IDE.saveAllEditors(new IResource[] { projectRoot }, false);
                for (SubprocessDefinition subprocessDefinition : editor.getDefinition().getEmbeddedSubprocesses().values()) {
                    WorkspaceOperations.saveProcessDefinition(subprocessDefinition);
                }
            }
        }
    }

    private class ReportUsageSelectionListener extends LoggingSelectionAdapter {
        @Override
        protected void onSelection(SelectionEvent e) throws Exception {
            FileDialog fd = new FileDialog(getSite().getShell(), SWT.SAVE);
            fd.setText(Localization.getString("DesignerVariableEditorPage.report.variablesUsage.dialog.title"));
            fd.setFileName(editor.getDefinition().getName() + ".vars-usage.xls");
            String filePath = fd.open();
            if (filePath != null) {
                VariablesUsageXlsExporter.go(editor.getDefinition(), filePath);
            }
        }
    }

    private static class VariableLabelProvider extends LabelProvider implements ITableLabelProvider {
        @Override
        public String getColumnText(Object element, int index) {
            Variable variable = (Variable) element;
            switch (index) {
            case 0:
                return variable.getName();
            case 1:
                return variable.getFormatLabel();
            case 2:
                return FileFormat.class.getName().equals(variable.getFormatClassName()) && !Strings.isNullOrEmpty(variable.getDefaultValue())
                        ? EmbeddedFileUtils.getProcessFileName(variable.getDefaultValue())
                        : Strings.nullToEmpty(variable.getDefaultValue());
            case 3:
                return variable.getStoreType().getDescription();
            default:
                return "unknown " + index;
            }
        }

        @Override
        public String getText(Object element) {
            return getColumnText(element, 0);
        }

        @Override
        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }
    }

}
