package ru.runa.gpd.bizagi.converter;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;
import org.dom4j.Document;
import org.dom4j.Element;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PrecisionPoint;
import org.eclipse.draw2d.geometry.Rectangle;
import ru.runa.gpd.Application;
import ru.runa.gpd.editor.GEFConstants;
import ru.runa.gpd.lang.model.Describable;
import ru.runa.gpd.lang.model.EndState;
import ru.runa.gpd.lang.model.EndTokenState;
import ru.runa.gpd.lang.model.GraphElement;
import ru.runa.gpd.lang.model.ITimed;
import ru.runa.gpd.lang.model.MultiSubprocess;
import ru.runa.gpd.lang.model.MultiTaskState;
import ru.runa.gpd.lang.model.NamedGraphElement;
import ru.runa.gpd.lang.model.Node;
import ru.runa.gpd.lang.model.ProcessDefinition;
import ru.runa.gpd.lang.model.StartState;
import ru.runa.gpd.lang.model.Subprocess;
import ru.runa.gpd.lang.model.SubprocessDefinition;
import ru.runa.gpd.lang.model.Swimlane;
import ru.runa.gpd.lang.model.TaskState;
import ru.runa.gpd.lang.model.Timer;
import ru.runa.gpd.lang.model.Transition;
import ru.runa.gpd.lang.model.bpmn.CatchEventNode;
import ru.runa.gpd.lang.model.bpmn.EventNodeType;
import ru.runa.gpd.lang.model.bpmn.ExclusiveGateway;
import ru.runa.gpd.lang.model.bpmn.IBoundaryEventContainer;
import ru.runa.gpd.lang.model.bpmn.ParallelGateway;
import ru.runa.gpd.lang.model.bpmn.ScriptTask;
import ru.runa.gpd.lang.model.bpmn.TextAnnotation;
import ru.runa.gpd.lang.model.bpmn.ThrowEventNode;
import ru.runa.gpd.util.IOUtils;
import ru.runa.gpd.util.SwimlaneDisplayMode;
import ru.runa.gpd.util.XmlUtil;

public class BpmnExporter implements GEFConstants {

    private static final int PARTICIPANT_SHAPE_DX = 30;
    private static final int PARTICIPANT_SHAPE_DY = 30;

    private static final String COLON = ":";

    private static final String BPMN_PREFIX = "bpmn";
    private static final String BPMN_NAMESPACE = "http://www.omg.org/spec/BPMN/20100524/MODEL";
    private static final String BPMNDI_PREFIX = "bpmndi";
    private static final String BPMNDI_NAMESPACE = "http://www.omg.org/spec/BPMN/20100524/DI";
    private static final String OMGDC_PREFIX = "omgdc";
    private static final String OMGDC_NAMESPACE = "http://www.omg.org/spec/DD/20100524/DC";
    private static final String OMGDI_PREFIX = "omgdi";
    private static final String OMGDI_NAMESPACE = "http://www.omg.org/spec/DD/20100524/DI";
    private static final String XSI_PREFIX = "xsi";
    private static final String XSI_NAMESPACE = "http://www.w3.org/2001/XMLSchema-instance";

