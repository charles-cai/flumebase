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

package com.odiago.flumebase.exec;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.nio.ByteBuffer;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.avro.Schema;

import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;

import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.Encoder;

import org.apache.thrift.TException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.flume.core.Event;
import com.cloudera.flume.core.EventImpl;

import com.odiago.flumebase.exec.builtins.bin2str;

import com.odiago.flumebase.exec.local.LocalContext;

import com.odiago.flumebase.flume.EmbeddedFlumeConfig;
import com.odiago.flumebase.flume.SourceContext;
import com.odiago.flumebase.flume.SourceContextBindings;

import com.odiago.flumebase.io.AvroEventParser;

import com.odiago.flumebase.lang.StreamType;
import com.odiago.flumebase.lang.Timestamp;
import com.odiago.flumebase.lang.TimestampBase;
import com.odiago.flumebase.lang.Type;

import com.odiago.flumebase.parser.FormatSpec;
import com.odiago.flumebase.parser.SQLStatement;
import com.odiago.flumebase.parser.StreamSourceType;
import com.odiago.flumebase.parser.TypedField;

import com.odiago.flumebase.server.UserSession;

import com.odiago.flumebase.util.StringUtils;

/**
 * FlowElement that sits at the end of a query flow.
 * Outputs records to foreign sources:
 * <ul>
 *   <li>Prints events to the consoles of each subscriber.
 *   <li>Emits Avro records on a named Flume stream.
 * </ul>
 */
public class OutputElement extends FlowElementImpl {
  private static final Logger LOG = LoggerFactory.getLogger(
      OutputElement.class.getName());

  /** Max queue length we deliver to Flume before blocking. */
  private static final int MAX_QUEUE_LEN = 512;

  private static bin2str BIN2STR_FN; // Used for stringifying ByteBuffers.
  static {
    BIN2STR_FN = new bin2str();
  }

  /** Input fields being delivered to this node. */
  private List<TypedField> mInputFields;

  /** Interface with Flume */
  private EmbeddedFlumeConfig mFlumeConfig;

  /** Name of Flume logical node that broadcasts the results of this query. */
  private String mFlumeNodeName;

  /**
   * Set to true if this OutputNode manages the symbol associated with
   * mFlumeNodeName.
   */
  private boolean mOwnsSymbol;

  /** Queue of events that are delivered to Flume by this OutputElement. */
  private BlockingQueue<Event> mOutputQueue;

  /** De-dup'd version of mInputFields, for version emitted to Flume. */
  private List<TypedField> mFlumeInputFields;

  /** Output fields emitted to Flume. */
  private List<TypedField> mOutputFields;

  /** Output schema emitted to Flume. */
  private Schema mOutputSchema;

  /** Symbol table where stream definitions are introduced. */
  private SymbolTable mRootSymbolTable;

  // Objects used for Avro serialization.
  private Encoder mEncoder;
  private GenericDatumWriter<GenericRecord> mDatumWriter;
  private ByteArrayOutputStream mOutputBytes;

  public OutputElement(FlowElementContext context, Schema inputSchema,
      List<TypedField> fields, EmbeddedFlumeConfig flumeConfig, String flumeNodeName,
      Schema outputSchema, List<TypedField> outputFields, SymbolTable rootSymbolTable) {
    super(context);

    mInputFields = fields;
    mFlumeConfig = flumeConfig;
    mFlumeNodeName = flumeNodeName;
    mOutputQueue = null;
    mOutputSchema = outputSchema;
    mOutputFields = outputFields;
    mRootSymbolTable = rootSymbolTable;
    mOwnsSymbol = false;

    mFlumeInputFields = SQLStatement.distinctFields(mInputFields);
    assert mFlumeInputFields.size() == mOutputFields.size();

    mDatumWriter = new GenericDatumWriter<GenericRecord>(outputSchema);
    mOutputBytes = new ByteArrayOutputStream();
    mEncoder = new BinaryEncoder(mOutputBytes);
  }

