package cn.edu.twt.retrox.recyclerviewdsldemo

import android.support.v7.widget.RecyclerView
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import cn.edu.twt.retrox.recyclerviewdsl.Item
import cn.edu.twt.retrox.recyclerviewdsl.ItemController
import org.jetbrains.anko.button
import org.jetbrains.anko.frameLayout
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.wrapContent
import kotlin.properties.Delegates

/**
 * you can also use anko in your ItemController
 */
class ButtonItem(val buttonText: String, val init: Button.() -> Unit) : Item {
    companion object Controller : ItemController {
        override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
            var button: Button by Delegates.notNull()
            val view = parent.context.frameLayout {
                layoutParams = RecyclerView.LayoutParams(matchParent, wrapContent)

                button = button {
                    textSize = 16f
                }.lparams(wrapContent, wrapContent) {
                    gravity = Gravity.CENTER_HORIZONTAL
                }
            }
            return ViewHolder(view, button)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, item: Item) {
            holder as ViewHolder
            item as ButtonItem
            holder.button.apply {
                text = item.buttonText
                item.init.invoke(this)
            }
        }

        private class ViewHolder(itemView: View?, val button: Button) : RecyclerView.ViewHolder(itemView)
    }

    override val controller: ItemController
        get() = Controller


    override fun areContentsTheSame(newItem: Item) = newItem is ButtonItem && newItem.buttonText == buttonText

    override fun areItemsTheSame(newItem: Item): Boolean = this.areContentsTheSame(newItem)

}

fun MutableList<Item>.buttonItem(text: String, init: Button.() -> Unit) = add(ButtonItem(text, init))