package com.automicosta.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.automicosta.app.ui.AutoMiCostaApp
import com.automicosta.app.ui.AutoMiCostaViewModel
import com.automicosta.app.ui.theme.AutoMiCostaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as AutoMiCostaApplication
        setContent {
            AutoMiCostaTheme {
                val vm: AutoMiCostaViewModel = viewModel(factory = AutoMiCostaViewModel.factory(app.database.dao()))
                AutoMiCostaApp(vm)
            }
        }
    }
}
