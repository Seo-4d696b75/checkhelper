package com.seo4d696b75.android.ekisagasu.data.xml

import android.util.Xml
import com.seo4d696b75.android.ekisagasu.domain.xml.XMLSerializer
import com.seo4d696b75.android.ekisagasu.domain.xml.XMLTagScope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.xmlpull.v1.XmlSerializer
import java.io.OutputStream
import java.io.StringWriter

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
object XMLSerializerModule {
    @Provides
    fun providerXMLSerializer() = object : XMLSerializer {

        private fun XmlSerializer.write(
            encoding: String,
            standalone: Boolean,
            rootTagName: String,
            content: XMLTagScope.() -> Unit
        ) {
            startDocument(encoding, standalone)
            setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)
            startTag("", rootTagName)
            XMLTagScopeImpl(this).content()
            endTag("", rootTagName)
            endDocument()
        }

        override fun OutputStream.writeXML(
            encoding: String,
            standalone: Boolean,
            rootTagName: String,
            content: XMLTagScope.() -> Unit,
        ) {
            bufferedWriter().use { writer ->
                Xml.newSerializer().apply {
                    setOutput(writer)
                    write(encoding, standalone, rootTagName, content)
                }
            }
        }

        override fun buildXMLString(
            encoding: String,
            standalone: Boolean,
            rootTagName: String,
            content: XMLTagScope.() -> Unit,
        ) = StringWriter().let { writer ->
            Xml.newSerializer().apply {
                setOutput(writer)
                write(encoding, standalone, rootTagName, content)
            }
        }.toString()
    }
}

private class XMLTagScopeImpl(
    private val serializer: XmlSerializer,
) : XMLTagScope {
    override fun attribute(vararg list: Pair<String, String>) {
        list.forEach { pair ->
            serializer.attribute("", pair.first, pair.second)
        }
    }

    override fun text(text: String) {
        serializer.text(text)
    }

    override fun tag(
        name: String,
        content: XMLTagScope.() -> Unit,
    ) {
        serializer.startTag("", name)
        XMLTagScopeImpl(serializer).content()
        serializer.endTag("", name)
    }
}
