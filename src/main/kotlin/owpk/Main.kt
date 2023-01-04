package owpk

import com.github.kwhat.jnativehook.GlobalScreen
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener
import owpk.mathevaluator.MathExpression
import java.util.*
import java.util.function.Predicate
import kotlin.math.abs

fun main(args: Array<String>) {
    val monitor = Monitor()
    val field = UiField(8, 12, monitor)

    val button0 = Button(field, "0", 3, 4)
    val button1 = Button(field, "1", 1, 1)
    val button2 = Button(field, "2", 3, 1)
    val button3 = Button(field, "3", 5, 1)
    val button4 = Button(field, "4", 1, 2)
    val button5 = Button(field, "5", 3, 2)
    val button6 = Button(field, "6", 5, 2)
    val button7 = Button(field, "7", 1, 3)
    val button8 = Button(field, "8", 3, 3)
    val button9 = Button(field, "9", 5, 3)

    val buttonPow = Button(field, "^", 9, 2)
    val buttonCloseBracket = Button(field, ")", 9, 1)
    val buttonOpenBracket = Button(field, "(", 7, 1)
    val buttonPlus = Button(field, "+", 7, 2)
    val buttonMinus = Button(field, "-", 7, 3)
    val buttonMul = Button(field, "*", 7, 4)
    val buttonDiv = Button(field, "/", 7, 5)

    val buttonEq = object : Button(field, "=", 9, 3) {
        override fun selectEvent() {
            field.getMonitor().evalContent()
        }
    }
    
    val buttonClear = object : Button(field, "C", 9, 4) {
        override fun selectEvent() {
            field.getMonitor().content = ""
        }
    }

    val buttonErase = object : Button(field, "<", 9, 5) {
        override fun selectEvent() {
            val content = field.getMonitor().content;
            field.getMonitor().content = content.substring(0, content.length - 1)
        }
    }

    val cursor = Cursor(field, button5)

    field.addUiElements(
        button0, button1, button2, button3,
        button4, button5, button6, button7,
        button8, button9, buttonOpenBracket,
        buttonCloseBracket, buttonPlus, buttonMinus, buttonMul,
        buttonDiv, buttonPow, buttonEq, buttonClear, buttonErase,
        cursor
    )

    field.addEventListeners(
        button1, button2, button3, button4,
        button5, button6, button7, button8,
        button9, button0, buttonOpenBracket,
        buttonPlus, buttonMinus, buttonMul, buttonDiv,
        buttonCloseBracket, buttonPow, buttonEq, buttonClear,
        buttonErase,
    )

    GlobalScreen.registerNativeHook()
    GlobalScreen.addNativeKeyListener(cursor)

    while (true) {
        field.render()
        Thread.sleep(150)
    }
}

class Monitor {
    var content: String = ""

    fun render() {
        println(" _______________________________")
        System.out.printf("|%31s|%n", content)
        println("|_______________________________|")
    }

    fun evalContent() {
        val me: MathExpression = MathExpression(content)
        content = me.evaluate().formulaRepresentation
    }
}

class Cursor(field: UiField, private var currentUiElement: UiElement) :
    AbstractUiElement(field, currentUiElement.getX(), currentUiElement.getY(), ""), NativeKeyListener {
    private var currentContent = uiContent

    override fun render() {
        field.getUiElements().forEach {
            if (xPoint == it.value.getX() && yPoint == it.value.getY()
                && it.value.javaClass != this.javaClass
            ) {
                currentContent = "|${it.value.getContent()}|"
            }
        }
        field.getMatrix()[yPoint][xPoint] = currentContent
    }

    override fun nativeKeyPressed(nativeEvent: NativeKeyEvent) {
        when (nativeEvent.keyCode) {
            NativeKeyEvent.VC_RIGHT -> {
                evalNearestElement(xPoint + 1, yPoint,
                    { xPoint < it.getX() }, { yPoint >= it.getY() || yPoint <= it.getY() })
                field.getEventListeners().get(currentUiElement.getId())!!.hoverEvent()
            }

            NativeKeyEvent.VC_UP -> {
                evalNearestElement(xPoint, yPoint + 1,
                    { xPoint >= it.getX() || xPoint <= it.getX() }, { yPoint > it.getY() })
                field.getEventListeners().get(currentUiElement.getId())!!.hoverEvent()
            }

            NativeKeyEvent.VC_LEFT -> {
                evalNearestElement(xPoint - 1, yPoint,
                    { xPoint > it.getX() }, { yPoint >= it.getY() || yPoint <= it.getY() })
                field.getEventListeners().get(currentUiElement.getId())!!.hoverEvent()
            }

            NativeKeyEvent.VC_DOWN -> {
                evalNearestElement(xPoint, yPoint - 1,
                    { xPoint >= it.getX() || xPoint <= it.getX() }, { yPoint < it.getY() })
                field.getEventListeners().get(currentUiElement.getId())!!.hoverEvent()
            }

            NativeKeyEvent.VC_ENTER -> {
                val eventListener = field.getEventListeners().get(currentUiElement.getId())
                eventListener!!.selectEvent()
            }
        }
    }

    private fun evalNearestElement(
        curX: Int, curY: Int,
        xPredicate: Predicate<UiElement>,
        yPredicate: Predicate<UiElement>
    ) {
        var minXDist = Int.MAX_VALUE
        var minYDist = Int.MAX_VALUE
        field.getUiElements().forEach {
            if (xPredicate.test(it.value) && yPredicate.test(it.value)) {
                val curXDist = abs(curX - it.value.getX())
                val curYDist = abs(curY - it.value.getY())
                if (curXDist <= minXDist && curYDist <= minYDist) {
                    minXDist = curXDist
                    minYDist = curYDist
                    currentUiElement = it.value
                }
            }
        }
        xPoint = currentUiElement.getX()
        yPoint = currentUiElement.getY()
    }

}

