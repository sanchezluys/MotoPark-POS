package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.repository.ParkingRepository
import com.example.ui.MainApp
import com.example.ui.ParkingViewModel
import com.example.ui.ParkingViewModelFactory
import com.example.ui.theme.MyApplicationTheme
import com.example.util.AppUpdateController

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: ParkingViewModel
    private lateinit var appUpdateController: AppUpdateController

    private val appUpdateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        appUpdateController.onUpdateFlowResult(result.resultCode)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )

        appUpdateController = AppUpdateController(applicationContext).apply {
            attachLauncher(appUpdateLauncher)
        }

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ParkingRepository(database)
        val factory = ParkingViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[ParkingViewModel::class.java]

        setContent {
            MyApplicationTheme {
                MainApp(
                    viewModel = viewModel,
                    appUpdateController = appUpdateController
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        appUpdateController.checkForUpdates(userInitiated = false)
    }

    override fun onDestroy() {
        appUpdateController.dispose()
        super.onDestroy()
    }
}
