package ru.runa.gpd.ui.view;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import ru.runa.gpd.BotCache;
import ru.runa.gpd.SharedImages;
import ru.runa.gpd.lang.model.BotTask;
import ru.runa.gpd.lang.model.BotTaskType;
import ru.runa.gpd.lang.model.ProcessDefinition;
import ru.runa.gpd.ProcessCache;

public class BotResourcesLabelProvider extends LabelProvider {

    @Override
    public String getText(Object element) {
        if (element instanceof IFile) {
            if (((IFile) element).getName().contains("process")) {
                ProcessDefinition definition = ProcessCache.getProcessDefinition((IFile) element);
                if (definition != null) {
                    return definition.getName();
                } else {
                    return ((IFile) element).getName();
                }
            } else {
                return ((IFile) element).getName();
            }
        }
        else if (element instanceof IResource) {
            return ((IResource) element).getName();
        }
        return super.getText(element);
    }

    @Override
    public Image getImage(Object element) {
        if (element instanceof IProject) {
            return SharedImages.getImage("icons/bot_station.gif");
        }
        if (element instanceof IFolder) {
            return SharedImages.getImage("icons/bot.gif");
        }
        if (element instanceof IFile) {
            if (((IFile) element).getName().contains("process")) {
                return SharedImages.getImage("icons/glb.gif");
            }
            BotTask task = BotCache.getBotTask((IFile) element);
            if (task == null) {
                return null;
            }
            if (task.getType() != BotTaskType.SIMPLE) {
                return SharedImages.getImage("icons/bot_task_formal.gif");
            }
        }
        return SharedImages.getImage("icons/bot_task.gif");
    }
}
