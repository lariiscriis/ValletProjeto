package br.edu.fatecpg.valletprojeto.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import br.edu.fatecpg.valletprojeto.worker.CheckReservaWorker

class ReservaExpiredReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reservaId = intent.getStringExtra("reservaId")
        val vagaId = intent.getStringExtra("vagaId")

        if (reservaId != null && vagaId != null) {
            val inputData = Data.Builder()
                .putString("reservaId", reservaId)
                .putString("vagaId", vagaId)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<CheckReservaWorker>()
                .setInputData(inputData)
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}