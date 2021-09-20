package ru.runa.gpd.ui.view;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import ru.runa.gpd.util.DataTableUtils;

public class DataTableTreeContentProvider implements ITreeContentProvider {

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
        IProject dataTablesProject = DataTableUtils.getDataTableProject();
        try {
            for (IResource resource : dataTablesProject.members()) {
                if (resource instanceof IFile && ((IFile) resource).getName().endsWith(DataTableUtils.DATA_TABLE_FILE_EXTENSION)) {
                    returnList.add(resource);
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
