# 构建Recyclerview DSL

> 接文章 DSL in action

上一篇文章说了如何把DSL用在项目的布局中，而这篇文章来讲讲怎么把DSL用在Recyclerview中。此框架已经在我的项目中大规模使用，并且极大地提高了Recyclerview列表构建效率和复用能力。

PS : I'm still working on the English Docment now. But the comments in the demo code are written in both Chinese and English.

## 特色
- 轻量级（只有一个Kotlin文件）
- 可拓展（你可以完全自定义自己的Item）
- 易用（它只是对Rec的`OnCreateVH` `OnBindVH`做了代理，不需要额外的学习成本）
- 写着爽（Anko风格写法，DSL配置列表灵活易用）

## 看看效果？

> 这是一个大概的效果，Recyclerview DSL中，我们可以用DSL的风格去配置Item被如何加入到Rec，各个Item的风格是什么样子，具有很大的灵活性和拓展性。

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

## 核心类概览

- `Item`： Recyclerview DSL中，用来保存View对应数据的类，比如说TextView的字符串，Imageview的url等等，基本上可以认为是担任着ViewModel的角色
- `ItemController`： 一般内嵌在`Item`类的`Companion Object`中，用于代理Item相关的`OnCreateVH`,`OnBindVH`逻辑，基本上一个Item的View逻辑和业务逻辑在这里表现。
- `ItemAdapter`：Recyclerview DSL所依赖的Adapter，在初始化的时候会用到，后面它很少出面了
- `ItemManager`:  RecyclerView DSL的Adapter的一个核心成员变量，统管着Adapter的Item和相应的ItemController，比如说他们的刷新，添加，删除。DSL的语法特性拓展，基本上在这里表现。

### 那怎么用？

- 定义列表要用的Item（可以全局复用 所以要好好设计）
- 写一个`MutableList<Item>`的拓展
- 开始使用！

### 举个栗子？

> 比如说我要写定义一类Item，这类Item就是一个FrameLayout里面包了个TextView。

然后怎么写呢？

1. 先定义一个Item，我们就叫它`SingleTextItem.kt`
   这个Item里面需要包含一个字符串，将来在`OnBindVH`的代理中传入到`View`中

   ```kotlin
   /**
    * 你自己定义的Item 示例：只有一个Text的Item
    * your custom Item
    * example: a RecyclerView Item contain a single TextView
    */
   class SingleTextItem(val content: String) : Item {
       override val controller: ItemController
           get() = TODO("Controller Need")
   
   }
   ```

2. 然后我们需要些这类Item对于的逻辑，也就是`ItemController`，在伴生对象中进行实现

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

3. 写个拓展函数，来让它支持DSL 

   ```kotlin
   /**
    * wrap the add SingleTextItem function with DSL style
    *
    * 用DSL来风格来简单保证add SingleTextItem的操作
    */
   fun MutableList<Item>.singleText(content: String) = add(SingleTextItem(content))
   ```

4. 来试试把，用一下~ 

   ```kotlin
   val recyclerView: RecyclerView = findViewById(R.id.recyclerview)
           recyclerView.layoutManager = LinearLayoutManager(this)
   
           recyclerView.withItems {
               repeat(10) {
                   singleText("this is a single Text: $it")
               }
           }
   ```

## 复杂情景讨论

#### 情景1： 同一个Item下，对于ViewStyle的不同处理

方案：Item中除了必要的数据类，再传入一个 `YourView.() -> Unit`类型的可空`?`闭包。

原理蛮简单，就弄代码了，注释很全… 还是中英双语的呢

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

#### 情景2 ： 可刷新列表

> 比如说，分页加载，列表变化，和其他所有可变的Recyclerview列表

方案：这种情况下，我们把`ItemManager`拿出来单独操作即可，善用`autorefresh`方法和`DiffUtil`

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

想要更加好的刷新体验，就要先给给RecyclerviewDSL加入DiffUtil的能力 ： 

```kotlin
interface Item {
    val controller: ItemController

    fun areItemsTheSame(newItem: Item): Boolean = false

    fun areContentsTheSame(newItem: Item): Boolean = false
}
```

实现Item接口的时候， 重写后面那俩默认方法即可。
比如说我们要做一个列表，列表里面是一堆文字的item，在最末尾有一个Button，点击Button就会让文字Item添加10个。然后在`autoRefresh`的闭包中，我们只需要用DSL来表达这个需求即可。框架会帮我们做这一切。

