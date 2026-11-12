package com.github.adriianh.innertube

import com.github.adriianh.innertube.models.YouTubeClient.Companion.WEB_REMIX
import com.github.adriianh.innertube.models.response.AccountSwitcherResponse
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccountSwitcherTest {
    // Anonymized capture of a real getAccountSwitcherEndpoint response (music.youtube.com,
    // WEB_REMIX). Section 0 is the current Google account with a primary and a brand channel;
    // the remaining sections belong to other signed-in Google accounts and must be ignored.
    private val accountSwitcherResponse =
        """
        )]}'
        {
          "code": "SUCCESS",
          "data": {
            "actions": [
              {
                "getMultiPageMenuAction": {
                  "menu": {
                    "multiPageMenuRenderer": {
                      "sections": [
                        {
                          "accountSectionListRenderer": {
                            "contents": [
                              {
                                "accountItemSectionRenderer": {
                                  "contents": [
                                    {
                                      "accountItem": {
                                        "accountName": { "runs": [{ "text": "Primary" }] },
                                        "accountPhoto": {
                                          "thumbnails": [
                                            { "url": "https://example.com/primary.jpg", "width": 48, "height": 48 }
                                          ]
                                        },
                                        "isSelected": true,
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
                                        "accountByline": { "runs": [{ "text": "No subscribers" }] },
                                        "channelHandle": { "runs": [{ "text": "@primary" }] }
                                      }
                                    },
                                    {
                                      "accountItem": {
                                        "accountName": { "runs": [{ "text": "Brand" }] },
                                        "accountPhoto": {
                                          "thumbnails": [
                                            { "url": "https://example.com/brand.jpg", "width": 48, "height": 48 }
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
                                              }
                                            ]
                                          }
                                        },
                                        "accountByline": { "runs": [{ "text": "No subscribers" }] },
                                        "channelHandle": { "runs": [{ "text": "@brand" }] }
                                      }
                                    }
                                  ]
                                }
                              }
                            ],
                            "header": {
                              "googleAccountHeaderRenderer": {
                                "name": { "runs": [{ "text": "Current Account" }] },
                                "email": { "runs": [{ "text": "current@example.com" }] }
                              }
                            }
                          }
                        },
                        {
                          "accountSectionListRenderer": {
                            "contents": [
                              {
                                "accountItemSectionRenderer": {
                                  "header": {
                                    "accountItemSectionHeaderRenderer": {
                                      "title": { "runs": [{ "text": "other1@example.com" }] }
                                    }
                                  }
                                }
                              }
                            ],
                            "header": {
                              "accountsDialogHeaderRenderer": {
                                "text": { "runs": [{ "text": "Other accounts", "bold": true }] }
                              }
                            }
                          }
                        },
                        {
                          "accountSectionListRenderer": {
                            "contents": [
                              {
                                "accountItemSectionRenderer": {
                                  "contents": [
                                    {
                                      "accountItem": {
                                        "accountName": { "runs": [{ "text": "Other Brand" }] },
                                        "isSelected": false,
                                        "hasChannel": true,
                                        "serviceEndpoint": {
                                          "selectActiveIdentityEndpoint": {
                                            "supportedTokens": [
                                              { "pageIdToken": { "pageId": "300000000000000000003" } }
                                            ]
                                          }
                                        },
                                        "accountByline": { "runs": [{ "text": "No subscribers" }] }
                                      }
                                    }
                                  ],
                                  "header": {
                                    "accountItemSectionHeaderRenderer": {
                                      "title": { "runs": [{ "text": "other2@example.com" }] }
                                    }
                                  }
                                }
                              }
                            ]
                          }
                        }
                      ]
                    }
                  }
                }
              }
            ]
          }
        }
        """.trimIndent()

    @Test
    fun `account switcher maps only the current account channels`() {
        val channels = AccountSwitcherResponse.parse(accountSwitcherResponse).toAccountChannels()

        assertEquals(2, channels.size)

        val primary = channels[0]
        assertEquals("Primary", primary.name)
        assertEquals("@primary", primary.handle)
        assertEquals("No subscribers", primary.byline)
        assertEquals("https://example.com/primary.jpg", primary.avatarUrl)
        assertNull(primary.pageId)
        assertEquals("100000000000000000001", primary.obfuscatedGaiaId)
        assertTrue(primary.isSelected)

        val brand = channels[1]
        assertEquals("Brand", brand.name)
        assertEquals("@brand", brand.handle)
        assertEquals("https://example.com/brand.jpg", brand.avatarUrl)
        assertEquals("200000000000000000002", brand.pageId)
        assertEquals("200000000000000000002", brand.obfuscatedGaiaId)
    }

    @Test
    fun `account switcher without a current account section yields no channels`() {
        val raw = ")]}'\n{\"code\":\"SUCCESS\",\"data\":{\"actions\":[]}}"

        assertEquals(emptyList(), AccountSwitcherResponse.parse(raw).toAccountChannels())
    }

    @Test
    fun `account switcher request is an authenticated GET on music youtube`() =
        runTest {
            var method: HttpMethod? = null
            var url: String? = null
            var cookieHeader: String? = null
            var authUserHeader: String? = null
            var pageIdHeader: String? = null
            val engine =
                MockEngine { request ->
                    method = request.method
                    url = request.url.toString()
                    cookieHeader = request.headers["cookie"]
                    authUserHeader = request.headers["X-Goog-AuthUser"]
                    pageIdHeader = request.headers["X-Goog-PageId"]
                    respond(accountSwitcherResponse, HttpStatusCode.OK)
                }
            val innerTube = InnerTube(engine)
            innerTube.cookie = "SAPISID=some-sapisid-value"
            innerTube.pageId = "200000000000000000002"

            val body = innerTube.accountSwitcher(WEB_REMIX).bodyAsText()

            assertEquals(HttpMethod.Get, method)
            assertTrue(
                url?.startsWith("https://music.youtube.com/getAccountSwitcherEndpoint") == true,
                "unexpected url: $url",
            )
            assertEquals("SAPISID=some-sapisid-value", cookieHeader)
            assertEquals("0", authUserHeader)
            assertEquals("200000000000000000002", pageIdHeader)
            assertEquals(2, AccountSwitcherResponse.parse(body).toAccountChannels().size)
        }
}
