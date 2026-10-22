package ir.hanzodev1375.ghostide.dialogs

import android.content.Context
import android.view.LayoutInflater
import android.widget.RadioButton
import android.widget.RadioGroup
import com.google.android.material.materialswitch.MaterialSwitch
import ir.hanzodev1375.components.sheet.customitemsheet.ui.DialogCompat
import ir.hanzodev1375.ghostide.R
import ir.hanzodev1375.ghostide.codeeditors.setting.PreferencesUtils
import ir.hanzodev1375.ghostide.enums.SortType
import ir.theme.M3Theme

class FileSortManager(
    private val context: Context,
    private val dirPath: String,
    private val onApplied: Runnable?
) {

    private val preferences = PreferencesUtils(context)

    fun show() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_file_sort, null)
        val group = dialogView.findViewById<RadioGroup>(R.id.sort_group)
        val onlyFolder = dialogView.findViewById<MaterialSwitch>(R.id.sort_only_folder)
        val reverse = dialogView.findViewById<MaterialSwitch>(R.id.sort_reverse)
        val sortName = dialogView.findViewById<RadioButton>(R.id.sort_name)
        val sortSize = dialogView.findViewById<RadioButton>(R.id.sort_size)
        val sortDate = dialogView.findViewById<RadioButton>(R.id.sort_date)
        val sortType = dialogView.findViewById<RadioButton>(R.id.sort_type)

        M3Theme.radio(sortName, sortSize, sortDate, sortType)
<<<<<<< HEAD
=======
        M3Theme.text(sortName, sortSize, sortDate, sortType)
>>>>>>> branch 'main' of https://github.com/HanzoDev30/Ghostide.git
        M3Theme.toggle(onlyFolder, reverse)
<<<<<<< HEAD
=======
        M3Theme.text(onlyFolder, reverse)

>>>>>>> branch 'main' of https://github.com/HanzoDev30/Ghostide.git
        group.check(radioIdFor(SortType.fromKey(preferences.getFileSortType(dirPath))))
        reverse.isChecked = preferences.isFileSortReverse(dirPath)
        onlyFolder.isChecked = preferences.hasFolderSortOverride(dirPath)

        DialogCompat(context)
            .setTitle(R.string.sort_title)
            .setView(dialogView)
            .setPositiveButton(R.string.ok) { _, _ ->
                val type = sortTypeFor(group.checkedRadioButtonId)
                val reverseOrder = reverse.isChecked
                if (onlyFolder.isChecked) {
                    preferences.setFileSortForFolder(dirPath, type.key, reverseOrder)
                } else {
                    preferences.setFileSortGlobal(type.key, reverseOrder)
                    preferences.clearFileSortForFolder(dirPath)
                }
                onApplied?.run()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun radioIdFor(type: SortType): Int = when (type) {
        SortType.SIZE -> R.id.sort_size
        SortType.DATE -> R.id.sort_date
        SortType.TYPE -> R.id.sort_type
        else -> R.id.sort_name
    }

    private fun sortTypeFor(radioId: Int): SortType = when (radioId) {
        R.id.sort_size -> SortType.SIZE
        R.id.sort_date -> SortType.DATE
        R.id.sort_type -> SortType.TYPE
        else -> SortType.NAME
    }
}