```kotlin
 /**
  * function autoRefresh don't wipe the data of list
  * you should customize the thing needed to do when it refresh (it create a snapshot of list internally and use DiffUtil)
  * in this function : Every Time we refresh , remove the last Button item , then add some Text Item, at Last we add the button at Last
  */
itemManager.autoRefresh {
    if (size > 0 && last() is ButtonItem) removeAt(size - 1) // 如果最后一个是ButtonItem 移除
    val currentSize = size
    repeat(10) {
        advancedText("This is Item : ${currentSize + it}") {
            textSize = if (it > 5) 14f else 18f
        }
    }
    buttonItem("Add Items") { // 添加ButtonItem
        setOnClickListener {
            refreshList()
        }
    }
}
```

AutoRefresh背后的原理就是，在调用闭包前，对Adapter的Item做一个SnapShot，然后对比AutoRefresh闭包使用之后的ItemList情况，最后使用DiffUtil来处理。

如果你是要对列表进行全量刷新，可以直接使用`refreshll`方法，此方法会清除列表然后再添加新的Item，当然这个过程是有DiffUtil参与的。

#原理/动机分析

### 常规开发

如果按照普通的开发流程，构建列表的时候，一般就是 Adapter + List。 Adapter里面包含着ViewHolder的创建和绑定逻辑，这样子在大规模开发迭代中会遇到的一个问题是：Adapter的逻辑越堆积越重，比如说在`OnBindViewHolder`方法中包含着重度的业务逻辑，`getItemViewType`,`onCreateViewHolder`中包含着大量的样板代码。

- 定义ViewType常量
- `getItemViewType`中各种判断
- `OnCreateViewHolder`中做创建
- `OnBindViewHolder`做数据绑定

这些代码都会堆积在Adapter中，时间一长，Type一多，Adapter写起来就会很蛋疼。另外，`ViewType/ViewHolder/BindViewHolder`逻辑都很难去复用，因为他们是写死在ViewHolder里面的。

### 简单优化一下？

> 我们开始思考，这些东西是不是可以解耦开呢？

于是你觉得，OnBindViewHolder的逻辑可以写在ViewHolder里面，然后

```kotlin
class CourseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val cardView: CardView = itemView.findViewById(R.id.cv_item_course)
    val textView: TextView = itemView.findViewById(R.id.tv_item_course)

    fun bind(course: Course) {
        // balabalabla
        //各种逻辑各种逻辑
    }
}

// 然后你的OnBindViewHolder方法就简单多了
override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
    //这里可以用ViewType / holder instance of 来处理多种VH
    val course = courseList[position]
    holder.bind(course)
}

```

在这种架构下，可以把ViewHolder独立开，解耦一部分Adapter中的逻辑。嗯… 还可以（没啥技术含量）

### 问题/不足

- ViewHolder复用问题：
  我们只解耦了`OnBindViewHolder`的逻辑，但`OnCreateViewHolder`还是要再写
- 复用灵活性问题：
  比如说我在复用的时候，Adapter1里面对`CardView`要设置1dp的阴影，Adapter2里面需要3dp。
  Adapter1里面对这类ViewHolder里面的TextView要设置：字体，颜色，字号。Adapter2里面需要另外的配置。
  又比如说，Adapter1里面对于不同地方的同类ViewHolder里面的TextView要设置：字体，颜色，字号等等….
- ViewType问题：
  我们真的需要手动指定ViewType吗，因为经过我的一番思考，ViewType和`ViewHolder::class.java`在合理的封装下，可以是1对1的关系。

### 再次思考 - 到底要怎么解耦？

> 于是我开始思考在Recyclerview的架构中，确定一类视图到底需要什么？哪些东西可以用一个最小的集合来定义一类视图？

**我们来梳理一下：**

```
展现给用户看的东西 = 视图 + 填充数据
视图 <- OnCreateViewHolder中相关逻辑
数据填充 <- OnBindViewHolder中把数据Set到View中
```

所以说，只要我们把`OnCreateVH`,`OnBindVH`的逻辑代理出去，就可以把一类Item的视图部分进行完整的解耦。**给太子端代码！**

```kotlin
interface ItemController {
    fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder // 视图
    fun onBindViewHolder(holder: RecyclerView.ViewHolder, item: Item) // 这里还需要具体实现 -> 视图填充
}
```

