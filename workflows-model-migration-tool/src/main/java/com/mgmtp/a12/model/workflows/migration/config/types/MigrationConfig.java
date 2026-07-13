
package com.mgmtp.a12.model.workflows.migration.config.types;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for MigrationConfig complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="MigrationConfig">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="migrationStep" type="{http://www.mgm-tp.com/a12/migration/config/v1}MigrationStepConfigType" maxOccurs="unbounded" minOccurs="0"/>
 *       </sequence>
 *       <attribute name="modelType" use="required" type="{http://www.mgm-tp.com/a12/migration/config/v1}MigratorModelType" />
 *       <attribute name="currentModelVersion" type="{http://www.mgm-tp.com/a12/migration/config/v1}ModelVersionType" />
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MigrationConfig", propOrder = {
    "migrationSteps"
})
@XmlRootElement(name = "migrationConfig")
public class MigrationConfig
    implements Serializable
{

    private static final long serialVersionUID = 1L;
    @XmlElement(name = "migrationStep")
    protected List<MigrationStepConfigType> migrationSteps;
    @XmlAttribute(name = "modelType", required = true)
    protected MigratorModelType modelType;
    @XmlAttribute(name = "currentModelVersion")
    protected String currentModelVersion;

    /**
     * Gets the value of the migrationSteps property.
     * 
     * <p>This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the migrationSteps property.</p>
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * </p>
     * <pre>
     * getMigrationSteps().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link MigrationStepConfigType }
     * </p>
     * 
     * 
     * @return
     *     The value of the migrationSteps property.
     */
    public List<MigrationStepConfigType> getMigrationSteps() {
        if (migrationSteps == null) {
            migrationSteps = new ArrayList<>();
        }
        return this.migrationSteps;
    }

    public boolean isSetMigrationSteps() {
        return ((this.migrationSteps!= null)&&(!this.migrationSteps.isEmpty()));
    }

    public void unsetMigrationSteps() {
        this.migrationSteps = null;
    }

    /**
     * Gets the value of the modelType property.
     * 
     * @return
     *     possible object is
     *     {@link MigratorModelType }
     *     
     */
    public MigratorModelType getModelType() {
        return modelType;
    }

    /**
     * Sets the value of the modelType property.
     * 
     * @param value
     *     allowed object is
     *     {@link MigratorModelType }
     *     
     */
    public void setModelType(MigratorModelType value) {
        this.modelType = value;
    }

    public boolean isSetModelType() {
        return (this.modelType!= null);
    }

    /**
     * Gets the value of the currentModelVersion property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCurrentModelVersion() {
        return currentModelVersion;
    }

    /**
     * Sets the value of the currentModelVersion property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCurrentModelVersion(String value) {
        this.currentModelVersion = value;
    }

    public boolean isSetCurrentModelVersion() {
        return (this.currentModelVersion!= null);
    }

}
