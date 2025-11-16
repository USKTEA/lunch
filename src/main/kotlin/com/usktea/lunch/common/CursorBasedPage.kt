package com.usktea.lunch.common

data class CursorBasedPage<T>(
    val content: List<T>,
    val meta: Metadata,
) {
    data class Metadata(
        val next: String?,
    )

    fun <R> map(transForm: (T) -> R): CursorBasedPage<R> {
        return CursorBasedPage(
            content = this.content.map(transForm),
            meta = this.meta,
        )
    }
}

fun <T> pageOf(
    content: List<T>,
    hasNext: Boolean,
    cursorSelector: (T) -> String,
): CursorBasedPage<T> {
    return CursorBasedPage(
        content = content,
        meta =
            CursorBasedPage.Metadata(
                next = if (hasNext && content.isNotEmpty()) cursorSelector(content.last()) else null,
            ),
    )
}
