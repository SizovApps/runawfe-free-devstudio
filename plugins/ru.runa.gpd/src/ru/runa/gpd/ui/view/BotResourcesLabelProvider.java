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
        ru.runa.gpd.PluginLogger.logInfo("Start getText label!!!");
        if (element instanceof IFile) {
            if (((IFile) element).getName().contains("process")) {
                ProcessDefinition definition = ProcessCache.getProcessDefinition((IFile) element);
                ru.runa.gpd.PluginLogger.logInfo("Process name: " + definition.getName());
                if (definition != null) {
                    return definition.getName().substring(1);
                }
                return ((IFile) element).getName();
            }
        }
        if (element instanceof IResource) {
            ru.runa.gpd.PluginLogger.logInfo("Resource name: " + ((IResource) element).getName());
            return ((IResource) element).getName();
        }
        return super.getText(element);
    }

    @Override
    public Image getImage(Object element) {
        ru.runa.gpd.PluginLogger.logInfo("Start getImage label!!!");
        if (element instanceof IProject) {
            return SharedImages.getImage("icons/bot_station.gif");
        }
        if (element instanceof IFolder) {
            ru.runa.gpd.PluginLogger.logInfo("Folder name: " + ((IFolder) element).getName());
            return SharedImages.getImage("icons/bot.gif");
        }
        if (element instanceof IFile) {
            ru.runa.gpd.PluginLogger.logInfo("File name: " + ((IFile) element).getName());
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
