/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ltde.gephi.io.spigot.plugin.email;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import javax.mail.Address;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.gephi.data.attributes.api.AttributeColumn;
import org.gephi.data.attributes.api.AttributeOrigin;
import org.gephi.data.attributes.api.AttributeRow;
import org.gephi.data.attributes.api.AttributeType;
import org.gephi.data.attributes.type.DynamicDouble;
import org.gephi.data.attributes.type.Interval;
import org.gephi.data.properties.PropertiesColumn;
import org.gephi.io.importer.api.ContainerLoader;
import org.gephi.io.importer.api.EdgeDraft;
import org.gephi.io.importer.api.EdgeDraftGetter;
import org.gephi.io.importer.api.Issue;
import org.gephi.io.importer.api.NodeDraft;
import org.gephi.io.importer.api.Report;
import org.gephi.io.importer.spi.SpigotImporter;
import org.gephi.utils.longtask.spi.LongTask;
import org.gephi.utils.progress.Progress;
import org.gephi.utils.progress.ProgressTicket;
import org.openide.util.Exceptions;

/**
 *
 * @author Hendrik
 */
public class DynamicMailSpigot implements SpigotImporter, LongTask {

    private static final Logger logger = Logger.getLogger(DynamicMailSpigot.class.getName());
    private ContainerLoader container;
    private Report report;
    private ProgressTicket progressTicket;
    private boolean cancel = false;
    private File[] files;
    private boolean hasCcAsWeight;
    private boolean hasBccAsWeight;
    private AttributeColumn dynCol;
    private HashMap<Pair<NodeDraft, NodeDraft>, DynamicDouble> edgesProto;
    private double HIGH_DATE = 2100d;
    private double LOW_DATE = 1900d;

    @Override
    public boolean execute(ContainerLoader loader) {
        System.setProperty(logger.getName() + ".level", "" + Level.ALL.intValue());
        try {
            LogManager.getLogManager().readConfiguration();
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } catch (SecurityException ex) {
            Exceptions.printStackTrace(ex);
        }
//        logger.setLevel();
        this.container = loader;
        this.report = new Report();
        Progress.start(progressTicket);
        doImportNew();
        Progress.finish(progressTicket);

        return !cancel;
    }

    @Override
    public ContainerLoader getContainer() {
        return container;
    }

    @Override
    public Report getReport() {
        return report;
    }

    @Override
    public boolean cancel() {
        cancel = true;
        return true;
    }

    @Override
    public void setProgressTicket(ProgressTicket progressTicket) {
        this.progressTicket = progressTicket;
    }

    private Double convertToDouble(Date sentDate) {
//        Date d = msg.getSentDate();
        Calendar c = new GregorianCalendar();
        c.setTime(sentDate);
//        log(c.toString());



        int y = c.get(Calendar.YEAR);
        int doy = c.get(Calendar.DAY_OF_YEAR);
        int hr = c.get(Calendar.HOUR_OF_DAY);
        int min = c.get(Calendar.MINUTE);
        int sec = c.get(Calendar.SECOND);
        int subsec = c.get(Calendar.MILLISECOND);

        Double converted = new Double(subsec / 1000);
        converted = (converted + sec) / 60;
        converted = (converted + min) / 60;
        converted = (converted + hr) / 12;
        converted = (converted + doy) / 366;
        converted = converted + y;
//        log("converted sub " + converted);
//        log("converted sec " + converted);
//        log("converted min " + converted);
//        log("converted hrs " + converted);
//        log("converted doy " + converted);
//        log("converted yrs " + converted);
//        = new Double(y + (doy + (hr + (min + (sec + (subsec) / 1000) / 60) / 60) / 24) / 365);
//        log("date converted to " + converted);
//        log(sentDate.toGMTString() + " = " + sentDate.getTime() + "\t\t converted to\t" + converted);
        return converted;

    }

    static {
        logger.addHandler(new ConsoleHandler());
        logger.setLevel(Level.ALL);
        Enumeration<String> loggerNames = LogManager.getLogManager().getLoggerNames();
        while (loggerNames.hasMoreElements()) {
            System.out.println(loggerNames.nextElement());
        }
    }

