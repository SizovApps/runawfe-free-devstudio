package plugins.ru.runa.gpd.src.ru.runa.gpd.ui.wizard;

import ru.runa.gpd.lang.model.ProcessDefinition;
import ru.runa.gpd.ui.wizard.NewGlobalSectionDefinitionWizardPage;
import org.eclipse.jface.viewers.IStructuredSelection;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class NewGlobalSectionDefinitionWizardPageFromBot extends NewGlobalSectionDefinitionWizardPage {
    public NewGlobalSectionDefinitionWizardPageFromBot(IStructuredSelection selection, ProcessDefinition parentProcessDefinition) {
        super(selection, parentProcessDefinition);
    }

    private void createProjectField(Composite parent) {
        Label label = new Label(parent, SWT.NONE);
        label.setText("Select bot: ");
        projectCombo = new Combo(parent, SWT.SINGLE | SWT.READ_ONLY | SWT.BORDER);
        for (IContainer container : processContainers) {
                boolean folderContainGlobalSection = false;
                for (String botName : ru.runa.gpd.BotCache.getAllBotNames()) {
                    projectCombo.add(botName);
                }
        }
        projectCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        if (initialSelection != null) {
            projectCombo.setText(ru.runa.gpd.util.IOUtils.getProcessContainerName(initialSelection));
        }
        projectCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                verifyContentsValid();
            }
        });
        if (parentProcessDefinition != null) {
            projectCombo.setText(parentProcessDefinition.getName());
            projectCombo.setEnabled(false);
        }
    }
}