    private static final String ELEM_DOCUMENTATION = "documentation";
    private static final String ELEM_DEFINITIONS = "definitions";
    private static final String ELEM_COLLABORATION = "collaboration";
    private static final String ELEM_PARTICIPANT = "participant";
    private static final String ELEM_PROCESS = "process";
    private static final String ELEM_LANE_SET = "laneSet";
    private static final String ELEM_LANE = "lane";
    private static final String ELEM_FLOW_NODE_REF = "flowNodeRef";
    private static final String ELEM_SEQUENCE_FLOW = "sequenceFlow";
    private static final String ELEM_OUTGOING = "outgoing";
    private static final String ELEM_INCOMING = "incoming";
    private static final String ELEM_START_EVENT = "startEvent";
    private static final String ELEM_END_EVENT = "endEvent";
    private static final String ELEM_MULTI_INSTANCE_LOOP_CHARACTERISTICS = "MultiInstanceLoopCharacteristics";
    private static final String ELEM_USER_TASK = "userTask";
    private static final String ELEM_EXCLUSIVE_GATEWAY = "exclusiveGateway";
    private static final String ELEM_PARALLEL_GATEWAY = "parallelGateway";
    private static final String ELEM_BOUNDARY_EVENT = "boundaryEvent";
    private static final String ELEM_INTERMEDIATE_THROW_EVENT = "intermediateThrowEvent";
    private static final String ELEM_INTERMEDIATE_CATCH_EVENT = "intermediateCatchEvent";
    private static final String ELEM_SCRIPT_TASK = "scriptTask";
    private static final String ELEM_SUBPROCESS = "subProcess";
    private static final String ELEM_TEXT_ANNOTATION = "textAnnotation";
    private static final String ELEM_TEXT = "text";
    private static final String ELEM_TIMER_EVENT_DEFINITION = "timerEventDefinition";
    private static final String ELEM_TIME_DURATION = "timeDuration";
    private static final String ELEM_MESSAGE_EVENT_DEFINITION = "messageEventDefinition";
    private static final String ELEM_SIGNAL_EVENT_DEFINITION = "signalEventDefinition";
    private static final String ELEM_CANCEL_EVENT_DEFINITION = "cancelEventDefinition";
    private static final String ELEM_ERROR_EVENT_DEFINITION = "errorEventDefinition";
    private static final String ELEM_TERMINATE_EVENT_DEFINITION = "terminateEventDefinition";

    private static final String ELEM_BPMN_DIAGRAM = "BPMNDiagram";
    private static final String ELEM_BPMN_PLANE = "BPMNPlane";
    private static final String ELEM_BPMN_SHAPE = "BPMNShape";
    private static final String ELEM_BPMN_EDGE = "BPMNEdge";
    private static final String ELEM_BOUNDS = "Bounds";
    private static final String ELEM_WAYPOINT = "waypoint";

    private static final String ATTR_TARGET_NAMESPACE = "targetNamespace";
    private static final String ATTR_EXPORTER = "exporter";
    private static final String ATTR_EXPORTER_VERSION = "exporterVersion";
    private static final String ATTR_ID = "id";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_EXECUTABLE = "isExecutable";
    private static final String ATTR_BPMN_ELEMENT = "bpmnElement";
    private static final String ATTR_HORIZONTAL = "isHorizontal";
    private static final String ATTR_GATEWAY_DIRECTION = "gatewayDirection";
    private static final String ATTR_PROCESS_REF = "processRef";
    private static final String ATTR_SOURCE_REF = "sourceRef";
    private static final String ATTR_TARGET_REF = "targetRef";
    private static final String ATTR_DEFAULT = "default";
    private static final String ATTR_CANCEL_ACTIVITY = "cancelActivity";
    private static final String ATTR_ATTACHED_TO_REF = "attachedToRef";
    private static final String ATTR_SEQUENTIAL = "isSequential";

    private static final String ATTR_X = "x";
    private static final String ATTR_Y = "y";
    private static final String ATTR_WIDTH = "width";
    private static final String ATTR_HEIGHT = "height";

    private static final Dimension MINIMIZED_NODE_SIZE = new Dimension(3 * GRID_SIZE, 3 * GRID_SIZE);

    private enum GatewayDirection {Unspecified, Converging, Diverging, Mixed}

    private boolean expandMinimizedNodes = true;
    private boolean horizontalSwimlanes = true;
    private Rectangle participantConstraint = null;
    private List<Transition> transitions = null;
    private Document document;
    private Element definitions;
    private Element collaboration;
    private Element process;
    private Element diagram;
    private Element plane;

