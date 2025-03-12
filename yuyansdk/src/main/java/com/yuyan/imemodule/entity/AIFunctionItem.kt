package com.yuyan.imemodule.entity

/**
 * AI功能项实体类
 * 用于表示AI功能列表中的每一项
 * @param name 功能名称
 * @param iconResId 功能图标资源ID
 * @param functionType 功能类型ID，用于区分不同的AI功能
 */
data class AIFunctionItem(
    val name: String,
    val iconResId: Int,
    val functionType: Int
)