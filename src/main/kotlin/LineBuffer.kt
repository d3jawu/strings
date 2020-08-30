// provides standard interface for both file streams and in-memory buffers

interface LineBuffer {
    // consume the next line from the buffer
    fun readLine(): String?

    // read next line without consuming it
    fun peekLine(): String?

    // places given line in the next slot in the buffer
    fun prependLine(line: String)

    // places line at end of buffer
    fun appendLine(line: String)
    val eof: Boolean
}

// for macros read in from a file or generated during execution
class LocalBuffer : LineBuffer {
    // TODO back with deque instead of list
    val lines: ArrayDeque<String>

    constructor(lines: List<String>) {
        this.lines = ArrayDeque(lines)
    }

    // create from raw string input
    constructor(raw: String = "") {
        lines = if (raw == "") {
            ArrayDeque() // prevents buffer from being created with empty line
        } else {
            ArrayDeque(raw.split('\n'))
        }
    }

    override fun readLine(): String? = lines.removeFirstOrNull()
    override fun peekLine(): String? = lines.firstOrNull()

    override fun prependLine(line: String) = lines.addFirst(line)
    override fun appendLine(line: String) = lines.addLast(line)

    override fun toString(): String = lines.joinToString(separator = "\n")

    override val eof: Boolean
        get() = (lines.size == 0)
}

// for input from stdin
/*
class StdinBuffer : LineBuffer {

}
*/

// for output to stdout
/*
class StdoutBuffer : LineBuffer {

}
*/

