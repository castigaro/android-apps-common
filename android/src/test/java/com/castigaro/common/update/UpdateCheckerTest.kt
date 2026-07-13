package com.castigaro.common.update

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UpdateCheckerTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
    }

    private fun checker(currentVersionCode: Int) =
        UpdateChecker(server.url("/version.json").toString(), currentVersionCode)

    @Test
    fun `neuere Version liefert UpdateInfo`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{"versionCode":5,"versionName":"2.0.0","apkUrl":"https://example.de/app.apk"}"""
            )
        )
        val info = checker(currentVersionCode = 4).check()
        assertEquals("2.0.0", info!!.versionName)
        assertEquals("https://example.de/app.apk", info.apkUrl)
    }

    @Test
    fun `fehlende apkUrl faellt auf appsonar zurueck`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"versionCode":5,"versionName":"2.0.0"}"""))
        val info = checker(currentVersionCode = 4).check()
        assertEquals("https://appsonar.de/", info!!.apkUrl)
    }

    @Test
    fun `gleiche Version liefert null`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"versionCode":4,"versionName":"1.0"}"""))
        assertNull(checker(currentVersionCode = 4).check())
    }

    @Test
    fun `aeltere Version liefert null`() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"versionCode":3,"versionName":"0.9"}"""))
        assertNull(checker(currentVersionCode = 4).check())
    }

    @Test
    fun `HTTP-Fehler liefert still null`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500))
        assertNull(checker(currentVersionCode = 1).check())
    }

    @Test
    fun `kaputtes JSON liefert still null`() = runBlocking {
        server.enqueue(MockResponse().setBody("das ist kein json {"))
        assertNull(checker(currentVersionCode = 1).check())
    }

    @Test
    fun `nicht erreichbarer Server liefert still null`() = runBlocking {
        val url = server.url("/version.json").toString()
        server.shutdown()
        assertNull(UpdateChecker(url, 1).check())
    }
}
