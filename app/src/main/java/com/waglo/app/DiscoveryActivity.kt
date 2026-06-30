
package com.waglo.app

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.waglo.app.databinding.ActivityDiscoveryBinding
import com.waglo.app.model.GroupLink
import com.waglo.app.repository.GroupLinkRepository
import kotlinx.coroutines.launch

/**
 * Lets users paste text / HTML that may contain WhatsApp invite links.
 * The app extracts, normalises, validates, classifies, and stores them.
 */
class DiscoveryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDiscoveryBinding
    private lateinit var repo: GroupLinkRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiscoveryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        repo = GroupLinkRepository(this)

        binding.btnDiscover.setOnClickListener { startDiscovery() }
        binding.btnClear.setOnClickListener {
            binding.etInput.text?.clear()
            binding.tvResult.text = ""
        }
    }

    private fun startDiscovery() {
        val input = binding.etInput.text?.toString()?.trim() ?: ""
        if (input.isBlank()) {
            Toast.makeText(this, getString(R.string.enter_text_hint), Toast.LENGTH_SHORT).show()
            return
        }
        val isHtml = binding.switchHtmlMode.isChecked
        binding.progressBar.visibility = View.VISIBLE
        binding.btnDiscover.isEnabled = false

        lifecycleScope.launch {
            try {
                val count = repo.discoverFromText(
                    text   = input,
                    source = GroupLink.Source.DISCOVERY,
                    isHtml = isHtml
                )
                val msg = if (count > 0)
                    getString(R.string.discovery_success, count)
                else
                    getString(R.string.discovery_no_links)
                binding.tvResult.text = msg
                Toast.makeText(this@DiscoveryActivity, msg, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                binding.tvResult.text = getString(R.string.error_generic)
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnDiscover.isEnabled = true
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
