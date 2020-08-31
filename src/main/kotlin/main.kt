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

// eval entire macro
fun eval(initialSelf: LineBuffer, initialInput: LineBuffer): LineBuffer {
    // set buffers to their initial defaults
    var input: LineBuffer = initialInput
    var self: LineBuffer = initialSelf
    var output: LineBuffer = LocalBuffer()

    while (!self.eof) {
        val line = self.readLine() ?: error("Tried to read from an empty buffer.")
        val command = line.split(" ", limit = 2)
        val macro = command[0].replace("\t", "")

        // Lookahead for indented parameter expressions
        val param = if (command.size > 1) {
            command[1]
        } else {
            val currentIndent = line.indentLevel()
            val sb = StringBuilder()

            var line = self.peekLine() ?: error("Unexpected eof")
            while (line.indentLevel() == currentIndent + 1) {
                // continue to use same self and input buffers
                sb.append(eval(self, input)
                        .toString()
                        .replace("\n", " ")
                        .replace("\t", "")
                )
                sb.append(" ")
                // consume line that was just read
                self.readLine()

                // load next line
                line = self.peekLine() ?: break
            }

            sb.toString()
        }

        // local evaluation
        // see if macro exists in scope
        val result = if (scope.containsKey(macro)) {
            // eval that macro with parameter as input
            eval(scope[macro]!!, LocalBuffer(param)).toString()
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
                ("<<") -> input.readLine()
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
                ("+") -> {
                    val (a, b) = param.split(" ", limit = 2)
                    (a.toDouble() + b.toDouble()).toString()
                }
                ("-") -> {
                    val (a, b) = param.split(" ", limit = 2)
                    (a.toDouble() - b.toDouble()).toString()
                }
                ("*") -> {
                    val (a, b) = param.split(" ", limit = 2)
                    (a.toDouble() * b.toDouble()).toString()
                }
                ("/") -> {
                    val (a, b) = param.split(" ", limit = 2)
                    (a.toDouble() / b.toDouble()).toString()
                }
                ("%") -> {
                    val (a, b) = param.split(" ", limit = 2)
                    (a.toDouble() % b.toDouble()).toString()
                }
                else -> {
                    error("Invalid macro: $macro")
                }
            }
        }

        output.appendLine(result ?: error(""))
    }

    return output
}