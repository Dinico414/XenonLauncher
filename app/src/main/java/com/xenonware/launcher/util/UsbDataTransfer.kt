package com.xenonware.launcher.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

private const val USB_STATE_ACTION = "android.hardware.usb.action.USB_STATE"

@Composable
fun rememberUsbDataTransfer(): Boolean {
    val context = LocalContext.current
    var dataTransfer by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager

        // The two independent ways a data link can exist; either one is enough
        var hostingStorage = false
        var pcDataLink = false

        fun publish() {
            dataTransfer = hostingStorage || pcDataLink
        }

        fun readAttachedStorage() {
            hostingStorage = usbManager?.deviceList?.values?.any { device ->
                // Composite devices report class 0 and declare the real class per interface
                (0 until device.interfaceCount).any { i ->
                    device.getInterface(i).interfaceClass == UsbConstants.USB_CLASS_MASS_STORAGE
                }
            } == true
        }

        fun readPcLink(intent: Intent?) {
            if (intent == null) return
            val connected = intent.getBooleanExtra("connected", false)
            val configured = intent.getBooleanExtra("configured", false)
            // Charge-only enumerates with no data function set
            pcDataLink = connected && configured &&
                    (intent.getBooleanExtra("mtp", false) ||
                            intent.getBooleanExtra("ptp", false))
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == USB_STATE_ACTION) readPcLink(intent) else readAttachedStorage()
                publish()
            }
        }

        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(USB_STATE_ACTION)
        }
        // USB_STATE is sticky, so registering hands back the state we are already in
        val sticky = ContextCompat.registerReceiver(
            context, receiver, filter, ContextCompat.RECEIVER_EXPORTED
        )
        readPcLink(sticky)
        readAttachedStorage()
        publish()

        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    return dataTransfer
}