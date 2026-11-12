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

    // Anonymized capture of a real accounts_list response (www.youtube.com, WEB client):
    // one primary channel without a pageId and one brand channel with a pageId.
    private val channelSwitcherResponse =
        """
        {
          "actions": [
            {
              "openPopupAction": {
                "popup": {
                  "channelSwitcherRenderer": {
                    "sections": [
                      {
                        "accountItemSectionRenderer": {
                          "contents": [
                            {
                              "accountItem": {
                                "accountName": { "simpleText": "Primary" },
                                "accountPhoto": {
                                  "thumbnails": [
                                    { "url": "https://example.com/primary.jpg", "width": 40, "height": 40 }
                                  ]
                                },
                                "isSelected": false,
                                "isDisabled": false,
                                "hasChannel": true,
                                "serviceEndpoint": {
                                  "selectActiveIdentityEndpoint": {
                                    "supportedTokens": [
                                      {
                                        "accountStateToken": {
                                          "hasChannel": true,
                                          "isMerged": true,
                                          "obfuscatedGaiaId": "100000000000000000001"
                                        }
                                      },
                                      { "offlineCacheKeyToken": { "clientCacheKey": "primary-cache-key" } },
                                      { "datasyncIdToken": { "datasyncIdToken": "100000000000000000001||" } }
                                    ]
                                  }
                                },
                                "accountByline": { "simpleText": "No subscribers" }
                              }
                            },
                            {
                              "accountItem": {
                                "accountName": { "simpleText": "Brand" },
                                "accountPhoto": {
                                  "thumbnails": [
                                    { "url": "https://example.com/brand.jpg", "width": 40, "height": 40 }
                                  ]
                                },
                                "isSelected": false,
                                "isDisabled": false,
                                "hasChannel": true,
                                "serviceEndpoint": {
                                  "selectActiveIdentityEndpoint": {
                                    "supportedTokens": [
                                      { "pageIdToken": { "pageId": "200000000000000000002" } },
                                      {
                                        "accountStateToken": {
                                          "hasChannel": true,
                                          "isMerged": true,
                                          "obfuscatedGaiaId": "200000000000000000002"
                                        }
                                      },
                                      {
                                        "datasyncIdToken": {
                                          "datasyncIdToken": "200000000000000000002||100000000000000000001"
                                        }
                                      }
                                    ]
                                  }
                                },
                                "accountByline": { "simpleText": "No subscribers" }
                              }
                            }
                          ]
                        }
                      }
                    ],
                    "header": {
                      "simpleMenuHeaderRenderer": {
                        "title": { "runs": [{ "text": "Choose a channel" }] }
                      }
                    }
                  }
                },
                "popupType": "MODAL"
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
            innerTube.pageId = "200000000000000000002"

            val channels =
                innerTube
                    .accountsList(WEB_REMIX)
                    .body<AccountsListResponse>()
                    .toAccountChannels()

            assertEquals(2, channels.size)

            val primary = channels[0]
            assertEquals("Primary", primary.name)
            assertNull(primary.handle)
            assertEquals("No subscribers", primary.byline)
            assertEquals("https://example.com/primary.jpg", primary.avatarUrl)
            assertNull(primary.pageId)
            assertEquals("100000000000000000001", primary.obfuscatedGaiaId)

            val brand = channels[1]
            assertEquals("Brand", brand.name)
            assertNull(brand.handle)
            assertEquals("No subscribers", brand.byline)
            assertEquals("https://example.com/brand.jpg", brand.avatarUrl)
            assertEquals("200000000000000000002", brand.pageId)
            assertEquals("200000000000000000002", brand.obfuscatedGaiaId)
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
            innerTube.pageId = "200000000000000000002"

            innerTube.accountsList(WEB_REMIX)

            assertEquals("/youtubei/v1/account/accounts_list", path)
            assertEquals("200000000000000000002", pageIdHeader)
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
