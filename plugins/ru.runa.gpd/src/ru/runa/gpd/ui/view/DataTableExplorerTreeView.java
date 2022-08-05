package ru.runa.gpd.ui.view;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import ru.runa.gpd.DataTableNature;
import ru.runa.gpd.Localization;
import ru.runa.gpd.SharedImages;
import ru.runa.gpd.editor.DataTableEditor;
import ru.runa.gpd.ui.custom.LoggingDoubleClickAdapter;
import ru.runa.gpd.util.UiUtil;
import ru.runa.gpd.util.WorkspaceOperations;
import ru.runa.wfe.InternalApplicationException;


public class DataTableExplorerTreeView extends ViewPart implements ISelectionListener {
    private static final String PROJECT_NAME = "DataTables";
    private TreeViewer viewer;

    @Override
    public void init(IViewSite site) throws PartInitException {
        super.init(site);
        getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(this);
        
        try {
            ru.runa.gpd.PluginLogger.logInfo("try create data!");
            IProject dtProject = ResourcesPlugin.getWorkspace().getRoot().getProject(PROJECT_NAME);
            if (!dtProject.exists()) {
                IProjectDescription description = ResourcesPlugin.getWorkspace().newProjectDescription(dtProject.getName());
                description.setNatureIds(new String[] { DataTableNature.NATURE_ID });
                dtProject.create(description, null);
                dtProject.open(IResource.BACKGROUND_REFRESH, null);
                dtProject.refreshLocal(IResource.DEPTH_ONE, null);
            }
        } catch (CoreException ex) {
            throw new InternalApplicationException(ex);
        }
    }

    @Override
    public void dispose() {
        getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(this);
        super.dispose();
    }

    @Override
    public void createPartControl(Composite parent) {
        UiUtil.hideToolBar(getViewSite());
        viewer = new TreeViewer(parent, SWT.NONE);
        viewer.setContentProvider(new DataTableTreeContentProvider());
        viewer.setLabelProvider(new DataTableResourcesLabelProvider());
        viewer.setInput(new Object());
        ResourcesPlugin.getWorkspace().addResourceChangeListener(event -> {
            if (event.getType() != IResourceChangeEvent.POST_CHANGE) {
                return;
            }
            IResourceDelta rootDelta = event.getDelta();
            IResourceDelta projectDelta = rootDelta.findMember(new Path(PROJECT_NAME));
            if (projectDelta == null) {
                return;
            }
            final List<IResource> changedResources = new ArrayList<>();
            IResourceDeltaVisitor visitor = delta -> {
                IResource resource = delta.getResource();
                if (resource.getType() == IResource.FILE) {
                    changedResources.add(resource);
                }
                return true;
            };

            try {
                projectDelta.accept(visitor);
                PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
                    if (!viewer.getControl().isDisposed()) {
                        viewer.refresh();
                        viewer.setSelection(new StructuredSelection(changedResources.get(0)));
                    }
                });
            } catch (Exception disposed) {
            }
        });
        viewer.addDoubleClickListener(new LoggingDoubleClickAdapter() {
            @Override
            protected void onDoubleClick(DoubleClickEvent event) {
                Object element = ((IStructuredSelection) event.getSelection()).getFirstElement();
                if (element instanceof IFile) {
                    WorkspaceOperations.openDataTable((IFile) element);
                }
            }
        });
        getSite().setSelectionProvider(viewer);
        MenuManager menuManager = new MenuManager();
        Menu menu = menuManager.createContextMenu(viewer.getControl());
        menuManager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
        menuManager.setRemoveAllWhenShown(true);
        menuManager.addMenuListener(DataTableExplorerTreeView.this::fillContextMenu);
        viewer.getControl().setMenu(menu);
    }

    @SuppressWarnings("unchecked")
    protected void fillContextMenu(IMenuManager manager) {
        final IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
        final Object selectedObject = selection.getFirstElement();
        final List<IResource> resources = selection.toList();
        final boolean isDataTableSelected = selectedObject instanceof IFile;
        manager.add(new Action(Localization.getString("DTExplorerTreeView.menu.label.addDT"), SharedImages.getImageDescriptor("icons/add_obj.gif")) {
            @Override
            public void run() {
                WorkspaceOperations.createDataTable(selection);
            }
        });
        manager.add(
                new Action(Localization.getString("DTExplorerTreeView.menu.label.importDT"), SharedImages.getImageDescriptor("icons/import_dt.gif")) {
                    @Override
                    public void run() {
                        WorkspaceOperations.importDataTable(selection);
                    }
                });

        if (isDataTableSelected) {
            manager.add(new Action(Localization.getString("DTExplorerTreeView.menu.label.renameDT"),
                    SharedImages.getImageDescriptor("icons/rename.gif")) {
                @Override
                public void run() {
                    WorkspaceOperations.renameDataTable(selection);
                }
            });
            manager.add(
                    new Action(Localization.getString("DTExplorerTreeView.menu.label.copyDT"), SharedImages.getImageDescriptor("icons/copy.gif")) {
                        @Override
                        public void run() {
                            WorkspaceOperations.copyDataTable(selection);
                        }
                    });
            manager.add(new Action(Localization.getString("DTExplorerTreeView.menu.label.exportDT"),
                    SharedImages.getImageDescriptor("icons/export_dt.gif")) {
                @Override
                public void run() {
                    WorkspaceOperations.exportDataTable(selection);
                }
            });
        }

        if (!selection.isEmpty()) {
            manager.add(
                    new Action(Localization.getString("ExplorerTreeView.menu.label.refresh"), SharedImages.getImageDescriptor("icons/refresh.gif")) {
                        @Override
                        public void run() {
                            WorkspaceOperations.refreshResources(resources);
                        }
            });
            manager.add(new Action(Localization.getString("button.delete"), SharedImages.getImageDescriptor("icons/delete.gif")) {
                @Override
                public void run() {
                    WorkspaceOperations.deleteDataTable(resources);
                }
            });
        }
    }

    @Override
    public void setFocus() {
    }

    @Override
    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        if (part instanceof DataTableEditor) {
            IFile dataTableFile = ((DataTableEditor) part).getDataTableFile();
            viewer.setSelection(new StructuredSelection(dataTableFile), true);
        }
    }
}
