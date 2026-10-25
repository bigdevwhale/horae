package app.horae.data

import app.horae.domain.Place

/** Places to reckon the sun from without asking for location; the name keeps both languages as "en|ru". */
object Cities {
    data class City(val en: String, val ru: String, val lat: Double, val lon: Double) {
        val place get() = Place(lat, lon, "$en|$ru")
    }

    val all = listOf(
        City("Moscow", "Москва", 55.756, 37.617),
        City("Saint Petersburg", "Санкт-Петербург", 59.939, 30.316),
        City("Novosibirsk", "Новосибирск", 55.030, 82.920),
        City("Yekaterinburg", "Екатеринбург", 56.838, 60.597),
        City("Kazan", "Казань", 55.796, 49.106),
        City("Nizhny Novgorod", "Нижний Новгород", 56.327, 44.006),
        City("Samara", "Самара", 53.195, 50.100),
        City("Rostov-on-Don", "Ростов-на-Дону", 47.222, 39.720),
        City("Krasnodar", "Краснодар", 45.035, 38.975),
        City("Sochi", "Сочи", 43.585, 39.723),
        City("Voronezh", "Воронеж", 51.672, 39.184),
        City("Perm", "Пермь", 58.010, 56.229),
        City("Ufa", "Уфа", 54.735, 55.958),
        City("Krasnoyarsk", "Красноярск", 56.010, 92.852),
        City("Irkutsk", "Иркутск", 52.287, 104.305),
        City("Vladivostok", "Владивосток", 43.115, 131.885),
        City("Murmansk", "Мурманск", 68.970, 33.075),
        City("Arkhangelsk", "Архангельск", 64.539, 40.516),
        City("Kaliningrad", "Калининград", 54.710, 20.452),
        City("Yakutsk", "Якутск", 62.035, 129.675),
        City("Minsk", "Минск", 53.902, 27.562),
        City("Kyiv", "Киев", 50.450, 30.523),
        City("Almaty", "Алматы", 43.238, 76.946),
        City("Astana", "Астана", 51.169, 71.449),
        City("Tashkent", "Ташкент", 41.299, 69.240),
        City("Tbilisi", "Тбилиси", 41.716, 44.783),
        City("Yerevan", "Ереван", 40.179, 44.499),
        City("Baku", "Баку", 40.409, 49.867),
        City("Riga", "Рига", 56.950, 24.105),
        City("Vilnius", "Вильнюс", 54.687, 25.280),
        City("Tallinn", "Таллин", 59.437, 24.754),
        City("Helsinki", "Хельсинки", 60.170, 24.938),
        City("Warsaw", "Варшава", 52.230, 21.012),
        City("Prague", "Прага", 50.076, 14.438),
        City("Berlin", "Берлин", 52.520, 13.405),
        City("Vienna", "Вена", 48.208, 16.373),
        City("Paris", "Париж", 48.857, 2.352),
        City("London", "Лондон", 51.507, -0.128),
        City("Madrid", "Мадрид", 40.417, -3.704),
        City("Rome", "Рим", 41.903, 12.496),
        City("Athens", "Афины", 37.984, 23.728),
        City("Belgrade", "Белград", 44.787, 20.457),
        City("Istanbul", "Стамбул", 41.008, 28.978),
        City("Cyprus (Limassol)", "Кипр (Лимасол)", 34.707, 33.022),
        City("Jerusalem", "Иерусалим", 31.769, 35.216),
        City("Dubai", "Дубай", 25.205, 55.271),
        City("Bangkok", "Бангкок", 13.756, 100.502),
        City("Bali (Denpasar)", "Бали (Денпасар)", -8.650, 115.216),
        City("Tokyo", "Токио", 35.676, 139.650),
        City("Kyoto", "Киото", 35.012, 135.768),
        City("New York", "Нью-Йорк", 40.713, -74.006),
        City("San Francisco", "Сан-Франциско", 37.775, -122.419),
        City("Buenos Aires", "Буэнос-Айрес", -34.604, -58.382),
        City("Sydney", "Сидней", -33.869, 151.209),
    )

    val default = all.first().place

    /** The listed city within about 80 km of [p], if any. */
    fun nearest(p: Place): City? = all.minByOrNull { km(p, it) }?.takeIf { km(p, it) < 80 }

    private fun km(p: Place, c: City): Double {
        val dLat = Math.toRadians(c.lat - p.lat)
        val dLon = Math.toRadians(c.lon - p.lon)
        val a = Math.sin(dLat / 2).let { it * it } + Math.cos(Math.toRadians(p.lat)) * Math.cos(Math.toRadians(c.lat)) * Math.sin(dLon / 2).let { it * it }
        return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }

    fun name(p: Place, ru: Boolean): String {
        if (p.name.isEmpty()) {
            val near = nearest(p)
            return if (near != null) (if (ru) "рядом: ${near.ru}" else "near ${near.en}") else "%.2f°, %.2f°".format(p.lat, p.lon)
        }
        val parts = p.name.split('|')
        return if (ru) parts.getOrElse(1) { parts[0] } else parts[0]
    }
}
