package ru.runa.gpd.ui.wizard;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

import ru.runa.gpd.Localization;
import ru.runa.gpd.PluginLogger;
import ru.runa.gpd.sync.WfeServerConnector;
import ru.runa.gpd.sync.WfeServerConnectorComposite;
import ru.runa.gpd.sync.WfeServerProcessDefinitionImporter;
import ru.runa.gpd.ui.custom.LoggingSelectionAdapter;
import ru.runa.gpd.util.DataTableUtils;
import ru.runa.gpd.util.IOUtils;

public class ExportDataTableWizardPage extends ExportWizardPage {
    private final Map<String, IFile> dataTableNameFileMap;
    private ListViewer dataTableListViewer;
    protected final IResource exportResource;
    private Button exportToFileButton;
    private Button exportToServerButton;
    private WfeServerConnectorComposite serverConnectorComposite;

    protected ExportDataTableWizardPage(IStructuredSelection selection) {
        super(ExportDataTableWizardPage.class);
        setTitle(Localization.getString("ExportDataTableWizard.wizard.title"));
        setDescription(Localization.getString("ExportDataTableWizardPage.page.description"));
        dataTableNameFileMap = new TreeMap<String, IFile>();
        for (IFile dataTableFile : DataTableUtils.getAllDataTables()) {
            dataTableNameFileMap.put(IOUtils.getWithoutExtension(dataTableFile.getName()), dataTableFile);
        }
        exportResource = getInitialElement(selection);
    }

    private IResource getInitialElement(IStructuredSelection selection) {
        if (selection != null && !selection.isEmpty()) {
            Object selectedElement = selection.getFirstElement();
            if (selectedElement instanceof IAdaptable) {
                IAdaptable adaptable = (IAdaptable) selectedElement;
                IResource resource = adaptable.getAdapter(IResource.class);
                return resource;
            }
        }
        return null;
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
        processListGroup.setText(Localization.getString("label.view.dataTableDesignerExplorer"));
        createViewer(processListGroup);
        Group exportGroup = new Group(sashForm, SWT.NONE);
        exportGroup.setLayout(new GridLayout(1, false));
        exportGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
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
            dataTableListViewer.setSelection(new StructuredSelection(IOUtils.getWithoutExtension(exportResource.getName())));
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
        String selectionName = getSelection();
        if (selectionName != null) {
            dialog.setFileName(IOUtils.getWithoutExtension(exportResource.getName()));
        }
        String currentSourceString = getDestinationValue();
        int lastSeparatorIndex = currentSourceString.lastIndexOf(File.separator);
        if (lastSeparatorIndex != -1) {
            dialog.setFilterPath(currentSourceString.substring(0, lastSeparatorIndex));
        }
        String selectedFileName = dialog.open();
        if (selectedFileName != null) {
            setErrorMessage(null);
            setDestinationValue(selectedFileName);
        }
    }

    private void createViewer(Composite parent) {
        dataTableListViewer = new ListViewer(parent, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
        dataTableListViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
        dataTableListViewer.setContentProvider(new ArrayContentProvider());
        dataTableListViewer.setInput(dataTableNameFileMap.keySet());
        dataTableListViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                setPageComplete(!event.getSelection().isEmpty());
            }
        });
    }

    private String getSelection() {
        return (String) ((IStructuredSelection) dataTableListViewer.getSelection()).getFirstElement();
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
        IResource exportResource = dataTableNameFileMap.get(selected);
        try {
            exportResource.refreshLocal(IResource.DEPTH_ONE, null);
            if (exportToFile) {
                exportToZipFile(exportResource);
            } else {
                deployToServer(exportResource);
            }
            return true;
        } catch (Throwable th) {
            PluginLogger.logErrorWithoutDialog("ExportDataTableWizardPage.error.export", th);
            setErrorMessage(Throwables.getRootCause(th).getMessage());
            return false;
        }
    }

    protected void exportToZipFile(IResource exportResource) throws Exception {
        new DataTableExportOperation(Lists.newArrayList((IFile) exportResource), new FileOutputStream(getDestinationValue())).run(null);
    }

    protected void deployToServer(IResource exportResource) throws Exception {
        new DataTableDeployOperation(Lists.newArrayList((IFile) exportResource)).run(null);
    }

    private static class DataTableExportOperation implements IRunnableWithProgress {
        
        protected final OutputStream outputStream;
        protected final List<IFile> resourcesToExport;

        public DataTableExportOperation(List<IFile> resourcesToExport, OutputStream outputStream) {
            this.outputStream = outputStream;
            this.resourcesToExport = resourcesToExport;
        }

        protected void exportResource(DataTableFileExporter exporter, IFile fileResource, IProgressMonitor progressMonitor)
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
                DataTableFileExporter exporter = new DataTableFileExporter(outputStream);
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

    private class DataTableDeployOperation extends DataTableExportOperation {
        
        public DataTableDeployOperation(List<IFile> resourcesToExport) {
            super(resourcesToExport, new ByteArrayOutputStream());
        }

        @Override
        public void run(final IProgressMonitor progressMonitor) throws InvocationTargetException, InterruptedException {
            exportResources(progressMonitor);
            final ByteArrayOutputStream baos = (ByteArrayOutputStream) outputStream;
            try {
                Display.getDefault().syncExec(new Runnable() {
                    @Override
                    public void run() {
                        WfeServerConnector.getInstance().deployDataTable(baos.toByteArray());
                    }
                });
            } catch (Exception e) {
                throw new InvocationTargetException(e);
            }
        }
    }

    private static class DataTableFileExporter {
        
        private final ZipOutputStream outputStream;

        public DataTableFileExporter(OutputStream outputStream) throws IOException {
            this.outputStream = new ZipOutputStream(outputStream, Charsets.UTF_8);
        }

        public void finished() throws IOException {
            outputStream.close();
        }

        private void write(ZipEntry entry, IFile contents) throws IOException, CoreException {
            byte[] readBuffer = new byte[1024];
            outputStream.putNextEntry(entry);
            InputStream contentStream = contents.getContents();
            try {
                int n;
                while ((n = contentStream.read(readBuffer)) > 0) {
                    outputStream.write(readBuffer, 0, n);
                }
            } finally {
                if (contentStream != null) {
                    contentStream.close();
                }
            }
            outputStream.closeEntry();
        }

        public void write(IFile resource, String destinationPath) throws IOException, CoreException {
            ZipEntry newEntry = new ZipEntry(destinationPath);
            write(newEntry, resource);
        }

    }

}
