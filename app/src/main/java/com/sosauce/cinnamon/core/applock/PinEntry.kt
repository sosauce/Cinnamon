package com.sosauce.cinnamon.core.applock

import android.text.format.DateUtils
import androidx.biometric.AuthenticationRequest
import androidx.biometric.AuthenticationResult
import androidx.biometric.compose.rememberAuthenticationLauncher
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.withResumed
import com.sosauce.cinnamon.R
import com.sosauce.cinnamon.core.ui.components.CuteHero
import com.sosauce.cinnamon.core.ui.components.pin.CutePinDots
import com.sosauce.cinnamon.core.ui.components.pin.CutePinPad
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.time.Duration.Companion.seconds

/**
 * Asks for the current PIN (or biometrics when enabled), and calls [onVerified] once the user is
 * verified. A correct PIN or biometric also unlocks the app.
 *
 * @param autoPromptKey The biometric prompt is shown automatically every time this changes
 */
@Composable
fun PinVerification(
    title: String,
    description: String,
    onVerified: () -> Unit,
    modifier: Modifier = Modifier,
    autoPromptKey: Any? = Unit,
    footer: @Composable ColumnScope.() -> Unit = {}
) {
    val appLockManager = koinInject<AppLockManager>()
    val context = LocalContext.current
    val resources = LocalResources.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var isChecking by remember { mutableStateOf(false) }
    val shakePositions = listOf(-24f, 20f, -14f, 8f, 0f)
    val errorShakeY = remember { Animatable(0f) }
    val useBiometrics by appLockManager.useBiometrics.collectAsStateWithLifecycle()
    val canUseBiometrics = useBiometrics && remember { context.canUseBiometrics() }
    val lockoutUntil by appLockManager.lockoutUntil.collectAsStateWithLifecycle()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(lockoutUntil) {
        while (lockoutUntil > System.currentTimeMillis()) {
            now = System.currentTimeMillis()
            delay(1.seconds)
        }
        now = System.currentTimeMillis()
    }
    val isLockedOut = lockoutUntil > now

    val biometricLauncher = rememberAuthenticationLauncher { result ->
        if (result is AuthenticationResult.Success) {
            appLockManager.unlockWithBiometrics()
            onVerified()
        }
    }
    LaunchedEffect(autoPromptKey, canUseBiometrics) {
        if (canUseBiometrics) {
            lifecycleOwner.lifecycle.withResumed {
                biometricLauncher.launch(
                    AuthenticationRequest.biometricRequest(
                        title = title,
                        AuthenticationRequest.Biometric.Fallback.CustomOption(resources.getString(R.string.use_pin))
                    ) {
                        setMinStrength(AuthenticationRequest.Biometric.Strength.Class2)
                        setIsConfirmationRequired(false)
                    }
                )
            }
        }
    }

    fun submit() {
        if (isChecking || isLockedOut || pin.length < AppLockManager.MIN_PIN_LENGTH) return
        isChecking = true
        scope.launch {
            when (val result = appLockManager.verifyPin(pin)) {
                PinCheckResult.Correct -> {
                    message = null
                    onVerified()
                }

                is PinCheckResult.Wrong -> {
                    message = resources.getQuantityString(
                        R.plurals.wrong_pin_attempts_left,
                        result.attemptsLeft,
                        result.attemptsLeft
                    )
                    shakePositions.forEach { target ->
                        errorShakeY.animateTo(target, tween(100))
                    }
                }

                is PinCheckResult.LockedOut -> {
                    message = null
                    shakePositions.forEach { target ->
                        errorShakeY.animateTo(target, tween(100))
                    }
                }
            }
            pin = ""
            isChecking = false
        }
    }

    val shownMessage = if (isLockedOut) {
        val seconds = ((lockoutUntil - now + 999) / 1_000).coerceAtLeast(1)
        stringResource(R.string.try_again_in, DateUtils.formatElapsedTime(seconds))
    } else message

    PinEntryLayout(
        modifier = modifier.graphicsLayer { translationX = errorShakeY.value },
        title = title,
        description = description,
        pinLength = pin.length,
        message = shownMessage,
        padEnabled = !isChecking && !isLockedOut,
        canConfirm = pin.length >= AppLockManager.MIN_PIN_LENGTH,
        onDigit = { digit ->
            if (pin.length < AppLockManager.MAX_PIN_LENGTH) {
                pin += digit
                message = null
            }
        },
        onDelete = { pin = pin.dropLast(1) },
        onConfirm = ::submit,
        aboveKeypad = {
            AnimatedVisibility(
                visible = canUseBiometrics,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                FilledTonalButton(
                    onClick = {
                        biometricLauncher.launch(
                            AuthenticationRequest.biometricRequest(
                                title = title,
                                AuthenticationRequest.Biometric.Fallback.CustomOption(resources.getString(R.string.use_pin))
                            ) {
                                setMinStrength(AuthenticationRequest.Biometric.Strength.Class2)
                                setIsConfirmationRequired(false)
                            }
                        )
                    },
                    enabled = !isChecking && !isLockedOut,
                    shapes = ButtonDefaults.shapes()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.fingerprint),
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(
                        text = stringResource(R.string.use_biometrics),
                        style = MaterialTheme.typography.labelLargeEmphasized
                    )
                }
            }
        },
        footer = footer
    )
}

