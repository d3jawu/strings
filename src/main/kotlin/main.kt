import java.io.File

val scope: MutableMap<String, LineBuffer> = HashMap();

fun main(args: Array<String>) {
    val dir = (if (args.size == 1) {
        println(args[0])
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

// eval entire macro
fun eval(initialSelf: LineBuffer, initialInput: LineBuffer): LineBuffer {
    // set buffers to their initial defaults
    var input: LineBuffer = initialInput
    var self: LineBuffer = initialSelf
    var output: LineBuffer = LocalBuffer()

    // evaluate a single line (effectively an expression)
    fun evalLine(line: String): LineBuffer {
        val (macro, param) = line.split(" ", limit = 2)

        // see if macro exists in scope
        return if (scope.containsKey(macro)) {
            // eval that macro with parameter as input
            eval(scope[macro] ?: error("Invalid macro: $macro"), LocalBuffer(param))
        } else {
            // see if macro exists as a built-in command
            when (macro) {
                ("+") -> {
                    val (a, b) = param.split(" ", limit=2)
                    LocalBuffer((a.toDouble() + b.toDouble()).toString())
                }
                ("-") -> {
                    val (a, b) = param.split(" ", limit=2)
                    LocalBuffer((a.toDouble() - b.toDouble()).toString())
                }
                ("*") -> {
                    val (a, b) = param.split(" ", limit=2)
                    LocalBuffer((a.toDouble() * b.toDouble()).toString())
                }
                ("/") -> {
                    val (a, b) = param.split(" ", limit=2)
                    LocalBuffer((a.toDouble() / b.toDouble()).toString())
                }
                ("%") -> {
                    val (a, b) = param.split(" ", limit=2)
                    LocalBuffer((a.toDouble() % b.toDouble()).toString())
                }
                else -> {
                    error("Invalid macro: $macro")
                }
            }
        }
    }

    while (!self.eof) {
        output.appendLine(evalLine(self.readLine()).toString())
    }

    return LocalBuffer();
}