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
package org.apache.syncope.fit.console;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import org.apache.syncope.client.console.pages.Types;
import org.apache.syncope.client.console.panels.AjaxDataTablePanel;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.IndicatingOnConfirmAjaxLink;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.util.tester.FormTester;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class AnyTypesITCase extends AbstractTypesITCase {

    @Test
    public void read() {
        browsingToAnyTypes();
        wicketTester.assertComponent(
                DATATABLE_PATH
                + ":tablePanel:groupForm:"
                + "checkgroup:dataTable:body:rows:1:cells:1:cell", Label.class);

        Component result = findComponentByProp(KEY, DATATABLE_PATH, "GROUP");

        wicketTester.assertComponent(
                result.getPageRelativePath() + ":cells:4:cell:panelEdit:editLink", IndicatingAjaxLink.class);

        wicketTester.clickLink(
                result.getPageRelativePath() + ":cells:4:cell:panelEdit:editLink");

        wicketTester.assertComponent(
                "body:content:tabbedPanel:panel:modal", BaseModal.class);
    }

    @Test
    public void create() {
        browsingToAnyTypes();
        final String anyTypeTest = "anyTypeTest2";

        wicketTester.clickLink("body:content:tabbedPanel:panel:container:content:add");

        wicketTester.assertComponent(
                "body:content:tabbedPanel:panel:modal", Modal.class);

        final FormTester formTester = wicketTester.newFormTester("body:content:tabbedPanel:panel:modal:form");
        formTester.setValue("content:anyTypeDetailsPanel:container:form:key:textField", anyTypeTest);
        formTester.setValue(
                "content:anyTypeDetailsPanel:container:form:classes:paletteField:recorder", "csv");

        wicketTester.clickLink("body:content:tabbedPanel:panel:modal:dialog:footer:inputs:0:submit");
        wicketTester.assertInfoMessages("Operation executed successfully");

        wicketTester.clearFeedbackMessages();
        wicketTester.assertRenderedPage(Types.class);

        wicketTester.clickLink("body:content:tabbedPanel:tabs-container:tabs:1:link");
        wicketTester.assertComponent(DATATABLE_PATH, AjaxDataTablePanel.class);

        Component result = findComponentByProp(KEY, DATATABLE_PATH, anyTypeTest);

        wicketTester.assertLabel(result.getPageRelativePath() + ":cells:1:cell", anyTypeTest);
        wicketTester.assertLabel(result.getPageRelativePath() + ":cells:3:cell", "[csv]");
    }

    @Test
    public void update() {
        final String name = "anyTypeClassUpdate";
        createAnyTypeClassWithoutSchema(name);
        browsingToAnyTypes();

        wicketTester.assertComponent(
                DATATABLE_PATH
                + ":tablePanel:groupForm:checkgroup:dataTable:"
                + "body:rows:1:cells:4:cell:panelEdit:editLink", IndicatingAjaxLink.class);

        wicketTester.clickLink(
                DATATABLE_PATH
                + ":tablePanel:groupForm:checkgroup:dataTable:body:rows:1:cells:4:cell:panelEdit:editLink");

        final FormTester formTester =
                wicketTester.newFormTester("body:content:tabbedPanel:panel:modal:form");
        formTester.setValue(
                "content:anyTypeDetailsPanel:container:form:classes:paletteField:recorder", name + ",minimal group");

        wicketTester.clickLink("body:content:tabbedPanel:panel:modal:dialog:footer:inputs:0:submit");
        wicketTester.assertInfoMessages("Operation executed successfully");
    }

    @Test
    public void delete() {
        final String name = "anyTypeDelete";
        createAnyType(name);
        browsingToAnyTypes();

        wicketTester.assertComponent(DATATABLE_PATH, AjaxDataTablePanel.class);
        Component result = findComponentByProp(KEY, DATATABLE_PATH, name);

        assertNotNull(result);
        wicketTester.assertComponent(
                result.getPageRelativePath() + ":cells:4:cell:panelDelete:deleteLink",
                IndicatingOnConfirmAjaxLink.class);

        wicketTester.getRequest().addParameter("confirm", "true");
        wicketTester.clickLink(
                wicketTester.getComponentFromLastRenderedPage(
                        result.getPageRelativePath() + ":cells:4:cell:panelDelete:deleteLink"));

        wicketTester.executeAjaxEvent(wicketTester.getComponentFromLastRenderedPage(
                result.getPageRelativePath() + ":cells:4:cell:panelDelete:deleteLink"), "onclick");
        wicketTester.assertInfoMessages("Operation executed successfully");

        wicketTester.cleanupFeedbackMessages();
        result = findComponentByProp(KEY, DATATABLE_PATH, name);

        assertNull(result);
    }
}
