package ru.runa.gpd.editor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;

import ru.runa.gpd.DataTableCache;
import ru.runa.gpd.Localization;
import ru.runa.gpd.PluginLogger;
import ru.runa.gpd.lang.model.ProcessDefinition;
import ru.runa.gpd.lang.model.Variable;
import ru.runa.gpd.lang.model.VariableUserType;
import ru.runa.gpd.ui.custom.LoggingSelectionAdapter;
import ru.runa.gpd.ui.wizard.CompactWizardDialog;
import ru.runa.gpd.ui.wizard.VariableWizard;
import ru.runa.gpd.util.IOUtils;
import ru.runa.gpd.util.WorkspaceOperations;

public class DataTableEditor extends EditorPart implements ISelectionListener, IResourceChangeListener, PropertyChangeListener {

    public static final String ID = "ru.runa.gpd.editor.DataTableEditor";

    private Composite editorComposite;
    private TableViewer tableViewer;
    private IFile dataTableFile;
    private VariableUserType dataTable;
    private FormToolkit toolkit;
    private Button createAttributeButton;
    private Button changeAttributeButton;
    private Button renameAttributeButton;
    private Button deleteAttributeButton;

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
        String dataTableFileNameWithoutExtension = dataTableFile.getName().substring(0, dataTableFile.getName().lastIndexOf('.'));
        try {
            dataTable = DataTableCache.getDataTable(dataTableFileNameWithoutExtension);
        } catch (Exception e) {
            throw new PartInitException("", e);
        }
        setPartName(IOUtils.getWithoutExtension(dataTableFile.getName()));
        getSite().getPage().addSelectionListener(this);
        ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
    }

    @Override
    public void setFocus() {
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void resourceChanged(IResourceChangeEvent arg0) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        // TODO Auto-generated method stub
        
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
        // TODO Auto-generated method stub
        
    }
    @Override
    public boolean isDirty() {
        // TODO Auto-generated method stub
        return false;
    }
    @Override
    public boolean isSaveAsAllowed() {
        // TODO Auto-generated method stub
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
        tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                // updateUI();
            }
        });
        getSite().setSelectionProvider(tableViewer);
        Table table = tableViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        String[] columnNames = new String[] { Localization.getString("property.name"), Localization.getString("Variable.property.format"),
                Localization.getString("Variable.property.defaultValue") };
        int[] columnWidths = new int[] { 400, 200, 200 };
        int[] columnAlignments = new int[] { SWT.LEFT, SWT.LEFT, SWT.LEFT };
        for (int i = 0; i < columnNames.length; i++) {
            TableColumn tableColumn = new TableColumn(table, columnAlignments[i]);
            tableColumn.setText(columnNames[i]);
            tableColumn.setWidth(columnWidths[i]);
        }
        List<String[]> userTypeAttributes = new ArrayList<>();
        if (dataTable != null) {
            for (Variable userTypeAttribute : dataTable.getAttributes()) {
                String[] attr = new String[] { userTypeAttribute.getName(), userTypeAttribute.getFormatLabel(), userTypeAttribute.getDefaultValue() };
                userTypeAttributes.add(attr);
            }
            tableViewer.setInput(userTypeAttributes);
        } else {
            tableViewer.setInput(new Object[0]);
        }
    }

    private void createButtonsBar(Composite composite) {
        Composite buttonsBar = getToolkit().createComposite(composite);
        buttonsBar.setLayout(new GridLayout(1, false));
        GridData gridData = new GridData();
        gridData.horizontalAlignment = SWT.LEFT;
        gridData.verticalAlignment = SWT.TOP;
        gridData.grabExcessVerticalSpace = true;
        buttonsBar.setLayoutData(gridData);
        createAttributeButton = addButton(buttonsBar, "button.create", new CreateAttributeSelectionListener(), false);
        changeAttributeButton = addButton(buttonsBar, "button.change", new ChangeAttributeSelectionListener(), false);
        renameAttributeButton = addButton(buttonsBar, "button.rename", new RenameAttributeSelectionListener(), false);
        deleteAttributeButton = addButton(buttonsBar, "button.delete", new DeleteAttributeSelectionListener(), false);
    }

    private static class TableLabelProvider extends LabelProvider implements ITableLabelProvider {
        @Override
        public String getColumnText(Object element, int index) {
            String[] data = (String[]) element;
            return data[index];
        }

        @Override
        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }
    }

    private Button addButton(Composite parent, String buttonKey, SelectionAdapter selectionListener, boolean addToMenu) {
        String title = Localization.getString(buttonKey);
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        Button button = getToolkit().createButton(parent, title, SWT.PUSH);
        button.setLayoutData(gridData);
        button.addSelectionListener(selectionListener);
        return button;
    }

    public FormToolkit getToolkit() {
        if (toolkit == null) {
            toolkit = new FormToolkit(Display.getDefault());
        }
        return toolkit;
    }

    @SuppressWarnings("unchecked")
    protected <T> T getSelection() {
        return tableViewer == null ? null : (T) ((IStructuredSelection) tableViewer.getSelection()).getFirstElement();
    }

    private class DeleteAttributeSelectionListener extends SelectionAdapter {

    }

    private class RenameAttributeSelectionListener extends SelectionAdapter {

    }

    private class ChangeAttributeSelectionListener extends SelectionAdapter {

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
}
