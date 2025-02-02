/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.common.lib.policy;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(allOf = { PolicyTO.class })
public class AttrReleasePolicyTO extends PolicyTO {

    private static final long serialVersionUID = -1432411162433533300L;

    private int order;

    private Boolean status;

    private AttrReleasePolicyConf conf;

    @JacksonXmlProperty(localName = "_class", isAttribute = true)
    @JsonProperty("_class")
    @Schema(name = "_class", required = true, example = "org.apache.syncope.common.lib.policy.AttrReleasePolicyTO")
    @Override
    public String getDiscriminator() {
        return getClass().getName();
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(final int order) {
        this.order = order;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(final Boolean status) {
        this.status = status;
    }

    public AttrReleasePolicyConf getConf() {
        return conf;
    }

    public void setConf(final AttrReleasePolicyConf conf) {
        this.conf = conf;
    }
}
