package ru.runa.gpd.ui.dialog;

        import org.eclipse.jface.dialogs.Dialog;
        import org.eclipse.jface.dialogs.IDialogConstants;
        import org.eclipse.swt.SWT;
        import org.eclipse.swt.events.ModifyEvent;
        import org.eclipse.swt.layout.GridData;
        import org.eclipse.swt.layout.GridLayout;
        import org.eclipse.swt.widgets.Composite;
        import org.eclipse.swt.widgets.Control;
        import org.eclipse.swt.widgets.Display;
        import org.eclipse.swt.widgets.Label;
        import org.eclipse.swt.widgets.Shell;
        import org.eclipse.swt.widgets.Text;
        import ru.runa.gpd.Localization;
        import ru.runa.gpd.extension.VariableFormatRegistry;
        import ru.runa.gpd.lang.model.ProcessDefinition;
        import ru.runa.gpd.lang.model.VariableUserType;
        import ru.runa.gpd.ui.custom.LoggingModifyTextAdapter;
        import ru.runa.gpd.ui.custom.VariableNameChecker;

public class NeedToRewriteDialog extends Dialog {
    private boolean needToRewrite;

    public NeedToRewriteDialog() {
        super(Display.getCurrent().getActiveShell());
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(ru.runa.gpd.Localization.getString("Need to rewrite?"));
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        GridLayout layout = new GridLayout(1, false);
        area.setLayout(layout);

        Composite composite = new Composite(area, SWT.NONE);

        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        composite.setLayout(gridLayout);
        composite.setLayoutData(new GridData());

        Label labelName = new Label(composite, SWT.NONE);
        labelName.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
        labelName.setText(ru.runa.gpd.Localization.getString("property.name") + ":");

        final Text nameField = new Text(composite, SWT.BORDER);
        GridData nameTextData = new GridData(GridData.FILL_HORIZONTAL);
        nameTextData.minimumWidth = 200;
        nameField.setText("what!");
        nameField.addKeyListener(new ru.runa.gpd.ui.custom.VariableNameChecker());
        nameField.setLayoutData(nameTextData);
        nameField.addModifyListener(new ru.runa.gpd.ui.custom.LoggingModifyTextAdapter() {

            @Override
            protected void onTextChanged(ModifyEvent e) throws Exception {
                ru.runa.gpd.PluginLogger.logInfo("Res: " + nameField.getText());

            }
        });
        nameField.selectAll();
        return area;
    }


    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
    }

    public boolean getRes() {
        return needToRewrite;
    }

}
