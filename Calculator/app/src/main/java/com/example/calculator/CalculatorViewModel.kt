package com.example.calculator

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.sqrt

class CalculatorViewModel : ViewModel() {
    private val _equationText = MutableLiveData("")
    val equationText: LiveData<String> = _equationText

    private val _resultText = MutableLiveData("0")
    val resultText: LiveData<String> = _resultText

    private var isRadians = true // Default to radians (scientific standard)
    private var lastResult = "0"
    private var lastCalculation = "0"
    private var isErrorState = false

    fun onButtonClick(btn: String) {
        Log.i("Clicked Button", btn)
        if (isErrorState && btn != "AC") {
            if (btn != "DEL") {
                clearAll()
            }
        }
        when (btn) {
            "AC" -> {
                clearAll(); return
            }
            "DEL" -> {
                deleteLast(); return
            }
            "=" -> {
                calculateFinalResult(); return
            }
            "RAD", "DEG" -> {
                toggleAngleMode(); calculateResult(); return
            }
            "ANS" -> {
                appendToEquation(lastResult); calculateResult(); return
            }
        }
        handleFunctionButtons(btn)
    }

    private fun clearAll() {
        _equationText.value = ""
        _resultText.value = "0"
        isErrorState = false
        lastCalculation = "0"
    }

    private fun deleteLast() {
        _equationText.value?.let { current ->
            if (current.isNotEmpty()) {
                _equationText.value = current.substring(0, current.length - 1)
                calculateResult()
            } else {
                _resultText.value = "0"
            }
        }
    }

    private fun toggleAngleMode() {
        isRadians = !isRadians
    }

    private fun handleFunctionButtons(btn: String) {
        val processedInput = when (btn) {
            "sin" -> "sin("
            "cos" -> "cos("
            "tan" -> "tan("
            "sin⁻¹" -> "asin("
            "cos⁻¹" -> "acos("
            "tan⁻¹" -> "atan("
            "log" -> "log("
            "ln" -> "ln("
            "√" -> "sqrt("
            "x!" -> { handleFactorial(); return }
            "1/x" -> { handleReciprocal(); return }
            "x^y" -> "^"
            "π" -> Math.PI.toString()
            "e" -> Math.E.toString()
            "(" -> "("
            ")" -> ")"
            else -> btn
        }
        appendToEquation(processedInput)
        calculateResult()
    }

    private fun handleFactorial() {
        try {
            val currentExpr = _equationText.value ?: ""
            if (currentExpr.isNotEmpty()) {
                val value = evaluateExpression(currentExpr)
                if (value >= 0 && value <= 170 && value == floor(value)) {
                    val fact = factorial(value.toLong())
                    val formatted = formatResult(fact.toDouble())
                    _equationText.value = formatted
                    _resultText.value = formatted
                    lastResult = formatted
                    lastCalculation = formatted
                } else {
                    setError("Invalid input")
                }
            }
        } catch (e: Exception) {
            setError("Error")
        }
    }

    private fun handleReciprocal() {
        try {
            val currentExpr = _equationText.value ?: ""
            if (currentExpr.isNotEmpty()) {
                val value = evaluateExpression(currentExpr)
                if (value != 0.0) {
                    val reciprocal = 1.0 / value
                    val formatted = formatResult(reciprocal)
                    _equationText.value = formatted
                    _resultText.value = formatted
                    lastResult = formatted
                    lastCalculation = formatted
                } else {
                    setError("Cannot divide by zero")
                }
            }
        } catch (e: Exception) {
            setError("Error")
        }
    }

    private fun appendToEquation(text: String) {
        _equationText.value = _equationText.value + text
    }

    private fun calculateResult() {
        _equationText.value?.let { equation ->
            if (equation.isEmpty()) {
                _resultText.value = "0"; return
            }
            try {
                val lastChar = equation.lastOrNull()
                if (lastChar in listOf('+', '-', '×', '÷', '^', '(', '.', 'E', 'π')) {
                    _resultText.value = ""; return
                }
                val result = evaluateExpression(equation)
                val formatted = formatResult(result)
                _resultText.value = formatted
                lastResult = formatted
                isErrorState = false
            } catch (e: Exception) {
                _resultText.value = ""
            }
        }
    }

