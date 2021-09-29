package com.fzc.luckyspin

import android.animation.Animator
import android.animation.TimeInterpolator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

data class LuckyItem(
    var rewardList: List<LuckyReward>,
    var winIds: List<Int>,
)

data class LuckyReward(var id: Int, var money: Int)

class LuckySpinView : View {

    private var mOutRange = RectF()
    private var mInnerRange = RectF()
    private var mOutRadius = 0
    private var mInnerRadius = 0
    private lateinit var mArcPaint: Paint
    private lateinit var mBackgroundPaint: Paint
    private lateinit var mTextPaint: TextPaint
    private lateinit var mTextStrokePaint: TextPaint
    private val mStartAngle = 0f
    private val mCenterPoint = PointF()
    private var mPadding = 0
    private var mTopTextPadding = 0
    private var mTextSize = 0F
    private var mRoundOfNumber = 2
    private var mBorderWidth = -1
    private var isRunning = false
    private var mBorderColor = 0
    private var mBackgroundColor = 0
    private var mTextColor = 0
    private lateinit var mRewardList: List<LuckyReward>
    private var mRotateListener: IRotateListener? = null
    @DrawableRes
    private var mBackgroundImgId: Int = 0
    @DrawableRes
    private var mCenterImageId: Int = 0
    private var mCenterBitmap: Bitmap? = null
    private var targetIndex: Int = INVALIDATE_INDEX

    private val rewardListSize: Int
        get() = mRewardList.size

    interface IRotateListener {
        fun rotateDone(index: Int)
    }

    constructor(context: Context?) : super(context) {
        initPaint()
    }

    companion object {
        const val DEFAULT_BACKGROUND_COLOR = Color.YELLOW
        const val DEFAULT_TEXT_SIZE = 30f
        const val DEFAULT_TEXT_PADDING = 30f
        const val DEFAULT_TEXT_COLOR = Color.BLACK
        const val DEFAULT_BORDER_WIDTH = 1f
        const val DEFAULT_BORDER_COLOR = Color.TRANSPARENT

        const val ITEM_CYCLE = 4
        val ONE_COLOR = Color.rgb(251, 6, 84)
        val TWO_COLOR = Color.rgb(255, 216, 99)
        val THREE_COLOR = Color.rgb(139, 38, 253)
        val FOUR_COLOR = Color.rgb(255, 245, 253)

        val TEXT_COLOR_YELLOW = Color.rgb(255, 216, 99)
        val TEXT_COLOR_WHITE = Color.rgb(255, 245, 253)

        val TEXT_STROKE_COLOR_ONE = Color.rgb(141, 27, 63)
        val TEXT_STROKE_COLOR_TWO = Color.rgb(149, 123, 43)
        val TEXT_STROKE_COLOR_THREE = Color.rgb(95, 45, 146)
        val TEXT_STROKE_COLOR_FOUR = Color.rgb(251, 6, 84)

        const val TEXT_STROKE_WIDTH = 3f

        const val ACCESS_ROTATE = 1
        const val DEC_ROTATE_NO_INDEX = 3
        const val DEC_ROTATE = 4
        const val INVALIDATE_INDEX = -1
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        if (attrs != null && context != null) {
            val typedArray =
                context.obtainStyledAttributes(attrs, R.styleable.LuckyWheelView)
            mBackgroundColor =
                typedArray.getColor(
                    R.styleable.LuckyWheelView_luckywheel_BackgroundColor,
                    DEFAULT_BACKGROUND_COLOR
                )
            mTextSize = typedArray.getDimensionPixelSize(
                R.styleable.LuckyWheelView_luckywheel_TopTextSize,
                Utils.convertDpToPixel(DEFAULT_TEXT_SIZE, context).toInt()
            ).toFloat()
            mTextColor =
                typedArray.getColor(
                    R.styleable.LuckyWheelView_luckywheel_TopTextColor,
                    DEFAULT_TEXT_COLOR
                )
            mTopTextPadding = typedArray.getDimensionPixelSize(
                R.styleable.LuckyWheelView_luckywheel_TopTextPadding,
                Utils.convertDpToPixel(DEFAULT_TEXT_PADDING, context).toInt()
            )
            mBorderWidth =
                typedArray.getDimensionPixelSize(
                    R.styleable.LuckyWheelView_luckywheel_BorderWidth,
                    Utils.convertDpToPixel(DEFAULT_BORDER_WIDTH, context).toInt()
                )
            mBorderColor =
                typedArray.getColor(
                    R.styleable.LuckyWheelView_luckywheel_BorderColor,
                    DEFAULT_BORDER_COLOR
                )
            mOutRadius = typedArray.getDimensionPixelSize(
                R.styleable.LuckyWheelView_luckywheel_OutRadius,
                0
            )
            mInnerRadius = typedArray.getDimensionPixelSize(
                R.styleable.LuckyWheelView_luckywheel_InnerRadius,
                0
            )
            typedArray.recycle()
        }
        initPaint()
    }

