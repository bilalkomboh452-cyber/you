
package com.waglo.app

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.waglo.app.adapter.GroupAdapter
import com.waglo.app.database.DatabaseHelper.Companion.COL_ADDED_AT
import com.waglo.app.database.DatabaseHelper.Companion.COL_CATEGORY
import com.waglo.app.database.DatabaseHelper.Companion.COL_CONFIDENCE
import com.waglo.app.database.DatabaseHelper.Companion.COL_SERIAL
import com.waglo.app.databinding.ActivityGroupListBinding
import com.waglo.app.model.GroupLink
import com.waglo.app.utils.ExportHelper
import com.waglo.app.utils.PrefsHelper
import com.waglo.app.viewmodel.GroupListViewModel
import java.io.IOException

class GroupListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGroupListBinding
    private val vm: GroupListViewModel by viewModels()
    private lateinit var adapter: GroupAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Restore sort preference
        vm.sortBy = PrefsHelper.getSortBy(this)

        setupRecyclerView()
        setupSearchBox()
        setupFilterSpinner()
        setupSelectionButtons()
        setupExportButtons()
        observeViewModel()
        vm.load()
    }

    private fun setupRecyclerView() {
        adapter = GroupAdapter(
            onNotesClick    = { group -> showGroupDialog(group) },
            onFavoriteClick = { group -> vm.toggleFavorite(group.id, group.isFavorite) }
        )
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@GroupListActivity)
            setHasFixedSize(true)
            adapter = this@GroupListActivity.adapter
        }
    }

    private fun setupSearchBox() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val q = s?.toString()?.trim() ?: ""
                if (q.isEmpty()) vm.clearSearch() else vm.search(q)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        binding.btnClearSearch.setOnClickListener {
            binding.etSearch.text?.clear()
            vm.clearSearch()
        }
    }

    private fun setupFilterSpinner() {
        val cats = listOf(getString(R.string.filter_all)) + GroupLink.Category.ALL
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, cats)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = spinnerAdapter
        binding.spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                vm.filterCategory = if (pos == 0) null else GroupLink.Category.ALL[pos - 1]
                vm.load()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        binding.chipFavorites.setOnCheckedChangeListener { _, checked ->
            vm.onlyFavorites = checked
            vm.load()
        }
    }

    private fun setupSelectionButtons() {
        binding.btnSelectAll.setOnClickListener { adapter.selectAll() }
        binding.btnUnselectAll.setOnClickListener { adapter.unselectAll() }
    }

    private fun setupExportButtons() {
        binding.btnExportTxt.setOnClickListener  { exportSelected("txt")  }
        binding.btnExportCsv.setOnClickListener  { exportSelected("csv")  }
        binding.btnExportJson.setOnClickListener { exportSelected("json") }
        binding.btnExportHtml.setOnClickListener { exportSelected("html") }
        binding.btnCopyClipboard.setOnClickListener {
            val selected = adapter.getSelectedGroups()
            if (selected.isEmpty()) {
                toast(getString(R.string.no_selection)); return@setOnClickListener
            }
            val ok = ExportHelper.copyToClipboard(this, selected)
            toast(getString(if (ok) R.string.copied_clipboard else R.string.export_failed))
        }
    }

    private fun observeViewModel() {
        vm.loading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }
        vm.groups.observe(this) { groups ->
            adapter.submitList(groups)
            binding.tvCount.text = getString(R.string.groups_count, groups.size)
            binding.tvEmpty.visibility = if (groups.isEmpty()) View.VISIBLE else View.GONE
        }
        vm.message.observe(this) { msg ->
            msg?.let { toast(it); vm.clearMessage() }
        }
    }

    private fun exportSelected(format: String) {
        val selected = adapter.getSelectedGroups()
        if (selected.isEmpty()) { toast(getString(R.string.no_selection)); return }
        try {
            val file = when (format) {
                "txt"  -> ExportHelper.exportAsTxt(this, selected)
                "csv"  -> ExportHelper.exportAsCsv(this, selected)
                "json" -> ExportHelper.exportAsJson(this, selected)
                "html" -> ExportHelper.exportAsHtml(this, selected)
                else   -> return
            }
            val mime = when (format) {
                "csv"  -> "text/csv"
                "json" -> "application/json"
                "html" -> "text/html"
                else   -> "text/plain"
            }
            toast("${getString(R.string.exported_to)}: ${file.name}")
            startActivity(Intent.createChooser(
                ExportHelper.getShareIntent(this, file, mime),
                getString(R.string.share_via)
            ))
        } catch (e: IOException) {
            toast("${getString(R.string.export_failed)}: ${e.message}")
        } catch (e: Exception) {
            toast(getString(R.string.export_failed))
        }
    }

    private fun showGroupDialog(group: com.waglo.app.model.GroupLink) {
        val editText = EditText(this).apply {
            setText(group.notes)
            hint = getString(R.string.add_notes_hint)
        }
        AlertDialog.Builder(this)
            .setTitle(group.groupName.ifBlank { group.inviteLink })
            .setView(editText)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                vm.updateNotes(group.id, editText.text?.toString() ?: "")
            }
            .setNeutralButton(getString(R.string.delete_group)) { _, _ ->
                AlertDialog.Builder(this)
                    .setMessage(getString(R.string.confirm_delete))
                    .setPositiveButton(getString(R.string.yes)) { _, _ -> vm.deleteGroup(group.id) }
                    .setNegativeButton(getString(R.string.no), null)
                    .show()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> { onBackPressedDispatcher.onBackPressed(); true }
            R.id.sort_serial    -> { applySortAndReload(COL_SERIAL);     true }
            R.id.sort_category  -> { applySortAndReload(COL_CATEGORY);   true }
            R.id.sort_date      -> { applySortAndReload(COL_ADDED_AT);   true }
            R.id.sort_confidence-> { applySortAndReload(COL_CONFIDENCE); true }
            R.id.remove_duplicates -> {
                AlertDialog.Builder(this)
                    .setMessage(getString(R.string.confirm_remove_duplicates))
                    .setPositiveButton(getString(R.string.yes)) { _, _ -> vm.deleteDuplicates() }
                    .setNegativeButton(getString(R.string.no), null).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun applySortAndReload(col: String) {
        vm.sortBy = col
        PrefsHelper.setSortBy(this, col)
        vm.load()
    }
}
