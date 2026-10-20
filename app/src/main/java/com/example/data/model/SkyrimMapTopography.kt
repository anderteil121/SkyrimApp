package com.example.data.model

import androidx.compose.ui.geometry.Offset

data class MountainPeak(val center: Offset, val radius: Float, val label: String? = null)
data class LakeArea(val center: Offset, val radiusX: Float, val radiusY: Float, val name: String)
data class RiverSegment(val points: List<Offset>)

object SkyrimMapTopography {
    // 9 Holds with central region indicators
    val holdCenters = mapOf(
        "Хаафингар" to Offset(240f, 210f),
        "Хьялмарк" to Offset(350f, 310f),
        "Белый Берег" to Offset(530f, 250f),
        "Винтерхолд" to Offset(740f, 200f),
        "Предел" to Offset(160f, 490f),
        "Вайтран" to Offset(470f, 530f),
        "Истмарк" to Offset(740f, 470f),
        "Фолкрит" to Offset(440f, 760f),
        "Рифт" to Offset(780f, 720f)
    )

    // Famous Mountain Peaks
    val mountainPeaks = listOf(
        MountainPeak(Offset(560f, 610f), 45f, "Глотка Мира (7000 Ступеней)"),
        MountainPeak(Offset(120f, 380f), 30f, "Горы Друадах"),
        MountainPeak(Offset(140f, 680f), 35f, "Пик Красного Орла"),
        MountainPeak(Offset(450f, 860f), 40f, "Хребет Джерол"),
        MountainPeak(Offset(600f, 850f), 35f, "Южный перевал"),
        MountainPeak(Offset(880f, 450f), 38f, "Горы Велоти"),
        MountainPeak(Offset(850f, 280f), 30f, "Восточные утесы"),
        MountainPeak(Offset(380f, 440f), 25f, "Холмы Вайтрана"),
        MountainPeak(Offset(640f, 360f), 28f, "Скалистый гребень")
    )

    // Major Lakes of Skyrim
    val lakes = listOf(
        LakeArea(Offset(430f, 660f), 55f, 30f, "Озеро Иллиналта"),
        LakeArea(Offset(820f, 800f), 65f, 35f, "Озеро Хонрих"),
        LakeArea(Offset(700f, 640f), 35f, 20f, "Гейзерные топи Истмарка")
    )

    // Rivers of Skyrim
    val rivers = listOf(
        // White River (from Lake Ilinalta past Riverwood, Whiterun, to Windhelm & sea)
        RiverSegment(
            listOf(
                Offset(470f, 650f),
                Offset(485f, 620f), // Riverwood
                Offset(520f, 560f),
                Offset(580f, 530f),
                Offset(670f, 500f), // Near Fort Amol
                Offset(750f, 390f), // Windhelm bridge
                Offset(800f, 340f),
                Offset(850f, 300f)  // To Sea of Ghosts
            )
        ),
        // Karth River (through The Reach)
        RiverSegment(
            listOf(
                Offset(140f, 610f),
                Offset(170f, 540f), // Kolskeggr
                Offset(190f, 430f), // Karthwasten
                Offset(230f, 300f), // Dragon Bridge
                Offset(260f, 230f), // Near Solitude
                Offset(270f, 150f)  // Sea of Ghosts
            )
        ),
        // Treva River to Lake Honrich
        RiverSegment(
            listOf(
                Offset(620f, 680f), // Ivarstead
                Offset(700f, 720f), // Treva's Watch
                Offset(780f, 780f)  // Lake Honrich / Riften
            )
        )
    )
}
