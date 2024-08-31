package com.ertools.utils

import java.util.*

class ObservableQueue<T> (
    private val callback: (T) -> Unit = { _ -> }
): Queue<T> by LinkedList() {
    override fun add(element: T): Boolean {
        val result = (this as Queue<T>).add(element)
        callback.invoke(element)
        return result
    }
}