import java.io.File

val scope: MutableMap<String, LineBuffer> = HashMap();

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
    println("result:")
    println(result.toString())
}

// eval entire macro
fun eval(initialSelf: LineBuffer, initialInput: LineBuffer): LineBuffer {
    // set buffers to their initial defaults
    var input: LineBuffer = initialInput
    var self: LineBuffer = initialSelf
    var output: LineBuffer = LocalBuffer()

    while (!self.eof) {
        // TODO Lookahead for indented parameter expressions
        val line = self.readLine() ?: error("Tried to read from an empty buffer.")
        val (macro, param) = line.split(" ", limit = 2)

        // see if macro exists in scope
        val result = if (scope.containsKey(macro)) {
            // eval that macro with parameter as input
            eval(scope[macro] ?: error("Invalid macro: $macro"), LocalBuffer(param)).toString()
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
    }

    return output
}