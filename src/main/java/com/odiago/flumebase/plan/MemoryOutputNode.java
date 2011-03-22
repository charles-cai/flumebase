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

package com.odiago.flumebase.plan;

import java.util.List;

import com.odiago.flumebase.parser.TypedField;

import com.odiago.flumebase.util.StringUtils;

/**
 * Node that emits specific fields from all input records to a memory buffer.
 */
public class MemoryOutputNode extends PlanNode {
  
  /** The set of field names and types to emit to the console. */
  private List<TypedField> mOutputFields;

  /**
   * The name to bind the MemoryOutputElement to, for later retrieval
   * by the client.
   */
  private String mBufferName;

  public MemoryOutputNode(String memoryBufferName, List<TypedField> fields) {
    mBufferName = memoryBufferName;
    mOutputFields = fields;
  }

  public List<TypedField> getFields() {
    return mOutputFields;
  }

  public String getName() {
    return mBufferName;
  }

  @Override 
  public void formatParams(StringBuilder sb) {
    sb.append("MemoryOutput(mBufferName=");
    sb.append(mBufferName);
    sb.append(", mOutputFields=(");
    StringUtils.formatList(sb, mOutputFields);
    sb.append("))\n");
    formatAttributes(sb);
  }
}
