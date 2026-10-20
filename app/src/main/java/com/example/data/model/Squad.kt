package com.example.data.model

enum class UnitClass(
    val titleRu: String,
    val speed: Float, // Higher means faster movement across map
    val baseAttack: Float,
    val baseDefense: Float,
    val costGold: Int,
    val costOre: Int,
    val costRecruits: Int,
    val descriptionRu: String
) {
    RECON("Разведчики", 1.8f, 18f, 12f, 80, 20, 10, "Быстрый легкий отряд для захвата незащищенных шахт и разведки."),
    INFANTRY("Линейная пехота", 1.0f, 32f, 30f, 140, 50, 20, "Основная ударная сила. Формируют стену щитов."),
    ARCHERS("Стрелки / Арбалетчики", 1.1f, 38f, 18f, 150, 40, 15, "Наносят урон до рукопашной с минимальными потерями."),
    MAGES("Боевые маги", 0.9f, 50f, 22f, 230, 80, 12, "Школы Разрушения и Восстановления. Пробивают укрепления."),
    ELITE("Элитные хускарлы", 0.95f, 65f, 55f, 320, 140, 25, "Тяжеловооруженные ветераны с несокрушимым боевым духом."),
    DOVAHKIIN_GUARD("Гвардия Довакина", 1.3f, 95f, 80f, 400, 200, 30, "Флагманский отряд под предводительством Драконорожденного.")
}

data class Squad(
    val id: String,
    val factionId: FactionId,
    val nameRu: String,
    val unitClass: UnitClass,
    val count: Int,
    val maxCount: Int,
    val morale: Float = 100f, // 0..100
    val fromLocationId: String,
    val targetLocationId: String,
    val progress: Float = 0f, // 0f..1f (interpolation between from and target)
    val isPlayerSquad: Boolean = false,
    val isDovahkiinSquad: Boolean = false
) {
    fun currentAttackPower(tierBonus: Float): Float {
        val moraleMod = (morale / 100f).coerceIn(0.4f, 1.3f)
        return unitClass.baseAttack * (count.toFloat() / unitClass.costRecruits.coerceAtLeast(1)) * tierBonus * moraleMod
    }

    fun currentDefensePower(tierBonus: Float): Float {
        val moraleMod = (morale / 100f).coerceIn(0.4f, 1.3f)
        return unitClass.baseDefense * (count.toFloat() / unitClass.costRecruits.coerceAtLeast(1)) * tierBonus * moraleMod
    }
}
