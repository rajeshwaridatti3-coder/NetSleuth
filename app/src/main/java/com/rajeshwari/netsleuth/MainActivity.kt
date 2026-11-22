package com.rajeshwari.netsleuth

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.rajeshwari.netsleuth.data.DiagnosticRecord
import com.rajeshwari.netsleuth.data.NetSleuthDatabase
import com.rajeshwari.netsleuth.diagnostics.DiagnosticResult
import com.rajeshwari.netsleuth.diagnostics.NetworkDiagnostics
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

class MainActivity : AppCompatActivity() {
    private val bg = Color.rgb(10, 16, 30)
    private val panel = Color.rgb(24, 34, 54)
    private val accent = Color.rgb(55, 211, 174)
    private val muted = Color.rgb(171, 185, 205)
    private lateinit var statusView: TextView
    private lateinit var networkView: TextView
    private lateinit var resultsView: TextView
    private lateinit var historyView: TextView
    private lateinit var runButton: Button
    private lateinit var shareButton: Button
    private var latestReport = "No report available. Run diagnostics first."
    private val dao by lazy { NetSleuthDatabase.get(this).diagnosticDao() }
    private val diagnostics by lazy { NetworkDiagnostics(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
        lifecycleScope.launch {
            dao.observeAll().collect { records -> renderHistory(records) }
        }
    }

    private fun buildScreen() {
        val scroll = ScrollView(this).apply { setBackgroundColor(bg) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(28), dp(20), dp(28))
        }
        scroll.addView(root)
        setContentView(scroll)

        root.addView(label("NETSLEUTH  /  NETWORK INTELLIGENCE", 12, accent, true))
        root.addView(label("Mobile Network\nDiagnostics", 31, Color.WHITE, true))
        root.addView(label("Inspect connectivity, resolve DNS, measure endpoint response and keep a local diagnostic history.", 15, muted))

        statusView = label("●  READY TO TEST", 15, accent, true)
        card(root, statusView)

        root.addView(section("LIVE NETWORK SNAPSHOT"))
        networkView = label("Network details will appear after your first scan.", 14, Color.WHITE)
        card(root, networkView)

        runButton = actionButton("RUN FULL DIAGNOSTICS") { runDiagnostics() }
        root.addView(runButton, params())

        root.addView(section("DIAGNOSTIC RESULTS"))
        resultsView = label("No scan has been run yet. The scan checks DNS resolution and three HTTPS endpoints.", 14, Color.WHITE)
        card(root, resultsView)

        shareButton = actionButton("SHARE / EXPORT LATEST REPORT") { shareReport() }
        shareButton.isEnabled = false
        root.addView(shareButton, params())

        root.addView(section("SAVED SCAN HISTORY"))
        historyView = label("Loading saved scans…", 14, Color.WHITE)
        card(root, historyView)
        val clear = actionButton("CLEAR HISTORY") {
            lifecycleScope.launch { dao.clearAll() }
        }
        root.addView(clear, params())
        root.addView(label("Privacy: scan history stays in the app's local Room database. NetSleuth does not upload your results.", 12, muted))
    }

