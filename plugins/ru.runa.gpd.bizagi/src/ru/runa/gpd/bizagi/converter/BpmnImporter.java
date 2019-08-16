package ru.runa.gpd.bizagi.converter;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.draw2d.geometry.Rectangle;
import ru.runa.gpd.ProcessCache;
import ru.runa.gpd.lang.BpmnSerializer;
import ru.runa.gpd.lang.Language;
import ru.runa.gpd.lang.ProcessSerializer;
import ru.runa.gpd.lang.model.ProcessDefinition;
import ru.runa.gpd.lang.model.Swimlane;
import ru.runa.gpd.util.IOUtils;
import ru.runa.gpd.util.SwimlaneDisplayMode;
import ru.runa.gpd.util.WorkspaceOperations;
import ru.runa.gpd.util.XmlUtil;
import ru.runa.wfe.definition.ProcessDefinitionAccessType;

public class BpmnImporter {

    private static Map<String, Element> dataStoreMap = new HashMap<>();
    private static Map<String, Element> planeMap = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static void go(IContainer dstFolder, String srcFileName) throws Exception {
        try (InputStream is = new FileInputStream(new File(srcFileName))) {
            Document bpmnDocument = XmlUtil.parseWithoutValidation(is);

            Element definitionsElement = bpmnDocument.getRootElement();

            dataStoreMap.clear();
            for (Element element : (List<Element>) definitionsElement.elements("dataStore")) {
                dataStoreMap.put(element.attributeValue("id"), element);
            }

            planeMap.clear();
            Element bpmnDiagram = definitionsElement.element("BPMNDiagram");
            if (bpmnDiagram != null) {
                Element bpmnPlane = bpmnDiagram.element("BPMNPlane");
                if (bpmnPlane != null) {
                    for (Element element : (List<Element>) bpmnPlane.elements()) {
                        planeMap.put(element.attributeValue("bpmnElement"), element);
                    }
                }
            }

            Element processElement = definitionsElement.element("process");

            String name = processElement.attributeValue("name");
            
            IFolder folder = IOUtils.getProcessFolder(dstFolder, name);
            folder.create(true, true, null);
            IFile definitionFile = IOUtils.getProcessDefinitionFile(folder);
            String processName = folder.getName();

            Language language = Language.BPMN;
            Map<String, String> properties = Maps.newHashMap();
            if (language == Language.BPMN) {
                Element element = definitionsElement.element("collaboration");
                if (element != null) {
                    element = element.element("participant");
                    if (element != null) {
                        Attribute id = element.attribute("id");
                        if (id != null) {
                            Element shape = planeMap.get(id.getText());
                            if (shape != null) {
                                properties.put(BpmnSerializer.SHOW_SWIMLANE,
                                        "true".equals(shape.attributeValue("isHorizontal")) ? SwimlaneDisplayMode.horizontal.name()
                                                : SwimlaneDisplayMode.vertical.name());
                            }
                        }
                    }
                }
            }
            properties.put(ProcessSerializer.ACCESS_TYPE, ProcessDefinitionAccessType.Process.name());
            Document document = language.getSerializer().getInitialProcessDefinitionDocument(processName, properties);
            byte[] bytes = XmlUtil.writeXml(document);
            definitionFile.create(new ByteArrayInputStream(bytes), true, null);

            ProcessDefinition definition = ProcessCache.getProcessDefinition(definitionFile);
            definition.setName(name);
            definition.setLanguage(language);

            Element laneSet = processElement.element("laneSet");
            if (laneSet != null) {
                for (Element lane : (List<Element>) laneSet.elements("lane")) {
                    Swimlane swimlane = new Swimlane();
                    String laneName = lane.attributeValue("name");
                    swimlane.setName(Strings.isNullOrEmpty(laneName) ? "*" : laneName);
                    swimlane.setConstraint(bounds(lane.attributeValue("id")));
                    definition.addChild(swimlane);
                }
            }

            WorkspaceOperations.saveProcessDefinition(definitionFile, definition);

            ProcessCache.newProcessDefinitionWasCreated(definitionFile);
            WorkspaceOperations.openProcessDefinition(definitionFile);
        }
    }

    private static Rectangle bounds(String id) {
        Element shape = planeMap.get(id);
        if (shape != null) {
            Element bounds = shape.element("Bounds");
            if (bounds != null) {
                return new Rectangle(Integer.parseInt(bounds.attributeValue("x")), Integer.parseInt(bounds.attributeValue("y")),
                        Integer.parseInt(bounds.attributeValue("width")), Integer.parseInt(bounds.attributeValue("height")));
            }
        }
        throw new IllegalStateException("id: " + id + " does not exist");
    }

    private BpmnImporter() {
        // All-static class
    }

}
