package com.exchangerates.app.core.math

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Вычислитель арифметических выражений для поля суммы («аннулятор», как в Xe).
 *
 * Поддерживает `+ - * /`, скобки, унарный минус, проценты и оба десятичных
 * разделителя (точку и запятую). Символы `× ÷ −` нормализуются автоматически.
 *
 * Проценты считаются по «калькуляторной» логике, привычной пользователю:
 * `100+10%` = 110, `100-10%` = 90, `100*10%` = 10, `100/10%` = 1000, `10%` = 0.1.
 */
object ExpressionEvaluator {

    private val MC = MathContext(24, RoundingMode.HALF_EVEN)
    private const val MAX_LENGTH = 64

    sealed interface Result {
        data class Success(val value: BigDecimal) : Result
        /** Пустой ввод — трактуется как ноль, но отличается от вычисленного нуля. */
        data object Empty : Result
        data class Error(val kind: ErrorKind) : Result
    }

    enum class ErrorKind { SYNTAX, DIVISION_BY_ZERO, TOO_LONG, OVERFLOW }

    fun evaluate(input: String): Result {
        if (input.length > MAX_LENGTH) return Result.Error(ErrorKind.TOO_LONG)
        val normalized = normalize(input)
        if (normalized.isBlank()) return Result.Empty
        return try {
            val tokens = tokenize(normalized) ?: return Result.Error(ErrorKind.SYNTAX)
            if (tokens.isEmpty()) return Result.Empty
            val parser = Parser(tokens)
            val node = parser.parseExpression() ?: return Result.Error(ErrorKind.SYNTAX)
            if (!parser.atEnd()) return Result.Error(ErrorKind.SYNTAX)
            val value = eval(node)
            Result.Success(value.stripTrailingZeros())
        } catch (e: DivisionByZero) {
            Result.Error(ErrorKind.DIVISION_BY_ZERO)
        } catch (e: ArithmeticException) {
            Result.Error(ErrorKind.OVERFLOW)
        } catch (e: NumberFormatException) {
            Result.Error(ErrorKind.SYNTAX)
        }
    }

    /**
     * Мягкое вычисление для ввода «на лету»: висящий оператор в конце
     * (`20.62+`) отбрасывается, чтобы список валют пересчитывался, пока
     * пользователь ещё печатает.
     */
    fun evaluatePartial(input: String): Result {
        val direct = evaluate(input)
        if (direct !is Result.Error || direct.kind != ErrorKind.SYNTAX) return direct
        var trimmed = normalize(input).trimEnd()
        while (trimmed.isNotEmpty() && trimmed.last() in "+-*/(.,") {
            trimmed = trimmed.dropLast(1).trimEnd()
        }
        // добираем незакрытые скобки
        val open = trimmed.count { it == '(' } - trimmed.count { it == ')' }
        if (open > 0) trimmed += ")".repeat(open)
        if (trimmed.isBlank()) return Result.Empty
        return evaluate(trimmed)
    }

    /** true, если строка выглядит как выражение, а не как одно число. */
    fun isExpression(input: String): Boolean {
        val normalized = normalize(input)
        val body = if (normalized.startsWith("-")) normalized.drop(1) else normalized
        return body.any { it in "+-*/()%" }
    }

    fun normalize(input: String): String = buildString(input.length) {
        for (ch in input) {
            when (ch) {
                '×', 'x', 'X', '∗' -> append('*')
                '÷', ':' -> append('/')
                '−', '–', '—' -> append('-')
                ' ', ' ', ' ', '\'' -> Unit // пробельные разделители разрядов
                else -> append(ch)
            }
        }
    }

    // ---------- лексер ----------

    private sealed interface Token {
        data class Num(val value: BigDecimal) : Token
        data class Op(val symbol: Char) : Token
        data object LParen : Token
        data object RParen : Token
        data object Percent : Token
    }

    private fun tokenize(s: String): List<Token>? {
        val tokens = mutableListOf<Token>()
        var i = 0
        while (i < s.length) {
            val ch = s[i]
            when {
                ch.isDigit() || ch == '.' || ch == ',' -> {
                    val start = i
                    while (i < s.length && (s[i].isDigit() || s[i] == '.' || s[i] == ',')) i++
                    val number = parseNumber(s.substring(start, i)) ?: return null
                    tokens += Token.Num(number)
                }
                ch in "+-*/" -> { tokens += Token.Op(ch); i++ }
                ch == '(' -> { tokens += Token.LParen; i++ }
                ch == ')' -> { tokens += Token.RParen; i++ }
                ch == '%' -> { tokens += Token.Percent; i++ }
                else -> return null
            }
        }
        return tokens
    }

