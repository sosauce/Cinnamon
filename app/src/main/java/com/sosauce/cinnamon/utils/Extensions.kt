@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.utils

import android.app.Activity
import android.app.WallpaperManager
import android.app.role.RoleManager
import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.provider.BlockedNumberContract
import android.provider.BlockedNumberContract.BlockedNumbers
import android.provider.ContactsContract.PhoneLookup
import android.provider.OpenableColumns
import android.provider.Telephony
import android.provider.Telephony.Mms
import android.provider.Telephony.Sms
import android.telecom.TelecomManager
import android.telephony.PhoneNumberUtils
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.util.Patterns
import android.widget.Toast
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.delete
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.core.net.toUri
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.materialkolor.PaletteStyle
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.data.datastore.rememberIsLandscape
import com.sosauce.cinnamon.presentation.navigation.Screen
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.time.Instant
import java.time.LocalDate
import java.time.MonthDay
import java.time.Year
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.DurationUnit
import kotlin.time.toDuration


val Context.appVersion
    get() = packageManager.getPackageInfo(packageName, 0).versionName

inline fun <T> List<T>.thenIf(
    condition: Boolean,
    crossinline block: List<T>.() -> List<T>
): List<T> = if (condition) block() else this

fun Long.toDate(): String {


    val locale = Locale.getDefault()
    val zoneId = ZoneId.systemDefault()
    val currentYear = Year.now(zoneId)
    val localDate = LocalDate.now(zoneId)
    val dateTime = Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()
    val year = dateTime.year
    val skeleton = if (year == currentYear.value) "MMMd" else "MMMdy"
    val pattern = DateFormat.getBestDateTimePattern(locale, skeleton)
    val formatter = DateTimeFormatter.ofPattern(pattern, locale)

    return when {
        dateTime.isEqual(localDate) -> "Today"
        dateTime.isEqual(localDate.minusDays(1)) -> "Yesterday"
        else -> dateTime.format(formatter)
    }
}

fun Long.toDateAndTime(): String {
    return "${toDate()}, ${toTime()}"
}

fun Long.toTime(): String {
    val zoneId = ZoneId.systemDefault()
    val locale = Locale.getDefault()
    val dateTime = Instant.ofEpochMilli(this).atZone(zoneId).toLocalTime()
    val skeleton = "jm"
    val pattern = DateFormat.getBestDateTimePattern(locale, skeleton)
    val formatter = DateTimeFormatter.ofPattern(pattern, locale)

    return dateTime.format(formatter)
}

fun Long.secondsToDuration(): String {

    var finalTime = ""
    val rawSeconds = this
    val hours = rawSeconds / 3600
    val minutes = (this % 3600) / 60
    val seconds = rawSeconds % 60

    if (hours > 0) {
        finalTime += "${hours}h "
    }

    if (minutes > 0) {
        finalTime += "${minutes}m "
    }

    return "$finalTime${seconds}s"
}

// I have way too many functions to convert time and date lmao needs cleanup
fun Long.toStopwatch(
    durationUnit: DurationUnit = DurationUnit.MILLISECONDS
): String {
    val duration = this.toDuration(durationUnit)
    return duration.toString()
}

/**
 * Returns the contact name if available, if not, returns the number as is
 */
fun String.getContactNameOrNothing(context: Context): String {

    if (this.isEmpty()) return this
    val uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(this))

    context.contentResolver.query(
        uri,
        arrayOf(PhoneLookup.DISPLAY_NAME),
        null,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            return cursor.getString(cursor.getColumnIndexOrThrow(PhoneLookup.DISPLAY_NAME))
        }
    }

    return this
}

//fun Int.getAddressFromThreadId(context: Context): String? {
//    val projection = arrayOf(Sms.ADDRESS)
//    val cursor = context.contentResolver.query(Uri.withAppendedPath(Sms.CONTENT_URI, this.toString()), projection, null, null, null)
//
//    cursor?.use {
//        if (it.moveToFirst()) {
//            println("address we got : ${it.getString(it.getColumnIndexOrThrow(Sms.ADDRESS))}")
//            return it.getString(it.getColumnIndexOrThrow(Sms.ADDRESS))
//        }
//    }
//    return null
//}


