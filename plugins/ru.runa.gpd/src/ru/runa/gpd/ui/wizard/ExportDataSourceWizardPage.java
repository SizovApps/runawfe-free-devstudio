package ru.runa.gpd.ui.wizard;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import ru.runa.gpd.Localization;
import ru.runa.gpd.PluginLogger;
import ru.runa.gpd.sync.WfeServerConnector;
import ru.runa.gpd.sync.WfeServerConnectorComposite;
import ru.runa.gpd.sync.WfeServerDataSourceImporter;
import ru.runa.gpd.sync.WfeServerProcessDefinitionImporter;
import ru.runa.gpd.ui.custom.Dialogs;
import ru.runa.gpd.ui.custom.LoggingSelectionAdapter;
import ru.runa.gpd.util.DataSourceUtils;
import ru.runa.gpd.util.WizardPageUtils;
import ru.runa.gpd.util.files.FileExporter;
import ru.runa.wfe.datasource.DataSourceStuff;

public class ExportDataSourceWizardPage extends ExportWizardPage {
    private final Map<String, IFile> dataSourceNameFileMap;
    private ListViewer dataSourceListViewer;
    protected final IResource exportResource;
    private Button exportToFileButton;
    private Button exportToServerButton;
    private WfeServerConnectorComposite serverConnectorComposite;

    protected ExportDataSourceWizardPage(IStructuredSelection selection) {
        super(ExportDataSourceWizardPage.class);
        setTitle(Localization.getString("ExportDataSourceWizard.wizard.title"));
        setDescription(Localization.getString("ExportDataSourceWizardPage.page.description"));
        dataSourceNameFileMap = new TreeMap<String, IFile>();
        for (IFile file : DataSourceUtils.getAllDataSources()) {
            String name = file.getName();
            dataSourceNameFileMap.put(name.substring(0, name.lastIndexOf('.')), file);
        }
        exportResource = WizardPageUtils.getInitialElement(selection);
    }

    @Override
    public void createControl(Composite parent) {
        Composite pageControl = WizardPageUtils.createPageControl(parent);
        SashForm sashForm = WizardPageUtils.createSashForm(pageControl);
        dataSourceListViewer = WizardPageUtils.createViewer(sashForm, "label.view.dataSourceDesignerExplorer",
                dataSourceNameFileMap.keySet(), e -> setPageComplete(!e.getSelection().isEmpty()));
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
        serverConnectorComposite = new WfeServerConnectorComposite(exportGroup, WfeServerProcessDefinitionImporter.getInstance(), null);
        setControl(pageControl);
        if (exportResource != null) {
            String name = exportResource.getName();
            dataSourceListViewer.setSelection(new StructuredSelection(name.substring(0, name.lastIndexOf('.'))));
        }
        onExportModeChanged();
    }

    private void onExportModeChanged() {
        boolean fromFile = exportToFileButton.getSelection();
        destinationValueText.setEnabled(fromFile);
        browseButton.setEnabled(fromFile);
        serverConnectorComposite.setEnabled(!fromFile);
    }

    private String getSelection() {
        return (String) ((IStructuredSelection) dataSourceListViewer.getSelection()).getFirstElement();
    }

    private String getFileName(String selectionName) {
        return selectionName.substring(selectionName.lastIndexOf("/") + 1) + DataSourceStuff.DATA_SOURCE_ARCHIVE_SUFFIX;
    }

    @Override
    protected void onBrowseButtonSelected() {
        FileDialog dialog = new FileDialog(getContainer().getShell(), SWT.SAVE);
        dialog.setFilterExtensions(new String[] { DataSourceStuff.DATA_SOURCE_ARCHIVE_SUFFIX, "*.*" });
        String selectionName = getSelection();
        WizardPageUtils.onBrowseButtonSelected(dialog, selectionName, () -> getFileName(selectionName), getDestinationValue(),
                () -> setErrorMessage(null), this::setDestinationValue);
    }

    public boolean finish() {
        boolean exportToFile = exportToFileButton.getSelection();
        String selected = getSelection();
        if (selected == null) {
            setErrorMessage("select");
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
        IResource exportResource = dataSourceNameFileMap.get(selected);
        try {
            exportResource.refreshLocal(IResource.DEPTH_ONE, null);
            if (exportToFile) {
                exportToZipFile(exportResource);
            } else {
                String name = exportResource.getName();
                name = name.substring(0, name.lastIndexOf('.'));
                List<String> serverDataSourceNames = WfeServerDataSourceImporter.getInstance().getData();
                if (!serverDataSourceNames.contains(name)
                        || Dialogs.confirm(Localization.getString("ExportDataSourceWizardPage.error.dataSourceWithSameNameExists", name))) {
                    deployToServer(exportResource);
                } else {
                    return false;
                }
            }
            return true;
        } catch (Throwable th) {
            PluginLogger.logErrorWithoutDialog("datasource.error.export", th);
            setErrorMessage(Throwables.getRootCause(th).getMessage());
            return false;
        }
    }

    protected void exportToZipFile(IResource exportResource) throws Exception {
        new DsExportOperation(Lists.newArrayList((IFile) exportResource), new FileOutputStream(getDestinationValue())).run(null);
    }

    protected void deployToServer(IResource exportResource) throws Exception {
        new DataSourceDeployOperation(Lists.newArrayList((IFile) exportResource)).run(null);
    }

    private static class DsExportOperation implements IRunnableWithProgress {
        
        protected final OutputStream outputStream;
        protected final List<IFile> resourcesToExport;

        public DsExportOperation(List<IFile> resourcesToExport, OutputStream outputStream) {
            this.outputStream = outputStream;
            this.resourcesToExport = resourcesToExport;
        }

        protected void exportResource(FileExporter exporter, IFile fileResource, IProgressMonitor progressMonitor)
                throws IOException, CoreException {
            if (!fileResource.isSynchronized(IResource.DEPTH_ONE)) {
                fileResource.refreshLocal(IResource.DEPTH_ONE, null);
            }
            if (!fileResource.isAccessible()) {
                return;
            }
            String destinationName = fileResource.getName();
            exporter.write(fileResource, destinationName);
        }

        protected void exportResources(IProgressMonitor progressMonitor) throws InvocationTargetException {
            try {
                FileExporter exporter = new FileExporter(outputStream);
                for (IFile resource : resourcesToExport) {
                    exportResource(exporter, resource, progressMonitor);
                }
                exporter.finished();
                outputStream.flush();
            } catch (Exception e) {
                throw new InvocationTargetException(e);
            }
        }

        @Override
        public void run(IProgressMonitor progressMonitor) throws InvocationTargetException, InterruptedException {
            exportResources(progressMonitor);
        }
    
    }

    private class DataSourceDeployOperation extends DsExportOperation {
        
        public DataSourceDeployOperation(List<IFile> resourcesToExport) {
            super(resourcesToExport, new ByteArrayOutputStream());
        }

        @Override
        public void run(final IProgressMonitor progressMonitor) throws InvocationTargetException, InterruptedException {
            exportResources(progressMonitor);
            final ByteArrayOutputStream baos = (ByteArrayOutputStream) outputStream;
            try {
                Display.getDefault().syncExec((Runnable) () -> WfeServerConnector.getInstance().deployDataSourceArchive(baos.toByteArray()));
            } catch (Exception e) {
                throw new InvocationTargetException(e);
            }
        }
    }

}
