package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [TranslationRecord::class, GlossaryEntry::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun translationDao(): TranslationDao
    abstract fun glossaryDao(): GlossaryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "game_translator_db"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialGlossary(database.glossaryDao())
                    }
                }
            }

            private suspend fun populateInitialGlossary(dao: GlossaryDao) {
                val defaults = listOf(
                    // Fantasy / Souls-like
                    GlossaryEntry(
                        gameTitle = "Elden Ring / Souls",
                        termEnglish = "Tarnished",
                        termThai = "ผู้มัวหมอง",
                        category = "CHARACTER",
                        note = "สรรพนามเรียกผู้เล่นใน Elden Ring"
                    ),
                    GlossaryEntry(
                        gameTitle = "Elden Ring / Souls",
                        termEnglish = "Grace",
                        termThai = "แสงพร",
                        category = "LOCATION",
                        note = "จุดพักฟื้นและเซฟ"
                    ),
                    GlossaryEntry(
                        gameTitle = "Elden Ring / Souls",
                        termEnglish = "Maidenless",
                        termThai = "ไร้ผู้ชี้นำพรหมจรรย์",
                        category = "CHARACTER",
                        note = "คำดูแคลนของ Varre"
                    ),
                    GlossaryEntry(
                        gameTitle = "Elden Ring / Souls",
                        termEnglish = "Elden Lord",
                        termThai = "จอมราชันเอลเดน",
                        category = "CHARACTER",
                        note = "เป้าหมายสูงสุดของผู้มัวหมอง"
                    ),
                    // Cyberpunk
                    GlossaryEntry(
                        gameTitle = "Cyberpunk 2077",
                        termEnglish = "Choom",
                        termThai = "เพื่อนเกลอ / สหาย",
                        category = "CHARACTER",
                        note = "สแลงสตรีทเรียกเพื่อนสนิท"
                    ),
                    GlossaryEntry(
                        gameTitle = "Cyberpunk 2077",
                        termEnglish = "Corpo",
                        termThai = "พวกบรรษัทนายทุน",
                        category = "FACTION",
                        note = "กลุ่มนายทุนผู้กุมอำนาจเมือง Night City"
                    ),
                    GlossaryEntry(
                        gameTitle = "Cyberpunk 2077",
                        termEnglish = "Ripperdoc",
                        termThai = "หมอใต้ดินตัดต่อไซเบอร์แวร์",
                        category = "CHARACTER",
                        note = "หมอผ่าตัดติดตั้งอวัยวะกลไก"
                    ),
                    GlossaryEntry(
                        gameTitle = "Cyberpunk 2077",
                        termEnglish = "Eddies",
                        termThai = "เหรียญเอ็ดดี้ (เงิน)",
                        category = "ITEM",
                        note = "Eurodollars เงินตราในเกม"
                    ),
                    // Genshin / JRPG
                    GlossaryEntry(
                        gameTitle = "Genshin / Anime RPG",
                        termEnglish = "Traveler",
                        termThai = "นักเดินทาง",
                        category = "CHARACTER",
                        note = "ตัวเอกผู้มาเยือนจากต่างโลก"
                    ),
                    GlossaryEntry(
                        gameTitle = "Genshin / Anime RPG",
                        termEnglish = "Archon",
                        termThai = "เทพเจ้าประจำเมือง",
                        category = "CHARACTER",
                        note = "เทพผู้ปกครองทั้งเจ็ด"
                    ),
                    GlossaryEntry(
                        gameTitle = "Genshin / Anime RPG",
                        termEnglish = "Vision",
                        termThai = "เนตรสวรรค์ / วิชัน",
                        category = "ITEM",
                        note = "ศิลาธาตุที่พระเจ้าประทานให้"
                    ),
                    // Baldur's Gate / D&D
                    GlossaryEntry(
                        gameTitle = "Baldur's Gate 3",
                        termEnglish = "Mind Flayer",
                        termThai = "มายด์เฟลเยอร์ (ผู้กลืนกินจิต)",
                        category = "CHARACTER",
                        note = "เผ่าพันธุ์อิลลิธิดหัวปลาหมึก"
                    ),
                    GlossaryEntry(
                        gameTitle = "Baldur's Gate 3",
                        termEnglish = "Tadpole",
                        termThai = "ตัวอ่อนปรสิต",
                        category = "ITEM",
                        note = "ปรสิตในสมองที่คอยครอบงำจิตใจ"
                    ),
                    GlossaryEntry(
                        gameTitle = "Baldur's Gate 3",
                        termEnglish = "Short Rest",
                        termThai = "พักผ่อนสั้น",
                        category = "SKILL",
                        note = "ฟื้นฟูพลังบางส่วน"
                    )
                )
                dao.insertAll(defaults)
            }
        }
    }
}
