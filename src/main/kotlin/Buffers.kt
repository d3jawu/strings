// provides standard interface for both file streams and in-memory buffers

interface Buffer {
    // consume until delimiter is met; delimiter is consumed and discarded
    fun readTo(delimiter: CharSequence): CharSequence

    fun readAll(): CharSequence

    // read next line without consuming it
    fun peekTo(delimiter: CharSequence): CharSequence

    fun peekAll(): CharSequence = this.toString()

    // places string at the beginning of the buffer (right in front of the PC)
    fun prepend(str: CharSequence)

    // places string at end of buffer
    fun append(str: CharSequence)
    val eof: Boolean
}

// for macros read in from a file or generated during execution
class LocalBuffer : Buffer {
    private var content: CharSequence

    constructor(lines: List<CharSequence>) {
        this.content = lines.joinToString(separator = "\n")
    }

    // create from raw string input
    constructor(raw: String = "") {
        content = raw
    }

    override fun readTo(delimiter: CharSequence): CharSequence {
        val split = this.content.split(delimiter.toString(), limit = 2)
        return when (split.size) {
            1 -> {
                // delimiter not found, consume whole string
                this.content = ""
                split[0]
            }
            2 -> {
                this.content = split[1]
                split[0]
            }
            else -> {
                error("What?")
            }
        }
    }

    override fun readAll(): CharSequence {
        val orig = content
        content = ""
        return orig
    }

    override fun peekTo(delimiter: CharSequence): CharSequence {
        val split = this.content.split(delimiter.toString(), limit = 2)
        // in either case, the first element contains what we want
        return split[0]
    }

    override fun prepend(str: CharSequence) {
        this.content = "$str${this.content}"
    }

    override fun append(str: CharSequence) {
        this.content = "${this.content}$str"
    }

    override fun toString(): String = content.toString()

    override val eof: Boolean
        get() = (content.isEmpty())
}

// is always empty and discards all input, like /dev/null
class NullBuffer : Buffer {
    override fun readTo(delimiter: CharSequence): CharSequence = ""

    override fun readAll(): CharSequence = ""

    override fun peekTo(delimiter: CharSequence): CharSequence = ""

    override fun prepend(str: CharSequence) = Unit

    override fun append(str: CharSequence) = Unit

    override fun toString(): String = ""

    override val eof: Boolean
        get() = true

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

