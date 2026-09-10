package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TranslationProvider
import com.example.ui.GameTranslatorViewModel
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldGlow
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
fun GameContextSettingsScreen(
    viewModel: GameTranslatorViewModel,
    modifier: Modifier = Modifier
) {
    val gameTitle by viewModel.gameTitle.collectAsStateWithLifecycle()
    val geminiApiKey by viewModel.geminiApiKey.collectAsStateWithLifecycle()
    val deepseekApiKey by viewModel.deepseekApiKey.collectAsStateWithLifecycle()
    val selectedProvider by viewModel.selectedProvider.collectAsStateWithLifecycle()
    val selectedGeminiModel by viewModel.selectedGeminiModel.collectAsStateWithLifecycle()
    val selectedDeepseekModel by viewModel.selectedDeepseekModel.collectAsStateWithLifecycle()
    val geminiModelsList by viewModel.geminiModelsList.collectAsStateWithLifecycle()
    val deepseekModelsList by viewModel.deepseekModelsList.collectAsStateWithLifecycle()
    val isFetchingModels by viewModel.isFetchingModels.collectAsStateWithLifecycle()
    val fetchModelMessage by viewModel.fetchModelMessage.collectAsStateWithLifecycle()
    val pronounConfig by viewModel.pronounConfig.collectAsStateWithLifecycle()
    val customPromptNotes by viewModel.customPromptNotes.collectAsStateWithLifecycle()
    val overlayOpacity by viewModel.overlayOpacity.collectAsStateWithLifecycle()
    val overlayFontSize by viewModel.overlayFontSize.collectAsStateWithLifecycle()
    val isPixelEnhanceEnabled by viewModel.isPixelEnhanceEnabled.collectAsStateWithLifecycle()

    var tempGameTitle by remember(gameTitle) { mutableStateOf(gameTitle) }
    var tempGeminiKey by remember(geminiApiKey) { mutableStateOf(geminiApiKey) }
    var tempDeepseekKey by remember(deepseekApiKey) { mutableStateOf(deepseekApiKey) }
    var isGeminiKeyVisible by remember { mutableStateOf(false) }
    var isDeepseekKeyVisible by remember { mutableStateOf(false) }

    var selfPronoun by remember(pronounConfig) { mutableStateOf(pronounConfig.protagonistSelf) }
    var otherPronoun by remember(pronounConfig) { mutableStateOf(pronounConfig.protagonistToOther) }
    var toneStyle by remember(pronounConfig) { mutableStateOf(pronounConfig.toneStyle) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(CyanNeon, IndigoDeep))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsEsports,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "ตั้งค่าเกมและ API Key",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ใส่ชื่อเกมและเลือกโมเดล AI (Gemini หรือ DeepSeek) เพื่อแปลให้เข้ากับเกม",
                        color = Slate400,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Section 1: Game Title Input (Replaces Era / Style Selection)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SportsEsports,
                        contentDescription = null,
                        tint = CyanNeon,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ชื่อเกมที่กำลังเล่น (Game Name)",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "เขียนชื่อเกมลงในช่องนี้ เพื่อให้ AI รู้จักเนื้อเรื่อง สไตล์ภาษา และบริบทของเกมนั้นๆ โดยตรง เช่น Elden Ring, Pokémon, Cyberpunk 2077, Genshin Impact",
                    color = Slate400,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                OutlinedTextField(
                    value = tempGameTitle,
                    onValueChange = {
                        tempGameTitle = it
                        viewModel.setGameTitle(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("game_title_input_field"),
                    label = { Text("ชื่อเกม (เช่น Elden Ring, Persona 5, Skyrim)") },
                    placeholder = { Text("ใส่ชื่อเกม...") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Slate200,
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = Slate700,
                        focusedLabelColor = CyanNeon
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // Quick presets buttons for popular games
                Text(
                    text = "ตัวอย่างเกมด่วน:",
                    color = Slate400,
                    fontSize = 11.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Elden Ring", "Pokémon", "Cyberpunk 2077", "Genshin").forEach { preset ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (tempGameTitle == preset) CyanGlow else Slate800)
                                .clickable {
                                    tempGameTitle = preset
                                    viewModel.setGameTitle(preset)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = preset,
                                color = if (tempGameTitle == preset) Color.White else Slate200,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Section 2: AI Provider & API Key Input (Gemini & DeepSeek)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = AmberGlow,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "เลือกโมเดลและใส่ API Key (AI Providers)",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "เลือกผู้ให้บริการ AI ที่ต้องการใช้งาน และใส่ API Key ของท่าน สามารถสลับใช้งานระหว่าง Gemini หรือ DeepSeek ได้อย่างอิสระ",
                    color = Slate400,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                // Provider Selector Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Gemini Option
                    val isGemini = selectedProvider == TranslationProvider.GEMINI
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isGemini) Slate800 else Slate950)
                            .border(
                                width = if (isGemini) 1.5.dp else 1.dp,
                                color = if (isGemini) CyanNeon else Slate700,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.setSelectedProvider(TranslationProvider.GEMINI) }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (isGemini) CyanNeon else Slate700)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Google Gemini",
                                    color = if (isGemini) CyanNeon else Slate200,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = selectedGeminiModel,
                                color = Slate400,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // DeepSeek Option
                    val isDeepSeek = selectedProvider == TranslationProvider.DEEPSEEK
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDeepSeek) Slate800 else Slate950)
                            .border(
                                width = if (isDeepSeek) 1.5.dp else 1.dp,
                                color = if (isDeepSeek) IndigoNeon else Slate700,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.setSelectedProvider(TranslationProvider.DEEPSEEK) }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (isDeepSeek) IndigoNeon else Slate700)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "DeepSeek",
                                    color = if (isDeepSeek) IndigoNeon else Slate200,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = selectedDeepseekModel,
                                color = Slate400,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // Gemini API Key Input
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Gemini API Key:",
                            color = CyanNeon,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (geminiApiKey.isNotBlank()) {
                            Text("✓ บันทึกแล้ว", color = EmeraldGlow, fontSize = 11.sp)
                        }
                    }

                    OutlinedTextField(
                        value = tempGeminiKey,
                        onValueChange = {
                            tempGeminiKey = it
                            viewModel.setGeminiApiKey(it)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gemini_api_key_input"),
                        placeholder = { Text("ใส่ Gemini API Key (AIzaSy...)") },
                        singleLine = true,
                        visualTransformation = if (isGeminiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isGeminiKeyVisible = !isGeminiKeyVisible }) {
                                Icon(
                                    imageVector = if (isGeminiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle key visibility",
                                    tint = Slate400
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Slate200,
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = Slate700,
                            focusedLabelColor = CyanNeon
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // DeepSeek API Key Input
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DeepSeek API Key:",
                            color = IndigoNeon,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (deepseekApiKey.isNotBlank()) {
                            Text("✓ บันทึกแล้ว", color = EmeraldGlow, fontSize = 11.sp)
                        }
                    }

                    OutlinedTextField(
                        value = tempDeepseekKey,
                        onValueChange = {
                            tempDeepseekKey = it
                            viewModel.setDeepseekApiKey(it)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("deepseek_api_key_input"),
                        placeholder = { Text("ใส่ DeepSeek API Key (sk-...)") },
                        singleLine = true,
                        visualTransformation = if (isDeepseekKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isDeepseekKeyVisible = !isDeepseekKeyVisible }) {
                                Icon(
                                    imageVector = if (isDeepseekKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle key visibility",
                                    tint = Slate400
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Slate200,
                            focusedBorderColor = IndigoNeon,
                            unfocusedBorderColor = Slate700,
                            focusedLabelColor = IndigoNeon
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Section 2.1: Model Selection & Fetcher
                val isGeminiSelected = selectedProvider == TranslationProvider.GEMINI
                val activeThemeColor = if (isGeminiSelected) CyanNeon else IndigoNeon
                val currentModel = if (isGeminiSelected) selectedGeminiModel else selectedDeepseekModel
                val availableModels = if (isGeminiSelected) geminiModelsList else deepseekModelsList

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Slate950)
                        .border(1.dp, Slate800, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isGeminiSelected) "โมเดล Gemini ที่ใช้งาน:" else "โมเดล DeepSeek ที่ใช้งาน:",
                                color = Slate400,
                                fontSize = 11.sp
                            )
                            Text(
                                text = currentModel,
                                color = activeThemeColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { viewModel.fetchModelsForProvider(selectedProvider) },
                            enabled = !isFetchingModels,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isGeminiSelected) CyanGlow else IndigoDeep,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            if (isFetchingModels) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("กำลังดึง...", fontSize = 11.sp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = "ดึงโมเดล",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ดึงโมเดล", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (fetchModelMessage != null) {
                        Text(
                            text = fetchModelMessage ?: "",
                            color = if (fetchModelMessage?.contains("สำเร็จ") == true) EmeraldGlow else AmberGlow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = "เลือกรุ่นโมเดลที่ต้องการใช้งาน (เน้นความเร็ว & ประหยัด Token):",
                        color = Slate200,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Scrollable list of models
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableModels.forEach { modelName ->
                            val isModelSelected = modelName == currentModel
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isModelSelected) activeThemeColor.copy(alpha = 0.2f) else Slate900)
                                    .border(
                                        width = if (isModelSelected) 1.5.dp else 1.dp,
                                        color = if (isModelSelected) activeThemeColor else Slate700,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        if (isGeminiSelected) {
                                            viewModel.setSelectedGeminiModel(modelName)
                                        } else {
                                            viewModel.setSelectedDeepseekModel(modelName)
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = modelName,
                                    color = if (isModelSelected) activeThemeColor else Slate200,
                                    fontSize = 11.sp,
                                    fontWeight = if (isModelSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // Token-Saver & High Speed Information Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(EmeraldGlow.copy(alpha = 0.1f))
                        .border(1.dp, EmeraldGlow.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = EmeraldGlow,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "โหมด OCR + AI ข้อความ (ประหยัด Token สูงสุด)",
                                color = EmeraldGlow,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "ระบบอ่านตัวหนังสือบนหน้าจอด้วย On-Device OCR แล้วส่งเฉพาะข้อความเข้า AI แปลภาษา (ไม่ส่งรูปภาพ) ประหยัด Token มากกว่า 95% และแปลเร็วมากใน ~150-200ms",
                                color = Slate200,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }

        // Section 3: Character Pronouns & Tone Customization
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = IndigoNeon,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "สรรพนามและบทบาทตัวละคร (Character Pronouns)",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Self Pronoun
                Text(
                    text = "สรรพนามแทนตนเองของตัวเอก:",
                    color = Slate400,
                    fontSize = 12.sp
                )
                OutlinedTextField(
                    value = selfPronoun,
                    onValueChange = {
                        selfPronoun = it
                        viewModel.updatePronouns(it, otherPronoun, toneStyle)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("เช่น ข้า, ฉัน, เรา, ผม, ผู้น้อย") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Slate200,
                        focusedBorderColor = IndigoNeon,
                        unfocusedBorderColor = Slate700
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // Other Pronoun
                Text(
                    text = "สรรพนามเรียกอีกฝ่าย / คู่สนทนา:",
                    color = Slate400,
                    fontSize = 12.sp
                )
                OutlinedTextField(
                    value = otherPronoun,
                    onValueChange = {
                        otherPronoun = it
                        viewModel.updatePronouns(selfPronoun, it, toneStyle)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("เช่น เจ้า, นาย, เธอ, ท่าน, มึง, แก") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Slate200,
                        focusedBorderColor = IndigoNeon,
                        unfocusedBorderColor = Slate700
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // Tone Style
                Text(
                    text = "สไตล์น้ำเสียง / บุคลิก:",
                    color = Slate400,
                    fontSize = 12.sp
                )
                OutlinedTextField(
                    value = toneStyle,
                    onValueChange = {
                        toneStyle = it
                        viewModel.updatePronouns(selfPronoun, otherPronoun, it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("เช่น สำนวนโบราณ ดุดัน, สดใส อบอุ่น, ยุทธวิธีกระชับ") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Slate200,
                        focusedBorderColor = IndigoNeon,
                        unfocusedBorderColor = Slate700
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // Section 4: Floating Overlay Customization (Opacity & Font Size)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "⚙️ ปรับแต่งหน้าต่างซ้อนทับ (Overlay Display)",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                // Opacity Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Opacity, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ความโปร่งใสพื้นหลัง (Opacity)", color = Slate200, fontSize = 12.sp)
                        }
                        Text("${(overlayOpacity * 100).toInt()}%", color = CyanNeon, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = overlayOpacity,
                        onValueChange = { viewModel.setOverlayOpacity(it) },
                        valueRange = 0.4f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = CyanNeon,
                            activeTrackColor = CyanGlow,
                            inactiveTrackColor = Slate700
                        )
                    )
                }

                // Font Size Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.FormatSize, contentDescription = null, tint = IndigoNeon, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ขนาดตัวอักษรคำแปล (Font Size)", color = Slate200, fontSize = 12.sp)
                        }
                        Text("${overlayFontSize} sp", color = IndigoNeon, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = overlayFontSize.toFloat(),
                        onValueChange = { viewModel.setOverlayFontSize(it.toInt()) },
                        valueRange = 12f..24f,
                        steps = 11,
                        colors = SliderDefaults.colors(
                            thumbColor = IndigoNeon,
                            activeTrackColor = IndigoDeep,
                            inactiveTrackColor = Slate700
                        )
                    )
                }
            }
        }

        // Section 5: GBA & Retro Pixel Art Sharpening Engine
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isPixelEnhanceEnabled) AmberGlow.copy(alpha = 0.2f) else Slate800),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "GBA",
                                color = if (isPixelEnhanceEnabled) AmberGlow else Slate400,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "เพิ่มความคมชัดเกม GBA / บิตแมป 8x8",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Nearest-Neighbor 4x + ปรับคอนทราสต์ตัวอักษร",
                                color = Slate400,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Switch(
                        checked = isPixelEnhanceEnabled,
                        onCheckedChange = { viewModel.setPixelEnhanceEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AmberGlow,
                            uncheckedThumbColor = Slate400,
                            uncheckedTrackColor = Slate800
                        )
                    )
                }
                Text(
                    text = "แก้ปัญหาฟอนต์บิตแมปความละเอียดต่ำในเกม Game Boy Advance, SNES, NDS ที่ตัวอักษรเบลอหรือขาดตอน เพื่อให้ AI อ่านข้อความได้แม่นยำสูงสุด",
                    color = Slate200,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}
