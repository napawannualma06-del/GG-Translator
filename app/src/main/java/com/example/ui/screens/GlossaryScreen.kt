package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.GlossaryEntry
import com.example.ui.GameTranslatorViewModel
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.IndigoDeep
import com.example.ui.theme.IndigoNeon
import com.example.ui.theme.RoseNeon
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950

@Composable
fun GlossaryScreen(
    viewModel: GameTranslatorViewModel,
    modifier: Modifier = Modifier
) {
    val glossaryList by viewModel.glossaryList.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredList = glossaryList.filter { entry ->
        val matchesQuery = searchQuery.isBlank() ||
                entry.termEnglish.contains(searchQuery, ignoreCase = true) ||
                entry.termThai.contains(searchQuery, ignoreCase = true) ||
                (entry.note?.contains(searchQuery, ignoreCase = true) == true)

        val matchesCategory = selectedCategory == "ALL" || entry.category.equals(selectedCategory, ignoreCase = true)

        matchesQuery && matchesCategory
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Slate950,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = CyanNeon,
                contentColor = Slate950,
                modifier = Modifier.testTag("add_glossary_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Glossary Term")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(CyanGlow, IndigoDeep))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "คลังศัพท์เฉพาะทางและชื่อตัวละคร (${glossaryList.size} คำ)",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "คำศัพท์เหล่านี้จะถูกส่งให้ AI ใช้อ้างอิงขณะแปลเกมอัตโนมัติ",
                            color = Slate400,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("glossary_search_field"),
                placeholder = { Text("ค้นหาชื่อตัวละคร, ไอเทม, สกิล หรือคำแปล...", color = Slate400, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = CyanNeon)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Text("✕", color = Slate400)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Slate200,
                    focusedBorderColor = CyanNeon,
                    unfocusedBorderColor = Slate700,
                    focusedContainerColor = Slate900,
                    unfocusedContainerColor = Slate900
                ),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val categories = listOf(
                    "ALL" to "ทั้งหมด",
                    "CHARACTER" to "ตัวละคร",
                    "ITEM" to "ไอเทม",
                    "SKILL" to "สกิล/เวท",
                    "LOCATION" to "สถานที่",
                    "FACTION" to "ฝ่าย/กลุ่ม"
                )

                categories.forEach { (catKey, catLabel) ->
                    val isSelected = selectedCategory == catKey
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) CyanGlow else Slate800)
                            .border(1.dp, if (isSelected) CyanNeon else Slate700, RoundedCornerShape(10.dp))
                            .clickable { selectedCategory = catKey }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = catLabel,
                            color = if (isSelected) Color.White else Slate200,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Term List
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = null,
                            tint = Slate700,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "ไม่พบคลังคำศัพท์ที่ตรงกับการค้นหา",
                            color = Slate400,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { it.id }) { entry ->
                        GlossaryItemCard(
                            entry = entry,
                            onDelete = { viewModel.deleteGlossaryEntry(entry) }
                        )
                    }
                }
            }
        }
    }

    // Add Glossary Term Dialog
    if (showAddDialog) {
        AddGlossaryDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { en, th, cat, note ->
                viewModel.addGlossaryEntry(en, th, cat, note)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun GlossaryItemCard(
    entry: GlossaryEntry,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900.copy(alpha = 0.85f)),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(Slate700, Slate800)))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.termEnglish,
                        color = CyanNeon,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(IndigoDeep.copy(alpha = 0.5f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = entry.category,
                            color = IndigoNeon,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "➜ ${entry.termThai}",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                if (!entry.note.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "💡 ${entry.note}",
                        color = AmberGlow,
                        fontSize = 11.sp
                    )
                }

                Text(
                    text = "เกม: ${entry.gameTitle}",
                    color = Slate400,
                    fontSize = 10.sp
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete term",
                    tint = RoseNeon.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun AddGlossaryDialog(
    onDismiss: () -> Unit,
    onConfirm: (termEn: String, termTh: String, category: String, note: String?) -> Unit
) {
    var termEnglish by remember { mutableStateOf("") }
    var termThai by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("CHARACTER") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate900,
        title = {
            Text("เพิ่มคำศัพท์ / ชื่อตัวละครใหม่", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = termEnglish,
                    onValueChange = { termEnglish = it },
                    label = { Text("คำภาษาอังกฤษในเกม (เช่น Tarnished, Choom)", color = Slate400) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Slate200,
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = Slate700
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = termThai,
                    onValueChange = { termThai = it },
                    label = { Text("คำแปลภาษาไทยที่ต้องการ (เช่น ผู้มัวหมอง)", color = Slate400) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Slate200,
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = Slate700
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Category selector
                Text("หมวดหมู่คำศัพท์:", color = Slate400, fontSize = 12.sp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("CHARACTER", "ITEM", "SKILL", "LOCATION", "FACTION").forEach { cat ->
                        val isSel = category == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) CyanGlow else Slate800)
                                .clickable { category = cat }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(cat, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("คำอธิบายบริบทเพิ่มเติม (ไม่บังคับ)", color = Slate400) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Slate200,
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = Slate700
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (termEnglish.isNotBlank() && termThai.isNotBlank()) {
                        onConfirm(termEnglish, termThai, category, note.ifBlank { null })
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanGlow),
                enabled = termEnglish.isNotBlank() && termThai.isNotBlank()
            ) {
                Text("บันทึก", color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("ยกเลิก", color = Slate400)
            }
        }
    )
}
