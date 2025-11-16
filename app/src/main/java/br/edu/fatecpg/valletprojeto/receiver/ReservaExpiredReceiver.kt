package br.edu.fatecpg.valletprojeto.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import br.edu.fatecpg.valletprojeto.worker.CheckReservaWorker

class ReservaExpiredReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val checkReservaWorkRequest = OneTimeWorkRequestBuilder<CheckReservaWorker>().build()
        WorkManager.getInstance(context).enqueue(checkReservaWorkRequest)
    }
}
