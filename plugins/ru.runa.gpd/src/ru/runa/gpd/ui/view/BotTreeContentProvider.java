package ru.runa.gpd.ui.view;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import ru.runa.gpd.BotStationNature;
import ru.runa.gpd.util.IOUtils;

public class BotTreeContentProvider implements ITreeContentProvider {
    @Override
    public Object[] getChildren(Object parentElement) {
        ru.runa.gpd.PluginLogger.logInfo("Start getChildren content!!! " + parentElement.toString());
        if (parentElement instanceof IProject) {
            List<IFolder> botFolders = IOUtils.getBotFolders((IProject) parentElement);
            return botFolders.toArray();
        }
        else if (parentElement instanceof IFolder) {
            ru.runa.gpd.PluginLogger.logInfo("Enter parentFolder: " + ((IFolder)parentElement).getName());
            List<IFile> files = IOUtils.getBotTaskFiles((IFolder) parentElement);
            List<IFile> botFiles = IOUtils.getProcessDefinitionFiles((IFolder) parentElement);
            for (IFile f : botFiles) {
                ru.runa.gpd.PluginLogger.logInfo("Get proc files: " + f.getName());
            }
            files.addAll(botFiles);
            return files.toArray();
        }
        return null;
    }


    @Override
    public Object getParent(Object element) {
        return null;
    }

    @Override
    public boolean hasChildren(Object element) {
        if (element instanceof IProject) {
            return IOUtils.getBotFolders((IProject) element).size() > 0;
        } else if (element instanceof IFolder) {
            return IOUtils.getBotTaskFiles((IFolder) element).size() > 0;
        }
        return false;
    }

    @Override
    public Object[] getElements(Object inputElement) {
        ru.runa.gpd.PluginLogger.logInfo("Start getElements content!!!");
        List<Object> returnList = new ArrayList<Object>();
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        try {
            for (IResource resource : workspace.getRoot().members()) {
                ru.runa.gpd.PluginLogger.logInfo("Resource: " + resource.getName());
                if (resource instanceof IProject && ((IProject) resource).getNature(BotStationNature.NATURE_ID) != null) {
                    returnList.add(resource);
                }
            }
            return returnList.toArray();
        } catch (CoreException e) {
            return new Object[] {};
        }

//        IOUtils.getProcessDefinitionFiles()
    }

    @Override
    public void dispose() {
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }
}
