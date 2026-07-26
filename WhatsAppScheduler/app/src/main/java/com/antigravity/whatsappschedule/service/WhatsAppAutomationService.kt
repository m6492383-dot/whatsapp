package com.antigravity.whatsappschedule.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.antigravity.whatsappschedule.data.model.FailureReason
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

data class SendTaskPayload(
    val messageId: Long,
    val contactName: String,
    val phoneNumber: String,
    val messageText: String,
    val attachmentPath: String?
)

sealed class AutomationResult {
    data class Success(val messageId: Long) : AutomationResult()
    data class Failure(val messageId: Long, val reason: FailureReason, val details: String? = null) : AutomationResult()
}

class WhatsAppAutomationService : AccessibilityService() {

    companion object {
        private const val TAG = "WhatsAppAutomation"
        var instance: WhatsAppAutomationService? = null
            private set

        private val _resultFlow = MutableSharedFlow<AutomationResult>(extraBufferCapacity = 5)
        val resultFlow: SharedFlow<AutomationResult> = _resultFlow

        var currentPayload: SendTaskPayload? = null

        fun isServiceRunning(): Boolean = instance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "Accessibility Service Connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val payload = currentPayload ?: return
        val rootNode = rootInActiveWindow ?: return

        try {
            // Attempt to locate and automate send
            val textSet = setTextMessage(rootNode, payload.messageText)
            if (textSet) {
                val sent = clickSendButton(rootNode)
                if (sent) {
                    Log.d(TAG, "Successfully clicked Send for message ${payload.messageId}")
                    _resultFlow.tryEmit(AutomationResult.Success(payload.messageId))
                    currentPayload = null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during accessibility automation", e)
        }
    }

    private fun setTextMessage(rootNode: AccessibilityNodeInfo, text: String): Boolean {
        // Strategy 1: Find EditText nodes
        val editTextNodes = rootNode.findAccessibilityNodeInfosByViewId("com.whatsapp:id/entry")
        if (editTextNodes.isNotEmpty()) {
            val node = editTextNodes[0]
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        }

        // Strategy 2: Dynamic search for any EditText input field
        val allEditTexts = mutableListOf<AccessibilityNodeInfo>()
        findNodesByClassName(rootNode, "android.widget.EditText", allEditTexts)
        if (allEditTexts.isNotEmpty()) {
            val node = allEditTexts[0]
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        }

        return false
    }

    private fun clickSendButton(rootNode: AccessibilityNodeInfo): Boolean {
        // Strategy 1: Resource ID match
        val sendNodes = rootNode.findAccessibilityNodeInfosByViewId("com.whatsapp:id/send")
        if (sendNodes.isNotEmpty()) {
            return sendNodes[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }

        // Strategy 2: Content Description match (Send, Enviar, etc.)
        val candidateDescriptions = listOf("Send", "Enviar", "Envoyer", "Senden", "Invia")
        for (desc in candidateDescriptions) {
            val nodes = rootNode.findAccessibilityNodeInfosByText(desc)
            for (node in nodes) {
                if (node.isClickable) {
                    return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
                // Try parent if button is inside a wrapper container
                var parent = node.parent
                while (parent != null) {
                    if (parent.isClickable) {
                        return parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    }
                    parent = parent.parent
                }
            }
        }

        // Strategy 3: Find ImageButton/ImageView next to message input
        val buttons = mutableListOf<AccessibilityNodeInfo>()
        findNodesByClassName(rootNode, "android.widget.ImageButton", buttons)
        for (btn in buttons) {
            if (btn.isClickable) {
                val contentDesc = btn.contentDescription?.toString()?.lowercase() ?: ""
                if (contentDesc.contains("send") || contentDesc.contains("enviar")) {
                    return btn.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
            }
        }

        return false
    }

    private fun findNodesByClassName(
        root: AccessibilityNodeInfo,
        className: String,
        results: MutableList<AccessibilityNodeInfo>
    ) {
        if (root.className?.toString() == className) {
            results.add(root)
        }
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            findNodesByClassName(child, className, results)
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility Service Interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}
