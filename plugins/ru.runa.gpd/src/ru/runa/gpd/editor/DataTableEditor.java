package ru.runa.gpd.editor;

import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;
import ru.runa.gpd.DataTableCache;
import ru.runa.gpd.Localization;
import ru.runa.gpd.PluginLogger;
import ru.runa.gpd.editor.EditorPartBase.DataViewerComparator;
import ru.runa.gpd.editor.EditorPartBase.TableColumnDescription;
import ru.runa.gpd.editor.EditorPartBase.ValueComparator;
import ru.runa.gpd.editor.clipboard.VariableTransfer;
import ru.runa.gpd.lang.model.ProcessDefinition;
import ru.runa.gpd.lang.model.Variable;
import ru.runa.gpd.lang.model.VariableUserType;
import ru.runa.gpd.ui.custom.LoggingSelectionAdapter;
import ru.runa.gpd.ui.dialog.UpdateVariableNameDialog;
import ru.runa.gpd.ui.wizard.CompactWizardDialog;
import ru.runa.gpd.ui.wizard.VariableWizard;
import ru.runa.gpd.util.EditorUtils;
import ru.runa.gpd.util.IOUtils;
import ru.runa.gpd.util.VariableUtils;
import ru.runa.gpd.util.WorkspaceOperations;

public class DataTableEditor extends EditorPart implements IResourceChangeListener, ISelectionChangedListener {

    public static final String ID = "ru.runa.gpd.editor.DataTableEditor";

    private Composite editorComposite;
    private TableViewer tableViewer;
    private IFile dataTableFile;
    private VariableUserType dataTable;
    private FormToolkit toolkit;
    private Button editTypeButton;
    private Button renameTypeButton;
    private Button deleteTypeButton;
    private Menu menu;

