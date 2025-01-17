package ru.runa.gpd.ui.wizard;

import com.google.common.base.Throwables;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import ru.runa.gpd.DataTableCache;
import ru.runa.gpd.Localization;
import ru.runa.gpd.PluginLogger;
import ru.runa.gpd.ProcessCache;
import ru.runa.gpd.lang.model.ProcessDefinition;
import ru.runa.gpd.lang.model.SubprocessDefinition;
import ru.runa.gpd.lang.model.VariableUserType;
import ru.runa.gpd.util.IOUtils;
import ru.runa.gpd.util.WorkspaceOperations;
import ru.runa.wfe.InternalApplicationException;

public class ExportDataTableToProcessWizardPage extends ExportWizardPage {
    private final Map<String, IFile> definitionNameFileMap;
    private ListViewer definitionListViewer;
    private String dataTableFileName;

    protected ExportDataTableToProcessWizardPage(IFile dataTableFile) {
        super(ExportDataTableToProcessWizardPage.class);
        this.dataTableFileName = IOUtils.getWithoutExtension(dataTableFile.getName());
        setTitle(Localization.getString("ExportDataTableToProcessWizardPage.page.title"));
        setDescription(Localization.getString("ExportDataTableToProcessWizardPage.page.description"));
        definitionNameFileMap = new TreeMap<>();
        for (IFile file : ProcessCache.getAllProcessDefinitionsMap().keySet()) {
            ProcessDefinition definition = ProcessCache.getProcessDefinition(file);
            if (definition != null && !(definition instanceof SubprocessDefinition)) {
                definitionNameFileMap.put(getKey(file, definition), file);
            }
        }
    }

    @Override
    public void createControl(Composite parent) {
        Composite pageControl = new Composite(parent, SWT.NONE);
        pageControl.setLayout(new GridLayout(1, false));
        pageControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        SashForm sashForm = new SashForm(pageControl, SWT.HORIZONTAL);
        sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));
        Group processListGroup = new Group(sashForm, SWT.NONE);
        processListGroup.setLayout(new GridLayout(1, false));
        processListGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
        processListGroup.setText(Localization.getString("label.process"));
        createViewer(processListGroup);
        setControl(pageControl);
    }

    @Override
    protected void onBrowseButtonSelected() {

    }

    private void createViewer(Composite parent) {
        definitionListViewer = new ListViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
        definitionListViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
        definitionListViewer.setContentProvider(new ArrayContentProvider());
        definitionListViewer.setInput(definitionNameFileMap.keySet());
        definitionListViewer.addSelectionChangedListener(event -> setPageComplete(!event.getSelection().isEmpty()));
    }

    private String getKey(IFile definitionFile, ProcessDefinition definition) {
        IProject project = definitionFile.getProject();
        if (IOUtils.isProjectHasProcessNature(project)) {
            String path = definitionFile.getParent().getFullPath().toString();
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            return path;
        } else {
            return project.getName() + "/" + definition.getName();
        }
    }

    public boolean finish() {
        saveDirtyEditors();
        List<String> selectedDefinitionNames = ((IStructuredSelection) definitionListViewer.getSelection()).toList();
        if (selectedDefinitionNames.size() == 0) {
            setErrorMessage(Localization.getString("ExportDataTableToProcessWizardPage.error.selectProcess"));
            return false;
        }
        for (final String selectedDefinitionName : selectedDefinitionNames) {
            try {
                VariableUserType dataTable = DataTableCache.getDataTable(dataTableFileName);
                final IFile definitionFile = definitionNameFileMap.get(selectedDefinitionName);
                ProcessDefinition processDefinition = ProcessCache.getProcessDefinition(definitionFile);
                Set<String> userTypeNamesInProcess = processDefinition.getVariableUserTypes().stream().map(VariableUserType::getName)
                        .collect(Collectors.toSet());
                if (userTypeNamesInProcess.contains(dataTable.getName())) {
                    throw new InternalApplicationException(Localization.getString("ExportDataTableToProcessWizardPage.error.usertype.already.exists",
                            dataTable.getName(), processDefinition.getName()));
                } else {
                    dataTable.setStoreInExternalStorage(true);
                    processDefinition.addVariableUserType(dataTable);
                    WorkspaceOperations.saveProcessDefinition(processDefinition);
                }
            } catch (Throwable th) {
                PluginLogger.logErrorWithoutDialog(Localization.getString("ExportParWizardPage.error.export"), th);
                setErrorMessage(Throwables.getRootCause(th).getMessage());
                return false;
            }
        }
        return true;
    }

}
