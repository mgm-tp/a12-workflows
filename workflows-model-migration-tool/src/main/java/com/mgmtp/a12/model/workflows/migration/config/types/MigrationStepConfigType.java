
package com.mgmtp.a12.model.workflows.migration.config.types;

import java.io.Serializable;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for MigrationStepConfigType complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="MigrationStepConfigType">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="bean" type="{http://www.mgm-tp.com/a12/migration/config/v1}BeanType"/>
 *       </sequence>
 *       <attribute name="sourceModelVersion" use="required" type="{http://www.mgm-tp.com/a12/migration/config/v1}ModelVersionType" />
 *       <attribute name="targetModelVersion" use="required" type="{http://www.mgm-tp.com/a12/migration/config/v1}ModelVersionType" />
 *       <attribute name="optional" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MigrationStepConfigType", propOrder = {
    "bean"
})
public class MigrationStepConfigType
    implements Serializable
{

    private static final long serialVersionUID = 1L;
    @XmlElement(required = true)
    protected BeanType bean;
    @XmlAttribute(name = "sourceModelVersion", required = true)
    protected String sourceModelVersion;
    @XmlAttribute(name = "targetModelVersion", required = true)
    protected String targetModelVersion;
    @XmlAttribute(name = "optional")
    protected Boolean optional;

    /**
     * Gets the value of the bean property.
     * 
     * @return
     *     possible object is
     *     {@link BeanType }
     *     
     */
    public BeanType getBean() {
        return bean;
    }

    /**
     * Sets the value of the bean property.
     * 
     * @param value
     *     allowed object is
     *     {@link BeanType }
     *     
     */
    public void setBean(BeanType value) {
        this.bean = value;
    }

    public boolean isSetBean() {
        return (this.bean!= null);
    }

    /**
     * Gets the value of the sourceModelVersion property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSourceModelVersion() {
        return sourceModelVersion;
    }

    /**
     * Sets the value of the sourceModelVersion property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSourceModelVersion(String value) {
        this.sourceModelVersion = value;
    }

    public boolean isSetSourceModelVersion() {
        return (this.sourceModelVersion!= null);
    }

    /**
     * Gets the value of the targetModelVersion property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTargetModelVersion() {
        return targetModelVersion;
    }

    /**
     * Sets the value of the targetModelVersion property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTargetModelVersion(String value) {
        this.targetModelVersion = value;
    }

    public boolean isSetTargetModelVersion() {
        return (this.targetModelVersion!= null);
    }

    /**
     * Gets the value of the optional property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isOptional() {
        return optional;
    }

    /**
     * Sets the value of the optional property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setOptional(boolean value) {
        this.optional = value;
    }

    public boolean isSetOptional() {
        return (this.optional!= null);
    }

    public void unsetOptional() {
        this.optional = null;
    }

}
