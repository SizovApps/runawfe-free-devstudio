package ru.runa.gpd.bizagi.converter;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
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
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import ru.runa.gpd.ProcessCache;
import ru.runa.gpd.lang.BpmnSerializer;
import ru.runa.gpd.lang.Language;
import ru.runa.gpd.lang.NodeRegistry;
import ru.runa.gpd.lang.NodeTypeDefinition;
import ru.runa.gpd.lang.ProcessSerializer;
import ru.runa.gpd.lang.model.EndState;
import ru.runa.gpd.lang.model.GraphElement;
import ru.runa.gpd.lang.model.Node;
import ru.runa.gpd.lang.model.ProcessDefinition;
import ru.runa.gpd.lang.model.StartState;
import ru.runa.gpd.lang.model.Subprocess;
import ru.runa.gpd.lang.model.Swimlane;
import ru.runa.gpd.lang.model.SwimlanedNode;
import ru.runa.gpd.lang.model.TaskState;
import ru.runa.gpd.lang.model.Transition;
import ru.runa.gpd.lang.model.bpmn.ExclusiveGateway;
import ru.runa.gpd.lang.model.bpmn.TextAnnotation;
import ru.runa.gpd.lang.model.bpmn.ThrowEventNode;
import ru.runa.gpd.util.IOUtils;
import ru.runa.gpd.util.SwimlaneDisplayMode;
import ru.runa.gpd.util.WorkspaceOperations;
import ru.runa.gpd.util.XmlUtil;
import ru.runa.wfe.definition.ProcessDefinitionAccessType;

@SuppressWarnings("unchecked")
public class BizagiBpmnImporter {

    private static Map<String, Element> dataStoreMap = new HashMap<>();
    private static Map<String, Element> planeMap = new HashMap<>();
    private static Map<String, String> idsMap = new HashMap<>();
    private static Map<String, GraphElement> geMap = new HashMap<>();
    private static Map<Rectangle, Swimlane> swimlaneBoundMap = new HashMap<>();

