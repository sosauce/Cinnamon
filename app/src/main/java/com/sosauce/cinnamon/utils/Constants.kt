package com.sosauce.cinnamon.utils

const val ICON_TEXT_SPACING = 10
const val APP_PACKAGE = "com.sosauce.cinnamon"
const val ACCEPT_INCOMING_CALL = "accept_incoming_call"
const val DECLINE_INCOMING_CALL = "decline_incoming_call"
const val HANGUP_ONGOING_CALL = "hangup_ongoing_call"
const val CALL_ACTIVITY = "callActivity"
const val RESULT_KEY = "result_key"
const val THREAD_ID = "thread_id"
const val SWITCH_AUDIO_SOURCE = "switch_audio_source"
const val AUDIO_SOURCE = "audio_source"
const val MUTE_SOURCE = "mute_source"
const val FULL_SCREEN_INTENT = "full_screen_intent"

// Googled "how many milliseconds in a day" lol
const val ONE_DAY_IN_MILLIS = 8.64e+7
const val EMOJI_REGEX = "(?:[\uD83C\uDF00-\uD83D\uDDFF]|[\uD83E\uDD00-\uD83E\uDDFF]|" +
        "[\uD83D\uDE00-\uD83D\uDE4F]|[\uD83D\uDE80-\uD83D\uDEFF]|" +
        "[\u2600-\u26FF]\uFE0F?|[\u2700-\u27BF]\uFE0F?|\u24C2\uFE0F?|" +
        "[\uD83C\uDDE6-\uD83C\uDDFF]{1,2}|" +
        "[\uD83C\uDD70\uD83C\uDD71\uD83C\uDD7E\uD83C\uDD7F\uD83C\uDD8E\uD83C\uDD91-\uD83C\uDD9A]\uFE0F?|" +
        "[\u0023\u002A\u0030-\u0039]\uFE0F?\u20E3|[\u2194-\u2199\u21A9-\u21AA]\uFE0F?|[\u2B05-\u2B07\u2B1B\u2B1C\u2B50\u2B55]\uFE0F?|" +
        "[\u2934\u2935]\uFE0F?|[\u3030\u303D]\uFE0F?|[\u3297\u3299]\uFE0F?|" +
        "[\uD83C\uDE01\uD83C\uDE02\uD83C\uDE1A\uD83C\uDE2F\uD83C\uDE32-\uD83C\uDE3A\uD83C\uDE50\uD83C\uDE51]\uFE0F?|" +
        "[\u203C\u2049]\uFE0F?|[\u25AA\u25AB\u25B6\u25C0\u25FB-\u25FE]\uFE0F?|" +
        "[\u00A9\u00AE]\uFE0F?|[\u2122\u2139]\uFE0F?|\uD83C\uDC04\uFE0F?|\uD83C\uDCCF\uFE0F?|" +
        "[\u231A\u231B\u2328\u23CF\u23E9-\u23F3\u23F8-\u23FA]\uFE0F?)+"
const val GITHUB_RELEASES = "https://github.com/sosauce/Cinnamon/releases"
const val SUPPORT_PAGE = "https://sosauce.github.io/support/"
const val HOW_TO_ENABLE_RESTRCITED_PERMS =
    "https://support.google.com/android/answer/12623953?hl=en"


object CuteTheme {
    const val SYSTEM = "SYSTEM"
    const val DARK = "DARK"
    const val LIGHT = "LIGHT"
    const val AMOLED = "AMOLED"
}

object MmsSize {
    const val FILE_SIZE_NONE = -1L
    const val FILE_SIZE_100_KB = 102_400L
    const val FILE_SIZE_200_KB = 204_800L
    const val FILE_SIZE_300_KB = 307_200L
    const val FILE_SIZE_600_KB = 614_400L
    const val FILE_SIZE_1_MB = 1_048_576L
    const val FILE_SIZE_2_MB = 2_097_152L
}

object CuteIntents {
    const val NOTIFICATION_NAVIGATE_TO_THREAD = "notification thread"
}

object DefaultTabOption {
    const val MESSAGES = "MESSAGES"
    const val CONTACTS = "CONTACTS"
    const val DIALER = "DIALER"
    const val DIALPAD = "DIALPAD"
}

/**
 * Constants for unique to use for [androidx.compose.foundation.lazy.LazyColumn]
 */
object LazyListKeys {
    const val ARCHIVED = "archived"
    const val PINNED_CONVERSATIONS = "pinned_conversations"
    const val FAVORITE_CONTACTS = "FAVORITE_CONTACTS"
    const val GROUP_CHAT_BUTTON = "GROUP_CHAT_BUTTON"
    const val VOICEMAIL = "VOICEMAIL"

}

object SharedTransitionKeys {
    const val CONTACT_POSTER = "CONTACT_POSTER"
    const val CONTACT_PFP = "CONTACT_PFP"
    const val CONTACT_NAME = "CONTACT_NAME"
    const val CONVERSATION_NAME = "CONVERSATION_NAME"
    const val CONVERSATION_WALLPAPER = "CONVERSATION_WALLPAPER"
}

object CutePaletteStyle {
    const val EXPRESSIVE = "Expressive"
    const val FIDELITY = "Fidelity"
    const val TONAL_SPOT = "Tonal spot"
    const val NEUTRAL = "Neutral"
    const val VIBRANT = "Vibrant"
    const val MONOCHROME = "Monochrome"
    const val FRUIT_SALAD = "Fruit salad"
}