    private void log(String string) {
        report.log(string);
        logger.info(string);
//        InputOutput io = IOProvider.getDefault().getIO("Hello", true);
//        io.getOut().println("Hello from standard out");
//        io.getErr().println("Hello from standard err");  //this text should appear in red
//        io.getOut().close();
//        io.getErr().close();

//        System.err.println(string);
//        System.out.println(string);

    }

    private void processMessage(MimeMessage m) {
        progressTicket.progress();
        Set<NodeDraft> tos = new HashSet<NodeDraft>();
        Set<NodeDraft> froms = new HashSet<NodeDraft>();
        Set<NodeDraft> ccs = new HashSet<NodeDraft>();
        Set<NodeDraft> bccs = new HashSet<NodeDraft>();
        Double date = null;
        Address[] from = null;
        Address[] to = null;
        Address[] cc = null;
        Address[] bcc = null;
        try {
            date = convertToDouble(m.getSentDate());
            from = m.getFrom();
            to = m.getRecipients(RecipientType.TO);
            cc = m.getRecipients(RecipientType.CC);
            bcc = m.getRecipients(RecipientType.BCC);
        } catch (MessagingException ex) {
            Exceptions.printStackTrace(ex);
            log("exception while getting message details");
        } finally {
            if (from != null) {
                for (Address a : from) {
                    froms.add(getNode(a));
                }
            } else {
//                log("from == null");
            }
            if (to != null) {
                for (Address a : to) {
                    tos.add(getNode(a));
                }
            } else {
//                log("to == null");
            }
            if (cc != null) {
                for (Address a : cc) {
                    ccs.add(getNode(a));
                }
            } else {
//                log("cc == null");
            }
            if (bcc != null) {
                for (Address a : bcc) {
                    bccs.add(getNode(a));
                }
            } else {
//                log("bcc == null");
            }
        }


        for (NodeDraft source : froms) {
            for (NodeDraft target : tos) {
                Double delta = 5d;
                //                EdgeDraftGetter edge = (EdgeDraftGetter) getEdge(source, target);
                //                insertEvent(edge, date, 1d);
                Pair pair = new Pair(source, target);
                if (edgesProto.containsKey(pair)) {
                    edgesProto.put(pair, insertEvent(edgesProto.get(pair), date, delta));
                } else {
                    Interval<Double> lo = new Interval<Double>(LOW_DATE, date, true, true, new Double(0));
                    Interval<Double> hi = new Interval<Double>(date, HIGH_DATE, false, true, delta);
                    edgesProto.put(pair, new DynamicDouble(new DynamicDouble(lo), hi));

                }
            }
            for (NodeDraft target : ccs) {
//                EdgeDraftGetter edge = (EdgeDraftGetter) getEdge(source, target);
//                insertEvent(edge, date, 0.4999d);
                Double delta = 1d;
                Pair pair = new Pair(source, target);
                if (edgesProto.containsKey(pair)) {
                    edgesProto.put(pair, insertEvent(edgesProto.get(pair), date, delta));
                } else {
                    Interval<Double> lo = new Interval<Double>(LOW_DATE, date, true, true, new Double(0));
                    Interval<Double> hi = new Interval<Double>(date, HIGH_DATE, false, true, delta);
                    edgesProto.put(pair, new DynamicDouble(new DynamicDouble(lo), hi));

                }

            }
            for (NodeDraft target : bccs) {
//                EdgeDraftGetter edge = (EdgeDraftGetter) getEdge(source, target);
//                insertEvent(edge, date, 0.01111d);
                Double delta = 0.05d;

                Pair pair = new Pair(source, target);
                if (edgesProto.containsKey(pair)) {
                    edgesProto.put(pair, insertEvent(edgesProto.get(pair), date, delta));
                } else {
                    Interval<Double> lo = new Interval<Double>(LOW_DATE, date, true, true, new Double(0));
                    Interval<Double> hi = new Interval<Double>(date, HIGH_DATE, false, true, delta);
                    edgesProto.put(pair, new DynamicDouble(new DynamicDouble(lo), hi));
                }
            }
        }


    }

