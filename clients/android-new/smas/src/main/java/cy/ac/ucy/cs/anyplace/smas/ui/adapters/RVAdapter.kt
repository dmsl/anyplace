package com.example.search.utils

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import cy.ac.ucy.cs.anyplace.lib.models.POI
import cy.ac.ucy.cs.anyplace.smas.databinding.RowItemBinding

class RVAdapter(var pois: List<POI>,
                private val clickListener:  (POI) -> Unit) : RecyclerView.Adapter<RVAdapter.ViewHolder>() {

    class ViewHolder(private var item: RowItemBinding, clickAtPosition: (Int) -> Unit) : RecyclerView.ViewHolder(item.root){
        init{
            item.root.setOnClickListener {
                clickAtPosition(adapterPosition)
            }
        }

        fun bind(poi: POI){
            item.tvFloor.text = "[${poi.floorNumber}]:"
            item.tvName.text = poi.name.trim()
        }
    }

    override fun getItemCount(): Int = pois.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            RowItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        ){
            clickListener(pois[it])
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(
            pois[position]
        )
    }

}