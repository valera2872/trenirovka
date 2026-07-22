package com.valera2872.bjjarm;

import java.util.Locale;

/** Discipline-specific wording and examples used by the 0.8.0 sport mode. */
public final class SportGuidance {
    private SportGuidance() { }

    public static String slug(String sport) {
        String value = sport == null ? "" : sport.toLowerCase(Locale.ROOT);
        if (value.contains("дзюдо")) return "judo";
        if (value.contains("вольн")) return "freestyle";
        if (value.contains("греко")) return "greco";
        if (value.contains("бразиль")) return "bjj";
        return "grappling";
    }

    public static String title(String sport) {
        switch (slug(sport)) {
            case "judo": return "Дзюдо";
            case "freestyle": return "Вольная борьба";
            case "greco": return "Греко-римская борьба";
            case "bjj": return "Бразильское джиу-джитсу";
            default: return "Грэпплинг / No-Gi";
        }
    }

    public static String description(String sport) {
        switch (slug(sport)) {
            case "judo":
                return "Захват, выведение из равновесия, вход в бросок и продолжение в партере.";
            case "freestyle":
                return "Стойка, подготовка атаки, проход, повторная атака и защита от контратаки.";
            case "greco":
                return "Контроль корпуса, подготовка броска, работа в клинче и продолжение в партере.";
            case "bjj":
                return "Позиция, контроль, переход, основная атака и восстановление после потери позиции.";
            default:
                return "Позиция, вход, основная атака, запасной вариант и возврат после ошибки.";
        }
    }

    public static String[] stepLabels(String sport) {
        switch (slug(sport)) {
            case "judo":
                return new String[]{
                        "Стойка и рабочий захват",
                        "Подготовка и выведение из равновесия",
                        "Основной бросок",
                        "Продолжение после защиты",
                        "Переход в партер или возврат в стойку"
                };
            case "freestyle":
                return new String[]{
                        "Стойка и первый контакт",
                        "Подготовка атаки",
                        "Основной проход или бросок",
                        "Повторная атака",
                        "Защита и возврат в свою стойку"
                };
            case "greco":
                return new String[]{
                        "Стойка и контроль корпуса",
                        "Подготовка броска",
                        "Основной бросок или перевод",
                        "Продолжение при защите соперника",
                        "Работа в партере или возврат в стойку"
                };
            case "bjj":
                return new String[]{
                        "Стартовая позиция",
                        "Контроль и переход",
                        "Основная атака",
                        "Запасная атака или смена позиции",
                        "Восстановление после потери позиции"
                };
            default:
                return new String[]{
                        "Стартовая позиция",
                        "Вход в нужную позицию",
                        "Основное продолжение",
                        "Запасной вариант",
                        "Возврат после потери позиции"
                };
        }
    }

    public static String[] hints(String sport) {
        switch (slug(sport)) {
            case "judo":
                return new String[]{
                        "Например: правая стойка, захват рукава и отворота",
                        "Например: шаг в сторону, тяга рукава, kuzushi вперёд-вправо",
                        "Например: учи-мата",
                        "Например: соперник отставляет ногу — перейти на о-учи-гари",
                        "Например: удержание сбоку или безопасно подняться в стойку"
                };
            case "freestyle":
                return new String[]{
                        "Например: низкая стойка, контроль головы и запястья",
                        "Например: рывок за голову и смена уровня",
                        "Например: проход в две ноги",
                        "Например: перейти на одну ногу или зайти за спину",
                        "Например: спролл, убрать руки соперника и вернуть стойку"
                };
            case "greco":
                return new String[]{
                        "Например: лоб в висок, андерхук и контроль локтя",
                        "Например: вытянуть соперника вверх и сместить его вес",
                        "Например: бросок прогибом или перевод за туловище",
                        "Например: перейти на другой захват или вытолкнуть из зоны",
                        "Например: удержать верх, выполнить накат или вернуться в стойку"
                };
            case "bjj":
                return new String[]{
                        "Например: закрытый гард",
                        "Например: контроль руки и переход в Williams Guard",
                        "Например: треугольник",
                        "Например: омоплата или свип",
                        "Например: рама, колено внутрь и восстановление гарда"
                };
            default:
                return new String[]{
                        "Например: seated guard или контроль сверху",
                        "Например: arm drag и выход за спину",
                        "Например: удушение со спины",
                        "Например: перейти на рычаг локтя или удержать позицию",
                        "Например: закрыть шею, поставить рамы и восстановить гард"
                };
        }
    }

    public static String[] focusPoints(String sport) {
        switch (slug(sport)) {
            case "judo":
                return new String[]{"качество захвата", "kuzushi до входа", "скорость входа", "переход в партер"};
            case "freestyle":
                return new String[]{"стойка и дистанция", "смена уровня", "завершение прохода", "повторная атака"};
            case "greco":
                return new String[]{"контроль корпуса", "баланс в клинче", "взрывной вход", "работа в партере"};
            case "bjj":
                return new String[]{"удержание позиции", "контроль захватов", "связки атак", "восстановление гарда"};
            default:
                return new String[]{"первый контакт", "выход в свою позицию", "связка атак", "безопасный возврат"};
        }
    }
}