/**
 * Formats the number this function is called on, if it's called on a non-number, it will do nothing.
 */
fun String.beautifyNumber() =
    PhoneNumberUtils.formatNumber(this, Locale.getDefault().country) ?: this

/**
 * Gets or creates a thread ID based on the number this function is called on. Wrapper around [Telephony.Threads.getOrCreateThreadId]
 */
fun String.getThreadIdOrCreate(context: Context) =
    Telephony.Threads.getOrCreateThreadId(context, setOf(this))

/**
 * Gets or creates a thread ID based on the number this function is called on but this one is multiple addresses. Wrapper around [Telephony.Threads.getOrCreateThreadId]
 */
fun List<String>.getThreadIdOrCreate(context: Context) =
    Telephony.Threads.getOrCreateThreadId(context, this.toSet())

fun Long.getAddressFromThreadId(context: Context): String {
    context.contentResolver.query(
        Sms.CONTENT_URI,
        arrayOf(Sms.ADDRESS),
        "${Sms.THREAD_ID} = ?",
        arrayOf(this.toString()),
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            return cursor.getString(cursor.getColumnIndexOrThrow(Sms.ADDRESS))
        }
    }
    return ""
}

/**
 * Return the contact ID of the number it's called on. -1 if contact doesn't exist.
 */
fun String.getContactId(context: Context): Long {


    if (isNullOrEmpty() || isNullOrBlank()) return -1


    context.contentResolver.query(
        Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(this)),
        arrayOf(PhoneLookup._ID),
        null,
        null,
        null
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(PhoneLookup._ID)

        if (cursor.moveToFirst()) {
            return cursor.getLong(idColumn)
        }
    }

    return -1
}

//fun String.getContactPfpFromNumber(
//    context: Context,
//    highRes: Boolean = true
//): Uri {
//
//    if (this.isEmpty()) return Uri.EMPTY
//
//
//    val uri = Uri.withAppendedPath(
//        PhoneLookup.CONTENT_FILTER_URI,
//        Uri.encode(this)
//    )
//
//    val photoPath = if (highRes) PhoneLookup.PHOTO_URI else PhoneLookup.PHOTO_THUMBNAIL_URI
//
//    context.contentResolver.query(
//        uri,
//        arrayOf(photoPath),
//        null,
//        null
//    )?.use { cursor ->
//
//        val photoColumn = cursor.getColumnIndexOrThrow(photoPath)
//
//        if (cursor.moveToFirst()) {
//            return cursor.getString(photoColumn)?.toUri() ?: Uri.EMPTY
//        }
//
//    }
//    return Uri.EMPTY
//}

// Arranged from Fossify
fun Context.getMMSSize(uri: Uri): Long {

    val assetFileDescriptor = try {
        contentResolver.openAssetFileDescriptor(uri, "r")
    } catch (_: FileNotFoundException) {
        null
    }

    val length = assetFileDescriptor?.use { it.length } ?: -1

    return length
}


fun Modifier.thenIf(
    condition: Boolean,
    modifier: Modifier.() -> Modifier
): Modifier {
    return if (condition) {
        this.then(modifier())
    } else this
}


// https://stackoverflow.com/a/73036713/28577483
fun <E, K> List<E>.groupSubsequentlyBy(propertySelector: (E) -> K): List<Pair<E, Int>> = buildList {
    if (this@groupSubsequentlyBy.isEmpty()) return@buildList

    var currentItem = this@groupSubsequentlyBy.first()
    var currentKey = propertySelector(currentItem)
    var count = 1

    for (i in 1 until this@groupSubsequentlyBy.size) {
        val item = this@groupSubsequentlyBy[i]
        val key = propertySelector(item)

        if (key == currentKey) {
            count++
        } else {
            add(currentItem to count)
            currentItem = item
            currentKey = key
            count = 1
        }
    }
    add(currentItem to count)
}


