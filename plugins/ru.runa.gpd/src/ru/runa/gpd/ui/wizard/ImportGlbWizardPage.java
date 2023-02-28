package ru.runa.gpd.ui.wizard;

import org.eclipse.jface.viewers.IStructuredSelection;
import ru.runa.gpd.Localization;
import ru.runa.gpd.lang.model.GlobalSectionDefinition;

public class ImportGlbWizardPage extends ImportParWizardPage {

    public ImportGlbWizardPage(String pageName, IStructuredSelection selection) {
        super(ImportGlbWizardPage.class, selection);
        setTitle(Localization.getString("ImportGlbWizardPage.page.title"));
        setDescription(Localization.getString("ImportGlbWizardPage.page.description"));
    }
    @Override
    public String fileExtension() {
    	return GlobalSectionDefinition.FILE_EXTENSION;
    }
    
}
