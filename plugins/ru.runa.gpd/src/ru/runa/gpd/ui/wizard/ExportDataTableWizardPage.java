package ru.runa.gpd.ui.wizard;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
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
import ru.runa.gpd.util.WizardPageUtils;
import ru.runa.gpd.util.files.FileExporter;
import ru.runa.wfe.InternalApplicationException;

public class ExportDataTableWizardPage extends ExportWizardPage {
    private final Map<String, IResource> dataTableNameFileMap;
    private ListViewer dataTableListViewer;
    protected final IResource exportResource;
    private Button exportToServerButton;
    private Button exportToProcessButton;

    private static final Set<String> FORBIDDEN_VARIABLE_FORMATS = ImmutableSet.of("ru.runa.wfe.var.format.HiddenFormat",
            "ru.runa.wfe.var.format.ListFormat",
            "ru.runa.wfe.var.format.MapFormat", "ru.runa.wfe.var.format.FileFormat", "usertype");

    protected ExportDataTableWizardPage(IStructuredSelection selection) {
        super(ExportDataTableWizardPage.class);
        setTitle(Localization.getString("ExportDataTableWizard.wizard.title"));
        setDescription(Localization.getString("ExportDataTableWizardPage.page.description"));
        dataTableNameFileMap = DataTableCache.getAllFiles().stream().collect(Collectors.toMap(f -> IOUtils.getWithoutExtension(f.getName()), f -> f));
        exportResource = WizardPageUtils.getInitialElement(selection);
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
        new WfeServerConnectorComposite(exportGroup, WfeServerProcessDefinitionImporter.getInstance(), null);
        new Label(parent, SWT.NONE); // vertical spacer
        exportToProcessButton = new Button(exportGroup, SWT.RADIO);
        exportToProcessButton.setText(Localization.getString("button.exportToProcess"));
        setControl(pageControl);
        if (exportResource != null) {
            dataTableListViewer.setSelection(new StructuredSelection(IOUtils.getWithoutExtension(exportResource.getName())));
        }
    }

    @Override
    protected void onBrowseButtonSelected() {
        FileDialog dialog = new FileDialog(getContainer().getShell(), SWT.SAVE);
        String selectionName = getSelection();
        WizardPageUtils.onBrowseButtonSelected(dialog, selectionName, () -> IOUtils.getWithoutExtension(exportResource.getName()),
                getDestinationValue(), () -> setErrorMessage("ExportDataTableWizardPage.error.empty.source.selection"), this::setDestinationValue);
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
        boolean exportToProcess = exportToProcessButton.getSelection();
        String selected = getSelection();
        if (selected == null) {
            setErrorMessage("ExportDataTableWizardPage.error.empty.source.selection");
            return false;
        }
        if (!WfeServerConnector.getInstance().isConfigured()) {
            setErrorMessage(Localization.getString("error.selectValidConnection"));
            return false;
        }
        IResource exportResource = dataTableNameFileMap.get(selected);
        try {
            exportResource.refreshLocal(IResource.DEPTH_ONE, null);
            if (exportToProcess) {
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

    private static class DataTableExportOperation implements IRunnableWithProgress {
        protected final OutputStream outputStream;
        protected final IFile resource;

        public DataTableExportOperation(IFile resource, OutputStream outputStream) {
            this.outputStream = outputStream;
            this.resource = resource;
        }

        protected void exportResource(FileExporter exporter, IFile fileResource) throws IOException, CoreException {
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
                FileExporter exporter = new FileExporter(outputStream);
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

    private static class DataTableDeployOperation extends DataTableExportOperation {
        
        public DataTableDeployOperation(IFile resource) {
            super(resource, new ByteArrayOutputStream());
        }

        @Override
        public void run(final IProgressMonitor progressMonitor) throws InvocationTargetException, InterruptedException {
            exportResources();
            final ByteArrayOutputStream baos = (ByteArrayOutputStream) outputStream;
            try {
                Display display = Display.getDefault();
                // default handler doesn't rethrow any exception thus we set custom one
                display.setRuntimeExceptionHandler(e -> {
                    throw new InternalApplicationException(e);
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

}