    public void go(ProcessDefinition pd, String outputFileName, boolean expandMinimizedNodes) throws Exception {
        this.expandMinimizedNodes = expandMinimizedNodes;
        if (pd.getSwimlaneDisplayMode() != SwimlaneDisplayMode.none) {
            horizontalSwimlanes = pd.getSwimlaneDisplayMode() == SwimlaneDisplayMode.horizontal;
        }
        
        document = XmlUtil.createDocument(bpmn(ELEM_DEFINITIONS));

        definitions = document.getRootElement();
        definitions.addNamespace(BPMN_PREFIX, BPMN_NAMESPACE);
        definitions.addNamespace(BPMNDI_PREFIX, BPMNDI_NAMESPACE);
        definitions.addNamespace(OMGDC_PREFIX, OMGDC_NAMESPACE);
        definitions.addNamespace(OMGDI_PREFIX, OMGDI_NAMESPACE);
        definitions.addNamespace(XSI_PREFIX, XSI_NAMESPACE);
        definitions.addAttribute(ATTR_TARGET_NAMESPACE, "http://bpmn.io/schema/bpmn");
        definitions.addAttribute(ATTR_EXPORTER, "RunaWFE Developer Studio");
        definitions.addAttribute(ATTR_EXPORTER_VERSION, Application.getVersion().toString());

        process = definitions.addElement(bpmn(ELEM_PROCESS));
        String processId = pd.getId() == null ? id() : pd.getId();
        process.addAttribute(ATTR_ID, processId);
        process.addAttribute(ATTR_NAME, pd.getName());
        if (pd.isInvalid()) {
            process.addAttribute(ATTR_EXECUTABLE, "false");
        }

        collaboration = definitions.addElement(bpmn(ELEM_COLLABORATION));
        String collaborationId = id();
        collaboration.addAttribute(ATTR_ID, collaborationId);
        Element participant = collaboration.addElement(bpmn(ELEM_PARTICIPANT));
        String participantId = id();
        participant.addAttribute(ATTR_ID, participantId);
        participant.addAttribute(ATTR_NAME, pd.getName());
        participant.addAttribute(ATTR_PROCESS_REF, processId);

        diagram = definitions.addElement(bpmndi(ELEM_BPMN_DIAGRAM));
        plane = diagram.addElement(bpmndi(ELEM_BPMN_PLANE));
        plane.addAttribute(ATTR_BPMN_ELEMENT, collaborationId);

        if (pd.getClass() != SubprocessDefinition.class) {
            Element laneSet = null;
            List<Swimlane> swimlanes = pd.getSwimlanes();
            for (Swimlane swimlane : swimlanes) {
                if (participantConstraint == null) {
                    if (swimlane.getConstraint() == null) { // no lanes
                        participantConstraint = pd.getConstraint().getCopy().setSize(0, 0);
                        for (GraphElement e : pd.getChildren(GraphElement.class)) {
                            participantConstraint.union(e.getConstraint());
                        }
                        participantConstraint.resize(PARTICIPANT_SHAPE_DX, PARTICIPANT_SHAPE_DY);
                        break;
                    } else {
                        participantConstraint = swimlane.getConstraint().getCopy();
                    }
                } else {
                    participantConstraint.union(swimlane.getConstraint().getCopy());
                }
                if (laneSet == null) {
                    laneSet = process.addElement(bpmn(ELEM_LANE_SET)).addAttribute(ATTR_ID, "laneSet1");
                }
                Element lane = laneSet.addElement(bpmn(ELEM_LANE));
                List<GraphElement> elements = pd.getContainerElements(swimlane);
                for (GraphElement ge : elements) {
                    lane.addElement(bpmn(ELEM_FLOW_NODE_REF)).addText(ge.getId());
                }
                baseProperties(lane, swimlane);
            }
        }
        if (participantConstraint != null) {
            plane.addElement(bpmndi(ELEM_BPMN_SHAPE)).addAttribute(ATTR_BPMN_ELEMENT, participantId)
                    .addAttribute(ATTR_HORIZONTAL, String.valueOf(horizontalSwimlanes))
                    .addElement(omgdc(ELEM_BOUNDS))
                    .addAttribute(ATTR_X, String.valueOf(participantConstraint.x - (horizontalSwimlanes ? PARTICIPANT_SHAPE_DX : 0)))
                    .addAttribute(ATTR_Y, String.valueOf(participantConstraint.y - (horizontalSwimlanes ? 0 : PARTICIPANT_SHAPE_DY)))
                    .addAttribute(ATTR_WIDTH, String.valueOf(participantConstraint.width + (horizontalSwimlanes ? PARTICIPANT_SHAPE_DX : 0)))
                    .addAttribute(ATTR_HEIGHT, String.valueOf(participantConstraint.height + (horizontalSwimlanes ? 0 : PARTICIPANT_SHAPE_DY)));
        }
        transitions = pd.getChildrenRecursive(Transition.class);
        for (GraphElement ge : pd.getChildren(GraphElement.class)) {
            shape(ge);
            boolean noName = false;
            Element e = null;
            if (ge instanceof StartState) {
                e = process.addElement(bpmn(ELEM_START_EVENT));
            }
            else if (ge instanceof ExclusiveGateway) {
                e = process.addElement(bpmn(ELEM_EXCLUSIVE_GATEWAY));
                noName = true;
            }
            else if (ge instanceof ParallelGateway) {
                e = process.addElement(bpmn(ELEM_PARALLEL_GATEWAY));
                noName = true;
            }
            else if (ge instanceof TaskState) {
                TaskState ce = (TaskState) ge;
                e = process.addElement(bpmn(ELEM_USER_TASK));
                if (ce instanceof MultiTaskState) {
                    multiInstance(e);
                }
                boundaryTimer(e, ce);
                boundaryEvents(e, ce);
            }
            else if (ge instanceof Timer) {
                Timer ce = (Timer) ge;
                e = process.addElement(bpmn(ELEM_INTERMEDIATE_CATCH_EVENT));
                e.addElement(bpmn(ELEM_TIMER_EVENT_DEFINITION));
                boundaryEvents(e, ce);
            }
            else if (ge instanceof ScriptTask) {
                ScriptTask ce = (ScriptTask) ge;
                e = process.addElement(bpmn(ELEM_SCRIPT_TASK));
                boundaryEvents(e, ce);
            }
            else if (ge instanceof Subprocess) {
                Subprocess ce = (Subprocess) ge;
                e = process.addElement(bpmn(ELEM_SUBPROCESS));
                if (ce instanceof MultiSubprocess) {
                    multiInstance(e);
                }
                boundaryEvents(e, ce);
            }
            else if (ge instanceof ThrowEventNode) {
                ThrowEventNode ce = (ThrowEventNode) ge;
                e = process.addElement(bpmn(ELEM_INTERMEDIATE_THROW_EVENT));
                e.addElement(bpmn(eventType(ce.getEventNodeType())));
                boundaryEvents(e, ce);
                noName = true;
            }
            else if (ge instanceof CatchEventNode) {
                CatchEventNode ce = (CatchEventNode) ge;
                e = process.addElement(bpmn(ELEM_INTERMEDIATE_CATCH_EVENT));
                e.addElement(bpmn(eventType(ce.getEventNodeType())));
                boundaryTimer(e, ce);
                boundaryEvents(e, ce);
                noName = true;
            }
            else if (ge instanceof TextAnnotation) {
                e = process.addElement(bpmn(ELEM_TEXT_ANNOTATION));
            }
            else if (ge instanceof EndTokenState) {
                e = process.addElement(bpmn(ELEM_END_EVENT));
            }
            else if (ge instanceof EndState) {
                e = process.addElement(bpmn(ELEM_END_EVENT));
                e.addElement(bpmn(ELEM_TERMINATE_EVENT_DEFINITION));
            }
            if (e != null) {
                baseProperties(e, ge, !noName);
                if (ge instanceof Node) {
                    transitions(e, (Node) ge);
                }
            }
        }

        byte[] bytes = XmlUtil.writeXml(document);

        try (InputStream is = new ByteArrayInputStream(bytes); OutputStream os = new FileOutputStream(outputFileName)) {
            IOUtils.copyStream(is, os);
        }
    }