    private fun runDiagnostics() {
        runButton.isEnabled = false
        shareButton.isEnabled = false
        statusView.text = "●  RUNNING NETWORK CHECKS…"
        resultsView.text = "Checking active network, DNS resolution and HTTPS endpoint reachability. This can take several seconds."
        lifecycleScope.launch {
            try {
                val result = diagnostics.runChecks()
                latestReport = createReport(result)
                networkView.text = buildString {
                    appendLine("Connection: ${result.connectionType}")
                    appendLine("Internet state: ${result.networkState}")
                    appendLine("Metered connection: ${if (result.isMetered) "Yes" else "No"}")
                    appendLine("Local address: ${result.localAddresses}")
                    append("DNS servers: ${result.dnsServers}")
                }
                resultsView.text = buildString {
                    appendLine("DNS LOOKUP")
                    appendLine("Host: ${result.dnsHost}")
                    appendLine("Resolved: ${result.resolvedAddress}")
                    appendLine("Lookup time: ${result.dnsLatencyMs?.let { "$it ms" } ?: "Failed"}")
                    appendLine()
                    appendLine("ENDPOINT PROBES")
                    result.probes.forEach { probe ->
                        appendLine("${if (probe.success) "✓" else "✕"} ${probe.name}: ${if (probe.success) "${probe.latencyMs} ms" else probe.detail}")
                        appendLine(probe.target)
                    }
                    appendLine()
                    append("SUMMARY\n${result.summary}")
                }
                statusView.text = if (result.probes.any { it.success }) "●  SCAN COMPLETED" else "●  NETWORK ISSUE DETECTED"
                shareButton.isEnabled = true
                dao.insert(
                    DiagnosticRecord(
                        timestamp = result.timestamp,
                        connectionType = result.connectionType,
                        networkState = result.networkState,
                        isMetered = result.isMetered,
                        localAddresses = result.localAddresses,
                        dnsServers = result.dnsServers,
                        dnsHost = result.dnsHost,
                        resolvedAddress = result.resolvedAddress,
                        dnsLatencyMs = result.dnsLatencyMs,
                        reachable = result.reachable,
                        latencyMs = result.latencyMs,
                        successfulProbes = result.probes.count { it.success },
                        totalProbes = result.probes.size,
                        summary = result.summary
                    )
                )
            } catch (e: Exception) {
                statusView.text = "●  SCAN FAILED"
                resultsView.text = e.localizedMessage ?: "Unexpected diagnostic error."
            } finally {
                runButton.isEnabled = true
            }
        }
    }

    private fun createReport(result: DiagnosticResult): String = buildString {
        appendLine("NETSLEUTH — MOBILE NETWORK DIAGNOSTIC REPORT")
        appendLine("Time: ${DateFormat.getDateTimeInstance().format(Date(result.timestamp))}")
        appendLine("Connection: ${result.connectionType}")
        appendLine("Internet state: ${result.networkState}")
        appendLine("Metered: ${result.isMetered}")
        appendLine("Local address: ${result.localAddresses}")
        appendLine("DNS servers: ${result.dnsServers}")
        appendLine("DNS host: ${result.dnsHost}")
        appendLine("Resolved addresses: ${result.resolvedAddress}")
        appendLine("DNS latency: ${result.dnsLatencyMs?.let { "$it ms" } ?: "Failed"}")
        appendLine("\nENDPOINT TESTS")
        result.probes.forEach {
            appendLine("${it.name}: ${if (it.success) "Reachable in ${it.latencyMs} ms" else "Failed — ${it.detail}"}")
            appendLine(it.target)
        }
        appendLine("\nSummary: ${result.summary}")
        appendLine("\nLatency is an observed request duration, not a guaranteed network quality score.")
    }

    private fun shareReport() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "NetSleuth Diagnostic Report")
            putExtra(Intent.EXTRA_TEXT, latestReport)
        }
        startActivity(Intent.createChooser(intent, "Export diagnostic report"))
    }

    private fun renderHistory(records: List<DiagnosticRecord>) {
        historyView.text = if (records.isEmpty()) "No saved scans yet. Run diagnostics to build your history." else
            records.take(20).joinToString("\n\n──────────────\n\n") { record ->
                "${DateFormat.getDateTimeInstance().format(Date(record.timestamp))}\n" +
                    "${record.connectionType} • ${record.successfulProbes}/${record.totalProbes} endpoints reachable\n" +
                    "DNS: ${record.dnsLatencyMs?.let { "$it ms" } ?: "failed"} • ${record.networkState}\n" +
                    record.summary
            }
    }

    private fun section(value: String) = label(value, 12, accent, true).apply { setPadding(0, dp(20), 0, dp(6)) }
    private fun label(value: String, size: Int, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = value
        textSize = size.toFloat()
        setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
        setLineSpacing(dp(3).toFloat(), 1f)
        setPadding(0, dp(6), 0, dp(6))
    }

    private fun card(parent: LinearLayout, child: TextView) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            setBackgroundColor(panel)
            addView(child)
        }
        parent.addView(container, params())
    }

    private fun actionButton(title: String, action: () -> Unit) = Button(this).apply {
        text = title
        isAllCaps = false
        setTextColor(Color.WHITE)
        setBackgroundColor(Color.rgb(20, 126, 111))
        setOnClickListener { action() }
    }

    private fun params() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { setMargins(0, dp(8), 0, dp(8)) }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