fun ContentResolver.observe(uri: Uri) = callbackFlow {
    val observer = object : ContentObserver(null) {
        override fun onChange(selfChange: Boolean) {
            trySend(selfChange)
        }
    }

    registerContentObserver(
        uri,
        true,
        observer
    )

    trySend(false)
    awaitClose {
        unregisterContentObserver(observer)
    }
}


fun Long.usePart(
    context: Context,
    block: (InputStream) -> String
): String {
    val partUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        Mms.Part.CONTENT_URI.buildUpon().appendPath(this.toString()).build()
    } else "content://mms/part/$this".toUri()
    try {
        val stream = context.contentResolver.openInputStream(partUri) ?: return ""
        stream.use {
            return block(stream)
        }
    } catch (e: IOException) {
        return ""
    }
}

fun String.isEmoji(): Boolean {
    val regex = EMOJI_REGEX.toRegex()
    return this.matches(regex)
}

fun String.isLink() = Patterns.WEB_URL.matcher(this).matches()

@Composable
fun Modifier.cuteHazeEffect(
    state: HazeState,
    intensity: Dp = 15.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    block: (HazeEffectScope.() -> Unit)? = null,
) = hazeEffect(
    state = state,
    style = HazeStyle(
        backgroundColor = backgroundColor,
        tints = emptyList(),
        blurRadius = intensity,
        noiseFactor = 0f
    ),
    block = block
)


fun Long.toShortDate(context: Context): String {

    val zoneId = ZoneId.systemDefault()
    val date = Instant.ofEpochMilli(this).atZone(zoneId).toLocalDateTime()

    val pattern = when {
        date.year != Year.now().value -> "MMM dd yyyy"
        DateUtils.isToday(this) -> {
            val is24Hour = DateFormat.is24HourFormat(context)
            if (is24Hour) "HH:mm" else "hh:mm a"
        }

        else -> "MMM dd"
    }


    val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
    return date.format(formatter)

}


fun Activity.requestRole(
    role: String // RoleManager.ROLE_SMS
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = getSystemService(RoleManager::class.java)
        val isRoleAvailable = roleManager.isRoleAvailable(role)
        val isRoleHeld = roleManager.isRoleHeld(role)

        if (isRoleAvailable && !isRoleHeld) {
            val roleRequestIntent = roleManager.createRequestRoleIntent(role)
            startActivityForResult(roleRequestIntent, 1)
        }
    } else {
        if (role == RoleManager.ROLE_SMS) {
            val intent = Intent(Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                putExtra(Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
            }
            startActivityForResult(intent, 1)
        } else {
            val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                putExtra(Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
            }
            startActivityForResult(intent, 1)
        }
    }
}

fun Activity.hasBothRoles(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = getSystemService(RoleManager::class.java)
        roleManager.isRoleHeld(RoleManager.ROLE_DIALER) && roleManager.isRoleHeld(RoleManager.ROLE_SMS)
    } else {
        Sms.getDefaultSmsPackage(this) == packageName &&
                getSystemService(TelecomManager::class.java)?.defaultDialerPackage == packageName
    }
}


fun getMmsText(
    context: Context,
    id: String
): String {
    val partUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        "${Mms.Part.CONTENT_URI}/$id".toUri()
    } else {
        "content://mms/part".toUri()
    }
    val stringBuilder = StringBuilder()

    try {
        context.contentResolver.openInputStream(partUri)?.use { `is` ->
            val reader = BufferedReader(InputStreamReader(`is`, "UTF-8"))
            var line: String? = reader.readLine()
            while (line != null) {
                stringBuilder.append(line)
                line = reader.readLine()
            }
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }

    return stringBuilder.toString()
}

inline fun <E> List<E>.copyMutate(block: MutableList<E>.() -> Unit): List<E> {
    return toMutableList().apply(block)
}

