package com.example.wao_fe

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import com.journeyapps.barcodescanner.CaptureManager
import com.journeyapps.barcodescanner.DecoratedBarcodeView

class CustomScannerActivity : AppCompatActivity() {
    private lateinit var capture: CaptureManager
    private lateinit var barcodeScannerView: DecoratedBarcodeView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_scanner)

        barcodeScannerView = findViewById(R.id.zxing_barcode_scanner)

        Log.d("BarcodeScan", "Khởi tạo CustomScannerActivity: Chế độ dọc với khung vuông")

        capture = CaptureManager(this, barcodeScannerView)
        capture.initializeFromIntent(intent, savedInstanceState)
        capture.setShowMissingCameraPermissionDialog(true)
        // Tập trung nhận diện vào khu vực khung
        capture.decode()
    }

    override fun onResume() {
        super.onResume()
        Log.d("BarcodeScan", "Resuming camera scan")
        capture.onResume()
    }

    override fun onPause() {
        super.onPause()
        Log.d("BarcodeScan", "Pausing camera scan")
        capture.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("BarcodeScan", "Destroying CustomScannerActivity")
        capture.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        capture.onSaveInstanceState(outState)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return barcodeScannerView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event)
    }
}

