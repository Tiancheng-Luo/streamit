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
 * $Id: JDivideExpression.java,v 1.1 2001-08-30 16:32:51 thies Exp $
 */

package at.dms.kjc;

import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;
import at.dms.compiler.UnpositionedError;

/**
 * This class implements '/' specific operations
 * Plus operand may be String, numbers
 */
public class JDivideExpression extends JBinaryArithmeticExpression {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

  /**
   * Construct a node in the parsing tree
   * This method is directly called by the parser
   * @param	where		the line of this node in the source code
   * @param	left		the left operand
   * @param	right		the right operand
   */
  public JDivideExpression(TokenReference where,
			   JExpression left,
			   JExpression right) {
    super(where, left, right);
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
    left = left.analyse(context);
    right = right.analyse(context);

    try {
      type = computeType(left.getType(), right.getType());
    } catch (UnpositionedError e) {
      throw e.addPosition(getTokenReference());
    }

    if (type.isOrdinal() && right.isConstant() && ((JLiteral)right).isDefault()) {
      throw new PositionedError(getTokenReference(), KjcMessages.DIVIDE_BY_ZERO);
    }

    left = left.convertType(type, context);
    right = right.convertType(type, context);

    if (left.isConstant() && right.isConstant()) {
      return constantFolding();
    } else {
      return this;
    }
  }

  /**
   * compute the type of this expression according to operands
   * @param	leftType		the type of left operand
   * @param	rightType		the type of right operand
   * @return	the type computed for this binary operation
   * @exception	UnpositionedError	this error will be positioned soon
   */
  public static CType computeType(CType	leftType, CType rightType) throws UnpositionedError {
    if (leftType.isNumeric() && rightType.isNumeric()) {
      return CNumericType.binaryPromote(leftType, rightType);
    }
    throw new UnpositionedError(KjcMessages.DIVIDE_BADTYPE, leftType, rightType);
  }

  // ----------------------------------------------------------------------
  // CONSTANT FOLDING
  // ----------------------------------------------------------------------

  /**
   * Computes the result of the operation at compile-time (JLS 15.28).
   * @param	left		the first operand
   * @param	right		the seconds operand
   * @return	the result of the operation
   */
  public int compute(int left, int right) {
    return left / right;
  }

  /**
   * Computes the result of the operation at compile-time (JLS 15.28).
   * @param	left		the first operand
   * @param	right		the seconds operand
   * @return	the result of the operation
   */
  public long compute(long left, long right) {
    return left / right;
  }

  /**
   * Computes the result of the operation at compile-time (JLS 15.28).
   * @param	left		the first operand
   * @param	right		the seconds operand
   * @return	the result of the operation
   */
  public float compute(float left, float right) {
    return left / right;
  }

  /**
   * Computes the result of the operation at compile-time (JLS 15.28).
   * @param	left		the first operand
   * @param	right		the seconds operand
   * @return	the result of the operation
   */
  public double compute(double left, double right) {
    return left / right;
  }

  // ----------------------------------------------------------------------
  // CODE GENERATION
  // ----------------------------------------------------------------------

  /**
   * Accepts the specified visitor
   * @param	p		the visitor
   */
  public void accept(KjcVisitor p) {
    p.visitBinaryExpression(this, "/", left, right);
  }

  /**
   * @param	type		the type of result
   * @return	the type of opcode for this operation
   */
  public static int getOpcode(CType type) {
    switch (type.getTypeID()) {
    case TID_FLOAT:
      return opc_fdiv;
    case TID_LONG:
      return opc_ldiv;
    case TID_DOUBLE:
      return opc_ddiv;
    default:
      return opc_idiv;
    }
  }

  /**
   * Generates JVM bytecode to evaluate this expression.
   *
   * @param	code		the bytecode sequence
   * @param	discardValue	discard the result of the evaluation ?
   */
  public void genCode(CodeSequence code, boolean discardValue) {
    setLineNumber(code);

    left.genCode(code, false);
    right.genCode(code, false);
    code.plantNoArgInstruction(getOpcode(getType()));

    if (discardValue) {
      code.plantPopInstruction(getType());
    }
  }
}
