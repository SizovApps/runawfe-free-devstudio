package ru.runa.gpd.ui.wizard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import ru.runa.gpd.Localization;
import ru.runa.gpd.PluginLogger;
import ru.runa.gpd.ProcessCache;
import ru.runa.gpd.DataTableCache;
import ru.runa.gpd.extension.VariableFormatArtifact;
import ru.runa.gpd.extension.VariableFormatRegistry;
import ru.runa.gpd.extension.handler.ParamDef;
import ru.runa.gpd.extension.handler.ParamDefGroup;
import ru.runa.gpd.lang.model.ProcessDefinition;
import ru.runa.gpd.lang.model.VariableUserType;
import ru.runa.gpd.lang.model.BotTask;
import ru.runa.gpd.lang.model.Variable;
import ru.runa.gpd.lang.model.bpmn.ScriptTask;
import ru.runa.wfe.var.format.ListFormat;
import ru.runa.gpd.ui.custom.LoggingSelectionAdapter;
import ru.runa.gpd.ui.enhancement.DialogEnhancementMode;
import ru.runa.gpd.ui.enhancement.DocxDialogEnhancementMode;
import ru.runa.gpd.util.DataTableUtils;
import ru.runa.gpd.util.IOUtils;

public class BotTaskParamDefWizardPage extends WizardPage {
    private final ParamDefGroup paramDefGroup;
    private final ParamDef paramDef;
    private Text nameText;
    private Combo typeCombo;
    private Button useVariableButton;
    private Button requiredButton;
    private final DialogEnhancementMode dialogEnhancementMode;

    public BotTaskParamDefWizardPage(ParamDefGroup paramDefGroup, ParamDef paramDef, DialogEnhancementMode dialogEnhancementMode) {
        super(Localization.getString("ParamDefWizardPage.page.title"));
        setTitle(Localization.getString("ParamDefWizardPage.page.title"));
        setDescription(Localization.getString("ParamDefWizardPage.page.description"));
        this.paramDefGroup = paramDefGroup;
        this.paramDef = paramDef;
        this.dialogEnhancementMode = dialogEnhancementMode;
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
        createVariableTypeField(composite);
        boolean isDocxMode = null != dialogEnhancementMode && dialogEnhancementMode.checkBotDocxTemplateEnhancementMode()
                && ((DocxDialogEnhancementMode) dialogEnhancementMode).checkDocxMode();
        if (!isDocxMode) {
            createUseVariableCheckbox(composite);
        }
        createOptionalCheckbox(composite);
        if (paramDef != null) {
            if (paramDef.getName() != null) {
                nameText.setText(paramDef.getName());
            }
            if (paramDef.getFormatFilters().size() > 0) {
                String type = paramDef.getFormatFilters().get(0);
                String label = VariableFormatRegistry.getInstance().getFilterLabel(type);
                typeCombo.setText(label);
                if (isDocxMode && paramDef.getName().compareTo(DocxDialogEnhancementMode.getInputFileParamName()) == 0
                        && type.compareTo(DocxDialogEnhancementMode.FILE_VARIABLE_FORMAT) == 0) {
                    typeCombo.setEnabled(false);
                }

            }
            if (useVariableButton != null) {
                useVariableButton.setSelection(paramDef.isUseVariable());
            }
            requiredButton.setSelection(!paramDef.isOptional());
        } else {
            typeCombo.setText(VariableFormatRegistry.getInstance().getFilterLabel(String.class.getName()));
            if (useVariableButton != null) {
                useVariableButton.setSelection(true);
            }
            requiredButton.setSelection(true);
        }
        verifyContentsValid();
        setControl(composite);
        Dialog.applyDialogFont(composite);
        if (paramDef == null) {
            setPageComplete(false);
        }
        nameText.setFocus();
    }

