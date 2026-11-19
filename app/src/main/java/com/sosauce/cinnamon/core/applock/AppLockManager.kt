package com.sosauce.cinnamon.core.applock

import android.os.SystemClock
import com.sosauce.cinnamon.core.datastore.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlin.math.min

/**
 * Owns the app lock: the PIN, the locked/unlocked state and when to lock again.
 *
 * Only the screens showing app content are locked (MainActivity, ContactPickerActivity), they report
 * their lifecycle through [onActivityStarted]/[onActivityStopped]. Calls (CallActivity, call
 * notifications) never go through here.
 *
 * Lifecycle methods must be called from the main thread.
 */
class AppLockManager(
    private val userPreferences: UserPreferences,
    private val scope: CoroutineScope
) {

    // This class is the only writer of the app lock preferences, so these are kept in sync by hand
    // and are always up to date synchronously (no locked content flashing while DataStore emits).
    private val _isEnabled = MutableStateFlow(read(userPreferences.appLockEnabled))
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    private val _useBiometrics = MutableStateFlow(read(userPreferences.appLockUseBiometrics))
    val useBiometrics: StateFlow<Boolean> = _useBiometrics.asStateFlow()

    private val _timeoutMillis = MutableStateFlow(read(userPreferences.appLockTimeout))
    val timeoutMillis: StateFlow<Long> = _timeoutMillis.asStateFlow()

    /** Wall-clock time until which PIN input is refused, 0 if not locked out */
    private val _lockoutUntil = MutableStateFlow(read(userPreferences.appLockLockoutUntil))
    val lockoutUntil: StateFlow<Long> = _lockoutUntil.asStateFlow()

    private var isUnlocked = false
    private val _isLocked = MutableStateFlow(_isEnabled.value)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    /** Incremented every time the app comes back to the foreground, used to re-prompt biometrics */
    private val _foregroundCount = MutableStateFlow(0)
    val foregroundCount: StateFlow<Int> = _foregroundCount.asStateFlow()

    private var startedActivities = 0
    private var backgroundedAt: Long? = null
    private var pendingConfigChange = false
    private var pendingActivityResult = false

    private fun <T> read(flow: Flow<T>): T = runBlocking { flow.first() }

    private fun refreshLocked() {
        _isLocked.value = _isEnabled.value && !isUnlocked
    }


    fun onActivityStarted() {
        if (pendingConfigChange) {
            pendingConfigChange = false
            return
        }
        startedActivities++
        if (startedActivities != 1) return

        val since = backgroundedAt
        backgroundedAt = null
        if (since != null && _isEnabled.value && isUnlocked) {
            val elapsed = SystemClock.elapsedRealtime() - since
            // Coming back from a picker/system screen we launched ourselves (attachments, backups,
            // default-app requests...) shouldn't instantly lock, but still lock if it took long
            val timeout = if (pendingActivityResult) {
                maxOf(_timeoutMillis.value, ACTIVITY_RESULT_GRACE_MILLIS)
            } else {
                _timeoutMillis.value
            }
            if (elapsed >= timeout) lock()
        }
        pendingActivityResult = false
        _foregroundCount.value++
    }

    fun onActivityStopped(isChangingConfigurations: Boolean) {
        if (isChangingConfigurations) {
            pendingConfigChange = true
            return
        }
        startedActivities = (startedActivities - 1).coerceAtLeast(0)
        if (startedActivities == 0) {
            backgroundedAt = SystemClock.elapsedRealtime()
            // Lock right away so the lock is already drawn when the user comes back
            if (_timeoutMillis.value == AppLockTimeout.IMMEDIATELY && !pendingActivityResult) {
                lock()
            }
        }
    }

    /**
     * A launched-for-result screen that didn't stop the activity (e.g permission dialogs) is done,
     * don't keep the grace period for the next time the app goes to the background.
     */
    fun onActivityResumed() {
        pendingActivityResult = false
    }

    /** Call when a locked activity launches another activity for a result */
    fun onLaunchingForResult() {
        pendingActivityResult = true
    }

    fun lock() {
        isUnlocked = false
        refreshLocked()
    }

    /** Unlocks without a PIN, only call after a successful biometric authentication */
    fun unlockWithBiometrics() {
        isUnlocked = true
        refreshLocked()
        resetAttempts()
    }

    //PIN

    /**
     * Checks [pin], counting failures. Unlocks the app when it's correct.
     */
    suspend fun verifyPin(pin: String): PinCheckResult {
        val now = System.currentTimeMillis()
        if (_lockoutUntil.value > now) return PinCheckResult.LockedOut(_lockoutUntil.value)

        val hash = userPreferences.appLockPinHash.first()
        val salt = userPreferences.appLockPinSalt.first()
        val matches = hash.isNotEmpty() && salt.isNotEmpty() && withContext(Dispatchers.Default) {
            MessageDigest.isEqual(
                hashPin(pin,Base64.getDecoder().decode(salt)),
                Base64.getDecoder().decode(hash)
            )
        }

        if (matches) {
            isUnlocked = true
            refreshLocked()
            resetAttempts()
            return PinCheckResult.Correct
        }

        val attempts = userPreferences.appLockFailedAttempts.first() + 1
        val lockoutUntil = if (attempts >= MAX_ATTEMPTS) {
            // 30s, then doubles for every new failure (capped)
            val exponent = min(attempts - MAX_ATTEMPTS, MAX_LOCKOUT_EXPONENT)
            System.currentTimeMillis() + BASE_LOCKOUT_MILLIS * (1L shl exponent)
        } else 0L
        _lockoutUntil.update { lockoutUntil }
        userPreferences.saveAppLockFailure(attempts, lockoutUntil)

        return if (lockoutUntil > 0L) {
            PinCheckResult.LockedOut(lockoutUntil)
        } else {
            PinCheckResult.Wrong(attemptsLeft = MAX_ATTEMPTS - attempts)
        }
    }

    //Settings (callers must have verified the user beforehand)

    suspend fun enable(pin: String) {
        isUnlocked = true
        savePin(pin, enable = true)
        _lockoutUntil.update { 0L }
        _isEnabled.update { true }
        refreshLocked()
    }

    suspend fun changePin(pin: String) {
        savePin(pin, enable = false)
        _lockoutUntil.update { 0L }
    }

    suspend fun disable() {
        userPreferences.disableAppLock()
        _lockoutUntil.update { 0L }
        _isEnabled.update { false }
        refreshLocked()
    }

    suspend fun setTimeout(timeoutMillis: Long) {
        userPreferences.saveAppLockTimeout(timeoutMillis)
        _timeoutMillis.update { timeoutMillis }
    }

    suspend fun setUseBiometrics(useBiometrics: Boolean) {
        userPreferences.saveAppLockUseBiometrics(useBiometrics)
        _useBiometrics.update { useBiometrics }
    }

    private suspend fun savePin(pin: String, enable: Boolean) {
        val salt = ByteArray(SALT_LENGTH).also { SecureRandom().nextBytes(it) }
        val hash = withContext(Dispatchers.Default) { hashPin(pin, salt) }
        val encoder = Base64.getEncoder()
        userPreferences.saveAppLockPin(
            hash = encoder.encodeToString(hash),
            salt = encoder.encodeToString(salt),
            enable = enable
        )
    }

    private fun resetAttempts() {
        _lockoutUntil.update { 0L }
        scope.launch { userPreferences.resetAppLockAttempts() }
    }

    private fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, HASH_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    companion object {
        const val MIN_PIN_LENGTH = 4
        const val MAX_PIN_LENGTH = 8
        const val MAX_ATTEMPTS = 5

        private const val BASE_LOCKOUT_MILLIS = 30_000L
        private const val MAX_LOCKOUT_EXPONENT = 7 // ~64 minutes at most
        private const val ACTIVITY_RESULT_GRACE_MILLIS = 5 * 60_000L
        private const val PBKDF2_ITERATIONS = 120_000
        private const val HASH_LENGTH_BITS = 256
        private const val SALT_LENGTH = 16
    }
}

sealed interface PinCheckResult {
    data object Correct : PinCheckResult
    data class Wrong(val attemptsLeft: Int) : PinCheckResult
    data class LockedOut(val until: Long) : PinCheckResult
}

/**
 * Lock timeout options, in milliseconds. 0 locks as soon as the app leaves the foreground.
 */
object AppLockTimeout {
    const val IMMEDIATELY = 0L
    const val SECONDS_30 = 30_000L
    const val MINUTE_1 = 60_000L
    const val MINUTES_5 = 5 * 60_000L
    const val MINUTES_15 = 15 * 60_000L
}
