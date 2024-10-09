package com.seo4d696b75.android.ekisagasu.domain.xml

interface XMLSerializer {
    operator fun invoke(
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
