package com.example.notable.view.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview


@Composable
fun ChangePasswordScreen(
    currentPassword: String = "",
    newPassword: String = "",
    retypePassword: String = "",
    onBackClick: () -> Unit = {},
    onCurrentPasswordChange: (String) -> Unit = {},
    onNewPasswordChange: (String) -> Unit = {},
    onRetypePasswordChange: (String) -> Unit = {},
    onSubmitClick: () -> Unit = {},
    isLoading: Boolean = false
) {
    var currentPass by remember { mutableStateOf(currentPassword) }
    var newPass by remember { mutableStateOf(newPassword) }
    var retypePass by remember { mutableStateOf(retypePassword) }
    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var retypePasswordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        // Status bar spacer
        Spacer(modifier = Modifier.height(24.dp))

        // Top bar with back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.clickable { onBackClick() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF6366F1),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Back",
                    color = Color(0xFF6366F1),
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Change Password",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )

            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Instructions
        Text(
            text = "Please input your current password first",
            fontSize = 14.sp,
            color = Color(0xFF6366F1),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Current Password section
        Column {
            Text(
                text = "Current Password",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = currentPass,
                onValueChange = {
                    currentPass = it
                    onCurrentPasswordChange(it)
                },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (currentPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { currentPasswordVisible = !currentPasswordVisible }) {
//                        Icon(
//                            imageVector = if (currentPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
//                            contentDescription = if (currentPasswordVisible) "Hide password" else "Show password",
//                            tint = Color.Gray
//                        )
                    }
                },
                placeholder = {
                    Text(
                        text = "••••••••••",
                        color = Color.Gray
                    )
                },
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6366F1),
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f)
                )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // New password instruction
        Text(
            text = "Now, create your new password",
            fontSize = 14.sp,
            color = Color(0xFF6366F1),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // New Password section
        Column {
            Text(
                text = "New Password",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = newPass,
                onValueChange = {
                    newPass = it
                    onNewPasswordChange(it)
                },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
//                        Icon(
//                            imageVector = if (newPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
//                            contentDescription = if (newPasswordVisible) "Hide password" else "Show password",
//                            tint = Color.Gray
//                        )
                    }
                },
                placeholder = {
                    Text(
                        text = "••••••••••",
                        color = Color.Gray
                    )
                },
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6366F1),
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f)
                )
            )

            Text(
                text = "Password should contain a-z, A-Z, 0-9",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Retype Password section
        Column {
            Text(
                text = "Retype New Password",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = retypePass,
                onValueChange = {
                    retypePass = it
                    onRetypePasswordChange(it)
                },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (retypePasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { retypePasswordVisible = !retypePasswordVisible }) {
//                        Icon(
//                            imageVector = if (retypePasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
//                            contentDescription = if (retypePasswordVisible) "Hide password" else "Show password",
//                            tint = Color.Gray
//                        )
                    }
                },
                placeholder = {
                    Text(
                        text = "••••••••••",
                        color = Color.Gray
                    )
                },
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6366F1),
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f)
                ),
                isError = newPass.isNotEmpty() && retypePass.isNotEmpty() && newPass != retypePass
            )

            if (newPass.isNotEmpty() && retypePass.isNotEmpty() && newPass != retypePass) {
                Text(
                    text = "Passwords do not match",
                    fontSize = 12.sp,
                    color = Color.Red,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Submit button
        Button(
            onClick = onSubmitClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = currentPass.isNotEmpty() &&
                    newPass.isNotEmpty() &&
                    retypePass.isNotEmpty() &&
                    newPass == retypePass &&
                    !isLoading,
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6366F1),
                disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = "Submit New Password",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Submit",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ChangePasswordScreenPreview() {
    ChangePasswordScreen()
}