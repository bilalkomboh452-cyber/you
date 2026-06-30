
package com.waglo.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.waglo.app.databinding.ActivityMainBinding
import com.waglo.app.service.WAAccessibilityService
import com.waglo.app.viewmodel.MainViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val vm: MainViewModel by viewModels()
    private var receiverRegistered = false

    private val linkReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val count = intent?.getIntExtra(WAAccessibilityService.EXTRA_NEW_COUNT, 0) ?: 0
            vm.loadStats()
            if (count > 0) {
                Snackbar.make(
                    binding.root,
                    getString(R.string.new_groups_captured, count),
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        setupButtons()
        observeViewModel()
        vm.loadStats()
        checkAccessibilityService()
    }

    private fun setupButtons() {
        binding.btnViewGroups.setOnClickListener {
            startActivity(Intent(this, GroupListActivity::class.java))
        }
        binding.btnDiscover.setOnClickListener {
            startActivity(Intent(this, DiscoveryActivity::class.java))
        }
        binding.btnEnableService.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun observeViewModel() {
        vm.loading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }
        vm.stats.observe(this) { stats ->
            binding.tvTotalCount.text     = stats.total.toString()
            binding.tvActiveCount.text    = stats.active.toString()
            binding.tvFavoriteCount.text  = stats.favorites.toString()
            binding.tvDuplicateCount.text = stats.duplicates.toString()

            // Category breakdown
            val catSummary = stats.byCategoryMap.entries
                .filter { it.value > 0 }
                .sortedByDescending { it.value }
                .take(5)
                .joinToString("  |  ") { "${it.key}: ${it.value}" }
            binding.tvCategoryBreakdown.text = catSummary.ifBlank { getString(R.string.no_data) }

            // Service status
            val isEnabled = isAccessibilityServiceEnabled()
            binding.tvServiceStatus.text = getString(
                if (isEnabled) R.string.service_active else R.string.service_inactive
            )
            binding.tvServiceStatus.setTextColor(
                getColor(if (isEnabled) R.color.green_whatsapp else R.color.red_error)
            )
            binding.btnEnableService.text = getString(
                if (isEnabled) R.string.service_settings else R.string.enable_service
            )
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedId = "$packageName/${WAAccessibilityService::class.java.name}"
        val enabledServices = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)
        return splitter.asSequence().any { it.equals(expectedId, ignoreCase = true) }
    }

    private fun checkAccessibilityService() {
        if (!isAccessibilityServiceEnabled()) {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.accessibility_required_title))
                .setMessage(getString(R.string.accessibility_required_msg))
                .setPositiveButton(getString(R.string.enable_now)) { _, _ ->
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
                .setNegativeButton(getString(R.string.later), null)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        vm.loadStats()
        if (!receiverRegistered) {
            ContextCompat.registerReceiver(
                this, linkReceiver,
                IntentFilter(WAAccessibilityService.ACTION_LINKS_UPDATED),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            receiverRegistered = true
        }
    }

    override fun onPause() {
        super.onPause()
        if (receiverRegistered) {
            unregisterReceiver(linkReceiver)
            receiverRegistered = false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
