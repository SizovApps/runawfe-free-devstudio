package ru.runa.gpd.ui.view;

import java.util.Arrays;
import org.eclipse.core.resources.IFile;
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
        try {
            return Arrays.stream(DataTableUtils.getDataTableProject().members())
                    .filter(r -> r instanceof IFile && r.getName().endsWith(DataTableUtils.FILE_EXTENSION)).toArray();
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
