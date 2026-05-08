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