    /**
     * Разбор одного числа с учётом разделителей.
     *
     * Правила подобраны под системную клавиатуру, где есть и точка, и запятая:
     * один разделитель — десятичный (`1,5` = 1.5); несколько одинаковых —
     * разряды (`1,234,567` = 1234567); разные вместе — последний десятичный
     * (`8,433.00` = 8433.00).
     */
    internal fun parseNumber(raw: String): BigDecimal? {
        if (raw.isEmpty()) return null
        val dots = raw.count { it == '.' }
        val commas = raw.count { it == ',' }
        val cleaned = when {
            dots == 0 && commas == 0 -> raw
            dots > 0 && commas > 0 -> {
                val decimalSeparator = if (raw.lastIndexOf('.') > raw.lastIndexOf(',')) '.' else ','
                val grouping = if (decimalSeparator == '.') ',' else '.'
                if (raw.count { it == decimalSeparator } > 1) return null
                raw.filter { it != grouping }.replace(decimalSeparator, '.')
            }
            dots == 1 -> raw
            commas == 1 -> raw.replace(',', '.')
            else -> raw.filter { it.isDigit() } // «1.234.567» / «1,234,567» — разряды
        }
        if (cleaned.isEmpty() || cleaned == ".") return null
        val normalized = if (cleaned.startsWith(".")) "0$cleaned" else cleaned
        if (normalized.endsWith(".")) return null
        return runCatching { BigDecimal(normalized) }.getOrNull()
    }

    // ---------- AST ----------

    private sealed interface Node
    private data class NumNode(val value: BigDecimal) : Node
    private data class PercentNode(val inner: Node) : Node
    private data class NegNode(val inner: Node) : Node
    private data class BinNode(val op: Char, val left: Node, val right: Node) : Node

    private class DivisionByZero : RuntimeException()

    private class Parser(private val tokens: List<Token>) {
        private var pos = 0

        fun atEnd(): Boolean = pos >= tokens.size

        private fun peek(): Token? = tokens.getOrNull(pos)

        fun parseExpression(): Node? {
            var left = parseTerm() ?: return null
            while (true) {
                val t = peek()
                if (t is Token.Op && (t.symbol == '+' || t.symbol == '-')) {
                    pos++
                    val right = parseTerm() ?: return null
                    left = BinNode(t.symbol, left, right)
                } else {
                    return left
                }
            }
        }

        private fun parseTerm(): Node? {
            var left = parseUnary() ?: return null
            while (true) {
                val t = peek()
                if (t is Token.Op && (t.symbol == '*' || t.symbol == '/')) {
                    pos++
                    val right = parseUnary() ?: return null
                    left = BinNode(t.symbol, left, right)
                } else {
                    return left
                }
            }
        }

        private fun parseUnary(): Node? {
            val t = peek()
            if (t is Token.Op) {
                return when (t.symbol) {
                    '-' -> { pos++; parseUnary()?.let(::NegNode) }
                    '+' -> { pos++; parseUnary() }
                    else -> null
                }
            }
            return parsePostfix()
        }

        private fun parsePostfix(): Node? {
            var node = parsePrimary() ?: return null
            while (peek() is Token.Percent) {
                pos++
                node = PercentNode(node)
            }
            return node
        }

        private fun parsePrimary(): Node? {
            return when (val t = peek()) {
                is Token.Num -> { pos++; NumNode(t.value) }
                is Token.LParen -> {
                    pos++
                    val inner = parseExpression() ?: return null
                    if (peek() !is Token.RParen) return null
                    pos++
                    inner
                }
                else -> null
            }
        }
    }

    private val HUNDRED = BigDecimal(100)

    private fun eval(node: Node): BigDecimal = when (node) {
        is NumNode -> node.value
        is NegNode -> eval(node.inner).negate()
        is PercentNode -> eval(node.inner).divide(HUNDRED, MC)
        is BinNode -> {
            val left = eval(node.left)
            val rightPercent = node.right as? PercentNode
            if (rightPercent != null) {
                val fraction = eval(rightPercent.inner).divide(HUNDRED, MC)
                when (node.op) {
                    '+' -> left.add(left.multiply(fraction, MC), MC)
                    '-' -> left.subtract(left.multiply(fraction, MC), MC)
                    '*' -> left.multiply(fraction, MC)
                    '/' -> divide(left, fraction)
                    else -> throw ArithmeticException("op ${node.op}")
                }
            } else {
                val right = eval(node.right)
                when (node.op) {
                    '+' -> left.add(right, MC)
                    '-' -> left.subtract(right, MC)
                    '*' -> left.multiply(right, MC)
                    '/' -> divide(left, right)
                    else -> throw ArithmeticException("op ${node.op}")
                }
            }
        }
    }

    private fun divide(a: BigDecimal, b: BigDecimal): BigDecimal {
        if (b.signum() == 0) throw DivisionByZero()
        return a.divide(b, MC)
    }
}
