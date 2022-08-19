package ru.runa.gpd.ui.action;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.part.FileEditorInput;
import ru.runa.gpd.BotStationNature;
import ru.runa.gpd.DataSourcesNature;
import ru.runa.gpd.DataTableNature;
import ru.runa.gpd.PluginLogger;
import ru.runa.gpd.editor.BotTaskEditor;
import ru.runa.gpd.editor.ProcessEditorBase;

public abstract class BaseActionDelegate implements IWorkbenchWindowActionDelegate {
    protected IWorkbenchWindow window;

    @Override
    public void init(IWorkbenchWindow window) {
        this.window = window;
    }

    @Override
    public void dispose() {
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
    }

    protected IEditorPart getActiveEditor() {
        return window.getActivePage().getActiveEditor();
    }

    protected List<ProcessEditorBase> getOpenedDesignerEditors() {
        List<ProcessEditorBase> editors = new ArrayList<ProcessEditorBase>();
        IEditorReference[] editorReferences = window.getActivePage().getEditorReferences();
        for (IEditorReference editorReference : editorReferences) {
            IEditorPart editor = editorReference.getEditor(true);
            if (editor instanceof ProcessEditorBase) {
                editors.add((ProcessEditorBase) editor);
            }
        }
        return editors;
    }

    protected ProcessEditorBase getActiveDesignerEditor() {
        IEditorPart editor = getActiveEditor();
        if (editor instanceof ProcessEditorBase) {
            return (ProcessEditorBase) editor;
        }
        return null;
    }

    protected IStructuredSelection getStructuredSelection() {
        ISelection selection = window.getSelectionService().getSelection();
        if (selection instanceof IStructuredSelection) {
            return (IStructuredSelection) selection;
        }
        IEditorPart editorPart = getActiveEditor();
        if (editorPart != null) {
            IEditorInput editorInput = editorPart.getEditorInput();
            if (editorInput instanceof FileEditorInput) {
                return new StructuredSelection(((FileEditorInput) editorInput).getFile());
            }
        }
        return null;
    }

    protected boolean isBotStructuredSelection() {
        return isStructuredSelection(BotStationNature.NATURE_ID, getActiveEditor() instanceof BotTaskEditor);
    }

    protected boolean isDataSourceStructuredSelection() {
        return isStructuredSelection(DataSourcesNature.NATURE_ID, false);
    }

    protected boolean isDataTableStructuredSelection() {
        return isStructuredSelection(DataTableNature.NATURE_ID, false);
    }

    private boolean isStructuredSelection(String natureId, boolean defaultValue) {
        IStructuredSelection selection = getStructuredSelection();
        if (selection != null && selection.getFirstElement() instanceof IResource) {
            IResource resource = (IResource) selection.getFirstElement();
            try {
                return resource.exists() && resource.getProject().getNature(natureId) != null;
            } catch (CoreException e) {
                PluginLogger.logError(e);
            }
        }
        return defaultValue;
    }
}
