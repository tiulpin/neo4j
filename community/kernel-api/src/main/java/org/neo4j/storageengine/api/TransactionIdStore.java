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
package org.neo4j.storageengine.api;

/**
 * Keeps a latest transaction id. There's one counter for {@code committed transaction id} and one for
 * {@code closed transaction id}. The committed transaction id is for writing into a log before making
 * the changes to be made. After that the application of those transactions might be asynchronous and
 * completion of those are marked using {@link #transactionClosed(long, long, long, int, long)}.
 * <p>
 * A transaction ID passes through a {@link TransactionIdStore} like this:
 * <ol>
 * <li>{@link #nextCommittingTransactionId()} is called and an id is returned to a committer.
 * At this point that id isn't visible from any getter.</li>
 * <li>{@link #transactionCommitted(long, int, long)} is called with this id after the fact that the transaction
 * has been committed, i.e. written forcefully to a log. After this call the id may be visible from
 * {@link #getLastCommittedTransactionId()} if all ids before it have also been committed.</li>
 * <li>{@link #transactionClosed(long, long, long, int, long)} is called with this id again, this time after all changes the
 * transaction imposes have been applied to the store.
 * </ol>
 */
public interface TransactionIdStore {
    /**
     * Tx id counting starting from this value (this value means no transaction ever committed).
     *
     * Note that a read only transaction will get txId = 0.
     */
    long BASE_TX_ID = 1;

    int BASE_TX_CHECKSUM = 0xDEAD5EED;

    /**
     * Timestamp value used initially for an empty database.
     */
    long BASE_TX_COMMIT_TIMESTAMP = 0;

    /**
     * CONSTANT FOR UNKNOWN TX CHECKSUM
     */
    int UNKNOWN_TX_CHECKSUM = 1;

    /**
     * Timestamp value used when record in the metadata store is not present and there are no transactions in logs.
     */
    long UNKNOWN_TX_COMMIT_TIMESTAMP = 1;

    TransactionId UNKNOWN_TRANSACTION_ID =
            new TransactionId(BASE_TX_ID - 1, UNKNOWN_TX_CHECKSUM, UNKNOWN_TX_COMMIT_TIMESTAMP);
    /**
     * @return the next transaction id for a committing transaction. The transaction id is incremented
     * with each call. Ids returned from this method will not be visible from {@link #getLastCommittedTransactionId()}
     * until handed to {@link #transactionCommitted(long, int, long)}.
     */
    long nextCommittingTransactionId();

    /**
     * @return the transaction id of last committing transaction.
     */
    long committingTransactionId();

    /**
     * Signals that a transaction with the given transaction id has been committed (i.e. appended to a log).
     * Calls to this method may come in out-of-transaction-id order. The highest transaction id
     * seen given to this method will be visible in {@link #getLastCommittedTransactionId()}.
     * @param transactionId the applied transaction id.
     * @param checksum checksum of the transaction.
     * @param commitTimestamp the timestamp of the transaction commit.
     */
    void transactionCommitted(long transactionId, int checksum, long commitTimestamp);

    /**
     * @return highest seen {@link #transactionCommitted(long, int, long)}  committed transaction id}.
     */
    long getLastCommittedTransactionId();

    /**
     * Returns transaction information about the highest committed transaction, i.e.
     * transaction id as well as checksum.
     *
     * @return {@link TransactionId} describing the last (i.e. highest) committed transaction.
     */
    TransactionId getLastCommittedTransaction();

    /**
     * @return highest seen gap-free {@link #transactionClosed(long, long, long, int, long)}  closed transaction id}.
     */
    long getLastClosedTransactionId();

    /**
     * Returns transaction information about the last committed transaction, i.e.
     * transaction id as well as the log position following the commit entry in the transaction log.
     *
     * @return transaction information about the last closed (highest gap-free) transaction.
     */
    ClosedTransactionMetadata getLastClosedTransaction();

    /**
     * Used by recovery, where last committed/closed transaction ids are set.
     * @param transactionId transaction id that will be the last closed/committed id.
     * @param checksum checksum of the transaction.
     * @param commitTimestamp the timestamp of the transaction commit.
     * @param byteOffset offset in the log file where the committed entry has been written.
     * @param logVersion version of log the committed entry has been written into.
     */
    void setLastCommittedAndClosedTransactionId(
            long transactionId, int checksum, long commitTimestamp, long byteOffset, long logVersion);

    /**
     * Signals that a transaction with the given transaction id has been fully applied. Calls to this method
     * may come in out-of-transaction-id order.
     * @param transactionId the applied transaction id.
     * @param logVersion version of log the committed entry has been written into.
     * @param byteOffset offset in the log file where start writing the next log entry.
     * @param checksum applied transaction checksum
     * @param commitTimestamp applied transaction commit timestamp
     */
    void transactionClosed(long transactionId, long logVersion, long byteOffset, int checksum, long commitTimestamp);

    /**
     * Unconditionally set last closed transaction info. Should be used for cases where last closed transaction info should be
     * set or overwritten.
     *
     * @param transactionId new last closed transaction id.
     * @param logVersion new last closed transaction log version
     * @param byteOffset new last closed transaction offset
     * @param checksum new last closed transaction checksum
     * @param commitTimestamp new last closed transaction commit timestamp
     */
    void resetLastClosedTransaction(
            long transactionId, long logVersion, long byteOffset, int checksum, long commitTimestamp);
}
