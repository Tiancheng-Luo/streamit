
package at.dms.kjc.cluster;

import at.dms.kjc.sir.*;
import at.dms.kjc.iterator.*;
import java.util.Vector;
import java.util.Iterator;
import java.util.HashSet;
import java.util.HashMap;
import streamit.misc.AssertedClass;

/**
 * A class that detects and stores pairwise latency constraints 
 * between message senders and receivers. A latency constraint
 * only exists if a downstream message is sent at negative latency
 * or an upstream message is being sent (latency must be positive).
 * Note that current implementation assumes that each filter that 
 * receives credits receives them from only one filter.
 */

public class LatencyConstraints {

    // consists of SIRFilter(s)
    public static HashSet restrictedExecutionFilters = new HashSet();

    // consists of SIRFilter -> Integer
    //public static HashMap initCredit = new HashMap();

    // consists of SIRFilter(s) -> HashSet of LatencyConstraint(s)
    public static HashMap outgoingLatencyConstraints = new HashMap();

    // Vector of SIRFilter(s) (sender, receiver) -> Boolean;
    public static HashMap messageDirectionDownstream = new HashMap();

    /**
     * Returns true if a {@link SIRFilter} needs to receive credit
     * messages and limit its execution accordingly.
     */

    public static boolean isRestricted(SIRFilter filter) {
        return restrictedExecutionFilters.contains(filter);
    }

    /*
    public static int getInitCredit(SIRFilter filter) {
        return ((Integer)initCredit.get(filter)).intValue();
    }

    private static void setInitCredit(SIRFilter filter, int val) {
        Integer now = (Integer)initCredit.get(filter);
        if (now == null || val < now.intValue()) {
            initCredit.put(filter, new Integer(val));
        }
    }
    */

    /**
     * Returns a set of outgoing constraints for a {@link SIRFilter}
     * this represents a set of filters to which an execution
     * credit messages will have to be sent.
     * @param filter the filter
     * @return a set of filters that need to receive credit messages
     */

    public static HashSet getOutgoingConstraints(SIRFilter filter) {

        if (outgoingLatencyConstraints.containsKey(filter)) {
            return (HashSet)outgoingLatencyConstraints.get(filter);     
        } else {
            HashSet tmp = new HashSet();
            outgoingLatencyConstraints.put(filter, tmp);
            return tmp;
        }
    }

    /**
     * Returns maximum latency associatew with a {@link SIRLatency} object
     * @param latency the latency object
     * @return the maximum latency as integer
     */

    private static int MaxLatency(SIRLatency latency) {
    
        if (latency instanceof SIRLatencyMax) {
            return ((SIRLatencyMax)latency).getMax();
        }

        if (latency instanceof SIRLatencyRange) {
            return ((SIRLatencyRange)latency).getMax();
        }
    
        if (latency instanceof SIRLatencySet) {
            Iterator it = ((SIRLatencySet)latency).iterator();

            int max = -1000000;

            while (it.hasNext()) {
                Integer i = (Integer)it.next();

                if (i.intValue() > max) max = i.intValue();
            }

            return max;
        }

        // this should never be reached!

        return 0;
    }

    /**
     * Checks if the message direction between sender and receiver is
     * downstream
     */

    public static boolean isMessageDirectionDownstream(SIRFilter sender,
                                                       SIRFilter receiver) {
        Vector v = new Vector();
        v.add(sender);
        v.add(receiver);

        Boolean b = (Boolean)messageDirectionDownstream.get(v);
    
        //System.out.println("sender: "+sender+" receiver: "+receiver);

        assert b != null:
            "Information about message direction from " + sender + " to " +
            receiver + " requested when no such data has been gathered";

        return b.booleanValue();
    }

    /**
     * Given a top stream iterator and an array of portals detecta and
     * register all pairwise latency constraints.
     * @param topStreamIter top stream iterator
     * @param portals an array of all portals
     */

