package org.antosik.reconciler

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement

@JacksonXmlRootElement(localName = "DATA_FETCH")
class DataFetch {
    companion object {
        val DEFAULT_PROPERTIES: List<DataItem.Property> = listOf(
            DataItem.Property.buildProperty("isValid", "true"),
            DataItem.Property.buildProperty("consume", "false")
        )
    }

    @JacksonXmlProperty(localName = "DATA_ITEM")
    @JacksonXmlElementWrapper(useWrapping = false)
    lateinit var dataItems: List<DataItem>
    class DataItem {
        companion object {
            fun buildDataItem(resource: String, properties: List<Property>, tenant: String): DataItem {
                val dataItem = DataItem()
                dataItem.resource = resource
                dataItem.properties = properties
                dataItem.tenant = tenant
                return dataItem
            }
        }
        @JacksonXmlProperty(localName = "RESOURCE")
        lateinit var resource: String

        @JacksonXmlElementWrapper(localName = "PROPERTIES")
        @JacksonXmlProperty(localName = "PROPERTY")
        lateinit var properties: List<Property>

        @JacksonXmlProperty(localName = "tenant", isAttribute = true)
        lateinit var tenant: String

        class Property {
            companion object {
                fun buildProperty(name: String, value: String): Property {
                    val property = Property()
                    property.name = name
                    property.value = value
                    return property
                }
            }
            @JacksonXmlProperty(localName = "name", isAttribute = true)
            lateinit var name: String
            @JacksonXmlProperty(localName = "value", isAttribute = true)
            lateinit var value: String
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Property

                if (name != other.name) return false
                if (value != other.value) return false

                return true
            }

            override fun hashCode(): Int {
                var result = name.hashCode()
                result = 31 * result + value.hashCode()
                return result
            }

        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as DataItem

            if (resource != other.resource) return false
            if (properties != other.properties) return false
            if (tenant != other.tenant) return false

            return true
        }

        override fun hashCode(): Int {
            var result = resource.hashCode()
            result = 31 * result + properties.hashCode()
            result = 31 * result + tenant.hashCode()
            return result
        }
    }
}
