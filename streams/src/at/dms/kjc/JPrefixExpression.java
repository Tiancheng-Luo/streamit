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
 * $Id: JPrefixExpression.java,v 1.1 2001-08-30 16:32:52 thies Exp $
 */

package at.dms.kjc;

import at.dms.classfile.IincInstruction;
import at.dms.classfile.PushLiteralInstruction;
import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;

/**
 * This class represents prefix increment and decrement expressions.
 * 15.13.2 Prefix Increment Operator ++
 * 15.13.3 Prefix Decrement Operator --
 */
public class JPrefixExpression extends JExpression {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

  /**
   * Construct a node in the parsing tree
   * @param	where		the line of this node in the source code
   * @param	oper		the operator
   * @param	expr		the operand
   */
  public JPrefixExpression(TokenReference where, int oper, JExpression expr) {
    super(where);
    this.oper = oper;
    this.expr = expr;
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * Compute the type of this expression (called after parsing)
   * @return the type of this expression
   */
  public CType getType() {
    return type;
  }

  /**
   * Returns true iff this expression can be used as a statement (JLS 14.8)
   */
  public boolean isStatementExpression() {
    return true;
  }

  // ----------------------------------------------------------------------
  // SEMANTIC ANALYSIS
  // ----------------------------------------------------------------------

  /**
   * Analyses the expression (semantically).
   * @param	context		the analysis context
   * @return	an equivalent, analysed expression
   * @exception	PositionedError	the analysis detected an error
   */
  public JExpression analyse(CExpressionContext context) throws PositionedError {
    expr = expr.analyse(new CExpressionContext(context, true, context.discardValue()));
    check(context, expr.getType().isNumeric(), KjcMessages.PREFIX_BADTYPE, expr.getType());
    check(context, expr.isLValue(context), KjcMessages.PREFIX_NOTLVALUE);
    check(context, expr.isInitialized(context), KjcMessages.PREFIX_NOTINITIALIZED);

    type = expr.getType();

    return this;
  }

  // ----------------------------------------------------------------------
  // CODE GENERATION
  // ----------------------------------------------------------------------

  /**
   * Accepts the specified visitor
   * @param	p		the visitor
   */
  public void accept(KjcVisitor p) {
    p.visitPrefixExpression(this, oper, expr);
  }

  /**
   * Generates JVM bytecode to evaluate this expression.
   *
   * @param	code		the bytecode sequence
   * @param	discardValue	discard the result of the evaluation ?
   */
  public void genCode(CodeSequence code, boolean discardValue) {
    setLineNumber(code);

    int			val = oper == OPE_PREINC ? 1 : -1;

    if ((expr.getType() == CStdType.Integer) &&
	(expr instanceof JLocalVariableExpression)) {
      code.plantInstruction(new IincInstruction(((JLocalVariableExpression)expr).getPosition(),
						val));
      expr.genCode(code, discardValue);
    } else {
      expr.genStartStoreCode(code);

      expr.genCode(code, discardValue);
      switch (expr.getType().getTypeID()) {
	case TID_FLOAT:
	  code.plantInstruction(new PushLiteralInstruction((float)val));
	  code.plantNoArgInstruction(opc_fadd);
	  break;
	case TID_LONG:
	  code.plantInstruction(new PushLiteralInstruction((long)val));
	  code.plantNoArgInstruction(opc_ladd);
	  break;
	case TID_DOUBLE:
	  code.plantInstruction(new PushLiteralInstruction((double)val));
	  code.plantNoArgInstruction(opc_dadd);
	  break;

	case TID_BYTE:
	case TID_CHAR:
	case TID_SHORT:
	case TID_INT:
	  code.plantInstruction(new PushLiteralInstruction(val));
	  code.plantNoArgInstruction(opc_iadd);
	  switch (expr.getType().getTypeID()) {
	  case TID_BYTE:
	    code.plantNoArgInstruction(opc_i2b);
	    break;
	  case TID_CHAR:
	    code.plantNoArgInstruction(opc_i2c);
	    break;
	  case TID_SHORT:
	    code.plantNoArgInstruction(opc_i2s);
	  }
      }

      expr.genEndStoreCode(code, discardValue);
    }
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  protected int			oper;
  protected JExpression		expr;
  protected CType		type;
}
