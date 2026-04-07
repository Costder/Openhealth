package com.openhealthbridge.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HealthConnectStatusScreen()
                }
            }
        }
    }
}

@Composable
fun HealthConnectStatusScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val healthConnectClient = remember {
        try {
            HealthConnectClient.getOrCreate(context)
        } catch (e: Exception) {
            null
        }
    }

    var permissionsGranted by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showResults by remember { mutableStateOf(false) }

    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(HeightRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(NutritionRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(MenstruationFlowRecord::class),
        HealthPermission.getReadPermission(MenstruationPeriodRecord::class),
        HealthPermission.getReadPermission(BasalBodyTemperatureRecord::class),
        HealthPermission.getReadPermission(OvulationTestRecord::class),
        HealthPermission.getReadPermission(CervicalMucusRecord::class),
        HealthPermission.getReadPermission(IntermenstrualBleedingRecord::class),
        HealthPermission.getReadPermission(SexualActivityRecord::class)
    )

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        permissionsGranted = granted
        showResults = true
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!showResults) {
            Button(onClick = {
                if (healthConnectClient != null) {
                    scope.launch {
                        val granted = healthConnectClient.permissionController.getGrantedPermissions()
                        if (granted.containsAll(permissions)) {
                            permissionsGranted = granted
                            showResults = true
                        } else {
                            requestPermissionLauncher.launch(permissions)
                        }
                    }
                }
            }) {
                Text("Test Health Connect")
            }
        } else {
            Text(
                text = "Health Connect Status",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(16.dp)
            )

            val statusList = listOf(
                "Steps" to HealthPermission.getReadPermission(StepsRecord::class),
                "Weight" to HealthPermission.getReadPermission(WeightRecord::class),
                "Height" to HealthPermission.getReadPermission(HeightRecord::class),
                "Calories" to HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
                "Sleep" to HealthPermission.getReadPermission(SleepSessionRecord::class),
                "Nutrition" to HealthPermission.getReadPermission(NutritionRecord::class),
                "Heart Rate" to HealthPermission.getReadPermission(HeartRateRecord::class),
                "Cycle" to HealthPermission.getReadPermission(MenstruationFlowRecord::class),
                "BBT" to HealthPermission.getReadPermission(BasalBodyTemperatureRecord::class),
                "Ovulation" to HealthPermission.getReadPermission(OvulationTestRecord::class),
                "Cervical Mucus" to HealthPermission.getReadPermission(CervicalMucusRecord::class),
                "Spotting" to HealthPermission.getReadPermission(IntermenstrualBleedingRecord::class),
                "Sexual Activity" to HealthPermission.getReadPermission(SexualActivityRecord::class)
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                items(statusList) { (name, permission) ->
                    val isGranted = permissionsGranted.contains(permission)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = name, style = MaterialTheme.typography.bodyLarge)
                        Icon(
                            imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = if (isGranted) "Granted" else "Denied",
                            tint = if (isGranted) Color.Green else Color.Red
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = { showResults = false }) {
                Text("Back")
            }
        }
    }
}
