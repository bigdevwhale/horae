package app.horae.domain

/** Ready-made rules to start from; every hour and step stays editable afterwards. */
enum class Preset { BENEDICT, STOIC, ZEN, HESYCHAST, METTA, FIVE, BLANK }

object Presets {
    private fun q(bank: Bank) = Step(Kind.QUESTION, mode = bank.name)
    private fun breath(b: Breath, min: Int) = Step(Kind.BREATH, min = min, mode = b.name)
    private fun read(vararg s: Source) = Step(Kind.READ, mode = s.joinToString(",") { it.name })

    fun hours(p: Preset, ru: Boolean): List<Hour> {
        fun t(r: String, e: String) = if (ru) r else e
        val list = when (p) {
            Preset.BENEDICT -> listOf(
                Hour(name = t("Полунощница", "Vigils"), pos = -3.0, bell = Bell.NONE, days = 0b1000000,
                    steps = listOf(read(Source.PSALM), Step(Kind.SILENCE, min = 5, mode = "FACE"))),
                Hour(name = t("Хвалитны", "Lauds"), pos = 0.0, bell = Bell.GREAT,
                    steps = listOf(Step(Kind.THANKS, n = 1), breath(Breath.EVEN, 3), read(Source.PSALM)),
                    brief = listOf(Step(Kind.THANKS, n = 1))),
                Hour(name = t("Третий час", "Terce"), pos = 3.0, bell = Bell.HAND, strokes = 0,
                    steps = listOf(q(Bank.WORK)), brief = listOf(q(Bank.WORK))),
                Hour(name = t("Шестой час", "Sext"), pos = 6.0, bell = Bell.HAND, strokes = 0,
                    steps = listOf(q(Bank.NOON), breath(Breath.BOX, 2)), brief = listOf(q(Bank.NOON))),
                Hour(name = t("Девятый час", "None"), pos = 9.0, bell = Bell.HAND, strokes = 0,
                    steps = listOf(read(Source.DESERT, Source.KEMPIS)), brief = listOf(breath(Breath.CALM, 1))),
                Hour(name = t("Вечерня", "Vespers"), pos = 11.5, bell = Bell.GREAT,
                    steps = listOf(Step(Kind.GAZE, min = 3, mode = "CANDLE"), read(Source.PSALM), Step(Kind.THANKS, n = 3)),
                    brief = listOf(Step(Kind.THANKS, n = 1))),
                Hour(name = t("Повечерие", "Compline"), pos = 13.5, bell = Bell.SMALL,
                    steps = listOf(Step(Kind.EXAMEN, mode = Examen.IGNATIUS.name), Step(Kind.SILENCE, min = 2, mode = "FREE")),
                    brief = listOf(Step(Kind.EXAMEN, mode = Examen.FRANKLIN.name))),
            )
            Preset.STOIC -> listOf(
                Hour(name = t("Утреннее размышление", "Morning reflection"), pos = 0.5, bell = Bell.SMALL,
                    steps = listOf(read(Source.STOIC), Step(Kind.QUESTION, text = t("Что сегодня может пойти не так — и как это встретить?", "What could go wrong today, and how will you meet it?"))),
                    brief = listOf(q(Bank.MORNING))),
                Hour(name = t("Полдень", "Noon"), pos = 6.0, bell = Bell.NONE,
                    steps = listOf(Step(Kind.QUESTION, text = t("Что сейчас в моей власти, а что нет?", "What right now is up to me, and what is not?")))),
                Hour(name = t("Вечерний отчёт", "Evening account"), pos = 14.0, bell = Bell.SMALL,
                    steps = listOf(Step(Kind.EXAMEN, mode = Examen.SENECA.name), read(Source.STOIC)),
                    brief = listOf(Step(Kind.EXAMEN, mode = Examen.FRANKLIN.name))),
            )
            Preset.ZEN -> listOf(
                Hour(name = t("Утренний дзадзэн", "Morning zazen"), pos = 0.0, bell = Bell.WOOD, strokes = 7,
                    steps = listOf(Step(Kind.SIT, min = 20, mode = Sit.BREATH.name, every = 10), Step(Kind.WALK, min = 5, n = 4), Step(Kind.BOWS, n = 3, mode = "BOW")),
                    brief = listOf(Step(Kind.SIT, min = 5, mode = Sit.BREATH.name))),
                Hour(name = t("Полдень", "Midday"), pos = 6.0, bell = Bell.BOWL, strokes = 1,
                    steps = listOf(Step(Kind.SIT, min = 10, mode = Sit.NOTING.name))),
                Hour(name = t("Вечерний дзадзэн", "Evening zazen"), pos = 13.0, bell = Bell.WOOD, strokes = 7,
                    steps = listOf(Step(Kind.SIT, min = 20, mode = Sit.OPEN.name), read(Source.ZEN, Source.TAO)),
                    brief = listOf(Step(Kind.SIT, min = 5, mode = Sit.OPEN.name))),
            )
            Preset.HESYCHAST -> listOf(
                Hour(name = t("Утреннее правило", "Morning rule"), pos = 0.0, bell = Bell.GREAT,
                    steps = listOf(Step(Kind.BOWS, n = 12, mode = "BOW"), Step(Kind.BEADS, n = 100, every = 25, mode = Beads.LESTOVKA.name,
                        text = t("Господи Иисусе Христе, помилуй мя", "Lord Jesus Christ, have mercy on me")), read(Source.PSALM)),
                    brief = listOf(Step(Kind.BEADS, n = 33, mode = Beads.LESTOVKA.name, every = 0, text = t("Господи, помилуй", "Lord, have mercy")))),
                Hour(name = t("Полуденная молитва", "Midday prayer"), pos = 6.0, bell = Bell.HAND,
                    steps = listOf(Step(Kind.BEADS, n = 33, mode = Beads.LESTOVKA.name, text = t("Господи Иисусе Христе, помилуй мя", "Lord Jesus Christ, have mercy on me")))),
                Hour(name = t("Вечернее правило", "Evening rule"), pos = 13.0, bell = Bell.GREAT,
                    steps = listOf(read(Source.DESERT), Step(Kind.BEADS, n = 100, every = 25, mode = Beads.LESTOVKA.name,
                        text = t("Господи Иисусе Христе, помилуй мя", "Lord Jesus Christ, have mercy on me")), Step(Kind.EXAMEN, mode = Examen.IGNATIUS.name)),
                    brief = listOf(Step(Kind.BEADS, n = 33, mode = Beads.LESTOVKA.name, text = t("Господи, помилуй", "Lord, have mercy")))),
            )
            Preset.METTA -> listOf(
                Hour(name = t("Утро", "Morning"), pos = 0.5, bell = Bell.BOWL, strokes = 3,
                    steps = listOf(breath(Breath.CALM, 2), Step(Kind.METTA, min = 10, list = Metta.phrases(ru))),
                    brief = listOf(Step(Kind.METTA, min = 2, list = Metta.phrases(ru)))),
                Hour(name = t("Полдень", "Noon"), pos = 6.0, bell = Bell.BOWL, strokes = 1,
                    steps = listOf(breath(Breath.EVEN, 3))),
                Hour(name = t("Вечер", "Evening"), pos = 13.0, bell = Bell.BOWL, strokes = 3,
                    steps = listOf(Step(Kind.THANKS, n = 3), Step(Kind.METTA, min = 10, list = Metta.phrases(ru)), read(Source.DHAMMA)),
                    brief = listOf(Step(Kind.THANKS, n = 1))),
            )
            Preset.FIVE -> listOf(
                Hour(name = t("Утро", "Morning"), anchor = Anchor.CLOCK, clock = 8 * 60, bell = Bell.SMALL, strokes = 1,
                    steps = listOf(breath(Breath.BOX, 1), q(Bank.MORNING))),
                Hour(name = t("День", "Midday"), pos = 6.0, bell = Bell.HAND, strokes = 1,
                    steps = listOf(breath(Breath.CALM, 1))),
                Hour(name = t("Вечер", "Evening"), anchor = Anchor.CLOCK, clock = 22 * 60, bell = Bell.SMALL, strokes = 1,
                    steps = listOf(Step(Kind.THANKS, n = 3))),
            )
            Preset.BLANK -> listOf(
                Hour(name = t("Повечерие", "Compline"), pos = 13.5, bell = Bell.SMALL, steps = listOf(q(Bank.EVENING))),
            )
        }
        return list.mapIndexed { i, h -> h.copy(ord = i) }
    }
}
