package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.room.Room
import com.example.data.SemaDatabase
import com.example.data.SemaRepository
import com.example.ui.SemaApp
import com.example.ui.SemaViewModel
import com.example.ui.SemaViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  
  private val database by lazy {
    Room.databaseBuilder(
      applicationContext,
      SemaDatabase::class.java,
      "sema_database"
    ).fallbackToDestructiveMigration()
     .build()
  }

  private val repository by lazy {
    SemaRepository(database.semaDao())
  }

  private val viewModel: SemaViewModel by viewModels {
    SemaViewModelFactory(application, repository)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          SemaApp(viewModel = viewModel)
        }
      }
    }
  }
}

