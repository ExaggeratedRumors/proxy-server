package utils

import java.util.*

class ObservableQueue<T> (
    private val callback: (T) -> Unit = { _ -> }
): LinkedList<T>() {
    override fun add(element: T): Boolean {
        callback.invoke(element)
        return super.add(element)
    }
}