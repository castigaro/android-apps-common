package com.appsonar.common.update

import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.appsonar.common.R
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Gemeinsame Oberfläche zur Update-Prüfung: Snackbar mit Download-Aktion,
 * wenn eine neue Version da ist; auf Wunsch ein "schon aktuell"-Hinweis.
 * Die Strings kommen aus dieser Bibliothek und können von der App lokal
 * überschrieben werden.
 */
object UpdateUi {

    fun showUpdateSnackbar(root: View, info: UpdateChecker.UpdateInfo) {
        Snackbar.make(
            root,
            root.context.getString(R.string.update_available, info.versionName),
            Snackbar.LENGTH_INDEFINITE,
        )
            .setAction(R.string.update_download) {
                root.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.apkUrl)))
            }
            .show()
    }

    /** Stille Prüfung beim Start — zeigt nur etwas, wenn eine neue Version da ist. */
    fun checkSilently(activity: AppCompatActivity, root: View, checker: UpdateChecker) {
        activity.lifecycleScope.launch {
            checker.check()?.let { showUpdateSnackbar(root, it) }
        }
    }

    /** Manuelle Prüfung über das Menü — meldet auch "schon aktuell". */
    fun checkManually(activity: AppCompatActivity, root: View, checker: UpdateChecker) {
        activity.lifecycleScope.launch {
            val info = checker.check()
            if (info != null) {
                showUpdateSnackbar(root, info)
            } else {
                Toast.makeText(activity, R.string.update_up_to_date, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
