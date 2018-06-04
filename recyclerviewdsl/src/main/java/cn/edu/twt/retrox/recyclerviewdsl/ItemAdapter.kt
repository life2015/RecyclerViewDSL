package cn.edu.twt.retrox.recyclerviewdsl

import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.ViewGroup

interface Item {
    val controller: ItemController

    fun areItemsTheSame(newItem: Item): Boolean = false

    fun areContentsTheSame(newItem: Item): Boolean = false
}

interface ItemController {
    fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder
    fun onBindViewHolder(holder: RecyclerView.ViewHolder, item: Item)
}

class ItemAdapter(val itemManager: ItemManagerAbstract) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), MutableList<Item> by itemManager {

    init {
        itemManager.observer = this
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ItemManager.getController(viewType).onCreateViewHolder(parent)

    override fun getItemCount() = itemManager.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
            itemManager[position].controller.onBindViewHolder(holder, itemManager[position])

    override fun getItemViewType(position: Int) = ItemManager.getViewType(itemManager[position].controller)
}

fun RecyclerView.withItems(items: List<Item>) {
    adapter = ItemAdapter(ItemManager(items.toMutableList()))
}

fun RecyclerView.withItems(init: MutableList<Item>.() -> Unit) = withItems(mutableListOf<Item>().apply(init))

interface ItemManagerAbstract : MutableList<Item> {
    var observer: RecyclerView.Adapter<RecyclerView.ViewHolder>?
}

class ItemManager(private val delegated: MutableList<Item> = mutableListOf()) : ItemManagerAbstract {
    override var observer: RecyclerView.Adapter<RecyclerView.ViewHolder>? = null
    val itemListSnapshot: List<Item> get() = delegated

    init {
        ensureControllers(delegated)
    }

    companion object ItemControllerManager {
        private var viewType = 0 // companion object保证了单例 因此ViewType肯定是从0开始

        // controller to view type
        private val c2vt = mutableMapOf<ItemController, Int>()

        // view type to controller
        private val vt2c = mutableMapOf<Int, ItemController>()

        /**
         * 检查Item(对应的controller)是否已经被注册，如果没有，那就注册一个ViewType
         */
        private fun ensureController(item: Item) {
            val controller = item.controller
            if (!c2vt.contains(controller)) {
                c2vt[controller] = viewType
                vt2c[viewType] = controller
                viewType++
            }
        }

        /**
         * 对于一个Collection的ViewType注册，先进行一次去重
         */
        private fun ensureControllers(items: Collection<Item>): Unit =
                items.distinctBy(Item::controller).forEach(ItemControllerManager::ensureController)

        /**
         * 根据ItemController获取对应的Item -> 代理Adapter.getItemViewType
         */
        fun getViewType(controller: ItemController): Int = c2vt[controller]
                ?: throw IllegalStateException("ItemController $controller is not ensured")

        /**
         * 根据ViewType获取ItemController -> 代理OnCreateViewHolder相关逻辑
         */
        fun getController(viewType: Int): ItemController = vt2c[viewType]
                ?: throw IllegalStateException("ItemController $viewType is unused")
    }


    override val size: Int get() = delegated.size

    override fun contains(element: Item) = delegated.contains(element)

    override fun containsAll(elements: Collection<Item>) = delegated.containsAll(elements)

    override fun get(index: Int): Item = delegated[index]

    override fun indexOf(element: Item) = delegated.indexOf(element)

    override fun isEmpty() = delegated.isEmpty()

    override fun iterator() = delegated.iterator()

    override fun lastIndexOf(element: Item) = delegated.lastIndexOf(element)

    override fun listIterator() = delegated.listIterator()

    override fun listIterator(index: Int) = delegated.listIterator(index)

    override fun subList(fromIndex: Int, toIndex: Int) = delegated.subList(fromIndex, toIndex)

    override fun add(element: Item) =
            delegated.add(element).also {
                ensureController(element)
                if (it) observer?.notifyItemInserted(size)
            }

    override fun add(index: Int, element: Item) =
            delegated.add(index, element).also {
                ensureController(element)
                observer?.notifyItemInserted(index)
            }

    override fun addAll(index: Int, elements: Collection<Item>) =
            delegated.addAll(elements).also {
                ensureControllers(elements)
                if (it) observer?.notifyItemRangeInserted(index, elements.size)
            }

    override fun addAll(elements: Collection<Item>) =
            delegated.addAll(elements).also {
                ensureControllers(elements)
                if (it) observer?.notifyItemRangeInserted(size, elements.size)
            }

    override fun clear() =
            delegated.clear().also {
                observer?.notifyItemRangeRemoved(0, size)
            }

    override fun remove(element: Item): Boolean =
            delegated.remove(element).also {
                if (it) observer?.notifyDataSetChanged()
            }

    override fun removeAll(elements: Collection<Item>): Boolean =
            delegated.removeAll(elements).also {
                if (it) observer?.notifyDataSetChanged()
            }

    override fun removeAt(index: Int) =
            delegated.removeAt(index).also {
                observer?.notifyItemRemoved(index)
            }

    override fun retainAll(elements: Collection<Item>) =
            delegated.retainAll(elements).also {
                if (it) observer?.notifyDataSetChanged()
            }

    override fun set(index: Int, element: Item) =
            delegated.set(index, element).also {
                ensureController(element)
                observer?.notifyItemChanged(index)
            }

    fun refreshAll(elements: List<Item>) {
        val tag = "refresh diff"
        val diffCallback = object : DiffUtil.Callback() {
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val oldItem = delegated[oldItemPosition]
                val newItem = elements[newItemPosition]
//                log(tag, "items the same: old-> $oldItemPosition new-> $newItemPosition diff: ${oldItem.areItemsTheSame(newItem)}")
                return oldItem.areItemsTheSame(newItem)
            }

            override fun getOldListSize(): Int = delegated.size
            override fun getNewListSize(): Int = elements.size

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val oldItem = delegated[oldItemPosition]
                val newItem = elements[newItemPosition]
//                Log.e(tag, "content the same: old-> $oldItemPosition new-> $newItemPosition diff: ${oldItem.areContentsTheSame(newItem)}")
                return oldItem.areContentsTheSame(newItem)
            }
        }
        val result = DiffUtil.calculateDiff(diffCallback, true)
        delegated.clear()
        delegated.addAll(elements)
        ensureControllers(elements)
        result.dispatchUpdatesTo(observer)
    }

    fun refreshAll(init: MutableList<Item>.() -> Unit) = refreshAll(mutableListOf<Item>().apply(init))

    fun autoRefresh(init: MutableList<Item>.() -> Unit) {
        val snapshot = this.itemListSnapshot.toMutableList()
        snapshot.apply(init)
        refreshAll(snapshot)
    }

}

internal fun log(tag: String, message: String) {
    Log.e(tag, message)
}


