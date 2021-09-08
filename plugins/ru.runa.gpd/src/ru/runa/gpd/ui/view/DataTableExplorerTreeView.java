package ru.runa.gpd.ui.view;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
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
import ru.runa.gpd.ui.custom.LoggingDoubleClickAdapter;
import ru.runa.gpd.util.UiUtil;
import ru.runa.gpd.util.WorkspaceOperations;
import ru.runa.wfe.InternalApplicationException;

public class DataTableExplorerTreeView extends ViewPart implements ISelectionListener {
    private TreeViewer viewer;

    @Override
    public void init(IViewSite site) throws PartInitException {
        super.init(site);
        getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(this);
        
        try {
            IProject dtProject = ResourcesPlugin.getWorkspace().getRoot().getProject("DataTables");
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
        viewer.setLabelProvider(new DataTablesResourcesLabelProvider());
        viewer.setInput(new Object());
        ResourcesPlugin.getWorkspace().addResourceChangeListener(new IResourceChangeListener() {
            @Override
            public void resourceChanged(IResourceChangeEvent event) {
                try {
                    PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            if (!viewer.getControl().isDisposed()) {
                                viewer.refresh();
                            }
                        }
                    });
                } catch (Exception e) {
                    // disposed
                }
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
        MenuManager menuMgr = new MenuManager();
        Menu menu = menuMgr.createContextMenu(viewer.getControl());
        menuMgr.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(IMenuManager manager) {
                DataTableExplorerTreeView.this.fillContextMenu(manager);
            }
        });
        viewer.getControl().setMenu(menu);
    }

    @SuppressWarnings("unchecked")
    protected void fillContextMenu(IMenuManager manager) {
        final IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
        final Object selectedObject = selection.getFirstElement();
        final List<IResource> resources = selection.toList();
        final boolean dtSelected = selectedObject instanceof IFile;
        manager.add(new Action(Localization.getString("DTExplorerTreeView.menu.label.addDT"), SharedImages.getImageDescriptor("icons/add_obj.gif")) {
            @Override
            public void run() {
                WorkspaceOperations.createDataTable(selection);
            }
        });

        if (dtSelected) {
            manager.add(
                    new Action(Localization.getString("DTExplorerTreeView.menu.label.renameDT"),
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
                    WorkspaceOperations.deleteDataTable(selection);
                }
            });
        }
    }

    @Override
    public void setFocus() {
    }

    @Override
    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
    }
}
