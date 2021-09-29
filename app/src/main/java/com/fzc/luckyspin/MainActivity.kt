package com.fzc.luckyspin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.google.gson.Gson

class MainActivity : AppCompatActivity() {

    private lateinit var luckySpin: LuckySpinView

    companion object {
        const val ASSETS_REWARD = "reward.json"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.btn_start).setOnClickListener {
            if (!luckySpin.isRunning()) {
                luckySpin.startLuckyWheelWithRandomTarget()
            }
        }
        initLuckWheel()
    }

    private fun initLuckWheel() {
        luckySpin = findViewById(R.id.luckySpin)
        luckySpin.setCenterImage(R.mipmap.luckywheel_ic_center)
        luckySpin.setBackgroundImg(R.mipmap.luckywheel_ic_bound)
        luckySpin.setData(initData())
    }

    private fun initData(): List<LuckyReward> {
        val jsonStr = String(assets.open(ASSETS_REWARD).readBytes())
        val luckyItem = Gson().fromJson(jsonStr, LuckyItem::class.java)
        return luckyItem.rewardList
    }

    override fun onStop() {
        super.onStop()
        if (luckySpin.isRunning()) {
            luckySpin.cancelRotate()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (luckySpin.isRunning()) {
            luckySpin.cancelRotate()
        }
    }
}