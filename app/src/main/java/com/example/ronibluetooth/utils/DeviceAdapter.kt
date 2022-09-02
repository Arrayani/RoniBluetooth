package com.example.ronibluetooth.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.ronibluetooth.MainActivity
import com.example.ronibluetooth.databinding.DeviceCardBinding
import com.example.ronibluetooth.databinding.DeviceFoundBinding
import com.example.ronibluetooth.models.Device

/**

 * Author: Roni Reynal Fitri  on $ {DATE} $ {HOUR}: $ {MINUTE}

 * Email: biruprinting@gmail.com

 */

class DeviceAdapter(val context: Context, var clickListener: MainActivity): RecyclerView.Adapter<DeviceAdapter.MyViewHolder>() {
    var deviceList = ArrayList<Device>()

    fun setData(deviceList: ArrayList<Device>){
        this.deviceList = deviceList
        //  this.barangListFilter = barangList
        //notifyDataSetChanged() //mendengar apakah ada perubahan di data
        notifyItemChanged(deviceList.size)
    }

    class MyViewHolder(val binding:DeviceCardBinding):RecyclerView.ViewHolder(binding.root) {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(DeviceCardBinding.inflate(LayoutInflater.from(parent.context),
        parent,false))
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentitem = deviceList[position]
        holder.binding.deviceNameTv.text = currentitem.deviceName
        holder.binding.deviceMacTv.text = currentitem.deviceMac
        // Updating the background color according to the odd/even positions in list.
//        if (position % 2 == 0) {
//            //holder.binding.cardViewItem
//            holder.binding.deviceCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.colorLightGray))
//        } else {
//            holder.binding.deviceCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
//        }
//        holder.binding.deviceMacTv.setOnClickListener{
//            clickListener.ClickedItem(currentitem)
//            //mendengarkan tekan tombol di baranglist position
//        }
//        holder.binding.deviceNameTv.setOnClickListener{
//            clickListener.ClickedItem(currentitem)
//            //mendengarkan tekan tombol di baranglist position
//        }
        holder.binding.deviceCard.setOnClickListener{
            clickListener.ClickedItem(currentitem)
            //mendengarkan tekan tombol di baranglist position
        }

    }

    override fun getItemCount(): Int {
        return deviceList.size
    }
    interface ClickListener{
        fun ClickedItem(deviceList:Device)
    }
}