package ru.runa.gpd.ui.wizard;

import com.google.common.base.Throwables;
import java.util.List;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.ide.IDE;
import ru.runa.gpd.Localization;
import ru.runa.gpd.PluginLogger;
import ru.runa.gpd.editor.DataTableEditor;
import ru.runa.gpd.lang.model.Variable;
import ru.runa.gpd.lang.model.VariableUserType;
import ru.runa.gpd.sync.WfeServerConnectorComposite;
import ru.runa.gpd.sync.WfeServerConnectorSynchronizationCallback;
import ru.runa.gpd.sync.WfeServerDataTableImporter;
import ru.runa.gpd.util.DataTableUtils;
import ru.runa.gpd.util.WorkspaceOperations;
import ru.runa.wfe.var.UserType;
import ru.runa.wfe.var.VariableDefinition;

public class ImportDataTableWizardPage extends ImportWizardPage {
    private WfeServerConnectorComposite serverConnectorComposite;
    private TreeViewer serverDataTableViewer;
    private IWorkbench workbench;

    public ImportDataTableWizardPage(IStructuredSelection selection) {
        super(ImportDataTableWizardPage.class, selection);
        setTitle(Localization.getString("ImportDataTableWizardPage.page.title"));
    }

    @Override
    public void createControl(Composite parent) {
        Composite pageControl = new Composite(parent, SWT.NONE);
        pageControl.setLayout(new GridLayout(1, false));
        pageControl.setLayoutData(new GridData(GridData.FILL_BOTH));
        Group importGroup = new Group(pageControl, SWT.NONE);
        importGroup.setLayout(new GridLayout(1, false));
        importGroup.setLayoutData(new GridData(GridData.FILL_BOTH));

        GridData gridData = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL);
        gridData.heightHint = 30;

        Label importFromServerLabel = new Label(importGroup, SWT.RADIO);
        importFromServerLabel.setText(Localization.getString("button.importFromServer"));
        new WfeServerConnectorComposite(importGroup, WfeServerDataTableImporter.getInstance(),
                new WfeServerConnectorSynchronizationCallback() {

                    @Override
                    public void onCompleted() {
                        updateServerDataTableViewer(WfeServerDataTableImporter.getInstance().getData());
                    }

                    @Override
                    public void onFailed() {
                        updateServerDataTableViewer(null);
                    }

                });
        createServerDataTableGroup(importGroup);
        setControl(pageControl);
        updateServerDataTableViewer(WfeServerDataTableImporter.getInstance().getData());
    }

    private void updateServerDataTableViewer(Object data) {
        serverDataTableViewer.setInput(data);
        serverDataTableViewer.refresh(true);
    }

    private void createServerDataTableGroup(Composite parent) {
        serverDataTableViewer = new TreeViewer(parent);
        GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.heightHint = 100;
        serverDataTableViewer.getControl().setLayoutData(gridData);
        serverDataTableViewer.setContentProvider(new ViewContentProvider());
        serverDataTableViewer.setLabelProvider(new LabelProvider());
        serverDataTableViewer.setInput(new Object());
    }

    public boolean finish() {
        try {
            for (TreeItem treeItem : serverDataTableViewer.getTree().getSelection()) {
                if (DataTableUtils.getDataTableProject().getFile(treeItem.getText() + DataTableUtils.FILE_EXTENSION).exists()) {
                    throw new Exception(Localization.getString("ImportDataTableWizardPage.error.dataTableWithSameNameExists", treeItem.getText()));
                }
                UserType userType = WfeServerDataTableImporter.getInstance().getDataTable(treeItem.getText());
                IProject dtProject = DataTableUtils.getDataTableProject();
                IFile dataTableFile = dtProject.getFile(treeItem.getText() + DataTableUtils.FILE_EXTENSION);
                VariableUserType dataTable = new VariableUserType(treeItem.getText());
                for (VariableDefinition varDef : userType.getAttributes()) {
                    Variable variable = new Variable(varDef.getName(), varDef.getScriptingName(), varDef.getFormat(), null);
                    dataTable.addAttribute(variable);
                }
                WorkspaceOperations.saveDataTable(dataTableFile, dataTable);
                IDE.openEditor(getActivePage(), dataTableFile, DataTableEditor.ID, true);
            }
        } catch (Exception exception) {
            PluginLogger.logErrorWithoutDialog("import data table", exception);
            setErrorMessage(Throwables.getRootCause(exception).getMessage());
            return false;
        } 
        return true;
    }

    class ViewContentProvider implements IStructuredContentProvider, ITreeContentProvider {

        @Override
        public void inputChanged(Viewer v, Object oldInput, Object newInput) {
        }

        @Override
        public void dispose() {
        }

        @Override
        public Object[] getElements(Object parent) {
            return getChildren(parent);
        }

        @Override
        public Object getParent(Object child) {
            return null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Object[] getChildren(Object parent) {
            return parent instanceof List<?> ? ((List<String>) parent).toArray(new String[] {}) : new Object[0];
        }

        @Override
        public boolean hasChildren(Object parent) {
            return false;
        }
    }

    public void setWorkbench(IWorkbench workbench) {
        this.workbench = workbench;
    }

    private IWorkbenchPage getActivePage() {
        return workbench.getActiveWorkbenchWindow().getActivePage();
    }
}
