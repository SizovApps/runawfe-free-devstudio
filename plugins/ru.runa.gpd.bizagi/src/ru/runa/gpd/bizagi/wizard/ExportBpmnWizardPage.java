package ru.runa.gpd.bizagi.wizard;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.internal.wizards.datatransfer.WizardFileSystemResourceExportPage1;
import ru.runa.gpd.Localization;
import ru.runa.gpd.PluginLogger;
import ru.runa.gpd.ProcessCache;
import ru.runa.gpd.SharedImages;
import ru.runa.gpd.bizagi.Activator;
import ru.runa.gpd.bizagi.converter.BpmnExporter;
import ru.runa.gpd.bizagi.resource.Messages;
import ru.runa.gpd.lang.model.ProcessDefinition;
import ru.runa.gpd.lang.model.SubprocessDefinition;
import ru.runa.gpd.util.IOUtils;

@SuppressWarnings("restriction")
public class ExportBpmnWizardPage extends WizardFileSystemResourceExportPage1 {
    private final Map<String, IFile> definitionNameFileMap;
    private ListViewer definitionListViewer;
    private BooleanFieldEditor expandMinimizedNodes;

    protected ExportBpmnWizardPage(IStructuredSelection selection) {
        super(selection);
        setTitle(Messages.getString("ExportBpmnWizardPage.page.title"));
        setDescription(Localization.getString("ExportParWizardPage.page.description"));
        setImageDescriptor(SharedImages.getImageDescriptor(Activator.getDefault().getBundle(), "icons/process.png", false));
        this.definitionNameFileMap = new TreeMap<String, IFile>();
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
        Group processListGroup = new Group(pageControl, SWT.NONE);
        processListGroup.setLayout(new GridLayout(1, false));
        processListGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
        processListGroup.setText(Localization.getString("label.process"));
        createViewer(processListGroup);
        createDestinationGroup(pageControl);
        restoreWidgetValues();
        giveFocusToDestination();
        setControl(pageControl);
        setPageComplete(false);
        IFile adjacentFile = IOUtils.getCurrentFile();
        if (adjacentFile != null && adjacentFile.getParent().exists()) {
            IFile definitionFile = IOUtils.getProcessDefinitionFile((IFolder) adjacentFile.getParent());
            if (definitionFile.exists()) {
                ProcessDefinition currentDefinition = ProcessCache.getProcessDefinition(definitionFile);
                if (currentDefinition != null && !(currentDefinition instanceof SubprocessDefinition)) {
                    definitionListViewer.setSelection(new StructuredSelection(getKey(definitionFile, currentDefinition)));
                }
            }
        }
        expandMinimizedNodes = new BooleanFieldEditor("expandMinimizedNodes", Messages.getString("ExportBpmnWizardPage.expand_minimized_nodes"),
                processListGroup);
    }

    private void createViewer(Composite parent) {
        // process selection
        definitionListViewer = new ListViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
        definitionListViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
        definitionListViewer.setContentProvider(new ArrayContentProvider());
        definitionListViewer.setInput(definitionNameFileMap.keySet());
        definitionListViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                setPageComplete(!event.getSelection().isEmpty());
            }
        });
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

    @Override
    protected String getDestinationLabel() {
        return Localization.getString("ExportParWizardPage.label.destination_file");
    }

    @Override
    protected void handleDestinationBrowseButtonPressed() {
        DirectoryDialog dialog = new DirectoryDialog(getContainer().getShell(), SWT.SAVE);
        dialog.setFilterPath(getDestinationValue());
        String selectedFolderName = dialog.open();
        if (selectedFolderName != null) {
            setErrorMessage(null);
            if (!selectedFolderName.endsWith(File.separator)) {
                selectedFolderName += File.separator;
            }
            setDestinationValue(selectedFolderName);
        }
    }

    @Override
    protected void updatePageCompletion() {
        setPageComplete(true);
    }

    @Override
    public boolean finish() {
        // Save dirty editors if possible but do not stop if not all are saved
        saveDirtyEditors();
        // about to invoke the operation so save our state
        saveWidgetValues();
        List<String> selectedDefinitionNames = ((IStructuredSelection) definitionListViewer.getSelection()).toList();
        if (selectedDefinitionNames.size() == 0) {
            setErrorMessage(Localization.getString("ExportParWizardPage.error.selectProcess"));
            return false;
        }
        if (Strings.isNullOrEmpty(getDestinationValue())) {
            setErrorMessage(Localization.getString("ExportParWizardPage.error.selectDestinationPath"));
            return false;
        }
        for (String selectedDefinitionName : selectedDefinitionNames) {
            try {
                IFile definitionFile = definitionNameFileMap.get(selectedDefinitionName);
                IFolder processFolder = (IFolder) definitionFile.getParent();
                processFolder.refreshLocal(IResource.DEPTH_ONE, null);
                ProcessDefinition definition = ProcessCache.getProcessDefinition(definitionFile);
                definition.getLanguage().getSerializer().validateProcessDefinitionXML(definitionFile);
                String outputFileName = getDestinationValue() + definition.getName() + ".bpmn";
                new BpmnExporter().go(definition, outputFileName, expandMinimizedNodes.getBooleanValue());
            } catch (Throwable th) {
                PluginLogger.logErrorWithoutDialog(Localization.getString("ExportParWizardPage.error.export"), th);
                setErrorMessage(Throwables.getRootCause(th).getMessage());
                return false;
            }
        }
        return true;
    }

    private final static String STORE_DESTINATION_NAMES_ID = "ExportBpmnWizardPage.STORE_DESTINATION_NAMES_ID";

    @Override
    protected void internalSaveWidgetValues() {
        // update directory names history
        IDialogSettings settings = getDialogSettings();
        if (settings != null) {
            String[] directoryNames = settings.getArray(STORE_DESTINATION_NAMES_ID);
            if (directoryNames == null) {
                directoryNames = new String[0];
            }
            directoryNames = addToHistory(directoryNames, getDestinationValue());
            settings.put(STORE_DESTINATION_NAMES_ID, directoryNames);
        }
    }

    @Override
    protected void restoreWidgetValues() {
        IDialogSettings settings = getDialogSettings();
        if (settings != null) {
            String[] directoryNames = settings.getArray(STORE_DESTINATION_NAMES_ID);
            if (directoryNames == null || directoryNames.length == 0) {
                return; // ie.- no settings stored
            }
            // destination
            setDestinationValue(directoryNames[0]);
            for (int i = 0; i < directoryNames.length; i++) {
                addDestinationItem(directoryNames[i]);
            }
        }
    }

}
