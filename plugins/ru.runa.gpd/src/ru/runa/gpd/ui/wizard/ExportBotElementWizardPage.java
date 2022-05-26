package ru.runa.gpd.ui.wizard;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import java.util.Map;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import ru.runa.gpd.Localization;
import ru.runa.gpd.PluginLogger;
import ru.runa.gpd.sync.WfeServerBotImporter;
import ru.runa.gpd.sync.WfeServerConnector;
import ru.runa.gpd.sync.WfeServerConnectorComposite;
import ru.runa.gpd.ui.custom.LoggingSelectionAdapter;
import ru.runa.gpd.util.WizardPageUtils;

public abstract class ExportBotElementWizardPage extends ExportWizardPage {
    protected Map<String, IResource> exportObjectNameFileMap;
    protected final IResource exportResource;
    private ListViewer exportResourceListViewer;
    private Button exportToFileButton;
    private Button exportToServerButton;
    private WfeServerConnectorComposite serverConnectorComposite;

    public ExportBotElementWizardPage(Class<? extends ExportWizardPage> clazz, IStructuredSelection selection) {
        super(clazz);
        this.exportResource = WizardPageUtils.getInitialElement(selection);
    }

    @Override
    public void createControl(Composite parent) {
        Composite pageControl = WizardPageUtils.createPageControl(parent);
        SashForm sashForm = WizardPageUtils.createSashForm(pageControl);
        exportResourceListViewer = WizardPageUtils.createViewer(sashForm, "label.process",
                exportObjectNameFileMap.keySet(), e -> setPageComplete(!e.getSelection().isEmpty()));
        Group exportGroup = WizardPageUtils.createExportGroup(sashForm);
        exportToFileButton = new Button(exportGroup, SWT.RADIO);
        exportToFileButton.setText(Localization.getString("button.exportToFile"));
        exportToFileButton.setSelection(true);
        exportToFileButton.addSelectionListener(new LoggingSelectionAdapter() {
            @Override
            protected void onSelection(SelectionEvent e) throws Exception {
                onExportModeChanged();
            }
        });
        createDestinationDirectoryGroup(exportGroup);
        exportToServerButton = new Button(exportGroup, SWT.RADIO);
        exportToServerButton.setText(Localization.getString("button.exportToServer"));
        serverConnectorComposite = new WfeServerConnectorComposite(exportGroup, WfeServerBotImporter.getInstance(), null);
        setControl(pageControl);
        if (exportResource != null) {
            exportResourceListViewer.setSelection(new StructuredSelection(getSelectionResourceKey(exportResource)));
        }
        onExportModeChanged();
    }

    private void onExportModeChanged() {
        boolean fromFile = exportToFileButton.getSelection();
        destinationValueText.setEnabled(fromFile);
        browseButton.setEnabled(fromFile);
        serverConnectorComposite.setEnabled(!fromFile);
    }

    @Override
    protected void onBrowseButtonSelected() {
        FileDialog dialog = new FileDialog(getContainer().getShell(), SWT.SAVE);
        dialog.setFilterExtensions(new String[] { getOutputSuffix(), "*.*" });
        String selectionName = getBotElementSelection();
        WizardPageUtils.onBrowseButtonSelected(dialog, selectionName, () -> getFileName(selectionName), getDestinationValue(),
                () -> setErrorMessage(null), this::setDestinationValue);
    }

    protected String getKey(IProject project, IResource resource) {
        return project.getName() + "/" + resource.getName();
    }

    private String getBotElementSelection() {
        return (String) ((IStructuredSelection) exportResourceListViewer.getSelection()).getFirstElement();
    }

    protected String getFileName(String selectionName) {
        return selectionName.substring(selectionName.lastIndexOf("/") + 1) + getOutputSuffix();
    }

    public boolean finish() {
        boolean exportToFile = exportToFileButton.getSelection();
        saveDirtyEditors();
        String selected = getBotElementSelection();
        if (selected == null) {
            setErrorMessage("ExportBotElementWizardPage.error.empty.source.selection");
            return false;
        }
        if (exportToFile && Strings.isNullOrEmpty(getDestinationValue())) {
            setErrorMessage(Localization.getString("error.selectDestinationPath"));
            return false;
        }
        if (!exportToFile && !WfeServerConnector.getInstance().isConfigured()) {
            setErrorMessage(Localization.getString("error.selectValidConnection"));
            return false;
        }
        IResource exportResource = exportObjectNameFileMap.get(selected);
        try {
            exportResource.refreshLocal(IResource.DEPTH_ONE, null);
            if (exportToFile) {
                exportToZipFile(exportResource);
            } else {
                deployToServer(exportResource);
            }
            return true;
        } catch (Throwable th) {
            PluginLogger.logErrorWithoutDialog("ExportBotElementWizardPage.error.export", th);
            setErrorMessage(Throwables.getRootCause(th).getMessage());
            return false;
        }
    }

    protected abstract void exportToZipFile(IResource exportResource) throws Exception;

    protected abstract void deployToServer(IResource exportResource) throws Exception;

    protected abstract String getOutputSuffix();

    protected abstract String getSelectionResourceKey(IResource resource);

}
