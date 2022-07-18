package org.antosik.reconciler

import com.fasterxml.jackson.core.exc.StreamReadException
import com.fasterxml.jackson.databind.DatabindException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import java.nio.file.Files
import kotlin.io.path.isRegularFile
import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.util.stream.Collectors.toSet
import kotlin.io.path.Path
import kotlin.io.path.relativeTo


object Reconciler {
    data class Entry(val name: String, val type: String, val tenant: String)

    private val mapper = XmlMapper.builder()
        .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true)
        .addModule(JacksonXmlModule().apply {
            setDefaultUseWrapper(false)
        })
        .build().apply {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
        .registerKotlinModule()

    @JvmStatic
    fun main(args: Array<String>) {
        val parser = ArgParser("example")
        val xmlInput by parser.option(ArgType.String, shortName = "p", description = "XML input file").default("src/test/resources/xmlparsing.xml")
        val rootPath by parser.option(ArgType.String, shortName = "r", description = "root of directories").default("src/test/resources/reconciler")
        val xmlOutput by parser.option(ArgType.String, shortName = "o", description = "XML input file").default("xmlparsing-with-added.xml")

        parser.parse(args)
        try {
            run(xmlInput, rootPath, xmlOutput)
        } catch (e: RuntimeException) {
            println("Error: ${e.message}")
        }
    }

    private fun run(xmlInput: String, rootPath: String, xmlOutput: String) {
        println("Current directory: ${System.getProperty("user.dir")}")
        val dataFetch = readDescription(File(xmlInput))
        val dataFetchEntries =
            dataFetch.dataItems.map {
                dataItemToEntry(it)
            }.toSet()

        val fileTreeEntries = tryReadTree(rootPath)

        val toRemove = dataFetchEntries - fileTreeEntries
        val toAdd = fileTreeEntries - dataFetchEntries

        println("To remove: $toRemove")
        println("To add: $toAdd")

        val dataItemsToAdd = toAdd.map {
            entryToDataItem(it)
        }
        dataFetch.dataItems = dataFetch.dataItems + dataItemsToAdd

        mapper.writeValue(File(xmlOutput), dataFetch)
    }

    private fun tryReadTree(rootPath: String): Set<Entry> {
        try {
            return readTree(Path(rootPath))
        } catch (e: java.nio.file.NoSuchFileException) {
            throw RuntimeException("Directory $rootPath does not exist")
        }
    }

    private fun entryToDataItem(entry: Entry): DataFetch.DataItem {
        return DataFetch.DataItem.buildDataItem(
            resource = entry.name,
            properties = DataFetch.DEFAULT_PROPERTIES,
            tenant = entry.tenant
        )
    }

    private fun readTree(rootPath: Path): Set<Entry> {
        Files.walk(rootPath).use { walker ->
            return walker
                .filter { it.isRegularFile() }
                .map { path ->
                    val relativizedPath = path.relativeTo(rootPath)
                    val lastLevel = relativizedPath.nameCount
                    println(relativizedPath)
                    Entry(
                        relativizedPath.toString(),
                        relativizedPath.getName(lastLevel - 2).toString(),
                        relativizedPath.getName(lastLevel - 3).toString()
                    )
                }
                .collect(toSet())
            }
    }

    fun dataItemToEntry(from: DataFetch.DataItem): Entry {
        return when {
            from.resource.contains("securitykey") -> "securitykey"
            from.resource.contains("otherkey") -> "otherkey"
            else -> throw RuntimeException("resource should contain securitykey or otherkey")
        }.let {
            Entry(from.resource, it, from.tenant)
        }
    }

    fun readDescription(file: File): DataFetch {
        try {
            return mapper.readValue(file, DataFetch::class.java)
        } catch (e: IOException) {
            throw RuntimeException("Cannot open xml description file ${file.absoluteFile}", e)
        } catch (e: StreamReadException) {
            throw RuntimeException("XML not readable ${file.absoluteFile}", e)
        } catch (e: DatabindException) {
            throw RuntimeException("XML (${file.absoluteFile}) does not contain expected data fetch structure", e)
        }
    }
}