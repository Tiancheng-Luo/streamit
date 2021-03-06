package at.dms.kjc.smp;

import java.io.*;
import at.dms.kjc.sir.SIRStructure;

/**
 * This class represents the structs.h file that will contain typedefs for 
 * our generated c code.  It is just a wrapper for a stringbuffer with some
 * append methods.
 * 
 * @author mgordon
 *
 */
public class Structs_h {
    
    private StringBuffer buf;
    
    /**
     * Create a new structs.h text file with an empty string buffer.
     */
    public Structs_h(SIRStructure[] structs) {
        buf = new StringBuffer();

        buf.append("#ifndef STRUCTS_H\n");
        buf.append("#define STRUCTS_H\n\n");
        
	buf.append("typedef int bit;\n");
	createStructureDefs(structs);
    }
    
     /**
     * create a c header file with all the structure definitions as typedef'ed
     * structs.
     */
    private void createStructureDefs(SIRStructure[] structs) {
        for (int i = 0; i < structs.length; i++) {
            SIRStructure current = structs[i];
            // write the typedef for the struct.
            buf.append(at.dms.kjc.common.CommonUtils.structToTypedef(current,true));
            buf.append("\n");
	}
    }

    /**
     * Append text to structs.h 
     *  
     * @param text The text to append
     */
    public void addText(String text) {
        buf.append(text);
    }
    
    /**
     * Append text plus a newline to the structs.h file
     * 
     * @param text The text to append
     */
    public void addLine(String text) {
        buf.append(text + "\n");
    }
    
    /**
     * Append text plus a semicolon and a newline to the structs.h file
     * 
     * @param text The text to append
     */
    public void addLineSC(String text) {
        buf.append(text + ";\n");
    }
    
    public void writeToFile() {
        try {
            buf.append("\n\n#endif\n");

            FileWriter fw = new FileWriter("structs.h");
            fw.write(buf.toString());
            fw.close();
        }
        catch (IOException e) {
            System.err.println("Error writing structs.h file!");
        }
    }
}
