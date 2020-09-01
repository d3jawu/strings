import java.io.File
import java.lang.StringBuilder

val scope: MutableMap<CharSequence, Buffer> = HashMap()

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

fun CharSequence.indentLevel(): Int {
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
fun eval(initialSelf: Buffer, initialInput: Buffer): Buffer {
    val initialOutput = LocalBuffer()
    // set buffers to their initial defaults
    var input = initialInput
    var self = initialSelf
    var output: Buffer = initialOutput

    fun getBuffer(name: CharSequence): Buffer {
        return if (scope[name] != null) {
            scope[name]!!
        } else {
            when (name) {
                ("in") -> {
                    initialInput
                }
                ("out") -> {
                    initialOutput
                }
                ("self") -> {
                    initialSelf
                }
                ("stdin") -> {
                    // TODO
                    LocalBuffer()
                }
                ("stdout") -> {
                    // TODO
                    LocalBuffer()
                }
                ("null") -> {
                    NullBuffer()
                }
                else -> {
                    val newBuf = LocalBuffer()
                    scope[name] = newBuf
                    newBuf
                }
            }
        }
    }

    fun evalLine(line: CharSequence): CharSequence {
        if (line == "") {
            // ignore empty line
            return ""
        }

        val command = line.split(" ", limit = 2)
        val macro = command[0].trimLeadingIndents()

        when (command.size) {
            1 -> {
                val sb = StringBuilder()
                var next = self.peekTo("\n")
                val currentIndent = line.indentLevel()
                while (next.indentLevel() == currentIndent + 1) {
                    val res = evalLine(self.readTo("\n"))
                    if (res != "") {
                        sb.append(" ")
                    }
                    sb.append(res)
                    next = self.peekTo("\n")
                    if (next.isEmpty()) {
                        break
                    }
                }
                return evalLine(line.toString() + sb.toString())
            }
            2 -> {
                // replace escape characters
                val param = command[1]
                    .replace("\\n", "\n")
                    .replace("\\s", " ")
                    .replace("\\t", "\t")
                // local evaluation
                // see if macro exists in scope
                (if (scope.containsKey(macro)) {
                    // eval that macro with parameter as input
                    return eval(LocalBuffer(scope[macro]!!.toString()), LocalBuffer(param)).toString()
                } else {
                    // see if macro exists as a built-in command
                    return when (macro) {
                        (">>") -> {
                            output.append(param)
                            ""
                        }
                        ("<<") -> {
                            input.readTo(param).toString()
                        }
                        ("?<") -> {
                            input.peekTo(param).toString()
                        }
                        ("<<<") -> {
                            input.readAll().toString()
                        }
                        ("?<<") -> {
                            input.peekAll().toString()
                        }
                        ("->") -> {
                            output = getBuffer(param)
                            ""
                        }
                        ("<-") -> {
                            input = getBuffer(param)
                            ""
                        }
                        ("<>") -> {
                            self = getBuffer(param)
                            ""
                        }
                        "?" -> {
                            if (param.startsWith("true")) {
                                evalLine(param.removePrefix("true "))
                            } else {
                                ""
                            }
                        }
                        "=" -> {
                            val (a, b) = param.split(" ", limit = 2)

                            if (a == b) {
                                "true"
                            } else {
                                "false"
                            }
                        }
                        "<" -> {
                            val (a, b) = param.split(" ", limit = 2)
                            if (a.toDouble() < b.toDouble()) {
                                "true"
                            } else {
                                "false"
                            }
                        }
                        "+", "*" -> {
                            val (a, b) = param.split(" ", limit = 2)

                            val res = when (macro) {
                                "+" -> (a.toDouble() + b.toDouble())
                                "*" -> (a.toDouble() * b.toDouble())
                                else -> error("What?")
                            }

                            res.toString().trimTrailingDecimal()
                        }
                        else -> {
                            error("Invalid macro: '$macro'")
                        }
                    }
                })
            }
            else -> {
                error("Invalid command")
            }
        }
    }

    while (!self.eof) {
        evalLine(self.readTo("\n"))
    }

    return output;
}