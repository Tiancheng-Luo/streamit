/*
 * Copyright (C) 1990-2001 DMS Decision Management Systems Ges.m.b.H.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * $Id: DefinitionFile.java,v 1.1 2001-08-30 16:32:46 thies Exp $
 */

package at.dms.compiler.tools.optgen;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.Vector;

import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;
import at.dms.compiler.tools.antlr.runtime.ParserException;
import at.dms.util.Utils;

class DefinitionFile {

  /**
   * Constructs a  definition file
   */
  public DefinitionFile(String sourceFile,
			String fileHeader,
			String packageName,
 			String parent,
 			String prefix,
 			String version,
 			String usage,
 			String help,		//!!! graf 991208 not used: remove
			OptionDefinition[] definitions)
  {
    this.sourceFile	= sourceFile;
    this.fileHeader	= fileHeader;
    this.packageName	= packageName;
    this.parent		= parent;
    this.version	= version == null ? null : version.substring(1, version.length() - 1);
    this.usage		= usage;
    this.prefix		= prefix;
    this.definitions	= definitions;
  }

  /**
   * Reads and parses an token definition file
   *
   * @param	sourceFile		the name of the source file
   * @return	a class info structure holding the information from the source
   *
   */
  public static DefinitionFile read(String sourceFile) throws OptgenError {
    try {
      InputStream	input = new BufferedInputStream(new FileInputStream(sourceFile));
      OptgenLexer	scanner = new OptgenLexer(input);
      OptgenParser	parser = new OptgenParser(scanner);
      DefinitionFile	defs = parser.aCompilationUnit(sourceFile);

      input.close();

      return defs;
    } catch (FileNotFoundException e) {
      throw new OptgenError(OptgenMessages.FILE_NOT_FOUND, sourceFile);
    } catch (IOException e) {
      throw new OptgenError(OptgenMessages.IO_EXCEPTION, sourceFile, e.getMessage());
    } catch (ParserException e) {
      throw new OptgenError(OptgenMessages.FORMATTED_ERROR,
			    new PositionedError(new TokenReference(sourceFile, e.getLine()),
						OptgenMessages.SYNTAX_ERROR,
						e.getMessage()));
    }
  }

  // --------------------------------------------------------------------
  // ACCESSORS
  // --------------------------------------------------------------------

  /**
   * Sets the version. Overrides the version supplied in the definitions file.
   */
  public void setVersion(String version) {
    this.version = version;
  }

  /**
   * Checks for duplicate identifiers.
   */
  public void checkIdentifiers() throws OptgenError {
    Hashtable		identifiers = new Hashtable();

    for (int i = 0; i < definitions.length; i++) {
      definitions[i].checkIdentifiers(identifiers, sourceFile);
    }
  }

  /**
o   * Checks for duplicate shortcuts.
   *
   */
  public void checkShortcuts() throws OptgenError {
    Hashtable		identifiers = new Hashtable();

    for (int i = 0; i < definitions.length; i++) {
      definitions[i].checkShortcuts(identifiers, sourceFile);
    }
  }

