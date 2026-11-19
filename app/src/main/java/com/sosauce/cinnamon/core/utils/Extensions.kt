@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.cinnamon.core.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.provider.BlockedNumberContract
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
import androidx.annotation.FloatRange
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.delete
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MenuDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.core.net.toUri
import com.materialkolor.PaletteStyle
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.app.navigation.Screen
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.MonthDay
import java.time.Year
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale


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
        // localized "Today" / "Yesterday" from the platform
        dateTime.isEqual(localDate) || dateTime.isEqual(localDate.minusDays(1)) -> DateUtils.getRelativeTimeSpanString(
            this,
            System.currentTimeMillis(),
            DateUtils.DAY_IN_MILLIS
        ).toString()

        else -> dateTime.format(formatter)
    }
}

fun Long.toDateAndTime(): String {
    return "${toDate()}, ${toTime()}"
}

fun Long.toMediaTime(): String {
    val totalSeconds = this / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
    }
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

    // Without the permission protecting this uri (app used without being a default app),
    // registering throws. The flow then stays silent so collectors never query the provider.
    val canObserve = try {
        registerContentObserver(
            uri,
            true,
            observer
        )
        true
    } catch (_: SecurityException) {
        false
    }

    if (canObserve) trySend(false)
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

@SuppressLint("InlinedApi")
fun Context.createDefaultSmsIntent(): Intent = createRoleIntent(RoleManager.ROLE_SMS) {
    Intent(Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
        putExtra(Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
    }
}

@SuppressLint("InlinedApi")
fun Context.createDefaultDialerIntent(): Intent = createRoleIntent(RoleManager.ROLE_DIALER) {
    Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
        putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
    }
}

/**
 * Intent asking the user to grant [role] to this app. Before Android 10 there is no
 * RoleManager, so [legacyIntent] is used instead.
 * An empty intent is returned when the role doesn't exist on this device.
 */
private inline fun Context.createRoleIntent(
    role: String,
    legacyIntent: () -> Intent
): Intent {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = getSystemService(RoleManager::class.java)
        if (roleManager.isRoleAvailable(role)) {
            roleManager.createRequestRoleIntent(role)
        } else {
            Intent()
        }
    } else {
        legacyIntent()
    }
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

inline fun <K, V> Map<K, V>.copyMutate(block: MutableMap<K, V>.() -> Unit): Map<K, V> {
    return toMutableMap().apply(block)
}

fun String.formateEventDate(): String {

    // run catching in case of niche formats I didn't take into account
    return runCatching {
        when {
            // --MM-DD or --MMDD partial format
            startsWith("--") -> {

                // add month/year separator hyphen to increase chances of ts working
                val text = if (length == 6) "${take(4)}-${takeLast(2)}" else this

                val monthDay = MonthDay.parse(text)
                monthDay.format(DateTimeFormatter.ofPattern("MMMM d", Locale.getDefault()))
            }

            // YYYY-MM-DD standard format
            length == 10 -> {
                val parsedDate = LocalDate.parse(this)
                parsedDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.getDefault()))
            }

            else -> this
        }
    }.getOrDefault(this)
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


fun Modifier.selfAlignHorizontally(
    align: Alignment.Horizontal = Alignment.CenterHorizontally,
    @FloatRange fraction: Float = 1f
): Modifier {
    return fillMaxWidth(fraction).wrapContentWidth(align)
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
    ?.let { it.endsWith("vCard") || it.endsWith("x-vCard") } == true


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

fun Context.toLocalizedTab(tab: Int): String {
    return when (tab) {
        DefaultTabOption.MESSAGES -> getString(R.string.messages)
        DefaultTabOption.CONTACTS -> getString(R.string.contacts)
        DefaultTabOption.CALLS -> getString(R.string.calls)
        DefaultTabOption.DIALER -> getString(R.string.dialer)
        else -> throw IllegalArgumentException("Not a valid tab!")
    }
}

fun Int.tabToScreen(): Screen {
    return when (this) {
        DefaultTabOption.MESSAGES -> Screen.Conversations
        DefaultTabOption.CONTACTS -> Screen.Contacts
        DefaultTabOption.CALLS -> Screen.Calls
        DefaultTabOption.DIALER -> Screen.Dialer()
        else -> throw IllegalArgumentException("Not a valid tab!")
    }
}


fun TextFieldState.backspace() {
    if (selection.collapsed) {
        edit {
            delete(length - 1, length)
        }
    }
}


fun String.isShortCode(): Boolean {
    return !Patterns.EMAIL_ADDRESS.matcher(this).matches() && any { it.isLetter() }
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

@Composable
fun ColumnScope.Spacer(height: Dp) {
    Spacer(Modifier.height(height))
}

/**
 * Display name of a file picked for a backup import, falls back to a generic label
 */
fun Context.readBackupDisplayName(uri: Uri): String {
    return runCatching {
        contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull() ?: getString(R.string.backup_file_name)
}

/**
 * Adds [numbers] to the system blocked numbers, it can also take emails
 * @return Whether blocking was successful
 */
suspend fun Context.blockNumbers(numbers: List<String>): Boolean = withContext(Dispatchers.IO) {
    val ops = ArrayList<ContentProviderOperation>()

    numbers.forEach { number ->
        ops.add(
            ContentProviderOperation.newInsert(BlockedNumberContract.BlockedNumbers.CONTENT_URI)
                .withValue(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER, number)
                .build()
        )
    }
    return@withContext try {
        contentResolver.applyBatch(BlockedNumberContract.AUTHORITY, ops)
        true
    } catch (_: Exception) {
        false
    }
}

/**
 * Unblocks [number] (a phone number or an email address) and notifies observers of the blocked
 * numbers, as [BlockedNumberContract.unblock] doesn't do it on its own for some reason
 *
 * @return the number of blocked entries that were removed
 */
fun Context.unblock(number: String): Int {
    val unblocked = BlockedNumberContract.unblock(this, number)

    // this should never throw, but god knows some OEMs have proved me things I thought not possible
    try {
        contentResolver.notifyChange(BlockedNumberContract.BlockedNumbers.CONTENT_URI, null)
    } catch (_: Exception) {}
    return unblocked
}

/**
 * Default name (without extension) of a backup file, e.g. "contacts_2026-01-31_18-05-00"
 */
fun backupFileName(prefix: String, nowMillis: Long = System.currentTimeMillis()): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
    return "${prefix}_${formatter.format(Date(nowMillis))}"
}

/**
 * Handles [ActivityNotFoundException]
 */
fun Context.safeStartActivity(
    intent: Intent,
    errorMessage: String? = null
) {
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(
            this,
            errorMessage ?: getString(R.string.generic_error),
            Toast.LENGTH_SHORT
        ).show()
    }
}
