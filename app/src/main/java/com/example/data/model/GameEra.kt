package com.example.data.model

enum class GameEra(
    val titleThai: String,
    val titleEnglish: String,
    val description: String,
    val samplePronoun: String,
    val toneInstruction: String
) {
    FANTASY_MEDIEVAL(
        titleThai = "ดาร์กแฟนตาซี / ยุคกลาง",
        titleEnglish = "Dark Fantasy / Medieval",
        description = "ใช้ภาษาโบราณ สรรพนาม เจ้า/ข้า/ท่าน/ใต้เท้า ศัพท์เวทมนตร์และตำนาน",
        samplePronoun = "ข้า - เจ้า / ท่าน",
        toneInstruction = "Use archaic, medieval, and high fantasy Thai vocabulary. Refer to royalty or gods with honorifics (ท่าน/ใต้เท้า), use 'ข้า' and 'เจ้า' for dialogue, and mythical terminology for magic and weapons."
    ),
    CYBERPUNK_SCIFI(
        titleThai = "ไซเบอร์พังก์ / อนาคต",
        titleEnglish = "Cyberpunk / Sci-Fi",
        description = "สไตล์ล้ำยุค สแลงสตรีท ศัพท์เทคโนโลยี คอร์ปอเรชัน ห้าวหาญ ตรงไปตรงมา",
        samplePronoun = "ฉัน/กู - นาย/มึง/พวกเรา",
        toneInstruction = "Use futuristic sci-fi terminology, cyberpunk street slang, and tech jargons. Dialogue should sound sharp, gritty, and modern with street-smart Thai phrasing."
    ),
    MODERN_MILITARY(
        titleThai = "สงคราม / ยุทธวิธี",
        titleEnglish = "Modern / Tactical Shooter",
        description = "คำสั่งทหาร สื่อสารกระชับ ฉับไว แม่นยำ รหัสวิทยุและพิกัด",
        samplePronoun = "ผม/ผู้หมวด - คุณ/เป้าหมาย",
        toneInstruction = "Use concise military tactics and comms terminology (วิทยุสื่อสาร, ประชิด, เข้าที่กำบัง). Keep instructions punchy, urgent, and professional."
    ),
    ANIME_JRPG(
        titleThai = "อนิเมะ JRPG / ผจญภัย",
        titleEnglish = "Anime JRPG / Adventure",
        description = "สำนวนสดใส มีพลัง อบอุ่น เป็นธรรมชาติ อารมณ์การผจญภัยกับเพื่อนพ้อง",
        samplePronoun = "ฉัน - นาย / เธอ / ทุกคน",
        toneInstruction = "Translate in vibrant Japanese anime/manga Thai localization style. Friendly, emotional, enthusiastic, with natural conversational flow."
    ),
    WUXIA_CHINESE(
        titleThai = "กำลังภายใน / จอมยุทธ",
        titleEnglish = "Wuxia / Eastern Fantasy",
        description = "สำนัก เคล็ดวิชา ปราณแท้ ศิษย์พี่ ศิษย์น้อง ผู้เยาว์ ผู้อาวุโส",
        samplePronoun = "ผู้น้อย/ศิษย์น้อง - ท่านจอมยุทธ/อาจารย์",
        toneInstruction = "Use classic Thai Wuxia martial arts novel diction (ศิษย์พี่, ท่านจอมยุทธ, ลมปราณ, เคล็ดวิชา). Eloquent, poetic, and respectful."
    ),
    SURVIVAL_HORROR(
        titleThai = "สยองขวัญ / เอาชีวิตรอด",
        titleEnglish = "Survival / Psychological Horror",
        description = "กดดัน ตึงเครียด อารมณ์สิ้นหวัง ข้อความลึกลับและบันทึกเปื้อนเลือด",
        samplePronoun = "ฉัน - แก / ใครน่ะ",
        toneInstruction = "Convey dread, panic, suspense, and fragmented psychological horror. Keep phrasing tense and eerie."
    ),
    RETRO_PIXEL_GBA(
        titleThai = "เกมเรโทร / GBA / พิกเซล",
        titleEnglish = "Retro 8/16-bit Pixel / GBA",
        description = "สำหรับเกม Game Boy Advance, SNES, NDS ฟอนต์บิตแมปความละเอียดต่ำ",
        samplePronoun = "ฉัน / เรา - นาย / เธอ / เจ้า",
        toneInstruction = "CRITICAL FOR GBA/RETRO: The image is from a low-resolution handheld retro console (e.g. Game Boy Advance, SNES, NDS) using 8x8 or 16x16 bitmap pixel fonts. Carefully disambiguate pixelated characters (e.g. 'rn' vs 'm', 'I' vs 'l' vs '1', 'c' vs 'e', 'O' vs '0'). Reconstruct missing/broken pixel strokes using English grammar and gaming dialogue context. Translate into classic Thai retro game localization style."
    )
}

data class CharacterPronounConfig(
    val protagonistSelf: String = "ข้า",
    val protagonistToOther: String = "เจ้า",
    val toneStyle: String = "ตามบทบาทตัวละคร"
)
