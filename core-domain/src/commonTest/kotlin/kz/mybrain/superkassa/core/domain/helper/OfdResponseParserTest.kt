package kz.mybrain.superkassa.core.domain.helper

import kotlinx.serialization.json.*
import kz.mybrain.superkassa.core.domain.model.ofd.OfdServiceInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertFailsWith

class OfdResponseParserTest {

    private val fallback = OfdServiceInfo(
        orgTitle = "Fallback",
        orgAddress = "Addr",
        orgAddressKz = "AddrKz",
        orgInn = "000",
        orgOkved = "000",
        geoLatitude = 0,
        geoLongitude = 0,
        geoSource = "src"
    )

    @Test
    fun testExtractShiftNumberHappyPath() {
        val json = buildJsonObject {
            put("payload", buildJsonObject {
                put("report", buildJsonObject {
                    put("zxReport", buildJsonObject {
                        put("shiftNumber", 42)
                    })
                })
            })
        }
        assertEquals(42, OfdResponseParser.extractShiftNumber(json))
    }

    @Test
    fun testExtractShiftNumberNonHappyPaths() {
        assertNull(OfdResponseParser.extractShiftNumber(null))

        // zxReport missing
        val jsonNoZx = buildJsonObject {
            put("payload", buildJsonObject {
                put("report", buildJsonObject {})
            })
        }
        assertNull(OfdResponseParser.extractShiftNumber(jsonNoZx))

        // shiftNumber missing
        val jsonNoShift = buildJsonObject {
            put("payload", buildJsonObject {
                put("report", buildJsonObject {
                    put("zxReport", buildJsonObject {})
                })
            })
        }
        assertNull(OfdResponseParser.extractShiftNumber(jsonNoShift))

        // shiftNumber not an integer
        val jsonBadShift = buildJsonObject {
            put("payload", buildJsonObject {
                put("report", buildJsonObject {
                    put("zxReport", buildJsonObject {
                        put("shiftNumber", "not-an-int")
                    })
                })
            })
        }
        assertNull(OfdResponseParser.extractShiftNumber(jsonBadShift))
    }

    @Test
    fun testExtractServiceInfoHappyPath() {
        val json = buildJsonObject {
            put("payload", buildJsonObject {
                put("service", buildJsonObject {
                    put("regInfo", buildJsonObject {
                        put("org", buildJsonObject {
                            put("title", "My Org")
                            put("address", "Org Addr")
                            put("addressKz", "Org Addr Kz")
                            put("inn", "12345")
                            put("okved", "999")
                        })
                        put("pos", buildJsonObject {
                            put("latitude", 45)
                            put("longitude", 78)
                        })
                    })
                })
            })
        }

        val res = OfdResponseParser.extractServiceInfo(json, fallback)
        assertEquals("My Org", res.orgTitle)
        assertEquals("12345", res.orgInn)
        assertEquals("999", res.orgOkved)
        assertEquals("Org Addr", res.orgAddress)
        assertEquals("Org Addr Kz", res.orgAddressKz)
        assertEquals(45, res.geoLatitude)
        assertEquals(78, res.geoLongitude)
    }

