package com.example.minlish

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch
import java.util.Calendar

class DashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // ── Toolbar nút back ← không hiện tên ────────────────────
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val tvStreak       = findViewById<TextView>(R.id.tvStreak)
        val tvDueToday     = findViewById<TextView>(R.id.tvDueToday)
        val tvTotalReviews = findViewById<TextView>(R.id.tvTotalReviews)
        val tvAccuracy     = findViewById<TextView>(R.id.tvAccuracy)
        val tvTodayReviews = findViewById<TextView>(R.id.tvTodayReviews)
        val tvLevel        = findViewById<TextView>(R.id.tvLevel)
        val tvLevelDesc    = findViewById<TextView>(R.id.tvLevelDesc)
        val progressLevel  = findViewById<ProgressBar>(R.id.progressLevel)
        val barChart       = findViewById<BarChart>(R.id.barChart)
        val btnStartStudy  = findViewById<Button>(R.id.btnStartStudy)
        val tvRetentionRate = findViewById<TextView?>(R.id.tvRetentionRate)

        val db         = AppDatabase.getInstance(applicationContext)
        val sessionDao = db.studySessionDao()

        lifecycleScope.launch {
            tvRetentionRate?.let {
                val retained     = sessionDao.getRetainedWordsCount()
                val totalReviewed = sessionDao.getTotalReviewedWordsCount()
                val retentionRate = if (totalReviewed > 0) retained * 100 / totalReviewed else 0
                it.text = "$retentionRate%"
            }

            val now          = System.currentTimeMillis()
            val dueCount     = sessionDao.getDueCount(now)
            val totalReviews = sessionDao.getTotalReviews()
            val correctCount = sessionDao.getCorrectCount()
            val accuracy     = if (totalReviews == 0) 0 else (correctCount * 100) / totalReviews

            val startOfDay   = getStartOfDay(now)
            val endOfDay     = startOfDay + 24 * 60 * 60 * 1000L
            val todayReviews = sessionDao.getWordsReviewedOnDay(startOfDay, endOfDay)

            val allTimes  = sessionDao.getAllReviewTimes()
            val streak    = calculateStreak(allTimes)
            val levelInfo = estimateLevel(totalReviews)

            val last7Days = mutableListOf<Int>()
            val dayLabels = mutableListOf<String>()
            val cal       = Calendar.getInstance()

            for (i in 6 downTo 0) {
                cal.timeInMillis = now
                cal.add(Calendar.DAY_OF_YEAR, -i)
                val dayStart = getStartOfDay(cal.timeInMillis)
                val dayEnd   = dayStart + 24 * 60 * 60 * 1000L
                val count    = sessionDao.getWordsReviewedOnDay(dayStart, dayEnd)
                last7Days.add(count)

                val label = when (i) {
                    0    -> "Hôm nay"
                    1    -> "Hôm qua"
                    else -> {
                        val day   = cal.get(Calendar.DAY_OF_MONTH)
                        val month = cal.get(Calendar.MONTH) + 1
                        "$day/$month"
                    }
                }
                dayLabels.add(label)
            }

            tvStreak.text       = "$streak ngày liên tiếp"
            tvDueToday.text     = "$dueCount từ"
            tvTotalReviews.text = "$totalReviews lượt"
            tvAccuracy.text     = "$accuracy%"
            tvTodayReviews.text = "$todayReviews từ"
            tvLevel.text        = levelInfo.name
            tvLevelDesc.text    = "$totalReviews / ${levelInfo.nextTarget} từ để lên level tiếp theo"

            val prevTarget = when {
                totalReviews < 100  -> 0
                totalReviews < 300  -> 100
                totalReviews < 600  -> 300
                totalReviews < 1000 -> 600
                totalReviews < 1500 -> 1000
                totalReviews < 2500 -> 1500
                else                -> 2500
            }
            progressLevel.max      = levelInfo.nextTarget - prevTarget
            progressLevel.progress = (totalReviews - prevTarget).coerceAtLeast(0)

            setupBarChart(barChart, last7Days, dayLabels)
        }

        btnStartStudy.setOnClickListener {
            startActivity(Intent(this, FlashcardActivity::class.java))
        }
    }

    data class LevelInfo(val name: String, val nextTarget: Int)

    private fun estimateLevel(totalReviews: Int): LevelInfo = when {
        totalReviews < 100  -> LevelInfo("🌱 Beginner", 100)
        totalReviews < 300  -> LevelInfo("📖 Elementary", 300)
        totalReviews < 600  -> LevelInfo("✏️ Pre-Intermediate", 600)
        totalReviews < 1000 -> LevelInfo("📝 Intermediate", 1000)
        totalReviews < 1500 -> LevelInfo("🎓 Upper-Intermediate", 1500)
        totalReviews < 2500 -> LevelInfo("🏅 Advanced", 2500)
        else                -> LevelInfo("🏆 Master", 2500)
    }

    private fun setupBarChart(chart: BarChart, data: List<Int>, labels: List<String>) {
        val entries = data.mapIndexed { index, value -> BarEntry(index.toFloat(), value.toFloat()) }
        val dataSet = BarDataSet(entries, "Số từ ôn").apply {
            color          = Color.parseColor("#1565C0")
            valueTextColor = Color.BLACK
            valueTextSize  = 10f
        }
        chart.apply {
            this.data            = BarData(dataSet)
            description.isEnabled = false
            legend.isEnabled      = false
            setDrawGridBackground(false)
            setTouchEnabled(false)
            xAxis.apply {
                valueFormatter  = IndexAxisValueFormatter(labels)
                position        = XAxis.XAxisPosition.BOTTOM
                granularity     = 1f
                setDrawGridLines(false)
                textSize        = 9f
            }
            axisLeft.apply {
                granularity    = 1f
                axisMinimum    = 0f
                setDrawGridLines(true)
            }
            axisRight.isEnabled = false
            animateY(800)
            invalidate()
        }
    }

    private fun calculateStreak(reviewTimes: List<Long>): Int {
        if (reviewTimes.isEmpty()) return 0
        val reviewDays = reviewTimes.map { getStartOfDay(it) }.toSortedSet().toList().reversed()
        val todayStart     = getStartOfDay(System.currentTimeMillis())
        val yesterdayStart = todayStart - 24 * 60 * 60 * 1000L
        if (reviewDays.first() < yesterdayStart) return 0
        var streak = 1
        val oneDayMs = 24 * 60 * 60 * 1000L
        for (i in 1 until reviewDays.size) {
            if (reviewDays[i - 1] - reviewDays[i] == oneDayMs) streak++ else break
        }
        return streak
    }

    private fun getStartOfDay(timestamp: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0);      cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}