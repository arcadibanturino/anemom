package com.kelompok.anemom.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kelompok.anemom.ui.theme.CardBorder
import com.kelompok.anemom.ui.theme.GradientEnd
import com.kelompok.anemom.ui.theme.GradientStart
import com.kelompok.anemom.ui.theme.Maroon
import com.kelompok.anemom.ui.theme.MutedText
import com.kelompok.anemom.ui.theme.Pink500

/** Tombol bulat putih dengan panah kembali (sesuai desain Figma). */
@Composable
fun BackCircleButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(40.dp)
            .shadow(2.dp, CircleShape)
            .clip(CircleShape)
            .background(Color.White)
            .border(1.dp, CardBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.ChevronLeft,
            contentDescription = "Kembali",
            tint = Maroon
        )
    }
}

/** Header halaman: tombol kembali + judul + ikon hati di kanan. */
@Composable
fun AnemomTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BackCircleButton(onClick = onBack)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = Maroon
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText
                )
            }
        }
        Icon(
            imageVector = Icons.Outlined.FavoriteBorder,
            contentDescription = null,
            tint = Pink500
        )
    }
}

/**
 * Field teks bergaya Anemom: label di atas, sudut membulat, ikon di kiri.
 * - [isPassword] menampilkan tombol lihat/sembunyikan kata sandi.
 * - [onClick] + [readOnly] dipakai untuk field yang membuka dialog/menu (mis. tanggal).
 */
@Composable
fun AnemomTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null,
    isPassword: Boolean = false,
    readOnly: Boolean = false,
    onClick: (() -> Unit)? = null,
    errorText: String? = null
) {
    var passwordVisible by remember { mutableStateOf(false) }

    val leading: (@Composable () -> Unit)? = leadingIcon?.let { icon ->
        { Icon(imageVector = icon, contentDescription = null) }
    }
    val passwordToggle: @Composable () -> Unit = {
        IconButton(onClick = { passwordVisible = !passwordVisible }) {
            Icon(
                imageVector = if (passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                contentDescription = if (passwordVisible) "Sembunyikan kata sandi" else "Tampilkan kata sandi"
            )
        }
    }
    val iconTrailing: (@Composable () -> Unit)? = trailingIcon?.let { icon ->
        { Icon(imageVector = icon, contentDescription = null) }
    }
    val supporting: (@Composable () -> Unit)? = errorText?.let { message ->
        { Text(message) }
    }

    Column(modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Maroon
        )
        Spacer(Modifier.height(6.dp))
        Box {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                readOnly = readOnly,
                singleLine = true,
                isError = errorText != null,
                placeholder = { Text(placeholder, color = MutedText.copy(alpha = 0.7f)) },
                leadingIcon = leading,
                trailingIcon = if (isPassword) passwordToggle else iconTrailing,
                supportingText = supporting,
                visualTransformation = if (isPassword && !passwordVisible) {
                    PasswordVisualTransformation()
                } else {
                    VisualTransformation.None
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
                    imeAction = imeAction
                ),
                keyboardActions = KeyboardActions(onDone = { onImeAction?.invoke() }),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    errorContainerColor = Color.White,
                    focusedBorderColor = Pink500,
                    unfocusedBorderColor = CardBorder,
                    cursorColor = Pink500,
                    focusedTextColor = Maroon,
                    unfocusedTextColor = Maroon,
                    focusedLeadingIconColor = Pink500,
                    unfocusedLeadingIconColor = Pink500,
                    focusedTrailingIconColor = MutedText,
                    unfocusedTrailingIconColor = MutedText
                )
            )
            if (onClick != null) {
                Box(
                    Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onClick)
                )
            }
        }
    }
}

/** Tombol utama dengan gradient pink. */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(
                elevation = if (enabled) 6.dp else 0.dp,
                shape = shape,
                ambientColor = Pink500,
                spotColor = Pink500
            )
            .clip(shape)
            .background(
                brush = Brush.horizontalGradient(listOf(GradientStart, GradientEnd)),
                shape = shape,
                alpha = if (enabled) 1f else 0.5f
            )
            .clickable(enabled = enabled && !isLoading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** Kartu putih dengan garis tepi pink lembut. */
@Composable
fun AnemomCard(
    modifier: Modifier = Modifier,
    containerColor: Color = Color.White,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

/** Label kecil berbentuk pil (mis. "Hasil laboratorium", "Risiko sedang"). */
@Composable
fun AnemomChip(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(containerColor)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
