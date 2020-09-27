package ru.runa.gpd.bizagi.wizard;

import com.google.common.base.Strings;
import java.io.File;
import java.util.Arrays;
import org.eclipse.core.resources.IContainer;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import ru.runa.gpd.PluginLogger;
import ru.runa.gpd.SharedImages;
import ru.runa.gpd.bizagi.Activator;
import ru.runa.gpd.bizagi.converter.BizagiBpmnImporter;
import ru.runa.gpd.bizagi.resource.Messages;
import ru.runa.gpd.ui.wizard.ImportWizardPage;
import ru.runa.gpd.util.IOUtils;
import ru.runa.gpd.util.WorkspaceOperations;

public class ImportBizagiBpmnWizardPage extends ImportWizardPage {

    private Composite fileSelectionArea;
    private FileFieldEditor selectFileEditor;
    private BooleanFieldEditor showSwimlanesEditor;
    private BooleanFieldEditor ignoreBendPointsEditor;

    public ImportBizagiBpmnWizardPage(String pageName, IStructuredSelection selection) {
        super(ImportBizagiBpmnWizardPage.class, selection);
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
        createProjectsGroup(sashForm, IOUtils.getAllProcessContainers(), new LabelProvider() {
            @Override
            public String getText(Object element) {
                return IOUtils.getProcessContainerName((IContainer) element);
            }
        });
        ((GridData) projectViewer.getControl().getLayoutData()).heightHint = 200;
        Group importGroup = new Group(sashForm, SWT.NONE);
        sashForm.setWeights(new int[] { 20, 20 });
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
        selectFileEditor = new FileFieldEditor("selectFile", Messages.getString("ImportBpmnWizardPage.page.selectFile"), fileSelectionArea);
        selectFileEditor.setFileExtensions(new String[] { "*.bpmn" });
        showSwimlanesEditor = new BooleanFieldEditor("showSwimlanes", Messages.getString("ImportBpmnWizardPage.show_swimlanes"), fileSelectionArea);
        ignoreBendPointsEditor = new BooleanFieldEditor("ignoreBendPoints", Messages.getString("ImportBpmnWizardPage.ignore_bend_points"),
                fileSelectionArea);
        showSwimlanesEditor.getChangeControl(fileSelectionArea).setSelection(true);
        setControl(pageControl);
    }

    public boolean performFinish() {
        try {
            IContainer container = getSelectedProject();
            String bpmnFileName = selectFileEditor.getStringValue();
            if (bpmnFileName == null || !new File(bpmnFileName).exists()) {
                throw new Exception(Messages.getString("ImportBpmnWizardPage.error.selectFile"));
            }
            BizagiBpmnImporter.go(container, bpmnFileName, showSwimlanesEditor.getBooleanValue(), ignoreBendPointsEditor.getBooleanValue());
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

    private class BooleanFieldEditor extends org.eclipse.jface.preference.BooleanFieldEditor {

        private Button changeControl;

        public BooleanFieldEditor(String name, String labelText, Composite parent) {
            super(name, labelText, parent);
        }

        @Override
        public Button getChangeControl(Composite parent) {
            if (changeControl == null) {
                changeControl = super.getChangeControl(parent);
            }
            return changeControl;
        }

        @Override
        public int getNumberOfControls() {
            return 3;
        }

    }

}
