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

package com.odiago.flumebase.parser;

import org.apache.avro.util.Utf8;

import org.testng.annotations.Test;

import static org.testng.AssertJUnit.*;

import com.odiago.flumebase.exec.HashSymbolTable;

import com.odiago.flumebase.lang.Type;
import com.odiago.flumebase.lang.TypeCheckException;
import com.odiago.flumebase.lang.TypeChecker;
import com.odiago.flumebase.lang.VisitException;

public class TestBinExpr extends ExprTestCase {

  @Test
  public void testTimes() throws Exception {
    Expr binExpr;
    TypeChecker checker;
    Object value;

    binExpr = new BinExpr(
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(4)),
        BinOp.Times,
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(2)));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertEquals(Integer.valueOf(8), value);

    binExpr = new BinExpr(
        new ConstExpr(Type.getNullable(Type.TypeName.INT), Integer.valueOf(4)),
        BinOp.Times,
        new ConstExpr(Type.getNullable(Type.TypeName.INT), Integer.valueOf(2)));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertEquals(Integer.valueOf(8), value);

    binExpr = new BinExpr(
        new ConstExpr(Type.getNullable(Type.TypeName.INT), null),
        BinOp.Times,
        new ConstExpr(Type.getNullable(Type.TypeName.INT), Integer.valueOf(2)));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertEquals(null, value);

    binExpr = new BinExpr(
        new ConstExpr(Type.getNullable(Type.TypeName.INT), Integer.valueOf(4)),
        BinOp.Times,
        new ConstExpr(Type.getNullable(Type.TypeName.INT), null));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertEquals(null, value);

    binExpr = new BinExpr(
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(4)),
        BinOp.Times,
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(-2)));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertEquals(Integer.valueOf(-8), value);

    binExpr = new BinExpr(
        new ConstExpr(Type.getPrimitive(Type.TypeName.BIGINT), Long.valueOf(4)),
        BinOp.Times,
        new ConstExpr(Type.getPrimitive(Type.TypeName.BIGINT), Long.valueOf(2)));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertEquals(Long.valueOf(8), value);

    binExpr = new BinExpr(
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(4)),
        BinOp.Times,
        new ConstExpr(Type.getPrimitive(Type.TypeName.BIGINT), Long.valueOf(2)));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertEquals(Long.valueOf(8), value);

    binExpr = new BinExpr(
        new ConstExpr(Type.getPrimitive(Type.TypeName.BIGINT), Long.valueOf(4)),
        BinOp.Times,
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(2)));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertEquals(Long.valueOf(8), value);

    binExpr = new BinExpr(
        new ConstExpr(Type.getPrimitive(Type.TypeName.FLOAT), Float.valueOf(12f)),
        BinOp.Times,
        new ConstExpr(Type.getPrimitive(Type.TypeName.FLOAT), Float.valueOf(2.5f)));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertEquals(Float.valueOf(12f * 2.5f), value);

    try {
      binExpr = new UnaryExpr(UnaryOp.Not,
          new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(42)));
      checker = new TypeChecker(new HashSymbolTable());
      binExpr.accept(checker);
      fail("Expected typechecker error on NOT(INTEGER)");
    } catch (TypeCheckException tce) {
      // expected this -- ok.
    }
  }

  @Test
  public void testPlus() throws Exception {
    Expr binExpr;
    TypeChecker checker;
    Object value;

    binExpr = new BinExpr(
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(4)),
        BinOp.Add,
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(2)));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertEquals(Integer.valueOf(6), value);

    binExpr = new BinExpr(
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(4)),
        BinOp.Add,
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(-2)));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertEquals(Integer.valueOf(2), value);

    binExpr = new BinExpr(
        new ConstExpr(Type.getPrimitive(Type.TypeName.STRING), new Utf8("abc")),
        BinOp.Add,
        new ConstExpr(Type.getPrimitive(Type.TypeName.STRING), new Utf8("def")));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertEquals("abcdef", value);

    binExpr = new BinExpr(
        new ConstExpr(Type.getPrimitive(Type.TypeName.STRING), new Utf8("abc")),
        BinOp.Add,
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(3)));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertEquals("abc3", value);

    binExpr = new BinExpr(
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(3)),
        BinOp.Add,
        new ConstExpr(Type.getPrimitive(Type.TypeName.STRING), new Utf8("xyz")));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertEquals("3xyz", value);
  }

  @Test
  public void testMinus() throws Exception {
    Expr binExpr;
    TypeChecker checker;
    Object value;

    binExpr = new BinExpr(
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(4)),
        BinOp.Subtract,
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(2)));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertEquals(Integer.valueOf(2), value);

    binExpr = new BinExpr(
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(4)),
        BinOp.Subtract,
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(-2)));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertEquals(Integer.valueOf(6), value);
  }

  @Test
  public void testDiv() throws Exception {
    Expr binExpr;
    TypeChecker checker;
    Object value;

    binExpr = new BinExpr(
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(6)),
        BinOp.Div,
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(2)));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertEquals(Integer.valueOf(3), value);

    binExpr = new BinExpr(
        new ConstExpr(Type.getPrimitive(Type.TypeName.FLOAT), Float.valueOf(7f)),
        BinOp.Div,
        new ConstExpr(Type.getPrimitive(Type.TypeName.FLOAT), Float.valueOf(2f)));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertEquals(Float.valueOf(7f/2f), value);
  }

  @Test
  public void testMod() throws Exception {
    Expr binExpr;
    TypeChecker checker;
    Object value;

    binExpr = new BinExpr(
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(7)),
        BinOp.Mod,
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(2)));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertEquals(Integer.valueOf(1), value);
  }

  @Test
  public void testGreater() throws Exception {
    Expr binExpr;
    TypeChecker checker;
    Object value;

    binExpr = new BinExpr(
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(7)),
        BinOp.Greater,
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(2)));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertEquals(Boolean.TRUE, value);

    binExpr = new BinExpr(
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(2)),
        BinOp.Greater,
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(7)));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertEquals(Boolean.FALSE, value);

    binExpr = new BinExpr(
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(4)),
        BinOp.Greater,
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(4)));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertEquals(Boolean.FALSE, value);

    // Note that booleans are comparable..
    binExpr = new BinExpr(
        new ConstExpr(Type.getPrimitive(Type.TypeName.BOOLEAN), Boolean.TRUE),
        BinOp.Greater,
        new ConstExpr(Type.getPrimitive(Type.TypeName.BOOLEAN), Boolean.FALSE));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertEquals(Boolean.TRUE, value);
  }

  @Test
  public void testGreaterEq() throws Exception {
    Expr binExpr;
    TypeChecker checker;
    Object value;

    binExpr = new BinExpr(
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(7)),
        BinOp.GreaterEq,
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(2)));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertEquals(Boolean.TRUE, value);

    binExpr = new BinExpr(
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(2)),
        BinOp.GreaterEq,
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(7)));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertEquals(Boolean.FALSE, value);

    binExpr = new BinExpr(
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(4)),
        BinOp.GreaterEq,
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(4)));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertEquals(Boolean.TRUE, value);
  }

  @Test
  public void testLessEq() throws Exception {
    Expr binExpr;
    TypeChecker checker;
    Object value;

    binExpr = new BinExpr(
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(7)),
        BinOp.LessEq,
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(2)));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertEquals(Boolean.FALSE, value);

    binExpr = new BinExpr(
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(2)),
        BinOp.LessEq,
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(7)));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertEquals(Boolean.TRUE, value);

    binExpr = new BinExpr(
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(4)),
        BinOp.LessEq,
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(4)));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertEquals(Boolean.TRUE, value);
  }

  @Test
  public void testLess() throws Exception {
    Expr binExpr;
    TypeChecker checker;
    Object value;

    binExpr = new BinExpr(
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(7)),
        BinOp.Less,
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(2)));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertEquals(Boolean.FALSE, value);

    binExpr = new BinExpr(
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(2)),
        BinOp.Less,
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(7)));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertEquals(Boolean.TRUE, value);

    binExpr = new BinExpr(
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(4)),
        BinOp.Less,
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(4)));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertEquals(Boolean.FALSE, value);
  }

  @Test
  public void testEq() throws Exception {
    Expr binExpr;
    TypeChecker checker;
    Object value;

    binExpr = new BinExpr(
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(7)),
        BinOp.Eq,
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(2)));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertEquals(Boolean.FALSE, value);

    binExpr = new BinExpr(
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(4)),
        BinOp.Eq,
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(4)));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertEquals(Boolean.TRUE, value);

    binExpr = new BinExpr(
        new ConstExpr(Type.getNullable(Type.TypeName.INT), null),
        BinOp.Eq,
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(4)));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertNull(value);

    binExpr = new BinExpr(
        new ConstExpr(Type.getPrimitive(Type.TypeName.STRING), new Utf8("abc")),
        BinOp.Eq,
        new ConstExpr(Type.getPrimitive(Type.TypeName.STRING), new Utf8("abc")));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertEquals(Boolean.TRUE, value);

    binExpr = new BinExpr(
        new ConstExpr(Type.getPrimitive(Type.TypeName.STRING), new Utf8("abc")),
        BinOp.Eq,
        new ConstExpr(Type.getPrimitive(Type.TypeName.STRING), new Utf8("ABC")));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertEquals(Boolean.FALSE, value);

    // integer-to-string promotion holds for the '=' operator too.

    binExpr = new BinExpr(
        new ConstExpr(Type.getPrimitive(Type.TypeName.STRING), new Utf8("4")),
        BinOp.Eq,
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(4)));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertEquals(Boolean.TRUE, value);

    binExpr = new BinExpr(
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(4)),
        BinOp.Eq,
        new ConstExpr(Type.getPrimitive(Type.TypeName.STRING), new Utf8("4")));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertEquals(Boolean.TRUE, value);
  }

  @Test
  public void testNotEq() throws Exception {
    Expr binExpr;
    TypeChecker checker;
    Object value;

    binExpr = new BinExpr(
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(7)),
        BinOp.NotEq,
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(2)));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertEquals(Boolean.TRUE, value);

    binExpr = new BinExpr(
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(4)),
        BinOp.NotEq,
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(4)));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertEquals(Boolean.FALSE, value);

    binExpr = new BinExpr(
        new ConstExpr(Type.getNullable(Type.TypeName.INT), null),
        BinOp.NotEq,
        new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(4)));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertNull(value);

    binExpr = new BinExpr(
        new ConstExpr(Type.getPrimitive(Type.TypeName.STRING), new Utf8("abc")),
        BinOp.NotEq,
        new ConstExpr(Type.getPrimitive(Type.TypeName.STRING), new Utf8("abc")));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertEquals(Boolean.FALSE, value);

    binExpr = new BinExpr(
        new ConstExpr(Type.getPrimitive(Type.TypeName.STRING), new Utf8("abc")),
        BinOp.NotEq,
        new ConstExpr(Type.getPrimitive(Type.TypeName.STRING), new Utf8("ABC")));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertEquals(Boolean.TRUE, value);
  }

  @Test
  public void testAnd() throws Exception {
    Expr binExpr;
    TypeChecker checker;
    Object value;

    binExpr = new BinExpr(
        new ConstExpr(Type.getPrimitive(Type.TypeName.BOOLEAN), Boolean.TRUE),
        BinOp.And,
        new ConstExpr(Type.getPrimitive(Type.TypeName.BOOLEAN), Boolean.TRUE));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertEquals(Boolean.TRUE, value);

    binExpr = new BinExpr(
        new ConstExpr(Type.getPrimitive(Type.TypeName.BOOLEAN), Boolean.FALSE),
        BinOp.And,
        new ConstExpr(Type.getPrimitive(Type.TypeName.BOOLEAN), Boolean.TRUE));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertEquals(Boolean.FALSE, value);

    binExpr = new BinExpr(
        new ConstExpr(Type.getPrimitive(Type.TypeName.BOOLEAN), Boolean.TRUE),
        BinOp.And,
        new ConstExpr(Type.getPrimitive(Type.TypeName.BOOLEAN), Boolean.FALSE));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertEquals(Boolean.FALSE, value);

    binExpr = new BinExpr(
        new ConstExpr(Type.getPrimitive(Type.TypeName.BOOLEAN), Boolean.FALSE),
        BinOp.And,
        new ConstExpr(Type.getPrimitive(Type.TypeName.BOOLEAN), Boolean.FALSE));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertEquals(Boolean.FALSE, value);

    try {
      // This should cause a typechecker exception.
      binExpr = new BinExpr(
          new ConstExpr(Type.getPrimitive(Type.TypeName.BOOLEAN), Boolean.FALSE),
          BinOp.And,
          new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(4)));
      checker = new TypeChecker(new HashSymbolTable());
      binExpr.accept(checker);

      fail("Expected typechecker error ; int does not promote to boolean.");
    } catch (VisitException ve) {
      // Expected.
    }
  }

  @Test
  public void testOr() throws Exception {
    Expr binExpr;
    TypeChecker checker;
    Object value;

    binExpr = new BinExpr(
        new ConstExpr(Type.getPrimitive(Type.TypeName.BOOLEAN), Boolean.TRUE),
        BinOp.Or,
        new ConstExpr(Type.getPrimitive(Type.TypeName.BOOLEAN), Boolean.TRUE));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertEquals(Boolean.TRUE, value);

    binExpr = new BinExpr(
        new ConstExpr(Type.getPrimitive(Type.TypeName.BOOLEAN), Boolean.FALSE),
        BinOp.Or,
        new ConstExpr(Type.getPrimitive(Type.TypeName.BOOLEAN), Boolean.TRUE));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertEquals(Boolean.TRUE, value);

    binExpr = new BinExpr(
        new ConstExpr(Type.getPrimitive(Type.TypeName.BOOLEAN), Boolean.TRUE),
        BinOp.Or,
        new ConstExpr(Type.getPrimitive(Type.TypeName.BOOLEAN), Boolean.FALSE));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertEquals(Boolean.TRUE, value);

    binExpr = new BinExpr(
        new ConstExpr(Type.getPrimitive(Type.TypeName.BOOLEAN), Boolean.FALSE),
        BinOp.Or,
        new ConstExpr(Type.getPrimitive(Type.TypeName.BOOLEAN), Boolean.FALSE));
    checker = new TypeChecker(new HashSymbolTable());
    binExpr.accept(checker);
    value = binExpr.eval(getEmptyEventWrapper());
    assertEquals(Boolean.FALSE, value);

    try {
      // This should cause a typechecker exception.
      binExpr = new BinExpr(
          new ConstExpr(Type.getPrimitive(Type.TypeName.BOOLEAN), Boolean.FALSE),
          BinOp.Or,
          new ConstExpr(Type.getPrimitive(Type.TypeName.INT), Integer.valueOf(4)));
      checker = new TypeChecker(new HashSymbolTable());
      binExpr.accept(checker);

      fail("Expected typechecker error ; int does not promote to boolean.");
    } catch (VisitException ve) {
      // Expected.
    }
  }
}
