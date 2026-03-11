package com.kana.watch

data class Kana(
    val character: String,
    val romaji: String,
    val type: KanaType
)

enum class KanaType { HIRAGANA, KATAKANA }

object KanaData {

    val hiragana = listOf(
        // Vowels
        Kana("あ", "a", KanaType.HIRAGANA),
        Kana("い", "i", KanaType.HIRAGANA),
        Kana("う", "u", KanaType.HIRAGANA),
        Kana("え", "e", KanaType.HIRAGANA),
        Kana("お", "o", KanaType.HIRAGANA),

        // K-row
        Kana("か", "ka", KanaType.HIRAGANA),
        Kana("き", "ki", KanaType.HIRAGANA),
        Kana("く", "ku", KanaType.HIRAGANA),
        Kana("け", "ke", KanaType.HIRAGANA),
        Kana("こ", "ko", KanaType.HIRAGANA),

        // S-row
        Kana("さ", "sa", KanaType.HIRAGANA),
        Kana("し", "shi", KanaType.HIRAGANA),
        Kana("す", "su", KanaType.HIRAGANA),
        Kana("せ", "se", KanaType.HIRAGANA),
        Kana("そ", "so", KanaType.HIRAGANA),

        // T-row
        Kana("た", "ta", KanaType.HIRAGANA),
        Kana("ち", "chi", KanaType.HIRAGANA),
        Kana("つ", "tsu", KanaType.HIRAGANA),
        Kana("て", "te", KanaType.HIRAGANA),
        Kana("と", "to", KanaType.HIRAGANA),

        // N-row
        Kana("な", "na", KanaType.HIRAGANA),
        Kana("に", "ni", KanaType.HIRAGANA),
        Kana("ぬ", "nu", KanaType.HIRAGANA),
        Kana("ね", "ne", KanaType.HIRAGANA),
        Kana("の", "no", KanaType.HIRAGANA),

        // H-row
        Kana("は", "ha", KanaType.HIRAGANA),
        Kana("ひ", "hi", KanaType.HIRAGANA),
        Kana("ふ", "fu", KanaType.HIRAGANA),
        Kana("へ", "he", KanaType.HIRAGANA),
        Kana("ほ", "ho", KanaType.HIRAGANA),

        // M-row
        Kana("ま", "ma", KanaType.HIRAGANA),
        Kana("み", "mi", KanaType.HIRAGANA),
        Kana("む", "mu", KanaType.HIRAGANA),
        Kana("め", "me", KanaType.HIRAGANA),
        Kana("も", "mo", KanaType.HIRAGANA),

        // Y-row
        Kana("や", "ya", KanaType.HIRAGANA),
        Kana("ゆ", "yu", KanaType.HIRAGANA),
        Kana("よ", "yo", KanaType.HIRAGANA),

        // R-row
        Kana("ら", "ra", KanaType.HIRAGANA),
        Kana("り", "ri", KanaType.HIRAGANA),
        Kana("る", "ru", KanaType.HIRAGANA),
        Kana("れ", "re", KanaType.HIRAGANA),
        Kana("ろ", "ro", KanaType.HIRAGANA),

        // W-row + N
        Kana("わ", "wa", KanaType.HIRAGANA),
        Kana("を", "wo", KanaType.HIRAGANA),
        Kana("ん", "n", KanaType.HIRAGANA),

        // Dakuten G-row
        Kana("が", "ga", KanaType.HIRAGANA),
        Kana("ぎ", "gi", KanaType.HIRAGANA),
        Kana("ぐ", "gu", KanaType.HIRAGANA),
        Kana("げ", "ge", KanaType.HIRAGANA),
        Kana("ご", "go", KanaType.HIRAGANA),

        // Dakuten Z-row
        Kana("ざ", "za", KanaType.HIRAGANA),
        Kana("じ", "ji", KanaType.HIRAGANA),
        Kana("ず", "zu", KanaType.HIRAGANA),
        Kana("ぜ", "ze", KanaType.HIRAGANA),
        Kana("ぞ", "zo", KanaType.HIRAGANA),

        // Dakuten D-row
        Kana("だ", "da", KanaType.HIRAGANA),
        Kana("ぢ", "di", KanaType.HIRAGANA),
        Kana("づ", "du", KanaType.HIRAGANA),
        Kana("で", "de", KanaType.HIRAGANA),
        Kana("ど", "do", KanaType.HIRAGANA),

        // Dakuten B-row
        Kana("ば", "ba", KanaType.HIRAGANA),
        Kana("び", "bi", KanaType.HIRAGANA),
        Kana("ぶ", "bu", KanaType.HIRAGANA),
        Kana("べ", "be", KanaType.HIRAGANA),
        Kana("ぼ", "bo", KanaType.HIRAGANA),

        // Handakuten P-row
        Kana("ぱ", "pa", KanaType.HIRAGANA),
        Kana("ぴ", "pi", KanaType.HIRAGANA),
        Kana("ぷ", "pu", KanaType.HIRAGANA),
        Kana("ぺ", "pe", KanaType.HIRAGANA),
        Kana("ぽ", "po", KanaType.HIRAGANA),
    )

