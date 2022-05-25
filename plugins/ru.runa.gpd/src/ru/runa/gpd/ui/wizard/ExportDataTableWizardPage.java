package ru.runa.gpd.ui.wizard;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Set;
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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
import ru.runa.gpd.DataTableCache;
import ru.runa.gpd.Localization;
import ru.runa.gpd.PluginLogger;
import ru.runa.gpd.lang.model.VariableUserType;
import ru.runa.gpd.sync.WfeServerConnector;
import ru.runa.gpd.sync.WfeServerConnectorComposite;
import ru.runa.gpd.sync.WfeServerProcessDefinitionImporter;
import ru.runa.gpd.ui.custom.Dialogs;
import ru.runa.gpd.util.IOUtils;
import ru.runa.wfe.InternalApplicationException;

public class ExportDataTableWizardPage extends ExportWizardPage {
    private final Map<String, IFile> dataTableNameFileMap;
    private ListViewer dataTableListViewer;
    protected final IResource exportResource;
    private Button exportToFileButton;
    private Button exportToServerButton;
    private Button exportToProcessButton;
    private WfeServerConnectorComposite serverConnectorComposite;

    private static final Set<String> FORBIDDEN_VARIABLE_FORMATS = ImmutableSet.of("ru.runa.wfe.var.format.HiddenFormat",
            "ru.runa.wfe.var.format.ListFormat",
            "ru.runa.wfe.var.format.MapFormat", "ru.runa.wfe.var.format.FileFormat", "usertype");

    protected ExportDataTableWizardPage(IStructuredSelection selection) {
        super(ExportDataTableWizardPage.class);
        setTitle(Localization.getString("ExportDataTableWizard.wizard.title"));
        setDescription(Localization.getString("ExportDataTableWizardPage.page.description"));
        dataTableNameFileMap = new TreeMap<>();
        for (IFile dataTableFile : DataTableCache.getAllFiles()) {
            dataTableNameFileMap.put(IOUtils.getWithoutExtension(dataTableFile.getName()), dataTableFile);
        }
        exportResource = getInitialElement(selection);
    }

    private IResource getInitialElement(IStructuredSelection selection) {
        return selection != null && !selection.isEmpty() && selection.getFirstElement() instanceof IAdaptable
                ? (IResource) ((IAdaptable) selection.getFirstElement()).getAdapter(IResource.class)
                : null;
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
        exportToServerButton = new Button(exportGroup, SWT.RADIO);
        exportToServerButton.setText(Localization.getString("button.exportToServer"));
        exportToServerButton.setSelection(true);
        serverConnectorComposite = new WfeServerConnectorComposite(exportGroup, WfeServerProcessDefinitionImporter.getInstance(), null);
        new Label(parent, SWT.NONE); // vertical spacer
        exportToProcessButton = new Button(exportGroup, SWT.RADIO);
        exportToProcessButton.setText(Localization.getString("button.exportToProcess"));
        setControl(pageControl);
        if (exportResource != null) {
            dataTableListViewer.setSelection(new StructuredSelection(IOUtils.getWithoutExtension(exportResource.getName())));
        }
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
            setErrorMessage("ExportDataTableWizardPage.error.empty.source.selection");
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
        boolean exportToFile = false;
        boolean exportToProcess = exportToProcessButton.getSelection();
        String selected = getSelection();
        if (selected == null) {
            setErrorMessage("ExportDataTableWizardPage.error.empty.source.selection");
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
                exportToFile(exportResource);
            } else if (exportToProcess) {
                exportToProcess(exportResource);
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

    protected void exportToFile(IResource exportResource) throws Exception {
        new DataTableExportOperation((IFile) exportResource, new FileOutputStream(getDestinationValue())).run(null);
    }

    protected void deployToServer(IResource exportResource) throws Exception {
        VariableUserType dataTable = DataTableCache.getDataTable(IOUtils.getWithoutExtension(exportResource.getName()));
        boolean hasUnsupportedFormats = dataTable.getAttributes().stream()
                .anyMatch(var -> FORBIDDEN_VARIABLE_FORMATS.contains(var.getFormat().split("[(:]")[0]));
        if (hasUnsupportedFormats) {
            Dialogs.information(Localization.getString("ExportDataTableWizardPage.dialog.error.forbidden.format"));
        } else if (dataTable.getAttributes().isEmpty()) {
            Dialogs.information(Localization.getString("ExportDataTableWizardPage.dialog.error.empty.variables"));
        } else {
            new DataTableDeployOperation((IFile) exportResource).run(null);
        }
    }

    protected void exportToProcess(IResource exportResource) throws Exception {
        new DataTableExportToProcessOperation((IFile) exportResource).run(null);
    }

    private class DataTableExportOperation implements IRunnableWithProgress {
        
        protected final OutputStream outputStream;
        protected final IFile resource;

        public DataTableExportOperation(IFile resource, OutputStream outputStream) {
            this.outputStream = outputStream;
            this.resource = resource;
        }

        protected void exportResource(DataTableFileExporter exporter, IFile fileResource)
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

        protected void exportResources() throws InvocationTargetException {
            try {
                DataTableFileExporter exporter = new DataTableFileExporter(outputStream);
                exportResource(exporter, resource);
                exporter.finished();
                outputStream.flush();
            } catch (Exception e) {
                throw new InvocationTargetException(e);
            }
        }

        @Override
        public void run(IProgressMonitor progressMonitor) throws InvocationTargetException, InterruptedException {
            exportResources();
        }
    
    }

    private class DataTableDeployOperation extends DataTableExportOperation {
        
        public DataTableDeployOperation(IFile resource) throws Exception {
            super(resource, new ByteArrayOutputStream());
        }

        @Override
        public void run(final IProgressMonitor progressMonitor) throws InvocationTargetException, InterruptedException {
            exportResources();
            final ByteArrayOutputStream baos = (ByteArrayOutputStream) outputStream;
            try {
                Display display = Display.getDefault();
                // default handler doesn't rethrow any exception thus we set custom one
                display.setRuntimeExceptionHandler(t -> {
                    throw new InternalApplicationException(t);
                });
                display.syncExec(() -> WfeServerConnector.getInstance().deployDataTable(baos.toByteArray()));
            } catch (Exception e) {
                if (e.getMessage().contains("DataTableAlreadyExistsException")) {
                    String dataTableName = IOUtils.getWithoutExtension(resource.getName());
                    throw new RuntimeException(
                            Localization.getString("ExportDataTableWizardPage.error.dataTable.already.exists", dataTableName));
                }
                throw new InvocationTargetException(e);
            }
        }
    }

    private static class DataTableExportToProcessOperation implements IRunnableWithProgress {

        protected final IFile resourceToExport;

        public DataTableExportToProcessOperation(IFile resourceToExport) {
            this.resourceToExport = resourceToExport;
        }
        @Override
        public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
            ExportDataTableToProcessWizard wizard = new ExportDataTableToProcessWizard(resourceToExport);
            wizard.init(PlatformUI.getWorkbench(), null);
            CompactWizardDialog dialog = new CompactWizardDialog(wizard);
            dialog.setMinimumPageSize(850, 200);
            dialog.open();
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
