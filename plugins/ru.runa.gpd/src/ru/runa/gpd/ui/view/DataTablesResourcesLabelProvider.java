package ru.runa.gpd.ui.view;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import ru.runa.gpd.SharedImages;


public class DataTablesResourcesLabelProvider extends LabelProvider {

    @Override
    public String getText(Object element) {
        if (element instanceof IResource) {
            String name = ((IResource) element).getName(); 
            return name.substring(0, name.lastIndexOf('.'));
        }
        return super.getText(element);
    }

    @Override
    public Image getImage(Object element) {
        return SharedImages.getImage("icons/data_tables.png");
    }

}