    val katakana = listOf(
        // Vowels
        Kana("ア", "a", KanaType.KATAKANA),
        Kana("イ", "i", KanaType.KATAKANA),
        Kana("ウ", "u", KanaType.KATAKANA),
        Kana("エ", "e", KanaType.KATAKANA),
        Kana("オ", "o", KanaType.KATAKANA),

        // K-row
        Kana("カ", "ka", KanaType.KATAKANA),
        Kana("キ", "ki", KanaType.KATAKANA),
        Kana("ク", "ku", KanaType.KATAKANA),
        Kana("ケ", "ke", KanaType.KATAKANA),
        Kana("コ", "ko", KanaType.KATAKANA),

        // S-row
        Kana("サ", "sa", KanaType.KATAKANA),
        Kana("シ", "shi", KanaType.KATAKANA),
        Kana("ス", "su", KanaType.KATAKANA),
        Kana("セ", "se", KanaType.KATAKANA),
        Kana("ソ", "so", KanaType.KATAKANA),

        // T-row
        Kana("タ", "ta", KanaType.KATAKANA),
        Kana("チ", "chi", KanaType.KATAKANA),
        Kana("ツ", "tsu", KanaType.KATAKANA),
        Kana("テ", "te", KanaType.KATAKANA),
        Kana("ト", "to", KanaType.KATAKANA),

        // N-row
        Kana("ナ", "na", KanaType.KATAKANA),
        Kana("ニ", "ni", KanaType.KATAKANA),
        Kana("ヌ", "nu", KanaType.KATAKANA),
        Kana("ネ", "ne", KanaType.KATAKANA),
        Kana("ノ", "no", KanaType.KATAKANA),

        // H-row
        Kana("ハ", "ha", KanaType.KATAKANA),
        Kana("ヒ", "hi", KanaType.KATAKANA),
        Kana("フ", "fu", KanaType.KATAKANA),
        Kana("ヘ", "he", KanaType.KATAKANA),
        Kana("ホ", "ho", KanaType.KATAKANA),

        // M-row
        Kana("マ", "ma", KanaType.KATAKANA),
        Kana("ミ", "mi", KanaType.KATAKANA),
        Kana("ム", "mu", KanaType.KATAKANA),
        Kana("メ", "me", KanaType.KATAKANA),
        Kana("モ", "mo", KanaType.KATAKANA),

        // Y-row
        Kana("ヤ", "ya", KanaType.KATAKANA),
        Kana("ユ", "yu", KanaType.KATAKANA),
        Kana("ヨ", "yo", KanaType.KATAKANA),

        // R-row
        Kana("ラ", "ra", KanaType.KATAKANA),
        Kana("リ", "ri", KanaType.KATAKANA),
        Kana("ル", "ru", KanaType.KATAKANA),
        Kana("レ", "re", KanaType.KATAKANA),
        Kana("ロ", "ro", KanaType.KATAKANA),

        // W-row + N
        Kana("ワ", "wa", KanaType.KATAKANA),
        Kana("ヲ", "wo", KanaType.KATAKANA),
        Kana("ン", "n", KanaType.KATAKANA),

        // Dakuten G-row
        Kana("ガ", "ga", KanaType.KATAKANA),
        Kana("ギ", "gi", KanaType.KATAKANA),
        Kana("グ", "gu", KanaType.KATAKANA),
        Kana("ゲ", "ge", KanaType.KATAKANA),
        Kana("ゴ", "go", KanaType.KATAKANA),

        // Dakuten Z-row
        Kana("ザ", "za", KanaType.KATAKANA),
        Kana("ジ", "ji", KanaType.KATAKANA),
        Kana("ズ", "zu", KanaType.KATAKANA),
        Kana("ゼ", "ze", KanaType.KATAKANA),
        Kana("ゾ", "zo", KanaType.KATAKANA),

        // Dakuten D-row
        Kana("ダ", "da", KanaType.KATAKANA),
        Kana("ヂ", "di", KanaType.KATAKANA),
        Kana("ヅ", "du", KanaType.KATAKANA),
        Kana("デ", "de", KanaType.KATAKANA),
        Kana("ド", "do", KanaType.KATAKANA),

        // Dakuten B-row
        Kana("バ", "ba", KanaType.KATAKANA),
        Kana("ビ", "bi", KanaType.KATAKANA),
        Kana("ブ", "bu", KanaType.KATAKANA),
        Kana("ベ", "be", KanaType.KATAKANA),
        Kana("ボ", "bo", KanaType.KATAKANA),

        // Handakuten P-row
        Kana("パ", "pa", KanaType.KATAKANA),
        Kana("ピ", "pi", KanaType.KATAKANA),
        Kana("プ", "pu", KanaType.KATAKANA),
        Kana("ペ", "pe", KanaType.KATAKANA),
        Kana("ポ", "po", KanaType.KATAKANA),
    )

    val all = hiragana + katakana

    fun random(): Kana = all.random()
}
