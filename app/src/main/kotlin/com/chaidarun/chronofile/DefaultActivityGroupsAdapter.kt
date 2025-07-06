package com.chaidarun.chronofile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.chaidarun.chronofile.databinding.ItemActivityGroupPresetBinding

class DefaultActivityGroupsAdapter(
  private val onLoadPreset: (String, Map<String, List<String>>) -> Unit
) : RecyclerView.Adapter<DefaultActivityGroupsAdapter.PresetViewHolder>() {
  
  private val presets = DefaultActivityGroups.getAllPresets()
  private val presetNames = presets.keys.toList()
  
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PresetViewHolder {
    val binding = ItemActivityGroupPresetBinding.inflate(
      LayoutInflater.from(parent.context),
      parent,
      false
    )
    return PresetViewHolder(binding)
  }
  
  override fun onBindViewHolder(holder: PresetViewHolder, position: Int) {
    val presetName = presetNames[position]
    val presetGroups = presets[presetName] ?: emptyMap()
    holder.bind(presetName, presetGroups)
  }
  
  override fun getItemCount(): Int = presetNames.size
  
  inner class PresetViewHolder(private val binding: ItemActivityGroupPresetBinding) : 
    RecyclerView.ViewHolder(binding.root) {
    
    fun bind(presetName: String, groups: Map<String, List<String>>) {
      binding.presetTitle.text = presetName
      
      // Create description from group names
      val groupNames = groups.keys.sorted().joinToString(", ")
      binding.presetDescription.text = groupNames
      
      // Count groups and activities
      val groupCount = groups.size
      val activityCount = groups.values.sumOf { it.size }
      binding.presetGroupCount.text = "$groupCount groups • $activityCount activities"
      
      binding.loadPresetButton.setOnClickListener {
        onLoadPreset(presetName, groups)
      }
      
      binding.root.setOnClickListener {
        onLoadPreset(presetName, groups)
      }
    }
  }
}