现在我们解耦出了视图，还剩下**视图的数据填充**。一般来讲，Model数据类型和ViewHolder类型一一对应，因此我们可以认为一种ItemController对应着一个类型的Item(一般就是嵌入的一个data Class)

于是我们把数据类嵌入进去

```kotlin
interface Item {
    val controller: ItemController // 这里应该用companion object
}
```

比如说我们有一个高度定制的TextView

```kotlin
class IndicatorTextItem(val text: String) : Item {
    private companion object Controller : ItemController {
        override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
            val view = parent.context.layoutInflater.inflate(R.layout.schedule_item_indicator, parent, false)
            return IndicatorTextViewHolder(view)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, item: Item) {
            holder as IndicatorTextViewHolder
            item as IndicatorTextItem
            holder.indicatorTextView.text = item.text
        }

        private class IndicatorTextViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val indicatorTextView: TextView = itemView.findViewById(R.id.tv_course_indicator)
        }

    }

    override val controller: ItemController get() = Controller
    
}
```

在这里，我们就已经把IndicatorTextView这个Recyclerview Item的视图层和数据填充都解耦了出来。只需要塞进去`IndicatorTextItem`对象，就可以做到相应的效果。并且这个Item可以在多个Recyclerview Adapter中复用。

## Adapter如何协调？

> 与这套解耦相配合的是一套Adapter的封装，来对接相关的接口完成对应逻辑的解耦已经ViewType的分配

对于Adapter，我们需要完成的逻辑就是 `ItemController` <----> `ViewType`的转换。

**一个理论前提是**：在高度封装的情况下，ViewType并没有具体的语义，它的作用在于区分不同的`ItemController`。而对于具体的语义，则转到Item那边来表示，比如说上面的`class IndicatorTextItem(val text: String) : Item `。

**落实到方法上**：我们可以实现一套`ItemController` <----> `ViewType`的注册机制，那么这套机制的具体需求是什么？应该怎样设计？先列下需求：

- 一对一的关系 支持相互索引
- 照顾ViewHolder的全局复用
- ViewType自动生成 
- 添加Item时自动注册

**一对一的关系 支持相互索引：**我们可以维护两个Map

```kotlin
// controller to view type
private val c2vt = mutableMapOf<ItemController, Int>()

// view type to controller
private val vt2c = mutableMapOf<Int, ItemController>()
```

因为要保证Key，Value的相互之前快速索引，因此需要同时管理这两个Map。

**添加Item时自动注册 + ViewType自动生成 ：**Item接口要求必须有一个`controller`成员变量，因此在添加到Item List的同时，进行监听。不如来看看代码

```kotlin
object ItemControllerManager {
    private var viewType = 0 // object保证了单例 因此ViewType肯定是从0开始

    // controller to view type
    private val c2vt = mutableMapOf<ItemController, Int>()

    // view type to controller
    private val vt2c = mutableMapOf<Int, ItemController>()

    /**
     * 检查Item(对应的controller)是否已经被注册，如果没有，那就注册一个ViewType
     */
    fun ensureController(item: Item) {
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
    fun ensureControllers(items: Collection<Item>): Unit =
            items.distinctBy(Item::controller).forEach(::ensureController)

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

```

在Adapter 的数据源修改时，调用相关的`ensureControllers`方法来完成相关的注册。同时Adapter中，相关的逻辑也可以被这里的ItemController代理，代码差不多是这样子的：

```kotlin
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ItemManager.getController(viewType).onCreateViewHolder(parent)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
            itemManager[position].controller.onBindViewHolder(holder, itemManager[position])

```

在这种情况下，Adapter的两个核心方法就被代理出去了，实现了不同VH逻辑的隔离。

关于自动注册ItemType，我们的做法是实现MutableList接口，内部组合一个普通的MutableList，对`add`,`addAll`,`remove`之类方法进行AOP处理，这些方法的执行的同时，自动检测或者注册`ItemController`，同时对于Adapter进行相应的Notify，这样子就可以实现一个轻量级的MVVM。

在这里，其实我们可以做很多事情，比如说代理出DiffUtil来进行自动Diff

```kotlin
interface Item {
    val controller: ItemController

    fun areItemsTheSame(newItem: Item): Boolean = false

    fun areContentsTheSame(newItem: Item): Boolean = false
}
```



