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
package org.neo4j.logging.event;

public interface EventPublisher {

    EventPublisher NO_OP = new EventPublisher() {
        @Override
        public void publish(Type type, String message, Parameters parameters) {}

        @Override
        public void publish(Type type, String message) {}
    };

    void publish(Type type, String message, Parameters parameters);

    void publish(Type type, String message);

    /**
     * @return A stateful {@link CappedEventPublisher} intended to be used where event publishing may be spammy. For
     * example in loops.
     */
    default EventPublisher capped(EventsFilter filter) {
        return CappedEventPublisher.capped(this, filter);
    }
}
