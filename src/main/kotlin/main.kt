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
fun eval(initialSelf: LineBuffer, initialInput: LineBuffer): LineBuffer {
    // set buffers to their initial defaults
    var input: LineBuffer = initialInput
    var self: LineBuffer = initialSelf
    val selfStr = self.toString()
    var output: LineBuffer = LocalBuffer()
    /*println("Beginning eval.")
    println("Input: '${input}'")
    println("Self: '${self}'")
    println("Level: $indentLevel")
    println("---")*/

    fun evalLine(line: String): String {
        println("eval'ing line with literal: '$line'")
        val command = line.split(" ", limit = 2)
        val macro = command[0].trimLeadingIndents()

        if(command.size == 1) {
                val sb = StringBuilder()
                var next = self.peekLine() ?: error("Reading empty buffer")
                val currentIndent = line.indentLevel()
                while(next.indentLevel() == currentIndent + 1) {
                    println("Next: '$next'")
                    sb.append(" ")
                    sb.append(evalLine(self.readLine() ?: error("Empty buffer")))
                    next = self.peekLine() ?: break
                }

                return evalLine(line + sb.toString())
        } else if(command.size == 2) {
            val param = command[1]
            // local evaluation
            // see if macro exists in scope
            (if (scope.containsKey(macro)) {
                // eval that macro with parameter as input
                return eval(scope[macro]!!, LocalBuffer(param)).toString()
            } else {
                // see if macro exists as a built-in command
                return when (macro) {
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
            })
        } else {
            error("Invalid command")
        }
    }

    while(!self.eof) {
        evalLine(self.readLine() ?: error("Attempted to read from empty buffer."))
    }

    /*
    while (!self.eof) {
        val line = self.readLine() ?: error("Tried to read from an empty buffer.")
        println("line: '$line'")
        val command = line.split(" ", limit = 2)

        // Lookahead for indented parameter expressions
        if (command.size > 1) {
            // if this was a single line with a literal, the output needs to resolve to the result
            val res = evalLineWithLiteral(line)
            println("Evaluated literal '$line': '$res'")
        } else {
            // if this was a composed line, the sub-lines need to be evaluated, joined, and then the result line needs to resolve to the result
            val sb = StringBuilder(line)

            var tabbedLine = self.peekLine() ?: error("Unexpected eof")
            while (tabbedLine.indentLevel() == indentLevel + 1) {
                println("tabbed line: '$tabbedLine'")
                // continue to use same self and input buffers (?)
                val res = eval(self, input, indentLevel + 1)
                        .toString()
                        .replace("\n", " ")
                        .replace("\t", "")
                println("Intermediate result: '$res'")
                sb.append(" ")
                sb.append(res)

                // load next line
                tabbedLine = self.peekLine() ?: break
            }

            println("sb: '$sb'")
            val res = evalLineWithLiteral(sb.toString())
            println("Evaluated composite: '$res'")
        }
    }

     */

    println("Result: '$output'")

    return output;
}