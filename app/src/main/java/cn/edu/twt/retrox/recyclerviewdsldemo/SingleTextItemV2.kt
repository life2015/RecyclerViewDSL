package cn.edu.twt.retrox.recyclerviewdsldemo

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import cn.edu.twt.retrox.recyclerviewdsl.Item
import cn.edu.twt.retrox.recyclerviewdsl.ItemController
import org.jetbrains.anko.layoutInflater

/**
 * Just do something new with DSL
 * we could pass View.() -> Unit
 */
class SingleTextItemV2(val content: String, val init: TextView.() -> Unit) : Item {

    /**
     * implements these functions to delegate the core method of RecyclerView's Item
     */
    companion object Controller : ItemController {
        override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
            val inflater = parent.context.layoutInflater
            val view = inflater.inflate(R.layout.item_single_text, parent, false)
            val textView = view.findViewById<TextView>(R.id.tv_single_text)
            return ViewHolder(view, textView)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, item: Item) {
            /**
             * with the help of Kotlin Smart Cast, we can cast the ViewHolder and item first.
             * the RecyclerView DSL framework could guarantee the holder and item are correct, just cast it !
             *
             * 因为Kotlin的智能Cast 所以后面我们就不需要自己强转了
             * DSL 框架可以保证holder和item的对应性
             */
            holder as ViewHolder
            item as SingleTextItemV2

            /**
             * what you do in OnBindViewHolder in RecyclerView, just do it here
             */
            holder.textView.text = item.content
            // custom settings for TextView passed by DSL
            holder.textView.apply(item.init)
        }

        /**
         * define your ViewHolder here to pass view from OnCreateViewViewHolder to OnBindViewHolder
         * this ViewHolder class should be private and only use in this scope
         *
         * 在这里声明此Item所对应的ViewHolder，用来从OnCreateViewHolder传View到OnBindViewHolder中。
         * 这个ViewHolder类应该是私有的，只在这里用
         */
        private class ViewHolder(itemView: View?, val textView: TextView) : RecyclerView.ViewHolder(itemView)
    }

    /**
     * ItemController is necessary , it is often placed in the Item's companion Object
     * DON'T new an ItemController , because item viewType is corresponding to ItemController::class.java
     * or you will get many different viewType (for one type really) , which could break the RecyclerView's Cache
     *
     * 一般来讲，我们把ItemController放在Item的伴生对象里面，不要在这里new ItemController，因为在自动生成ViewType的时候，
     * 我们是根据ItemController::class.java 来建立一一对应关系，如果是new的话，会导致无法相等以至于生成许多ItemType，这样子会严重破坏Recyclerview的缓存机制
     */
    override val controller: ItemController
        get() = Controller

    override fun areContentsTheSame(newItem: Item): Boolean {
        return newItem is SingleTextItemV2 && content == newItem.content
    }

    override fun areItemsTheSame(newItem: Item): Boolean = this.areContentsTheSame(newItem)

}

/**
 * wrap the add SingleTextItem function with DSL style
 *
 * 用DSL来风格来简单保证add SingleTextItem的操作
 */
fun MutableList<Item>.advancedText(content: String, init: TextView.() -> Unit) = add(SingleTextItemV2(content, init))