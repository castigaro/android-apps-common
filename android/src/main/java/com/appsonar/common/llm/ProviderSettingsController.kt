package com.appsonar.common.llm

import android.content.Intent
import android.net.Uri
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.appsonar.common.R
import com.appsonar.common.databinding.ProviderSettingsBinding
import com.google.android.material.snackbar.Snackbar
import java.util.Locale

/**
 * Verdrahtet den wiederverwendbaren Einstellungs-Block (provider_settings.xml)
 * einer App. Die Oberfläche ist auf den Normalfall "ein Key" zugeschnitten:
 *
 * - Kein Key gespeichert: Anbieter wählen (Toggle), nur dessen Sektion ist
 *   sichtbar, mit Link zur Key-Registrierung.
 * - Genau ein Key: nur diese Sektion; der zweite Anbieter lässt sich über
 *   einen Button einblenden.
 * - Beide Keys: beide Sektionen plus Toggle für den primären Anbieter —
 *   nur dieser wird genutzt, der andere ruht (kein Fallback).
 *
 * Gespeichert wird ausschließlich in den app-privaten SharedPreferences der
 * einbettenden App.
 */
class ProviderSettingsController(
    private val activity: AppCompatActivity,
    private val binding: ProviderSettingsBinding,
) {

    /** Vom Nutzer eingeblendete Zweit-Sektion (Zustand B), nicht persistent. */
    private var showSecondSection = false

    init {
        // Felder mit gespeicherten Werten vorbelegen.
        binding.inputKeyAnthropic.setText(ProviderSettings.getKey(activity, ProviderSettings.PROVIDER_ANTHROPIC))
        binding.inputModelAnthropic.setText(ProviderSettings.getModel(activity, ProviderSettings.PROVIDER_ANTHROPIC))
        binding.switchEnabledAnthropic.isChecked = ProviderSettings.isEnabled(activity, ProviderSettings.PROVIDER_ANTHROPIC)
        binding.inputKeyOpenai.setText(ProviderSettings.getKey(activity, ProviderSettings.PROVIDER_OPENAI))
        binding.inputModelOpenai.setText(ProviderSettings.getModel(activity, ProviderSettings.PROVIDER_OPENAI))
        binding.switchEnabledOpenai.isChecked = ProviderSettings.isEnabled(activity, ProviderSettings.PROVIDER_OPENAI)

        binding.inputModelAnthropic.setAdapter(
            ArrayAdapter(activity, android.R.layout.simple_list_item_1, ModelPricing.SUGGESTED_ANTHROPIC)
        )
        binding.inputModelOpenai.setAdapter(
            ArrayAdapter(activity, android.R.layout.simple_list_item_1, ModelPricing.SUGGESTED_OPENAI)
        )

        if (ProviderSettings.getPrimaryProvider(activity) == ProviderSettings.PROVIDER_OPENAI) {
            binding.providerToggle.check(binding.toggleOpenai.id)
        } else {
            binding.providerToggle.check(binding.toggleAnthropic.id)
        }

        // Im Zustand A (kein Key) schaltet der Toggle die sichtbare Sektion um.
        binding.providerToggle.addOnButtonCheckedListener { _, _, isChecked ->
            if (isChecked) refreshUi()
        }

        // Registrierungs-Link nur zeigen, solange das Key-Feld leer ist.
        binding.inputKeyAnthropic.afterTextChanged { refreshRegisterLinks() }
        binding.inputKeyOpenai.afterTextChanged { refreshRegisterLinks() }
        binding.registerLinkAnthropic.setOnClickListener { openUrl(URL_REGISTER_ANTHROPIC) }
        binding.registerLinkOpenai.setOnClickListener { openUrl(URL_REGISTER_OPENAI) }

        binding.buttonAddSecond.setOnClickListener {
            showSecondSection = true
            refreshUi()
        }

        binding.buttonResetCostAnthropic.setOnClickListener { resetCost(ProviderSettings.PROVIDER_ANTHROPIC) }
        binding.buttonResetCostOpenai.setOnClickListener { resetCost(ProviderSettings.PROVIDER_OPENAI) }
        binding.buttonDeleteAnthropic.setOnClickListener { deleteKey(ProviderSettings.PROVIDER_ANTHROPIC) }
        binding.buttonDeleteOpenai.setOnClickListener { deleteKey(ProviderSettings.PROVIDER_OPENAI) }

        binding.buttonSaveSettings.setOnClickListener { save() }

        refreshUi()
    }

    // ---- Zustandslogik ----

    private fun refreshUi() {
        val hasAnthropic = ProviderSettings.getKey(activity, ProviderSettings.PROVIDER_ANTHROPIC).isNotBlank()
        val hasOpenai = ProviderSettings.getKey(activity, ProviderSettings.PROVIDER_OPENAI).isNotBlank()

        val toggledOpenai = binding.providerToggle.checkedButtonId == binding.toggleOpenai.id

        when {
            // Zustand A: kein Key — Anbieter wählen, nur diese Sektion zeigen.
            !hasAnthropic && !hasOpenai -> {
                binding.providerToggleLabel.text = activity.getString(R.string.provider_choose_label)
                binding.providerToggleLabel.visibility = View.VISIBLE
                binding.providerToggle.visibility = View.VISIBLE
                binding.sectionAnthropic.visibility = if (toggledOpenai) View.GONE else View.VISIBLE
                binding.sectionOpenai.visibility = if (toggledOpenai) View.VISIBLE else View.GONE
                binding.buttonAddSecond.visibility = View.GONE
                showSecondSection = false
            }
            // Zustand C: beide Keys — Toggle wählt den primären Anbieter.
            hasAnthropic && hasOpenai -> {
                binding.providerToggleLabel.text = activity.getString(R.string.primary_provider_label)
                binding.providerToggleLabel.visibility = View.VISIBLE
                binding.providerToggle.visibility = View.VISIBLE
                binding.sectionAnthropic.visibility = View.VISIBLE
                binding.sectionOpenai.visibility = View.VISIBLE
                binding.buttonAddSecond.visibility = View.GONE
            }
            // Zustand B: genau ein Key — nur dessen Sektion, zweite auf Wunsch.
            else -> {
                binding.providerToggleLabel.visibility = View.GONE
                binding.providerToggle.visibility = View.GONE
                binding.sectionAnthropic.visibility =
                    if (hasAnthropic || showSecondSection) View.VISIBLE else View.GONE
                binding.sectionOpenai.visibility =
                    if (hasOpenai || showSecondSection) View.VISIBLE else View.GONE
                binding.buttonAddSecond.visibility = if (showSecondSection) View.GONE else View.VISIBLE
                binding.buttonAddSecond.text = activity.getString(
                    R.string.add_second_provider,
                    activity.getString(
                        if (hasAnthropic) R.string.provider_openai else R.string.provider_anthropic
                    ),
                )
            }
        }

        refreshRegisterLinks()
        refreshCosts()
    }

    private fun refreshRegisterLinks() {
        binding.registerLinkAnthropic.visibility =
            if (binding.inputKeyAnthropic.text.isNullOrBlank()) View.VISIBLE else View.GONE
        binding.registerLinkOpenai.visibility =
            if (binding.inputKeyOpenai.text.isNullOrBlank()) View.VISIBLE else View.GONE
    }

    private fun refreshCosts() {
        binding.costAnthropic.text = costText(ProviderSettings.PROVIDER_ANTHROPIC)
        binding.costOpenai.text = costText(ProviderSettings.PROVIDER_OPENAI)
    }

    private fun costText(provider: String): String {
        val usd = ProviderSettings.getCostMicros(activity, provider) / 1_000_000.0
        return activity.getString(R.string.api_costs, String.format(Locale.GERMANY, "%.4f", usd))
    }

    // ---- Aktionen ----

    private fun save() {
        val anthropicKey = binding.inputKeyAnthropic.text.toString().trim()
        val openaiKey = binding.inputKeyOpenai.text.toString().trim()

        // Primär ist der Toggle-Anbieter; hat der aber (nach diesem Speichern)
        // keinen Key und der andere schon, gewinnt der mit Key.
        val toggled = if (binding.providerToggle.checkedButtonId == binding.toggleOpenai.id) {
            ProviderSettings.PROVIDER_OPENAI
        } else {
            ProviderSettings.PROVIDER_ANTHROPIC
        }
        val toggledKey = if (toggled == ProviderSettings.PROVIDER_OPENAI) openaiKey else anthropicKey
        val otherKey = if (toggled == ProviderSettings.PROVIDER_OPENAI) anthropicKey else openaiKey
        val primary = if (toggledKey.isBlank() && otherKey.isNotBlank()) {
            ProviderSettings.otherProvider(toggled)
        } else {
            toggled
        }

        ProviderSettings.save(
            activity,
            primaryProvider = primary,
            anthropicKey = anthropicKey,
            anthropicModel = binding.inputModelAnthropic.text.toString(),
            anthropicEnabled = binding.switchEnabledAnthropic.isChecked,
            openaiKey = openaiKey,
            openaiModel = binding.inputModelOpenai.text.toString(),
            openaiEnabled = binding.switchEnabledOpenai.isChecked,
        )
        refreshUi()
        Snackbar.make(binding.root, R.string.provider_settings_saved, Snackbar.LENGTH_SHORT).show()
    }

    private fun deleteKey(provider: String) {
        ProviderSettings.deleteKey(activity, provider)
        if (provider == ProviderSettings.PROVIDER_ANTHROPIC) {
            binding.inputKeyAnthropic.setText("")
        } else {
            binding.inputKeyOpenai.setText("")
        }
        // Toggle auf den neuen primären Anbieter nachziehen.
        if (ProviderSettings.getPrimaryProvider(activity) == ProviderSettings.PROVIDER_OPENAI) {
            binding.providerToggle.check(binding.toggleOpenai.id)
        } else {
            binding.providerToggle.check(binding.toggleAnthropic.id)
        }
        showSecondSection = false
        refreshUi()
        Snackbar.make(binding.root, R.string.key_deleted, Snackbar.LENGTH_SHORT).show()
    }

    private fun resetCost(provider: String) {
        ProviderSettings.resetCost(activity, provider)
        refreshCosts()
        Snackbar.make(binding.root, R.string.api_costs_reset_done, Snackbar.LENGTH_SHORT).show()
    }

    private fun openUrl(url: String) {
        activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    companion object {
        private const val URL_REGISTER_ANTHROPIC = "https://console.anthropic.com/settings/keys"
        private const val URL_REGISTER_OPENAI = "https://platform.openai.com/api-keys"
    }
}

/** Kleiner Helfer, damit TextWatcher nicht drei leere Methoden braucht. */
private fun EditText.afterTextChanged(action: () -> Unit) {
    addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) = action()
    })
}
