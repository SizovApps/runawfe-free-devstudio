package ru.runa.gpd.ui.wizard;

import com.google.common.collect.Maps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

import ru.runa.gpd.Localization;
import ru.runa.gpd.ProcessCache;
import ru.runa.gpd.extension.VariableFormatArtifact;
import ru.runa.gpd.extension.VariableFormatRegistry;
import ru.runa.gpd.lang.model.ProcessDefinition;
import ru.runa.gpd.lang.model.Variable;
import ru.runa.gpd.lang.model.VariableContainer;
import ru.runa.gpd.lang.model.VariableUserType;
import ru.runa.gpd.ui.custom.DynaContentWizardPage;
import ru.runa.gpd.ui.custom.LoggingSelectionAdapter;
import ru.runa.gpd.ui.custom.SwtUtils;
import ru.runa.gpd.util.DataTableUtils;
import ru.runa.gpd.util.IOUtils;
import ru.runa.wfe.var.format.BigDecimalFormat;
import ru.runa.wfe.var.format.ListFormat;
import ru.runa.wfe.var.format.LongFormat;
import ru.runa.wfe.var.format.MapFormat;
import ru.runa.wfe.var.format.StringFormat;
import ru.runa.wfe.var.format.UserTypeFormat;



public class VariableFormatPage extends DynaContentWizardPage {
    private final VariableContainer variableContainer;
    private VariableFormatArtifact type;
    private VariableUserType userType;
    private String[] componentClassNames;
    private final ProcessDefinition processDefinition;
    private final boolean editFormat;

    private final boolean isStoreInExternalStorage;
    private boolean primaryKey = false;
    private boolean autoincrement = false;
    private String format = "";

    private Button autoincrementCheckbox;

    private static Map<String, String[]> containerFormats = Maps.newHashMap();
    static {
        containerFormats.put(ListFormat.class.getName(), new String[] { Localization.getString("VariableFormatPage.components.list.value") });
        containerFormats.put(MapFormat.class.getName(), new String[] { Localization.getString("VariableFormatPage.components.map.key"),
                Localization.getString("VariableFormatPage.components.map.value") });
    }

    public VariableFormatPage(ProcessDefinition processDefinition, VariableContainer variableContainer, Variable variable, boolean editFormat) {
        this.processDefinition = processDefinition;
        this.variableContainer = variableContainer;
        this.isStoreInExternalStorage = (variableContainer instanceof VariableUserType)
                ? ((VariableUserType) variableContainer).isStoreInExternalStorage()
                : false;
        if (variable != null) {
            this.primaryKey = variable.isPrimaryKey();
            this.autoincrement = variable.isAutoincrement();
            format = variable.getFormat();
            if (variable.getUserType() != null) {
                this.userType = variable.getUserType();
                setTypeByFormatClassName(UserTypeFormat.class.getName());
            } else {
                setTypeByFormatClassName(variable.getFormatClassName());
            }
            componentClassNames = variable.getFormatComponentClassNames();
            if (containerFormats.containsKey(type.getName()) && componentClassNames.length != containerFormats.get(type.getName()).length) {
                createDefaultComponentClassNames();
            }
        } else {
            setTypeByFormatClassName(StringFormat.class.getName());
            componentClassNames = new String[0];
        }
        this.editFormat = editFormat;
    }

    private void setTypeByFormatClassName(String formatClassName) {
        this.type = VariableFormatRegistry.getInstance().getArtifactNotNull(formatClassName);
    }

    @Override
    protected int getGridLayoutColumns() {
        return 1;
    }

    @Override
    protected void createContent(Composite composite) {
        final Combo combo = createTypeCombo(composite, false, false);
        combo.setEnabled(editFormat);
        combo.setText(userType != null ? userType.getName() : type.getLabel());
        combo.addSelectionListener(new LoggingSelectionAdapter() {
            @Override
            protected void onSelection(SelectionEvent e) throws Exception {
                String label = combo.getText();
                ru.runa.gpd.PluginLogger.logInfo("Variable format: " + label);
                ru.runa.gpd.PluginLogger.logInfo("VariableFormatPage падает начало!!!");
                userType = processDefinition.getVariableUserType(label);
                ru.runa.gpd.PluginLogger.logInfo("VariableFormatPage падает конец!!! " + userType);
                if (userType != null) {
                    type = VariableFormatRegistry.getInstance().getArtifactNotNull(UserTypeFormat.class.getName());
                } else {
                    type = VariableFormatRegistry.getInstance().getArtifactNotNullByLabel(label);
                }
                ru.runa.gpd.PluginLogger.logInfo("Variable format: " + type.getJavaClassName());
                updateAutoincrementCheckboxVisibility(type.getName());
                createDefaultComponentClassNames();
                updateContent();
            }
        });

        if (isStoreInExternalStorage) {
            final Button primaryKeyChecbox = new Button(composite, SWT.CHECK);
            primaryKeyChecbox.setText(Localization.getString("VariableFormatPage.primaryKey"));
            primaryKeyChecbox.setSelection(primaryKey);
            primaryKeyChecbox.setEnabled(editFormat);
            primaryKeyChecbox.addSelectionListener(SelectionListener.widgetSelectedAdapter(c -> primaryKey = !primaryKey));

            autoincrementCheckbox = new Button(composite, SWT.CHECK);
            autoincrementCheckbox.setText(Localization.getString("VariableFormatPage.autoincrement"));
            autoincrementCheckbox.setSelection(autoincrement);
            autoincrementCheckbox.setEnabled(editFormat);
            autoincrementCheckbox.addSelectionListener(SelectionListener.widgetSelectedAdapter(c -> autoincrement = !autoincrement));
            updateAutoincrementCheckboxVisibility(format);
        }

        dynaComposite = new Composite(composite, SWT.NONE);
        dynaComposite.setLayout(new GridLayout(2, false));
        dynaComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
    }

