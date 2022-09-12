package com.example.ronibluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import com.example.ronibluetooth.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.DialogInterface
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback

import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ronibluetooth.databinding.DeviceFoundBinding
import com.example.ronibluetooth.models.Device
import com.example.ronibluetooth.utils.DeviceAdapter
import kotlinx.coroutines.*
import java.io.IOException
import java.io.OutputStream
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity(), DeviceAdapter.ClickListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var bindingListBl: DeviceFoundBinding
    private lateinit var dialogBox: Dialog
    private lateinit var recyclerviewDevice: RecyclerView
    private val applicationUUID = UUID
        .fromString("00001101-0000-1000-8000-00805F9B34FB")
    lateinit var mBluetoothSocket: BluetoothSocket

    //lateinit var activityResultLauncher:ActivityResultLauncher<Intent>
    lateinit var registerForResult: ActivityResultLauncher<Intent>
    var deviceList = ArrayList<Device>()
    private var deviceAdapter: DeviceAdapter = DeviceAdapter(this, this)

    lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()

        val secondActBtn = binding.secondActBtn
        secondActBtn.setOnClickListener{
            intent = Intent(this,SecondActivity::class.java)
            startActivity(intent)
            println("button push")
        }
    }

    private fun initView() {
        checkBTPermissions()
        cekForBTConPermision()
        cekScanBTPermission()


        val findbtn = binding.findBtn
        findbtn.setOnClickListener {
            val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
            val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
            val root = binding.mainRoot
            if (bluetoothAdapter == null) {
                Snackbar.make(root, "Perangkat tidak mendukung bluetooth", Snackbar.LENGTH_LONG)
                    .show()
                binding.findBtn.text = "No Bluetooth"
                binding.findBtn.isEnabled = false
            }
            if (bluetoothAdapter?.isEnabled == false) {
                cekForBTConPermision()
                checkBTPermissions()
                cekScanBTPermission()
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
                println("device is on")
                displayDevice()
            }

            if (bluetoothAdapter?.isEnabled == true) {
                cekForBTConPermision()
                checkBTPermissions()
                cekScanBTPermission()
                println("device is on, start to display bonded device")
                displayDevice()
            }

//            if(binding.findBtn.text=="None"){
//                checkBTPermissions()
//                cekScanBTPermission()
//                println("device is on, start to display bonded device")
//                displayDevice()
//            }
            if (binding.findBtn.text == "Clear Device") {
                mBluetoothSocket.close()
                binding.conStatsTv.text = "None"
                binding.findBtn.text = "Find Device"
                cekConected()
                return@setOnClickListener
            }
        }


        cekConected()

    }

    private fun cekConected() {
        val print1 = binding.print1Btn
        val print2 = binding.print2Btn
        val stats = binding.conStatsTv.text
        if (stats == "None") {
            print1.isEnabled = false
            print2.isEnabled = false
        }
        if (stats != "None") {
            print1.isEnabled = true
            print2.isEnabled = true
        }
        print1.setOnClickListener{
            p1()
        }
    }



    private fun displayDevice() {
        deviceList.clear()
        bindingListBl = DeviceFoundBinding.inflate(layoutInflater)
        this.dialogBox = Dialog(this)
        this.dialogBox.setContentView(bindingListBl.root)
        this.dialogBox.setCanceledOnTouchOutside(true)
        //getBondedDevice()
        recyclerviewDevice = bindingListBl.recyclerView
        recyclerviewDevice.layoutManager = LinearLayoutManager(this)
        recyclerviewDevice.setHasFixedSize(true)
        recyclerviewDevice.addItemDecoration(
            DividerItemDecoration(
                this,
                LinearLayoutManager.VERTICAL
            )
        )
        this.recyclerviewDevice.adapter = deviceAdapter
        getBondedDevice()
        this.deviceAdapter.setData(deviceList)
        this.dialogBox.show()


    }

    private fun getBondedDevice() {
        //cekForBTConPermision()
        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        pairedDevices?.forEach { device ->
            val deviceName = device.name
            val deviceHardwareAddress = device.address // MAC address
            println("nama $deviceName")
            println("Mac $deviceHardwareAddress")
            deviceList.add(
                Device(deviceName, deviceHardwareAddress)
            )
        }
        println(deviceList)

    }





    private fun cekScanBTPermission() {
        val mLayout = binding.mainRoot
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
            == PackageManager.PERMISSION_GRANTED
        ) {
            //Toast.makeText(this@MainActivity,"Sudah diberikan izin akses Scan Bluetooth", Toast.LENGTH_SHORT).show()
            //Snackbar.make(mLayout,"Sudah diberikan izin akses Scan Bluetooth", Snackbar.LENGTH_LONG).show()
        } else {
            Snackbar.make(
                mLayout,
                "Belum diberikan izin akses Scan Bluetooth",
                Snackbar.LENGTH_LONG
            ).show()
            requestScanBTPermission()
        }
    }

    private fun requestScanBTPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            )
        ) {
            val builder = AlertDialog.Builder(this)
            //Builder(this)
            builder.setTitle("Izin dibutuhkan")
                .setMessage("Untuk dapat menggunakan printer, izin bluetooth dibutuhkan")
                .setPositiveButton("ok",
                    DialogInterface.OnClickListener { dialog, which ->
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.BLUETOOTH_SCAN),
                            PERMISSION_REQUEST_BLUETOOTH_SCAN
                        )
                    })
                .setNegativeButton("cancel",
                    DialogInterface.OnClickListener { dialog, which -> dialog.dismiss() })
                .create().show()
        } else {
            ActivityCompat.requestPermissions(
                this@MainActivity, arrayOf((Manifest.permission.BLUETOOTH_SCAN)),
                PERMISSION_REQUEST_BLUETOOTH_SCAN
            )

        }

    }

    //    private fun checkBTPermissions() {
