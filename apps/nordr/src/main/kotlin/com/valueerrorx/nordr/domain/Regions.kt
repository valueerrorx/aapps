package com.valueerrorx.nordr.domain

// Magnetisch (missweisend) oder geografisch (rechtweisend)
enum class NorthRef { MAGNETIC, TRUE }

// Grobe Region statt Standort: reicht fuer die Deklination auf ein bis zwei Grad
data class Region(val id: String, val lat: Float, val lon: Float, val timeZones: List<String>)

// Deklination in Grad (Ost positiv) fuer Ort und Zeitpunkt; Android liefert sie ueber GeomagneticField
fun interface DeclinationModel {
    fun declination(lat: Float, lon: Float, timeMillis: Long): Float
}

object Regions {
    val all: List<Region> = listOf(
        Region("vienna", 48.21f, 16.37f, listOf("Europe/Vienna")),
        Region("innsbruck", 47.27f, 11.39f, emptyList()),
        Region("berlin", 52.52f, 13.40f, listOf("Europe/Berlin", "Europe/Busingen")),
        Region("hamburg", 53.55f, 9.99f, emptyList()),
        Region("cologne", 50.94f, 6.96f, emptyList()),
        Region("munich", 48.14f, 11.58f, emptyList()),
        Region("zurich", 47.37f, 8.54f, listOf("Europe/Zurich", "Europe/Vaduz")),
        Region("amsterdam", 52.37f, 4.90f, listOf("Europe/Amsterdam", "Europe/Brussels", "Europe/Luxembourg")),
        Region("london", 51.51f, -0.13f, listOf("Europe/London", "Europe/Dublin")),
        Region("paris", 48.86f, 2.35f, listOf("Europe/Paris", "Europe/Monaco")),
        Region("madrid", 40.42f, -3.70f, listOf("Europe/Madrid", "Europe/Lisbon")),
        Region("rome", 41.90f, 12.50f, listOf("Europe/Rome", "Europe/Malta", "Europe/Vatican", "Europe/San_Marino")),
        Region("ljubljana", 46.06f, 14.51f, listOf("Europe/Ljubljana", "Europe/Zagreb")),
        Region("prague", 50.08f, 14.44f, listOf("Europe/Prague", "Europe/Bratislava")),
        Region("budapest", 47.50f, 19.04f, listOf("Europe/Budapest", "Europe/Belgrade", "Europe/Sarajevo")),
        Region("warsaw", 52.23f, 21.01f, listOf("Europe/Warsaw")),
        Region("copenhagen", 55.68f, 12.57f, listOf("Europe/Copenhagen")),
        Region("stockholm", 59.33f, 18.07f, listOf("Europe/Stockholm", "Europe/Oslo", "Europe/Helsinki")),
        Region("athens", 37.98f, 23.73f, listOf("Europe/Athens", "Europe/Sofia", "Europe/Bucharest")),
        Region("istanbul", 41.01f, 28.98f, listOf("Europe/Istanbul")),
        Region("new_york", 40.71f, -74.01f, listOf("America/New_York", "America/Toronto", "America/Detroit")),
        Region("chicago", 41.88f, -87.63f, listOf("America/Chicago", "America/Winnipeg")),
        Region("denver", 39.74f, -104.99f, listOf("America/Denver", "America/Phoenix", "America/Edmonton")),
        Region("los_angeles", 34.05f, -118.24f, listOf("America/Los_Angeles", "America/Vancouver")),
        Region("mexico_city", 19.43f, -99.13f, listOf("America/Mexico_City")),
        Region("sao_paulo", -23.55f, -46.63f, listOf("America/Sao_Paulo")),
        Region("cape_town", -33.92f, 18.42f, listOf("Africa/Johannesburg")),
        Region("dubai", 25.20f, 55.27f, listOf("Asia/Dubai")),
        Region("delhi", 28.61f, 77.21f, listOf("Asia/Kolkata", "Asia/Calcutta")),
        Region("beijing", 39.90f, 116.40f, listOf("Asia/Shanghai", "Asia/Hong_Kong", "Asia/Taipei")),
        Region("tokyo", 35.68f, 139.69f, listOf("Asia/Tokyo", "Asia/Seoul")),
        Region("sydney", -33.87f, 151.21f, listOf("Australia/Sydney", "Australia/Melbourne", "Australia/Brisbane")),
        Region("auckland", -36.85f, 174.76f, listOf("Pacific/Auckland")),
    )

    fun byId(id: String?): Region? = all.firstOrNull { it.id == id }

    // Startwert aus der Zeitzone, ohne Standort-Berechtigung
    fun forTimeZone(tz: String): Region? = all.firstOrNull { tz in it.timeZones }
}
