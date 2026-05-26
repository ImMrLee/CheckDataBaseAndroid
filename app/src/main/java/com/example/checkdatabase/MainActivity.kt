package com.example.checkdatabase

import android.os.AsyncTask
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.checkdatabase.ui.InactiveUsersScreen
import com.example.checkdatabase.ui.UserListScreen
import com.example.checkdatabase.ui.UserDetailScreen
import com.example.checkdatabase.ui.theme.CheckInTestTheme
import java.sql.Connection
import java.sql.DriverManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CheckInTestTheme {
                val navController = rememberNavController()
                var records by remember { mutableStateOf(listOf<CheckinRecord>()) }
                var isLoading by remember { mutableStateOf(true) }
                var isRefreshing by remember { mutableStateOf(false) }
                var errorMessage by remember { mutableStateOf<String?>(null) }

                fun loadData(onComplete: (() -> Unit)? = null) {
                    DatabaseHelper().getAllCheckins { result ->
                        records = result
                        errorMessage = null
                        isLoading = false
                        isRefreshing = false
                        onComplete?.invoke()
                    }
                }

                LaunchedEffect(Unit) {
                    loadData()
                }

                NavHost(
                    navController = navController,
                    startDestination = "admin"
                ) {
                    composable("admin") {
                        AdminScreen(
                            records = records,
                            isLoading = isLoading,
                            isRefreshing = isRefreshing,
                            errorMessage = errorMessage,
                            onRefresh = {
                                isRefreshing = true
                                loadData()
                            },
                            onNavigateToUserList = {
                                navController.navigate("user_list")
                            },
                            onNavigateToInactiveUsers = {
                                navController.navigate("inactive_users")
                            }
                        )
                    }

                    composable("user_list") {
                        UserListScreen(
                            records = records,
                            onNavigateToUserDetail = { userName ->
                                navController.navigate("user_detail/$userName")
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("user_detail/{userName}") { backStackEntry ->
                        val userName = backStackEntry.arguments?.getString("userName") ?: ""
                        UserDetailScreen(
                            userName = userName,
                            records = records,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("inactive_users") {
                        InactiveUsersScreen(
                            onBack = { navController.popBackStack() },
                            onNavigateToUserDetail = { userName ->
                                navController.navigate("user_detail/$userName")
                            }
                        )
                    }
                }
            }
        }
    }
}

data class CheckinRecord(
    val id: Int,
    val userName: String,
    val phoneNumber: String,
    val age: Int,
    val gender: String,
    val checkinTime: String,
    val latitude: Double,
    val longitude: Double,
    val city: String,
    val address: String
)
data class UserStats(
    val name: String,
    val phoneNumber: String,
    val age: Int,
    val gender: String,
    val totalCheckins: Int,
    val lastCheckinTime: String,
    val lastCheckinLocation: String
)
class DatabaseHelper {
    companion object {
        private const val HOST = "124.223.118.98"  // 远程测试
        private const val PORT = "3306"
        private const val DATABASE = "checkin_db"
        private const val USER = "app_user"
        private const val XOR_KEY = 0x55
        private val ENCRYPTED_PASSWORD = byteArrayOf(101)

        private val PASSWORD: String by lazy {
            val decryptedBytes = ENCRYPTED_PASSWORD.map { (it.toInt() xor XOR_KEY).toByte() }.toByteArray()
            String(decryptedBytes, Charsets.UTF_8)
        }

        private const val URL = "jdbc:mysql://$HOST:$PORT/$DATABASE?useSSL=false&serverTimezone=Asia/Shanghai"
    }

    fun getAllCheckins(callback: (List<CheckinRecord>) -> Unit) {
        QueryTask(callback).execute()
    }

    private class QueryTask(private val callback: (List<CheckinRecord>) -> Unit) :
        AsyncTask<Void, Void, List<CheckinRecord>>() {

        override fun doInBackground(vararg params: Void?): List<CheckinRecord> {
            val records = mutableListOf<CheckinRecord>()
            var connection: Connection? = null

            return try {
                Class.forName("com.mysql.jdbc.Driver")
                connection = DriverManager.getConnection(URL, USER, PASSWORD)

                val statement = connection.createStatement()
                val resultSet = statement.executeQuery(
                    "SELECT * FROM checkin_records ORDER BY checkin_time DESC LIMIT 200"
                )

                while (resultSet.next()) {
                    records.add(
                        CheckinRecord(
                            id = resultSet.getInt("id"),
                            userName = resultSet.getString("user_name"),
                            phoneNumber = resultSet.getString("phone_number"),
                            age = resultSet.getInt("age"),
                            gender = resultSet.getString("gender"),
                            checkinTime = resultSet.getString("checkin_time"),
                            latitude = resultSet.getDouble("latitude"),
                            longitude = resultSet.getDouble("longitude"),
                            city = resultSet.getString("city"),
                            address = resultSet.getString("address")
                        )
                    )
                }
                records
            } catch (e: Exception) {
                e.printStackTrace()
                records
            } finally {
                connection?.close()
            }
        }

        override fun onPostExecute(result: List<CheckinRecord>) {
            callback(result)
        }
    }
    fun getInactiveUsers(days: Int = 1, callback: (List<InactiveUser>) -> Unit) {
        InactiveUserTask(callback, days).execute()
    }

    data class InactiveUser(
        val userName: String,
        val phoneNumber: String,
        val lastCheckinTime: String,
        val lastCheckinCity: String,
        val inactiveDays: Int
    )

    private class InactiveUserTask(
        private val callback: (List<InactiveUser>) -> Unit,
        private val days: Int
    ) : AsyncTask<Void, Void, List<InactiveUser>>() {

        override fun doInBackground(vararg params: Void?): List<InactiveUser> {
            val inactiveUsers = mutableListOf<InactiveUser>()
            var connection: java.sql.Connection? = null

            return try {
                Class.forName("com.mysql.jdbc.Driver")
                connection = DriverManager.getConnection(URL, USER, PASSWORD)

                // 查询每个用户的最后一次打卡时间
                val sql = """
                SELECT 
                    user_name, 
                    phone_number, 
                    MAX(checkin_time) as last_checkin_time,
                    SUBSTRING_INDEX(GROUP_CONCAT(city ORDER BY checkin_time DESC), ',', 1) as last_city
                FROM checkin_records 
                GROUP BY user_name, phone_number
                HAVING last_checkin_time < DATE_SUB(NOW(), INTERVAL $days DAY)
                ORDER BY last_checkin_time ASC
            """.trimIndent()

                val statement = connection.createStatement()
                val resultSet = statement.executeQuery(sql)

                while (resultSet.next()) {
                    val lastCheckinTime = resultSet.getString("last_checkin_time")
                    val inactiveDays = calculateInactiveDays(lastCheckinTime)

                    inactiveUsers.add(
                        InactiveUser(
                            userName = resultSet.getString("user_name"),
                            phoneNumber = resultSet.getString("phone_number"),
                            lastCheckinTime = lastCheckinTime,
                            lastCheckinCity = resultSet.getString("last_city"),
                            inactiveDays = inactiveDays
                        )
                    )
                }
                inactiveUsers
            } catch (e: Exception) {
                e.printStackTrace()
                inactiveUsers
            } finally {
                connection?.close()
            }
        }

        private fun calculateInactiveDays(lastCheckinTime: String): Int {
            return try {
                val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val lastTime = format.parse(lastCheckinTime)
                val now = Date()
                val diffInMillis = now.time - lastTime.time
                (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
            } catch (e: Exception) {
                0
            }
        }

        override fun onPostExecute(result: List<InactiveUser>) {
            callback(result)
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    records: List<CheckinRecord>,
    isLoading: Boolean,
    isRefreshing: Boolean,
    errorMessage: String?,
    onRefresh: () -> Unit,
    onNavigateToUserList: () -> Unit,
    onNavigateToInactiveUsers: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("打卡数据查看") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            state = rememberPullToRefreshState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                // 统计卡片
                if (!isLoading && records.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                StatItem("总打卡数", records.size)
                                StatItemClickable(
                                    title = "总人数",
                                    value = records.distinctBy { it.userName }.size,
                                    onClick = onNavigateToUserList
                                )
                            }

                            Divider()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = onNavigateToInactiveUsers,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("未打卡人员")
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 错误提示
                if (errorMessage != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = "连接失败: $errorMessage",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 首次加载中
                if (isLoading && records.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                // 打卡记录列表
                if (!isLoading && records.isNotEmpty()) {
                    Text(
                        text = "最新打卡记录",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(records) { record ->
                            CheckinListItem(record = record)
                        }
                    }
                }

                // 无数据
                if (!isLoading && records.isEmpty() && errorMessage == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("暂无打卡记录")
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(title: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = title,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun StatItemClickable(title: String, value: Int, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Text(
            text = value.toString(),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = title,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
@Composable
fun HistoryCard(record: CheckinRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = record.checkinTime,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = record.city,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = record.address,
                fontSize = 12.sp,
                maxLines = 2
            )
            Text(
                text = "${String.format("%.4f", record.latitude)}, ${String.format("%.4f", record.longitude)}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun CheckinListItem(record: CheckinRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = record.userName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = record.checkinTime,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${record.city} ${record.address.take(30)}",
                fontSize = 13.sp
            )

            Text(
                text = "${record.phoneNumber}  |  ${record.age}岁  |  ${record.gender}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "${String.format("%.4f", record.latitude)}, ${String.format("%.4f", record.longitude)}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}