//        var permissionCheck: Int =
//            this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION")
//        permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION")
//        if (permissionCheck != 0) {
//            this.requestPermissions(
//                arrayOf(
//                    Manifest.permission.ACCESS_FINE_LOCATION,
//                    Manifest.permission.ACCESS_COARSE_LOCATION
//                ),
//                1001
//            ) //Any number
//        } // minta 2 permission sekaligus
//    }
    private fun checkBTPermissions() {
        val mLayout = binding.mainRoot
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)
            == PackageManager.PERMISSION_GRANTED
        ) {
            //Toast.makeText(this@MainActivity,"Sudah diberikan izin akses Bluetooth", Toast.LENGTH_SHORT).show()
            Snackbar.make(mLayout,"Sudah diberikan izin akses Bluetooth", Snackbar.LENGTH_LONG).show()
        } else {
            Snackbar.make(
                mLayout,
                "Belum diberikan izin akses Scan Bluetooth",
                Snackbar.LENGTH_LONG
            ).show()
            requestBTPermission()
        }
    }

    private fun requestBTPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.BLUETOOTH
            )
        ) {
            val builder = AlertDialog.Builder(this)
            //Builder(this)
            builder.setTitle("Izin dibutuhkan")
                .setMessage("Untuk dapat menggunakan printer, izin bluetooth dibutuhkan")
                .setPositiveButton("ok",
                    DialogInterface.OnClickListener { dialog, which ->
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.BLUETOOTH),
                            PERMISSION_REQUEST_BLUETOOTH
                        )
                    })
                .setNegativeButton("cancel",
                    DialogInterface.OnClickListener { dialog, which -> dialog.dismiss() })
                .create().show()
        } else {
            ActivityCompat.requestPermissions(
                this@MainActivity, arrayOf((Manifest.permission.BLUETOOTH)),
                PERMISSION_REQUEST_BLUETOOTH
            )

        }
    }


    private fun cekForBTConPermision() {
        val root = binding.mainRoot
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            == PackageManager.PERMISSION_GRANTED
        ) {
            Snackbar.make(root, "Sudah diberikan izin Bluetooth Connect", Snackbar.LENGTH_LONG)
                .show()
        } else {
            Snackbar.make(root, "Belum diberikan izin akses", Snackbar.LENGTH_LONG).show()
            requestBluetoothConnetctPermission()
        }
    }

    private fun requestBluetoothConnetctPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        ) {
            val builder = AlertDialog.Builder(this)
            //Builder(this)
            builder.setTitle("Izin dibutuhkan")
                .setMessage("Untuk dapat menggunakan printer, izin bluetooth dibutuhkan")
                .setPositiveButton("ok",
                    DialogInterface.OnClickListener { dialog, which ->
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                            PERMISSION_REQUEST_BLUETOOTH_CONNECT
                        )
                    })
                .setNegativeButton("cancel",
                    DialogInterface.OnClickListener { dialog, which -> dialog.dismiss() })
                .create().show()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(
                    this@MainActivity, arrayOf((Manifest.permission.BLUETOOTH_CONNECT)),
                    PERMISSION_REQUEST_BLUETOOTH_CONNECT
                )
            }

        }
    }


    companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSION_REQUEST_BLUETOOTH_CONNECT = 20
        private const val PERMISSION_REQUEST_BLUETOOTH_SCAN = 30
        private const val PERMISSION_REQUEST_BLUETOOTH = 10
        private const val REQUEST_ENABLE_BT = 2
    }

    private fun p1() {
        val mmOutStream: OutputStream = mBluetoothSocket.outputStream
        val mmBuffer: ByteArray = ByteArray(1024) // mmBuffer store for the stream
//        val scope = CoroutineScope(Dispatchers.IO)
//        scope.launch {
//            try {
//                    val os =mBluetoothSocket.outputStream
//                    var header = "Rose Medical"
//                    val blank ="\n\n"
//
//                    os.write(header.toByteArray())
//                    os.write(blank.toByteArray())
//
//                }
//                catch (e: Exception) {
//                    Log.e("PrintActivity", "Exe ", e)
//                }
//        }
        val namaToko = "Rose Medical"
        val alamatToko = "Pramuka raya no.1"
        val merk = "Terumo"
        val namaBrg ="Syringe"
        val varian = "5cc"
        val harga = "3.000"
        val rp ="Rp."
        val jumlah = "30"
        val total = "90.000"
        val enter = "\n"
        val hp = "0811901081"
        val payment ="Cash"
        val strip = "-"
        val ex = " X "
        val textTotal = "Total Rp:"
        val satuan = "Pcs"
        val tanggal = "09/04/2022"
        val jam = "18:30"
        val idTag = "Rose Medical"
        val ppnTv = "PPN :Rp."
        val ppnValue ="0"
        val chargeTv ="Charge :Rp."
        val chargeValue = "100.000"
        val totalTv = "Total Belanja :Rp."
        val totalValue = "1.000.000"
        val blank="                    "//20
        val stripe ="--------------------"//20
        val stripe2 ="------------------------------"//30
        val stripe3 =""



        val scope = CoroutineScope(Dispatchers.IO)
        val scope2 = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try{

    //          writeWithFormat(message.toByteArray(),Formatter().bold().get(),Formatter.centerAlign())

                writeWithFormat(namaToko.toByteArray(),Formatter().bold().get(),Formatter.centerAlign())
                writeWithFormat(enter.toByteArray(),Formatter().get(),Formatter.centerAlign())
                writeWithFormat(alamatToko.toByteArray(),Formatter().get(),Formatter.centerAlign())
                writeWithFormat(enter.toByteArray(),Formatter().get(),Formatter.centerAlign())
                writeWithFormat(hp.toByteArray(),Formatter().get(),Formatter.centerAlign())
                writeWithFormat(enter.toByteArray(),Formatter().get(),Formatter.centerAlign())

                // Bold underlined format with right alignment:
                writeWithFormat(stripe2.toByteArray(),Formatter().bold().get(),
                    Formatter.centerAlign())
                writeWithFormat(enter.toByteArray(),Formatter().get(),Formatter.centerAlign())


            }catch (e: Exception) {
                Log.e("PrintActivity", "Exe ", e)
            }

            for (i in 1..1){
                try {
//                    writeWithFormat(merk.toByteArray(),Formatter().get(),Formatter.leftAlign())
//                    writeWithFormat(strip.toByteArray(),Formatter().get(),Formatter.leftAlign())
//                    writeWithFormat(namaBrg.toByteArray(),Formatter().get(),Formatter.leftAlign())
//                    writeWithFormat(enter.toByteArray(),Formatter().get(),Formatter.leftAlign())
//                    writeWithFormat(varian.toByteArray(),Formatter().get(),Formatter.leftAlign())
//                    writeWithFormat(enter.toByteArray(),Formatter().get(),Formatter.leftAlign())
//                    writeWithFormat(rp.toByteArray(),Formatter().get(),Formatter.leftAlign())
//                    writeWithFormat(harga.toByteArray(),Formatter().get(),Formatter.leftAlign())
//                    writeWithFormat(ex.toByteArray(),Formatter().get(),Formatter.leftAlign())
//                    writeWithFormat(jumlah.toByteArray(),Formatter().get(),Formatter.leftAlign())
//                    writeWithFormat(satuan.toByteArray(),Formatter().get(),Formatter.leftAlign())
//                    writeWithFormat(enter.toByteArray(),Formatter().get(),Formatter.leftAlign())
//                    writeWithFormat(textTotal.toByteArray(),Formatter().get(),Formatter.rightAlign())
//                    writeWithFormat(total.toByteArray(),Formatter().get(),Formatter.rightAlign())
//                    writeWithFormat(enter.toByteArray(),Formatter().get(),Formatter.leftAlign())
                    // writeWithFormat(enter.toByteArray(),Formatter().get(),Formatter.leftAlign())
//                val message = "Example message\n"
                    // Default format:
//                writeWithFormat(message.toByteArray(), Formatter().get(), Formatter.leftAlign())
                    // Bold format center:
//                writeWithFormat(message.toByteArray(),
//                    Formatter().bold().get(),
//                    Formatter.centerAlign())
                    // Bold underlined format with right alignment:
//                writeWithFormat(message.toByteArray(),
//                    Formatter().bold().underlined().get(),
//                    Formatter.rightAlign())
                }catch (e: Exception) {
                    Log.e("PrintActivity", "Exe ", e)
                }}
        }