  /**
   * Generates the option parser.
   *
   * @param	out		the output stream
   */
  public void printFile(PrintWriter out) {
    if (fileHeader != null) {
      out.println(fileHeader);
    }
    out.print("// Generated by optgen from " + sourceFile);
    out.println();
    out.println("package " + packageName + ";");
    out.println();
    out.println("import gnu.getopt.Getopt;");
    out.println("import gnu.getopt.LongOpt;");
    out.println();
    out.print("public class " + prefix + "Options");
    out.print(parent == null ? "" : " extends " + parent);
    out.println(" {");

    // CONSTRUCTORS
    out.println();
    out.println("  public " + prefix + "Options(String name) {");
    out.println("    super(name);");
    out.println("  }");
    out.println();
    out.println("  public " + prefix + "Options() {");
    out.println("    this(\"" + prefix + "\");");
    out.println("  }");

    // FIELDS
    for (int i = 0; i < definitions.length; i++) {
      definitions[i].printFields(out);
    }

    // PROCESSOPTION
    out.println();
    out.println("  public boolean processOption(int code, Getopt g) {");
    out.println("    switch (code) {");
    for (int i = 0; i < definitions.length; i++) {
      definitions[i].printParseArgument(out);
    }
    out.println("    default:");
    out.println("      return super.processOption(code, g);");
    out.println("    }");
    out.println("  }");


    // GETOPTIONS
    out.println();
    out.println("  public String[] getOptions() {");
    out.println("    String[]	parent = super.getOptions();");
    out.println("    String[]	total = new String[parent.length + " + definitions.length+ "];");
    out.println("    System.arraycopy(parent, 0, total, 0, parent.length);");
    for (int i = 0; i < definitions.length; i++) {
      out.print("    total[parent.length + " + i + "] = ");
      definitions[i].printUsage(out);
      out.println(";");
    }
    out.println("    ");
    out.println("    return total;");
    out.println("  }");

    // GETSHORTOPTIONS
    out.println("\n");
    out.println("  public String getShortOptions() {");
    out.print("    return \"");
    for (int i = 0; i < definitions.length; i++) {
      definitions[i].printShortOption(out);
    }
    out.println("\" + super.getShortOptions();");
    out.println("  }");

    // VERSION
    out.println("\n");
    out.println("  public void version() {");
    out.print("    System.out.println(");
    out.print(version == null ? "" : "\"" + version + "\"");
    out.println(");");
    out.println("  }");

    // USAGE
    out.println("\n");
    out.println("  public void usage() {");
    out.print("    System.err.println(");
    out.print(usage);
    out.println(");");
    out.println("  }");

    // HELP
    out.println("\n");
    out.println("  public void help() {");
    out.print("    System.err.println(");
    out.print(usage);
    out.println(");");
    out.println("    printOptions();");
    out.println("    System.err.println();");
    out.println("    version();");
    out.println("    System.err.println();");
    out.print("    System.err.println(");
    out.print("\"This program is part of the Kopi Suite.\"");
    out.println(");");
    out.print("    System.err.println(");
    out.print("\"For more info, please see: http://www.dms.at/kopi\"");
    out.println(");");
    out.println("  }");

    // GETLONGOPTIONS
    out.println();
    out.println("  public LongOpt[] getLongOptions() {");
    out.println("    LongOpt[]	parent = super.getLongOptions();");
    out.println("    LongOpt[]	total = new LongOpt[parent.length + LONGOPTS.length];");
    out.println("    ");
    out.println("    System.arraycopy(parent, 0, total, 0, parent.length);");
    out.println("    System.arraycopy(LONGOPTS, 0, total, parent.length, LONGOPTS.length);");
    out.println("    ");
    out.println("    return total;");
    out.println("  }");

    // LONGOPTS
    out.println();
    out.println("  private static final LongOpt[] LONGOPTS = {");
    for (int i = 0; i < definitions.length; i++) {
      if (i != 0) {
	out.println(",");
      }
      definitions[i].printLongOpts(out);
    }
    out.println();
    out.println("  };");

    out.println("}");
  }

  /**
   * Returns the package name
   */
  public String getClassName() {
    return packageName + "." + prefix + "Options";
  }

  /**
   * Returns the package name
   */
  public String getPackageName() {
    return packageName;
  }

  /**
   * Returns the literal prefix
   */
  public String getPrefix() {
    return prefix;
  }

  // --------------------------------------------------------------------
  // DATA MEMBERS
  // --------------------------------------------------------------------

  private final String			sourceFile;
  private final String			fileHeader;
  private final String			packageName;
  private final String			parent;
  private final String			prefix;
  private String			version;
  private final String			usage;
  private final OptionDefinition[]	definitions;
}