  private StringBuilder formatHeader() {
    StringBuilder sb = new StringBuilder();
    sb.append("timestamp");
    for (TypedField field : mInputFields) {
      sb.append("\t");
      sb.append(field.getDisplayName());
    }
    return sb;
  }

  @Override
  public void onConnect(UserSession session) {
    // When a user first connects, print the header for our output columns.
    session.sendInfo(formatHeader().toString());
  }

  @Override
  public void open() throws IOException {
    if (null != mFlumeNodeName) {
      if (!mFlumeConfig.isRunning()) {
        mFlumeConfig.start();
      }

      // Open a Flume logical node to host the results of this query.
      // TODO(aaron): What happens if this flume node already exists? This should error...
      mOutputQueue = new ArrayBlockingQueue<Event>(MAX_QUEUE_LEN);
      SourceContext srcContext = new SourceContext(mFlumeNodeName, mOutputQueue);
      SourceContextBindings.get().bindContext(mFlumeNodeName, srcContext);
      try {
        mFlumeConfig.spawnLogicalNode(mFlumeNodeName,
            "rtsqlsource(\"" + mFlumeNodeName + "\")",
            "rtsqlmultisink(\"" + mFlumeNodeName + "\")");
        mFlumeConfig.addLocalMultiSink(mFlumeNodeName);

        if (mRootSymbolTable.resolve(mFlumeNodeName) != null) {
          // TODO(aaron): This should make it back to the UserSession who submitted
          // the job, if this is the first call to open(), or to the UserSession who
          // bound the query to the current output name.
          // Also, should we fail the job? etc etc... check preconditions?
          LOG.error("Cannot create stream for flow; object already exists at top level: "
              + mFlumeNodeName);
          mOwnsSymbol = false;
          ((LocalContext) getContext()).getFlowData().setStreamName(null);
        } else {
          FormatSpec formatSpec = new FormatSpec(FormatSpec.FORMAT_AVRO);
          formatSpec.setParam(AvroEventParser.SCHEMA_PARAM, mOutputSchema.toString());

          List<Type> outputTypes = new ArrayList<Type>();
          for (TypedField field : mFlumeInputFields) {
            outputTypes.add(field.getType());
          }
  
          Type streamType = new StreamType(outputTypes);
          StreamSymbol streamSym = new StreamSymbol(mFlumeNodeName, StreamSourceType.Node,
              streamType, mFlumeNodeName, true, mOutputFields, formatSpec);
          if (!streamSym.getEventParser().validate(streamSym)) {
            throw new IOException("Could not create valid stream for schema");
          }
          mRootSymbolTable.addSymbol(streamSym);
          mOwnsSymbol = true;
          ((LocalContext) getContext()).getFlowData().setStreamName(mFlumeNodeName);
          LOG.info("CREATE STREAM (" + mFlumeNodeName + ")");
        }
      } catch (TException te) {
        throw new IOException(te);
      }
    }
  }

  /**
   * Sets the logical node which should broadcast this flow's output.  If
   * logicalNodeName is null, disables Flume-based output of this flow.  If
   * this FlowElement was previously broadcasting to a particular logical
   * node, this will close any prior logical node before opening a new one.
   */
  public void setFlumeTarget(String logicalNodeName) throws IOException {
    if (mFlumeNodeName != null) {
      if (mFlumeNodeName.equals(logicalNodeName)) {
        // Nothing to do.
        return;
      } else {
        stopFlumeNode();
      }
    }

    mFlumeNodeName = logicalNodeName;
    // Create a new output record schema, with records taking the new stream name.
    if (null != mFlumeNodeName) {
      mOutputSchema = SQLStatement.createFieldSchema(mOutputFields, mFlumeNodeName);
      mDatumWriter = new GenericDatumWriter<GenericRecord>(mOutputSchema);
      open();
    }
  }

