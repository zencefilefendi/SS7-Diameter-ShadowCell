package com.shadowcell.scoring

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * TensorFlow Lite (ML) modeli entegrasyonu.
 * Offline makine öğrenimi modeli ile cihaz davranışına göre gelişmiş skorlama yapar.
 */
class MlAnomalyScorer(private val context: Context) {

    private var tflite: Interpreter? = null

    init {
        // Model dosyasının assets klasöründe "shadowcell_anomaly_model.tflite" olarak bulunduğunu varsayıyoruz.
        // Hata fırlatmaması için try-catch bloğunda model yüklenir.
        try {
            val options = Interpreter.Options()
            // Donanımsal hızlandırma eklenebilir (NNAPI, GPU Delegate vb.)
            tflite = Interpreter(loadModelFile(), options)
        } catch (e: Exception) {
            e.printStackTrace()
            // Model bulunamazsa fallback mekanizması çalışacak
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd("shadowcell_anomaly_model.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    /**
     * @param features Normalleştirilmiş girdi verileri (ör. sinyal gücü, hücre değişim sıklığı, network tipi vb.)
     * @return 0 ile 1.0 arasında bir risk olasılığı (probability) döndürür.
     */
    fun predictRisk(features: FloatArray): Float {
        if (tflite == null) return -1f // Model yoksa fallback işareti

        val inputArray = arrayOf(features)
        val outputArray = arrayOf(FloatArray(1)) // Çıktı: Tek bir risk skoru

        tflite?.run(inputArray, outputArray)

        return outputArray[0][0]
    }

    fun close() {
        tflite?.close()
        tflite = null
    }
}