    private void multiInstance(Element element) {
        element.addElement(bpmn(ELEM_MULTI_INSTANCE_LOOP_CHARACTERISTICS)).addAttribute(ATTR_SEQUENTIAL, Boolean.FALSE.toString());
    }

    private void boundaryEvents(Element processElement, IBoundaryEventContainer boundaryEventContainer) {
        List<CatchEventNode> catchEventNodes = ((GraphElement) boundaryEventContainer).getChildren(CatchEventNode.class);
        for (CatchEventNode eventNode : catchEventNodes) {
            shape(eventNode);
            Element boundaryEvent = process.addElement(bpmn(ELEM_BOUNDARY_EVENT));
            baseProperties(boundaryEvent, eventNode, false);
            boundaryEvent.addAttribute(ATTR_CANCEL_ACTIVITY, String.valueOf(eventNode.isInterruptingBoundaryEvent()));
            boundaryEvent.addAttribute(ATTR_ATTACHED_TO_REF, eventNode.getParent().getId());
            boundaryEvent.addElement(bpmn(eventType(eventNode.getEventNodeType())));
            transitions(boundaryEvent, eventNode);
        }
    }

    private String eventType(EventNodeType nodeType) {
        switch (nodeType) {
        case signal:
            return ELEM_SIGNAL_EVENT_DEFINITION;
        case cancel:
            return ELEM_CANCEL_EVENT_DEFINITION;
        case error:
            return ELEM_ERROR_EVENT_DEFINITION;
        default:
            return ELEM_MESSAGE_EVENT_DEFINITION;
        }
    }

