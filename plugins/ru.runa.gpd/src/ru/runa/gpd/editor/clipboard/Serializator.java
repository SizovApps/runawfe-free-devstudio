package ru.runa.gpd.editor.clipboard;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;
import ru.runa.gpd.lang.model.ProcessDefinition;
import ru.runa.gpd.lang.model.Swimlane;
import ru.runa.gpd.lang.model.Variable;
import ru.runa.gpd.lang.model.VariableStoreType;
import ru.runa.gpd.lang.model.VariableUserType;
import ru.runa.gpd.util.VariableUtils;
import ru.runa.gpd.validation.ValidatorConfig;

/**
 * Recursive serialization for {@link org.eclipse.swt.dnd.Clipboard}.
 *
 * @author KuchmaMA
 *
 */
final class Serializator {

    private Serializator() {
        // no action
    }

    static void write(ObjectOutputStream out, Swimlane swimlane) throws IOException {
        out.writeObject(Strings.nullToEmpty(swimlane.getEditorPath()));
        write(out, (Variable) swimlane);
    }

    static void read(ObjectInputStream in, Swimlane swimlane, ProcessDefinition processDefinition) throws IOException, ClassNotFoundException {
        swimlane.setEditorPath((String) in.readObject());
        read(in, (Variable) swimlane, processDefinition);
    }

    @SuppressWarnings("unchecked")
    static void read(ObjectInputStream in, ValidatorConfig validatorConfig,  ProcessDefinition processDefinition) throws IOException, ClassNotFoundException {
        validatorConfig.setType((String) in.readObject());
        validatorConfig.setMessage((String) in.readObject());
        Map<String, String> params = validatorConfig.getParams();
        params.putAll((Map<String, String>) in.readObject());
        validatorConfig.getTransitionNames().addAll((List<String>) in.readObject());
    }

    static void write(ObjectOutputStream out, ValidatorConfig validatorConfig) throws IOException {
        out.writeObject(validatorConfig.getType());
        out.writeObject(validatorConfig.getMessage());
        out.writeObject(validatorConfig.getParams());
        out.writeObject(validatorConfig.getTransitionNames());
    }

    static void write(ObjectOutputStream out, Variable variable) throws IOException {
        out.writeObject(variable.getScriptingName());
        out.writeObject(variable.getFormat());
        out.writeBoolean(variable.isPublicVisibility());
        out.writeBoolean(variable.isEditableInChat());
        out.writeObject(Strings.nullToEmpty(variable.getDefaultValue()));
        out.writeObject(variable.getName());
        out.writeObject(Strings.nullToEmpty(variable.getDescription()));
        out.writeBoolean(variable.isPrimaryKey());
        out.writeBoolean(variable.isAutoincrement());
        out.writeObject(variable.getUserType() == null ? "" : variable.getUserType().getName());
        out.writeBoolean(variable.isComplex());
        if (variable.isComplex()) {
            write(out, variable.getUserType());
        }
        else {
            boolean containerVariable = variable.getProcessDefinition() != null && VariableUtils.isContainerVariable(variable);
            out.writeBoolean(containerVariable);
            if (containerVariable) {
                String[] componentNames = variable.getFormatComponentClassNames();
                List<VariableUserType> vuts = Lists.newArrayList();
                for (String componentName : componentNames) {
                    if (VariableUtils.isValidUserTypeName(componentName)) {
                        VariableUserType vut = variable.getProcessDefinition().getVariableUserType(componentName);
                        if (vut != null) {
                            vuts.add(vut);
                        }
                    }
                }
                out.writeInt(vuts.size());
                for (VariableUserType vut : vuts) {
                    write(out, vut);
                }
            }
        }
        out.writeObject(variable.getStoreType());
    }

    static void read(ObjectInputStream in, Variable variable, ProcessDefinition processDefinition) throws IOException, ClassNotFoundException {
        variable.setScriptingName((String) in.readObject());
        variable.setFormat((String) in.readObject());
        variable.setPublicVisibility(in.readBoolean());
        variable.setEditableInChat(in.readBoolean());
        variable.setDefaultValue((String) in.readObject());
        variable.setName((String) in.readObject());
        variable.setDescription((String) in.readObject());
        variable.setPrimaryKey(in.readBoolean());
        variable.setAutoincrement(in.readBoolean());
        String label = (String) in.readObject();
        if (!label.isEmpty() && processDefinition != null) {
            variable.setUserType(processDefinition.getVariableUserType(label));
        }
        if (in.readBoolean()) {
            VariableUserType type = new VariableUserType();
            read(in, type, processDefinition);
            variable.setUserType(type);
        }
        else if (in.readBoolean()) {
            int vutSize = in.readInt();
            for (int i = 0; i < vutSize; i++) {
                VariableUserType vut = new VariableUserType();
                read(in, vut, processDefinition);
                if (processDefinition.getVariableUserType(vut.getName()) == null) {
                    addVariableUserType(processDefinition, vut);
                }
            }
        }
        variable.setStoreType((VariableStoreType) in.readObject());
    }

    private static void addVariableUserType(ProcessDefinition pd, VariableUserType type) {
        pd.addVariableUserType(type);
        for (Variable v : type.getAttributes()) {
            if (v.isComplex()) {
                if (pd.getVariableUserType(v.getUserType().getName()) == null) {
                    addVariableUserType(pd, v.getUserType());
                }
            }
        }
    }

    static void write(ObjectOutputStream out, VariableUserType type) throws IOException {
        out.writeObject(type.getName());
        out.writeBoolean(type.isStoreInExternalStorage());
        out.writeInt(type.getAttributes().size());
        for (Variable var : type.getAttributes()) {
            write(out, var);
        }
    }

    static void read(ObjectInputStream in, VariableUserType type, ProcessDefinition processDefinition) throws IOException, ClassNotFoundException {
        type.setName((String) in.readObject());
        type.setStoreInExternalStorage(in.readBoolean());
        int attrLength = in.readInt();
        for (int j = 0; j < attrLength; j++) {
            Variable var = new Variable();
            read(in, var, processDefinition);
            type.getAttributes().add(var);
        }
    }

}
