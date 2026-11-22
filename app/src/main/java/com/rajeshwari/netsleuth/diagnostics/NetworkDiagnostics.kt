package com.rajeshwari.netsleuth.diagnostics

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.LinkProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL

class NetworkDiagnostics(context: Context) {
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)

    suspend fun runChecks(): DiagnosticResult = withContext(Dispatchers.IO) {
        val network = connectivity.activeNetwork
        val caps = network?.let { connectivity.getNetworkCapabilities(it) }
        val link = network?.let { connectivity.getLinkProperties(it) }
        val connected = network != null && caps != null
        val type = when {
            caps == null -> "Offline"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile data"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            else -> "Other"
        }
        val validated = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
        val metered = connectivity.isActiveNetworkMetered
        val local = link?.linkAddresses?.map { it.address.hostAddress ?: "" }
            ?.filter { it.isNotBlank() }?.joinToString() ?: "Unavailable"
        val dnsServers = link?.dnsServers?.mapNotNull { it.hostAddress }?.joinToString() ?: "Unavailable"

        val dnsHost = "example.com"
        var resolved = "Not resolved"
        var dnsMs: Long? = null
        try {
            val start = System.nanoTime()
            resolved = InetAddress.getAllByName(dnsHost).joinToString { it.hostAddress ?: "?" }
            dnsMs = elapsedMs(start)
        } catch (_: Exception) { }

        val targets = listOf(
            "Google connectivity" to "https://www.google.com/generate_204",
            "Cloudflare" to "https://www.cloudflare.com/cdn-cgi/trace",
            "Example HTTPS" to "https://example.com/"
        )
        val probeResults = coroutineScope {
            targets.map { (name, url) -> async { checkEndpoint(name, url) } }.map { it.await() }
        }
        val primary = probeResults.first()
        val successful = probeResults.count { it.success }
        val summary = when {
            !connected -> "No active network is available. Connect to Wi-Fi or mobile data and try again."
            successful == 0 && dnsMs == null -> "DNS resolution and all endpoint checks failed. Check your connection, VPN or captive portal."
            successful == 0 -> "DNS resolved, but all test endpoints were unreachable. A firewall, VPN or captive portal may be interfering."
            successful < probeResults.size -> "$successful of ${probeResults.size} endpoints responded. Some destinations may be blocked or unavailable."
            else -> "Network checks completed successfully. DNS resolved and all test endpoints responded."
        }
        DiagnosticResult(
            timestamp = System.currentTimeMillis(), connectionType = type,
            networkState = if (!connected) "Disconnected" else if (validated) "Internet validated" else "Connected; internet not validated",
            isMetered = metered, isValidated = validated, localAddresses = local,
            dnsServers = dnsServers, dnsHost = dnsHost, resolvedAddress = resolved,
            dnsLatencyMs = dnsMs, endpoint = primary.target, reachable = primary.success,
            latencyMs = primary.latencyMs, probes = probeResults, summary = summary
        )
    }

    private fun checkEndpoint(name: String, address: String): ProbeResult {
        var connection: HttpURLConnection? = null
        return try {
            val start = System.nanoTime()
            connection = (URL(address).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4500
                readTimeout = 4500
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "NetSleuth-Android/1.0")
            }
            val code = connection.responseCode
            val duration = elapsedMs(start)
            val ok = code in 200..399
            ProbeResult(name, address, ok, duration, "HTTP $code")
        } catch (e: Exception) {
            ProbeResult(name, address, false, null, e.localizedMessage ?: e.javaClass.simpleName)
        } finally {
            connection?.disconnect()
        }
    }

    private fun elapsedMs(startNanos: Long): Long = (System.nanoTime() - startNanos) / 1_000_000
}
