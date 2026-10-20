package com.example.data.model

enum class LocationType(val titleRu: String) {
    CITY("Город-столица"),
    FORT("Военный форт"),
    MINE("Ресурсная шахта"),
    SETTLEMENT("Стратегическое поселение"),
    STRONGHOLD("Оплот / Твердыня")
}

data class LocationNode(
    val id: String,
    val nameRu: String,
    val holdRu: String,
    val type: LocationType,
    val x: Float, // coordinate on authentic Skyrim province map (1200x896)
    val y: Float, // coordinate on authentic Skyrim province map (1200x896)
    val ownerFactionId: FactionId,
    val connections: List<String> = emptyList(),
    val garrison: Int,
    val maxGarrison: Int,
    val fortificationLevel: Int = 1, // 1..5
    val goldIncome: Int = 15,
    val oreIncome: Int = 5,
    val recruitIncome: Int = 5,
    val isUnderSiege: Boolean = false
)

object SkyrimLocationsData {
    val initialLocations = listOf(
        // === HAAFINGAR (ХААФИНГАР) ===
        LocationNode(
            id = "castle_volkihar",
            nameRu = "Замок Волкихар",
            holdRu = "Море Призраков",
            type = LocationType.STRONGHOLD,
            x = 160f, y = 140f,
            ownerFactionId = FactionId.VOLKIHAR,
            connections = listOf("fort_hraggstad"),
            garrison = 65, maxGarrison = 110, fortificationLevel = 5,
            goldIncome = 35, oreIncome = 10, recruitIncome = 14
        ),
        LocationNode(
            id = "fort_hraggstad",
            nameRu = "Форт Храгстад",
            holdRu = "Хаафингар",
            type = LocationType.FORT,
            x = 340f, y = 180f,
            ownerFactionId = FactionId.IMPERIAL,
            connections = listOf("solitude", "castle_volkihar"),
            garrison = 45, maxGarrison = 90, fortificationLevel = 3,
            goldIncome = 15, oreIncome = 10, recruitIncome = 10
        ),
        LocationNode(
            id = "solitude",
            nameRu = "Солитьюд (Синий Дворец)",
            holdRu = "Хаафингар",
            type = LocationType.CITY,
            x = 420f, y = 220f,
            ownerFactionId = FactionId.IMPERIAL,
            connections = listOf("dragon_bridge", "fort_hraggstad", "morthal"),
            garrison = 85, maxGarrison = 150, fortificationLevel = 4,
            goldIncome = 65, oreIncome = 15, recruitIncome = 16
        ),
        LocationNode(
            id = "dragon_bridge",
            nameRu = "Драконий Мост (Аванпост)",
            holdRu = "Хаафингар",
            type = LocationType.SETTLEMENT,
            x = 390f, y = 290f,
            ownerFactionId = FactionId.THALMOR,
            connections = listOf("solitude", "morthal", "karthwasten", "rorikstead"),
            garrison = 40, maxGarrison = 80, fortificationLevel = 2,
            goldIncome = 30, oreIncome = 15, recruitIncome = 8
        ),

        // === THE REACH (ПРЕДЕЛ) ===
        LocationNode(
            id = "druadach_redoubt",
            nameRu = "Оплот Друадах",
            holdRu = "Предел",
            type = LocationType.STRONGHOLD,
            x = 180f, y = 330f,
            ownerFactionId = FactionId.FORSWORN,
            connections = listOf("markarth", "karthwasten"),
            garrison = 55, maxGarrison = 100, fortificationLevel = 3,
            goldIncome = 20, oreIncome = 30, recruitIncome = 18
        ),
        LocationNode(
            id = "karthwasten",
            nameRu = "Картвастен (Серебряная шахта)",
            holdRu = "Предел",
            type = LocationType.MINE,
            x = 260f, y = 370f,
            ownerFactionId = FactionId.FORSWORN,
            connections = listOf("druadach_redoubt", "dragon_bridge", "kolskeggr_mine", "rorikstead"),
            garrison = 30, maxGarrison = 60, fortificationLevel = 1,
            goldIncome = 45, oreIncome = 50, recruitIncome = 6
        ),
        LocationNode(
            id = "markarth",
            nameRu = "Маркарт (Подкаменная крепость)",
            holdRu = "Предел",
            type = LocationType.CITY,
            x = 190f, y = 500f,
            ownerFactionId = FactionId.IMPERIAL,
            connections = listOf("kolskeggr_mine", "fort_sungard", "druadach_redoubt"),
            garrison = 80, maxGarrison = 140, fortificationLevel = 4,
            goldIncome = 70, oreIncome = 45, recruitIncome = 12
        ),
        LocationNode(
            id = "kolskeggr_mine",
            nameRu = "Золотая шахта Колскеггр",
            holdRu = "Предел",
            type = LocationType.MINE,
            x = 280f, y = 510f,
            ownerFactionId = FactionId.FORSWORN,
            connections = listOf("markarth", "karthwasten", "fort_sungard"),
            garrison = 35, maxGarrison = 70, fortificationLevel = 2,
            goldIncome = 75, oreIncome = 25, recruitIncome = 6
        ),
        LocationNode(
            id = "fort_sungard",
            nameRu = "Форт Сангард",
            holdRu = "Предел",
            type = LocationType.FORT,
            x = 370f, y = 550f,
            ownerFactionId = FactionId.IMPERIAL,
            connections = listOf("markarth", "kolskeggr_mine", "rorikstead", "falkreath"),
            garrison = 50, maxGarrison = 95, fortificationLevel = 4,
            goldIncome = 20, oreIncome = 20, recruitIncome = 12
        ),

        // === HJAALMARCH (ХЬЯЛМАРК) ===
        LocationNode(
            id = "fort_snowhawk",
            nameRu = "Форт Сноухок",
            holdRu = "Хьялмарк",
            type = LocationType.FORT,
            x = 490f, y = 270f,
            ownerFactionId = FactionId.BANDITS,
            connections = listOf("morthal", "solitude"),
            garrison = 35, maxGarrison = 75, fortificationLevel = 2,
            goldIncome = 25, oreIncome = 15, recruitIncome = 10
        ),
        LocationNode(
            id = "morthal",
            nameRu = "Морфал (Болота)",
            holdRu = "Хьялмарк",
            type = LocationType.CITY,
            x = 540f, y = 330f,
            ownerFactionId = FactionId.IMPERIAL,
            connections = listOf("solitude", "dragon_bridge", "fort_snowhawk", "dawnstar"),
            garrison = 50, maxGarrison = 100, fortificationLevel = 2,
            goldIncome = 30, oreIncome = 10, recruitIncome = 10
        ),

        // === THE PALE (БЕЛЫЙ БЕРЕГ) ===
        LocationNode(
            id = "dawnstar",
            nameRu = "Данстар (Северный порт)",
            holdRu = "Белый Берег",
            type = LocationType.CITY,
            x = 710f, y = 210f,
            ownerFactionId = FactionId.STORMCLOAK,
            connections = listOf("morthal", "fort_dunstad", "winterhold"),
            garrison = 60, maxGarrison = 110, fortificationLevel = 2,
            goldIncome = 45, oreIncome = 35, recruitIncome = 12
        ),
        LocationNode(
            id = "fort_dunstad",
            nameRu = "Форт Дунстад",
            holdRu = "Белый Берег",
            type = LocationType.FORT,
            x = 730f, y = 310f,
            ownerFactionId = FactionId.STORMCLOAK,
            connections = listOf("dawnstar", "whiterun", "windhelm", "silent_moons"),
            garrison = 45, maxGarrison = 85, fortificationLevel = 3,
            goldIncome = 20, oreIncome = 15, recruitIncome = 10
        ),

        // === WINTERHOLD (ВИНТЕРХОЛД) ===
        LocationNode(
            id = "winterhold",
            nameRu = "Винтерхолд (Коллегия Магов)",
            holdRu = "Винтерхолд",
            type = LocationType.CITY,
            x = 980f, y = 180f,
            ownerFactionId = FactionId.STORMCLOAK,
            connections = listOf("dawnstar", "fort_kastav", "windhelm"),
            garrison = 55, maxGarrison = 100, fortificationLevel = 2,
            goldIncome = 35, oreIncome = 10, recruitIncome = 8
        ),
        LocationNode(
            id = "fort_kastav",
            nameRu = "Форт Кастав (Ледяной форпост)",
            holdRu = "Винтерхолд",
            type = LocationType.FORT,
            x = 970f, y = 270f,
            ownerFactionId = FactionId.STORMCLOAK,
            connections = listOf("winterhold", "windhelm"),
            garrison = 40, maxGarrison = 80, fortificationLevel = 3,
            goldIncome = 15, oreIncome = 15, recruitIncome = 8
        ),

        // === WHITERUN HOLD (ВАЙТРАН) ===
        LocationNode(
            id = "silent_moons",
            nameRu = "Лагерь Чистых Лун (Лунный алтарь)",
            holdRu = "Вайтран",
            type = LocationType.STRONGHOLD,
            x = 590f, y = 390f,
            ownerFactionId = FactionId.BANDITS,
            connections = listOf("whiterun", "fort_greymoor", "fort_dunstad"),
            garrison = 30, maxGarrison = 60, fortificationLevel = 2,
            goldIncome = 25, oreIncome = 30, recruitIncome = 8
        ),
        LocationNode(
            id = "rorikstead",
            nameRu = "Рорикстед (Житница)",
            holdRu = "Вайтран",
            type = LocationType.SETTLEMENT,
            x = 430f, y = 450f,
            ownerFactionId = FactionId.IMPERIAL,
            connections = listOf("dragon_bridge", "karthwasten", "fort_sungard", "fort_greymoor"),
            garrison = 30, maxGarrison = 65, fortificationLevel = 1,
            goldIncome = 40, oreIncome = 5, recruitIncome = 14
        ),
        LocationNode(
            id = "fort_greymoor",
            nameRu = "Форт Греймур",
            holdRu = "Вайтран",
            type = LocationType.FORT,
            x = 560f, y = 460f,
            ownerFactionId = FactionId.BANDITS,
            connections = listOf("whiterun", "rorikstead", "silent_moons"),
            garrison = 40, maxGarrison = 80, fortificationLevel = 3,
            goldIncome = 20, oreIncome = 10, recruitIncome = 12
        ),
        LocationNode(
            id = "whiterun",
            nameRu = "Вайтран (Драконий Предел)",
            holdRu = "Вайтран",
            type = LocationType.CITY,
            x = 660f, y = 460f,
            ownerFactionId = FactionId.IMPERIAL,
            connections = listOf("fort_greymoor", "riverwood", "silent_moons", "fort_dunstad", "fort_amol"),
            garrison = 90, maxGarrison = 160, fortificationLevel = 4,
            goldIncome = 70, oreIncome = 20, recruitIncome = 20
        ),
        LocationNode(
            id = "riverwood",
            nameRu = "Ривервуд (Лесопилка)",
            holdRu = "Вайтран",
            type = LocationType.SETTLEMENT,
            x = 660f, y = 560f,
            ownerFactionId = FactionId.IMPERIAL,
            connections = listOf("whiterun", "helgen", "high_hrothgar"),
            garrison = 35, maxGarrison = 70, fortificationLevel = 1,
            goldIncome = 35, oreIncome = 10, recruitIncome = 12
        ),
        LocationNode(
            id = "high_hrothgar",
            nameRu = "Высокий Хротгар (Глотка Мира)",
            holdRu = "Вайтран",
            type = LocationType.STRONGHOLD,
            x = 750f, y = 530f,
            ownerFactionId = FactionId.IMPERIAL,
            connections = listOf("riverwood", "ivarshead"),
            garrison = 25, maxGarrison = 50, fortificationLevel = 5,
            goldIncome = 10, oreIncome = 0, recruitIncome = 6
        ),

        // === EASTMARCH (ИСТМАРК) ===
        LocationNode(
            id = "windhelm",
            nameRu = "Виндхельм (Королевский дворец)",
            holdRu = "Истмарк",
            type = LocationType.CITY,
            x = 920f, y = 340f,
            ownerFactionId = FactionId.STORMCLOAK,
            connections = listOf("winterhold", "fort_kastav", "fort_dunstad", "kynesgrove", "fort_amol"),
            garrison = 90, maxGarrison = 160, fortificationLevel = 4,
            goldIncome = 65, oreIncome = 25, recruitIncome = 18
        ),
        LocationNode(
            id = "kynesgrove",
            nameRu = "Роща Кин (Малахитовая шахта)",
            holdRu = "Истмарк",
            type = LocationType.MINE,
            x = 950f, y = 410f,
            ownerFactionId = FactionId.STORMCLOAK,
            connections = listOf("windhelm", "darkwater_crossing"),
            garrison = 30, maxGarrison = 65, fortificationLevel = 1,
            goldIncome = 30, oreIncome = 45, recruitIncome = 8
        ),
        LocationNode(
            id = "fort_amol",
            nameRu = "Форт Амол",
            holdRu = "Истмарк",
            type = LocationType.FORT,
            x = 830f, y = 460f,
            ownerFactionId = FactionId.STORMCLOAK,
            connections = listOf("windhelm", "whiterun", "darkwater_crossing", "ivarshead"),
            garrison = 50, maxGarrison = 90, fortificationLevel = 3,
            goldIncome = 20, oreIncome = 15, recruitIncome = 10
        ),
        LocationNode(
            id = "darkwater_crossing",
            nameRu = "Черный Брод (Корундовая шахта)",
            holdRu = "Истмарк",
            type = LocationType.MINE,
            x = 880f, y = 530f,
            ownerFactionId = FactionId.STORMCLOAK,
            connections = listOf("kynesgrove", "fort_amol", "shors_stone"),
            garrison = 30, maxGarrison = 60, fortificationLevel = 1,
            goldIncome = 30, oreIncome = 50, recruitIncome = 8
        ),

        // === FALKREATH HOLD (ФОЛКРИТ) ===
        LocationNode(
            id = "falkreath",
            nameRu = "Фолкрит (Длинный дом ярла)",
            holdRu = "Фолкрит",
            type = LocationType.CITY,
            x = 510f, y = 680f,
            ownerFactionId = FactionId.IMPERIAL,
            connections = listOf("fort_sungard", "helgen", "fort_neugrad"),
            garrison = 55, maxGarrison = 105, fortificationLevel = 2,
            goldIncome = 40, oreIncome = 20, recruitIncome = 12
        ),
        LocationNode(
            id = "helgen",
            nameRu = "Хелген (Крепость предгорий)",
            holdRu = "Фолкрит",
            type = LocationType.SETTLEMENT,
            x = 620f, y = 630f,
            ownerFactionId = FactionId.BANDITS,
            connections = listOf("riverwood", "falkreath", "fort_neugrad", "ivarshead"),
            garrison = 35, maxGarrison = 70, fortificationLevel = 2,
            goldIncome = 25, oreIncome = 10, recruitIncome = 10
        ),
        LocationNode(
            id = "fort_neugrad",
            nameRu = "Форт Нойград",
            holdRu = "Фолкрит",
            type = LocationType.FORT,
            x = 670f, y = 710f,
            ownerFactionId = FactionId.IMPERIAL,
            connections = listOf("falkreath", "helgen", "trevas_watch"),
            garrison = 45, maxGarrison = 85, fortificationLevel = 3,
            goldIncome = 20, oreIncome = 15, recruitIncome = 10
        ),

        // === THE RIFT (РИФТ) ===
        LocationNode(
            id = "ivarshead",
            nameRu = "Айварстед (Подножие 7000 ступеней)",
            holdRu = "Рифт",
            type = LocationType.SETTLEMENT,
            x = 780f, y = 590f,
            ownerFactionId = FactionId.STORMCLOAK,
            connections = listOf("high_hrothgar", "fort_amol", "helgen", "shors_stone", "trevas_watch"),
            garrison = 30, maxGarrison = 60, fortificationLevel = 1,
            goldIncome = 25, oreIncome = 10, recruitIncome = 10
        ),
        LocationNode(
            id = "shors_stone",
            nameRu = "Камень Шора (Эбонитовая шахта)",
            holdRu = "Рифт",
            type = LocationType.MINE,
            x = 940f, y = 590f,
            ownerFactionId = FactionId.STORMCLOAK,
            connections = listOf("darkwater_crossing", "ivarshead", "fort_greenwall"),
            garrison = 35, maxGarrison = 70, fortificationLevel = 2,
            goldIncome = 50, oreIncome = 65, recruitIncome = 8
        ),
        LocationNode(
            id = "trevas_watch",
            nameRu = "Дозор Тревы",
            holdRu = "Рифт",
            type = LocationType.FORT,
            x = 880f, y = 670f,
            ownerFactionId = FactionId.BANDITS,
            connections = listOf("ivarshead", "fort_neugrad", "riften"),
            garrison = 35, maxGarrison = 75, fortificationLevel = 2,
            goldIncome = 25, oreIncome = 10, recruitIncome = 10
        ),
        LocationNode(
            id = "fort_greenwall",
            nameRu = "Форт Гринвол",
            holdRu = "Рифт",
            type = LocationType.FORT,
            x = 970f, y = 650f,
            ownerFactionId = FactionId.STORMCLOAK,
            connections = listOf("shors_stone", "riften"),
            garrison = 45, maxGarrison = 85, fortificationLevel = 3,
            goldIncome = 20, oreIncome = 15, recruitIncome = 10
        ),
        LocationNode(
            id = "riften",
            nameRu = "Рифтен (Крепость Миствейл)",
            holdRu = "Рифт",
            type = LocationType.CITY,
            x = 990f, y = 720f,
            ownerFactionId = FactionId.STORMCLOAK,
            connections = listOf("fort_greenwall", "trevas_watch", "fort_dawnguard"),
            garrison = 80, maxGarrison = 140, fortificationLevel = 3,
            goldIncome = 60, oreIncome = 25, recruitIncome = 16
        ),
        LocationNode(
            id = "fort_dawnguard",
            nameRu = "Форт Стражи Рассвета (Ущелье)",
            holdRu = "Рифт",
            type = LocationType.STRONGHOLD,
            x = 1080f, y = 760f,
            ownerFactionId = FactionId.DAWNGUARD,
            connections = listOf("riften"),
            garrison = 65, maxGarrison = 120, fortificationLevel = 5,
            goldIncome = 35, oreIncome = 25, recruitIncome = 14
        )
    )
}
