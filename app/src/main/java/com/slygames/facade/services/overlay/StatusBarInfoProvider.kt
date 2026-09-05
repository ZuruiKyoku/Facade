package com.slygames.facade.services.overlay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.BatteryManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/** Live, non-clock status bar facts: battery charge and Wi-Fi connectivity. Cellular signal
 * strength is deliberately not included - reading it needs READ_PHONE_STATE, a dangerous runtime
 * permission Facade would otherwise have no reason to ask for. */
data class StatusBarInfo(
    val batteryPercent: Int = 100,
    val isCharging: Boolean = false,
    val wifiConnected: Boolean = false
)

/** Sources [StatusBarInfo] from two OS-level, no-runtime-permission-needed signals: the sticky
 * `ACTION_BATTERY_CHANGED` broadcast and a [ConnectivityManager] Wi-Fi transport callback. Both
 * are push-based (no polling), so the status bar overlay updates the instant either changes. */
class StatusBarInfoProvider(private val context: Context) {

    fun observe(): Flow<StatusBarInfo> = combine(batteryFlow(), wifiConnectedFlow()) { battery, wifi ->
        battery.copy(wifiConnected = wifi)
    }.distinctUntilChanged()

    private fun batteryFlow(): Flow<StatusBarInfo> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val percent = if (level >= 0 && scale > 0) (level * 100 / scale) else 100
                val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
                trySend(StatusBarInfo(batteryPercent = percent, isCharging = charging))
            }
        }
        // ACTION_BATTERY_CHANGED is sticky: registering returns the last broadcast immediately,
        // so there's no need for a separate initial query.
        val sticky = context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        sticky?.let { receiver.onReceive(context, it) }
        awaitClose { context.unregisterReceiver(receiver) }
    }

    private fun wifiConnectedFlow(): Flow<Boolean> = callbackFlow {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
            }

            override fun onLost(network: Network) {
                trySend(false)
            }
        }
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        trySend(false)
        connectivityManager.registerNetworkCallback(request, callback)
        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }
}