    @Test
    fun testExtractServiceInfoFallbackPaths() {
        // null response
        assertEquals(fallback, OfdResponseParser.extractServiceInfo(null, fallback))

        // payload missing
        val jsonNoPayload = buildJsonObject {}
        assertEquals(fallback, OfdResponseParser.extractServiceInfo(jsonNoPayload, fallback))

        // service missing
        val jsonNoService = buildJsonObject { put("payload", buildJsonObject {}) }
        assertEquals(fallback, OfdResponseParser.extractServiceInfo(jsonNoService, fallback))

        // regInfo missing
        val jsonNoReg = buildJsonObject {
            put("payload", buildJsonObject { put("service", buildJsonObject {}) })
        }
        assertEquals(fallback, OfdResponseParser.extractServiceInfo(jsonNoReg, fallback))

        // org & pos missing (but regInfo exists)
        val jsonRegEmpty = buildJsonObject {
            put("payload", buildJsonObject {
                put("service", buildJsonObject {
                    put("regInfo", buildJsonObject {})
                })
            })
        }
        val resEmpty = OfdResponseParser.extractServiceInfo(jsonRegEmpty, fallback)
        assertEquals(fallback.orgTitle, resEmpty.orgTitle)
        assertEquals(fallback.orgAddress, resEmpty.orgAddress)
        assertEquals(fallback.orgAddressKz, resEmpty.orgAddressKz)
        assertEquals(fallback.orgInn, resEmpty.orgInn)
        assertEquals(fallback.orgOkved, resEmpty.orgOkved)
        assertEquals(fallback.geoLatitude, resEmpty.geoLatitude)
        assertEquals(fallback.geoLongitude, resEmpty.geoLongitude)

        // address and addressKz in pos instead of org
        val jsonAddressInPos = buildJsonObject {
            put("payload", buildJsonObject {
                put("service", buildJsonObject {
                    put("regInfo", buildJsonObject {
                        put("org", buildJsonObject {})
                        put("pos", buildJsonObject {
                            put("address", "Pos Addr")
                            put("addressKz", "Pos Addr Kz")
                        })
                    })
                })
            })
        }
        val resAddressInPos = OfdResponseParser.extractServiceInfo(jsonAddressInPos, fallback)
        assertEquals("Pos Addr", resAddressInPos.orgAddress)
        assertEquals("Pos Addr Kz", resAddressInPos.orgAddressKz)
    }

    @Test
    fun testExtractRegistrationNumber() {
        assertNull(OfdResponseParser.extractRegistrationNumber(null))

        // fnsKkmId present
        val json1 = buildJsonObject {
            put("payload", buildJsonObject {
                put("service", buildJsonObject {
                    put("regInfo", buildJsonObject {
                        put("kkm", buildJsonObject {
                            put("fnsKkmId", "fns-id-123")
                        })
                    })
                })
            })
        }
        assertEquals("fns-id-123", OfdResponseParser.extractRegistrationNumber(json1))

        // fnsKkmId blank, registrationNumber in pos present
        val json2 = buildJsonObject {
            put("payload", buildJsonObject {
                put("service", buildJsonObject {
                    put("regInfo", buildJsonObject {
                        put("kkm", buildJsonObject {
                            put("fnsKkmId", "  ")
                        })
                        put("pos", buildJsonObject {
                            put("registrationNumber", "pos-reg-1")
                        })
                    })
                })
            })
        }
        assertEquals("pos-reg-1", OfdResponseParser.extractRegistrationNumber(json2))

        // fnsKkmId missing, registrationNumber missing, regNumber in pos present
        val json3 = buildJsonObject {
            put("payload", buildJsonObject {
                put("service", buildJsonObject {
                    put("regInfo", buildJsonObject {
                        put("pos", buildJsonObject {
                            put("regNumber", "pos-reg-2")
                        })
                    })
                })
            })
        }
        assertEquals("pos-reg-2", OfdResponseParser.extractRegistrationNumber(json3))

        // all registration fields missing
        val json4 = buildJsonObject {
            put("payload", buildJsonObject {
                put("service", buildJsonObject {
                    put("regInfo", buildJsonObject {})
                })
            })
        }
        assertNull(OfdResponseParser.extractRegistrationNumber(json4))
    }

