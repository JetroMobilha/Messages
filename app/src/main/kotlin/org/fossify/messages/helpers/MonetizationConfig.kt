package org.fossify.messages.helpers

object MonetizationConfig {
    /**
     * Set to true during testing phase to disable all contact limits and feature locks.
     * Change to false for production deployment.
     */
    var isTestingMode: Boolean = true

    /**
     * Number of free target contacts allowed in the Vault when not in Pro mode or Testing mode.
     */
    var freeVaultContactLimit: Int = 2

    /**
     * Global Pro status flag.
     */
    var isProUnlocked: Boolean = false

    fun isLimitReached(currentContactCount: Int): Boolean {
        if (isTestingMode || isProUnlocked) {
            return false
        }
        return currentContactCount >= freeVaultContactLimit
    }
}
