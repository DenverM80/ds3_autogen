/*
 * ******************************************************************************
 *   Copyright 2014-2017 Spectra Logic Corporation. All Rights Reserved.
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.spectralogic.ds3autogen.api.CodeGenerator;
import com.spectralogic.ds3autogen.api.FileUtils;
import com.spectralogic.ds3autogen.api.models.apispec.Ds3ApiSpec;
import com.spectralogic.ds3autogen.api.models.apispec.Ds3Request;
import com.spectralogic.ds3autogen.api.models.apispec.Ds3Type;
import com.spectralogic.ds3autogen.api.models.docspec.Ds3DocSpec;
import com.spectralogic.ds3autogen.java.converters.ClientConverter;
import com.spectralogic.ds3autogen.java.generators.requestmodels.*;
import com.spectralogic.ds3autogen.java.generators.responsemodels.*;
import com.spectralogic.ds3autogen.java.generators.responseparser.*;
import com.spectralogic.ds3autogen.java.generators.typemodels.*;
import com.spectralogic.ds3autogen.java.helpers.JavaHelper;
import com.spectralogic.ds3autogen.java.models.*;
import com.spectralogic.ds3autogen.utils.Ds3TypeClassificationUtil;
import com.spectralogic.ds3autogen.utils.Helper;
import freemarker.template.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.spectralogic.ds3autogen.java.models.Constants.*;
import static com.spectralogic.ds3autogen.java.utils.JavaModuleUtil.getCommandPackage;
import static com.spectralogic.ds3autogen.utils.ConverterUtil.*;
import static com.spectralogic.ds3autogen.utils.Ds3RequestClassificationUtil.*;
import static com.spectralogic.ds3autogen.utils.Ds3TypeClassificationUtil.*;

/**
 * Generates Java SDK code based on the contents of a Ds3ApiSpec.
 *
 * Generated Code:
 *   Request handlers
 *   Response handlers
 *   Ds3Client
 *   Ds3ClientImpl
 *   Models
 */