//                try {
//                writeWithFormat(tanggal.toByteArray(),Formatter().get(),Formatter.leftAlign())
//                writeWithFormat(strip.toByteArray(),Formatter().get(),Formatter.leftAlign())
//                writeWithFormat(jam.toByteArray(),Formatter().get(),Formatter.leftAlign())
//                writeWithFormat(strip.toByteArray(),Formatter().get(),Formatter.leftAlign())
//                writeWithFormat(idTag.toByteArray(),Formatter().get(),Formatter.leftAlign())
//                writeWithFormat(enter.toByteArray(),Formatter().get(),Formatter.leftAlign())
//                writeWithFormat(payment.toByteArray(),Formatter().get(),Formatter.leftAlign())
//                writeWithFormat(enter.toByteArray(),Formatter().get(),Formatter.leftAlign())
//                writeWithFormat(ppnTv.toByteArray(),Formatter().get(),Formatter.rightAlign())
//                writeWithFormat(ppnValue.toByteArray(),Formatter().get(),Formatter.rightAlign())
//                writeWithFormat(enter.toByteArray(),Formatter().get(),Formatter.rightAlign())
//                writeWithFormat(chargeTv.toByteArray(),Formatter().get(),Formatter.rightAlign())
//                writeWithFormat(chargeValue.toByteArray(),Formatter().get(),Formatter.rightAlign())
//                writeWithFormat(enter.toByteArray(),Formatter().get(),Formatter.rightAlign())
//                writeWithFormat(totalTv.toByteArray(),Formatter().get(),Formatter.rightAlign())
//                writeWithFormat(totalValue.toByteArray(),Formatter().get(),Formatter.rightAlign())
//                writeWithFormat(enter.toByteArray(),Formatter().get(),Formatter.rightAlign())
//                writeWithFormat(enter.toByteArray(),Formatter().get(),Formatter.rightAlign())
//                writeWithFormat(enter.toByteArray(),Formatter().get(),Formatter.rightAlign())
//                writeWithFormat(enter.toByteArray(),Formatter().get(),Formatter.rightAlign())
//                writeWithFormat(enter.toByteArray(),Formatter().get(),Formatter.rightAlign())
//                writeWithFormat(enter.toByteArray(),Formatter().get(),Formatter.rightAlign())
//
//            }catch (e: Exception) {
//                Log.e("PrintActivity", "Exe ", e)
//            }

    }


    override fun ClickedItem(deviceList: Device) {
        println("ini yang di pilih ${deviceList.deviceMac}")
        val mLayout = binding.mainRoot
        cekScanBTPermission()
        checkBTPermissions()
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            withTimeout(10000) {
                try {
                    val bluetoothManager: BluetoothManager =
                        getSystemService(BluetoothManager::class.java) //daily dose
                    val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter //daily dose
                    val mBluetoothDevice = bluetoothAdapter!!.getRemoteDevice(deviceList.deviceMac)
                    mBluetoothSocket =
                        mBluetoothDevice.createRfcommSocketToServiceRecord(applicationUUID)
                    bluetoothAdapter.cancelDiscovery()
                    withContext(Dispatchers.Main) {
                        dialogBox.dismiss()
                        binding.progressBar.visibility = View.VISIBLE
                    }

                    mBluetoothSocket.connect()
                    //ini cara update ui dari dalam coroutine, dengan ini kita pindah ke mainthread
                    withContext(Dispatchers.Main) {
                        //binding.conStatsTv.text ="Connected"
                        binding.progressBar.visibility = View.INVISIBLE
                        val namaDevice = deviceList.deviceName
                        val addressDevice = deviceList.deviceMac
                        binding.conStatsTv.text = "$namaDevice-$addressDevice"
                        binding.findBtn.text = "Clear Device"
                        Snackbar.make(mLayout, "Koneksi bluetooth berhasil", Snackbar.LENGTH_LONG)
                            .show()
                        cekConected()
                    }
                    Log.d(TAG, "berhasil connect ke socket bluetoothdevice")
                } catch (eConnectException: IOException) {
                    Log.d(TAG, "CouldNotConnectToSocket", eConnectException)
                    withContext(Dispatchers.Main) {
                        binding.progressBar.visibility = View.INVISIBLE
                        Snackbar.make(mLayout, "Koneksi bluetooth gagal", Snackbar.LENGTH_LONG)
                            .show()
                    }
                    closeSocket(mBluetoothSocket)
                }
            }
        }
    }

    private fun closeSocket(mBluetoothSocket: BluetoothSocket) {
        try {
            mBluetoothSocket!!.close()
            Log.d(TAG, "SocketClosed")
        } catch (ex: IOException) {
            Log.d(TAG, "CouldNotCloseSocket")
        }
    }

    class Formatter {
        /** The format that is being build on  */
        private val mFormat: ByteArray

        init {
            // Default:
            mFormat = byteArrayOf(27, 33, 0)
        }

        /**
         * Method to get the Build result
         *
         * @return the format
         */
        fun get(): ByteArray {
            return mFormat
        }

        fun bold(): Formatter {
            // Apply bold:
            mFormat[2] = (0x8 or mFormat[2].toInt()).toByte()
            return this
        }

        fun small(): Formatter {
            mFormat[2] = (0x1 or mFormat[2].toInt()).toByte()
            return this
        }

        fun height(): Formatter {
            mFormat[2] = (0x10 or mFormat[2].toInt()).toByte()
            return this
        }

        fun width(): Formatter {
            mFormat[2] = (0x20 or mFormat[2].toInt()).toByte()
            return this
        }

        fun underlined(): Formatter {
            mFormat[2] = (0x80 or mFormat[2].toInt()).toByte()
            return this
        }

        companion object {
            fun rightAlign(): ByteArray {
                return byteArrayOf(0x1B, 'a'.code.toByte(), 0x02)
            }

            fun leftAlign(): ByteArray {
                return byteArrayOf(0x1B, 'a'.code.toByte(), 0x00)
            }

            fun centerAlign(): ByteArray {
                return byteArrayOf(0x1B, 'a'.code.toByte(), 0x01)
            }
        }
    }//last
    fun writeWithFormat(buffer: ByteArray, pFormat: ByteArray?, pAlignment: ByteArray?): Boolean {
        val mmOutStream: OutputStream = mBluetoothSocket.outputStream
        return try {
            // Notify printer it should be printed with given alignment:
            mmOutStream.write(pAlignment)
            // Notify printer it should be printed in the given format:
            mmOutStream.write(pFormat)
            // Write the actual data:
            mmOutStream.write(buffer, 0, buffer.size)

            // Share the sent message back to the UI Activity
            //App.getInstance().getHandler().obtainMessage(MESSAGE_WRITE, buffer.size, -1, buffer).sendToTarget()
            true
        } catch (e: IOException) {
            Log.e(TAG, "Exception during write", e)
            false
        }
    }

}//last bracket