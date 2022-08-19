package ru.runa.gpd.ui.wizard;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import ru.runa.gpd.Localization;
import ru.runa.gpd.ui.custom.FileNameChecker;

public class NewDataTableWizardPage extends WizardPage {
    private Text nameText;
    private String startName;

    public NewDataTableWizardPage() {
        super(Localization.getString("NewDataTableWizardPage.page.name"));
        setTitle(Localization.getString("NewDataTableWizardPage.page.title"));
        setDescription(Localization.getString("NewDataTableWizardPage.page.description"));
    }

    public NewDataTableWizardPage(String startName) {
        super(Localization.getString("NewDataTableWizardPage.page.name"));
        setTitle(Localization.getString("NewDataTableWizardPage.page.title"));
        setDescription(Localization.getString("NewDataTableWizardPage.page.description"));
        this.startName = startName;
    }

    @Override
    public void createControl(Composite parent) {
        initializeDialogUnits(parent);
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.numColumns = 2;
        composite.setLayout(layout);
        createNameField(composite);
        setControl(composite);
        Dialog.applyDialogFont(composite);
        setPageComplete(false);
        nameText.setFocus();
    }

    private void createNameField(Composite parent) {
        Label label = new Label(parent, SWT.NONE);
        label.setText(Localization.getString("NewDataTableWizardPage.page.name"));
        nameText = new Text(parent, SWT.BORDER);
        if (startName != null && startName.length() > 0) {
            nameText.setText(startName);
        }
        nameText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                verifyContentsValid();
            }
        });
        nameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    }

    private void verifyContentsValid() {
        if (isDataTableNameEmpty()) {
            setErrorMessage(Localization.getString("error.no_datatable_name"));
            setPageComplete(false);
        } else if (!isDataTableNameValid()) {
            setErrorMessage(Localization.getString("error.datatable_name_not_valid"));
            setPageComplete(false);
        } else {
            setErrorMessage(null);
            setPageComplete(true);
        }
    }

    public String getDataTableName() {
        return nameText != null ? nameText.getText().trim() : "";
    }

    private boolean isDataTableNameEmpty() {
        return nameText.getText().length() == 0;
    }

    private boolean isDataTableNameValid() {
        return FileNameChecker.isValid(nameText.getText());
    }
}
