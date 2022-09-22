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
package org.neo4j.internal.batchimport.cache.idmapping.string;

import static java.lang.String.format;

import org.neo4j.internal.batchimport.input.DataException;
import org.neo4j.internal.batchimport.input.Group;

public class DuplicateInputIdException extends DataException {
    public DuplicateInputIdException(Object id, Group group) {
        super(message(id, group));
    }

    public static String message(Object id, Group group) {
        return format("Id '%s' is defined more than once in group '%s'", id, group);
    }
}
