package com.moka.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.moka.app.StringsAdapter.TagsViewHolder

class StringsAdapter(private var arrayList: List<String>) : RecyclerView.Adapter<TagsViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagsViewHolder {
        val layoutView = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
        return TagsViewHolder(layoutView)
    }

    override fun onBindViewHolder(holder: TagsViewHolder, position: Int) {
        holder.tagText.text = arrayList[position]
    }

    override fun getItemCount(): Int {
        return arrayList.size
    }

    class TagsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var tagText: TextView = view.findViewById(android.R.id.text1)
    }
}