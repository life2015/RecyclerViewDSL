package cn.edu.twt.retrox.recyclerviewdsldemo.act

import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import cn.edu.twt.retrox.recyclerviewdsl.withItems
import cn.edu.twt.retrox.recyclerviewdsldemo.R
import cn.edu.twt.retrox.recyclerviewdsldemo.advancedText
import cn.edu.twt.retrox.recyclerviewdsldemo.buttonItem
import cn.edu.twt.retrox.recyclerviewdsldemo.singleText
import org.jetbrains.anko.textColor

class TextListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val recyclerView: RecyclerView = findViewById(R.id.recyclerview)
        recyclerView.layoutManager = LinearLayoutManager(this)

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
    }
}