    public static void go(IContainer dstFolder, String srcFileName, boolean showSwimlanes, boolean optimizeElementsSize, boolean ignoreBendPoints)
            throws Exception {
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

            String processName = processElement.attributeValue("name");
            
            IFolder folder = IOUtils.getProcessFolder(dstFolder, processName);
            folder.create(true, true, null);
            IFile definitionFile = IOUtils.getProcessDefinitionFile(folder);

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
                                        showSwimlanes ? ("true".equals(shape.attributeValue("isHorizontal")) ? SwimlaneDisplayMode.horizontal.name()
                                                : SwimlaneDisplayMode.vertical.name()) : SwimlaneDisplayMode.none.name());
                            }
                        }
                    }
                }
            }
            properties.put(ProcessSerializer.ACCESS_TYPE, ProcessDefinitionAccessType.Process.name());
            Document document = language.getSerializer().getInitialProcessDefinitionDocument(folder.getName(), properties);
            byte[] bytes = XmlUtil.writeXml(document);
            definitionFile.create(new ByteArrayInputStream(bytes), true, null);

            ProcessDefinition definition = ProcessCache.getProcessDefinition(definitionFile);
            definition.setName(processName);
            definition.setLanguage(language);

            Element laneSet = processElement.element("laneSet");
            if (laneSet != null) {
                for (Element lane : (List<Element>) laneSet.elements("lane")) {
                    Swimlane swimlane = new Swimlane();
                    String laneName = lane.attributeValue("name");
                    swimlane.setName(Strings.isNullOrEmpty(laneName) ? "*" : laneName);
                    Rectangle bounds = bounds(lane.attributeValue("id"));
                    if (showSwimlanes) {
                        swimlane.setConstraint(bounds);
                    }
                    definition.addChild(swimlane);
                    swimlaneBoundMap.put(bounds, swimlane); // TODOO
                }
            }

            idsMap.clear();
            geMap.clear();
            List<Element> elements = processElement.elements();
            for (Element element : elements) {
                String elementName = element.getName();
                String id = element.attributeValue("id");
                String name = element.attributeValue("name");
                switch (elementName) {
                case "startEvent": {
                    NodeTypeDefinition typeDefinition = NodeRegistry.getNodeTypeDefinition(StartState.class);
                    StartState start = new StartState();
                    start.setName(name);
                    // start.setConstraint(bounds(id));
                    // start.getConstraint().width = typeDefinition.getGraphitiEntry().getDefaultSize().width;
                    // start.getConstraint().height = typeDefinition.getGraphitiEntry().getDefaultSize().height;
                    setConstraint(start, id);
                    Swimlane swimlane = swimlane(start);
                    start.setSwimlane(swimlane);
                    if (showSwimlanes) {
                        start.setParentContainer(swimlane);
                    }
                    start.getConstraint().translate(swimlane.getConstraint().getTopLeft().negate());
                    definition.addChild(start);
                    idsMap.put(id, start.getId());
                    idsMap.put(start.getId(), id);
                    geMap.put(id, start);
                    break;
                }
                case "task":
                case "manualTask":
                case "userTask": {
                    TaskState task = new TaskState();
                    task.setName(name);
                    task.setConstraint(bounds(id));
                    // setConstraint(task, id);
                    Swimlane swimlane = swimlane(task);
                    task.setSwimlane(swimlane);
                    task.setParentContainer(swimlane);
                    task.getConstraint().translate(swimlane.getConstraint().getTopLeft().negate());
                    definition.addChild(task);
                    idsMap.put(id, task.getId());
                    idsMap.put(task.getId(), id);
                    geMap.put(id, task);
                    break;
                }
                case "exclusiveGateway": {
                    ExclusiveGateway eg = new ExclusiveGateway();
                    eg.setName(name);
                    // eg.setConstraint(bounds(id));
                    setConstraint(eg, id);
                    definition.addChild(eg);
                    idsMap.put(id, eg.getId());
                    idsMap.put(eg.getId(), id);
                    geMap.put(id, eg);
                    break;
                }
                case "subProcess": {
                    Subprocess sp = new Subprocess();
                    sp.setName(name);
                    sp.setConstraint(bounds(id));
                    // setConstraint(sp, id);
                    definition.addChild(sp);
                    idsMap.put(id, sp.getId());
                    idsMap.put(sp.getId(), id);
                    geMap.put(id, sp);
                    break;
                }
                case "endEvent": {
                    EndState end = new EndState();
                    end.setName(name);
                    // end.setConstraint(bounds(id));
                    setConstraint(end, id);
                    definition.addChild(end);
                    idsMap.put(id, end.getId());
                    idsMap.put(end.getId(), id);
                    geMap.put(id, end);
                    break;
                }
                case "boundaryEvent": {
                    ThrowEventNode event = new ThrowEventNode();
                    event.setName(name);
                    // event.setConstraint(bounds(id));
                    setConstraint(event, id);
                    definition.addChild(event);
                    /* TODO
                    String attachedToRefId = element.attributeValue("attachedToRef");
                    event.setParentContainer(geMap.get(attachedToRefId));
                    */
                    idsMap.put(id, event.getId());
                    idsMap.put(event.getId(), id);
                    geMap.put(id, event);
                    break;
                }
                case "sequenceFlow": {
                    String sourceRef = element.attributeValue("sourceRef");
                    String targetRef = element.attributeValue("targetRef");
                    NodeTypeDefinition transitionDefinition = NodeRegistry.getNodeTypeDefinition(Transition.class);
                    Node source = (Node) geMap.get(sourceRef);
                    Transition transition = transitionDefinition.createElement(source, false);
                    // transition.setId(id);
                    transition.setName(Strings.isNullOrEmpty(name) ? source.getNextTransitionName(transitionDefinition) : name);
                    transition.setTarget((Node) geMap.get(targetRef));
                    source.addLeavingTransition(transition);
                    if (!ignoreBendPoints) {
                        transition.setBendpoints(waypoints(id));
                    }
                    idsMap.put(id, transition.getId());
                    idsMap.put(transition.getId(), id);
                    geMap.put(id, transition);
                    break;
                }
                case "association":
                case "extensionElements":
                case "laneSet": {
                    // Do nothing
                    break;
                }
                /*
                case "": {
                    break;
                }
                */
                default: {
                    TextAnnotation annotation = new TextAnnotation();
                    annotation.setConstraint(bounds(id));
                    // setConstraint(annotation, id);
                    annotation.setDescription("Bizagi: " + elementName + "\nId: " + id + "\nName: " + name);
                    definition.addChild(annotation);
                    idsMap.put(id, annotation.getId());
                    idsMap.put(annotation.getId(), id);
                    geMap.put(id, annotation);
                }
                }
            }

            WorkspaceOperations.saveProcessDefinition(definitionFile, definition);

            ProcessCache.newProcessDefinitionWasCreated(definitionFile);
            WorkspaceOperations.openProcessDefinition(definitionFile);
        }
    }

    private static void setConstraint(GraphElement ge, String id) {
        NodeTypeDefinition typeDefinition = NodeRegistry.getNodeTypeDefinition(ge.getClass());
        Dimension defaultSize = typeDefinition.getGraphitiEntry().getDefaultSize();
        Rectangle bounds = bounds(id);
        // bounds.width = Math.max(bounds.width, defaultSize.width);
        // bounds.height = Math.max(bounds.height, defaultSize.height);
        if (bounds.width < defaultSize.width) {
            bounds.x -= (defaultSize.width - bounds.width) / 2;
            bounds.width = defaultSize.width;
        }
        if (bounds.height < defaultSize.height) {
            bounds.y -= (defaultSize.height - bounds.height) / 2;
            bounds.height = defaultSize.height;
        }
        ge.setConstraint(bounds);
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

    private static List<Point> waypoints(String id) {
        Element shape = planeMap.get(id);
        if (shape != null) {
            List<Point> bendPoints = Lists.newArrayList();
            List<Element> waypoints = shape.elements("waypoint");
            if (waypoints != null) {
                for (Element waypoint : waypoints) {
                    bendPoints.add(new Point(Integer.parseInt(waypoint.attributeValue("x")), Integer.parseInt(waypoint.attributeValue("y"))));
                }
            }
            if (bendPoints.size() >= 2) {
                bendPoints.remove(bendPoints.size() - 1);
                bendPoints.remove(0);
            }
            return bendPoints;
        }
        throw new IllegalStateException("id: " + id + " does not exist");
    }

    private static Swimlane swimlane(SwimlanedNode node) {
        for (Map.Entry<Rectangle, Swimlane> entry : swimlaneBoundMap.entrySet()) {
            if (entry.getKey().contains(node.getConstraint().getTopLeft())) {
                return entry.getValue();
            }
        }
        throw new IllegalStateException("unknown swimlane for GraphElement " + node + ", constraint: " + node.getConstraint());
    }

    private BizagiBpmnImporter() {
        // All-static class
    }

}
