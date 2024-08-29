package com.ertools.utils

import java.util.*

class ObservableQueue<T> (
    private val callback: () -> Unit = { }
): Queue<T> by LinkedList() {
    override fun add(element: T): Boolean {
        val result = (this as Queue<T>).add(element)
        callback.invoke()
        return result
    }

}