package ru.runa.gpd.bizagi.converter;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.QName;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PrecisionPoint;
import org.eclipse.draw2d.geometry.PrecisionRectangle;
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
import ru.runa.gpd.lang.model.bpmn.CatchEventNode;
import ru.runa.gpd.lang.model.bpmn.ExclusiveGateway;
import ru.runa.gpd.lang.model.bpmn.ParallelGateway;
import ru.runa.gpd.lang.model.bpmn.ScriptTask;
import ru.runa.gpd.lang.model.bpmn.TextAnnotation;
import ru.runa.gpd.lang.model.bpmn.ThrowEventNode;
import ru.runa.gpd.util.IOUtils;
import ru.runa.gpd.util.SwimlaneDisplayMode;
import ru.runa.gpd.util.WorkspaceOperations;
import ru.runa.gpd.util.XmlUtil;
import ru.runa.wfe.definition.ProcessDefinitionAccessType;

@SuppressWarnings("unchecked")
public class BizagiBpmnImporter {

    private static final String ASSOCIATION = "association";
    private static final String EMPTY_ROLE = "*";
    private static final String Y = "y";
    private static final String X = "x";
    private static final String HEIGHT = "height";
    private static final String WIDTH = "width";
    private static final String BOUNDS = "Bounds";
    private static final String WAYPOINT = "waypoint";
    private static final String TARGET_REF = "targetRef";
    private static final String SOURCE_REF = "sourceRef";
    private static final String ATTACHED_TO_REF = "attachedToRef";
    private static final String DOCUMENTATION = "documentation";
    private static final String LANE = "lane";
    private static final String LANE_SET = "laneSet";
    private static final String IS_HORIZONTAL = "isHorizontal";
    private static final String TRUE = "true";
    private static final String ID = "id";
    private static final String PROCESS = "process";
    private static final String PROCESS_REF = "processRef";
    private static final String NAME = "name";
    private static final String IS_MAIN_PARTICIPANT = "isMainParticipant";
    private static final String EXTENSION_ELEMENTS = "extensionElements";
    private static final String PARTICIPANT = "participant";
    private static final String COLLABORATION = "collaboration";
    private static final String BPMN_ELEMENT = "bpmnElement";
    private static final String BPMN_PLANE = "BPMNPlane";
    private static final String BPMN_DIAGRAM = "BPMNDiagram";
    private static final String BIZAGI_NAMESPACE = "http://www.bizagi.com/bpmn20";
    private static final String BIZAGI_PROPERTY = "BizagiProperty";
    private static final String BIZAGI_PROPERTIES = "BizagiProperties";
    private static final String BIZAGI_EXTENSIONS = "BizagiExtensions";

    private static Map<String, Element> planeMap = new HashMap<>();
    private static Map<String, GraphElement> geMap = new HashMap<>();
    private static Map<Rectangle, Swimlane> swimlaneBoundMap = new HashMap<>();

