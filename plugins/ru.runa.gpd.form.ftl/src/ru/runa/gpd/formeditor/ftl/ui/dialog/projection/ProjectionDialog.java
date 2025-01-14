package ru.runa.gpd.formeditor.ftl.ui.dialog.projection;

import java.text.MessageFormat;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import ru.runa.gpd.extension.handler.XmlBasedConstructorProvider;
import ru.runa.gpd.formeditor.resources.Messages;
import ru.runa.gpd.lang.ValidationError;
import ru.runa.gpd.lang.model.Delegable;
import ru.runa.gpd.lang.model.GraphElement;
import ru.runa.gpd.lang.model.VariableUserType;
import ru.runa.gpd.ui.custom.LoggingSelectionAdapter;
import ru.runa.gpd.ui.custom.SwtUtils;
import ru.runa.wfe.commons.CollectionUtil;

public class ProjectionDialog extends XmlBasedConstructorProvider<ProjectionDataModel> {
    private final VariableUserType userType;
    private final GraphElement graphElement;

    public ProjectionDialog(VariableUserType userType, GraphElement graphElement) {
        this.userType = userType;
        this.graphElement = graphElement;
    }

    @Override
    protected String getTitle() {
        return Messages.getString("ProjectionDialog.title");
    }

    @Override
    protected Composite createConstructorComposite(Composite parent, Delegable delegable, ProjectionDataModel model) {
        return new ProjectionView(parent, delegable, model).build();
    }

    @Override
    protected ProjectionDataModel createDefault() {
        return ProjectionDataModel.by(userType);
    }

    @Override
    protected ProjectionDataModel fromXml(String xml) throws Exception {
        return ProjectionDataModel.fromXml(xml).map(model -> model.merge(createDefault())).orElseGet(() -> createDefault());
    }

    @Override
    public boolean validateValue(Delegable delegable, List<ValidationError> errors) throws Exception {
        if (StringUtils.isBlank(delegable.getDelegationConfiguration())) {
            errors.add(ValidationError.createError(graphElement, Messages.getString("validation.projection.not.set")));
            return false;
        }
        return validateModel(delegable, ProjectionDataModel.fromXml(delegable.getDelegationConfiguration()).get(), errors);
    }

    @Override
    protected boolean validateModel(Delegable delegable, ProjectionDataModel model, List<ValidationError> errors) {
        final List<Projection> projections = model.getProjections();
        final List<Projection> actualProjections = createDefault().getProjections();

        if (projections.size() > actualProjections.size() && !CollectionUtil.diffSet(projections, actualProjections).isEmpty()
                || projections.size() < actualProjections.size() && !CollectionUtil.diffSet(actualProjections, projections).isEmpty()) {
            errors.add(ValidationError.createError(graphElement,
                    MessageFormat.format(Messages.getString("validation.projection.usertype.changed"), userType.getName())));
            return false;
        }
        return super.validateModel(delegable, model, errors);
    }

    private class ProjectionView extends ConstructorComposite {

        public ProjectionView(Composite parent, Delegable delegable, ProjectionDataModel model) {
            super(parent, delegable, model);
            setLayout(new GridLayout(3, false));
        }

        public Composite build() {
            buildFromModel();
            return this;
        }

        @Override
        protected void buildFromModel() {
            for (Control control : getChildren()) {
                control.dispose();
            }

            SwtUtils.createLabel(this, Messages.getString("ProjectionDialog.label.attributes"));
            SwtUtils.createLabel(this, Messages.getString("ProjectionDialog.label.visible"));
            SwtUtils.createLabel(this, Messages.getString("ProjectionDialog.label.sort"));

            for (Projection projection : model.getProjections()) {
                buildProjectionView(projection);
            }

            ((ScrolledComposite) getParent()).setMinSize(computeSize(getSize().x, SWT.DEFAULT));
            updateComponents();
        }

        private void buildProjectionView(Projection projection) {
            SwtUtils.createLabel(this, projection.getName());

            final Button button = new Button(this, SWT.CHECK);
            button.setSelection(projection.getVisibility() == Visibility.VISIBLE);
            button.addSelectionListener(LoggingSelectionAdapter.widgetSelectedAdapter(
                    e -> projection.setVisibility(projection.getVisibility() == Visibility.VISIBLE ? Visibility.INVISIBLE : Visibility.VISIBLE)));

            final Combo combo = new Combo(this, SWT.READ_ONLY);
            Sort.messages().forEach(combo::add);
            combo.setText(projection.getSort().getMessage());

            combo.addSelectionListener(LoggingSelectionAdapter.widgetSelectedAdapter(e -> {
                projection.setSort(Sort.by(combo.getText()).orElse(Sort.NONE));
                updateComponents();
            }));
        }

        private void updateComponents() {
            this.layout(true, true);
            this.redraw();
        }

    }

}