    public static void detectConstraints(
                                         streamit.scheduler2.iriter.Iterator topStreamIter,
                                         SIRPortal portals[]) {
        if (ClusterBackend.debugPrint)
            System.out.println("Number of portals is: "+portals.length);

        for (int t = 0; t < portals.length; t++) {
        
            SIRPortalSender senders[] = portals[t].getSenders();
            SIRStream receivers[] = portals[t].getReceivers();

            HashSet visited_senders = new HashSet();
        
            int min_latency = 0;
        
            boolean any_latency_found = false;

            if (ClusterBackend.debugPrint)
                System.out.println("\n    Portal: "+portals[t]);

            for (int i = 0; i < senders.length; i++) {
                SIRStream sender = senders[i].getStream();

                if (!visited_senders.contains(sender)) {

                    if (ClusterBackend.debugPrint)
                        System.out.print("        sender: ("+sender+")");
                    
                    try {
                        int id = NodeEnumerator.getSIROperatorId(sender);
                        if (ClusterBackend.debugPrint)
                            System.out.print(" ID:"+id);
                    } catch (Exception ex) {
                    }

                    if (ClusterBackend.debugPrint)
                        System.out.println();
        
                    visited_senders.add(sender);

                    for (int y = i; y < senders.length; y++) {
                        if (senders[y].getStream().equals(sender)) {
            
                            int this_max = 0;

                            SIRLatency latency = senders[y].getLatency();

                            // we are interested in the least of the
                            // upper bounds. in each case we will send 
                            // the message with as large latency
                            // as possible.

                            this_max = MaxLatency(latency);

                            if (ClusterBackend.debugPrint)
                                System.out.println("          detect Latency: "+
                                                   this_max);

                            if (!any_latency_found) {
                                min_latency = this_max;
                                any_latency_found = true;
                            }
                
                            if (this_max < min_latency) {
                                min_latency = this_max;
                            }
                        }
                    } 

                    if (ClusterBackend.debugPrint)
                        System.out.println("          minimum Latency: "
                                           + min_latency);
                }
            }

            for (int i = 0; i < receivers.length; i++) {
                SIRStream receiver = receivers[i];
        
                try {

                    int id = NodeEnumerator.getSIROperatorId(receiver);
                    if (ClusterBackend.debugPrint)
                        System.out.println("        receiver: ("
                                           + receiver + ") ID:" + id);
                } catch (Exception ex) {
                    if (ClusterBackend.debugPrint)
                        System.out.println("        receiver: ("
                                           + receiver + ") ID: NOT FOUND");
            
                }
            }

            if (ClusterBackend.debugPrint)
                System.out.println();

            Iterator senders_i = visited_senders.iterator();

            while (senders_i.hasNext()) {
                SIRStream sender = (SIRStream)senders_i.next();

                HashSet constraints = new HashSet();

                boolean upstream = false;

                for (int i2 = 0; i2 < receivers.length; i2++) {
                    SIRStream receiver = receivers[i2];
            
                    if (ClusterBackend.debugPrint)
                        System.out.println("    sender: " + sender + " receiver: "
                                           + receiver);

                    SIRFilter f1 = (SIRFilter)sender;
                    SIRFilter f2 = (SIRFilter)receiver;

                    int n1 = NodeEnumerator.getSIROperatorId(f1);
                    int n2 = NodeEnumerator.getSIROperatorId(f2);

                    FindPath.find(n1,n2);

                    streamit.scheduler2.constrained.Scheduler cscheduler2 =
                        streamit.scheduler2.constrained.Scheduler.
                        create(topStreamIter);

                    streamit.scheduler2.iriter.Iterator iter1 = 
                        IterFactory.createFactory().createIter(f1);
                    streamit.scheduler2.iriter.Iterator iter2 = 
                        IterFactory.createFactory().createIter(f2);

                    streamit.scheduler2.SDEPData sdep2;

                    try {
                        sdep2 = cscheduler2.computeSDEP(iter1, iter2);
            
                        // message is being sent downstream

                        Vector v = new Vector();
                        v.add(f1);
                        v.add(f2);
                        messageDirectionDownstream.put(v, new Boolean(true));
                        if (ClusterBackend.debugPrint)
                            System.out.println("sender: "+f1+" receiver: "+f2);


                        upstream = false;

                    } catch (streamit.scheduler2.constrained.
                             NoPathException ex) {
            
                        try {
                            sdep2 = cscheduler2.computeSDEP(iter2, iter1);

                            Vector v = new Vector();
                            v.add(f1);
                            v.add(f2);
                            messageDirectionDownstream.put(v, 
                                                           new Boolean(false));
                            if (ClusterBackend.debugPrint)
                                System.out.println("sender: "+f1+" receiver: "+f2);

                            // message is being sent upstream

                            upstream = true;

                        } catch (streamit.scheduler2.constrained.
                                 NoPathException ex2) {
                
                            // no path between source and dest has been found
            
                            AssertedClass.ASSERT(cscheduler2, false, 
                                                 "no path found between source and destination of message");
                
                            // never executed

                            continue;

                        }

                    }

                    if (ClusterBackend.debugPrint) {
                        System.out.println("      Upstream Init Phases: "
                                           + sdep2.getNumSrcInitPhases());
                        System.out.println("      Downstr. Init Phases: "
                                           + sdep2.getNumDstInitPhases());
                        System.out.println("      Upstream Steady Phases: "
                                           + sdep2.getNumSrcSteadyPhases());
                        System.out.println("      Downstr. Steady Phases: "
                                           + sdep2.getNumDstSteadyPhases());
                    }

                    int upstreamSteady = sdep2.getNumSrcSteadyPhases();
                    int downstreamSteady = sdep2.getNumDstSteadyPhases();
            
                    for (int t2 = 0; t2 < 20; t2++) {
                        int phase = sdep2.getSrcPhase4DstPhase(t2);
                        int phaserev = sdep2.getDstPhase4SrcPhase(t2);
                        if (ClusterBackend.debugPrint)
                            System.out.println("      sdep ["+t2+"] = "+phase+
                                               " reverse_sdep["+t2+"] = "+phaserev);
                    }
            
                    // take care of negative latency downstream messages

                    if (min_latency < 0 && !upstream) {
                
                        int last_dep;
                        int iter;
            
                        // add receiver to set of restricted filters
            
                        restrictedExecutionFilters.add(receiver);
            
                        for (iter = 1;; iter++) {
                            last_dep = sdep2.getDstPhase4SrcPhase(iter);

                            if (last_dep > 1) break;
                        }

                        if (ClusterBackend.debugPrint)
                            System.out.println("iter: " + iter
                                               + " last_dep: " + last_dep);
            
                        int init = (iter-1)-min_latency-1;

                        LatencyConstraint constraint = 
                            new LatencyConstraint(init,
                                                  upstreamSteady,
                                                  downstreamSteady,
                                                  (SIRFilter)receiver);
            
            
                        // add constraint to the senders 
                        // list of constraints
            
                        constraints.add(constraint);
            
                        last_dep = 0;
            
                        for (int inc = 0; inc < upstreamSteady; inc++) {
                            int current = 
                                sdep2.getDstPhase4SrcPhase(iter+inc)-1;
                            if (current > last_dep) {
                
                                if (ClusterBackend.debugPrint)
                                    System.out.println("Can exec "+current
                                                       +" at source iteration nr. "
                                                       +(init+inc)
                                                       +" array:"+current);
                                
                                constraint.setDependencyData(inc, 
                                                             current); 
                
                                last_dep = current;
                            } else {
                
                                if (ClusterBackend.debugPrint)
                                    System.out.println("Can not advance dest. at source iteration nr. "
                                                       +(init+inc)+" array:0");
                
                                constraint.setDependencyData(inc, 
                                                             0); 
                            }
                        }
            
                        constraint.output();
                    }

                    // take care of upstream messages

                    if (upstream && any_latency_found && min_latency < 0) {

                        AssertedClass.ASSERT(topStreamIter, false, 
                                             "Error: an upstream message is being sent with a negative latency.");
            
                    }
            
                    if (upstream && any_latency_found && min_latency > 0) {
            
                        int init_credit;
                        int iter = 1;
            
                        // add receiver to set of restricted filters
            
                        restrictedExecutionFilters.add(receiver);

                        init_credit = 
                            sdep2.getSrcPhase4DstPhase(1 + min_latency) - 1;

                        //setInitCredit((SIRFilter)receiver, init_credit);

                        if (ClusterBackend.debugPrint)
                            System.out.println("Init credit: "+init_credit);

                        if (init_credit < sdep2.getSrcPhase4DstPhase(iter)) {
                            // a deadlock
                            if (ClusterBackend.debugPrint)
                                System.out.println("WARNING === possible deadlock ===");

                        }

                        LatencyConstraint constraint = 
                            new LatencyConstraint(init_credit,
                                                  downstreamSteady,
                                                  upstreamSteady,
                                                  (SIRFilter)receiver);

                        constraints.add(constraint);

                        int credit_sent = init_credit;

                        for (int offset = 0; offset < downstreamSteady; 
                             offset++) {
                
                            int credit = 
                                sdep2.getSrcPhase4DstPhase(offset + 
                                                           2 + min_latency)- 1;
                            if (credit > credit_sent) { 

                                constraint.setDependencyData(offset, 
                                                             credit); 

                                if (ClusterBackend.debugPrint)
                                    System.out.println("At end of iter: "
                                                       +(offset+1)
                                                       +" can send credit: "
                                                       +credit); 
                            } else {

                                constraint.setDependencyData(offset, 
                                                             0); 

                                if (ClusterBackend.debugPrint)
                                    System.out.println("At end of iter: "
                                                       +(offset+1)
                                                       +" no additional credit."); 
                            }

                            credit_sent = credit;
                        }
            
                        constraint.output();
                    }

                    // for loop closes
                    if (ClusterBackend.debugPrint)
                        System.out.println();
                }

                outgoingLatencyConstraints.put(sender, constraints);

            }

            if (ClusterBackend.debugPrint)
                System.out.println();
        }
    }

}
