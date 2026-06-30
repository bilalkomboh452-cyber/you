
package com.waglo.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import com.waglo.app.databinding.ActivitySettingsBinding
import com.waglo.app.repository.GroupLinkRepository
import com.waglo.app.utils.PrefsHelper
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var repo: GroupLinkRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        repo = GroupLinkRepository(this)

        // Dark mode
        binding.switchDarkMode.isChecked = PrefsHelper.isDarkMode(this)
        binding.switchDarkMode.setOnCheckedChangeListener { _, checked ->
            PrefsHelper.setDarkMode(this, checked)
            AppCompatDelegate.setDefaultNightMode(
                if (checked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
            recreate()
        }

        // Language
        binding.rgLanguage.check(
            if (PrefsHelper.getLanguage(this) == "ur") R.id.rbUrdu else R.id.rbEnglish
        )
        binding.rgLanguage.setOnCheckedChangeListener { _, checkedId ->
            val lang = if (checkedId == R.id.rbUrdu) "ur" else "en"
            if (lang != PrefsHelper.getLanguage(this)) {
                PrefsHelper.setLanguage(this, lang)
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang))
            }
        }

        // Auto-classify
        binding.switchAutoClassify.isChecked = PrefsHelper.isAutoClassify(this)
        binding.switchAutoClassify.setOnCheckedChangeListener { _, checked ->
            PrefsHelper.setAutoClassify(this, checked)
        }

        // Accessibility
        binding.btnAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        // Delete all
        binding.btnDeleteAll.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.delete_all_title))
                .setMessage(getString(R.string.delete_all_msg))
                .setPositiveButton(getString(R.string.yes)) { _, _ ->
                    lifecycleScope.launch {
                        repo.deleteAll()
                        Toast.makeText(
                            this@SettingsActivity,
                            getString(R.string.all_deleted),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .setNegativeButton(getString(R.string.no), null)
                .show()
        }

        // About
        binding.btnAbout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.app_name))
                .setMessage(getString(R.string.about_message))
                .setPositiveButton("OK", null)
                .show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
