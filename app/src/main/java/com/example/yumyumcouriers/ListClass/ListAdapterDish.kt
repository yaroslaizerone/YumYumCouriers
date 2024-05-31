package com.example.yumyumcouriers.ListClass

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.example.yumyumcouriers.R
import com.squareup.picasso.Picasso

class ListAdapterDish(context: Context, dataArrayList: ArrayList<ListDataDish?>?) :
    ArrayAdapter<ListDataDish?>(context, R.layout.list_item_dish, dataArrayList!!) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        val listData = getItem(position)
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.list_item_dish, parent, false)
        }

        val listImage = view!!.findViewById<ImageView>(R.id.listImage)
        val listName = view.findViewById<TextView>(R.id.listName)
        val listPrice = view.findViewById<TextView>(R.id.listPrice)
        val listCount = view.findViewById<TextView>(R.id.listCount)

        // Загрузка изображения по URL с использованием Picasso
        Picasso.get().load(listData!!.photo).into(listImage)

        listName.text = listData.name
        listCount.text = listData.quantity.toString()
        listPrice.text = listData.cost.toString()

        return view
    }
}

