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
import android.app.Dialog
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.AsyncTask
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
    lateinit var  registerForResult:ActivityResultLauncher<Intent>
    var deviceList = ArrayList<Device>()
    private var deviceAdapter: DeviceAdapter = DeviceAdapter(this,this)

    lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()


    }

    private fun initView() {
        val findbtn = binding.findBtn
        findbtn.setOnClickListener{
            val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
            val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
            val root = binding.mainRoot
            if (bluetoothAdapter == null) {
                Snackbar.make(root,"Perangkat tidak mendukung bluetooth", Snackbar.LENGTH_LONG).show()
                binding.findBtn.text = "No Bluetooth"
                binding.findBtn.isEnabled=false
            }
            if (bluetoothAdapter?.isEnabled == false) {
                cekForBTConPermision()
//                checkBTPermissions()
//                cekScanBTPermission()
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
                println("device is on")
                displayDevice()
            }

            if (bluetoothAdapter?.isEnabled == true) {
                //cekForBTConPermision()
//                checkBTPermissions()
//                cekScanBTPermission()
                println("device is on, start to display bonded device")
                displayDevice()
            }
            if(binding.findBtn.text=="Clear Device"){
                mBluetoothSocket.close()
                binding.conStatsTv.text = "None"
                binding.findBtn.text="Find Device"
                return@setOnClickListener
            }
        }
    }

    private fun displayDevice() {
        deviceList.clear()
        bindingListBl = DeviceFoundBinding.inflate(layoutInflater)
        this.dialogBox = Dialog(this)
        this.dialogBox.setContentView(bindingListBl.root)
        this.dialogBox.setCanceledOnTouchOutside(true)
        getBondedDevice()
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
        this.deviceAdapter.setData(deviceList)
        this.dialogBox.show()


    }

    private fun getBondedDevice() {
        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        pairedDevices?.forEach { device ->
            val deviceName = device.name
            val deviceHardwareAddress = device.address // MAC address
            println("nama $deviceName")
            println("Mac $deviceHardwareAddress")
            deviceList.add(
                Device(deviceName,deviceHardwareAddress)
                )
        }
        println(deviceList)

    }

    private fun cekScanBTPermission() {
        val mLayout = binding.mainRoot
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
            == PackageManager.PERMISSION_GRANTED){
            //Toast.makeText(this@MainActivity,"Sudah diberikan izin akses Scan Bluetooth", Toast.LENGTH_SHORT).show()
            //Snackbar.make(mLayout,"Sudah diberikan izin akses Scan Bluetooth", Snackbar.LENGTH_LONG).show()
        }
        else{
            Snackbar.make(mLayout,"Belum diberikan izin akses Scan Bluetooth", Snackbar.LENGTH_LONG).show()
            requestScanBTPermission()
        }
    }
    private fun requestScanBTPermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.BLUETOOTH_SCAN

            )){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(
                    this@MainActivity, arrayOf((Manifest.permission.BLUETOOTH_SCAN)),
                    PERMISSION_REQUEST_BLUETOOTH_SCAN
                )
            }
        }else{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(
                    this@MainActivity, arrayOf((Manifest.permission.BLUETOOTH_SCAN)),
                    PERMISSION_REQUEST_BLUETOOTH_SCAN
                )
            }

        }

    }

    private fun checkBTPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            var permissionCheck: Int = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION")
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION")
            if (permissionCheck != 0) {
                this.requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION),
                    1001) //Any number
            } // minta 2 permission sekaligus
        } else {
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.")
        }
    }

    private fun cekForBTConPermision() {
        val root = binding.mainRoot
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            == PackageManager.PERMISSION_GRANTED){
            Snackbar.make(root,"Sudah diberikan izin Bluetooth Connect", Snackbar.LENGTH_LONG).show()
        }
        else{
            Snackbar.make(root,"Belum diberikan izin akses", Snackbar.LENGTH_LONG).show()
            requestBluetoothConnetctPermission()
        }
    }
    private fun requestBluetoothConnetctPermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.BLUETOOTH_CONNECT

            )){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(
                    this@MainActivity, arrayOf((Manifest.permission.BLUETOOTH_CONNECT)),
                    PERMISSION_REQUEST_BLUETOOTH_CONNECT
                )
            }
        }else{
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
        private const val REQUEST_CONNECT_DEVICE = 1
        private const val REQUEST_ENABLE_BT = 2
    }

    override fun ClickedItem(deviceList: Device) {
        println("ini yang di pilih ${deviceList.deviceMac}")
        val mLayout=binding.mainRoot
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
                        Snackbar.make(mLayout,"Koneksi bluetooth berhasil", Snackbar.LENGTH_LONG).show()

                    }
                    Log.d(TAG, "berhasil connect ke socket bluetoothdevice")
                } catch (eConnectException: IOException) {
                    Log.d(TAG, "CouldNotConnectToSocket", eConnectException)
                    withContext(Dispatchers.Main) {
                        binding.progressBar.visibility = View.INVISIBLE
                        Snackbar.make(mLayout,"Koneksi bluetooth gagal", Snackbar.LENGTH_LONG).show()
                    }
                           closeSocket(mBluetoothSocket)
                }
            }}
    }

    private fun closeSocket(mBluetoothSocket: BluetoothSocket) {
        try {
            mBluetoothSocket!!.close()
            Log.d(TAG, "SocketClosed")
        } catch (ex: IOException) {
            Log.d(TAG, "CouldNotCloseSocket")
        }
    }


}