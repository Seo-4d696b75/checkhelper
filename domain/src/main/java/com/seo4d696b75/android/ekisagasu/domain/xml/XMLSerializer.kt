package com.seo4d696b75.android.ekisagasu.domain.xml

import java.io.OutputStream

interface XMLSerializer {
    fun OutputStream.writeXML(
        encoding: String,
        standalone: Boolean,
        rootTagName: String,
        content: XMLTagScope.() -> Unit,
    )

    fun buildXMLString(
        encoding: String,
        standalone: Boolean,
        rootTagName: String,
        content: XMLTagScope.() -> Unit,
    ): String
}

interface XMLTagScope {
    fun attribute(vararg list: Pair<String, String>)
    fun text(text: String)
    fun tag(name: String, content: XMLTagScope.() -> Unit)
}