    private NodeDraft getNode(Address a) {
        InternetAddress address = (InternetAddress) a;
        String addr = address.getAddress().trim();
        String name = address.getPersonal();
        NodeDraft sourceNode;
        if (!container.nodeExists(addr)) {
            sourceNode = container.factory().newNodeDraft();
            sourceNode.setId(addr);
//                sourceNode.setLabel(Utilities.codecTranslate(codecType, fromAddress.getPersonal()));
            sourceNode.setLabel(name);
            container.addNode(sourceNode);
        } else {
            sourceNode = container.getNode(addr);
        }

        return sourceNode;
    }

    private EdgeDraft getEdge(NodeDraft sourceNode, NodeDraft targetNode) {
        EdgeDraft edge = container.getEdge(sourceNode, targetNode);
        if (edge == null) {
            // add edge
            edge = container.factory().newEdgeDraft();
            edge.setSource(sourceNode);
            edge.setTarget(targetNode);
            AttributeColumn newDynCol = container.getAttributeModel().getEdgeTable().getColumn(PropertiesColumn.EDGE_WEIGHT.getIndex());
            if (newDynCol == null || newDynCol.getType() != AttributeType.DYNAMIC_DOUBLE) {
                AttributeColumn oldWeight = container.getAttributeModel().getEdgeTable().getColumn(PropertiesColumn.EDGE_WEIGHT.getIndex());
                newDynCol = container.getAttributeModel().getEdgeTable().replaceColumn(oldWeight, PropertiesColumn.EDGE_WEIGHT.getId(), PropertiesColumn.EDGE_WEIGHT.getTitle(), AttributeType.DYNAMIC_DOUBLE, AttributeOrigin.PROPERTY, null);
//                !column.getType().isDynamicType();
            }
//            edge.addAttributeValue(dynCol, new DynamicDouble(new Interval<Double>(1990d, 2004d, false, false, 0d)));
//            edge.addTimeInterval(1990d + "", "" + 2010d);

//            edge.setWeight(1f);
            container.addEdge(edge);
        }

        return edge;
    }

    private DynamicDouble getTimeline(EdgeDraftGetter edge) {
        AttributeRow attributeRow = edge.getAttributeRow();
        Object value = attributeRow.getValue(dynCol);
        DynamicDouble dd;
        if (value instanceof DynamicDouble) {
            dd = (DynamicDouble) value;
        } else {
            dd = null;
        }
        return dd;
    }

    private List<Interval<Double>> splitInterval(Interval<Double> source, Double splitPos, Double delta) {
//        log("split at "+ splitPos + " : " + source.toString());
        List<Interval<Double>> result = new ArrayList<Interval<Double>>();
        if (delta == null) {
            // illegal arguments - dont change anything. only wrap input into a list.
            result.add(source);
            return result;
        }

        Double value = (source.getValue() == null || source.getValue().isNaN() || source.getValue().isInfinite()) ? 0 : source.getValue();

//        // debug:
//        if (source.getLow() == splitPos) {
//            log("low == splitPos");
//        }
//        if (source.getLow() < splitPos) {
//            log("low < splitPos");
//        }
//        if (source.getLow() > splitPos) {
//            log("low > splitPos");
//        }
//
//        if (source.getHigh() == splitPos) {
//            log("high == splitPos");
//        }
//        if (source.getHigh() < splitPos) {
//            log("high < splitPos");
//        }
//        if (source.getHigh() > splitPos) {
//            log("high > splitPos");
//        }



        if (source.getHigh() < splitPos) {
            // Split in the future, nothing to do;
//            log("low\t<\thigh\t<\tsplit");
            result.add(source);
        } else if (splitPos < source.getLow()) {
            // split happend in the past => only change value
//            log("split\t<\tlow\t<\thigh");
//            log("split < low");
            result.add(new Interval<Double>(source, new Double(value + delta)));
        } else if (source.getLow() < splitPos && splitPos < source.getHigh()) {
//            log("low\t<\tsplit\t<\thigh");
//            log("splitting " + source.getLow() + "-----" + splitPos + "----" + source.getHigh());


            Interval<Double> lower = new Interval<Double>(source.getLow(), splitPos, source.isLowExcluded(), true, new Double(value));
            Interval<Double> upper = new Interval<Double>(splitPos, source.getHigh(), true, source.isHighExcluded(), new Double(value + delta));
            result.add(lower);
            result.add(upper);
        } else {
            log("NEEEDAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAANOTHERRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRONEEEEEEEEEEEEEEEEEEE");
        }
//        for(Interval<Double> i :result) {
//            log(i.toString());
//        }
        return result;
    }

