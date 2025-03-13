package com.yuyan.imemodule.entity

/**
 * AI功能子分类实体类
 * 用于表示AI功能的子分类信息
 * @param name 子分类名称
 * @param functions 该子分类下的功能列表
 * @param isExpanded 子分类是否展开
 */
data class AISubCategory(
    val name: String,
    val functions: List<AIFunctionItem>,
    var isExpanded: Boolean = false
)