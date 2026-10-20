package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

enum class FactionId {
    IMPERIAL,
    STORMCLOAK,
    THALMOR,
    DAWNGUARD,
    VOLKIHAR,
    FORSWORN,
    BANDITS
}

enum class TechTier(val titleRu: String, val attackBonus: Float, val defenseBonus: Float, val upgradeCostGold: Int, val upgradeCostOre: Int) {
    IRON("Железное снаряжение", 1.0f, 1.0f, 0, 0),
    STEEL("Стальные доспехи и мечи", 1.25f, 1.2f, 350, 150),
    ELVEN_ORCISH("Эльфийская и орочья сталь", 1.55f, 1.45f, 800, 350),
    EBONY_GLASS("Эбонит и лунный камень", 1.9f, 1.8f, 1600, 750),
    DRAGONBONE("Драконье вооружение", 2.4f, 2.3f, 3000, 1500);

    fun nextTier(): TechTier? = when (this) {
        IRON -> STEEL
        STEEL -> ELVEN_ORCISH
        ELVEN_ORCISH -> EBONY_GLASS
        EBONY_GLASS -> DRAGONBONE
        DRAGONBONE -> null
    }
}

data class Faction(
    val id: FactionId,
    val nameRu: String,
    val leaderRu: String,
    val capitalRu: String,
    val mottoRu: String,
    val descriptionRu: String,
    val primaryColor: Color,
    val darkColor: Color,
    val specialBonusRu: String,
    val baseGoldPerTurn: Int,
    val baseOrePerTurn: Int,
    val baseRecruitsPerTurn: Int
)

object FactionsData {
    val allFactions = listOf(
        Faction(
            id = FactionId.IMPERIAL,
            nameRu = "Имперский легион",
            leaderRu = "Генерал Туллий",
            capitalRu = "Солитьюд",
            mottoRu = "За Империю и Императора!",
            descriptionRu = "Регулярная профессиональная армия Тамриэля. Непревзойденная организация, тяжелые когорты легионеров и постоянное финансирование из Сиродила.",
            primaryColor = ImperialRed,
            darkColor = ImperialRedDark,
            specialBonusRu = "+30% к защите фортов, регулярное золото из казны Сиродила",
            baseGoldPerTurn = 45,
            baseOrePerTurn = 20,
            baseRecruitsPerTurn = 12
        ),
        Faction(
            id = FactionId.STORMCLOAK,
            nameRu = "Братья Бури",
            leaderRu = "Ярл Ульфрик Буревестник",
            capitalRu = "Виндхельм",
            mottoRu = "Скайрим для нордов!",
            descriptionRu = "Гордые норды, поднявшие восстание за свободу веры в Талоса и независимость родины. Отличаются свирепостью в бою и высоким боевым духом.",
            primaryColor = StormcloakBlue,
            darkColor = StormcloakBlueDark,
            specialBonusRu = "+25% к урону в атаке, удешевленный найм нордов-ополченцев",
            baseGoldPerTurn = 30,
            baseOrePerTurn = 25,
            baseRecruitsPerTurn = 18
        ),
        Faction(
            id = FactionId.THALMOR,
            nameRu = "Талмор (Альдмерский Доминион)",
            leaderRu = "Первый эмиссар Эленвен",
            capitalRu = "Посольство / Драконий Мост",
            mottoRu = "Власть высших эльфов абсолютна.",
            descriptionRu = "Надменные высшие эльфы, следящие за исполнением Конкордата Белого Золота. Могущественная школа магии и превосходная экипировка юстициаров.",
            primaryColor = ThalmorGold,
            darkColor = ThalmorGoldDark,
            specialBonusRu = "Боевые маги пробивают 50% брони врага, огромный стартовый запас золота",
            baseGoldPerTurn = 60,
            baseOrePerTurn = 15,
            baseRecruitsPerTurn = 10
        ),
        Faction(
            id = FactionId.DAWNGUARD,
            nameRu = "Стража Рассвета",
            leaderRu = "Изран",
            capitalRu = "Форт Стражи Рассвета",
            mottoRu = "Когда солнце заходит — встаем мы.",
            descriptionRu = "Орден непримиримых охотников на вампиров и чудовищ. Используют тяжелые арбалеты, солнечные чары и тренированных бронированных троллей.",
            primaryColor = DawnguardAmber,
            darkColor = DawnguardAmberDark,
            specialBonusRu = "Арбалетчики наносят удвоенный урон на дистанции, иммунитет к ночным засадам",
            baseGoldPerTurn = 35,
            baseOrePerTurn = 30,
            baseRecruitsPerTurn = 12
        ),
        Faction(
            id = FactionId.VOLKIHAR,
            nameRu = "Клан Волкихар",
            leaderRu = "Лорд Харкон",
            capitalRu = "Замок Волкихар",
            mottoRu = "Тьма поглотит свет солнца.",
            descriptionRu = "Древний чистокровный вампирский клан. Невидимые ночные рейды, призыв горгулий и магия похищения жизненной силы врагов.",
            primaryColor = VolkiharBlood,
            darkColor = VolkiharPurple,
            specialBonusRu = "Ночью +40% к силе штурма, способность обращать павших врагов",
            baseGoldPerTurn = 35,
            baseOrePerTurn = 15,
            baseRecruitsPerTurn = 14
        ),
        Faction(
            id = FactionId.FORSWORN,
            nameRu = "Изгои Предела",
            leaderRu = "Маданах (Король в лохмотьях)",
            capitalRu = "Оплот Друадах",
            mottoRu = "Предел принадлежит нам по праву крови!",
            descriptionRu = "Коренные жители Предела, поклоняющиеся древним духам. Опасные партизаны с костяным оружием и вересковыми сердцами.",
            primaryColor = ForswornGreen,
            darkColor = ForswornGreenDark,
            specialBonusRu = "Мгновенные засады на караваны и тракты Предела, дешевые бойцы",
            baseGoldPerTurn = 25,
            baseOrePerTurn = 35,
            baseRecruitsPerTurn = 20
        ),
        Faction(
            id = FactionId.BANDITS,
            nameRu = "Вольные шайки разбойников",
            leaderRu = "Атаман Железная Рука",
            capitalRu = "Башня Светотень",
            mottoRu = "Кошелек или жизнь!",
            descriptionRu = "Разрозненные, но многочисленные банды мародеров и дезертиров, терроризирующие тракты, шахты и одинокие заставы Скайрима.",
            primaryColor = BanditGray,
            darkColor = BanditGrayDark,
            specialBonusRu = "+50% золота за разграбление шахт и караванов",
            baseGoldPerTurn = 40,
            baseOrePerTurn = 20,
            baseRecruitsPerTurn = 16
        )
    )

    fun getFaction(id: FactionId): Faction {
        return allFactions.firstOrNull { it.id == id } ?: allFactions[0]
    }
}
