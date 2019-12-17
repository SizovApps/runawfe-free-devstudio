package ru.runa.gpd.lang.model.bpmn;

import java.util.List;
import org.eclipse.core.resources.IFile;
import ru.runa.gpd.lang.ValidationError;
import ru.runa.gpd.lang.model.ISendMessageNode;

public class ThrowEventNode extends AbstractEventNode implements ISendMessageNode, IBoundaryEventContainer {
    @Override
    public void validate(List<ValidationError> errors, IFile definitionFile) {
        List<CatchEventNode> catchEventNodes = getChildren(CatchEventNode.class);
        for (CatchEventNode catchEventNode : catchEventNodes) {
            if (catchEventNode.getEventNodeType() != EventNodeType.error) {
                errors.add(ValidationError.createLocalizedError(this, "catchEventNode.notErrorType"));
            }
        }

        super.validate(errors, definitionFile);
    }
}
