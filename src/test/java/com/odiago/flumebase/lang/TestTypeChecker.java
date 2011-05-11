/**
 * Licensed to Odiago, Inc. under one or more contributor license
 * agreements.  See the NOTICE.txt file distributed with this work for
 * additional information regarding copyright ownership.  Odiago, Inc.
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.odiago.flumebase.lang;

import org.testng.annotations.Test;

import com.odiago.flumebase.exec.AssignedSymbol;
import com.odiago.flumebase.exec.HashSymbolTable;
import com.odiago.flumebase.exec.SymbolTable;

import com.odiago.flumebase.parser.BinExpr;
import com.odiago.flumebase.parser.BinOp;
import com.odiago.flumebase.parser.ConstExpr;
import com.odiago.flumebase.parser.Expr;
import com.odiago.flumebase.parser.IdentifierExpr;

public class TestTypeChecker {

  @Test
  public void testBasicBinop() throws VisitException {
    Expr binopExpr = new BinExpr(
      new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(2)),
      BinOp.Add, new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(3)));
    TypeChecker tc = new TypeChecker(new HashSymbolTable());
    binopExpr.accept(tc);
  }

  @Test
  public void testNestedBinop() throws VisitException {
    Expr binopExpr = new BinExpr(
      new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(2)),
      BinOp.Add, 
      new BinExpr(
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(3)),
        BinOp.Add, 
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(4))));

    TypeChecker tc = new TypeChecker(new HashSymbolTable());
    binopExpr.accept(tc);
  }

  @Test(expectedExceptions = VisitException.class)
  public void testBasicBinopFail() throws VisitException {
    // can't add INT and TIMESTAMP.
    Expr binopExpr = new BinExpr(
      new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(2)),
      BinOp.Add, new ConstExpr(Type.getPrimitive(Type.TypeName.TIMESTAMP), Integer.valueOf(3)));
    TypeChecker tc = new TypeChecker(new HashSymbolTable());
    binopExpr.accept(tc);
  }

  @Test(expectedExceptions = VisitException.class)
  public void testNestedBinopFail() throws VisitException {
    // can't add INT and TIMESTAMP in a subexpr.
    Expr binopExpr = new BinExpr(
      new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(2)),
      BinOp.Add, 
      new BinExpr(
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(3)),
        BinOp.Add, 
        new ConstExpr(Type.getPrimitive(Type.TypeName.TIMESTAMP), Integer.valueOf(4))));

    TypeChecker tc = new TypeChecker(new HashSymbolTable());
    binopExpr.accept(tc);
  }

  @Test
  public void testPromotion1() throws VisitException {
    // Test that INT can promote to BIGINT.
    Expr binopExpr = new BinExpr(
      new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(2)),
      BinOp.Add, new ConstExpr(Type.getPrimitive(Type.TypeName.BIGINT), Integer.valueOf(3)));
    TypeChecker tc = new TypeChecker(new HashSymbolTable());
    binopExpr.accept(tc);
  }

  @Test
  public void testPromotion2() throws VisitException {
    // Test that INT can promote to BIGINT on the lhs.
    Expr binopExpr = new BinExpr(
      new ConstExpr(Type.getPrimitive(Type.TypeName.BIGINT), Integer.valueOf(2)),
      BinOp.Add, new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(3)));
    TypeChecker tc = new TypeChecker(new HashSymbolTable());
    binopExpr.accept(tc);
  }

  @Test
  public void testIdentifier() throws VisitException {
    // Test that we can look up an identifier in the symbol table.
    SymbolTable symbols = new HashSymbolTable();
    symbols.addSymbol(new AssignedSymbol("x", Type.getPrimitive(Type.TypeName.INT), "x",
        IdentifierExpr.AccessType.FIELD));

    Expr binopExpr = new BinExpr(
      new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(2)),
      BinOp.Add, new IdentifierExpr("x"));
    TypeChecker tc = new TypeChecker(symbols);
    binopExpr.accept(tc);
  }

  @Test
  public void testIdentifierPromotion() throws VisitException {
    // Test that an identifier's type can promote to a constant.
    SymbolTable symbols = new HashSymbolTable();
    symbols.addSymbol(new AssignedSymbol("x", Type.getPrimitive(Type.TypeName.INT), "x",
        IdentifierExpr.AccessType.FIELD));

    Expr binopExpr = new BinExpr(
      new ConstExpr(Type.getPrimitive(Type.TypeName.BIGINT), Integer.valueOf(2)),
      BinOp.Add, new IdentifierExpr("x"));
    TypeChecker tc = new TypeChecker(symbols);
    binopExpr.accept(tc);
  }

  @Test
  public void testIdentifierPromotion2() throws VisitException {
    // Test that a const's type can promote to an identifier's.
    SymbolTable symbols = new HashSymbolTable();
    symbols.addSymbol(new AssignedSymbol("x", Type.getPrimitive(Type.TypeName.BIGINT), "x",
        IdentifierExpr.AccessType.FIELD));

    Expr binopExpr = new BinExpr(
      new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(2)),
      BinOp.Add, new IdentifierExpr("x"));
    TypeChecker tc = new TypeChecker(symbols);
    binopExpr.accept(tc);
  }

  // TODO:
  // Test unary expressions
  // Test restrictions on aliasedexpr with AllFieldsExpr
  // Test select stmt, inferring field types from a stream in the symboltable.
}
