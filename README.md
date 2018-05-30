# Build RecyclerView With DSL

[ ![Download](https://api.bintray.com/packages/retrox/RecyclerViewDSL/recyclerviewdsl/images/download.svg) ](https://bintray.com/retrox/RecyclerViewDSL/recyclerviewdsl/_latestVersion)

> Let's talk about how to use DSL in RecyclerView, this framework has been widely used in my project.
 It could help you build RecyclerView List in DSL Style like Anko Layout.

```groovy
implementation 'cn.edu.twt.retrox:recyclerviewdsl:x.y.z' //the use display on the badge above
```
[中文文档以及原理介绍](https://github.com/life2015/RecyclerViewDSLDemo/blob/master/README_ZH.md)

## Features
- Super Light Weight (Only one kotlin file)
- Extensible (You can create your own items)
- Easy to use (Just a simple delegate of `OnCreateVH` and `OnBindVH` )
- Flexible (Anko Style DSL to build RecyclerView, much better than original Adapter)

## Code Sample
```kotlin
itemManager.refreshAll {
    val books = viewModel.getBooks()
    val bookShelfs = viewModel.getBookShelfs()
    header {
        text = "DSL header"
        color = Color.BLUE
    }
    book.foreach { book ->
        bookItem {
            title = if (book.id != 0) book.title else "Empty Book"
            date = book.returnDate
            url = book.imageUrl
        }
    }
    bookShelfs.foreachIndexed { index, bookShelf ->
        bookShelf {
            title = "Number$index Shelf - ${bookShelf.name}"
            size = bookShelf.size
            url = bookShelf.imageUrl
            onclick {
                startActivity<BookShelfActivity>("id" to bookShelf.id)
            }
        }    
    }
    footer {
        text = "Load More"
        onClick {
            loadMore()
        }
    }
}
```

## Core Classes Introduction
- `Item`: In RecyclerView, `Items` are used to store the corresponding data of the itemView. Such as the String content of a TextView,
the url of ImageView. We can treat them as a role of ViewModel.
- `ItemController`: Embedded in the `Item` class's Companion Object, used to delegate the `OnCreateVH` `OnBindVH` method. 
We often put our view logic and business logic here.
- `ItemAdapter`: The Adapter for RecyclerView DSL. We often use it just for RecyclerView initialization, then we use ItemManager instead of directly use the ItemAdapter.
- `ItemManager`: The core of RecyclerView DSL. It manage all the `Items` and their corresponding `ItemController`, and the list's refresh, add, remove.
DSL extensions are also implemented by ItemManager

## How to use 
- Define the Item which RecyclerView DSL will use (the Items can be reused global so design them properly with consideration of Expandability)
- Write an extension function of `MutableList<Item>`
- Just Use it!

## Give me an Example!
> Assume I want to define an Item which represents a single TextView inside FrameLayout

Then what we should do with RecyclerView DSL Framework?
1. Define an Item, we call it `SingleTextItem.kt` it should implements `Item` interface
In this Item, we need a String represents the text content, then pass it to the `OnBindVH` delegate method later
```kotlin
/**
 * 你自己定义的Item 示例：只有一个Text的Item
 * your custom Item goes there
 * example: a RecyclerView Item contain a single TextView
 */
class SingleTextItem(val content: String) : Item {
    override val controller: ItemController
        get() = TODO("Controller Need")
}
```
2.Then we should write the view logic and business logic in the Companion Object which implements ItemController
```kotlin
/**
 * 你自己定义的Item 示例：只有一个Text的Item
 * your custom Item
 * example: a RecyclerView Item contain a single TextView
 */
class SingleTextItem(val content: String) : Item {
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
            item as SingleTextItem
            /**
             * what you do in OnBindViewHolder in RecyclerView, just do it here
             */
            holder.textView.text = item.content
        }
        /**
         * define your ViewHolder here to pass view from OnCreateViewViewHolder to OnBindViewHolder
         * this ViewHolder class should be private and only use in this scope
         *
         * 在这里声明此Item所对应的ViewHolder，用来从OnCreateViewHolder传View到OnBindViewHolder中。
         * 这个ViewHolder类应该是私有的，只在这里用
         */
        private class ViewHolder(itemView: View?,val textView: TextView) : RecyclerView.ViewHolder(itemView)
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
}
```
3. Write an extension function to make it support DSL
```kotlin
/**
 * wrap the add SingleTextItem function with DSL style
 *
 * 用DSL来风格来简单保证add SingleTextItem的操作
 */
fun MutableList<Item>.singleText(content: String) = add(SingleTextItem(content))
```
4.Let's have a try!
```kotlin
val recyclerView: RecyclerView = findViewById(R.id.recyclerview)
recyclerView.layoutManager = LinearLayoutManager(this)
recyclerView.withItems {
    repeat(10) {
        singleText("this is a single Text: $it")
    }
}
```
## Complex situation 

#### Situation 1: Different ViewStyle with the same Item
**Solution**: pass an `YourView.() -> Unit` Closure in addition to Other necessary data into `Item`. 
Your could even pass this Closure only then leave every thing to DSL config.
It is not hard, so I pasted the code with detailed comments here.
```kotlin
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
```
then you can use it.
```kotlin
recyclerView.withItems {
            repeat(10) {
                singleText("this is a single Text: $it")
            }
            repeat(20) {
                advancedText("this is Advanced Text $it") {
                    when (it) {
                        in 1..10 -> textColor = Color.RED
                        in 11..15 -> textSize = 20f
                        else -> textColor = Color.GREEN
                    }
                }
            }
        }
```

#### Situation 2: We need to refresh the List with our own logic
> such as partial refresh, load by page, full amount refresh etc.

**Solution**: In this kind of situation, we take the ItemManager out and operate it alone. Then use the `autoRefresh` `refreshAll` functions to do this.
```kotlin
lateinit var itemManager: ItemManager
val recyclerView: RecyclerView = findViewById(R.id.recyclerview)
recyclerView.layoutManager = LinearLayoutManager(this)
itemManager = ItemManager()
recyclerView.adapter = ItemAdapter(itemManager)
itemManager.autoRefresh {
 // do something here 
 // see cn.edu.twt.retrox.recyclerviewdsldemo.act.DiffRefreshListAct
}
```
**PS**: You can override the `areItemsTheSame` `areContentsTheSame` function to take the advantage of the built in DiffUtil for better user experience when refreshing a List.
```kotlin
interface Item {
    val controller: ItemController
    fun areItemsTheSame(newItem: Item): Boolean = false
    fun areContentsTheSame(newItem: Item): Boolean = false
}
```
For example, we want to build a list, which has a ButtonItem at bottom, every time a tap the button, the number of TextItems above it increase by 10.
```
 text               text
 text               text
 ...                text
 text               text
 Button -> tap ->   ...
                    text
                    text
                    text
                    Button
```
We just need to describe this by DSL in the `autoRefresh` Closure. Then the framework do all the things left.
```kotlin
 /**
  * function autoRefresh don't wipe the data of list
  * you should customize the thing needed to do when it refresh (it create a snapshot of list internally and use DiffUtil)
  * in this function : Every Time we refresh , remove the last Button item , then add some Text Item, at Last we add the button at Last
  */
itemManager.autoRefresh {
    if (size > 0 && last() is ButtonItem) removeAt(size - 1) // if the last one is Button, remove it
    val currentSize = size
    repeat(10) {
        advancedText("This is Item : ${currentSize + it}") { // add textItems
            textSize = if (it > 5) 14f else 18f
        }
    }
    buttonItem("Add Items") { // we add the buttonItem again at bottom
        setOnClickListener {
            refreshList()
        }
    }
}
```
The magic behind the `autoRefresh` is: it create a snapshot of the current item list of the adapter. 
What we do in the Closure is applied to the snapShot, then we use DiffUtil to diff the modified snapshot and the original adapter list, 
then we apply the snapshot to the adapter and Diffutil dispatch DiffResult to RecyclerView

If you just want to do full amount refresh,you can use the `refreshAll` function.