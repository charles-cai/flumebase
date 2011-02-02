// (c) Copyright 2010 Odiago, Inc.

package com.odiago.rtengine.exec;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

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

import com.odiago.rtengine.exec.local.LocalContext;

import com.odiago.rtengine.flume.EmbeddedFlumeConfig;
import com.odiago.rtengine.flume.SourceContext;
import com.odiago.rtengine.flume.SourceContextBindings;

import com.odiago.rtengine.parser.SQLStatement;
import com.odiago.rtengine.parser.TypedField;

import com.odiago.rtengine.server.UserSession;

import com.odiago.rtengine.util.StringUtils;

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

  /** Input fields being delivered to this node. */
  private List<TypedField> mInputFields;

  /** Interface with Flume */
  private EmbeddedFlumeConfig mFlumeConfig;

  /** Name of Flume logical node that broadcasts the results of this query. */
  private String mFlumeNodeName;

  /** Queue of events that are delivered to Flume by this OutputElement. */
  private BlockingQueue<Event> mOutputQueue;

  /** De-dup'd version of mInputFields, for version emitted to Flume. */
  private List<TypedField> mFlumeInputFields;

  /** Output fields emitted to Flume. */
  private List<TypedField> mOutputFields;

  /** Output schema emitted to Flume. */
  private Schema mOutputSchema;

  // Objects used for Avro serialization.
  private Encoder mEncoder;
  private GenericDatumWriter<GenericRecord> mDatumWriter;
  private ByteArrayOutputStream mOutputBytes;

  public OutputElement(FlowElementContext context, Schema inputSchema,
      List<TypedField> fields, EmbeddedFlumeConfig flumeConfig, String flumeNodeName,
      Schema outputSchema, List<TypedField> outputFields) {
    super(context);

    mInputFields = fields;
    mFlumeConfig = flumeConfig;
    mFlumeNodeName = flumeNodeName;
    mOutputQueue = null;
    mOutputSchema = outputSchema;
    mOutputFields = outputFields;

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
      // Open a Flume logical node to host the results of this query.
      // TODO(aaron): What happens if this flume node already exists? This should error...
      mOutputQueue = new ArrayBlockingQueue<Event>(MAX_QUEUE_LEN);
      SourceContext srcContext = new SourceContext(mFlumeNodeName, mOutputQueue);
      SourceContextBindings.get().bindContext(mFlumeNodeName, srcContext);
      try {
        mFlumeConfig.spawnLogicalNode(mFlumeNodeName,
            "rtsqlsource(\"" + mFlumeNodeName + "\")",
            "null");
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
    open();
  }

  /**
   * Stops the current Flume node broadcasting our output.
   */
  private void stopFlumeNode() throws IOException {
    if (mFlumeNodeName != null) {
      try {
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
    List<UserSession> subscribers = context.getFlowData().getSubscribers();

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
      if (null == fieldVal) {
        sb.append("null");
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