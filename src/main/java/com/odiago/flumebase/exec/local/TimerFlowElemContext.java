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

package com.odiago.flumebase.exec.local;

import java.io.IOException;

import java.util.LinkedList;
import java.util.List;

import com.odiago.flumebase.exec.EventWrapper;
import com.odiago.flumebase.exec.FlowElement;

import com.odiago.flumebase.util.concurrent.ArrayBoundedSelectableQueue;
import com.odiago.flumebase.util.concurrent.SelectableQueue;

/**
 * Context for a FlowElement which has a single downstream FE on the
 * same physical host, in the same thread, but which also uses a separate
 * internal thread which drives timer interrupts into a second 'timer' downstream FE.
 * 
 * <p>This FlowElementContext implementation is unusual in that it differentiates
 * between the kinds of downstream flow elements it has. The 'main' downstream flow
 * element processes normal events, and is direct-coupled to our output. The
 * 'timer' downstream flow element will only process events generated by our timer
 * thread, which may contain arbitrary data. The timer element is not actually
 * recognized by our emit() method; we provide a separate method to retrieve the
 * timer queue for manual use.</p>
 */
public class TimerFlowElemContext extends LocalContext {

  /** The main downstream element where we sent events. */
  private FlowElement mDownstream;

  /** The separate timer element we drive via a queue. */
  private FlowElement mTimerElement;

  /** Event queue driving the timer element. null if mTimerElement is null. */
  private SelectableQueue<Object> mTimerQueue;

  public TimerFlowElemContext(FlowElement downstream) {
    mDownstream = downstream;
  }

  /**
   * Create the downstream queue to communicate with our downstream FlowElement.
   */
  @Override
  public void createDownstreamQueues() {
    if (null != mTimerElement) {
      mTimerQueue = new ArrayBoundedSelectableQueue<Object>(LocalEnvironment.MAX_QUEUE_LEN);
    }
  }

  @Override
  public List<SelectableQueue<Object>> getDownstreamQueues() {
    List<SelectableQueue<Object>> out = new LinkedList<SelectableQueue<Object>>();
    out.add(null);
    if (null != mTimerQueue) {
      out.add(mTimerQueue);
    }
    return out;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void emit(EventWrapper e) throws IOException, InterruptedException {
    // NOTE: we emit to the main downstream element. We don't emit to the timer element.
    mDownstream.takeEvent(e);
  }

  public SelectableQueue<Object> getTimerQueue() {
    return mTimerQueue;
  }

  public FlowElement getTimerElement() {
    return mTimerElement;
  }

  public void setTimerElement(FlowElement elem) {
    mTimerElement = elem;
  }

  /**
   * Return the downstream FlowElement. Used by the LocalEnvironment.
   */
  @Override
  List<FlowElement> getDownstream() {
    List<FlowElement> out = new LinkedList<FlowElement>();
    out.add(mDownstream);
    if (null != mTimerElement) {
      out.add(mTimerElement);
    }
    return out;
  }
}
