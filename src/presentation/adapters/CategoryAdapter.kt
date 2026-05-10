package com.aminmart.moneymanager.presentation.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aminmart.moneymanager.R

/**
 * Simple adapter for category selection
 */
class CategoryAdapter(
    private var categories: List<String> = emptyList(),
    private val selectedCategory: String?,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textCategory: TextView = itemView.findViewById(R.id.text_category_name)
        val indicatorSelected: View = itemView.findViewById(R.id.indicator_category_selected)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        holder.textCategory.text = category

        val isSelected = category == selectedCategory
        holder.indicatorSelected.visibility = if (isSelected) View.VISIBLE else View.GONE
        
        val context = holder.itemView.context
        holder.textCategory.setTextColor(
            if (isSelected) context.getColor(R.color.primary)
            else context.getColor(android.R.color.black)
        )

        holder.itemView.setOnClickListener {
            onItemClick(category)
        }
    }

    override fun getItemCount(): Int = categories.size

    fun submitList(newCategories: List<String>) {
        categories = newCategories
        notifyDataSetChanged()
    }
}
