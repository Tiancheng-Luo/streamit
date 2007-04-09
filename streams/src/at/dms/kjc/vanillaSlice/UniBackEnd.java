package at.dms.kjc.vanillaSlice;

import at.dms.kjc.sir.*;
import at.dms.kjc.*;
import at.dms.kjc.backendSupport.*;
import at.dms.kjc.slicegraph.DataFlowOrder;
import at.dms.kjc.slicegraph.Partitioner;
import at.dms.kjc.spacetime.AddBuffering;
import at.dms.kjc.spacetime.BasicGenerateSteadyStateSchedule;
import at.dms.kjc.common.CodegenPrintWriter;
import java.io.*;
/**
 * The entry to the back end for a uniprocesor or cluster.
 */
public class UniBackEnd {
    /** holds pointer to BackEndFactory instance during back end portion of this compiler. */
    public static UniBackEndFactory backEndBits = null;
    
    /**
     * Top level method for SpaceTime backend, called via reflection from {@link at.dms.kjc.Main}.
     * @param str               SIRStream from {@link at.dms.kjc.Kopi2SIR}
     * @param interfaces        JInterfaceDeclaration[] from {@link at.dms.kjc.Kopi2SIR}
     * @param interfaceTables   SIRInterfaceTable[] from  {@link at.dms.kjc.Kopi2SIR}
     * @param structs           SIRStructure[] from  {@link at.dms.kjc.Kopi2SIR}
     * @param helpers           SIRHelper[] from {@link at.dms.kjc.Kopi2SIR}
     * @param global            SIRGlobal from  {@link at.dms.kjc.Kopi2SIR}
     */
    public static void run(SIRStream str,
            JInterfaceDeclaration[] interfaces,
            SIRInterfaceTable[] interfaceTables,
            SIRStructure[]structs,
            SIRHelper[] helpers,
            SIRGlobal global) {

        int numCores = KjcOptions.newSimple;
        
        // The usual optimizations
        CommonPasses commonPasses = new CommonPasses();
        /*Slice[] sliceGraph = */ commonPasses.run(str, interfaces, 
                interfaceTables, structs, helpers, global, numCores);

        // partitioner contains information about the Slice graph.
        Partitioner partitioner = commonPasses.getPartitioner();
        // fix any rate skew introduced in conversion to Slice graph.
        AddBuffering.doit(partitioner,false,numCores);
        // decompose any pipelines of filters in the Slice graph.
        partitioner.ensureSimpleSlices();

        // guarantee tha we are not going to hack properties of filters
        FilterInfo.canUse();

        // Set schedules for initialization, priming (if --spacetime), and steady state.
        SpaceTimeScheduleAndPartitioner schedule = new SpaceTimeScheduleAndPartitioner(partitioner);
        // set init schedule in standard order
        schedule.setInitSchedule(DataFlowOrder.getTraversal(partitioner.getSliceGraph()));
        // set prime pump schedule (if --spacetime and not --noswpipe)
        new at.dms.kjc.spacetime.GeneratePrimePumpSchedule(schedule).schedule(partitioner.getSliceGraph());
        // set steady schedule in standard order unless --spacetime in which case in 
        // decreasing order of estimated work
        new BasicGenerateSteadyStateSchedule(schedule, partitioner).schedule();

        // create a collection of (very uninformative) processor descriptions.
        UniProcessors processors = new UniProcessors(numCores);

        // assign SliceNodes to processors
        Layout<UniProcessor> layout;
        if (KjcOptions.spacetime && !KjcOptions.noswpipe) {
            layout = new BasicGreedyLayout<UniProcessor>(schedule, processors.toArray());
        } else {
            layout = new NoSWPipeLayout<UniProcessor,UniProcessors>(schedule, processors);
        }
        layout.run();
 
        // create other info needed to convert Slice graphs to Kopi code + Channels
        UniBackEndFactory uniBackEndBits  = new UniBackEndFactory(processors);
        backEndBits = uniBackEndBits;
        uniBackEndBits.setLayout(layout);
        
        // now convert to Kopi code plus channels.
        uniBackEndBits.getBackEndMain().run(schedule, uniBackEndBits);

        /*
         * Emit code to str.c
         */
        String outputFileName = "str.c";
        try {
        CodegenPrintWriter p = new CodegenPrintWriter(new BufferedWriter(new FileWriter(outputFileName, false)));
        // write out C code
        EmitStandaloneCode codeEmitter = new EmitStandaloneCode(uniBackEndBits);
        // Concat emitted code for all nodes into one file.
        for (ComputeNode n : uniBackEndBits.getComputeNodes().toArray()) {
            codeEmitter.emitCodeForComputeNode(n,p);
        }
        p.close();
        } catch (IOException e) {
            throw new AssertionError("I/O error on " + outputFileName + ": " + e);
        }
        // return success
        System.exit(0);
    }
}