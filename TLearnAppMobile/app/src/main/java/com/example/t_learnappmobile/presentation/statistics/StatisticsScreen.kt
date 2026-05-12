// presentation/statistics/StatisticsScreen.kt
package com.example.t_learnappmobile.presentation.statistics

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.t_learnappmobile.domain.model.DailyStats
import com.example.t_learnappmobile.domain.model.LeaderboardPlayer
import com.example.t_learnappmobile.presentation.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel,
    onClose: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refreshStats()
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Статистика", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(uiState.dictionaryName, fontSize = 14.sp, color = YellowPrimary)
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Общая статистика", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            StatItem(uiState.newWords.toString(), "Новые", BlueColor)
                            StatItem(uiState.inProgressWords.toString(), "В процессе", YellowPrimary)
                            StatItem(uiState.learnedWords.toString(), "Выучено", Color(0xFF4CAF50))
                        }
                    }
                }
            }

            item {
                WeeklyStatsCard(uiState.weeklyStats, uiState.currentWeekOffset, viewModel)
            }

            item {
                LeaderboardCard(uiState)
            }

            item {
                UserPositionCard(uiState)
            }
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable(enabled = false) { },
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = YellowPrimary)
            }
        }
    }
}

@Composable
fun WeeklyStatsCard(
    weeklyStats: List<DailyStats>,
    currentWeekOffset: Int,
    viewModel: StatisticsViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.previousWeek() }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.ArrowBack, "Предыдущая неделя", tint = BlueColor)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Игры за неделю", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(getWeekLabel(currentWeekOffset), fontSize = 12.sp, color = MediumGray)
                }
                IconButton(
                    onClick = { viewModel.nextWeek() },
                    modifier = Modifier.size(40.dp),
                    enabled = currentWeekOffset < 0
                ) {
                    Icon(
                        Icons.Default.ArrowForward,
                        "Следующая неделя",
                        tint = if (currentWeekOffset < 0) BlueColor else LightGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (weeklyStats.isEmpty() || weeklyStats.all { it.gamesPlayed == 0 }) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text("Нет игр за эту неделю", color = MediumGray)
                }
            } else {
                WeeklyGamesChart(
                    weeklyStats = weeklyStats,
                    currentWeekOffset = currentWeekOffset
                )
            }
        }
    }
}