public class JavaCodeGenerator implements CodeGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(JavaCodeGenerator.class);

    private static final Path baseProjectPath = Paths.get("ds3-sdk/src/main/java/");

    private final Configuration config = new Configuration(Configuration.VERSION_2_3_23);

    private FileUtils fileUtils;
    private Path destDir;

    public JavaCodeGenerator() throws TemplateModelException {
        config.setDefaultEncoding("UTF-8");
        config.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        config.setClassForTemplateLoading(JavaCodeGenerator.class, "/tmpls/java/");
        config.setSharedVariable("javaHelper", JavaHelper.getInstance());
        config.setSharedVariable("helper", Helper.getInstance());
    }

    @Override
    public void generate(
            final Ds3ApiSpec spec,
            final FileUtils fileUtils,
            final Path destDir,
            final Ds3DocSpec docSpec) throws IOException {
        this.fileUtils = fileUtils;
        this.destDir = destDir;

        try {
            final ImmutableList<Ds3Request> requests = spec.getRequests();
            final ImmutableMap<String, Ds3Type> types = removeUnusedTypes(
                    spec.getTypes(),
                    spec.getRequests());

            generateCommands(requests, types, docSpec);
        } catch (final TemplateException e) {
            LOG.error("Unable to generate Java SDK code", e);
        }
    }

    /**
     * Generates all code associated with the Ds3ApiSpec
     * @throws IOException
     * @throws TemplateException
     */
    private void generateCommands(
            final ImmutableList<Ds3Request> requests,
            final ImmutableMap<String, Ds3Type> types,
            final Ds3DocSpec docSpec) throws IOException, TemplateException {
        generateAllRequests(requests, docSpec);
        generateAllModels(types);
        generateClient(requests, docSpec);
    }

    /**
     * Generates the Models described within the Ds3ApiSpec that are being used
     * by at least one request
     * @throws IOException
     * @throws TemplateException
     */
    private void generateAllModels(final ImmutableMap<String, Ds3Type> types) throws IOException, TemplateException {
        if (isEmpty(types)) {
            LOG.info("There were no models to generate");
            return;
        }
        for (final Ds3Type ds3Type : types.values()) {
            generateModel(ds3Type);
        }
    }

    /**
     * Generates code for a Model from a Ds3Type
     * @param ds3Type A Ds3Type
     * @throws IOException
     * @throws TemplateException
     */
    private void generateModel(final Ds3Type ds3Type) throws IOException, TemplateException {
        final Template modelTmpl = getModelTemplate(ds3Type);
        final Model model = toModel(ds3Type, getModelPackage());
        final Path modelPath = toModelFilePath(model.getName());

        LOG.info("Getting outputstream for file: {}", modelPath.toString());

        try (final OutputStream outStream = fileUtils.getOutputFile(modelPath);
             final Writer writer = new OutputStreamWriter(outStream)) {
            modelTmpl.process(model, writer);
        }
    }

    /**
     * Converts a Ds3Type into a Model
     */
    private Model toModel(final Ds3Type ds3Type, final String packageName) {
        final TypeModelGenerator<?> modelGenerator = getModelGenerator(ds3Type);
        return modelGenerator.generate(ds3Type, packageName);
    }

    /**
     * Retrieves the associated type model generator for the specified Ds3TYpe
     */
    private TypeModelGenerator<?> getModelGenerator(final Ds3Type ds3Type) {
        if (isChecksumType(ds3Type)) {
            return new ChecksumTypeGenerator();
        }
        if (Ds3TypeClassificationUtil.isJobsApiBean(ds3Type)) {
            return new JobsApiBeanTypeGenerator();
        }
        if (isCommonPrefixesType(ds3Type)) {
            return new CommonPrefixGenerator();
        }
        return new BaseTypeGenerator();
    }

    /**
     * Gets the Model template that is used to generate the given Ds3Type content
     * @param ds3Type A Ds3Type
     * @return The appropriate template to generate the given Ds3Type
     * @throws IOException
     */
    private Template getModelTemplate(final Ds3Type ds3Type) throws IOException {
        if (isHttpErrorType(ds3Type)) {
            return config.getTemplate("models/http_error_template.ftl");
        }
        if (isChecksumType(ds3Type)) {
            return config.getTemplate("models/checksum_type_template.ftl");
        }
        if (isS3Object(ds3Type)) {
            return config.getTemplate("models/s3object_model_template.ftl");
        }
        if (isBulkObject(ds3Type)) {
            return config.getTemplate("models/bulk_object_template.ftl");
        }
        if (hasContent(ds3Type.getEnumConstants())) {
            return config.getTemplate("models/enum_model_template.ftl");
        }
        if (hasContent(ds3Type.getElements())) {
            return config.getTemplate("models/model_template.ftl");
        }
        throw new IllegalArgumentException("Type must have Elements and/or EnumConstants");
    }

    /**
     * Determines if a given Ds3Type is a BulkObject
     */
    private boolean isBulkObject(final Ds3Type ds3type) {
        return ds3type.getName().endsWith(".BulkObject");
    }

    /**
     * Determines if a given Ds3Type is the S3Object
     */
    private boolean isS3Object(final Ds3Type ds3type) {
        return ds3type.getName().endsWith(".S3Object");
    }

    /**
     * Gets the package name for where the Models will be generated
     * @return The package name of where the Models are going to be generated
     */
    private String getModelPackage() {
        return ROOT_PACKAGE_PATH + MODELS_PACKAGE;
    }

    /**
     * Converts a Model name into a Model file path
     * @param modelName The name of a Model
     * @return The file path of a Model
     */
    private Path toModelFilePath(final String modelName) {
        return destDir.resolve(baseProjectPath.resolve(
                Paths.get(getModelPackage().replace(".", "/") + "/" + modelName + ".java")));
    }

    /**
     * Generates the Request Handler, Response Handler, and Response Parser
     * for all specified requests
     * @throws IOException
     * @throws TemplateException
     */
    private void generateAllRequests(
            final ImmutableList<Ds3Request> requests,
            final Ds3DocSpec docSpec) throws IOException, TemplateException {
        if (isEmpty(requests)) {
            LOG.info("There were no requests to generate");
            return;
        }
        for (final Ds3Request request : requests) {
            generateRequest(request, docSpec);
            generateResponse(request);
            generateResponseParser(request);
        }
    }

    /**
     * Generates the Response Parser code for the specified Ds3Request
     */
    private void generateResponseParser(final Ds3Request ds3Request) throws IOException, TemplateException {
        final Template tmpl = getResponseParserTemplate(ds3Request);

        final ResponseParser responseParser = toResponseParser(ds3Request);
        final Path responsePath = toResponseParserPath(responseParser.getName());

        LOG.info("Getting outputstream for file: {}", responsePath.toString());

        try (final OutputStream outStream = fileUtils.getOutputFile(responsePath);
             final Writer writer = new OutputStreamWriter(outStream)) {
            tmpl.process(responseParser, writer);
        }
    }

    /**
     * Converts a file name into the path containing said file within the client path
     * @param fileName The name of a file
     * @return The client path to the given file
     */
    protected Path toResponseParserPath(final String fileName) {
        return destDir.resolve(
                baseProjectPath.resolve(
                        Paths.get(RESPONSE_PARSER_PACKAGE_PATH.replace(".", "/") + "/" + fileName + ".java")));
    }

    /**
     * Retrieves the response parser template used to generate the specified request
     */
    protected Template getResponseParserTemplate(final Ds3Request ds3Request) throws IOException {
        if (isBulkRequest(ds3Request)) {
            return config.getTemplate("responseparser/bulk_response_parser.ftl");
        }
        if (isAllocateJobChunkRequest(ds3Request)) {
            return config.getTemplate("responseparser/allocate_job_chunk_parser.ftl");
        }
        if (isHeadObjectRequest(ds3Request)) {
            return config.getTemplate("responseparser/head_object_parser.ftl");
        }
        if (isGetObjectAmazonS3Request(ds3Request)) {
            return config.getTemplate("responseparser/get_object_parser.ftl");
        }
        if (isGetJobChunksReadyForClientProcessingRequest(ds3Request)) {
            return config.getTemplate("responseparser/get_job_chunks_ready_parser.ftl");
        }
        return config.getTemplate("responseparser/response_parser_template.ftl");
    }

    /**
     * Converts a Ds3Request into a Response Parser model
     */
    protected static ResponseParser toResponseParser(final Ds3Request ds3Request) {
        final ResponseParserGenerator<?> generator = getResponseParserGenerator(ds3Request);
        return generator.generate(ds3Request, RESPONSE_PARSER_PACKAGE_PATH);
    }

    /**
     * Retrieves the response parser generator used to generate the specified request
     */
    protected static ResponseParserGenerator<?> getResponseParserGenerator(final Ds3Request ds3Request) {
        if (isHeadBucketRequest(ds3Request)) {
            return new HeadBucketParserGenerator();
        }
        if (isAllocateJobChunkRequest(ds3Request)) {
            return new AllocateJobChunkParserGenerator();
        }
        if (isHeadObjectRequest(ds3Request)) {
            return new HeadObjectParserGenerator();
        }
        if (isGetObjectAmazonS3Request(ds3Request)) {
            return new GetObjectParserGenerator();
        }
        if (isGetJobChunksReadyForClientProcessingRequest(ds3Request)) {
            return new GetJobChunksReadyParserGenerator();
        }
        return new BaseResponseParserGenerator();
    }

    /**
     * Generates the Client and ClientImpl code that contains all non-SpectraInternal requests
     * that are described within the Ds3ApiSpec
     * @throws IOException
     * @throws TemplateException
     */
    private void generateClient(
            final ImmutableList<Ds3Request> requests,
            final Ds3DocSpec docSpec) throws IOException, TemplateException {
        if (isEmpty(requests)) {
            LOG.info("Not generating client: no requests.");
            return;
        }
        final Template clientTmpl = config.getTemplate("client/ds3client_template.ftl");
        final Client client = ClientConverter.toClient(requests, ROOT_PACKAGE_PATH, docSpec);
        final Path clientPath = toClientPath("Ds3Client.java");

        LOG.info("Getting outputstream for file: {}", clientPath.toString());

        try (final OutputStream outStream = fileUtils.getOutputFile(clientPath);
             final Writer writer = new OutputStreamWriter(outStream)) {
            clientTmpl.process(client, writer);
        }

        final Template clientImplTmpl = config.getTemplate("client/ds3client_impl_template.ftl");
        final Path clientImplPath = toClientPath("Ds3ClientImpl.java");

        LOG.info("Getting outputstream for file: {}", clientPath.toString());

        try (final OutputStream outStream = fileUtils.getOutputFile(clientImplPath);
             final Writer writer = new OutputStreamWriter(outStream)) {
            clientImplTmpl.process(client, writer);
        }
    }

    /**
     * Converts a file name into the path containing said file within the client path
     * @param fileName The name of a file
     * @return The client path to the given file
     */
    private Path toClientPath(final String fileName) {
        return destDir.resolve(baseProjectPath.resolve(Paths.get(ROOT_PACKAGE_PATH.replace(".", "/") + "/" + fileName)));
    }

    /**
     * Generates the code for the Response handler described in a Ds3Request
     * @param ds3Request A Ds3Request
     * @throws IOException
     * @throws TemplateException
     */
    private void generateResponse(final Ds3Request ds3Request) throws IOException, TemplateException {
        final Template tmpl = getResponseTemplate(ds3Request);

        final Response response = toResponse(ds3Request);
        final Path responsePath = getPathFromPackage(ds3Request, response.getName());

        LOG.info("Getting outputstream for file: {}", responsePath.toString());

        try (final OutputStream outStream = fileUtils.getOutputFile(responsePath);
             final Writer writer = new OutputStreamWriter(outStream)) {
            tmpl.process(response, writer);
        }
    }

    /**
     * Converts a Ds3Request into a Response model
     * @param ds3Request A Ds3Request
     * @return A Response
     */
    private static Response toResponse(final Ds3Request ds3Request) {
        final ResponseModelGenerator<?> modelGenerator = getResponseGenerator(ds3Request);
        return modelGenerator.generate(ds3Request, getCommandPackage(ds3Request));
    }

    /**
     * Retrieves the associated response generator for the specified Ds3Request
     */
    protected static ResponseModelGenerator<?> getResponseGenerator(final Ds3Request ds3Request) {
        if (isHeadObjectRequest(ds3Request)) {
            return new HeadObjectResponseGenerator();
        }
        if (isHeadBucketRequest(ds3Request)) {
            return new HeadBucketResponseGenerator();
        }
        if (isAllocateJobChunkRequest(ds3Request)
                || isGetJobChunksReadyForClientProcessingRequest(ds3Request)) {
            return new RetryAfterResponseGenerator();
        }
        if (isBulkRequest(ds3Request)) {
            return new BulkResponseGenerator();
        }
        if (isGetObjectAmazonS3Request(ds3Request)) {
            return new GetObjectResponseGenerator();
        }
        //This should be the last test so that it does not overwrite any special cased generators
        if (supportsPaginationRequest(ds3Request)) {
            return new PaginationResponseGenerator();
        }
        return new BaseResponseGenerator();
    }

    /**
     * Gets the Response template that is used to generate the given Ds3Request's
     * Response handler
     * @param ds3Request A Ds3Request
     * @return The appropriate template to generate the required Response
     * @throws IOException
     */
    private Template getResponseTemplate(final Ds3Request ds3Request) throws IOException {
        if (isHeadObjectRequest(ds3Request)) {
            return config.getTemplate("response/head_object_response.ftl");
        }
        if (isHeadBucketRequest(ds3Request)) {
            return config.getTemplate("response/head_bucket_response.ftl");
        }
        if (isAllocateJobChunkRequest(ds3Request)) {
            return config.getTemplate("response/allocate_chunk_response.ftl");
        }
        if (isGetJobChunksReadyForClientProcessingRequest(ds3Request)) {
            return config.getTemplate("response/chunks_ready_response.ftl");
        }
        if (isBulkRequest(ds3Request)) {
            return config.getTemplate("response/bulk_response.ftl");
        }
        //This should be the last test so that it does not overwrite any special cased templates
        if (supportsPaginationRequest(ds3Request)) {
            return config.getTemplate("response/pagination_response.ftl");
        }
        return config.getTemplate("response/response_template.ftl");
    }

    /**
     * Generates the code for the Request handler described in a Ds3Request
     * @param ds3Request A Ds3Request
     * @throws IOException
     * @throws TemplateException
     */
    private void generateRequest(final Ds3Request ds3Request, final Ds3DocSpec docSpec) throws IOException, TemplateException {
        final Template tmpl = getRequestTemplate(ds3Request);

        final Request request = toRequest(ds3Request, docSpec);
        final Path requestPath = getPathFromPackage(ds3Request, request.getName());

        LOG.info("Getting outputstream for file: {}", requestPath.toString());

        try (final OutputStream outStream = fileUtils.getOutputFile(requestPath);
             final Writer writer = new OutputStreamWriter(outStream)) {
            tmpl.process(request, writer);
        }
    }

    /**
     * Returns the file system path for a request given it's package
     * @param ds3Request A Ds3Request
     * @param fileName The file name to be given to this Ds3Request
     * @return The system path to this Ds3Request's generated code
     */
    private Path getPathFromPackage(final Ds3Request ds3Request, final String fileName) {
        return destDir.resolve(baseProjectPath.resolve(
                Paths.get(getCommandPackage(ds3Request).replace(".", "/") + "/" + fileName + ".java")));
    }

    /**
     * Converts a Ds3Request into a Request model
     * @param ds3Request A Ds3Request
     * @return A Request model
     */
    private Request toRequest(final Ds3Request ds3Request, final Ds3DocSpec docSpec) {
        final RequestModelGenerator<?> modelGenerator = getRequestGenerator(ds3Request);
        return modelGenerator.generate(ds3Request, getCommandPackage(ds3Request), docSpec);
    }

    /**
     * Retrieves the associated request generator for the specified Ds3Request
     */
    static RequestModelGenerator<?> getRequestGenerator(final Ds3Request ds3Request) {
        if (hasIdsRequestPayload(ds3Request)) {
            return new IdsRequestPayloadGenerator();
        }
        if (hasStringRequestPayload(ds3Request)) {
            return new StringRequestPayloadGenerator();
        }
        if (isBulkRequest(ds3Request)) {
            return new BulkRequestGenerator();
        }
        if (hasSimpleObjectsRequestPayload(ds3Request) || isCreateVerifyJobRequest(ds3Request)) {
            return new ObjectsRequestPayloadGenerator();
        }
        if (isCreateObjectRequest(ds3Request)) {
            return new CreateObjectRequestGenerator();
        }
        if (isCreateNotificationRequest(ds3Request)) {
            return new CreateNotificationRequestGenerator();
        }
        if ((isGetNotificationRequest(ds3Request) || isDeleteNotificationRequest(ds3Request)) && ds3Request.getIncludeInPath()) {
            return new NotificationRequestGenerator();
        }
        if (isGetObjectRequest(ds3Request)) {
            return new  GetObjectRequestGenerator();
        }
        if (isMultiFileDeleteRequest(ds3Request)) {
            return new MultiFileDeleteRequestGenerator();
        }
        if (isCreateMultiPartUploadPartRequest(ds3Request)) {
            return new StreamRequestPayloadGenerator();
        }
        if (isCompleteMultiPartUploadRequest(ds3Request)) {
            return new CompleteMultipartUploadRequestGenerator();
        }
        return new BaseRequestGenerator();
    }

    /**
     * Gets the appropriate template that will generate the code for this
     * Ds3Request's request handler
     * @param ds3Request A Ds3Request
     * @return The appropriate template to generate the required Request
     * @throws IOException
     */
    private Template getRequestTemplate(final Ds3Request ds3Request) throws IOException {
        if (isBulkRequest(ds3Request)) {
            return config.getTemplate("request/bulk_request_template.ftl");
        }
        if (hasIdsRequestPayload(ds3Request)) {
            return config.getTemplate("request/ids_request_payload_template.ftl");
        }
        if (hasStringRequestPayload(ds3Request)) {
            return config.getTemplate("request/request_with_string_payload_template.ftl");
        }
        if (hasSimpleObjectsRequestPayload(ds3Request) || isCreateVerifyJobRequest(ds3Request)) {
            return config.getTemplate("request/objects_request_payload_request_template.ftl");
        }
        if (isMultiFileDeleteRequest(ds3Request)) {
            return config.getTemplate("request/multi_file_delete_request_template.ftl");
        }
        if (isGetObjectRequest(ds3Request)) {
            return config.getTemplate("request/get_object_template.ftl");
        }
        if (isCreateObjectRequest(ds3Request)) {
            return config.getTemplate("request/create_object_template.ftl");
        }
        if (isDeleteNotificationRequest(ds3Request)) {
            return config.getTemplate("request/delete_notification_request_template.ftl");
        }
        if (isCreateNotificationRequest(ds3Request)) {
            return config.getTemplate("request/create_notification_request_template.ftl");
        }
        if (isGetNotificationRequest(ds3Request) && ds3Request.getIncludeInPath()) {
            return config.getTemplate("request/get_notification_request_template.ftl");
        }
        if (isGetJobRequest(ds3Request)) {
            return config.getTemplate("request/get_job_request_template.ftl");
        }
        if (isCompleteMultiPartUploadRequest(ds3Request)) {
            return config.getTemplate("request/complete_multipart_upload_template.ftl");
        }
        return config.getTemplate("request/request_template.ftl");
    }
}
