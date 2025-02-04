/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.sql.streaming

import java.util.UUID

import org.apache.spark.annotation.Evolving
import org.apache.spark.scheduler.SparkListenerEvent

/**
 * Interface for listening to events related to [[StreamingQuery StreamingQueries]].
 * @note The methods are not thread-safe as they may be called from different threads.
 *
 * @since 2.0.0
 */
@Evolving
abstract class StreamingQueryListener {

  import StreamingQueryListener._

  /**
   * Called when a query is started.
   * @note This is called synchronously with
   *       [[org.apache.spark.sql.streaming.DataStreamWriter `DataStreamWriter.start()`]],
   *       that is, `onQueryStart` will be called on all listeners before
   *       `DataStreamWriter.start()` returns the corresponding [[StreamingQuery]]. Please
   *       don't block this method as it will block your query.
   * @since 2.0.0
   */
  def onQueryStarted(event: QueryStartedEvent): Unit

  /**
   * Called when there is some status update (ingestion rate updated, etc.)
   *
   * @note This method is asynchronous. The status in [[StreamingQuery]] will always be
   *       latest no matter when this method is called. Therefore, the status of [[StreamingQuery]]
   *       may be changed before/when you process the event. E.g., you may find [[StreamingQuery]]
   *       is terminated when you are processing `QueryProgressEvent`.
   * @since 2.0.0
   */
  def onQueryProgress(event: QueryProgressEvent): Unit

  /**
   * Called when the query is idle and waiting for new data to process.
   * @since 3.5.0
   */
  def onQueryIdle(event: QueryIdleEvent): Unit = {}

  /**
   * Called when a query is stopped, with or without error.
   * @since 2.0.0
   */
  def onQueryTerminated(event: QueryTerminatedEvent): Unit
}

/**
 * Py4J allows a pure interface so this proxy is required.
 */
private[spark] trait PythonStreamingQueryListener {
  import StreamingQueryListener._

  def onQueryStarted(event: QueryStartedEvent): Unit

  def onQueryProgress(event: QueryProgressEvent): Unit

  def onQueryIdle(event: QueryIdleEvent): Unit

  def onQueryTerminated(event: QueryTerminatedEvent): Unit
}

private[spark] class PythonStreamingQueryListenerWrapper(
    listener: PythonStreamingQueryListener) extends StreamingQueryListener {
  import StreamingQueryListener._

  def onQueryStarted(event: QueryStartedEvent): Unit = listener.onQueryStarted(event)

  def onQueryProgress(event: QueryProgressEvent): Unit = listener.onQueryProgress(event)

  override def onQueryIdle(event: QueryIdleEvent): Unit = listener.onQueryIdle(event)

  def onQueryTerminated(event: QueryTerminatedEvent): Unit = listener.onQueryTerminated(event)
}

/**
 * Companion object of [[StreamingQueryListener]] that defines the listener events.
 * @since 2.0.0
 */
@Evolving
object StreamingQueryListener {

  /**
   * Base type of [[StreamingQueryListener]] events
   * @since 2.0.0
   */
  @Evolving
  trait Event extends SparkListenerEvent

  /**
   * Event representing the start of a query
   * @param id A unique query id that persists across restarts. See `StreamingQuery.id()`.
   * @param runId A query id that is unique for every start/restart. See `StreamingQuery.runId()`.
   * @param name User-specified name of the query, null if not specified.
   * @param timestamp The timestamp to start a query.
   * @since 2.1.0
   */
  @Evolving
  class QueryStartedEvent private[sql](
      val id: UUID,
      val runId: UUID,
      val name: String,
      val timestamp: String) extends Event

  /**
   * Event representing any progress updates in a query.
   * @param progress The query progress updates.
   * @since 2.1.0
   */
  @Evolving
  class QueryProgressEvent private[sql](val progress: StreamingQueryProgress) extends Event

  /**
   * Event representing that query is idle and waiting for new data to process.
   *
   * @param id    A unique query id that persists across restarts. See `StreamingQuery.id()`.
   * @param runId A query id that is unique for every start/restart. See `StreamingQuery.runId()`.
   * @param timestamp The timestamp when the latest no-batch trigger happened.
   * @since 3.5.0
   */
  @Evolving
  class QueryIdleEvent private[sql](
      val id: UUID,
      val runId: UUID,
      val timestamp: String) extends Event

  /**
   * Event representing that termination of a query.
   *
   * @param id A unique query id that persists across restarts. See `StreamingQuery.id()`.
   * @param runId A query id that is unique for every start/restart. See `StreamingQuery.runId()`.
   * @param exception The exception message of the query if the query was terminated
   *                  with an exception. Otherwise, it will be `None`.
   * @since 2.1.0
   */
  @Evolving
  class QueryTerminatedEvent private[sql](
      val id: UUID,
      val runId: UUID,
      val exception: Option[String]) extends Event
}
