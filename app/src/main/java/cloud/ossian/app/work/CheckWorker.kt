package cloud.ossian.app.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import cloud.ossian.app.R
import cloud.ossian.app.data.Repository
import cloud.ossian.app.data.day
import cloud.ossian.app.data.describe
import cloud.ossian.app.data.headline
import cloud.ossian.app.data.matches
import cloud.ossian.app.data.parseRome
import cloud.ossian.app.ui.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

private const val CH_NEW = "nuovi"
private const val CH_DEADLINE = "scadenze"
private val ROME = ZoneId.of("Europe/Rome")

/** Every few hours: download the calls if they changed, notify new matches of each alert, and remind saved
 *  calls 7 days and 1 day before their deadline. Everything is computed here, on the phone. */
class CheckWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val repo = Repository(applicationContext)
        val fresh = runCatching { repo.refresh() }.getOrElse { return@withContext Result.retry() }
        val feed = fresh ?: repo.cached() ?: return@withContext Result.success()
        val now = LocalDateTime.now(ROME)
        val newest = feed.calls.maxOfOrNull { it.m } ?: ""
        repo.updateUser { user ->
            val alerts = user.alerts.map { alert ->
                // disabled alerts still move "since" forward, so turning one back on does not flood old calls
                val hits = if (!alert.enabled) emptyList()
                else feed.calls.filter { it.m > alert.since && alert.filter.matches(it) }
                if (hits.isNotEmpty()) {
                    val title = if (hits.size == 1) "Nuovo concorso: ${hits[0].headline()}" else "${hits.size} nuovi concorsi"
                    val text = alert.filter.describe(feed) + if (hits.size > 1) " · " + hits.take(3).joinToString(" · ") { it.headline() } else ""
                    notify(CH_NEW, alert.id.toInt(), title, text)
                }
                if (newest > alert.since) alert.copy(since = newest) else alert
            }
            val sent = user.notified.toMutableSet()
            for ((id, call) in user.saved) {
                val end = parseRome(call.s) ?: continue
                val days = ChronoUnit.DAYS.between(now.toLocalDate(), end.toLocalDate())
                val kind = when { end.isBefore(now) -> null; days <= 1 -> "1"; days <= 7 -> "7"; else -> null } ?: continue
                if (sent.add("$id:$kind")) {
                    val text = if (kind == "1") "Scade domani o oggi (${day(call.s)})" else "Scade il ${day(call.s)}"
                    notify(CH_DEADLINE, ("$id:$kind").hashCode(), call.headline(), text)
                }
            }
            user.copy(alerts = alerts, notified = sent)
        }
        Result.success()
    }

    private fun notify(channel: String, id: Int, title: String, text: String) {
        val ctx = applicationContext
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val open = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(ctx).notify(id, n)
    }

    companion object {
        fun channels(ctx: Context) {
            val nm = ctx.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(CH_NEW, "Nuovi concorsi", NotificationManager.IMPORTANCE_DEFAULT))
            nm.createNotificationChannel(NotificationChannel(CH_DEADLINE, "Scadenze dei concorsi salvati", NotificationManager.IMPORTANCE_DEFAULT))
        }

        fun schedule(ctx: Context) {
            val req = PeriodicWorkRequestBuilder<CheckWorker>(6, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork("check", ExistingPeriodicWorkPolicy.KEEP, req)
        }
    }
}
