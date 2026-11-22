package com.rajeshwari.netsleuth.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diagnostic_records")
data class DiagnosticRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val connectionType: String,
    val networkState: String,
    val isMetered: Boolean,
    val localAddresses: String,
    val dnsServers: String,
    val dnsHost: String,
    val resolvedAddress: String,
    val dnsLatencyMs: Long?,
    val reachable: Boolean,
    val latencyMs: Long?,
    val successfulProbes: Int,
    val totalProbes: Int,
    val summary: String
)