  /**
   * Stops the current Flume node broadcasting our output.
   */
  private void stopFlumeNode() throws IOException {
    if (mFlumeNodeName != null) {
      if (mOwnsSymbol) {
        // TODO: Broadcast this DROP STREAM event back to the user who ordered the config change.
        mRootSymbolTable.remove(mFlumeNodeName);
        ((LocalContext) getContext()).getFlowData().setStreamName(null);
        mOwnsSymbol = false;
      }
      try {
        mFlumeConfig.dropLocalMultiSink(mFlumeNodeName);
        mFlumeConfig.decommissionLogicalNode(mFlumeNodeName);
      } catch (TException te) {
        throw new IOException(te);
      } finally {
        SourceContextBindings.get().dropContext(mFlumeNodeName);
        mOutputQueue = null;
      }
    }
  }

  @Override
  public void close() throws IOException, InterruptedException {
    try {
      stopFlumeNode();
    } catch (IOException ioe) {
      LOG.error("TException decommissioning logical node " + mFlumeNodeName + ": " + ioe);
    }

    super.close();
  }

  /**
   * Format the internal event as an Avro record of the output schema,
   * and emit it to the Flume node via our queue.
   */
  private void emitToFlume(EventWrapper e) throws IOException, InterruptedException {
    GenericData.Record record = new GenericData.Record(mOutputSchema);
    for (int i = 0; i < mFlumeInputFields.size(); i++) {
      TypedField inField = mFlumeInputFields.get(i);
      TypedField outField = mOutputFields.get(i);

      Object val = e.getField(inField);
      record.put(outField.getAvroName(), val);
    }

    mOutputBytes.reset();
    try {
      mDatumWriter.write(record, mEncoder);
    } catch (NullPointerException npe) {
      // Schema error -- null output value in non-null field. Drop the record.
      LOG.debug("Dropping output record with NULL value in non-null field: " + npe);
      return;
    }

    Event out = new EventImpl(mOutputBytes.toByteArray());
    mOutputQueue.put(out);
  }

  @Override
  public void takeEvent(EventWrapper e) throws IOException, InterruptedException {
    LocalContext context = (LocalContext) getContext();
    List<UserSession> subscribers = new LinkedList<UserSession>(
        context.getFlowData().getSubscribers());

    if (mOutputQueue != null) {
      emitToFlume(e);
    }

    if (subscribers.size() == 0) {
      // Nobody is listening on a console; don't waste time formatting it as a string.
      return;
    }

    StringBuilder sb = new StringBuilder();
    long ts = e.getEvent().getTimestamp();
    sb.append(ts);

    // Extract the Avro record from the event.
    for (TypedField field : mInputFields) {
      sb.append('\t');
      Object fieldVal = e.getField(field);
      if (null != fieldVal) {
        LOG.debug("Printing val of class " + fieldVal.getClass().getName());
      }

      // If we get any GenericRecord types, convert them to our own specific types
      // if we can figure out which to use. This makes toString'ing prettier.
      if (fieldVal instanceof GenericRecord) {
        GenericRecord record = (GenericRecord) fieldVal;
        if (record.getSchema().equals(TimestampBase.SCHEMA$)) {
          fieldVal = new Timestamp((Long) record.get("milliseconds"),
              (Long) record.get("nanos"));
        }
      }

      if (null == fieldVal) {
        sb.append("null");
      } else if (fieldVal instanceof ByteBuffer) {
        sb.append("B[");
        String toStr = (String) BIN2STR_FN.eval(null, fieldVal);
        sb.append(toStr);
        sb.append("]");
      } else {
        sb.append(fieldVal);
      }
    }

    // Notify all subscribers of our output.
    String output = sb.toString();
    for (UserSession session : subscribers) {
      session.sendInfo(output);
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Output(");
    StringUtils.formatList(sb, mInputFields);
    sb.append(")");
    return sb.toString();
  }
}