    @Test
    fun testExtractFactoryNumber() {
        assertNull(OfdResponseParser.extractFactoryNumber(null))

        // serialNumber present
        val json1 = buildJsonObject {
            put("payload", buildJsonObject {
                put("service", buildJsonObject {
                    put("regInfo", buildJsonObject {
                        put("kkm", buildJsonObject {
                            put("serialNumber", "serial-123")
                        })
                    })
                })
            })
        }
        assertEquals("serial-123", OfdResponseParser.extractFactoryNumber(json1))

        // serialNumber blank, factoryNumber in pos present
        val json2 = buildJsonObject {
            put("payload", buildJsonObject {
                put("service", buildJsonObject {
                    put("regInfo", buildJsonObject {
                        put("kkm", buildJsonObject {
                            put("serialNumber", "")
                        })
                        put("pos", buildJsonObject {
                            put("factoryNumber", "fac-1")
                        })
                    })
                })
            })
        }
        assertEquals("fac-1", OfdResponseParser.extractFactoryNumber(json2))

        // serialNumber missing, factoryNumber missing, factoryNum in pos present
        val json3 = buildJsonObject {
            put("payload", buildJsonObject {
                put("service", buildJsonObject {
                    put("regInfo", buildJsonObject {
                        put("pos", buildJsonObject {
                            put("factoryNum", "fac-2")
                        })
                    })
                })
            })
        }
        assertEquals("fac-2", OfdResponseParser.extractFactoryNumber(json3))

        // all factory fields missing
        val json4 = buildJsonObject {
            put("payload", buildJsonObject {
                put("service", buildJsonObject {
                    put("regInfo", buildJsonObject {})
                })
            })
        }
        assertNull(OfdResponseParser.extractFactoryNumber(json4))
    }

    @Test
    fun testExtractZxReportVariations() {
        // Case 1: zxReport in report
        val json1 = buildJsonObject {
            put("payload", buildJsonObject {
                put("report", buildJsonObject {
                    put("zxReport", buildJsonObject { put("source", "report-zx") })
                })
            })
        }
        val res1 = OfdResponseParser.extractZxReport(json1)
        assertEquals("report-zx", res1?.get("source")?.jsonPrimitive?.content)

        // Case 2: report lacks zxReport, but service has lastZReport with zxReport
        val json2 = buildJsonObject {
            put("payload", buildJsonObject {
                put("report", buildJsonObject {})
                put("service", buildJsonObject {
                    put("lastZReport", buildJsonObject {
                        put("zxReport", buildJsonObject { put("source", "last-z-zx") })
                    })
                })
            })
        }
        val res2 = OfdResponseParser.extractZxReport(json2)
        assertEquals("last-z-zx", res2?.get("source")?.jsonPrimitive?.content)

        // Case 3: report lacks zxReport, service has lastZReport which itself is the zxReport
        val json3 = buildJsonObject {
            put("payload", buildJsonObject {
                put("service", buildJsonObject {
                    put("lastZReport", buildJsonObject { put("source", "last-z-itself") })
                })
            })
        }
        val res3 = OfdResponseParser.extractZxReport(json3)
        assertEquals("last-z-itself", res3?.get("source")?.jsonPrimitive?.content)

        // Case 4: report lacks zxReport, lastZReport missing, but service has zxReport
        val json4 = buildJsonObject {
            put("payload", buildJsonObject {
                put("service", buildJsonObject {
                    put("zxReport", buildJsonObject { put("source", "service-zx") })
                })
            })
        }
        val res4 = OfdResponseParser.extractZxReport(json4)
        assertEquals("service-zx", res4?.get("source")?.jsonPrimitive?.content)

        // Case 5: all missing
        val json5 = buildJsonObject {
            put("payload", buildJsonObject {
                put("service", buildJsonObject {})
            })
        }
        assertNull(OfdResponseParser.extractZxReport(json5))
    }

    @Test
    fun testCorruptJsonStructureFails() {
        // Passing a string instead of object where object is expected
        val corruptJson = buildJsonObject {
            put("payload", "corrupt-string-value")
        }

        // Standard behavior of kotlinx.serialization when jsonObject is called on JsonPrimitive
        assertFailsWith<IllegalArgumentException> {
            OfdResponseParser.extractZxReport(corruptJson)
        }
    }
}