@Composable
fun LeaderboardCard(uiState: StatisticsUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("🏆 Таблица лидеров", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.leaderboard.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text("Нет данных", color = MediumGray)
                }
            } else {
                TopThreeLeaders(uiState.leaderboard.take(3))

                Spacer(modifier = Modifier.height(12.dp))

                val otherPlayers = uiState.leaderboard.drop(3)
                if (otherPlayers.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().height(400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(otherPlayers) { player ->
                            LeaderboardItem(
                                player = player,
                                isCurrentUser = player.id == uiState.yourUserId
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserPositionCard(uiState: StatisticsUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = YellowPrimary.copy(alpha = 0.15f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(YellowPrimary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "#${uiState.yourPosition?.position ?: "-"}",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("${uiState.firstName} ${uiState.lastName}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Вы", fontSize = 12.sp, color = MediumGray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${uiState.yourPosition?.score ?: uiState.yourGameScore}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = YellowPrimary
                )
                Text("очков", fontSize = 12.sp, color = MediumGray)
            }
        }
    }
}

fun getWeekLabel(weekOffset: Int): String {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)

    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val daysToMonday = if (dayOfWeek == Calendar.SUNDAY) -6 else Calendar.MONDAY - dayOfWeek
    calendar.add(Calendar.DAY_OF_YEAR, daysToMonday)
    calendar.add(Calendar.WEEK_OF_YEAR, weekOffset)

    val startDate = calendar.time

    val endCalendar = calendar.clone() as Calendar
    endCalendar.add(Calendar.DAY_OF_YEAR, 6)
    val endDate = endCalendar.time

    val dateFormat = SimpleDateFormat("dd.MM", Locale("ru"))
    return "${dateFormat.format(startDate)} - ${dateFormat.format(endDate)}"
}

@Composable
fun WeeklyGamesChart(
    weeklyStats: List<DailyStats>,
    currentWeekOffset: Int
) {
    val data = weeklyStats.map { it.gamesPlayed }
    val scores = weeklyStats.map { it.totalScore }
    val maxValue = data.maxOrNull()?.coerceAtLeast(1) ?: 1

    val dates = getWeekDates(currentWeekOffset)
    val dayLabels = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

    Row(
        modifier = Modifier.fillMaxWidth().height(240.dp).padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEachIndexed { index, value ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(42.dp)
            ) {
                Box(
                    modifier = Modifier.height(150.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(((150f * value / maxValue).dp).coerceAtLeast(if (value > 0) 4.dp else 0.dp))
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(BlueColor)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (scores.getOrNull(index) ?: 0 > 0) "${scores[index]}" else "",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = YellowPrimary,
                    maxLines = 1
                )

                Text(
                    text = dayLabels.getOrNull(index) ?: "",
                    fontSize = 11.sp,
                    color = MediumGray,
                    maxLines = 1
                )

                Text(
                    text = dates.getOrNull(index) ?: "",
                    fontSize = 8.sp,
                    color = MediumGray.copy(alpha = 0.7f),
                    maxLines = 1
                )
            }
        }
    }
}

fun getWeekDates(weekOffset: Int): List<String> {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)

    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val daysToMonday = if (dayOfWeek == Calendar.SUNDAY) -6 else Calendar.MONDAY - dayOfWeek
    calendar.add(Calendar.DAY_OF_YEAR, daysToMonday)
    calendar.add(Calendar.WEEK_OF_YEAR, weekOffset)

    val dateFormat = SimpleDateFormat("dd.MM", Locale("ru"))
    val dates = mutableListOf<String>()

    val baseTime = calendar.timeInMillis
    for (i in 0 until 7) {
        calendar.timeInMillis = baseTime
        calendar.add(Calendar.DAY_OF_YEAR, i)
        dates.add(dateFormat.format(calendar.time))
    }

    return dates
}

@Composable
fun TopThreeLeaders(players: List<LeaderboardPlayer>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        if (players.size >= 2) LeaderPodium(players[1], 2, 100.dp, Color(0xFFC0C0C0))
        else Spacer(Modifier.width(100.dp))

        if (players.isNotEmpty()) LeaderPodium(players[0], 1, 130.dp, YellowPrimary)
        else Spacer(Modifier.width(100.dp))

        if (players.size >= 3) LeaderPodium(players[2], 3, 80.dp, Color(0xFFCD7F32))
        else Spacer(Modifier.width(100.dp))
    }
}

@Composable
fun LeaderPodium(
    player: LeaderboardPlayer,
    position: Int,
    height: androidx.compose.ui.unit.Dp,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier.width(100.dp)
    ) {
        Text(
            player.name.take(10),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(color),
            contentAlignment = Alignment.Center
        ) {
            Text("$position", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(color.copy(alpha = 0.3f)),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                "${player.score}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun LeaderboardItem(player: LeaderboardPlayer, isCurrentUser: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentUser) YellowPrimary.copy(alpha = 0.1f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrentUser) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "#${player.position}",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isCurrentUser) YellowPrimary else LightGray)
                    .wrapContentSize(Alignment.Center),
                fontWeight = FontWeight.Bold,
                color = if (isCurrentUser) Color.Black else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = if (isCurrentUser) YellowPrimary else MediumGray,
                modifier = Modifier.size(24.dp)
            )
            Text(
                player.name,
                fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            )
            Text(
                "${player.score}",
                fontWeight = FontWeight.Bold,
                color = YellowPrimary,
                fontSize = 18.sp
            )
        }
    }
}

@Composable
fun StatItem(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 12.sp, color = MediumGray)
    }
}

@Preview(showBackground = true, showSystemUi = true, widthDp = 360, heightDp = 720)
@Composable
fun StatisticsScreenPreview() {
    TLearnAppMobileTheme {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Statistics Preview")
        }
    }
}