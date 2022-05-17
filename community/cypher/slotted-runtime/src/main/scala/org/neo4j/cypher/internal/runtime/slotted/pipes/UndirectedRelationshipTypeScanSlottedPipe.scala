/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.cypher.internal.runtime.slotted.pipes

import org.neo4j.cypher.internal.logical.plans.IndexOrder
import org.neo4j.cypher.internal.runtime.ClosingIterator
import org.neo4j.cypher.internal.runtime.CypherRow
import org.neo4j.cypher.internal.runtime.interpreted.pipes.CypherRowFactory
import org.neo4j.cypher.internal.runtime.interpreted.pipes.LazyType
import org.neo4j.cypher.internal.runtime.interpreted.pipes.Pipe
import org.neo4j.cypher.internal.runtime.interpreted.pipes.QueryState
import org.neo4j.cypher.internal.util.attribution.Id

case class UndirectedRelationshipTypeScanSlottedPipe(
  relOffset: Int,
  fromOffset: Int,
  typ: LazyType,
  toOffset: Int,
  indexOrder: IndexOrder
)(val id: Id = Id.INVALID_ID) extends Pipe {

  protected def internalCreateResults(state: QueryState): ClosingIterator[CypherRow] = {
    val typeId = typ.getId(state.query)
    if (typeId == LazyType.UNKNOWN) {
      ClosingIterator.empty
    } else {
      new UndirectedIterator(relOffset, typeId, fromOffset, toOffset, rowFactory, state, indexOrder).filter(row =>
        row != null
      )
    }
  }
}

private class UndirectedIterator(
  relOffset: Int,
  relToken: Int,
  fromOffset: Int,
  toOffset: Int,
  rowFactory: CypherRowFactory,
  state: QueryState,
  indexOrder: IndexOrder
) extends ClosingIterator[CypherRow] {
  private var emitSibling = false
  private var lastRelationship: Long = -1L
  private var lastStart: Long = -1L
  private var lastEnd: Long = -1L

  private val query = state.query
  private val relIterator = query.getRelationshipsByType(state.relTypeTokenReadSession.get, relToken, indexOrder)

  def next(): CypherRow = {
    val context = state.newRowWithArgument(rowFactory)
    if (emitSibling) {
      emitSibling = false
      context.setLongAt(fromOffset, lastEnd)
      context.setLongAt(toOffset, lastStart)
    } else {
      emitSibling = true
      lastRelationship = relIterator.next()
      lastStart = relIterator.startNodeId()
      lastEnd = relIterator.endNodeId()
      context.setLongAt(fromOffset, lastStart)
      context.setLongAt(toOffset, lastEnd)
    }
    context.setLongAt(relOffset, lastRelationship)
    context
  }

  override protected[this] def closeMore(): Unit = {
    relIterator.close()
  }
  override protected[this] def innerHasNext: Boolean = emitSibling || relIterator.hasNext
}