    private void timer(Element element, Timer timer) {
        if (timer == null) {
            return;
        }
        baseProperties(element, timer);
        Element eventDefinitionElement = element.addElement(bpmn(ELEM_TIMER_EVENT_DEFINITION));
        Element durationElement = eventDefinitionElement.addElement(bpmn(ELEM_TIME_DURATION));
        durationElement.addText(timer.getDelay().getDuration());
    }

    private void boundaryTimer(Element element, ITimed timed) {
        Timer timer = timed.getTimer();
        if (timer == null) {
            return;
        }
        shape(timer);
        Element boundaryEvent = process.addElement(bpmn(ELEM_BOUNDARY_EVENT));
        timer(boundaryEvent, timer);
        boundaryEvent.addAttribute(ATTR_CANCEL_ACTIVITY, String.valueOf(timer.isInterruptingBoundaryEvent()));
        boundaryEvent.addAttribute(ATTR_ATTACHED_TO_REF, ((GraphElement) timed).getId());
        transitions(boundaryEvent, timer);
    }

    private void baseProperties(Element element, GraphElement graphElement) {
        baseProperties(element, graphElement, true);
    }

    private void baseProperties(Element element, GraphElement graphElement, boolean withName) {
        setAttribute(element, ATTR_ID, graphElement.getId());
        if (withName && graphElement instanceof NamedGraphElement) {
            if (!(graphElement instanceof Node) || expandMinimizedNodes || !((Node) graphElement).isMinimizedView()) {
                setAttribute(element, ATTR_NAME,
                        graphElement instanceof Timer ? ((Timer) graphElement).getDelay().toString() : ((NamedGraphElement) graphElement).getName());
            }
        }
        if (graphElement instanceof Describable) {
            String description = ((Describable) graphElement).getDescription();
            if (!Strings.isNullOrEmpty(description)) {
                element.addElement(graphElement instanceof TextAnnotation ? ELEM_TEXT : ELEM_DOCUMENTATION).addCDATA(description);
            }
        }
    }

    private void setAttribute(Element node, String attributeName, String attributeValue) {
        if (attributeValue != null) {
            node.addAttribute(attributeName, attributeValue);
        }
    }

