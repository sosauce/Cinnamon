package com.sosauce.cuteconnect.utils

import android.app.Activity
import android.app.role.RoleManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.provider.BlockedNumberContract
import android.provider.BlockedNumberContract.BlockedNumbers
import android.provider.ContactsContract
import android.provider.ContactsContract.PhoneLookup
import android.provider.OpenableColumns
import android.provider.Settings
import android.provider.Telephony
import android.provider.Telephony.Mms
import android.provider.Telephony.Sms
import android.telecom.TelecomManager
import android.telephony.PhoneNumberUtils
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sosauce.cuteconnect.data.datastore.rememberIsLandscape
import com.sosauce.cuteconnect.domain.model.CuteContact
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.random.Random
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.core.content.contentValuesOf
import com.sosauce.cuteconnect.ui.navigation.Screen
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
import kotlin.text.matches
import androidx.core.net.toUri
import androidx.core.telephony.TelephonyManagerCompat
import androidx.navigation3.runtime.NavKey
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.sosauce.cuteconnect.data.datastore.PreferencesKeys.DEFAULT_SIM
import com.sosauce.cuteconnect.data.datastore.dataStore
import com.sosauce.cuteconnect.data.datastore.getPreference
import kotlinx.coroutines.flow.collectLatest
import java.io.FileNotFoundException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.MonthDay
import java.time.Year
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.TimeZone
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

fun Long.parsedDate(): String {
    val date = Date(this)
    val format = SimpleDateFormat("dd/MMM/yyyy HH:mm", Locale.getDefault())

    return format.format(date)
}

fun Int.toReadableTime(): String {
    val totalSeconds = this / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}

fun Long.toDate(): String {
    val date = LocalDate.ofInstant(
        Instant.ofEpochMilli(this),
        ZoneId.systemDefault()
    )
    val currentYear = Year.now().value
    val isFromPreviousYear = date.year != currentYear
    val formatStyle = if (!isFromPreviousYear) {
        FormatStyle.MEDIUM
    } else FormatStyle.SHORT

    return date.format(DateTimeFormatter.ofLocalizedDate(formatStyle))

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
fun String.betterFormatNumber(): String {
    return if (PhoneNumberUtils.isWellFormedSmsAddress(this)) {
        PhoneNumberUtils.formatNumber(this, Locale.getDefault().country) ?: this
    } else {
        this
    }
}

/**
 * Gets or creates a thread ID based on the number this function is called on. Wrapper around Threads.getThreadIdOrCreate
 */
fun String.getThreadIdOrCreate(context: Context) = Telephony.Threads.getOrCreateThreadId(context, setOf(this))

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

/**
 * Returns the contact associated to the number this function is called on, if no contact is found, it will build on with the number.
 */

fun String.getContactPfpUri(context: Context): Uri {
    val uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(this))

    context.contentResolver.query(
        uri,
        arrayOf(PhoneLookup.PHOTO_URI),
        null,
        null,
        null
    )?.use { cursor ->
        val photoColumn = cursor.getColumnIndexOrThrow(PhoneLookup.PHOTO_URI)
        if (cursor.moveToFirst()) {
            val photoUri = cursor.getString(photoColumn)?.toUri() ?: Uri.EMPTY

            return photoUri
        }
    }
    return Uri.EMPTY
}

// Arranged from Fossify
fun Context.getMMSSize(uri: Uri): Long {

    val assetFileDescriptor = try {
        contentResolver.openAssetFileDescriptor(uri, "r")
    } catch (_: FileNotFoundException) {
        null
    }

    val length = assetFileDescriptor?.use { it.length } ?: -1
    if (length != -1L) {
        return length
    }

    return -1
}


fun Long.toReadableDate(): String {

    val todayCalendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val yesterdayCalendar = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val dateMidnight = Calendar.getInstance().apply {
        timeInMillis = this@toReadableDate
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    if (dateMidnight.timeInMillis == todayCalendar.timeInMillis) return "Today"
    if (dateMidnight.timeInMillis == yesterdayCalendar.timeInMillis) return "Yesterday"

    val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return format.format(Date(this))
}



fun Long.millisToDate(
    pattern: String = "MMMM d, yyyy"
): String {
    val formatter = SimpleDateFormat(pattern, Locale.getDefault())
    val calendar = Calendar.getInstance().apply {
        timeInMillis = this@millisToDate
    }

    return formatter.format(calendar.time)
}

fun Long.toReadableDuration(
    durationUnit: DurationUnit = DurationUnit.SECONDS
): String {
    val duration = this.toDuration(durationUnit)

    return duration.toComponents { _, minutes, seconds, _, _ ->
        "%02d:%02d".format(minutes, seconds)
    }
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
fun SubscriptionManager.observeSims(context: Context) = callbackFlow {
    val listener = object : SubscriptionManager.OnSubscriptionsChangedListener() {
        override fun onSubscriptionsChanged() {
            trySend(true)
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        this@observeSims.addOnSubscriptionsChangedListener(context.mainExecutor, listener)
    } else {
        this@observeSims.addOnSubscriptionsChangedListener(listener)
    }

    trySend(false)

    awaitClose {
        this@observeSims.removeOnSubscriptionsChangedListener(listener)
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

fun String.isLink(): Boolean {
    val regex = LINK_REGEX.toRegex()
    return this.matches(regex)
}

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

val LazyListState.showCuteSearchbar
    get() =
        if (layoutInfo.totalItemsCount == 0) {
            true
        } else if (
            layoutInfo.visibleItemsInfo.firstOrNull()?.index == 0 &&
            layoutInfo.visibleItemsInfo.lastOrNull()?.index == layoutInfo.totalItemsCount - 1
        ) {
            true
        } else {
            layoutInfo.visibleItemsInfo.lastOrNull()?.index != layoutInfo.totalItemsCount - 1
        }



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

/**
 * Attempts to block the number this function is called on.
 */
fun String.blockNumber(context: Context) {
    val values = contentValuesOf(
        BlockedNumbers.COLUMN_ORIGINAL_NUMBER to this
    )
    context.contentResolver.insert(BlockedNumbers.CONTENT_URI, values)
    Toast.makeText(
        context,
        "Blocked",
        Toast.LENGTH_SHORT
    ).show()
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

fun <E> MutableList<E>.addOrRemove(element: E) {
    if (contains(element)) {
        remove(element)
    } else add(element)
}

fun <E> MutableList<E>.addOrNot(element: E) {
    if (!contains(element)) {
        add(element)
    }
}

fun <E> MutableSet<E>.addOrRemove(element: E) {
    if (contains(element)) {
        remove(element)
    } else add(element)
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