/**
 * Asks for a new PIN twice, then calls [onPinCreated] with it.
 */
@Composable
fun PinCreation(
    onPinCreated: (String) -> Unit,
    modifier: Modifier = Modifier,
    footer: @Composable ColumnScope.() -> Unit = {}
) {
    var firstPin by remember { mutableStateOf<String?>(null) }
    var pin by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    val mismatch = stringResource(R.string.pins_dont_match)
    val tooShort = stringResource(R.string.pin_too_short)
    val isConfirming = firstPin != null
    val shakePositions = listOf(-24f, 20f, -14f, 8f, 0f)
    val errorShakeY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()


    PinEntryLayout(
        modifier = modifier.graphicsLayer { translationX = errorShakeY.value },
        title = stringResource(if (isConfirming) R.string.confirm_pin else R.string.create_pin),
        description = stringResource(if (isConfirming) R.string.confirm_pin_desc else R.string.create_pin_desc),
        pinLength = pin.length,
        message = message,
        padEnabled = true,
        canConfirm = pin.length >= AppLockManager.MIN_PIN_LENGTH,
        onDigit = { digit ->
            if (pin.length < AppLockManager.MAX_PIN_LENGTH) {
                pin += digit
                message = null
            }
        },
        onDelete = { pin = pin.dropLast(1) },
        onConfirm = {
            scope.launch {
                when {
                    pin.length < AppLockManager.MIN_PIN_LENGTH -> {
                        message = tooShort
                        shakePositions.forEach { target ->
                            errorShakeY.animateTo(target, tween(100))
                        }
                    }

                    firstPin == null -> {
                        firstPin = pin
                        pin = ""
                    }

                    firstPin == pin -> onPinCreated(pin)

                    else -> {
                        firstPin = null
                        pin = ""
                        message = mismatch
                        shakePositions.forEach { target ->
                            errorShakeY.animateTo(target, tween(100))
                        }
                    }
                }
            }
        },
        footer = footer
    )
}

@Composable
private fun PinEntryLayout(
    title: String,
    description: String,
    pinLength: Int,
    message: String?,
    padEnabled: Boolean,
    canConfirm: Boolean,
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    aboveKeypad: @Composable ColumnScope.() -> Unit = {},
    footer: @Composable ColumnScope.() -> Unit = {}
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Spacer(Modifier.weight(1f))
            AnimatedContent(
                targetState = title to description,
                transitionSpec = { fadeIn() togetherWith fadeOut() }
            ) { (title, description) ->
                CuteHero(
                    icon = R.drawable.lock_filled,
                    title = title,
                    description = description,
                    size = 96.dp
                )
            }
            Spacer(Modifier.height(24.dp))
            CutePinDots(
                length = pinLength,
                maxLength = AppLockManager.MAX_PIN_LENGTH
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp)
            ) {
                this@Column.AnimatedVisibility(
                    visible = message != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    message?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMediumEmphasized.copy(
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                aboveKeypad()
                CutePinPad(
                    onDigit = onDigit,
                    onDelete = onDelete,
                    onConfirm = onConfirm,
                    enabled = padEnabled,
                    canDelete = pinLength > 0,
                    canConfirm = canConfirm
                )
                footer()
            }
        }
    }
}
