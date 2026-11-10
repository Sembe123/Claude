package io.github.sembe123.punktjaeger

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import io.github.sembe123.punktjaeger.ui.PunktjaegerApp
import io.github.sembe123.punktjaeger.ui.PunktjaegerTheme

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Der Hintergrund ist immer dunkel, daher helle Symbole in Status- und Navigationsleiste.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            PunktjaegerTheme {
                PunktjaegerApp(viewModel)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Wenn die App in den Hintergrund geht (Anruf, Home-Taste ...), Spiel pausieren.
        viewModel.pause()
    }
}
