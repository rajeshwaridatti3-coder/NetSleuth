package com.rajeshwari.netsleuth.diagnostics

data class ProbeResult(
    val name: String,
    val target: String,
    val success: Boolean,
    val latencyMs: Long?,
    val detail: String
)

data class DiagnosticResult(
    val timestamp: Long,
    val connectionType: String,
    val networkState: String,
    val isMetered: Boolean,
    val isValidated: Boolean,
    val localAddresses: String,
    val dnsServers: String,
    val dnsHost: String,
    val resolvedAddress: String,
    val dnsLatencyMs: Long?,
    val endpoint: String,
    val reachable: Boolean,
    val latencyMs: Long?,
    val probes: List<ProbeResult>,
    val summary: String
)