    private void transitions(Element owner, Node node) {
        int incomingCount = 0;
        for (Transition transition : transitions) {
            if (transition.getTarget() == node) {
                owner.addElement(bpmn(ELEM_INCOMING)).setText(transition.getId());
                incomingCount++;
            }
        }
        int outgoingCount = 0;
        List<Transition> transitions = node.getLeavingTransitions();
        for (Transition transition : transitions) {
            if (transition.isDefaultFlow()) {
                owner.addAttribute(ATTR_DEFAULT, transition.getId());
            }
            owner.addElement(bpmn(ELEM_OUTGOING)).setText(transition.getId());
            Element transitionElement = process.addElement(bpmn(ELEM_SEQUENCE_FLOW));
            baseProperties(transitionElement, transition, !(node instanceof ParallelGateway) && transitions.size() > 1);
            String sourceNodeId = transition.getSource().getId();
            String targetNodeId = transition.getTarget().getId();
            if (Objects.equal(sourceNodeId, targetNodeId)) {
                throw new IllegalArgumentException("Invalid transition " + transition);
            }
            transitionElement.addAttribute(ATTR_SOURCE_REF, sourceNodeId);
            transitionElement.addAttribute(ATTR_TARGET_REF, targetNodeId);

            List<Point> waypoints = Lists.newArrayList();

            Rectangle parentBound = null;
            Point startPoint = null;
            Rectangle startBound = null;
            if (transition.getSource().getParentContainer() instanceof IBoundaryEventContainer) {
                Rectangle laneConstraints = parentBound(transition.getSource().getParentContainer()).getCopy();
                parentBound = transition.getSource().getParentContainer().getConstraint().getCopy();
                parentBound.translate(laneConstraints.getTopLeft());
                startBound = transition.getSource().getConstraint().getCopy().translate(parentBound.getTopLeft());
                startBound.setSize(startBound.width / 2, startBound.height / 2);
                if (node instanceof Timer) {
                    startBound.setLocation(parentBound.x, parentBound.y + parentBound.height - startBound.height);
                } else {
                    startBound.setLocation(parentBound.x + parentBound.width - startBound.width,
                            parentBound.y + parentBound.height - startBound.height);
                }
            } else if (transition.getSource().getParentContainer() instanceof Swimlane) {
                parentBound = transition.getSource().getParentContainer().getConstraint().getCopy();
                startBound = transition.getSource().getConstraint().getCopy().translate(parentBound.getTopLeft());
            } else { // ProcessDefinition
                startBound = bound(transition.getSource());
            }
            if (transition.getSource() instanceof Node && !expandMinimizedNodes && transition.getSource().isMinimizedView()) {
                startBound.setSize(MINIMIZED_NODE_SIZE);
            }
            startPoint = new Point(startBound.x + startBound.width / 2, startBound.y + startBound.height / 2);
            waypoints.add(startPoint);

            for (Point bendPoint : transition.getBendpoints()) {
                waypoints.add(bendPoint);
            }

            Rectangle endBound = null;
            if (transition.getTarget().getParentContainer() != null) {
                parentBound = transition.getTarget().getParentContainer().getConstraint().getCopy();
                endBound = transition.getTarget().getConstraint().getCopy().translate(parentBound.getTopLeft());
            } else { // ProcessDefinition
                endBound = bound(transition.getTarget());
            }
            if (transition.getTarget() instanceof Node && !expandMinimizedNodes && transition.getTarget().isMinimizedView()) {
                endBound.setSize(MINIMIZED_NODE_SIZE);
            }
            Point endPoint = new Point(endBound.x + endBound.width / 2, endBound.y + endBound.height / 2);
            waypoints.add(endPoint);

            Point intersectionPoint = intersectionPoint(startPoint, waypoints.get(1), startBound);
            if (intersectionPoint != null) {
                waypoints.set(0, intersectionPoint);
            }

            startPoint = waypoints.get(waypoints.size() - 2);
            intersectionPoint = intersectionPoint(startPoint, endPoint, endBound);
            if (intersectionPoint != null) {
                waypoints.set(waypoints.size() - 1, intersectionPoint);
            }
            Element edge = plane.addElement(bpmndi(ELEM_BPMN_EDGE)).addAttribute(ATTR_BPMN_ELEMENT, transition.getId());
            for (Point waypoint : waypoints) {
                edge.addElement(omgdi(ELEM_WAYPOINT)).addAttribute(ATTR_X, String.valueOf(waypoint.x)).addAttribute(ATTR_Y,
                        String.valueOf(waypoint.y));
            }
            outgoingCount++;
        }
        if (node instanceof ExclusiveGateway || node instanceof ParallelGateway) {
            GatewayDirection direction = GatewayDirection.Unspecified;
            if (incomingCount > 1) {
                if (outgoingCount > 1) {
                    direction = GatewayDirection.Mixed;
                } else {
                    direction = GatewayDirection.Converging;
                }
            } else {
                if (outgoingCount > 1) {
                    direction = GatewayDirection.Diverging;
                }
            }
            owner.addAttribute(ATTR_GATEWAY_DIRECTION, direction.name());
        }
    }