    private List<Interval<Double>> getSplittedIntervals(List<Interval<Double>> source, Double splitPos, Double delta) {
//        log("source:" + source.size());
        List<Interval<Double>> result = new ArrayList<Interval<Double>>();
        for (Interval<Double> i : source) {
            result.addAll(splitInterval(i, splitPos, delta));
        }
//        log("result:" + result.size());
//        for (Interval<Double> f : result) {
//            if (f.getLow() > splitPos) {
//                result.remove(f);
//                result.add(new Interval<Double>(f, f.getValue() + delta));
//                log("Adding new interval");
//            }
//        }

//        log("splitted " + source.size() + " into " + result.size() + " intervals");
        return result;

    }

    private DynamicDouble insertEvent(DynamicDouble source, Double date, double d) {
        source = new DynamicDouble(getSplittedIntervals(source.getIntervals(), date, d));
        log(source.toString());
        return source;
    }

    private void insertEvent(EdgeDraftGetter edge, Double date, Double aDouble) {
        DynamicDouble oldTimeline = getTimeline(edge);
        if (oldTimeline == null) {
            oldTimeline = new DynamicDouble(new Interval<Double>(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, true, true, 0d));
        }

//        log("oldTimeline size:\t" + oldTimeline.getIntervals().size());
        List<Interval<Double>> templist = getSplittedIntervals(oldTimeline.getIntervals(), date, aDouble);

        oldTimeline = new DynamicDouble(oldTimeline, templist, oldTimeline.getIntervals());
//        log("newTimeline:\t" + newTimeline.getIntervals().size() + "\t" + newTimeline.toString());

//
//        Double oldLow = oldTimeline.getLow();
//        Double oldHigh = oldTimeline.getHigh();
//        List<Interval<Double>> past, present, future;
//        if (date < oldLow) {
//            // insert something in the front and increase the values after date
//            newTimeline = new DynamicDouble(new Interval<Double>(date, oldLow, false, true, aDouble));
//            for (Interval<Double> id : oldTimeline.getIntervals()) {
//                newTimeline = new DynamicDouble(newTimeline, new Interval<Double>(id.getLow(), id.getHigh(), false, true, id.getValue() + aDouble));
//            }
//
//        } else if (date > oldHigh) {
//            // simplay add a new interval at the end.
//
//            newTimeline = new DynamicDouble(oldTimeline, new Interval<Double>(oldHigh, date, false, true, oldTimeline.getValue(oldHigh, oldHigh) + aDouble));
//
//        } else {
//        // if (oldLow < date && date < oldHigh) {
//
//        /**
//         * splitting the interval and adding aDouble to the "future"
//         */
//        past = getPast(oldTimeline.getIntervals(oldLow, date), date);
//
//        log("past size:\t\t" + past.size());
////        present = oldTimeline.getIntervals(date, date);
////        log("present size:\t\t" + present.size());
//        future = getFuture(oldTimeline.getIntervals(date, oldHigh), date);
//        log("future size:\t\t" + future.size());
//
////        DynamicDouble newPresent = new DynamicDouble(present);
////        log("newPresent size:\t\t" + newPresent.getIntervals().size());
//
//        DynamicDouble newPast = new DynamicDouble(past);
//        log("newPast size:\t\t" + newPast.getIntervals().size());
//
//        DynamicDouble newFuture = new DynamicDouble(future);
//        log("newFuture size:\t\t" + newFuture.getIntervals().size());
//
//        newTimeline = new DynamicDouble(newPast);
//        for (Interval<Double> id : present) {
//            newPresent = new DynamicDouble(newPresent, new Interval<Double>(id.getLow(), id.getHigh(), id.getValue() + aDouble));
//        }
//        newTimeline = new DynamicDouble(newTimeline, newPresent.getIntervals());
//        for (Interval<Double> id : future) {
//            newFuture = new DynamicDouble(newFuture, new Interval<Double>(id.getLow(), id.getHigh(), id.getValue() + aDouble));
//        }
//        newTimeline = new DynamicDouble(newTimeline, newFuture.getIntervals());
//        }
//        log("before setting " + edge.getAttributeRow().getValue(dynCol).toString());
//        edge.getAttributeRow().setValue(dynCol, oldTimeline);
        edge.addAttributeValue(dynCol, oldTimeline);
        log("setting edge " + edge.getId() + " (" + dynCol.getTitle() + ")" + " timeline to " + edge.getAttributeRow().getValue(dynCol).toString());
//        edge.addAttributeValue(dynCol, newTimeline);
//        log("newTimeline size\t " + newTimeline.getIntervals().size());

    }
//
//    private List<Interval<Double>> getPast(List<Interval<Double>> intervals, Double date) {
//        List<Interval<Double>> result = new ArrayList<Interval<Double>>();
//        for (Interval<Double> i : intervals) {
//            result.add(splitInterval(i, date).get(0));
//        }
//        return result;
//    }
//
//    private List<Interval<Double>> getFuture(List<Interval<Double>> intervals, Double date) {
//        List<Interval<Double>> result = new ArrayList<Interval<Double>>();
//        for (Interval<Double> i : intervals) {
//            result.add(splitInterval(i, date).get(1));
//        }
//        return result;
//    }

