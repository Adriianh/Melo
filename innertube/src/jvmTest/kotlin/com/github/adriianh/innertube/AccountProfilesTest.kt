package com.github.adriianh.innertube

import com.github.adriianh.innertube.models.YouTubeClient.Companion.WEB_REMIX
import com.github.adriianh.innertube.models.body.AccountsListBody
import com.github.adriianh.innertube.models.response.AccountsListResponse
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccountProfilesTest {
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private val channelSwitcherResponse =
        """
        {
          "actions": [
            {
              "updateChannelSwitcherPageAction": {
                "page": {
                  "channelSwitcherPageRenderer": {
                    "contents": [
                      {
                        "accountItemRenderer": {
                          "accountName": { "simpleText": "SEG" },
                          "accountPhoto": {
                            "thumbnails": [
                              { "url": "https://example.com/primary.jpg" }
                            ]
                          },
                          "serviceEndpoint": {
                            "selectActiveIdentityEndpoint": {
                              "supportedTokens": [
                                {
                                  "accountStateToken": {
                                    "obfuscatedGaiaId": "105919436917282079228"
                                  }
                                }
                              ]
                            }
                          },
                          "channelHandle": { "simpleText": "@xxsegxxyt7653" }
                        }
                      },
                      {
                        "accountItemRenderer": {
                          "accountName": { "simpleText": "SEGm" },
                          "accountPhoto": {
                            "thumbnails": [
                              { "url": "https://example.com/brand.jpg" }
                            ]
                          },
                          "serviceEndpoint": {
                            "selectActiveIdentityEndpoint": {
                              "supportedTokens": [
                                {
                                  "pageIdToken": {
                                    "pageId": "112761172095825284049"
                                  }
                                },
                                {
                                  "accountStateToken": {
                                    "obfuscatedGaiaId": "112761172095825284049"
                                  }
                                }
                              ]
                            }
                          },
                          "channelHandle": { "simpleText": "@SEGm-f3e" }
                        }
                      }
                    ]
                  }
                }
              }
            }
          ]
        }
        """.trimIndent()

    @Test
    fun `accounts list response maps every channel`() =
        runTest {
            val engine =
                MockEngine {
                    respond(channelSwitcherResponse, HttpStatusCode.OK, jsonHeaders)
                }
            val innerTube = InnerTube(engine)
            innerTube.cookie = "SAPISID=some-sapisid-value"
            innerTube.pageId = "112761172095825284049"

            val channels =
                innerTube
                    .accountsList(WEB_REMIX)
                    .body<AccountsListResponse>()
                    .toAccountChannels()

            assertEquals(2, channels.size)

            val primary = channels[0]
            assertEquals("SEG", primary.name)
            assertEquals("@xxsegxxyt7653", primary.handle)
            assertEquals("https://example.com/primary.jpg", primary.avatarUrl)
            assertNull(primary.pageId)
            assertEquals("105919436917282079228", primary.obfuscatedGaiaId)

            val brand = channels[1]
            assertEquals("SEGm", brand.name)
            assertEquals("@SEGm-f3e", brand.handle)
            assertEquals("https://example.com/brand.jpg", brand.avatarUrl)
            assertEquals("112761172095825284049", brand.pageId)
            assertEquals("112761172095825284049", brand.obfuscatedGaiaId)
        }

    @Test
    fun `accounts list request sends the channel switcher body and page id`() =
        runTest {
            var bodyText: String? = null
            var pageIdHeader: String? = null
            var path: String? = null
            val engine =
                MockEngine { request ->
                    bodyText = (request.body as? TextContent)?.text
                    pageIdHeader = request.headers["X-Goog-PageId"]
                    path = request.url.encodedPath
                    respond(channelSwitcherResponse, HttpStatusCode.OK, jsonHeaders)
                }
            val innerTube = InnerTube(engine)
            innerTube.cookie = "SAPISID=some-sapisid-value"
            innerTube.pageId = "112761172095825284049"

            innerTube.accountsList(WEB_REMIX)

            assertEquals("/youtubei/v1/account/accounts_list", path)
            assertEquals("112761172095825284049", pageIdHeader)
            assertTrue(
                bodyText?.contains(AccountsListBody.REQUEST_TYPE_CHANNEL_SWITCHER) == true,
                "body must request the channel switcher",
            )
            assertTrue(
                bodyText?.contains(AccountsListBody.CALL_CIRCUMSTANCE_SWITCHING_USERS_FULL) == true,
                "body must use the full channel switcher circumstance",
            )
        }
}
