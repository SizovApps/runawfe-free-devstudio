package ru.runa.gpd.ui.view;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import ru.runa.gpd.DataTablesNature;

public class DataTablesTreeContentProvider implements ITreeContentProvider {

    @Override
    public Object[] getChildren(Object element) {
        return null;
    }

    @Override
    public Object getParent(Object element) {
        return null;
    }

    @Override
    public boolean hasChildren(Object element) {
        return false;
    }

    @Override
    public Object[] getElements(Object inputElement) {
        List<Object> returnList = new ArrayList<Object>();
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        try {
            for (IResource resource : workspace.getRoot().members()) {
                if (resource instanceof IProject && ((IProject) resource).getNature(DataTablesNature.NATURE_ID) != null) {
                    for (IResource dtResource : ((IProject) resource).members()) {
                        if (dtResource instanceof IFile && ((IFile) dtResource).getFileExtension().equalsIgnoreCase("xml")) {
                            returnList.add(dtResource);
                        }
                    }
                }
            }
            return returnList.toArray();
        } catch (CoreException e) {
            return new Object[] {};
        }
    }

    @Override
    public void dispose() {
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

}
