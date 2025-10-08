package com.eventradar.data.model

data class CommentWithAuthor(
    val comment: Comment,
    val author: User?
)