    @Override
    public void createPartControl(Composite parent) {
        Section section = getToolkit().createSection(parent, ExpandableComposite.TITLE_BAR);
        section.marginHeight = 5;
        section.marginWidth = 5;
        section.setText(Localization.getString("VariableUserType.attributes"));
        editorComposite = getToolkit().createComposite(section);
        section.setClient(editorComposite);
        editorComposite.setLayout(new GridLayout(2, false));
        editorComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        rebuildView(editorComposite);
    }

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        setSite(site);
        setInput(input);
        FileEditorInput fileInput = (FileEditorInput) input;
        dataTableFile = fileInput.getFile();
        String dataTableFileNameWithoutExtension = IOUtils.getWithoutExtension(dataTableFile.getName());
        try {
            dataTable = DataTableCache.getDataTable(dataTableFileNameWithoutExtension);
        } catch (Exception e) {
            throw new PartInitException("", e);
        }
        setPartName(IOUtils.getWithoutExtension(dataTableFile.getName()));
        ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
    }

    @Override
    public void setFocus() {
    }

    @Override
    public void resourceChanged(IResourceChangeEvent event) {
        EditorUtils.closeEditorIfRequired(event, dataTableFile, this);
    }

    @Override
    public void selectionChanged(SelectionChangedEvent event) {
        enableAction(editTypeButton, !event.getSelection().isEmpty());
        enableAction(deleteTypeButton, !event.getSelection().isEmpty());
        enableAction(renameTypeButton, !event.getSelection().isEmpty());

    }

    @Override
    public void doSave(IProgressMonitor monitor) {
        try {
            WorkspaceOperations.saveDataTable(dataTableFile, dataTable);
        } catch (Exception e) {
            PluginLogger.logError(e);
        }
    }
    @Override
    public void doSaveAs() {
    }
    @Override
    public boolean isDirty() {
        return false;
    }
    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    private void rebuildView(Composite composite) {
        for (Control control : composite.getChildren()) {
            control.dispose();
        }
        createAttributeTableViewer(composite);
        createButtonsBar(composite);
        composite.layout(true, true);
    }

    private void createAttributeTableViewer(Composite composite) {
        tableViewer = new TableViewer(composite, SWT.BORDER | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.MULTI);
        tableViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
        tableViewer.setContentProvider(new ArrayContentProvider());
        tableViewer.setLabelProvider(new TableLabelProvider());
        tableViewer.addSelectionChangedListener(this);

        getSite().setSelectionProvider(tableViewer);
        createTable(new DataViewerComparator<>(new ValueComparator<Variable>() {
            @Override
            public int compare(Variable o1, Variable o2) {
                int result = 0;
                switch (getColumn()) {
                case 0:
                    result = o1.getName().compareTo(o2.getName());
                    break;
                case 1:
                    result = o1.getFormatLabel().compareTo(o2.getFormatLabel());
                }
                return result;
            }
        }));
        List<String[]> userTypeAttributes = new ArrayList<>();
        if (dataTable != null) {
            for (Variable userTypeAttribute : dataTable.getAttributes()) {
                String[] attr = new String[] { userTypeAttribute.getName(), userTypeAttribute.getFormatLabel(), userTypeAttribute.getDefaultValue() };
                userTypeAttributes.add(attr);
            }
            tableViewer.setInput(dataTable.getAttributes());
        } else {
            tableViewer.setInput(new Object[0]);
        }
        createContextMenu(tableViewer.getControl());
    }

    private void createButtonsBar(Composite composite) {
        Composite buttonsBar = getToolkit().createComposite(composite);
        buttonsBar.setLayout(new GridLayout(1, false));
        GridData gridData = new GridData();
        gridData.horizontalAlignment = SWT.LEFT;
        gridData.verticalAlignment = SWT.TOP;
        gridData.grabExcessVerticalSpace = true;
        buttonsBar.setLayoutData(gridData);
        addButton(buttonsBar, "button.create", new CreateAttributeSelectionListener(), true, true);
        editTypeButton = addButton(buttonsBar, "button.change", new ChangeAttributeSelectionListener(), true, false);
        renameTypeButton = addButton(buttonsBar, "button.rename", new RenameAttributeSelectionListener(), true, false);
        deleteTypeButton = addButton(buttonsBar, "button.delete", new DeleteAttributeSelectionListener(), true, false);
        addButton(buttonsBar, "button.copy", new CopyAttributeSelectionListener(), true, true);
        addButton(buttonsBar, "button.paste", new PasteAttributeSelectionListener(), true, true);
    }

    private static class TableLabelProvider extends LabelProvider implements ITableLabelProvider {
        @Override
        public String getColumnText(Object element, int index) {
            Variable variable = (Variable) element;
            switch (index) {
            case 0:
                return variable.getName();
            case 1:
                return variable.getFormatLabel();
            case 2:
                return Strings.nullToEmpty(variable.getDefaultValue());
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

    private Button addButton(Composite parent, String buttonKey, SelectionAdapter selectionListener, boolean addToMenu, boolean enabled) {
        String title = Localization.getString(buttonKey);
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        Button button = getToolkit().createButton(parent, title, SWT.PUSH);
        button.setLayoutData(gridData);
        button.addSelectionListener(selectionListener);
        button.setEnabled(enabled);
        if (addToMenu) {
            MenuItem item = new MenuItem(menu, SWT.NONE);
            item.setText(title);
            item.addSelectionListener(selectionListener);
            item.setEnabled(enabled);
            button.setData("menuItem", item);
        }
        return button;
    }

    public FormToolkit getToolkit() {
        if (toolkit == null) {
            toolkit = new FormToolkit(Display.getDefault());
        }
        return toolkit;
    }

    @SuppressWarnings("unchecked")
    private <T> T getSelection() {
        return tableViewer == null ? null : (T) ((IStructuredSelection) tableViewer.getSelection()).getFirstElement();
    }

    private void createContextMenu(Control control) {
        menu = new Menu(control.getShell(), SWT.POP_UP);
        control.setMenu(menu);
    }

    private Display getDisplay() {
        Display result = Display.getCurrent();
        if (result == null) {
            result = Display.getDefault();
        }
        return result;
    }

    private void enableAction(Button button, boolean enabled) {
        button.setEnabled(enabled);
        MenuItem menuItem = ((MenuItem) button.getData("menuItem"));
        if (menuItem != null) {
            menuItem.setEnabled(enabled);
        }
    }

    private <S> void createTable(DataViewerComparator<S> comparator) {
        tableViewer.setComparator(comparator);
        Table table = tableViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        TableColumnDescription[] columnDescriptions = { new TableColumnDescription("property.name", 200, SWT.LEFT),
                new TableColumnDescription("Variable.property.format", 200, SWT.LEFT),
                new TableColumnDescription("Variable.property.defaultValue", 200, SWT.LEFT, false) };
        int index = 0;
        for (TableColumnDescription col : columnDescriptions) {
            TableColumn tableColumn = new TableColumn(table, col.getStyle());
            tableColumn.setText(Localization.getString(col.getTitleKey()));
            tableColumn.setWidth(col.getWidth());
            if (col.isSort()) {
                tableColumn.addSelectionListener(createSelectionListener(tableViewer, comparator, tableColumn, index));
            }
            index++;
        }
    }

    private <S> SelectionListener createSelectionListener(final TableViewer viewer, final DataViewerComparator<S> comparator,
            final TableColumn column, final int index) {
        SelectionAdapter selectionAdapter = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                comparator.setColumn(index);
                viewer.getTable().setSortDirection(comparator.getDirection());
                viewer.getTable().setSortColumn(column);
                viewer.refresh();
            }
        };
        return selectionAdapter;
    }

    private class DeleteAttributeSelectionListener extends LoggingSelectionAdapter {
        @Override
        protected void onSelection(SelectionEvent e) throws Exception {
            @SuppressWarnings("unchecked")
            List<Variable> attributes = ((IStructuredSelection) tableViewer.getSelection()).toList();
            for (Variable attribute : attributes) {
                dataTable.removeAttribute(attribute);
            }
            WorkspaceOperations.saveDataTable(dataTableFile, dataTable);
            rebuildView(editorComposite);
        }
    }

    private class RenameAttributeSelectionListener extends LoggingSelectionAdapter {
        @Override
        protected void onSelection(SelectionEvent e) throws Exception {
            Variable attribute = getSelection();
            UpdateVariableNameDialog dialog = new UpdateVariableNameDialog(dataTable, attribute);
            int result = dialog.open();
            if (result != IDialogConstants.OK_ID) {
                return;
            }
            String newAttributeName = dialog.getName();
            String newAttributeScriptingName = dialog.getScriptingName();
            attribute.setName(newAttributeName);
            attribute.setScriptingName(newAttributeScriptingName);
            WorkspaceOperations.saveDataTable(dataTableFile, dataTable);
            rebuildView(editorComposite);
        }
    }

    private class ChangeAttributeSelectionListener extends LoggingSelectionAdapter {
        @Override
        protected void onSelection(SelectionEvent e) throws Exception {
            Variable variable = getSelection();
            VariableWizard wizard = new VariableWizard(new ProcessDefinition(null), dataTable, variable, false, true, false);
            CompactWizardDialog dialog = new CompactWizardDialog(wizard);
            if (dialog.open() == Window.OK) {
                variable.setFormat(wizard.getVariable().getFormat());
                variable.setUserType(wizard.getVariable().getUserType());
                variable.setDefaultValue(wizard.getVariable().getDefaultValue());
                variable.setStoreType(wizard.getVariable().getStoreType());
                WorkspaceOperations.saveDataTable(dataTableFile, dataTable);
                rebuildView(editorComposite);
            }
        }
    }

    private class CreateAttributeSelectionListener extends LoggingSelectionAdapter {
        @Override
        protected void onSelection(SelectionEvent e) throws Exception {
            VariableWizard wizard = new VariableWizard(new ProcessDefinition(null), dataTable, null, true, true, false);
            CompactWizardDialog dialog = new CompactWizardDialog(wizard);
            if (dialog.open() == Window.OK) {
                Variable variable = wizard.getVariable();
                dataTable.addAttribute(variable);
                WorkspaceOperations.saveDataTable(dataTableFile, dataTable);
                tableViewer.setSelection(new StructuredSelection(variable));
                rebuildView(editorComposite);
            }
        }
    }

    private class CopyAttributeSelectionListener extends LoggingSelectionAdapter {
        @Override
        protected void onSelection(SelectionEvent e) throws Exception {
            Clipboard clipboard = new Clipboard(getDisplay());
            @SuppressWarnings("unchecked")
            List<Variable> list = ((IStructuredSelection) tableViewer.getSelection()).toList();
            clipboard.setContents(new Object[] { list }, new Transfer[] { VariableTransfer.getInstance() });
        }
    }

    private class PasteAttributeSelectionListener extends LoggingSelectionAdapter {
        @Override
        protected void onSelection(SelectionEvent e) throws Exception {
            Clipboard clipboard = new Clipboard(getDisplay());
            @SuppressWarnings("unchecked")
            List<Variable> data = (List<Variable>) clipboard.getContents(VariableTransfer.getInstance());
            if (data != null) {
                for (Variable variable : data) {
                    boolean nameAllowed = true;
                    Variable newVariable = VariableUtils.getVariableByName(dataTable, variable.getName());
                    if (newVariable == null) {
                        newVariable = new Variable(variable);
                        dataTable.addAttribute(newVariable);
                        WorkspaceOperations.saveDataTable(dataTableFile, dataTable);
                    } else {
                        UpdateVariableNameDialog dialog = new UpdateVariableNameDialog(dataTable, newVariable);
                        nameAllowed = dialog.open() == Window.OK;
                        if (nameAllowed) {
                            newVariable = new Variable(variable);
                            newVariable.setName(dialog.getName());
                            newVariable.setScriptingName(dialog.getScriptingName());
                            dataTable.addAttribute(newVariable);
                            tableViewer.setSelection(new StructuredSelection(variable));
                            WorkspaceOperations.saveDataTable(dataTableFile, dataTable);
                        }
                    }
                    rebuildView(editorComposite);
                }
            }
            clipboard.dispose();
        }
    }
}