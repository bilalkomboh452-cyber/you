package com.waglo.app

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.waglo.app.databinding.ActivityMainBinding
import com.waglo.app.viewmodel.MainViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        setupClickListeners()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        vm.refresh()
    }

    private fun setupClickListeners() {
        binding.btnDiscover.setOnClickListener {
            startActivity(Intent(this, DiscoveryActivity::class.java))
        }
        binding.cardAccessibility.setOnClickListener {
            startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    private fun observeViewModel() {
        vm.connectionStatus.observe(this) { isGranted ->
            binding.cardAccessibility.isVisible = !isGranted
            if (isGranted) {
                binding.tvStatus.text  = getString(R.string.status_active)
                binding.tvStatus.setTextColor(getColor(R.color.success))
            } else {
                binding.tvStatus.text  = getString(R.string.status_inactive)
                binding.tvStatus.setTextColor(getColor(R.color.error))
            }
        }

        vm.dashboardStats.observe(this) { stats ->
            if (stats == null) return@observe
            binding.tvTotalCount.text     = stats.total.toString()
            binding.tvActiveCount.text    = stats.active.toString()
            binding.tvFavoriteCount.text  = stats.favorites.toString()
            binding.tvDuplicateCount.text = stats.duplicates.toString()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_group_list -> {
                startActivity(Intent(this, GroupListActivity::class.java))
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