    public static void go(IContainer dstFolder, String srcFileName, boolean showSwimlanes, boolean ignoreBendPoints)
            throws Exception {
        try (InputStream is = new FileInputStream(new File(srcFileName))) {
            Document bpmnDocument = XmlUtil.parseWithoutValidation(is);
            Element definitionsElement = bpmnDocument.getRootElement();
            planeMap.clear();
            Element bpmnDiagram = definitionsElement.element(BPMN_DIAGRAM);
            if (bpmnDiagram != null) {
                Element bpmnPlane = bpmnDiagram.element(BPMN_PLANE);
                if (bpmnPlane != null) {
                    for (Element element : (List<Element>) bpmnPlane.elements()) {
                        planeMap.put(element.attributeValue(BPMN_ELEMENT), element);
                    }
                }
            }
            Element processElement = null;
            SwimlaneDisplayMode swimlaneDisplayMode = SwimlaneDisplayMode.none;
            Element collaborationElement = definitionsElement.element(COLLABORATION);
            String processName = collaborationElement.attributeValue(NAME);
            nextParticipant:
            for (Element participant : (List<Element>) collaborationElement.elements(PARTICIPANT)) {
                Element extensionElements = participant.element(EXTENSION_ELEMENTS);
                if (extensionElements != null) {
                    Element bizagiExtensionElements = extensionElements.element(QName.get(BIZAGI_EXTENSIONS, BIZAGI_NAMESPACE));
                    if (bizagiExtensionElements != null) {
                        Element bizagiProperties = bizagiExtensionElements.element(QName.get(BIZAGI_PROPERTIES, BIZAGI_NAMESPACE));
                        if (bizagiProperties != null) {
                            List<Element> properties = bizagiProperties.elements(QName.get(BIZAGI_PROPERTY, BIZAGI_NAMESPACE));
                            for (Element property : properties) {
                                if (IS_MAIN_PARTICIPANT.equals(property.attributeValue(NAME))) {
                                    continue nextParticipant;
                                }
                            }
                        }
                    }
                }
                String processRef = participant.attributeValue(PROCESS_REF);
                for (Element process : (List<Element>) definitionsElement.elements(PROCESS)) {
                    if (processRef.equals(process.attributeValue(ID))) {
                        processElement = process;
                        Element participantShape = planeMap.get(participant.attributeValue(ID));
                        if (participantShape != null) {
                            swimlaneDisplayMode = showSwimlanes
                                    ? (TRUE.equals(participantShape.attributeValue(IS_HORIZONTAL)) ? SwimlaneDisplayMode.horizontal
                                            : SwimlaneDisplayMode.vertical)
                                    : SwimlaneDisplayMode.none;
                        }
                        break;
                    }
                }
                break;
            }
            IFolder folder = IOUtils.getProcessFolder(dstFolder, processName);
            folder.create(true, true, null);
            IFile definitionFile = IOUtils.getProcessDefinitionFile(folder);
            Map<String, String> properties = Maps.newHashMap();
            properties.put(BpmnSerializer.SHOW_SWIMLANE, swimlaneDisplayMode.name());
            properties.put(ProcessSerializer.ACCESS_TYPE, ProcessDefinitionAccessType.Process.name());
            Document document = Language.BPMN.getSerializer().getInitialProcessDefinitionDocument(folder.getName(), properties);
            byte[] bytes = XmlUtil.writeXml(document);
            definitionFile.create(new ByteArrayInputStream(bytes), true, null);
            ProcessDefinition definition = ProcessCache.getProcessDefinition(definitionFile);
            definition.setName(processName);
            definition.setLanguage(Language.BPMN);
            swimlaneBoundMap.clear();
            Element laneSet = processElement.element(LANE_SET);
            if (laneSet != null) {
                for (Element lane : (List<Element>) laneSet.elements(LANE)) {
                    Swimlane swimlane = new Swimlane();
                    String laneName = lane.attributeValue(NAME);
                    swimlane.setName(Strings.isNullOrEmpty(laneName) ? EMPTY_ROLE : laneName);
                    Rectangle bounds = bounds(lane.attributeValue(ID));
                    if (showSwimlanes) {
                        swimlane.setConstraint(bounds);
                    }
                    definition.addChild(swimlane);
                    swimlaneBoundMap.put(bounds, swimlane);
                }
            }
            geMap.clear();
            List<Element> sequenceFlows = new ArrayList<>();
            List<Element> elements = processElement.elements();
            for (Element element : elements) {
                String elementName = element.getName();
                String id = element.attributeValue(ID);
                String name = element.attributeValue(NAME);
                String documentation = element.elementText(DOCUMENTATION);
                switch (elementName) {
                case "startEvent": {
                    StartState start = new StartState();
                    start.setName(name);
                    start.setDescription(documentation);
                    setConstraint(start, id);
                    Swimlane swimlane = swimlane(start);
                    start.setSwimlane(swimlane);
                    if (showSwimlanes) {
                        start.setParentContainer(swimlane);
                        start.getConstraint().translate(swimlane.getConstraint().getTopLeft().negate());
                    }
                    definition.addChild(start);
                    geMap.put(id, start);
                    break;
                }
                case "serviceTask":
                case "task":
                case "manualTask":
                case "userTask": {
                    TaskState task = new TaskState();
                    task.setName(name);
                    task.setDescription(documentation);
                    task.setConstraint(bounds(id));
                    Swimlane swimlane = swimlane(task);
                    task.setSwimlane(swimlane);
                    if (showSwimlanes) {
                        task.setParentContainer(swimlane);
                        task.getConstraint().translate(swimlane.getConstraint().getTopLeft().negate());
                    }
                    definition.addChild(task);
                    geMap.put(id, task);
                    break;
                }
                case "eventBasedGateway":
                case "inclusiveGateway":
                case "exclusiveGateway": {
                    ExclusiveGateway eg = new ExclusiveGateway();
                    eg.setName(name);
                    eg.setDescription(documentation);
                    setConstraint(eg, id);
                    definition.addChild(eg);
                    geMap.put(id, eg);
                    break;
                }
                case "parallelGateway": {
                    ParallelGateway pg = new ParallelGateway();
                    pg.setName(name);
                    pg.setDescription(documentation);
                    setConstraint(pg, id);
                    definition.addChild(pg);
                    geMap.put(id, pg);
                    break;
                }
                case "subProcess": {
                    Subprocess sp = new Subprocess();
                    sp.setName(name);
                    sp.setDescription(documentation);
                    sp.setConstraint(bounds(id));
                    definition.addChild(sp);
                    geMap.put(id, sp);
                    break;
                }
                case "endEvent": {
                    EndState end = new EndState();
                    end.setName(name);
                    end.setDescription(documentation);
                    setConstraint(end, id);
                    definition.addChild(end);
                    geMap.put(id, end);
                    break;
                }
                case "intermediateCatchEvent": {
                    CatchEventNode event = new CatchEventNode();
                    event.setName(name);
                    event.setDescription(documentation);
                    setConstraint(event, id);
                    definition.addChild(event);
                    geMap.put(id, event);
                    break;
                }
                case "intermediateThrowEvent": {
                    ThrowEventNode event = new ThrowEventNode();
                    event.setName(name);
                    event.setDescription(documentation);
                    setConstraint(event, id);
                    definition.addChild(event);
                    geMap.put(id, event);
                    break;
                }
                case "boundaryEvent": {
                    CatchEventNode event = new CatchEventNode();
                    event.setName(name);
                    event.setDescription(documentation);
                    setConstraint(event, id);
                    String attachedToRefId = element.attributeValue(ATTACHED_TO_REF);
                    GraphElement parent = geMap.get(attachedToRefId);
                    parent.addChild(event);
                    event.setParent(parent);
                    event.setParentContainer(parent);
                    geMap.put(id, event);
                    break;
                }
                case "scriptTask": {
                    ScriptTask task = new ScriptTask();
                    task.setName(name);
                    task.setDescription(documentation);
                    task.setConstraint(bounds(id));
                    definition.addChild(task);
                    geMap.put(id, task);
                    break;
                }
                case "sequenceFlow": {
                    sequenceFlows.add(element);
                    break;
                }
                case DOCUMENTATION:
                case ASSOCIATION:
                case EXTENSION_ELEMENTS:
                case LANE_SET: {
                    // Do nothing
                    break;
                }
                // Undefined elements
                case "callActivity": {
                    ScriptTask task = new ScriptTask();
                    task.setName("?" + elementName + "? " + name);
                    task.setDescription(documentation);
                    task.setConstraint(bounds(id));
                    definition.addChild(task);
                    geMap.put(id, task);
                    break;
                }
                default: {
                    TextAnnotation annotation = new TextAnnotation();
                    annotation.setConstraint(bounds(id));
                    annotation.setDescription("?" + elementName + "? - " + name);
                    definition.addChild(annotation);
                    geMap.put(id, annotation);
                }
                }
            }
            for (Element element : sequenceFlows) {
                String id = element.attributeValue(ID);
                String name = element.attributeValue(NAME);
                String sourceRef = element.attributeValue(SOURCE_REF);
                String targetRef = element.attributeValue(TARGET_REF);
                NodeTypeDefinition transitionDefinition = NodeRegistry.getNodeTypeDefinition(Transition.class);
                Node source = (Node) geMap.get(sourceRef);
                Transition transition = transitionDefinition.createElement(source, false);
                transition.setName(Strings.isNullOrEmpty(name) ? source.getNextTransitionName(transitionDefinition) : name);
                transition.setTarget((Node) geMap.get(targetRef));
                source.addLeavingTransition(transition);
                if (!ignoreBendPoints) {
                    transition.setBendpoints(waypoints(id));
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
            Element bounds = shape.element(BOUNDS);
            if (bounds != null) {
                return new PrecisionRectangle(Double.parseDouble(bounds.attributeValue(X)), Double.parseDouble(bounds.attributeValue(Y)),
                        Double.parseDouble(bounds.attributeValue(WIDTH)), Double.parseDouble(bounds.attributeValue(HEIGHT)));
            }
        }
        throw new IllegalStateException("id: " + id + " does not exist");
    }

    private static List<Point> waypoints(String id) {
        Element shape = planeMap.get(id);
        if (shape != null) {
            List<Point> bendPoints = Lists.newArrayList();
            List<Element> waypoints = shape.elements(WAYPOINT);
            if (waypoints != null) {
                for (Element waypoint : waypoints) {
                    bendPoints.add(
                            new PrecisionPoint(Double.parseDouble(waypoint.attributeValue(X)), Double.parseDouble(waypoint.attributeValue(Y))));
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
