package com.seo4d696b75.android.ekisagasu.data.gpx

import android.util.Xml
import org.xmlpull.v1.XmlSerializer
import java.io.StringWriter

fun serializeXML(
    encoding: String,
    standalone: Boolean,
    rootTagName: String,
    content: TagScope.() -> Unit,
): String {
    val writer = StringWriter()
    val serializer = Xml.newSerializer()
    serializer.setOutput(writer)
    serializer.startDocument(encoding, standalone)
    serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)
    serializer.startTag("", rootTagName)
    TagScope(serializer).content()
    serializer.endTag("", rootTagName)
    serializer.endDocument()
    return writer.toString()
}

class TagScope internal constructor(
    private val serializer: XmlSerializer,
) {
    fun attribute(vararg list: Pair<String, String>) {
        list.forEach { pair ->
            serializer.attribute("", pair.first, pair.second)
        }
    }

    fun text(text: String) {
        serializer.text(text)
    }

    fun tag(
        name: String,
        content: TagScope.() -> Unit,
    ) {
        serializer.startTag("", name)
        TagScope(serializer).content()
        serializer.endTag("", name)
    }
}