    private fun initPaint() {
        mArcPaint = Paint()
        mArcPaint.isAntiAlias = true
        mArcPaint.isDither = true

        mTextPaint = TextPaint()
        mTextPaint.style = Paint.Style.FILL
        mTextPaint.isAntiAlias = true
        mTextPaint.isDither = true
        mTextPaint.color = mTextColor
        mTextPaint.textSize = mTextSize
        mTextPaint.typeface = ResourcesCompat.getFont(context, R.font.impact)

        mTextStrokePaint = TextPaint(mTextPaint)
        mTextStrokePaint.style = Paint.Style.STROKE
        mTextStrokePaint.strokeWidth = TEXT_STROKE_WIDTH

        mBackgroundPaint = Paint()
        mBackgroundPaint.isAntiAlias = true
        mBackgroundPaint.isDither = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = measuredWidth.coerceAtMost(measuredHeight)
        mPadding = paddingLeft

        if (mOutRadius == 0) mOutRadius = width / 2
        if (mInnerRadius == 0) mInnerRadius = width / 2
        setMeasuredDimension(mOutRadius * 2, mOutRadius * 2)
    }

    /**
     * @param canvas
     */
    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        computeOutRange()
        computeInnerRange()
        computeCenterPoint()
        drawArc(canvas)
        drawCenterImg(canvas)
        drawOutBoundImg(canvas)
    }

    private fun computeOutRange() {
        val outDiff = if (mOutRadius - width / 2 > 0) mOutRadius - width / 2 else 0
        val left = mPadding - outDiff
        val top = mPadding
        val right = mPadding + width + outDiff
        val bottom = mPadding + mOutRadius * 2
        mOutRange.set(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
    }

    private fun computeInnerRange() {
        val diffRadius = mOutRadius - mInnerRadius
        val left = mOutRange.left + diffRadius
        val top = mOutRange.top + diffRadius
        val right = mOutRange.right - diffRadius
        val bottom = mOutRange.bottom - diffRadius
        mInnerRange.set(left, top, right, bottom)
    }

    private fun computeCenterPoint() {
        mCenterPoint.x = width / 2f
        mCenterPoint.y = mOutRadius * 1f
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun drawCenterImg(canvas: Canvas) {
        if (mCenterImageId == 0) return
        if (mCenterBitmap == null) {
            val drawable = resources.getDrawable(mCenterImageId, null)
            val bitmap = Utils.drawableToBitmap(drawable)
            mCenterBitmap = Bitmap.createScaledBitmap(
                bitmap,
                drawable!!.intrinsicWidth,
                drawable.intrinsicHeight,
                false
            )
        }
        val bitmap = mCenterBitmap!!
        canvas.drawBitmap(
            bitmap, mCenterPoint.x - bitmap.width / 2.toFloat(),
            mCenterPoint.y - bitmap.height / 2.toFloat(), null
        )
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun drawOutBoundImg(canvas: Canvas) {
        val drawable = resources.getDrawable(mBackgroundImgId, null)
        drawable.bounds = Rect(
            mOutRange.left.toInt() ,
            mOutRange.top.toInt() ,
            mOutRange.right.toInt() ,
            mOutRange.bottom.toInt()
        )
        drawable.draw(canvas)
    }

    private fun getItemColorByIndex(index: Int): Int {
        return when {
            index % ITEM_CYCLE == 1 -> {
                ONE_COLOR
            }
            index % ITEM_CYCLE == 2 -> {
                TWO_COLOR
            }
            index % ITEM_CYCLE == 3 -> {
                THREE_COLOR
            }
            index % ITEM_CYCLE == 0 -> {
                FOUR_COLOR
            }
            else -> {
                ONE_COLOR
            }
        }
    }

    private fun getContentStrokeColor(index: Int): Int {
        return when {
            index % ITEM_CYCLE == 0 -> {
                TEXT_STROKE_COLOR_FOUR
            }
            index % ITEM_CYCLE == 1 -> {
                TEXT_STROKE_COLOR_ONE
            }
            index % ITEM_CYCLE == 2 -> {
                TEXT_STROKE_COLOR_TWO
            }
            index % ITEM_CYCLE == 3 -> {
                TEXT_STROKE_COLOR_THREE
            }
            else -> {
                TEXT_STROKE_COLOR_ONE
            }
        }
    }

    private fun drawArc(canvas: Canvas) {
        var tmpAngle = mStartAngle
        val sweepAngle = 360f / mRewardList.size

        for (i in mRewardList.indices) {
            fillArc(canvas, tmpAngle, sweepAngle, i)
            drawArcBorder(canvas, tmpAngle, sweepAngle)
            drawItemContentWithoutIcon(i, canvas, tmpAngle, sweepAngle)
            tmpAngle += sweepAngle
        }
    }

    private fun fillArc(canvas: Canvas, tmpAngle: Float, sweepAngle: Float, index: Int) {
        mArcPaint.style = Paint.Style.FILL
        mArcPaint.color = getItemColorByIndex(index)
        canvas.drawArc(mInnerRange, tmpAngle, sweepAngle, true, mArcPaint)
    }

    private fun drawArcBorder(canvas: Canvas, tmpAngle: Float, sweepAngle: Float) {
        mArcPaint.style = Paint.Style.STROKE
        mArcPaint.color = mBorderColor
        mArcPaint.strokeWidth = mBorderWidth.toFloat()
        canvas.drawArc(mInnerRange, tmpAngle, sweepAngle, true, mArcPaint)
    }

    private fun drawItemContentWithoutIcon(
        index: Int,
        canvas: Canvas,
        tmpAngle: Float,
        sweepAngle: Float
    ) {
        val reward = mRewardList[index]
        val rewardValue = generateContentText(reward.money)
        drawContentColor(index)
        drawContentStroke(index)
        if (!TextUtils.isEmpty(rewardValue)) drawRewardText(
            canvas,
            tmpAngle,
            sweepAngle,
            rewardValue
        )
    }

    private fun generateContentText(reward: Int): String {
        return when {
            reward > 100 * 10000 -> {
                "${(reward / 1000000F).toInt()}KK"
            }
            reward > 10000 -> {
                "${(reward / 1000F).toInt()}K"
            }
            else -> {
                "$reward"
            }
        }
    }

    private fun drawContentStroke(index: Int) {
        val strokeColor = getContentStrokeColor(index)
        mTextStrokePaint.color = strokeColor
    }

    private fun drawContentColor(index: Int) {
        val contentColor = if (index % ITEM_CYCLE == 0) TEXT_COLOR_YELLOW else TEXT_COLOR_WHITE
        mTextPaint.color = contentColor
    }

    private fun drawRewardText(
        canvas: Canvas,
        tmpAngle: Float,
        sweepAngle: Float,
        str: String,
        inCenter: Boolean = true
    ) {
        if (inCenter) {
            drawCenterText(canvas, tmpAngle, sweepAngle, str)
        } else {
            drawTopText(canvas, tmpAngle, sweepAngle, str)
        }
    }

    private fun drawTopText(
        canvas: Canvas,
        tmpAngle: Float,
        sweepAngle: Float,
        mStr: String
    ) {
        val path = Path()
        path.addArc(mOutRange, tmpAngle, sweepAngle)
        val textWidth = mTextPaint.measureText(mStr)
        val hOffset =
            (mOutRadius * 2 * Math.PI / mRewardList.size / 2 - textWidth / 2).toInt()
        val vOffset = mTopTextPadding
        canvas.drawTextOnPath(mStr, path, hOffset.toFloat(), vOffset.toFloat(), mTextPaint)
    }

    private fun drawCenterText(
        canvas: Canvas,
        tmpAngle: Float,
        sweepAngle: Float,
        str: String
    ) {
        val angle = ((tmpAngle + sweepAngle / 2) * Math.PI / 180).toFloat()

        val sinValue = sin(angle.toDouble())
        val cosValue = cos(angle.toDouble())

        val x = (mCenterPoint.x + mInnerRadius * cosValue).toFloat()
        val y = (mCenterPoint.y + mInnerRadius * sinValue).toFloat()

        val degrees = 180 + tmpAngle + sweepAngle / 2

        // 绘制中线, 用于内容校准
//        canvas.drawLine(mCenterPoint.x, mCenterPoint.y, x, y,mTextPaint)

        drawVerticalText(canvas, str, x, y, mTextPaint, degrees)

    }

    private fun drawVerticalText(
        canvas: Canvas,
        str: String,
        px: Float,
        py: Float,
        paint: Paint,
        degrees: Float
    ) {
        if (degrees != 0.0f) {
            canvas.rotate(degrees, px, py)
        }
        // 由于使用的是 impact 字体，所以需要加20的宽度
        val textWidth = mTextPaint.measureText("0").toInt().toFloat() + 20
        canvas.translate(textWidth / 2, textWidth / 2)
        canvas.drawText(str, px, py, paint)
        canvas.drawText(str, px, py, mTextStrokePaint)
        canvas.translate(-textWidth / 2, -textWidth / 2)
        if (degrees != 0.0f) {
            canvas.rotate(-degrees, px, py)
        }
    }

    /**
     * @param numberOfRound
     */
    fun setRound(numberOfRound: Int) {
        mRoundOfNumber = numberOfRound
    }

    fun rotate(rotateState: Int = ACCESS_ROTATE) {
        val accessRoundNumber = 3
        val accessRoundDuring = 3000L
        if (rotateState == ACCESS_ROTATE) {
            accessRotateZeroAngle(accessRoundDuring, accessRoundNumber)
            return
        }

        if (rotateState == DEC_ROTATE_NO_INDEX) { // accessRoundDuring 时间内没有得到响应
            decelerateRotate(360f * accessRoundNumber, accessRoundDuring)
            return
        }

        val targetAngle =
            360f * accessRoundNumber + 270f - getAngleOfIndexTarget(targetIndex) - 360f / mRewardList.size / 2
        decelerateRotate(targetAngle, accessRoundDuring)
    }

    private fun getAngleOfIndexTarget(index: Int): Float {
        return 360f / mRewardList.size * index
    }

    private fun decelerateRotate(angle: Float, during: Long) {
        val interpolator = DecelerateInterpolator()

        animate()
            .setInterpolator(interpolator)
            .setDuration(during + 500L)
            .setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                    isRunning = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    isRunning = false
                    rotation %= 360f
                    if (mRotateListener != null) {
                        val realIndex = if (rotation == 0f) INVALIDATE_INDEX else targetIndex
                        mRotateListener!!.rotateDone(realIndex)
                    }
                    targetIndex = INVALIDATE_INDEX
                }

                override fun onAnimationCancel(animation: Animator) {}

                override fun onAnimationRepeat(animation: Animator) {}
            })
            .rotation(angle)
            .start()
    }


    private fun accessRotateZeroAngle(during: Long, roundOfNumber: Int) {
        if (rotation != 0f) {
            rotation %= 360f
        }
        val interpolator = AccelerateInterpolator()
        animate()
            .setInterpolator(interpolator)
            .setDuration(during)
            .setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                    isRunning = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    rotation = 0f // 加速到角度为0的水平轴， 接着再减速到目标角度
                    if (targetIndex == INVALIDATE_INDEX) {
                        rotate(DEC_ROTATE_NO_INDEX)
                    } else {
                        rotate(DEC_ROTATE)
                    }
                }

                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })
            .rotation(360f * roundOfNumber)
            .start()
    }

    private fun rotateTo(index: Int) {
        HandlerUtils.runOnUiThread {
            val rand = Random()
            rotateTo(index, rand.nextInt() and 1, true)
        }
    }

    fun rotateTo(index: Int, rotation: Int, startShow: Boolean) {
        if (isRunning) {
            return
        }
        val rotationAssess = if (rotation <= 0) 1 else -1
        var roundOfNumber = mRoundOfNumber
        val accDuration = 1500L
        var decDuration = 1500L
        var interpolator: TimeInterpolator

        // 从上一次转盘最终的位置处旋转
        if (getRotation() != 0.0f) {
            setRotation(getRotation() % 360f)
            interpolator = AccelerateInterpolator()
            animate()
                .setInterpolator(interpolator)
                .setDuration(accDuration)
                .setListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(animation: Animator) {
                        isRunning = true
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        isRunning = false
                        setRotation(0f) // 加速到角度为0的水平轴， 接着再减速到目标角度
                        rotateTo(index, rotation, false)
                    }

                    override fun onAnimationCancel(animation: Animator) {}

                    override fun onAnimationRepeat(animation: Animator) {}
                })
                .rotation(360f * roundOfNumber * rotationAssess)
                .start()
            return
        }
        interpolator = DecelerateInterpolator()

        // 进入转盘后的第一次玩转盘, 先加速再减速
        if (startShow) {
            roundOfNumber = roundOfNumber shl 1
            decDuration = accDuration shl 1
            interpolator = AccelerateDecelerateInterpolator()
        }

        val targetAngle =
            360f * roundOfNumber * rotationAssess + 270f - getAngleOfIndexTarget(index) - 360f / mRewardList.size / 2
        animate()
            .setInterpolator(interpolator)
            .setDuration(decDuration + 500L)
            .setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                    isRunning = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    isRunning = false
                    setRotation(getRotation() % 360f)
                    if (mRotateListener != null) {
                        mRotateListener!!.rotateDone(index)
                    }
                }

                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })
            .rotation(targetAngle)
            .start()
    }

    fun setTargetIndex(index: Int) {
        targetIndex = index
    }

    fun startLuckyWheelWithTargetIndex(index: Int) {
        this.rotateTo(index)
    }

    fun startLuckyWheelWithRandomTarget() {
        val r = Random()
        this.rotateTo(r.nextInt(this.rewardListSize - 1))
    }

    fun cancelRotate() {
        animate().cancel()
    }

    fun setBackgroundImg(resId: Int) {
        if (resId <= 0) return
        this.mBackgroundImgId = resId
    }

    fun setCenterImage(@DrawableRes resId: Int ) {
        this.mCenterImageId = resId
    }

    fun setRotateListener(listener: IRotateListener?) {
        this.mRotateListener = listener
    }

    fun isRunning(): Boolean {
        return this.isRunning
    }

    fun setData(rewardList: List<LuckyReward>) {
        mRewardList = rewardList
        invalidate()
    }
}

fun Int.dpToPx():Int {
   return Utils.dpToPx(this)
}