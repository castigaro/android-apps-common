package com.appsonar.common.llm

import org.junit.Assert.assertEquals
import org.junit.Test

class ModelPricingTest {

    @Test
    fun `claude-sonnet-5 rechnet mit 3 und 15 Dollar pro Million Token`() {
        // 1 Mio Input (3 $) + 1 Mio Output (15 $) = 18 $ = 18.000.000 Mikro-Dollar.
        assertEquals(
            18_000_000L,
            ModelPricing.estimateCostMicros("anthropic", "claude-sonnet-5", 1_000_000, 1_000_000),
        )
    }

    @Test
    fun `claude-haiku rundet exakt auf Mikro-Dollar`() {
        // 1000 × 1$/MTok + 500 × 5$/MTok = 0,0035 $ = 3500 Mikro-Dollar.
        assertEquals(
            3500L,
            ModelPricing.estimateCostMicros("anthropic", "claude-haiku-4-5", 1000, 500),
        )
    }

    @Test
    fun `unbekanntes Anthropic-Modell nutzt Default-Preise`() {
        val unknown = ModelPricing.estimateCostMicros("anthropic", "claude-zukunft-9", 1000, 500)
        val default = ModelPricing.estimateCostMicros("anthropic", "claude-sonnet-5", 1000, 500)
        assertEquals(default, unknown)
    }

    @Test
    fun `unbekanntes OpenAI-Modell nutzt Default-Preise`() {
        // 1000 × 0,15$/MTok + 500 × 0,60$/MTok = 0,00045 $ = 450 Mikro-Dollar.
        assertEquals(450L, ModelPricing.estimateCostMicros("openai", "gpt-zukunft", 1000, 500))
    }

    @Test
    fun `null Token kosten nichts`() {
        assertEquals(0L, ModelPricing.estimateCostMicros("anthropic", "claude-sonnet-5", 0, 0))
    }
}