    private fun calculateFinalResult() {
        _equationText.value?.let { equation ->
            if (equation.isEmpty()) return
            try {
                val result = evaluateExpression(equation)
                val formatted = formatResult(result)
                _resultText.value = formatted
                _equationText.value = formatted
                lastResult = formatted
                lastCalculation = formatted
                isErrorState = false
            } catch (e: Exception) {
                setError("Error")
            }
        }
    }

    private fun evaluateExpression(expression: String): Double {
        if (expression.trim().isEmpty()) return 0.0
        val context = Context.enter()
        context.optimizationLevel = -1
        try {
            val scriptable = context.initStandardObjects()
            addCustomFunctions(scriptable, context)
            val processedExpr = preprocessExpression(expression)
            Log.d("Calculator", "Evaluating: $processedExpr")
            val result = context.evaluateString(scriptable, processedExpr, "JavaScript", 1, null)
            return Context.toNumber(result)
        } catch (e: Exception) {
            Log.e("Calculator", "Evaluation error: ${e.message}")
            throw e
        } finally {
            Context.exit()
        }
    }

    private fun preprocessExpression(expr: String): String {
        var processed = expr
            .replace("÷", "/")
            .replace("×", "*")
            .replace("—", "-")
            .replace("π", Math.PI.toString())
            .replace("e", Math.E.toString())
        processed = processed.replace("^", "**")
        processed = addImplicitMultiplication(processed)
        processed = handleAngleConversions(processed)
        return processed
    }

    private fun addImplicitMultiplication(expr: String): String {
        var result = expr
        result = result.replace(Regex("(\\d)([a-zA-Z(])"), "$1*$2")
        result = result.replace(Regex("(\\))(\\d)"), "$1*$2")
        result = result.replace(Regex("(\\))(\\()"), "$1*$2")
        return result
    }

    private fun handleAngleConversions(expr: String): String {
        var result = expr
        val trigFunctions = listOf("sin", "cos", "tan")
        val inverseTrigFunctions = listOf("asin", "acos", "atan")
        if (!isRadians) {
            trigFunctions.forEach { func ->
                val pattern = "$func\\(".toRegex()
                result = result.replace(pattern) { "$func(degToRad(" }
            }
            inverseTrigFunctions.forEach { func ->
                val pattern = "$func\\(".toRegex()
                result = result.replace(pattern) { "radToDeg($func(" }
            }
        }
        return result
    }

