package com.example.data.model

enum class BattleTactic(val titleRu: String, val descriptionRu: String) {
    BALANCED("Обычный строй", "Стандартное ведение боя с балансом атаки и защиты"),
    SHIELD_WALL("Стена щитов", "+40% к стойкости, снижает потери пехоты на треть"),
    ARCHER_VOLLEY("Залп лучников", "Концентрированный обстрел стрелами и арбалетами"),
    FLANK_ATTACK("Фланговый обход", "Удар во фланг: высокий урон, шанс сломить мораль"),
    MAGIC_SURGE("Магический шквал", "Вспышки огня и молний, игнорирующие частоколы"),
    ALL_OUT_CHARGE("Общий штурм", "Яростная атака всеми силами: максимальный урон!"),
    RETREAT("Отступление", "Организованный отход к ближайшей безопасной цитадели")
}

data class ParticipatingSquadInfo(
    val nameRu: String,
    val count: Int,
    val unitClass: UnitClass,
    val isDovahkiin: Boolean = false,
    val power: Float = 0f
)

data class Battle(
    val id: String,
    val locationId: String,
    val attackerFactionId: FactionId,
    val defenderFactionId: FactionId,
    val attackerStrength: Float,
    val defenderStrength: Float,
    val maxAttackerStrength: Float,
    val maxDefenderStrength: Float,
    val selectedTactic: BattleTactic = BattleTactic.BALANCED,
    val log: List<String> = emptyList(),
    val isDovahkiinPresent: Boolean = false,
    val isResolved: Boolean = false,
    val winnerFactionId: FactionId? = null,
    val attackerSquads: List<ParticipatingSquadInfo> = emptyList(),
    val defenderSquads: List<ParticipatingSquadInfo> = emptyList(),
    val totalAttackerSoldiers: Int = 0,
    val totalDefenderSoldiers: Int = 0
) {
    fun isPlayerInvolved(playerFactionId: FactionId): Boolean =
        attackerFactionId == playerFactionId || defenderFactionId == playerFactionId
}
