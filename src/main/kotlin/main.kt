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

    eval(scope["main"] ?: error("Entry point file main.st not found."), LocalBuffer())
}

// eval entire macro
fun eval(initialSelf: LineBuffer, initialInput: LineBuffer): LineBuffer {
    // set buffers to their initial defaults
    var input: LineBuffer = initialInput
    var self: LineBuffer = initialSelf

    // evaluate a single line (effectively an expression)
    fun evalLine(line: String): LineBuffer {
        println(line)
        val (macro, param) = line.split(" ", limit = 2)

        // see if macro exists in scope
        return if (scope.containsKey(macro)) {
            // eval that macro with parameter as input
            eval(scope[macro] ?: error("Invalid macro: $macro"), LocalBuffer(param))
        } else {
            // see if macro exists as a built-in command
            when (macro) {
                else -> {
                    error("Invalid macro: $macro")
                }
            }
        }
    }

    while (!self.eof) {
        evalLine(self.readLine())
    }

    return LocalBuffer();
}