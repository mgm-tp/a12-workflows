/**
 * LICENSE
 * SPDX-License-Identifier: EUPL-1.2 OR LicenseRef-commercial
 *
 * mgm A12 Platform – https://www.mgm-tp.com
 * Copyright (c) 2012–2026 mgm technology partners GmbH
 *
 * This software is developed by mgm technology partners GmbH and
 * is available under a choice of two different licenses:
 *
 * 1. Open-Source License – EUPL v1.2
 *    You may redistribute and/or modify this software under the
 *    terms of the European Union Public Licence, version 1.2.
 *    The license text is available at https://eupl.eu/
 *    and included in this distribution as:
 *        licenses/EUPL-1.2.txt
 *
 * 2. Commercial License
 *    Alternatively, you may obtain a commercial license from
 *    mgm technology partners GmbH, that permits use of this
 *    software under different terms (including support and
 *    maintenance options). For commercial licensing, please contact:
 *        a12-license@mgm-tp.com
 *
 * You must select and comply with exactly one of the above
 * licensing options.
 *
 * Third-Party Components
 * ----------------------
 * This software includes third-party components.
 * Their licenses are provided in:
 *    THIRD_PARTY_NOTICES
 *    licenses/   (folder with full license texts)
 *
 * Trademarks
 * ----------
 * “mgm A12” is a registered trademark of mgm technology partners GmbH.
 * The A12 logo is protected by copyright.
 *
 * Warranty Disclaimer (applies to either option)
 * ----------------------------------------------
 * THIS SOFTWARE IS PROVIDED “AS IS” AND WITHOUT WARRANTY OF ANY KIND,
 * WHETHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, AND NON-INFRINGEMENT,
 * EXCEPT WHERE SUCH DISCLAIMERS ARE NOT PERMITTED BY APPLICABLE LAW.
 * SEE THE RESPECTIVE LICENSE TEXT FOR DETAILS.
 *
 * End of LICENSE.
 */

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
