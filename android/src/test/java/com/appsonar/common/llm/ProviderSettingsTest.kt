package com.appsonar.common.llm

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProviderSettingsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    /** Roh-Zugriff auf die Prefs, ohne die Migration anzustoßen. */
    private fun rawPrefs() = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    @Before
    fun clearPrefs() {
        rawPrefs().edit().clear().commit()
    }

    // ---- Migration vom alten Ein-Key-Schema ----

    @Test
    fun `Migration ordnet alten Key dem damals gewaehlten Provider zu`() {
        rawPrefs().edit()
            .putString("provider", ProviderSettings.PROVIDER_OPENAI)
            .putString("apiKey", "sk-alt")
            .putString("model", "gpt-4o")
            .commit()

        assertEquals("sk-alt", ProviderSettings.getKey(context, ProviderSettings.PROVIDER_OPENAI))
        assertEquals("gpt-4o", ProviderSettings.getModel(context, ProviderSettings.PROVIDER_OPENAI))
        assertEquals(ProviderSettings.PROVIDER_OPENAI, ProviderSettings.getPrimaryProvider(context))
        assertTrue(ProviderSettings.isEnabled(context, ProviderSettings.PROVIDER_OPENAI))
        // Alte Schlüssel sind entfernt, der andere Provider bleibt leer.
        assertFalse(rawPrefs().contains("apiKey"))
        assertFalse(rawPrefs().contains("provider"))
        assertFalse(rawPrefs().contains("model"))
        assertEquals("", ProviderSettings.getKey(context, ProviderSettings.PROVIDER_ANTHROPIC))
    }

    @Test
    fun `Migration mit Anthropic als altem Provider`() {
        rawPrefs().edit()
            .putString("provider", ProviderSettings.PROVIDER_ANTHROPIC)
            .putString("apiKey", "sk-ant")
            .putString("model", "")
            .commit()

        assertEquals("sk-ant", ProviderSettings.getKey(context, ProviderSettings.PROVIDER_ANTHROPIC))
        assertEquals(ProviderSettings.PROVIDER_ANTHROPIC, ProviderSettings.getPrimaryProvider(context))
        // Leeres Alt-Modell → Default des Providers.
        assertEquals(
            ProviderSettings.DEFAULT_MODEL_ANTHROPIC,
            ProviderSettings.getModel(context, ProviderSettings.PROVIDER_ANTHROPIC),
        )
    }

    @Test
    fun `Ohne Altdaten passiert bei der Migration nichts`() {
        assertEquals(ProviderSettings.PROVIDER_ANTHROPIC, ProviderSettings.getPrimaryProvider(context))
        assertEquals("", ProviderSettings.getKey(context, ProviderSettings.PROVIDER_ANTHROPIC))
        assertEquals("", ProviderSettings.getKey(context, ProviderSettings.PROVIDER_OPENAI))
    }

    // ---- activeConfig ----

    private fun saveBoth(
        primary: String = ProviderSettings.PROVIDER_ANTHROPIC,
        anthropicKey: String = "",
        anthropicEnabled: Boolean = true,
        openaiKey: String = "",
        openaiEnabled: Boolean = true,
    ) = ProviderSettings.save(
        context,
        primaryProvider = primary,
        anthropicKey = anthropicKey,
        anthropicModel = "claude-sonnet-5",
        anthropicEnabled = anthropicEnabled,
        openaiKey = openaiKey,
        openaiModel = "gpt-4o-mini",
        openaiEnabled = openaiEnabled,
    )

    @Test
    fun `activeConfig liefert Werte des primaeren Providers`() {
        saveBoth(anthropicKey = "sk-1")
        val config = ProviderSettings.activeConfig(context)!!
        assertEquals(ProviderSettings.PROVIDER_ANTHROPIC, config.provider)
        assertEquals("sk-1", config.apiKey)
        assertEquals("claude-sonnet-5", config.model)
    }

    @Test
    fun `activeConfig ist null bei leerem Key`() {
        saveBoth(anthropicKey = "")
        assertNull(ProviderSettings.activeConfig(context))
    }

    @Test
    fun `activeConfig ist null bei deaktiviertem Key`() {
        saveBoth(anthropicKey = "sk-1", anthropicEnabled = false)
        assertNull(ProviderSettings.activeConfig(context))
    }

    @Test
    fun `Kein Fallback auf den anderen Provider`() {
        // Primär Anthropic ohne Key — OpenAI hat einen, darf aber ruhen.
        saveBoth(primary = ProviderSettings.PROVIDER_ANTHROPIC, openaiKey = "sk-oai")
        assertNull(ProviderSettings.activeConfig(context))
    }

    // ---- deleteKey ----

    @Test
    fun `deleteKey entfernt Key und Kostenzaehler`() {
        saveBoth(anthropicKey = "sk-1")
        ProviderSettings.addCostMicros(context, ProviderSettings.PROVIDER_ANTHROPIC, 500L)

        ProviderSettings.deleteKey(context, ProviderSettings.PROVIDER_ANTHROPIC)

        assertEquals("", ProviderSettings.getKey(context, ProviderSettings.PROVIDER_ANTHROPIC))
        assertEquals(0L, ProviderSettings.getCostMicros(context, ProviderSettings.PROVIDER_ANTHROPIC))
    }

    @Test
    fun `deleteKey macht den anderen Provider primaer wenn er einen Key hat`() {
        saveBoth(primary = ProviderSettings.PROVIDER_ANTHROPIC, anthropicKey = "sk-1", openaiKey = "sk-2")
        ProviderSettings.deleteKey(context, ProviderSettings.PROVIDER_ANTHROPIC)
        assertEquals(ProviderSettings.PROVIDER_OPENAI, ProviderSettings.getPrimaryProvider(context))
    }

    @Test
    fun `deleteKey laesst primaer unveraendert wenn der andere keinen Key hat`() {
        saveBoth(primary = ProviderSettings.PROVIDER_ANTHROPIC, anthropicKey = "sk-1")
        ProviderSettings.deleteKey(context, ProviderSettings.PROVIDER_ANTHROPIC)
        assertEquals(ProviderSettings.PROVIDER_ANTHROPIC, ProviderSettings.getPrimaryProvider(context))
    }

    // ---- Kostenzähler ----

    @Test
    fun `addCostMicros akkumuliert und ignoriert nicht-positive Werte`() {
        val p = ProviderSettings.PROVIDER_OPENAI
        ProviderSettings.addCostMicros(context, p, 100L)
        ProviderSettings.addCostMicros(context, p, 200L)
        ProviderSettings.addCostMicros(context, p, 0L)
        ProviderSettings.addCostMicros(context, p, -50L)
        assertEquals(300L, ProviderSettings.getCostMicros(context, p))

        ProviderSettings.resetCost(context, p)
        assertEquals(0L, ProviderSettings.getCostMicros(context, p))
    }

    @Test
    fun `save trimmt Key und Modell`() {
        saveBoth(anthropicKey = "sk-1")
        ProviderSettings.save(
            context,
            primaryProvider = ProviderSettings.PROVIDER_ANTHROPIC,
            anthropicKey = "  sk-space  ",
            anthropicModel = "  modell  ",
            anthropicEnabled = true,
            openaiKey = "",
            openaiModel = "",
            openaiEnabled = true,
        )
        assertEquals("sk-space", ProviderSettings.getKey(context, ProviderSettings.PROVIDER_ANTHROPIC))
        assertEquals("modell", ProviderSettings.getModel(context, ProviderSettings.PROVIDER_ANTHROPIC))
    }
}
