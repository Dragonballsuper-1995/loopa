package com.loopa.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loopa.viewmodel.AuthUiState
import com.loopa.viewmodel.AuthViewModel

@Composable
fun AuthScreen(viewModel: AuthViewModel, onAuthSuccess: () -> Unit, onGuestClick: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) onAuthSuccess()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Loopa.Base),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
                .padding(top = 80.dp, bottom = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ── Brand Logo ─────────────────────────────────────────────────
            Row {
                Text(
                    text = "loopa",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 64.sp,
                    color = Loopa.TextPrimary,
                    letterSpacing = (-2).sp,
                    lineHeight = 64.sp
                )
                Text(
                    text = ".",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 64.sp,
                    color = Loopa.Amber,
                    letterSpacing = 0.sp,
                    lineHeight = 64.sp
                )
            }

            Spacer(Modifier.height(8.dp))

            // Amber left-bar subtitle
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(16.dp)
                        .background(Loopa.Amber)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (isSignUp) "Create your account" else "Sign in to continue",
                    color = Loopa.TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(40.dp))

            // ── Email Field ────────────────────────────────────────────────
            LoopTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email",
                leadingIcon = {
                    Icon(Icons.Filled.Email, null, tint = Loopa.Amber, modifier = Modifier.size(18.dp))
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
            )

            Spacer(Modifier.height(14.dp))

            // ── Password Field ─────────────────────────────────────────────
            LoopTextField(
                value = password,
                onValueChange = {
                    password = it
                    if (isSignUp) passwordError = if (password == confirmPassword) null else "Passwords do not match"
                },
                label = "Password",
                leadingIcon = {
                    Icon(Icons.Filled.Lock, null, tint = Loopa.Amber, modifier = Modifier.size(18.dp))
                },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = if (isSignUp) ImeAction.Next else ImeAction.Done)
            )

            // ── Confirm Password (sign-up only) ────────────────────────────
            AnimatedVisibility(visible = isSignUp) {
                Column {
                    Spacer(Modifier.height(14.dp))
                    LoopTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            passwordError = if (password == confirmPassword) null else "Passwords do not match"
                        },
                        label = "Confirm Password",
                        leadingIcon = {
                            Icon(Icons.Filled.Lock, null, tint = Loopa.Amber, modifier = Modifier.size(18.dp))
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        isError = passwordError != null
                    )
                    if (passwordError != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = passwordError ?: "",
                            color = Loopa.Error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // ── Error Banner ───────────────────────────────────────────────
            if (uiState is AuthUiState.Error) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(Loopa.InputShape)
                        .background(Loopa.Error.copy(alpha = 0.12f))
                        .border(1.dp, Loopa.Error.copy(alpha = 0.4f), Loopa.InputShape)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = (uiState as AuthUiState.Error).message,
                        color = Loopa.Error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Primary CTA: Sign In / Sign Up ─────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Loopa.PillShape)
                    .background(
                        if (uiState is AuthUiState.Loading) Loopa.Raised
                        else Loopa.Amber
                    )
                    .clickable(
                        enabled = uiState !is AuthUiState.Loading
                                && email.isNotBlank()
                                && password.isNotBlank()
                                && (!isSignUp || confirmPassword.isNotBlank())
                    ) {
                        val trimmedEmail = email.trim()
                        if (isSignUp) {
                            if (password == confirmPassword) viewModel.signUpWithEmail(trimmedEmail, password)
                            else passwordError = "Passwords do not match"
                        } else {
                            viewModel.signInWithEmail(trimmedEmail, password)
                        }
                    }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (uiState is AuthUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Loopa.Amber)
                } else {
                    Text(
                        text = if (isSignUp) "Create Account" else "Sign In",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Loopa.Base
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Google Sign-In ─────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Loopa.PillShape)
                    .background(Loopa.Surface)
                    .border(1.dp, Loopa.BorderMd, Loopa.PillShape)
                    .clickable { viewModel.signInWithGoogle() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Continue with Google",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = Loopa.TextPrimary
                )
            }

            Spacer(Modifier.height(28.dp))

            // ── Toggle Sign In / Sign Up ───────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isSignUp = !isSignUp }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isSignUp) "Already have an account?  Sign In" else "Don't have an account?  Sign Up",
                    color = Loopa.Amber,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(4.dp))

            // ── Guest Mode ─────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onGuestClick)
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Continue as Guest",
                    color = Loopa.TextMuted,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ── Loopa Text Field ──────────────────────────────────────────────────────────
@Composable
fun LoopTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation =
        androidx.compose.ui.text.input.VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    isError: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        shape = Loopa.InputShape,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Loopa.Surface,
            focusedContainerColor   = Loopa.Surface,
            unfocusedBorderColor    = Loopa.Border,
            focusedBorderColor      = Loopa.Amber,
            errorBorderColor        = Loopa.Error,
            unfocusedTextColor      = Loopa.TextPrimary,
            focusedTextColor        = Loopa.TextPrimary,
            unfocusedLabelColor     = Loopa.TextMuted,
            focusedLabelColor       = Loopa.Amber,
            unfocusedLeadingIconColor = Loopa.TextMuted,
            focusedLeadingIconColor   = Loopa.Amber
        ),
        isError = isError
    )
}

// Compatibility shim
@Composable
fun TsugiTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation =
        androidx.compose.ui.text.input.VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    isError: Boolean = false
) = LoopTextField(
    value = value,
    onValueChange = onValueChange,
    label = label,
    modifier = modifier,
    leadingIcon = leadingIcon,
    visualTransformation = visualTransformation,
    keyboardOptions = keyboardOptions,
    isError = isError
)
