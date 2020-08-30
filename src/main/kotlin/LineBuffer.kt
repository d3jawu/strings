// provides standard interface for both file streams and in-memory buffers

interface LineBuffer {
    // consume the next line from the buffer
    fun readLine(): String
    // places given line in the next slot in the buffer
    fun prependLine(line: String)
    // places line at end of buffer
    fun appendLine(line: String)
    val eof: Boolean
}

// for macros read in from a file or generated during execution
class LocalBuffer: LineBuffer {
    val lines: List<String>
    var pos: Int = 0
    constructor(lines: List<String>) {
        this.lines = lines
    }

    // create from raw string input
    constructor(raw: String = "") {
        lines = raw.split('\n')
    }

    override fun readLine(): String {
        val line = lines[pos]
        pos += 1
        return line
    }

    override fun prependLine(line: String) {
        TODO("Not yet implemented")
    }

    override fun appendLine(line: String) {
        TODO("Not yet implemented")
    }

    override val eof: Boolean
        get() = (pos >= lines.size)
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