    private void createNameField(Composite parent) {
        Label label = new Label(parent, SWT.NONE);
        label.setText(Localization.getString("ParamDefWizardPage.page.name"));
        nameText = new Text(parent, SWT.BORDER);
        nameText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                verifyContentsValid();
            }
        });
        nameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        if (null != dialogEnhancementMode && dialogEnhancementMode.checkBotDocxTemplateEnhancementMode()
                && ((DocxDialogEnhancementMode) dialogEnhancementMode).checkDocxMode()) {
            nameText.setEnabled(false);
        }

    }

    private void createVariableTypeField(Composite parent) {
        Label label = new Label(parent, SWT.NONE);
        label.setText(Localization.getString("ParamDefWizardPage.page.type"));
        List<String> types = new ArrayList<String>();
        if (BotTask.usingBotTask.getDelegationClassName().equals(ScriptTask.INTERNAL_STORAGE_HANDLER_CLASS_NAME)) {
            types = setTypesForInternalStorage();
        } else {
            for (VariableFormatArtifact artifact : VariableFormatRegistry.getInstance().getFilterArtifacts()) {
                types.add(artifact.getLabel());
            }
            try {
                Arrays.stream(DataTableUtils.getDataTableProject().members())
                        .filter(r -> r instanceof IFile && r.getName().endsWith(DataTableUtils.FILE_EXTENSION))
                        .map(r -> IOUtils.getWithoutExtension(r.getName()))
                        .forEach(types::add);
            } catch (Exception ignored) {
            }

            ProcessCache.getGlobalProcessDefinitions().stream()
                    .map(ProcessDefinition::getVariableUserTypes).flatMap(List::stream)
                    .map(VariableUserType::getName)
                    .forEach(types::add);
        }

        typeCombo = new Combo(parent, SWT.SINGLE | SWT.READ_ONLY | SWT.BORDER);
        typeCombo.setItems(types.toArray(new String[types.size()]));
        typeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        typeCombo.addSelectionListener(new LoggingSelectionAdapter() {
            @Override
            protected void onSelection(SelectionEvent e) throws Exception {
                verifyContentsValid();
            }
        });
    }

    private List<String> setTypesForInternalStorage() {
        List<String> types = new ArrayList<>();
        IFile tableFile = null;
        try {
            if (paramDefGroup.getName().equals("output")) {
                Arrays.stream(DataTableUtils.getDataTableProject().members())
                        .filter(r -> r instanceof IFile && r.getName().endsWith(DataTableUtils.FILE_EXTENSION))
                        .map(r -> IOUtils.getWithoutExtension(r.getName()))
                        .filter(r -> r.equals(BotTask.usingBotTask.getSelectedDataTableName()))
                        .forEach(types::add);
                String filterLabel = VariableFormatRegistry.getInstance().getFilterLabel("java.util.List");
                filterLabel += "(" + BotTask.usingBotTask.getSelectedDataTableName() + ")";
                types.add(filterLabel);

            } else {
                for (IResource file : DataTableUtils.getDataTableProject().members()) {
                    if (file instanceof IFile && file.getName().endsWith(DataTableUtils.FILE_EXTENSION) && IOUtils.getWithoutExtension(file.getName()).equals(BotTask.usingBotTask.getSelectedDataTableName())) {
                        tableFile = (IFile) file;
                    }
                }
                if (tableFile == null) {
                    return new ArrayList<>();
                }
                String tableFileName = IOUtils.getWithoutExtension(tableFile.getName());
                String tableFileNameExtension = IOUtils.getExtension(tableFile.getName());
                VariableUserType userType = DataTableCache.getDataTable(tableFileName);
                for (Variable variable : userType.getAttributes()) {
                    if (!checkNotTableTypeVariable(variable)) {
                        continue;
                    }
                    types.add(variable.getFormatLabel());
                }
            }
        }
        catch (Exception ignored) {
            PluginLogger.logError(ignored.getMessage(), ignored);
        }
        return types;
    }

    private boolean checkNotTableTypeVariable(Variable variable) {
        if (ListFormat.class.getName().equals(variable.getFormatClassName())) {
            String innerTypeName = VariableFormatRegistry.getInstance().getUserTypeOfList(variable.getFormat());
            try {
                for (IResource file : DataTableUtils.getDataTableProject().members()) {
                    if (file instanceof IFile && file.getName().endsWith(DataTableUtils.FILE_EXTENSION)) {
                        if (IOUtils.getWithoutExtension(file.getName()).equals(innerTypeName)) {
                            return false;
                        }
                    }
                }
            }
            catch (CoreException exception) {
                PluginLogger.logErrorWithoutDialog(Localization.getString("ParamDefWizardPage.error.cantOpenTables"));
            }
            return true;
        }

        try {
            for (IResource file : DataTableUtils.getDataTableProject().members()) {
                if (file instanceof IFile && file.getName().endsWith(DataTableUtils.FILE_EXTENSION)) {
                    if (IOUtils.getWithoutExtension(file.getName()).equals(variable.getFormatLabel())) {
                        return false;
                    }
                }
            }
        }
        catch (CoreException exception) {
            PluginLogger.logErrorWithoutDialog(Localization.getString("ParamDefWizardPage.error.cantOpenTables"));
        }
        return true;
    }

    private void createUseVariableCheckbox(Composite parent) {
        new Label(parent, SWT.NONE);
        useVariableButton = new Button(parent, SWT.CHECK);
        useVariableButton.setText(Localization.getString("ParamDefWizardPage.page.useVariable"));
    }

    private void createOptionalCheckbox(Composite parent) {
        new Label(parent, SWT.NONE);
        requiredButton = new Button(parent, SWT.CHECK);
        requiredButton.setText(Localization.getString("ParamDefWizardPage.page.required"));
    }

    private void verifyContentsValid() {
        if (nameText.getText().length() == 0) {
            setErrorMessage(Localization.getString("error.paramDef.no_param_name"));
            setPageComplete(false);
        } else if (isDuplicated()) {
            setErrorMessage(Localization.getString("error.paramDef.param_exist"));
            setPageComplete(false);
        } else if (typeCombo.getText().length() == 0) {
            setErrorMessage(Localization.getString("error.paramDef.no_param_type"));
            setPageComplete(false);
        } else {
            setErrorMessage(null);
            setPageComplete(true);
        }
    }

    @Override
    public String getName() {
        if (nameText == null) {
            return "";
        }
        return nameText.getText().trim();
    }

    public String getType() {
        return VariableFormatRegistry.getInstance().getFilterJavaClassName(typeCombo.getText());
    }

    public boolean isUseVariable() {
        if (null != dialogEnhancementMode && dialogEnhancementMode.checkBotDocxTemplateEnhancementMode()) {
            return true;
        }
        return useVariableButton.getSelection();
    }

    public boolean isOptional() {
        return !requiredButton.getSelection();
    }

    private boolean isDuplicated() {
        if (paramDef != null) {
            return false;
        }
        for (ParamDef p : paramDefGroup.getParameters()) {
            if (paramDef != p && p.getName().equals(nameText.getText())) {
                return true;
            }
        }
        return false;
    }

    public ParamDefGroup getParamDefGroup() {
        return paramDefGroup;
    }

    public ParamDef getParamDef() {
        return paramDef;
    }
}
