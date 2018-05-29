package cn.edu.twt.retrox.recyclerviewdsldemo

import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import cn.edu.twt.retrox.recyclerviewdsl.withItems
import cn.edu.twt.retrox.recyclerviewdsldemo.act.DiffRefreshListAct
import cn.edu.twt.retrox.recyclerviewdsldemo.act.TextListActivity
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.textColor

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val recyclerView: RecyclerView = findViewById(R.id.recyclerview)
        recyclerView.layoutManager = LinearLayoutManager(this)

        /**
         * Let's GO
         * A simple use of RecyclerView DSL
         */
        recyclerView.withItems {
            buttonItem("Simple Text List") {
                setOnClickListener {
                    startActivity<TextListActivity>()
                }
            }

            buttonItem("Diff Refresh Text List") {
                setOnClickListener {
                    startActivity<DiffRefreshListAct>()
                }
            }
        }
    }
}