    private Rectangle bound(Node node) {
        Rectangle bound = node.getConstraint().getCopy();
        if (!node.getTypeDefinition().getGraphitiEntry().isFixedSize()) {
            if (!expandMinimizedNodes && node.isMinimizedView()) {
                bound.setSize(MINIMIZED_NODE_SIZE);
            } else {
                bound.shrink(GRID_SIZE / 2, GRID_SIZE / 2);
            }
        }
        return bound;
    }

    private Point intersectionPoint(Point startPoint, Point endPoint, Rectangle bound) {
        Point intersectionPoint = null;
        intersectionPoint = intersectionPoint(startPoint, endPoint, bound.getTopLeft(), bound.getTopRight());
        if (intersectionPoint == null) {
            intersectionPoint = intersectionPoint(startPoint, endPoint, bound.getTopRight(), bound.getBottomRight());
            if (intersectionPoint == null) {
                intersectionPoint = intersectionPoint(startPoint, endPoint, bound.getBottomLeft(), bound.getBottomRight());
                if (intersectionPoint == null) {
                    intersectionPoint = intersectionPoint(startPoint, endPoint, bound.getTopLeft(), bound.getBottomLeft());
                }
            }
        }
        return intersectionPoint;
    }

    //
    // source: https://vscode.ru/prog-lessons/nayti-tochku-peresecheniya-otrezkov.html
    //
    private Point intersectionPoint(Point p1, Point p2, Point p3, Point p4) {
        // подразумеваем, что начальная точка находится левее конечной относительно оси абсцисс (оси X)
        if (p2.x < p1.x) {
            Point p = p1;
            p1 = p2;
            p2 = p;
        }
        if (p4.x < p3.x) {
            Point p = p3;
            p3 = p4;
            p4 = p;
        }
        if (p2.x < p3.x) {
            return null;
        }
        // если оба отрезка вертикальные
        if ((p1.x - p2.x == 0) && (p3.x - p4.x == 0)) {
            /*! never
            // если они лежат на одном X
            if (p1.x == p3.x) {
                // проверим пересекаются ли они, т.е. есть ли у них общий Y для этого возьмём отрицание от случая, когда они НЕ пересекаются
                if (!((Math.max(p1.y, p2.y) < Math.min(p3.y, p4.y)) || (Math.min(p1.y, p2.y) > Math.max(p3.y, p4.y)))) {
                    return true;
                }
            }
            */
            return null;
        }
        // если первый отрезок вертикальный
        if (p1.x - p2.x == 0) {
            // найдём Xa, Ya - точки пересечения двух прямых
            double Xa = p1.x;
            double A2 = (double) (p3.y - p4.y) / (p3.x - p4.x);
            double b2 = p3.y - A2 * p3.x;
            double Ya = A2 * Xa + b2;
            if (p3.x <= Xa && p4.x >= Xa && Math.min(p1.y, p2.y) <= Ya && Math.max(p1.y, p2.y) >= Ya) {
                return new PrecisionPoint(Xa, Ya);
            }
            return null;
        }
        // если второй отрезок вертикальный
        if (p3.x - p4.x == 0) {
            // найдём Xa, Ya - точки пересечения двух прямых
            double Xa = p3.x;
            double A1 = (double) (p1.y - p2.y) / (p1.x - p2.x);
            double b1 = p1.y - A1 * p1.x;
            double Ya = A1 * Xa + b1;
            if (p1.x <= Xa && p2.x >= Xa && Math.min(p3.y, p4.y) <= Ya && Math.max(p3.y, p4.y) >= Ya) {
                return new PrecisionPoint(Xa, Ya);
            }
            return null;
        }
        // оба отрезка невертикальные
        double A1 = (double) (p1.y - p2.y) / (p1.x - p2.x);
        double A2 = (double) (p3.y - p4.y) / (p3.x - p4.x);
        double b1 = p1.y - A1 * p1.x;
        double b2 = p3.y - A2 * p3.x;
        if (A1 == A2) {
            return null; // отрезки параллельны
        }
        // Xa - абсцисса точки пересечения двух прямых
        double Xa = (b2 - b1) / (A1 - A2);
        if ((Xa < Math.max(p1.x, p3.x)) || (Xa > Math.min(p2.x, p4.x))) {
            return null; // точка Xa находится вне пересечения проекций отрезков на ось X
        } else {
            return new PrecisionPoint(Xa, A1 * Xa + b1);
        }
    }

