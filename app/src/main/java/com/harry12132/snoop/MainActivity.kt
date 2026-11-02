package com.harry12132.snoop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SnoopTheme {
                SnoopHome()
            }
        }
    }
}

@Composable
private fun SnoopTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color(0xFF090A0C),
            surface = Color(0xFF141619),
            primary = Color(0xFFE8EAED),
            onBackground = Color(0xFFF4F5F6),
            onSurface = Color(0xFFF4F5F6)
        ),
        content = content
    )
}

@Composable
private fun SnoopHome() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 36.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "snoop",
                color = Color.White,
                fontSize = 42.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-2).sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Everything worth a glance.",
                color = Color(0xFFB4BAC2),
                fontSize = 17.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Your Snoop space is getting ready.",
                modifier = Modifier
                    .background(
                        color = Color(0xFF171A1E),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                color = Color(0xFFE3E6EA),
                fontSize = 14.sp
            )
        }
    }
}
