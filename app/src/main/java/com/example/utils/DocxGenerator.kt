package com.example.utils

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object DocxGenerator {

    /**
     * Generates a valid Microsoft Word (.docx) file from text with paragraphs, headings, and bullet points.
     */
    fun createDocxFile(outputFile: File, title: String, textContent: String) {
        val zipOut = ZipOutputStream(FileOutputStream(outputFile))

        // 1. [Content_Types].xml
        val contentTypesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
    <Default Extension="xml" ContentType="application/xml"/>
    <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>""".trimIndent()
        addZipEntry(zipOut, "[Content_Types].xml", contentTypesXml)

        // 2. _rels/.rels
        val relsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>""".trimIndent()
        addZipEntry(zipOut, "_rels/.rels", relsXml)

        // 3. word/document.xml
        val paragraphsXml = buildString {
            // Document Title
            append("<w:p><w:pPr><w:jc w:val=\"center\"/><w:spacing w:after=\"240\"/></w:pPr>")
            append("<w:r><w:rPr><w:b/><w:sz w:val=\"36\"/><w:color w:val=\"1E40AF\"/></w:rPr>")
            append("<w:t>${escapeXml(title)}</w:t></w:r></w:p>")

            // Subtitle Tagline
            append("<w:p><w:pPr><w:jc w:val=\"center\"/><w:spacing w:after=\"360\"/></w:pPr>")
            append("<w:r><w:rPr><w:i/><w:sz w:val=\"20\"/><w:color w:val=\"64748B\"/></w:rPr>")
            append("<w:t>Converted with Scanvoro • Smart Scanning, Powerful Editing</w:t></w:r></w:p>")

            // Content lines
            val lines = textContent.split("\n")
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) {
                    append("<w:p><w:pPr><w:spacing w:after=\"120\"/></w:pPr></w:p>")
                } else if (trimmed.startsWith("### ") || trimmed.startsWith("## ") || trimmed.startsWith("# ")) {
                    val headingText = trimmed.replace(Regex("^#+\\s*"), "")
                    append("<w:p><w:pPr><w:spacing w:before=\"200\" w:after=\"100\"/></w:pPr>")
                    append("<w:r><w:rPr><w:b/><w:sz w:val=\"28\"/><w:color w:val=\"0F172A\"/></w:rPr>")
                    append("<w:t>${escapeXml(headingText)}</w:t></w:r></w:p>")
                } else if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ")) {
                    val bulletText = trimmed.substring(2).trim()
                    append("<w:p><w:pPr><w:ind w:left=\"360\"/><w:spacing w:after=\"80\"/></w:pPr>")
                    append("<w:r><w:rPr><w:color w:val=\"1E40AF\"/></w:rPr><w:t>• </w:t></w:r>")
                    append("<w:r><w:rPr><w:sz w:val=\"22\"/></w:rPr><w:t>${escapeXml(bulletText)}</w:t></w:r></w:p>")
                } else {
                    append("<w:p><w:pPr><w:spacing w:after=\"120\"/></w:pPr>")
                    append("<w:r><w:rPr><w:sz w:val=\"22\"/><w:color w:val=\"1E293B\"/></w:rPr>")
                    append("<w:t>${escapeXml(line)}</w:t></w:r></w:p>")
                }
            }
        }

        val documentXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
    <w:body>
        $paragraphsXml
        <w:sectPr>
            <w:pgSz w:w="11906" w:h="16838"/>
            <w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440"/>
        </w:sectPr>
    </w:body>
</w:document>""".trimIndent()
        addZipEntry(zipOut, "word/document.xml", documentXml)

        zipOut.close()
    }

    private fun addZipEntry(zipOut: ZipOutputStream, entryName: String, content: String) {
        val entry = ZipEntry(entryName)
        zipOut.putNextEntry(entry)
        zipOut.write(content.toByteArray(Charsets.UTF_8))
        zipOut.closeEntry()
    }

    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
