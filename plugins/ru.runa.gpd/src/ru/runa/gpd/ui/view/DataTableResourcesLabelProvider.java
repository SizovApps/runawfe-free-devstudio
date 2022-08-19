package ru.runa.gpd.ui.view;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import ru.runa.gpd.SharedImages;
import ru.runa.gpd.util.IOUtils;

public class DataTableResourcesLabelProvider extends LabelProvider {

    @Override
    public String getText(Object element) {
        return element instanceof IResource ? IOUtils.getWithoutExtension(((IResource) element).getName()) : super.getText(element);
    }

    @Override
    public Image getImage(Object element) {
        return SharedImages.getImage("icons/data_tables.png");
    }

}
