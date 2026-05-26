package com.example.checkdatabase.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.checkdatabase.DatabaseHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InactiveUsersScreen(
    onBack: () -> Unit,
    onNavigateToUserDetail: (String) -> Unit
) {
    val context = LocalContext.current
    val dbHelper = DatabaseHelper()
    var inactiveUsers by remember { mutableStateOf(listOf<DatabaseHelper.InactiveUser>()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            dbHelper.getInactiveUsers(days = 1) { users ->
                inactiveUsers = users
                isLoading = false
            }
        } catch (e: Exception) {
            errorMessage = e.message
            isLoading = false
        }
    }

    val filteredUsers = remember(inactiveUsers, searchQuery) {
        if (searchQuery.isBlank()) {
            inactiveUsers
        } else {
            inactiveUsers.filter { user ->
                user.userName.contains(searchQuery, ignoreCase = true) ||
                        user.phoneNumber.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("超过一天未打卡人员") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("搜索姓名或手机号") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "清除"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp)
            )

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("加载失败: $errorMessage")
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = {
                                isLoading = true
                                errorMessage = null
                                dbHelper.getInactiveUsers(days = 1) { users ->
                                    inactiveUsers = users
                                    isLoading = false
                                }
                            }) {
                                Text("重试")
                            }
                        }
                    }
                }

                filteredUsers.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (searchQuery.isNotEmpty()) Icons.Default.Search else Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "未找到匹配的成员" else "无人缺席！！！",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                else -> {
                    Column {
                        if (searchQuery.isNotEmpty()) {
                            Text(
                                text = "找到 ${filteredUsers.size} 位未打卡成员",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    )
                                ) {
                                    Text(
                                        text = "注意：共有 ${filteredUsers.size} 人超过1天未打卡",
                                        modifier = Modifier.padding(16.dp),
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            items(filteredUsers) { user ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onNavigateToUserDetail(user.userName) },
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = user.userName,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp
                                                )
                                                Text(
                                                    text = "${user.inactiveDays}天未打卡",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.error,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Text(
                                                text = " ${user.phoneNumber}",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "最后一次打卡: ${user.lastCheckinTime}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                            Text(
                                                text = " ${user.lastCheckinCity}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                                    data = Uri.parse("tel:${user.phoneNumber}")
                                                }
                                                context.startActivity(intent)
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Phone,
                                                contentDescription = "拨打电话",
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}