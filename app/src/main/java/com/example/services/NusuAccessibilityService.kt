package com.example.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object AccessibilityEngine {
    private val _serviceInstance = MutableStateFlow<NusuAccessibilityService?>(null)
    val serviceInstance = _serviceInstance.asStateFlow()

    fun setInstance(instance: NusuAccessibilityService?) {
        _serviceInstance.value = instance
    }
}

class NusuAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        AccessibilityEngine.setInstance(this)
        Log.d("NusuAccessibility", "Accessibility Service Connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Here we could track window changes and notify voice service
    }

    override fun onInterrupt() {
        Log.d("NusuAccessibility", "Accessibility Service Interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        AccessibilityEngine.setInstance(null)
    }

    // Helper functions for UI automation

    fun clickNodeText(textToFind: String, ignoreCase: Boolean = true): Boolean {
        val root = rootInActiveWindow ?: return false
        val foundNodes = root.findAccessibilityNodeInfosByText(textToFind)
        for (node in foundNodes) {
            val text = node.text?.toString() ?: node.contentDescription?.toString() ?: ""
            if (text.equals(textToFind, ignoreCase) || text.contains(textToFind, ignoreCase)) {
                if (node.isClickable) {
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    return true
                } else if (node.parent?.isClickable == true) {
                    node.parent?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    return true
                }
            }
        }
        return false
    }

    fun typeTextIntoNode(textToType: String, nodeTextHint: String? = null): Boolean {
        val root = rootInActiveWindow ?: return false

        // Attempt to find a node by hint or just find the first editable text node
        fun findEditText(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            if (node.isEditable) return node
            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child != null) {
                    val res = findEditText(child)
                    if (res != null) return res
                }
            }
            return null
        }

        val targetNode = if (nodeTextHint != null) {
            val nodes = root.findAccessibilityNodeInfosByText(nodeTextHint)
            nodes.firstOrNull { it.isEditable } ?: findEditText(root)
        } else {
            findEditText(root)
        }

        if (targetNode != null) {
            val arguments = Bundle()
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToType)
            targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            return true
        }
        return false
    }

    fun performGlobalBack() {
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    fun scrollForward() {
        val root = rootInActiveWindow ?: return
        fun findScrollable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            if (node.isScrollable) return node
            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child != null) {
                    val res = findScrollable(child)
                    if (res != null) return res
                }
            }
            return null
        }
        findScrollable(root)?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
    }
}
