package com.example.data.model

enum class ShoutId(
    val words: String,
    val nameRu: String,
    val descriptionRu: String,
    val cooldownSeconds: Int,
    val voiceAudioTone: Float // Base frequency for synthesizer
) {
    FUS_RO_DAH(
        words = "FUS RO DAH",
        nameRu = "Безжалостная сила",
        descriptionRu = "Мощная звуковая волна сметает ряды врага: наносит 40 урона и снижает мораль осаждающих на 35%.",
        cooldownSeconds = 15,
        voiceAudioTone = 120f
    ),
    YOL_TOOR_SHUL(
        words = "YOL TOOR SHUL",
        nameRu = "Огненное дыхание",
        descriptionRu = "Поток первородного драконьего пламени: наносит 85 чистого урона передовым отрядам врага.",
        cooldownSeconds = 25,
        voiceAudioTone = 200f
    ),
    TIID_KLO_UL(
        words = "TIID KLO UL",
        nameRu = "Замедление времени",
        descriptionRu = "Время замирает: скорость и урон союзников удваиваются на 10 секунд.",
        cooldownSeconds = 35,
        voiceAudioTone = 160f
    ),
    ODAHVIING(
        words = "OD AH VIING",
        nameRu = "Зов Дракона",
        descriptionRu = "Призыв красного дракона Одавинга: пикирует на поле боя, нанося 160 сокрушительного урона.",
        cooldownSeconds = 60,
        voiceAudioTone = 90f
    )
}

data class DovahkiinState(
    val currentLocationId: String = "whiterun",
    val unlockedShouts: Set<ShoutId> = setOf(ShoutId.FUS_RO_DAH, ShoutId.YOL_TOOR_SHUL),
    val dragonSouls: Int = 3,
    val level: Int = 1,
    val activeBuffTimeSeconds: Int = 0
)
