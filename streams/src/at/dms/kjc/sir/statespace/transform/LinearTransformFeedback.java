package at.dms.kjc.sir.statespace.transform;

import at.dms.kjc.sir.statespace.*;
import java.util.*;

public class LinearTransformFeedback extends LinearTransform {

    LinearFilterRepresentation bodyRep, loopRep;
    int inputToBody, loopToBody, bodyToLoop, bodyToOutput;

    private LinearTransformFeedback(LinearFilterRepresentation bodyRep, LinearFilterRepresentation loopRep, int[] weights) {

	this.bodyRep = bodyRep;
	this.loopRep = loopRep;
	this.inputToBody = weights[0];
	this.loopToBody = weights[1];
	this.bodyToOutput = weights[2];
	this.bodyToLoop = weights[3];

    }

    public LinearFilterRepresentation transform() throws NoTransformPossibleException {
	
	FilterMatrix A1, B1, C1, D1, A2, B2, C2, D2;
	FilterMatrix B2_1, B2_2, C2_1, C2_2, D2_11, D2_12, D2_21, D2_22;
	FilterMatrix A, B, C, D;
	FilterVector init1, init2, init3, init;
	int state1Count, state2Count, state3Count, totalStateCount, totalInput, totalOutput;

	A1 = loopRep.getA();
	B1 = loopRep.getB();
	C1 = loopRep.getC();
	D1 = loopRep.getD();
	init1 = loopRep.getInit();

	A2 = bodyRep.getA();
	B2 = bodyRep.getB();
	C2 = bodyRep.getC();
	D2 = bodyRep.getD();
	init2 = loopRep.getInit();

	totalInput = inputToBody;
	totalOutput = bodyToOutput;

	state1Count = loopRep.getStateCount();
	state2Count = bodyRep.getStateCount();
	state3Count = loopToBody;

	totalStateCount = state1Count+state2Count+state3Count;

	B2_1 = new FilterMatrix(state2Count,inputToBody);
	B2_2 = new FilterMatrix(state2Count,loopToBody);
	B2_1.copyColumnsAt(0,B2,0,inputToBody);
	B2_2.copyColumnsAt(0,B2,inputToBody,loopToBody);

	C2_1 = new FilterMatrix(bodyToOutput,state2Count);
	C2_2 = new FilterMatrix(bodyToLoop,state2Count);
	C2_1.copyRowsAt(0,C2,0,bodyToOutput);
	C2_2.copyRowsAt(0,C2,bodyToOutput,bodyToLoop);

	D2_11 = new FilterMatrix(bodyToOutput,inputToBody);
	D2_12 = new FilterMatrix(bodyToOutput,loopToBody);
	D2_21 = new FilterMatrix(bodyToLoop,inputToBody);
	D2_22 = new FilterMatrix(bodyToLoop,loopToBody);
	D2_11.copyRowsAndColsAt(0,0,D2,0,0,bodyToOutput,inputToBody);
	D2_12.copyRowsAndColsAt(0,0,D2,0,inputToBody,bodyToOutput,loopToBody);
	D2_21.copyRowsAndColsAt(0,0,D2,bodyToOutput,0,bodyToLoop,inputToBody);
	D2_22.copyRowsAndColsAt(0,0,D2,bodyToOutput,inputToBody,bodyToLoop,loopToBody);

	LinearPrinter.println("B2: " + B2);
	LinearPrinter.println("B2_1: " + B2_1);
	LinearPrinter.println("B2_2: " + B2_2);
	LinearPrinter.println("C2: " + C2);
	LinearPrinter.println("C2_1: " + C2_1);
	LinearPrinter.println("C2_2: " + C2_2);
	LinearPrinter.println("D2: " + D2);
	LinearPrinter.println("D2_11: " + D2_11);
	LinearPrinter.println("D2_12: " + D2_12);
	LinearPrinter.println("D2_21: " + D2_21);
	LinearPrinter.println("D2_22: " + D2_22);

	A = new FilterMatrix(totalStateCount,totalStateCount);
	B = new FilterMatrix(totalStateCount,totalInput);
	C = new FilterMatrix(totalOutput,totalStateCount);
	D = new FilterMatrix(totalOutput,totalInput);
	init = new FilterVector(totalStateCount);

	A.copyAt(0,0,A1);
	A.copyAt(0,state1Count,B1.times(C2_2));
	A.copyAt(0,state1Count+state2Count,B1.times(D2_22));
	A.copyAt(state1Count,state1Count,A2);
	A.copyAt(state1Count,state1Count+state2Count,B2_2);
	A.copyAt(state1Count+state2Count,0,C1);
	A.copyAt(state1Count+state2Count,state1Count,D1.times(C2_2));
	A.copyAt(state1Count+state2Count,state1Count+state2Count,D1.times(D2_22));

	B.copyAt(0,0,B1.times(D2_21));
	B.copyAt(state1Count,0,B2_1);
	B.copyAt(state1Count+state2Count,0,D1.times(D2_21));

	C.copyAt(0,state1Count,C2_1);
	C.copyAt(0,state1Count+state2Count,D2_12);
	
	D = D2_11.copy();
	
	init.copyAt(0,0,init1);
	init.copyAt(0,state1Count,init2);


	LinearFilterRepresentation newRep;

	//no prework function, peek==pop

	newRep = new LinearFilterRepresentation(A,B,C,D,init,totalInput);

	return newRep;
    }

    public static LinearTransform calculateFeedback(LinearFilterRepresentation bodyRep, LinearFilterRepresentation loopRep, int[] weights) {

	int bodyInput, bodyOutput;

	bodyInput = weights[0] + weights[1];
	bodyOutput = weights[2] + weights[3];

	if(bodyInput != bodyRep.getPopCount())
	    return new LinearTransformNull("Body pop count doesn't match --> feedback is unschedulable");

	if(bodyOutput != bodyRep.getPushCount())
	    return new LinearTransformNull("Body push count doesn't match --> feedback is unschedulable");

	if(weights[3] != loopRep.getPopCount())
	    return new LinearTransformNull("Loop pop count doesn't match --> feedback is unschedulable");
	
	if(weights[1] != loopRep.getPushCount())
	    return new LinearTransformNull("Loop push count doesn't match --> feedback is unschedulable");

	return new LinearTransformFeedback(bodyRep,loopRep,weights);

    }

}