    private void shape(GraphElement graphElement) {
        if (graphElement.getConstraint() != null) {
            Rectangle constraints = graphElement.getConstraint().getCopy();
            if (graphElement.getParentContainer() != null) {
                GraphElement parent = graphElement.getParentContainer();
                Rectangle parentConstraints = parent.getConstraint().getCopy();
                if (parent instanceof IBoundaryEventContainer) {
                    Rectangle laneConstraints = parentBound(parent).getCopy();
                    parentConstraints.translate(laneConstraints.getTopLeft());
                    constraints.setSize(constraints.width / 2, constraints.height / 2);
                    if (graphElement instanceof Timer) {
                        constraints.setLocation(parentConstraints.x, parentConstraints.y + parentConstraints.height - constraints.height);
                    } else {
                        constraints.setLocation(parentConstraints.x + parentConstraints.width - constraints.width,
                                parentConstraints.y + parentConstraints.height - constraints.height);
                    }
                } else {
                    constraints.translate(parentConstraints.getTopLeft().getCopy().translate(GRID_SIZE / 2, GRID_SIZE / 2));
                    if (graphElement instanceof TextAnnotation) {
                        constraints.setSize(constraints.width - GRID_SIZE / 2, constraints.height - GRID_SIZE / 2);
                    } else {
                        constraints.setSize(constraints.width - GRID_SIZE, constraints.height - GRID_SIZE);
                    }
                }
            } else if (graphElement instanceof Node) {
                constraints = bound((Node) graphElement);
            }
            if (graphElement instanceof Node && !expandMinimizedNodes && ((Node) graphElement).isMinimizedView()) {
                constraints.setSize(MINIMIZED_NODE_SIZE);
            }
            Element shape = plane.addElement(bpmndi(ELEM_BPMN_SHAPE));
            shape.addAttribute(ATTR_BPMN_ELEMENT, graphElement.getId()).addElement(omgdc(ELEM_BOUNDS))
                    .addAttribute(ATTR_X, String.valueOf(constraints.x)).addAttribute(ATTR_Y, String.valueOf(constraints.y))
                    .addAttribute(ATTR_WIDTH, String.valueOf(constraints.width)).addAttribute(ATTR_HEIGHT, String.valueOf(constraints.height));
            if (graphElement instanceof Swimlane) {
                shape.addAttribute(ATTR_HORIZONTAL, String.valueOf(horizontalSwimlanes));
            }
        }
    }

    private Rectangle parentBound(GraphElement element) {
        GraphElement parentContainer = element.getParentContainer();
        if (parentContainer == null) {
            if (participantConstraint != null) {
                return participantConstraint;
            } else {
                parentContainer = element.getParent();
            }
        }
        return parentContainer.getConstraint();
    }

    private String id() {
        return "id_" + UUID.randomUUID();
    }

    private String bpmn(String nodeName) {
        return BPMN_PREFIX + COLON + nodeName;
    }

    private String bpmndi(String nodeName) {
        return BPMNDI_PREFIX + COLON + nodeName;
    }

    private String omgdc(String nodeName) {
        return OMGDC_PREFIX + COLON + nodeName;
    }

    private String omgdi(String nodeName) {
        return OMGDI_PREFIX + COLON + nodeName;
    }

}
