package org.fossify.messages.activities

import android.os.Bundle
import android.view.WindowManager
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.updateTextColors
import org.fossify.commons.extensions.viewBinding
import org.fossify.messages.databinding.ActivityFakeVaultBinding

class FakeVaultActivity : SimpleActivity() {

    private val binding by viewBinding(ActivityFakeVaultBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Anti-Snoop Protection
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        setContentView(binding.root)

        setupTheme()
    }

    private fun setupTheme() {
        binding.root.setBackgroundColor(getProperBackgroundColor())
        updateTextColors(binding.root)
    }

    override fun onResume() {
        super.onResume()
        setupTheme()
    }

    override fun onPause() {
        super.onPause()
        finish()
    }
}