    private void updateAutoincrementCheckboxVisibility(String format) {
        if (autoincrementCheckbox == null) {
            return;
        }
        final boolean visible = LongFormat.class.getName().equals(format) || BigDecimalFormat.class.getName().equals(format);
        autoincrementCheckbox.setVisible(visible);
        if (!visible) {
            autoincrement = false;
        }
    }

    private void createDefaultComponentClassNames() {
        String[] labels = containerFormats.get(type.getName());
        componentClassNames = new String[labels != null ? labels.length : 0];
        for (int i = 0; i < componentClassNames.length; i++) {
            componentClassNames[i] = StringFormat.class.getName();
        }
    }

    private Combo createTypeCombo(Composite composite, boolean disableListFormat, boolean disableMapFormat) {
        final Combo combo = new Combo(composite, SWT.BORDER | SWT.READ_ONLY);
        ArrayList<VariableFormatArtifact> usedArtifacts = new ArrayList<>();
        for (VariableFormatArtifact artifact : VariableFormatRegistry.getInstance().getAll()) {
            if (usedArtifacts.contains(artifact)) {
                continue;
            }
            usedArtifacts.add(artifact);
            if (UserTypeFormat.class.getName().equals(artifact.getName())
                    || disableListFormat && ListFormat.class.getName().equals(artifact.getName())
                    || disableMapFormat && MapFormat.class.getName().equals(artifact.getName())) {
                continue;
            }

            if (artifact.isEnabled()) {
                combo.add(artifact.getLabel());
            }
        }
        ArrayList<VariableUserType> usedUserTypes = new ArrayList<>();

        for (VariableUserType userType : processDefinition.getVariableUserTypes()) {
            if (usedUserTypes.contains(userType)) {
                continue;
            }
            if (!(variableContainer instanceof VariableUserType) || ((VariableUserType) variableContainer).canUseAsAttributeType(userType)) {
                usedUserTypes.add(userType);
                combo.add(userType.getName());
            }
        }

        for (ProcessDefinition processDefinition : ProcessCache.getAllProcessDefinitions()) {
            for (VariableUserType userType : processDefinition.getVariableUserTypes()) {
                if (usedUserTypes.contains(userType)) {
                    continue;
                }
                if (!(variableContainer instanceof VariableUserType) || ((VariableUserType) variableContainer).canUseAsAttributeType(userType)) {
                    usedUserTypes.add(userType);
                    combo.add(userType.getName());
                }
            }
        }

        combo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        return combo;
    }

    @Override
    protected void createDynaContent() {
        String[] labels = containerFormats.get(type.getName());
        if (labels != null) {
            GridData strokeData = new GridData(GridData.FILL_HORIZONTAL);
            strokeData.horizontalSpan = 2;
            SwtUtils.createStrokeComposite(dynaComposite, strokeData, Localization.getString("VariableFormatPage.components.label"), 3);
            for (int i = 0; i < labels.length; i++) {
                Label label = new Label(dynaComposite, SWT.NONE);
                label.setText(labels[i]);
                final Combo combo = createTypeCombo(dynaComposite, true, true);
                combo.setData(i);
                VariableFormatArtifact artifact = VariableFormatRegistry.getInstance().getArtifact(componentClassNames[i]);
                combo.setText(artifact != null ? artifact.getLabel() : componentClassNames[i]);
                combo.addSelectionListener(new LoggingSelectionAdapter() {
                    @Override
                    protected void onSelection(SelectionEvent e) throws Exception {
                        int index = (Integer) combo.getData();
                        String label = combo.getText();
                        VariableUserType userType = processDefinition.getVariableUserType(label);
                        if (userType != null) {
                            componentClassNames[index] = label;
                        } else {
                            componentClassNames[index] = VariableFormatRegistry.getInstance().getArtifactNotNullByLabel(label).getName();
                        }
                    }
                });
            }
        }
    }

    @Override
    protected void verifyContentIsValid() {
    }

    public VariableUserType getUserType() {
        return userType;
    }

    public VariableFormatArtifact getType() {
        return type;
    }

    public String[] getComponentClassNames() {
        return componentClassNames;
    }

    public boolean isPrimaryKey() {
        return primaryKey;
    }

    public boolean isAutoincrement() {
        return autoincrement;
    }
}
