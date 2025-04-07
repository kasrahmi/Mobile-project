package com.example.notable

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.notable.login.Login
import com.example.notable.onboarding.Onboarding
import com.example.notable.ui.theme.NotableTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NotableTheme {
                Onboarding(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        Toast.makeText(
                            applicationContext,
                            "Run Ahmadi",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }
    }
}