interface IdImplementation {
    fun getId(): String
}

interface UiElement : IdImplementation {
    fun getY(): Int
    fun getX(): Int
    fun setY(y: Int)
    fun setX(x: Int)
    fun enable(enable: Boolean)
    fun isEnabled(): Boolean
    fun getContent(): String
    fun render()
}

interface EventListenable : UiElement {
    fun hoverEvent()
    fun selectEvent()
}

class UiField(private var height: Int, private var width: Int, private var monitor: Monitor) {
    private val uiElementHolder: UiElementHolder = UiElementHolder()
    private val eventListenersHolder: EventListenersHolder = EventListenersHolder()
    private var matrix: Array<Array<String?>> = Array(this.height - 1) { arrayOfNulls(width - 1) }

    init {
        initEmpty()
    }

    fun getMatrix() = matrix
    fun getHeight() = this.height - 1
    fun getWidth() = this.width - 1

    fun render() {
        clearScreen()
        println()
        monitor.render()
        initEmpty()
        uiElementHolder.getAllElements().forEach { it.value.render() }
        matrix.forEach {
            it.forEach { i -> print(i) }
            println()
        }
        println()
    }

    private fun clearScreen() {
        for (i in 0 until 15) {
            System.out.printf("\u001b[%dA", 1)
            print("\u001b[2K")
        }
    }

    private fun initEmpty() {
        matrix.forEach {
            for (i in it.indices) {
                it[i] = "   "

            }
        }
    }

    fun getUiElements() = uiElementHolder.getAllElements()
    fun getEventListeners() = eventListenersHolder.getAllElements()

    fun addUiElement(uiElement: UiElement) {
        uiElementHolder.registerElement(uiElement)
    }

    fun addUiElements(vararg uiElement: UiElement) {
        uiElement.forEach { addUiElement(it) }
    }

    fun addEventListeners(vararg eventListener: EventListenable) {
        eventListener.forEach { addEventListener(it) }
    }

    fun addEventListener(eventListener: EventListenable) {
        eventListenersHolder.registerElement(eventListener)
    }

    fun getMonitor() = monitor
}

abstract class AbstractUiElement(
    protected var field: UiField,
    protected var xPoint: Int,
    protected var yPoint: Int,
    protected var uiContent: String
) : UiElement {
    private val id: String = UUID.randomUUID().toString()
    private var enabled: Boolean = false

    override fun getContent(): String {
        return this.uiContent
    }

    override fun getY(): Int {
        return this.yPoint
    }

    override fun getX(): Int {
        return this.xPoint
    }

    override fun setY(y: Int) {
        this.yPoint = y
    }

    override fun setX(x: Int) {
        this.xPoint = x
    }

    override fun getId(): String {
        return id
    }

    override fun enable(enable: Boolean) {
        enabled = true
    }

    override fun isEnabled() = enabled

    override fun render() {
        field.getMatrix()[yPoint][xPoint] = " $uiContent "
    }

}

open class Button(field: UiField, content: String, x: Int, y: Int) : AbstractUiElement(field, x, y, content),
    EventListenable {

    override fun hoverEvent() {

    }

    override fun selectEvent() {
        var content = field.getMonitor().content
        content += uiContent
        field.getMonitor().content = content
    }
}

open class AbstractHolder<T : IdImplementation> {
    private val uiElements = mutableMapOf<String, T>()

    fun registerElement(element: T): String {
        uiElements[element.getId()] = element
        return element.getId()
    }

    fun getElementById(id: String): T {
        return uiElements[id]!!
    }

    fun getAllElements() = uiElements

}

class UiElementHolder : AbstractHolder<UiElement>()
class EventListenersHolder : AbstractHolder<EventListenable>()