    private fun addCustomFunctions(scriptable: Scriptable, context: Context) {
        scriptable.put("degToRad", scriptable, object : org.mozilla.javascript.BaseFunction() {
            override fun call(cx: Context?, scope: Scriptable?, thisObj: Scriptable?, args: Array<out Any>?): Any {
                return if (args != null && args.isNotEmpty()) {
                    Math.toRadians(Context.toNumber(args[0]))
                } else Double.NaN
            }
        })
        scriptable.put("radToDeg", scriptable, object : org.mozilla.javascript.BaseFunction() {
            override fun call(cx: Context?, scope: Scriptable?, thisObj: Scriptable?, args: Array<out Any>?): Any {
                return if (args != null && args.isNotEmpty()) {
                    Math.toDegrees(Context.toNumber(args[0]))
                } else Double.NaN
            }
        })
        scriptable.put("log", scriptable, object : org.mozilla.javascript.BaseFunction() {
            override fun call(cx: Context?, scope: Scriptable?, thisObj: Scriptable?, args: Array<out Any>?): Any {
                return if (args != null && args.isNotEmpty()) {
                    val x = Context.toNumber(args[0])
                    if (x > 0) log10(x) else Double.NaN
                } else Double.NaN
            }
        })
        scriptable.put("ln", scriptable, object : org.mozilla.javascript.BaseFunction() {
            override fun call(cx: Context?, scope: Scriptable?, thisObj: Scriptable?, args: Array<out Any>?): Any {
                return if (args != null && args.isNotEmpty()) {
                    val x = Context.toNumber(args[0])
                    if (x > 0) ln(x) else Double.NaN
                } else Double.NaN
            }
        })
        scriptable.put("sqrt", scriptable, object : org.mozilla.javascript.BaseFunction() {
            override fun call(cx: Context?, scope: Scriptable?, thisObj: Scriptable?, args: Array<out Any>?): Any {
                return if (args != null && args.isNotEmpty()) {
                    val x = Context.toNumber(args[0])
                    if (x >= 0) sqrt(x) else Double.NaN
                } else Double.NaN
            }
        })
        scriptable.put("sin", scriptable, object : org.mozilla.javascript.BaseFunction() {
            override fun call(cx: Context?, scope: Scriptable?, thisObj: Scriptable?, args: Array<out Any>?): Any {
                return if (args != null && args.isNotEmpty()) {
                    Math.sin(Context.toNumber(args[0]))
                } else Double.NaN
            }
        })
        scriptable.put("cos", scriptable, object : org.mozilla.javascript.BaseFunction() {
            override fun call(cx: Context?, scope: Scriptable?, thisObj: Scriptable?, args: Array<out Any>?): Any {
                return if (args != null && args.isNotEmpty()) {
                    Math.cos(Context.toNumber(args[0]))
                } else Double.NaN
            }
        })
        scriptable.put("tan", scriptable, object : org.mozilla.javascript.BaseFunction() {
            override fun call(cx: Context?, scope: Scriptable?, thisObj: Scriptable?, args: Array<out Any>?): Any {
                return if (args != null && args.isNotEmpty()) {
                    Math.tan(Context.toNumber(args[0]))
                } else Double.NaN
            }
        })
        scriptable.put("asin", scriptable, object : org.mozilla.javascript.BaseFunction() {
            override fun call(cx: Context?, scope: Scriptable?, thisObj: Scriptable?, args: Array<out Any>?): Any {
                return if (args != null && args.isNotEmpty()) {
                    val x = Context.toNumber(args[0])
                    if (x >= -1 && x <= 1) Math.asin(x) else Double.NaN
                } else Double.NaN
            }
        })
        scriptable.put("acos", scriptable, object : org.mozilla.javascript.BaseFunction() {
            override fun call(cx: Context?, scope: Scriptable?, thisObj: Scriptable?, args: Array<out Any>?): Any {
                return if (args != null && args.isNotEmpty()) {
                    val x = Context.toNumber(args[0])
                    if (x >= -1 && x <= 1) Math.acos(x) else Double.NaN
                } else Double.NaN
            }
        })
        scriptable.put("atan", scriptable, object : org.mozilla.javascript.BaseFunction() {
            override fun call(cx: Context?, scope: Scriptable?, thisObj: Scriptable?, args: Array<out Any>?): Any {
                return if (args != null && args.isNotEmpty()) {
                    Math.atan(Context.toNumber(args[0]))
                } else Double.NaN
            }
        })
    }

    private fun factorial(n: Long): Long {
        return if (n <= 1) 1 else n * factorial(n - 1)
    }

    private fun formatResult(result: Double): String {
        return when {
            result.isNaN() -> "Error"
            result.isInfinite() -> if (result > 0) "∞" else "-∞"
            abs(result) < 1e-12 -> "0"
            result == floor(result) && abs(result) < 1e15 -> {
                val longValue = result.toLong()
                if (abs(longValue.toDouble() - result) < 1e-10) {
                    longValue.toString()
                } else {
                    "%.10g".format(result).trimEnd('0').trimEnd('.')
                }
            }
            else -> {
                val formatted = "%.10g".format(result)
                formatted.replace(",", ".").replace(Regex("\\.?0+$"), "")
            }
        }
    }

    private fun setError(message: String) {
        _resultText.value = message
        isErrorState = true
    }

    fun getAngleMode(): String = if (isRadians) "RAD" else "DEG"
}
