package ru.runa.gpd.bizagi.wizard;

import com.google.common.base.Strings;
import java.io.File;
import java.util.Arrays;
import org.eclipse.core.resources.IContainer;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import ru.runa.gpd.PluginLogger;
import ru.runa.gpd.SharedImages;
import ru.runa.gpd.bizagi.Activator;
import ru.runa.gpd.bizagi.converter.BpmnImporter;
import ru.runa.gpd.bizagi.resource.Messages;
import ru.runa.gpd.ui.wizard.ImportWizardPage;
import ru.runa.gpd.util.WorkspaceOperations;

public class ImportBpmnWizardPage extends ImportWizardPage {

    private Composite fileSelectionArea;
    private FileFieldEditor editor;

    public ImportBpmnWizardPage(String pageName, IStructuredSelection selection) {
        super(pageName, selection);
        setTitle(Messages.getString("ImportBpmnWizardPage.page.title"));
        setImageDescriptor(SharedImages.getImageDescriptor(Activator.getDefault().getBundle(), "icons/process.png", false));
    }

    @Override
    public void createControl(Composite parent) {
        Composite pageControl = new Composite(parent, SWT.NONE);
        pageControl.setLayout(new GridLayout(1, false));
        pageControl.setLayoutData(new GridData(GridData.FILL_BOTH));
        SashForm sashForm = new SashForm(pageControl, SWT.VERTICAL);
        sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));
        createProjectsGroup(sashForm);
        ((GridData) projectViewer.getControl().getLayoutData()).heightHint = 200;
        Group importGroup = new Group(sashForm, SWT.NONE);
        sashForm.setWeights(new int[] { 8, 2 });
        importGroup.setLayout(new GridLayout(1, false));
        importGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
        fileSelectionArea = new Composite(importGroup, SWT.NONE);
        GridData fileSelectionData = new GridData(GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL);
        fileSelectionArea.setLayoutData(fileSelectionData);
        GridLayout fileSelectionLayout = new GridLayout();
        fileSelectionLayout.numColumns = 3;
        fileSelectionLayout.makeColumnsEqualWidth = false;
        fileSelectionLayout.marginWidth = 0;
        fileSelectionLayout.marginHeight = 0;
        fileSelectionArea.setLayout(fileSelectionLayout);
        editor = new FileFieldEditor("fileSelect", Messages.getString("ImportBpmnWizardPage.page.title"), fileSelectionArea);
        editor.setFileExtensions(new String[] { "*.bpmn" });
        setControl(pageControl);
    }

    public boolean performFinish() {
        try {
            IContainer container = getSelectedContainer();
            String bpmnFileName = editor.getStringValue();
            if (bpmnFileName == null || !new File(bpmnFileName).exists()) {
                throw new Exception(Messages.getString("ImportBpmnWizardPage.error.selectFile"));
            }
            BpmnImporter.go(container, bpmnFileName);
            WorkspaceOperations.refreshResources(Arrays.asList(container));
            return true;
        } catch (Exception e) {
            String s = e.getMessage();
            if (Strings.isNullOrEmpty(s)) {
                s = e.getClass().getName();
            }
            setErrorMessage(s);
            PluginLogger.logErrorWithoutDialog("import from bpmn", e);
            return false;
        }
    }

}
