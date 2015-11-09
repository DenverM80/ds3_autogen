/*
 * ******************************************************************************
 *   Copyright 2014-2015 Spectra Logic Corporation. All Rights Reserved.
 *   Licensed under the Apache License, Version 2.0 (the "License"). You may not use
 *   this file except in compliance with the License. A copy of the License is located at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   or in the "license" file accompanying this file.
 *   This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *   CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *   specific language governing permissions and limitations under the License.
 * ****************************************************************************
 */

package com.spectralogic.ds3autogen.java;

import com.spectralogic.ds3autogen.api.CodeGenerator;
import com.spectralogic.ds3autogen.api.FileUtils;
import com.spectralogic.ds3autogen.api.models.*;
import com.spectralogic.ds3autogen.java.converters.RequestConverter;
import com.spectralogic.ds3autogen.java.models.Request;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JavaCodeGenerator implements CodeGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(JavaCodeGenerator.class);

    private static final Path baseProjectPath = Paths.get("ds3-sdk/src/main/java/");

    private static final String ROOT_PACKAGE = "com.spectralogic.ds3client";
    private static final String COMMANDS_PACKAGE = ROOT_PACKAGE + ".commands";
    private static final String SPECTRADS3_COMMANDS_PACKAGE = COMMANDS_PACKAGE + ".spectrads3";

    private final Configuration config = new Configuration(Configuration.VERSION_2_3_23);

    private Ds3ApiSpec spec;
    private FileUtils fileUtils;
    private Path destDir;

    public JavaCodeGenerator() {
        config.setDefaultEncoding("UTF-8");
        config.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        config.setClassForTemplateLoading(JavaCodeGenerator.class, "/tmpls");
    }

    @Override
    public void generate(
            final Ds3ApiSpec spec,
            final FileUtils fileUtils,
            final Path destDir) throws IOException {
        this.spec = spec;
        this.fileUtils = fileUtils;
        this.destDir = destDir;

        try {
            generateCommands();
        } catch (final TemplateException e) {
            e.printStackTrace();
        }
    }

    private void generateCommands() throws IOException, TemplateException {
        for (final Ds3Request request : spec.getRequests()) {
            generateRequest(request);
        }
    }

    private void generateRequest(final Ds3Request ds3Request) throws IOException, TemplateException {
        final Template tmpl = getRequestTemplate(ds3Request);

        final Request request = getRequest(ds3Request);
        final Path requestPath = getPath(ds3Request, request);

        LOG.info("Getting outputstream for file:" + requestPath.toString());

        try (final OutputStream outStream = fileUtils.getOutputFile(requestPath);
             final Writer writer = new OutputStreamWriter(outStream)) {
            tmpl.process(request, writer);
        }

    }

    private Path getPath(final Ds3Request ds3Request, final Request request) {
        if (ds3Request.getClassification() == Classification.spectrads3) {
            return destDir.resolve(baseProjectPath.resolve(Paths.get(SPECTRADS3_COMMANDS_PACKAGE.replace(".", "/") + "/" + request.getName() + ".java")));
        } else {
            return destDir.resolve(baseProjectPath.resolve(Paths.get(COMMANDS_PACKAGE.replace(".", "/") + "/" + request.getName() + ".java")));
        }
    }

    private Request getRequest(final Ds3Request ds3Request) {
        if (ds3Request.getClassification() == Classification.spectrads3) {
            return RequestConverter.toRequest(ds3Request, SPECTRADS3_COMMANDS_PACKAGE);
        } else {
            return RequestConverter.toRequest(ds3Request, COMMANDS_PACKAGE);
        }
    }

    private Template getRequestTemplate(final Ds3Request ds3Request) throws IOException {
        final Template template;
        if (isBulkRequest(ds3Request)) {
            template = config.getTemplate("bulk_request_template.tmpl");
        } else if (isPhysicalPlacementRequest(ds3Request)) {
            template = config.getTemplate("physical_placement_request_template.tmpl");
        } else if (isMultiFileDelete(ds3Request)) {
            template = config.getTemplate("multi_file_delete_request_template.tmpl");
        } else if (isGetObject(ds3Request)) {
            template = config.getTemplate("get_object_template.tmpl");
        } else if (isCreateObject(ds3Request)) {
            template = config.getTemplate("create_object_template.tmpl");
        } else {
            template = config.getTemplate("request_template.tmpl");
        }

        return template;
    }

    private static boolean isPhysicalPlacementRequest(final Ds3Request ds3Request) {
        return ds3Request.getOperation() != null && (
                ds3Request.getOperation() == Operation.GET_PHYSICAL_PLACEMENT
                || ds3Request.getOperation() == Operation.VERIFY_PHYSICAL_PLACEMENT
                || ds3Request.getOperation() == Operation.START_BULK_VERIFY);
    }

    private static boolean isBulkRequest(final Ds3Request ds3Request) {
        return ds3Request.getOperation() != null && (
                ds3Request.getOperation() == Operation.START_BULK_GET
                || ds3Request.getOperation() == Operation.START_BULK_PUT);
    }

    private static boolean isMultiFileDelete(final Ds3Request ds3Request) {
        return ds3Request.getName().endsWith("DeleteObjectsRequestHandler");
    }

    private static boolean isCreateObject(final Ds3Request ds3Request) {
        return ds3Request.getName().endsWith("CreateObjectRequestHandler");
    }

    private static boolean isGetObject(final Ds3Request ds3Request) {
        return ds3Request.getName().endsWith("GetObjectRequestHandler");
    }
}
