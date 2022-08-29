package com.example.arcoreaugmentedimage.models

data class NodeModel(
    var is_parent_node: Boolean = false,
    var parent_node_text: String = "",
    var child_node_text: String = "",
    var child_node_image_uri: String = "",
    var is_text_field: Boolean = false
)