inline fun <E> Set<E>.copyMutate(block: MutableSet<E>.() -> Unit): Set<E> {
    return toMutableSet().apply(block)
}

fun String.formateEventDate(): String {
    return when {
        startsWith("--") -> {
            val monthDay = MonthDay.parse(this)
            monthDay.format(DateTimeFormatter.ofPattern("MMMM d", Locale.getDefault()))
        }

        length == 10 -> {
            val parsedDate = LocalDate.parse(this)
            parsedDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.getDefault()))
        }

        else -> this
    }
}


//fun Uri.fileSize(context: Context): Long {
//
//    context.contentResolver.query(
//        this,
//        arrayOf(OpenableColumns.SIZE),
//        null,
//        null
//    )?.use { cursor ->
//        val sizeColumn = cursor.getColumnIndexOrThrow(OpenableColumns.SIZE)
//        cursor.moveToFirst()
//        return cursor.getLong(sizeColumn)
//
//    }
//
//    return 0
//}

fun CuteRoundedCornerShape(
    top: Dp,
    bottom: Dp
): Shape =
    RoundedCornerShape(topStart = top, topEnd = top, bottomEnd = bottom, bottomStart = bottom)

@Composable
fun rememberFocusRequester(): FocusRequester {
    return remember { FocusRequester() }
}

@Composable
fun rememberHazeState(): HazeState {
    return remember { HazeState() }
}


@Composable
fun rememberSearchbarAlignment(
): Alignment {

    val isLandscape = rememberIsLandscape()

    return remember(isLandscape) {
        if (isLandscape) {
            Alignment.BottomEnd
        } else {
            Alignment.BottomCenter
        }
    }
}

@Composable
fun rememberSearchbarMaxFloatValue(
): Float {

    val isLandscape = rememberIsLandscape()

    return remember(isLandscape) {
        if (isLandscape) {
            0.4f
        } else {
            0.85f
        }
    }
}

@Composable
fun rememberSearchbarRightPadding(
): Dp {

    val isLandscape = rememberIsLandscape()

    return remember(isLandscape) {
        if (isLandscape) {
            10.dp
        } else {
            0.dp
        }
    }
}

@Composable
fun rememberInteractionSource(): MutableInteractionSource {
    return remember { MutableInteractionSource() }
}

@Composable
fun keyboardAsState(): State<Boolean> {
    val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    return rememberUpdatedState(isImeVisible)
}

@Composable
fun anyDarkColorScheme(): ColorScheme {
    val context = LocalContext.current

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicDarkColorScheme(context)
    } else {
        darkColorScheme()
    }
}

@Composable
fun anyLightColorScheme(): ColorScheme {
    val context = LocalContext.current

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicLightColorScheme(context)
    } else {
        lightColorScheme()
    }
}

fun Modifier.selfAlignHorizontally(align: Alignment.Horizontal = Alignment.CenterHorizontally): Modifier {
    return fillMaxWidth().wrapContentWidth(align)
}

fun Context.getAdaptivePrimaryColor(fallbackColor: Color): Color {
    val color = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicDarkColorScheme(this).primary
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 -> {
            val manager = WallpaperManager.getInstance(this)
            manager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)?.primaryColor?.toArgb()
                ?.let { Color(it) } ?: fallbackColor
        }

        else -> fallbackColor
    }

    return color
}

/**
 * @param lastIndex Last index of the list the DropdownMenuItem is being iterated through
 */
@Composable
fun MenuDefaults.getItemShape(
    index: Int,
    lastIndex: Int
): Shape {
    return when (index) {
        0 -> leadingItemShape
        lastIndex -> trailingItemShape
        else -> middleItemShape
    }
}

fun Uri.isImage(context: Context): Boolean =
    context.contentResolver.getType(this)?.startsWith("image/") == true

fun Uri.isVideo(context: Context): Boolean =
    context.contentResolver.getType(this)?.startsWith("video/") == true

fun Uri.isVcard(context: Context): Boolean = context.contentResolver.getType(this)
    ?.endsWith("vCard") == true || context.contentResolver.getType(this)
    ?.endsWith("x-vCard") == true


