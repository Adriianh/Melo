package com.github.adriianh.innertube

import com.github.adriianh.innertube.models.YouTubeClient.Companion.WEB_REMIX
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Regression guard: cookies and the SAPISIDHASH header must only be attached
 * to requests that request the logged-in context, so anonymous calls keep
 * working when no session is configured.
 */
class InnerTubeAuthTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private val sessionCookies =
        "SAPISID=some-sapisid-value; __Secure-3PSID=some-psid-value; SID=some-sid-value"

    @Test
    fun `login request attaches cookie and sapisid hash`() = runTest {
        var cookieHeader: String? = null
        var authHeader: String? = null
        val engine = MockEngine { request ->
            cookieHeader = request.headers[HttpHeaders.Cookie]
            authHeader = request.headers[HttpHeaders.Authorization]
            respond(
                """{"actions":[{"openPopupAction":{"popup":{"multiPageMenuRenderer":{}}}}]}""",
                HttpStatusCode.OK,
                jsonHeaders,
            )
        }

        val innerTube = InnerTube(engine)
        innerTube.cookie = sessionCookies
        innerTube.accountMenu(WEB_REMIX)

        assertEquals(sessionCookies, cookieHeader, "cookie header must be attached")
        val prefix = "SAPISIDHASH "
        assertTrue(authHeader?.startsWith(prefix) == true, "SAPISIDHASH must be attached")
        val payload = authHeader!!.removePrefix(prefix)
        val parts = payload.split("_")
        assertEquals(2, parts.size, "SAPISIDHASH must be <timestamp>_<hash>")
        val expectedHash = com.github.adriianh.core.platform.sha1(
            "${parts[0]} some-sapisid-value ${com.github.adriianh.innertube.models.YouTubeClient.ORIGIN_YOUTUBE_MUSIC}"
        )
        assertEquals(expectedHash, parts[1], "hash must match sha1(timestamp SAPISID origin)")
    }

    @Test
    fun `anonymous request does not attach session headers`() = runTest {
        var cookieHeader: String? = "unset"
        var authHeader: String? = "unset"
        val engine = MockEngine { request ->
            cookieHeader = request.headers[HttpHeaders.Cookie]
            authHeader = request.headers[HttpHeaders.Authorization]
            respond("""{"contents":{}}""", HttpStatusCode.OK, jsonHeaders)
        }

        val innerTube = InnerTube(engine)
        innerTube.cookie = sessionCookies
        innerTube.browse(WEB_REMIX, browseId = "FEmusic_home")

        assertNull(cookieHeader, "anonymous browse must not send cookies")
        assertNull(authHeader, "anonymous browse must not send authorization")
    }

    @Test
    fun `useLoginForBrowse authenticates browse requests`() = runTest {
        var authHeader: String? = null
        val engine = MockEngine { request ->
            authHeader = request.headers[HttpHeaders.Authorization]
            respond("""{"contents":{}}""", HttpStatusCode.OK, jsonHeaders)
        }

        val innerTube = InnerTube(engine)
        innerTube.cookie = sessionCookies
        innerTube.useLoginForBrowse = true
        innerTube.browse(WEB_REMIX, browseId = "FEmusic_home")

        assertTrue(
            authHeader?.startsWith("SAPISIDHASH ") == true,
            "useLoginForBrowse must authenticate the home feed",
        )
    }
}