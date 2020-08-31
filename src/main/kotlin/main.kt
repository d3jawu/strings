import java.io.File
import java.lang.StringBuilder

val scope: MutableMap<String, LineBuffer> = HashMap()

fun main(args: Array<String>) {
    val dir = (if (args.size == 1) {
        args[0]
    } else {
        "."
    })

    // load all local .st files into scope
    File(dir).walk().filter { it.toString().endsWith(".st") }.forEach { it ->
        scope[it.name.replace(".st", "")] = it.useLines { LocalBuffer(it.toList()) }
    }

    val result = eval(scope["main"] ?: error("Entry point file main.st not found."), LocalBuffer())
    println(result.toString())
}

fun String.indentLevel(): Int {
    var t = 0
    while (this[t] == '\t') {
        t += 1
    }

    return t
}

// remove ".0" from whole number doubles
fun String.trimTrailingDecimal(): String =
        if (this.endsWith(".0")) {
            this.replace(".0", "")
        } else {
            this
        }

// removes ALL leading indents to string
fun String.trimLeadingIndents(): String = if (this.startsWith("\t")) {
    this.removePrefix("\t").trimLeadingIndents()
} else {
    this
}

// eval entire macro
fun eval(initialSelf: LineBuffer, initialInput: LineBuffer, indentLevel: Int = 0): LineBuffer {
    // set buffers to their initial defaults
    var input: LineBuffer = initialInput
    var self: LineBuffer = initialSelf
    var output: LineBuffer = LocalBuffer()
    println("Beginning eval.")
    println("Input: '${input}'")
    println("Self: '${self}'")
    println("Level: $indentLevel")
    println("---")

    // eval a single line in-place that has a literal param
    fun evalLineWithLiteral(line: String): String {
        println("eval'ing line with literal: '$line'")
        val command = line.split(" ", limit = 2)
        val macro = command[0].trimLeadingIndents()
        val param = command[1]
        // local evaluation
        // see if macro exists in scope
        return if (scope.containsKey(macro)) {
            // eval that macro with parameter as input
            eval(scope[macro]!!, LocalBuffer(param), 0).toString()
        } else {
            // see if macro exists as a built-in command
            when (macro) {
                (">>") -> {
                    output.prependLine(param)
                    ""
                }
                (">>>") -> {
                    output.appendLine(param)
                    ""
                }
                ("<<") -> {
                    val res = input.readLine() ?: error("Tried to read empty input buffer.")
                    println("Input read line: '$res'")
                    res
                }
                ("->") -> {
                    if (scope[param] != null) {
                        output = scope[param]!!
                    } else {
                        input = when (param) {
                            ("stdin") -> {
                                // TODO
                                LocalBuffer()
                            }
                            ("stdout") -> {
                                // TODO
                                LocalBuffer()
                            }
                            else -> {
                                val newBuf = LocalBuffer()
                                scope[param] = newBuf
                                newBuf
                            }
                        }
                    }
                    ""
                }
                "+", "-", "*", "/", "%" -> {
                    val (a, b) = param.split(" ", limit = 2)

                    val res = when (macro) {
                        "+" -> (a.toDouble() + b.toDouble())
                        "-" -> (a.toDouble() - b.toDouble())
                        "*" -> (a.toDouble() * b.toDouble())
                        "/" -> (a.toDouble() / b.toDouble())
                        "%" -> (a.toDouble() % b.toDouble())
                        else -> error("What?")
                    }

                    res.toString().trimTrailingDecimal()
                }
                else -> {
                    error("Invalid macro: '$macro'")
                }
            }
        }
    }

    while (!self.eof) {
        val line = self.readLine() ?: error("Tried to read from an empty buffer.")
        println("line: '$line'")
        val command = line.split(" ", limit = 2)

        // Lookahead for indented parameter expressions
        val res = if (command.size > 1) {
            evalLineWithLiteral(line)
        } else {
            println("Using tabbed composition")
            val sb = StringBuilder(line)

            var line = self.peekLine() ?: error("Unexpected eof")
            while (line.indentLevel() == indentLevel + 1) {
                println("tabbed line: '$line'")
                // continue to use same self and input buffers (?)
                val res = eval(self, input)
                        .toString()
                        .replace("\n", " ")
                        .replace("\t", "")
                sb.append(" ")
                sb.append(res)

                // load next line
                line = self.peekLine() ?: break
            }
        }
        println("Result after evalLineWithLiteral: '$res'")
        println("Output after evalLineWithLiteral: '$output'")
    }

    println("eval output: '$output'")
    return output
}