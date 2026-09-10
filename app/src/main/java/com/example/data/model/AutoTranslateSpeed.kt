package com.example.data.model

enum class AutoTranslateSpeed(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val stabilityMs: Long,
    val terminalMs: Long,
    val pollMs: Long
) {
    ULTRA_FAST(
        id = "ultra_fast",
        title = "เร็วทันใจสุดๆ",
        subtitle = "100ms",
        description = "ตรวจจับและส่งแปลทันที ตอบสนองไวที่สุด เหมาะกับคนเล่นเร็ว",
        stabilityMs = 120L,
        terminalMs = 60L,
        pollMs = 100L
    ),
    FAST(
        id = "fast",
        title = "เร็ว (แนะนำ)",
        subtitle = "200ms",
        description = "ความเร็วสูง ลื่นไหล ไม่ต้องรอนาน รอตัวอักษรพิมพ์เสร็จแล้วแปลทันที",
        stabilityMs = 220L,
        terminalMs = 100L,
        pollMs = 140L
    ),
    NORMAL(
        id = "normal",
        title = "ปานกลาง",
        subtitle = "380ms",
        description = "ความเร็วสมดุล รอให้ประโยคพิมพ์จนจบสมบูรณ์ก่อนแปล",
        stabilityMs = 380L,
        terminalMs = 200L,
        pollMs = 180L
    ),
    STABLE(
        id = "stable",
        title = "นิ่งสนิท",
        subtitle = "650ms",
        description = "รอนิ่งสนิท สำหรับเกมที่ตัวอักษรค่อยๆ พิมพ์ช้ามากๆ",
        stabilityMs = 650L,
        terminalMs = 350L,
        pollMs = 220L
    );

    companion object {
        fun fromId(id: String?): AutoTranslateSpeed =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: FAST
    }
}