    private enum MyRecipientType {

        TO, CC, BCC;
    }

    private void processFile(File file) {
        progressTicket.progress();
        if (!file.isDirectory()) {
            MimeMessage message = parseFile(file, report);
            if (message == null) {
//                report.log("file " + file.getName() + "can't be parsed into a mimemessage");
            } else {
                // process message!
                processMessage(message);
            }
        } else if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                processFile(f);
            }
        }
    }

    private void doImportNew() {
        File[] files = getFiles();
        // make weight column dynamic and accessible
//        container.getAttributeModel();

        dynCol = container.getAttributeModel().getEdgeTable().getColumn(PropertiesColumn.EDGE_WEIGHT.getIndex());
        if (dynCol == null || dynCol.getType() != AttributeType.DYNAMIC_DOUBLE) {
            AttributeColumn oldWeight = container.getAttributeModel().getEdgeTable().getColumn(PropertiesColumn.EDGE_WEIGHT.getIndex());
            dynCol = container.getAttributeModel().getEdgeTable().replaceColumn(oldWeight, PropertiesColumn.EDGE_WEIGHT.getId(), PropertiesColumn.EDGE_WEIGHT.getTitle(), AttributeType.DYNAMIC_DOUBLE, AttributeOrigin.PROPERTY, null);
        }

//        dynCol = container.getAttributeModel().getEdgeTable().addColumn("DynWeightColumnId", "DynWeightColTitle", AttributeType.DYNAMIC_DOUBLE, AttributeOrigin.DATA, null);

        edgesProto = new HashMap<Pair<NodeDraft, NodeDraft>, DynamicDouble>();

        progressTicket.progress("Estimating total amount of work to be done. May take a while");
        int totalNumOfEmails = getNumOfLocalEmailFile(files);

        progressTicket.switchToDeterminate(totalNumOfEmails);
        for (File file : files) {
//            progressTicket.progress();
            processFile(file);
        }
        progressTicket.progress("Almost done, adding edges");
        progressTicket.switchToDeterminate(edgesProto.size());
        for (Pair<NodeDraft, NodeDraft> k : edgesProto.keySet()) {
            progressTicket.progress();
            log("adding edge: " + k.key + "\t" + k.element + "\t with " + edgesProto.get(k).getIntervals().size() + " events");


//                progressTicket.progress();
            EdgeDraft ed = container.factory().newEdgeDraft();
            ed.setSource(k.key);
            ed.setTarget(k.element);
////                AttributeColumn weightColumn = container.getAttributeModel().getEdgeTable().getColumn(PropertiesColumn.EDGE_WEIGHT.getIndex());
////                container.getAttributeModel();
//            AttributeColumn newDynCol = container.getAttributeModel().getEdgeTable().getColumn(PropertiesColumn.EDGE_WEIGHT.getIndex());
//            if (newDynCol == null || newDynCol.getType() != AttributeType.DYNAMIC_DOUBLE) {
//                AttributeColumn oldWeight = container.getAttributeModel().getEdgeTable().getColumn(PropertiesColumn.EDGE_WEIGHT.getIndex());
//                newDynCol = container.getAttributeModel().getEdgeTable().replaceColumn(oldWeight, PropertiesColumn.EDGE_WEIGHT.getId(), PropertiesColumn.EDGE_WEIGHT.getTitle(), AttributeType.DYNAMIC_DOUBLE, AttributeOrigin.PROPERTY, null);
////                !column.getType().isDynamicType();
//            }
            DynamicDouble dynWeight = edgesProto.get(k);
            Double min = Double.POSITIVE_INFINITY;
            Double max = Double.NEGATIVE_INFINITY;
            for (Interval<Double> i : dynWeight.getIntervals()) {
                if (i.getLow() != Double.NEGATIVE_INFINITY && i.getLow() < min) {
                    min = i.getLow();
                }
                if (i.getHigh() != Double.POSITIVE_INFINITY && i.getHigh() > max) {
                    max = i.getHigh();
                }
                ed.addAttributeValue(dynCol, i.getValue(), i.getLow() + "", i.getHigh() + "");
            }
            ed.addTimeInterval(min + "", max + "");
//            SortedSet<Event> evts = edges.get(k);
//            Double weight = 0d;
//            ed.addTimeInterval(min + "", max + "");
//            ed.addAttributeValue(dynCol, dynWeight);

            container.addEdge(ed);
//            ed.addAttributeValue(dynCol, dynWeight);
            

            log("edge " + ed.toString() + " from " + min + " to " + max);
        }
    }

    private void doImport() {
        File[] files = getFiles();
        int totalNumOfEmails = getNumOfLocalEmailFile(files);
        progressTicket.switchToDeterminate(totalNumOfEmails);


        Collection<MimeMessage> msgs = importFromLocalFile(files);
//        progressTicket.switchToIndeterminate();
//        progressTicket.start(msgs.size());
        Set<EmailNode> nodes = new HashSet<EmailNode>();
        HashMap<Pair<EmailNode, EmailNode>, TreeSet<Event>> edges = new HashMap<Pair<EmailNode, EmailNode>, TreeSet<Event>>();

        progressTicket.switchToDeterminate(msgs.size());
        for (MimeMessage m : msgs) {
            progressTicket.progress();
            Set<EmailNode> froms = new HashSet<EmailNode>();
            Set<EmailNode> tos = new HashSet<EmailNode>();
            Set<EmailNode> ccs = new HashSet<EmailNode>();
            Set<EmailNode> bccs = new HashSet<EmailNode>();
            Double date = null;
            Address[] from = null;
            Address[] to = null;
            Address[] cc = null;
            Address[] bcc = null;
            try {
                date = convertToDouble(m.getSentDate());
                from = m.getFrom();
                to = m.getRecipients(RecipientType.TO);
                cc = m.getRecipients(RecipientType.CC);
                bcc = m.getRecipients(RecipientType.BCC);
            } catch (MessagingException ex) {
                Exceptions.printStackTrace(ex);
                log("exception while getting message details");
            } finally {
                if (from != null) {
                    for (Address a : from) {
                        froms.add(new EmailNode(a));
                    }
                    nodes.addAll(froms);
                } else {
                    log("from == null");
                }
                if (to != null) {
                    for (Address a : to) {
                        tos.add(new EmailNode(a));
                    }
                    nodes.addAll(tos);
                } else {
                    log("from == null");
                }
                if (cc != null) {
                    for (Address a : cc) {
                        ccs.add(new EmailNode(a));
                    }
                    nodes.addAll(ccs);
                } else {
                    log("cc == null");
                }
                if (bcc != null) {
                    for (Address a : bcc) {
                        bccs.add(new EmailNode(a));
                    }
                    nodes.addAll(bccs);
                } else {
                    log("bcc == null");
                }
            }

            for (EmailNode source : froms) {
                for (EmailNode target : tos) {
                    Event e = new Event(date, "TO");

                    Pair<EmailNode, EmailNode> p = new Pair(source, target);
                    if (!edges.containsKey(p)) {
                        log("creating new edge prototype from " + source.address + " to " + target.address);
                        TreeSet<Event> ts = new TreeSet<Event>();
                        ts.add(e);
                        edges.put(p, ts);
                    } else {
                        TreeSet<Event> ts = edges.get(p);
                        if (ts == null) {
                            ts = new TreeSet<Event>();
                        }
                        ts.add(e);
                    }
                }

                for (EmailNode target : ccs) {
                    Event e = new Event(date, "CC");

                    Pair<EmailNode, EmailNode> p = new Pair(source, target);
                    if (!edges.containsKey(p)) {
                        log("creating new edge prototype from " + source.address + " to " + target.address);

                        TreeSet<Event> ts = new TreeSet<Event>();
                        ts.add(e);
                        edges.put(p, ts);
                    } else {
                        TreeSet<Event> ts = edges.get(p);
                        if (ts == null) {
                            ts = new TreeSet<Event>();
                        }
                        ts.add(e);
                    }
                }
                for (EmailNode target : bccs) {
                    Event e = new Event(date, "BCC");

                    Pair<EmailNode, EmailNode> p = new Pair(source, target);
                    if (!edges.containsKey(p)) {
                        log("creating new edge prototype from " + source.address + " to " + target.address);

                        TreeSet<Event> ts = new TreeSet<Event>();
                        ts.add(e);
                        edges.put(p, ts);
                    } else {
                        TreeSet<Event> ts = edges.get(p);
                        if (ts == null) {
                            ts = new TreeSet<Event>();
                        }
                        ts.add(e);
                    }
                }
            }
//        progressTicket.finish();
            // now create all nodes at once
            progressTicket.setDisplayName("Creating nodes");
            progressTicket.switchToDeterminate(nodes.size());

            for (EmailNode n : nodes) {
                progressTicket.progress();

                NodeDraft sourceNode = null;
                log("adding node: " + n.address);
                sourceNode = container.factory().newNodeDraft();
                sourceNode.setId(n.address);
                sourceNode.setLabel(n.name);
                container.addNode(sourceNode);
            }
//        progressTicket.finish();

            progressTicket.setDisplayName("Creating edges");
            progressTicket.switchToDeterminate(edges.keySet().size());
//        progressTicket.start();

            for (Pair<EmailNode, EmailNode> k : edges.keySet()) {
                progressTicket.progress();
                log("adding edge: " + k.key.address + "\t" + k.element.address + "\t with " + edges.get(k).size() + " events");

//                progressTicket.progress();
                EdgeDraft ed = container.factory().newEdgeDraft();
                ed.setSource(container.getNode(k.key.address));
                ed.setTarget(container.getNode(k.element.address));
//                AttributeColumn weightColumn = container.getAttributeModel().getEdgeTable().getColumn(PropertiesColumn.EDGE_WEIGHT.getIndex());
//                container.getAttributeModel();
                AttributeColumn newDynCol = container.getAttributeModel().getEdgeTable().getColumn(PropertiesColumn.EDGE_WEIGHT.getIndex());
                if (newDynCol == null || newDynCol.getType() != AttributeType.DYNAMIC_DOUBLE) {
                    AttributeColumn oldWeight = container.getAttributeModel().getEdgeTable().getColumn(PropertiesColumn.EDGE_WEIGHT.getIndex());
                    newDynCol = container.getAttributeModel().getEdgeTable().replaceColumn(oldWeight, PropertiesColumn.EDGE_WEIGHT.getId(), PropertiesColumn.EDGE_WEIGHT.getTitle(), AttributeType.DYNAMIC_DOUBLE, AttributeOrigin.PROPERTY, null);
//                !column.getType().isDynamicType();
                }
                DynamicDouble dynWeight = new DynamicDouble();
                SortedSet<Event> evts = edges.get(k);
                Double weight = 0d;
                ed.addTimeInterval(evts.first().start.toString(), evts.last().start.toString());
                while (!evts.isEmpty()) {
                    log("evts has " + evts.size() + " events left.");
                    Event e = evts.first();
                    evts.remove(e);
                    if (!evts.isEmpty()) {
                        weight += 1;
                        dynWeight = new DynamicDouble(dynWeight, new Interval<Double>(e.start, evts.first().start, false, true, new Double(weight)));
                    }
                }

                ed.addAttributeValue(dynCol, dynWeight);

                container.addEdge(ed);



            }
        }
    }

    private class Event implements Comparable {

        public Double start;
        public String type;

        public Event(Double time, String type) {
            this.start = time;
            this.type = type;
        }

        @Override
        public int compareTo(Object obj) {
            if (obj == null) {
                throw new NullPointerException();
            }
            if (getClass() != obj.getClass()) {
                throw new IllegalArgumentException();
            }
            final Event other = (Event) obj;
            if (this.start < other.start) {
                return -1;
            }
            if (this.start > other.start) {
                return 1;
            }
            return 0;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Event other = (Event) obj;
            if (this.start != other.start && (this.start == null || !this.start.equals(other.start))) {
                return false;
            }
            return true;
        }
    }

    /**
     * import from local files
     * @param files
     */
    private Collection<MimeMessage> importFromLocalFile(File[] files) {
        Collection<MimeMessage> msgs = new ArrayList<MimeMessage>();
        if (files == null) {
            return msgs;
        }
        // create nodes
        for (File file : files) {
            if (!file.isDirectory()) {
                progressTicket.progress();
                MimeMessage message = parseFile(file, report);
                if (message == null) {
                    report.log("file " + file.getName() + "can't be parsed");
                    return msgs;
                } else {
                    msgs.add(message);
//                  filterOneEmail(message);
//                  filterSingleEmail(message);
                }
            } else if (file.isDirectory()) {
                msgs.addAll(importFromLocalFile(file.listFiles()));
            } else {
                continue;
            }
        }
        return msgs;
        // create timeline edges

    }

    public MimeMessage parseFile(File file, Report report) {
        InputStream is = null;
        Session s = null;
        MimeMessage message = null;
        try {
            is = new FileInputStream(file);
            s = Session.getDefaultInstance(System.getProperties(), null);
            message = new MimeMessage(s, is);
        } catch (MessagingException ex) {
            report.logIssue(new Issue(ex.getMessage(), Issue.Level.WARNING));
            return null;
        } catch (FileNotFoundException ex) {
            report.logIssue(new Issue(ex.getMessage(), Issue.Level.WARNING));
            return null;
        } finally {
            try {
                is.close();
            } catch (IOException ex) {
                report.logIssue(new Issue(ex.getMessage(), Issue.Level.WARNING));
                return null;
            }
        }
        return message;
    }

    private int getNumOfLocalEmailFile(File[] files) {
        int totalNum = 0;
        if (files == null) {
            return 0;
        }
        for (File f : files) {
            totalNum += getNumOfOneFile(f);
        }
        return totalNum;
    }

    private int getNumOfOneFile(File f) {
        int temp = 0;
        if (!f.isDirectory()) {
            temp = 1;
        } else {
            temp = temp + getNumOfLocalEmailFile(f.listFiles());
        }
        return temp;
    }

    public File[] getFiles() {
        return files;
    }

    public void setFiles(File[] filePath) {
        this.files = filePath;
    }

    public boolean hasCcAsWeight() {
        return hasCcAsWeight;
    }

    public void setCcAsWeight(boolean hasCcAsWeight) {
        this.hasCcAsWeight = hasCcAsWeight;
    }

    public boolean hasBccAsWeight() {
        return hasBccAsWeight;
    }

    public void setBccAsWeight(boolean hasBccAsWeight) {
        this.hasBccAsWeight = hasBccAsWeight;
    }
}
