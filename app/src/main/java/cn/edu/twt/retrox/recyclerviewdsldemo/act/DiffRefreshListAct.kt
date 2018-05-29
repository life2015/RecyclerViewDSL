package cn.edu.twt.retrox.recyclerviewdsldemo.act

import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import cn.edu.twt.retrox.recyclerviewdsl.ItemAdapter
import cn.edu.twt.retrox.recyclerviewdsl.ItemManager
import cn.edu.twt.retrox.recyclerviewdsl.withItems
import cn.edu.twt.retrox.recyclerviewdsldemo.*
import org.jetbrains.anko.textColor

class DiffRefreshListAct : AppCompatActivity() {
    lateinit var itemManager: ItemManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val recyclerView: RecyclerView = findViewById(R.id.recyclerview)
        recyclerView.layoutManager = LinearLayoutManager(this)
        itemManager = ItemManager()
        recyclerView.adapter = ItemAdapter(itemManager)

// wipe the list and refresh
// you could implement areItemsTheSame areContentsTheSame and the framework do the diff for you
//        itemManager.refreshAll {
//
//        }

        refreshList()
    }

    private fun refreshList() {
        /**
         * function autoRefresh don't wipe the data of list
         * you should customize the thing needed to do when it refresh (it create a snapshot of list internally and use DiffUtil)
         * in this function : Every Time we refresh , remove the last Button item , then add some Text Item, at Last we add the button at Last
         */
        itemManager.autoRefresh {
            if (size > 0 && last() is ButtonItem) removeAt(size - 1)
            val currentSize = size
            repeat(10) {
                advancedText("This is Item : ${currentSize + it}") {
                    textSize = if (it > 5) 14f else 18f
                }
            }
            buttonItem("Add Items") {
                setOnClickListener {
                    refreshList()
                }
            }
        }
    }
}