fun Uri.getVcfName(context: Context): String? {
    context.contentResolver.openInputStream(this)?.bufferedReader()?.useLines { lines ->
        lines.forEach { line ->
            if (line.startsWith("FN:")) {
                return line.removePrefix("FN:").trim()
            }
        }
    }
    return null
}

// Source - https://stackoverflow.com/a/25005243
// Posted by Stefan Haustein
// Retrieved 2026-02-25, License - CC BY-SA 3.0
fun Uri.getFileName(context: Context): String? {

    var result: String? = null
    if (scheme == "content") {
        context.contentResolver.query(this, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    result =
                        cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                }
            }
    }
    if (result == null) {
        result = path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) result = result?.substring(cut + 1)
    }
    return result
}

fun Context.toLocalizedTab(tab: String): String {
    return when (tab) {
        DefaultTabOption.MESSAGES -> getString(R.string.messages)
        DefaultTabOption.CONTACTS -> getString(R.string.contacts)
        DefaultTabOption.DIALER -> getString(R.string.dialer)
        DefaultTabOption.DIALPAD -> getString(R.string.dialpad)
        else -> throw IllegalArgumentException("Not a valid tab!")
    }
}

fun String.tabToScreen(): Screen {
    return when (this) {
        DefaultTabOption.MESSAGES -> Screen.Messages
        DefaultTabOption.CONTACTS -> Screen.Contacts
        DefaultTabOption.DIALER -> Screen.Dialer
        DefaultTabOption.DIALPAD -> Screen.Dialpad()
        else -> throw IllegalArgumentException("Not a valid tab!")
    }
}


fun NavBackStack<NavKey>.navigateBack() {
    if (size == 1) {
        add(Screen.Messages)
    } else {
        removeLastOrNull()
    }
}

fun TextFieldState.backspace() {
    if (selection.collapsed) {
        edit {
            delete(length - 1, length)
        }
    }
}

fun <T> bouncySpec() = spring<T>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessLow
)

fun <T> bouncySpecNavigation() = spring<T>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessLow
)

/**
 * It can also take emails
 */
suspend fun Context.blockNumbers(numbers: List<String>) = withContext(Dispatchers.IO) {
    val ops = ArrayList<ContentProviderOperation>()

    numbers.fastForEach { number ->
        ops.add(
            ContentProviderOperation.newInsert(BlockedNumbers.CONTENT_URI)
                .withValue(BlockedNumbers.COLUMN_ORIGINAL_NUMBER, number)
                .build()
        )
    }
    try {
        contentResolver.applyBatch(BlockedNumberContract.AUTHORITY, ops)
    } catch (_: Exception) {
        withContext(Dispatchers.Main) {

            val text = if (numbers.size > 1) {
                "Couldn't block ${numbers.first()} and ${numbers.size - 1} more"
            } else "Couldn't block ${numbers.first()}"

            Toast.makeText(this@blockNumbers, text, Toast.LENGTH_SHORT).show()
        }
    }
}


fun String.isShortCode(): Boolean {
    if (Patterns.EMAIL_ADDRESS.matcher(this).matches()) {
        return false
    }

    return any { it.isLetter() }
}

fun String.toPaletteStyle(): PaletteStyle {
    return when (this) {
        CutePaletteStyle.EXPRESSIVE -> PaletteStyle.Expressive
        CutePaletteStyle.FIDELITY -> PaletteStyle.Fidelity
        CutePaletteStyle.TONAL_SPOT -> PaletteStyle.TonalSpot
        CutePaletteStyle.NEUTRAL -> PaletteStyle.Neutral
        CutePaletteStyle.VIBRANT -> PaletteStyle.Vibrant
        CutePaletteStyle.MONOCHROME -> PaletteStyle.Monochrome
        CutePaletteStyle.FRUIT_SALAD -> PaletteStyle.FruitSalad
        else -> throw IllegalArgumentException("Not a valid palette!")
    }
}