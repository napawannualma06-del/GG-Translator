package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.GameTranslatorViewModel
import com.example.ui.components.FloatingBubbleSimulator
import com.example.ui.components.TranslationOverlayHud
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

import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.SystemUpdate

@Composable
fun LiveTranslatorScreen(
    viewModel: GameTranslatorViewModel,
    modifier: Modifier = Modifier,
    onRequestScreenCapture: () -> Unit = {}
) {
    val context = LocalContext.current

    val currentEra by viewModel.currentEra.collectAsStateWithLifecycle()
    val pronounConfig by viewModel.pronounConfig.collectAsStateWithLifecycle()
    val isOverlayRunning by viewModel.isOverlayRunning.collectAsStateWithLifecycle()
    val isTranslating by viewModel.isTranslating.collectAsStateWithLifecycle()
    val lastResult by viewModel.lastResult.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val overlayOpacity by viewModel.overlayOpacity.collectAsStateWithLifecycle()
    val overlayFontSize by viewModel.overlayFontSize.collectAsStateWithLifecycle()
    val isPixelEnhanceEnabled by viewModel.isPixelEnhanceEnabled.collectAsStateWithLifecycle()
    val gameTitle by viewModel.gameTitle.collectAsStateWithLifecycle()
    val selectedProvider by viewModel.selectedProvider.collectAsStateWithLifecycle()
    val selectedSceneIndex by viewModel.selectedPresetIndex.collectAsStateWithLifecycle()
    val customBitmap by viewModel.customBitmap.collectAsStateWithLifecycle()
    val isScreenCaptureReady by viewModel.isScreenCaptureReady.collectAsStateWithLifecycle()
    val hasTargetFrame by viewModel.hasTargetFrame.collectAsStateWithLifecycle()
    val isAutoTranslateEnabled by viewModel.isAutoTranslateEnabled.collectAsStateWithLifecycle()

    var isOverlayCardVisible by remember { mutableStateOf(true) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.loadCustomImage(context, it) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // System Overlay Floating Bubble Toggle Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("floating_bubble_toggle_card")
                .border(1.dp, if (isOverlayRunning) CyanNeon else Slate700, RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isOverlayRunning) Slate900 else Slate900.copy(alpha = 0.8f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
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
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isOverlayRunning)
                                        Brush.radialGradient(listOf(CyanNeon, IndigoDeep))
                                    else
                                        Brush.radialGradient(listOf(Slate700, Slate800))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = "Floating Bubble Status",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "ปุ่มบับเบิลลอยบนจอเกม",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isOverlayRunning) EmeraldGlow.copy(alpha = 0.2f) else Slate700)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isOverlayRunning) "พร้อมใช้งาน" else "ปิดอยู่",
                                        color = if (isOverlayRunning) EmeraldGlow else Slate400,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = if (isOverlayRunning)
                                    "แตะเพื่อแปล • กดค้างเพื่อเปิดกรอบเล็งข้อความ"
                                else
                                    "เปิดเพื่อแสดงปุ่มบับเบิลลอยเหนือเกม",
                                color = Slate400,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Switch(
                        checked = isOverlayRunning,
                        onCheckedChange = { viewModel.toggleOverlayService(context, onRequestScreenCapture) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = CyanNeon,
                            uncheckedThumbColor = Slate400,
                            uncheckedTrackColor = Slate800
                        ),
                        modifier = Modifier.testTag("floating_bubble_switch")
                    )
                }

                // Screen Capture & Aiming Frame Status inside the Bubble Card
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Slate950.copy(alpha = 0.6f))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isScreenCaptureReady) Icons.Default.CheckCircle else Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = if (isScreenCaptureReady) EmeraldGlow else AmberGlow,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isScreenCaptureReady)
                                "จับภาพหน้าจอ: พร้อมทำงาน"
                            else
                                "ยังไม่ได้เปิดสิทธิ์จับภาพหน้าจอเกม",
                            color = if (isScreenCaptureReady) EmeraldGlow else AmberGlow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (!isScreenCaptureReady) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AmberGlow.copy(alpha = 0.2f))
                                .clickable { onRequestScreenCapture() }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "เปิดสิทธิ์บันทึกจอ",
                                color = AmberGlow,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 🎯 Aiming Frame (กรอบเล็งจับข้อความ) Feature Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AmberGlow.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Slate900.copy(alpha = 0.85f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AmberGlow.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CenterFocusStrong,
                            contentDescription = "Aiming Frame",
                            tint = AmberGlow,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🎯 วิธีใช้กรอบเล็งข้อความเกม",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (hasTargetFrame != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(EmeraldGlow.copy(alpha = 0.2f))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "ล็อคแล้ว",
                                        color = EmeraldGlow,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Text(
                            text = "กดค้างที่ปุ่มบับเบิล (Long-Press) เพื่อเปิดกรอบเล็ง แล้วลากไปครอบบทสนทนาในเกม",
                            color = Slate400,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Slate950)
                            .border(1.dp, Slate800, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text(
                                text = "⚡ แตะ 1 ครั้ง",
                                color = CyanNeon,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "แปลข้อความจุดที่เล็งไว้ทันที",
                                color = Slate400,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Slate950)
                            .border(1.dp, Slate800, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text(
                                text = "💬 แปลสะอาดตา",
                                color = EmeraldGlow,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "แสดงเฉพาะบทสนทนาไทย ไม่รกจอ",
                                color = Slate400,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Slate950)
                            .border(1.dp, Slate800, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text(
                                text = "📐 ย่อขยายอิสระ",
                                color = AmberGlow,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "ลากมุม ◢ ปรับขนาดย่อขยายได้อิสระ",
                                color = Slate400,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        // ⚡ Auto-Translate (แปลอัตโนมัติเมื่อเปลี่ยนบทสนทนา) Switch Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("auto_translate_switch_card")
                .border(
                    1.dp,
                    if (isAutoTranslateEnabled) EmeraldGlow.copy(alpha = 0.8f) else Slate700,
                    RoundedCornerShape(16.dp)
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isAutoTranslateEnabled) Slate900 else Slate900.copy(alpha = 0.6f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
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
                                .background(
                                    if (isAutoTranslateEnabled) EmeraldGlow.copy(alpha = 0.2f)
                                    else Slate800
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Auto Translate",
                                tint = if (isAutoTranslateEnabled) EmeraldGlow else Slate400,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "แปลอัตโนมัติ (Auto-Translate)",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (isAutoTranslateEnabled) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(EmeraldGlow.copy(alpha = 0.2f))
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "ตรวจจับเฟรม",
                                            color = EmeraldGlow,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "ตรวจจับและแปลอัตโนมัติเมื่อมีบทสนทนาใหม่ ถ้าเป็นคำเดิมในกรอบจะไม่แปลซ้ำเพื่อประหยัดโควต้า",
                                color = Slate400,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Switch(
                        checked = isAutoTranslateEnabled,
                        onCheckedChange = { viewModel.setAutoTranslateEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = EmeraldGlow,
                            uncheckedThumbColor = Slate400,
                            uncheckedTrackColor = Slate800
                        ),
                        modifier = Modifier.testTag("auto_translate_switch")
                    )
                }
            }
        }

        // Low-Resolution GBA / Pixel Font Sharpen & Upscale Switch
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pixel_enhance_switch_card")
                .border(
                    1.dp,
                    if (isPixelEnhanceEnabled) AmberGlow.copy(alpha = 0.8f) else Slate700,
                    RoundedCornerShape(16.dp)
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isPixelEnhanceEnabled) Slate900 else Slate900.copy(alpha = 0.6f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
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
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "เร่งความคมชัดเกม GBA / พิกเซล",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (isPixelEnhanceEnabled) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(AmberGlow.copy(alpha = 0.2f))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "Upscale 4x",
                                        color = AmberGlow,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Text(
                            text = if (isPixelEnhanceEnabled)
                                "เปิดอยู่: ขยายภาพ Nearest-Neighbor 4x + ดึงคอนทราสต์ตัวอักษร 8x8 ให้อ่านชัดเจน"
                            else
                                "ปิดอยู่: ส่งภาพความละเอียดดั้งเดิม",
                            color = if (isPixelEnhanceEnabled) Slate200 else Slate400,
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
                    ),
                    modifier = Modifier.testTag("pixel_enhance_switch")
                )
            }
        }

        // Current Active AI Provider & Game Name Badge
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900.copy(alpha = 0.85f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (selectedProvider.id == "gemini") CyanNeon else IndigoNeon)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI: ${selectedProvider.displayName}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = "เกม: $gameTitle",
                    color = AmberGlow,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // In-App Update Button & Version Information Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("app_update_card")
                .border(1.dp, CyanNeon.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900.copy(alpha = 0.9f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CyanNeon.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = "อัพเดทแอพ",
                            tint = CyanNeon,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "อัพเดทเวอร์ชันแอพ",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(EmeraldGlow.copy(alpha = 0.2f))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "v1.1 ล่าสุด",
                                    color = EmeraldGlow,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "ตรวจสอบการอัพเดท APK ใหม่และโหลดข้อมูลล่าสุด",
                            color = Slate400,
                            fontSize = 11.sp
                        )
                    }
                }

                Button(
                    onClick = {
                        viewModel.translateCurrentScreen()
                        Toast.makeText(context, "แอปพลิเคชันเป็นเวอร์ชันล่าสุด (v1.1 พร้อมระบบแปลงเสถียร)", Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier.testTag("check_update_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanGlow,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("อัพเดท", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Error message banner if any
        if (errorMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = RoseNeon.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = RoseNeon,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { viewModel.clearError() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Text("✕", color = RoseNeon)
                    }
                }
            }
        }

        // Game Preset Scene Selector Chips
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🎯 เลือกจำลองเกม / สไตล์ยุคสมัย",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${viewModel.presetScenes.size} ฉากทดสอบ",
                    color = Slate400,
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                viewModel.presetScenes.forEachIndexed { index, scene ->
                    val isSelected = selectedSceneIndex == index && customBitmap == null
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected)
                                    Brush.horizontalGradient(listOf(CyanGlow, IndigoDeep))
                                else
                                    Brush.linearGradient(listOf(Slate900, Slate800))
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) CyanNeon else Slate700,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.selectPreset(index) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Column {
                            Text(
                                text = scene.title,
                                color = if (isSelected) Color.White else Slate200,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            Text(
                                text = scene.era.titleThai,
                                color = if (isSelected) AmberGlow else Slate400,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        // Active Context Banner (Era + Pronoun Settings Active)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900.copy(alpha = 0.9f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = AmberGlow,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "เกม: $gameTitle",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "สรรพนาม: ${pronounConfig.protagonistSelf} - ${pronounConfig.protagonistToOther} | ${pronounConfig.toneStyle}",
                            color = Slate400,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Game Screen Canvas & Interactive Simulator
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("game_screen_canvas_card")
                .border(1.5.dp, CyanNeon.copy(alpha = 0.6f), RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            ) {
                // Game Scene Graphic Display
                if (customBitmap != null) {
                    Image(
                        bitmap = customBitmap!!.asImageBitmap(),
                        contentDescription = "Custom Game Screenshot",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Synthetic game scene drawing
                    SyntheticGameView(
                        scene = viewModel.presetScenes[selectedSceneIndex],
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Scanning Radar Animation during translation
                if (isTranslating) {
                    ScanningRadarOverlay()
                }

                // Floating Bubble inside simulator (Draggable)
                FloatingBubbleSimulator(
                    onClick = {
                        isOverlayCardVisible = !isOverlayCardVisible
                        if (isOverlayCardVisible && lastResult == null) {
                            viewModel.translateCurrentScreen()
                        }
                    },
                    modifier = Modifier.align(Alignment.TopStart)
                )

                // Quick Floating Hint on Top
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .border(1.dp, CyanNeon.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "👆 ลากหรือแตะปุ่มบับเบิลสีฟ้าเพื่อแปลหน้าจอทันที",
                        color = CyanNeon,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Action Buttons Row: One-tap Capture & Upload Screenshot
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    isOverlayCardVisible = true
                    viewModel.translateCurrentScreen()
                },
                enabled = !isTranslating,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("translate_screen_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanGlow,
                    contentColor = Color.White
                )
            ) {
                if (isTranslating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("กำลังแปลด้วย AI...", fontSize = 13.sp)
                } else {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("จับภาพ & แปลเดี๋ยวนี้", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            OutlinedButton(
                onClick = { photoPickerLauncher.launch("image/*") },
                modifier = Modifier
                    .height(48.dp)
                    .testTag("upload_screenshot_button"),
                shape = RoundedCornerShape(14.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(Slate700, Slate700))),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate200)
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Slate400
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("เลือกรูปเกม", fontSize = 12.sp)
            }
        }

        // Live Translated Overlay Result HUD
        lastResult?.let { payload ->
            TranslationOverlayHud(
                isVisible = isOverlayCardVisible,
                translations = payload.translations,
                gameTitle = payload.gameTitleEstimate ?: viewModel.gameTitle.value,
                sceneContext = payload.sceneContext,
                opacity = overlayOpacity,
                fontSizeSp = overlayFontSize,
                onDismiss = { isOverlayCardVisible = false },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun SyntheticGameView(
    scene: com.example.ui.GamePresetScene,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                when (scene.era) {
                    com.example.data.model.GameEra.RETRO_PIXEL_GBA -> Brush.verticalGradient(listOf(Color(0xFF1E3A2B), Color(0xFF0F1E16)))
                    com.example.data.model.GameEra.FANTASY_MEDIEVAL -> Brush.verticalGradient(listOf(Color(0xFF130E1F), Color(0xFF231535)))
                    com.example.data.model.GameEra.CYBERPUNK_SCIFI -> Brush.verticalGradient(listOf(Color(0xFF090D1A), Color(0xFF151633)))
                    com.example.data.model.GameEra.ANIME_JRPG -> Brush.verticalGradient(listOf(Color(0xFF1B2E3D), Color(0xFF0E1A24)))
                    com.example.data.model.GameEra.MODERN_MILITARY -> Brush.verticalGradient(listOf(Color(0xFF111713), Color(0xFF1C241F)))
                    com.example.data.model.GameEra.WUXIA_CHINESE -> Brush.verticalGradient(listOf(Color(0xFF261D1A), Color(0xFF171210)))
                    com.example.data.model.GameEra.SURVIVAL_HORROR -> Brush.verticalGradient(listOf(Color(0xFF0D0D0E), Color(0xFF1C1314)))
                }
            )
            .padding(12.dp)
    ) {
        // Top HUD Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "🎮 ${scene.title}",
                    color = CyanNeon,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "FPS 60 • 4K HDR",
                    color = EmeraldGlow,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Bottom Dialogue Box (Simulating in-game text)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xE60F172A))
                .border(1.5.dp, CyanNeon.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                .padding(10.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(CyanGlow)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = scene.speaker,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "[${scene.category}]",
                        color = Slate400,
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = scene.sampleTextEnglish,
                    color = Color.White,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun ScanningRadarOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val scanY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_sweep"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyanNeon.copy(alpha = 0.08f))
    ) {
        // Glowing horizontal scan laser line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.TopCenter)
                    .padding(top = (scanY * 180).dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, CyanNeon, Color.White, CyanNeon, Color.Transparent)
                        )
                    )
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.85f))
                .border(1.dp, CyanNeon, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = CyanNeon,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI กำลังสแกนหน้าจอและวิเคราะห์บริบท...",
                    color = CyanNeon,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
