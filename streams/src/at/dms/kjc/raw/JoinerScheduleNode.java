package at.dms.kjc.raw;

public class JoinerScheduleNode 
{
    public static final int FIRE = 0;
    public static final int RECEIVE = 1;
    public static final int INITPATH = 2;

    public JoinerScheduleNode next;
    public int type;
    public String buffer;
    public int initPathIndex = 0;

    public JoinerScheduleNode() 
    {
    }
        
    //create an initPath call node...
    public JoinerScheduleNode(int index, String buf) {
	type = INITPATH;
	initPathIndex = index;
	buffer = buf;
    }
    
    public int getType() 
    {
	return type;
    }
    
    public String getBuffer() 
    {
	return buffer;
    }

    //print the c code for this node
    //fp is true if this is floating point code
    public String getC(boolean fp) {
	StringBuffer ret = new StringBuffer();
	
	if (type == FIRE) {
	    ret.append("static_send(__buffer" + buffer +
		       "[__first" + buffer + "++]);\n");
	    ret.append("__first" + buffer + " = __first" + buffer + " & __MINUSONE__;\n");
	    //	    ret.append("if (first" + buffer + " >= BUFSIZE) first" + 
	    //	       buffer + " = 0;\n");
	    // ret.append("if (first" + buffer + " == last" + 
	    //       buffer + ") print_int(2000);\n");
	    
	}
	else if (type == RECEIVE) { //receive
	    ret.append("__buffer" + buffer + "[__last" + buffer + "++] = static_receive");
	    if (fp)
		ret.append("_f");
	    ret.append("();\n");
	    ret.append("__last" + buffer + " = __last" + buffer + " & __MINUSONE__;\n");
	    //ret.append("if (last" + buffer + " >= BUFSIZE) last" + buffer + " = 0;\n");
	}
	else if (type == INITPATH){
	    ret.append("__buffer" + buffer + "[__last" + buffer + "++] = initPath(" + 
		       initPathIndex + ");\n");
	    ret.append("__last" + buffer + " = __last" + buffer + " & __MINUSONE__;\n");
	}
	
	return ret.toString();
    }
    
    /**
     * Returns whether <other> has the same type and buffer name as
     * this.
     */
    public boolean equals(JoinerScheduleNode other) {
	return type==other.type && buffer.equals(other.buffer) && initPathIndex == other.initPathIndex;
    }

    /**
     * Traverses the list defined from <node> and returns an array of
     * all elements reachable through the list.  result[0] is node,
     * and result[result.length-1].next = null.
     */
    public static JoinerScheduleNode[] toArray(JoinerScheduleNode node) {
	// figure that two traversals is cheaper than making a
	// linkedlist and going to an array

	// figure out how many elements in the list
	int count = 0;
	for (JoinerScheduleNode n=node; n!=null; n=n.next) {
	    count++;
	}

	// make output array
	JoinerScheduleNode[] result = new JoinerScheduleNode[count];
	for (int i=0; i<count; i++) {
	    result[i] = node;
	    node = node.next;
	}
	return result;
    }
    
    public void printMe() 
    {
	if (type == FIRE)
	    System.out.print("Fire: ");
	else if (type == RECEIVE) 
	    System.out.print("Receive: ");
	else if (type == INITPATH) {
	    System.out.println("InitPath: " + initPathIndex);
	    return;
	}
	System.out.println(buffer);
    }

    public static void printSchedule(JoinerScheduleNode first) 
    {
	do {
	    first.printMe();
	    first = first.next;
	}while(first != null);
    }
    
}
