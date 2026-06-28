package com.example.database

import kotlinx.coroutines.flow.Flow

class CallGuardianRepository(private val dao: CallGuardianDao) {

    // Call history records API
    val allCallHistory: Flow<List<CallHistoryRecord>> = dao.getAllCallHistory()

    suspend fun insertCallRecord(record: CallHistoryRecord): Long {
        return dao.insertCallRecord(record)
    }

    suspend fun deleteCallRecordById(id: Int) {
        dao.deleteCallRecordById(id)
    }

    suspend fun clearHistory() {
        dao.clearAllCallHistory()
    }

    // Blocked spam contacts API
    val allBlockedContacts: Flow<List<BlockedContact>> = dao.getAllBlockedContacts()

    suspend fun blockContact(contact: BlockedContact) {
        dao.blockContact(contact)
    }

    suspend fun unblockContact(phoneNumber: String) {
        dao.unblockContact(phoneNumber)
    }

    suspend fun isBlocked(phoneNumber: String): Boolean {
        return dao.isBlocked(phoneNumber) > 0
    }

    // VIP contacts API
    val allVIPContacts: Flow<List<VIPContact>> = dao.getAllVIPContacts()

    suspend fun insertVIPContact(contact: VIPContact) {
        dao.insertVIPContact(contact)
    }

    suspend fun deleteVIPContact(phoneNumber: String) {
        dao.deleteVIPContact(phoneNumber)
    }

    suspend fun deleteVIPContactById(id: Int) {
        dao.deleteVIPContactById(id)
    }
}
