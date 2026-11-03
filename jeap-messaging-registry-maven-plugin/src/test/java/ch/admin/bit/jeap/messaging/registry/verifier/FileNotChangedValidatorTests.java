package ch.admin.bit.jeap.messaging.registry.verifier;

import ch.admin.bit.jeap.messaging.avro.plugin.validator.MessageTypeRegistryConstants;
import ch.admin.bit.jeap.messaging.avro.plugin.validator.ValidationResult;
import ch.admin.bit.jeap.messaging.registry.helper.MessagingType;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

class FileNotChangedValidatorTests {
    private File oldCommon;
    private File newCommon;
    private File newSystem;
    private File oldEvent;
    private File newEvent;
    private File oldCommonSystem;
    private File newCommonSystem;
    private ValidationContext validationContext;

    @BeforeEach
    void setup(@TempDir File tmpDir) throws IOException {
        File oldDescriptor = new File(tmpDir, "old");
        FileUtils.forceMkdir(oldDescriptor);
        File newDescriptor = new File(tmpDir, "new");
        FileUtils.forceMkdir(newDescriptor);
        validationContext = ValidationContext.builder()
                .oldDescriptorDir(oldDescriptor)
                .descriptorDir(newDescriptor)
                .systemName("test")
                .messageTypeName("test")
                .messagingType(MessagingType.EVENT)
                .trunkBranchName("test")
                .build();
        oldCommon = new File(oldDescriptor, MessageTypeRegistryConstants.COMMON_DIR_NAME);
        newCommon = new File(newDescriptor, MessageTypeRegistryConstants.COMMON_DIR_NAME);
        File oldSystem = new File(oldDescriptor, "test");
        FileUtils.forceMkdir(oldSystem);
        newSystem = new File(newDescriptor, "test");
        FileUtils.forceMkdir(newSystem);
        oldCommonSystem = new File(oldSystem, MessageTypeRegistryConstants.COMMON_DIR_NAME);
        newCommonSystem = new File(newSystem, MessageTypeRegistryConstants.COMMON_DIR_NAME);
        oldEvent = new File(oldSystem, "event/test");
        FileUtils.forceMkdir(oldEvent);
        newEvent = new File(newSystem, "event/test");
        FileUtils.forceMkdir(newEvent);
    }

    @Test
    void commonFolderNotExisting() {
        ValidationResult validationResult = FileNotChangedValidator.commonRootDirNotChanged(validationContext);

        Assertions.assertTrue(validationResult.isValid(), String.join(",", validationResult.getErrors()));
    }

    @Test
    void commonFolderCreated() throws IOException {
        FileUtils.forceMkdir(newCommon);

        ValidationResult validationResult = FileNotChangedValidator.commonRootDirNotChanged(validationContext);

        Assertions.assertFalse(validationResult.isValid(), "Common folder cannot be created");
    }

    @Test
    void commonFolderDeleted() throws IOException {
        FileUtils.forceMkdir(oldCommon);

        ValidationResult validationResult = FileNotChangedValidator.commonRootDirNotChanged(validationContext);

        Assertions.assertFalse(validationResult.isValid(), "Common folder cannot be deleted");
    }

    @Test
    void commonFoldersEmpty() throws IOException {
        FileUtils.forceMkdir(oldCommon);
        FileUtils.forceMkdir(newCommon);

        ValidationResult validationResult = FileNotChangedValidator.commonRootDirNotChanged(validationContext);

        Assertions.assertTrue(validationResult.isValid(), String.join(",", validationResult.getErrors()));
    }

    @Test
    void commonFoldersFileCreated() throws IOException {
        FileUtils.forceMkdir(oldCommon);
        FileUtils.forceMkdir(newCommon);
        FileUtils.write(new File(newCommon, "test"), "hi");

        ValidationResult validationResult = FileNotChangedValidator.commonRootDirNotChanged(validationContext);

        Assertions.assertFalse(validationResult.isValid(), "File cannot be crated in common folder");
    }

    @Test
    void commonFoldersFileDeleted() throws IOException {
        FileUtils.forceMkdir(oldCommon);
        FileUtils.forceMkdir(newCommon);
        FileUtils.write(new File(oldCommon, "test"), "hi");

        ValidationResult validationResult = FileNotChangedValidator.commonRootDirNotChanged(validationContext);

        Assertions.assertFalse(validationResult.isValid(), "File cannot be delete in common folder");
    }

    @Test
    void commonFoldersExistingFile() throws IOException {
        FileUtils.forceMkdir(oldCommon);
        FileUtils.forceMkdir(newCommon);
        FileUtils.write(new File(oldCommon, "test"), "hi");
        FileUtils.write(new File(newCommon, "test"), "hi");

        ValidationResult validationResult = FileNotChangedValidator.commonRootDirNotChanged(validationContext);

        Assertions.assertTrue(validationResult.isValid(), String.join(",", validationResult.getErrors()));
    }

    @Test
    void commonFoldersFileChanged() throws IOException {
        FileUtils.forceMkdir(oldCommon);
        FileUtils.forceMkdir(newCommon);
        FileUtils.write(new File(oldCommon, "test"), "hi");
        FileUtils.write(new File(newCommon, "test"), "other");

        ValidationResult validationResult = FileNotChangedValidator.commonRootDirNotChanged(validationContext);

        Assertions.assertFalse(validationResult.isValid(), "File cannot be delete in common folder");
    }

    @Test
    void commonSystemFoldersNotExisting() {
        ValidationResult validationResult = FileNotChangedValidator.commonSystemDirNoFilesChanged(validationContext);

        Assertions.assertTrue(validationResult.isValid(), String.join(",", validationResult.getErrors()));
    }

    @Test
    void commonSystemFoldersCreated() throws IOException {
        FileUtils.forceMkdir(newCommonSystem);

        ValidationResult validationResult = FileNotChangedValidator.commonSystemDirNoFilesChanged(validationContext);

        Assertions.assertTrue(validationResult.isValid(), String.join(",", validationResult.getErrors()));
    }

    @Test
    void commonSystemFoldersDeleted() throws IOException {
        FileUtils.forceMkdir(oldCommonSystem);

        ValidationResult validationResult = FileNotChangedValidator.commonSystemDirNoFilesChanged(validationContext);

        Assertions.assertFalse(validationResult.isValid(), "Cannot delete common system folder");
    }

    @Test
    void commonSystemFoldersFileCreated() throws IOException {
        FileUtils.forceMkdir(newCommonSystem);
        FileUtils.forceMkdir(oldCommonSystem);
        FileUtils.write(new File(newCommonSystem, "test"), "other");

        ValidationResult validationResult = FileNotChangedValidator.commonSystemDirNoFilesChanged(validationContext);

        Assertions.assertTrue(validationResult.isValid(), String.join(",", validationResult.getErrors()));
    }

    @Test
    void commonSystemFoldersFileDeleted() throws IOException {
        FileUtils.forceMkdir(newCommonSystem);
        FileUtils.forceMkdir(oldCommonSystem);
        FileUtils.write(new File(oldCommonSystem, "test"), "other");

        ValidationResult validationResult = FileNotChangedValidator.commonSystemDirNoFilesChanged(validationContext);

        Assertions.assertFalse(validationResult.isValid(), "Cannot delete file in common system folder");
    }

    @Test
    void commonSystemFoldersFileChanged() throws IOException {
        FileUtils.forceMkdir(newCommonSystem);
        FileUtils.forceMkdir(oldCommonSystem);
        FileUtils.write(new File(oldCommonSystem, "test"), "hi");
        FileUtils.write(new File(newCommonSystem, "test"), "other");

        ValidationResult validationResult = FileNotChangedValidator.commonSystemDirNoFilesChanged(validationContext);

        Assertions.assertFalse(validationResult.isValid(), "Cannot change files in common system folder");
    }

    @Test
    void systemDeleted() throws IOException {
        FileUtils.deleteDirectory(newSystem);

        ValidationResult validationResult = FileNotChangedValidator.noSystemDeleted(validationContext);

        Assertions.assertFalse(validationResult.isValid(), "Cannot delete system");
    }

    @Test
    void eventDeleted() throws IOException {
        FileUtils.deleteDirectory(newEvent);

        ValidationResult validationResult = FileNotChangedValidator.noMessageTypeDeleted(validationContext);

        Assertions.assertFalse(validationResult.isValid(), "Cannot delete event");
    }

    @Test
    void schemaDeleted() throws IOException {
        FileUtils.write(new File(oldEvent, "test"), "hi");

        ValidationResult validationResult = FileNotChangedValidator.noExistingSchemasChanged(validationContext);

        Assertions.assertFalse(validationResult.isValid(), "Cannot delete schema");
    }

    @Test
    void schemaCreated() throws IOException {
        FileUtils.write(new File(newEvent, "test"), "hi");

        ValidationResult validationResult = FileNotChangedValidator.noExistingSchemasChanged(validationContext);

        Assertions.assertTrue(validationResult.isValid(), String.join(",", validationResult.getErrors()));
    }

    @Test
    void existingSchema() throws IOException {
        FileUtils.write(new File(newEvent, "test"), "hi");
        FileUtils.write(new File(oldEvent, "test"), "hi");

        ValidationResult validationResult = FileNotChangedValidator.noExistingSchemasChanged(validationContext);

        Assertions.assertTrue(validationResult.isValid(), String.join(",", validationResult.getErrors()));
    }

    @Test
    void schemaChanged() throws IOException {
        FileUtils.write(new File(newEvent, "test"), "hi");
        FileUtils.write(new File(oldEvent, "test"), "other");

        ValidationResult validationResult = FileNotChangedValidator.noExistingSchemasChanged(validationContext);

        Assertions.assertFalse(validationResult.isValid(), "Cannot change schema");
    }

    @Test
    void descriptorChanged_newVersionAdded_validationOK() throws IOException {
        String oldData = """
                {
                  "versions": [
                    {
                      "version": "1.0.0",
                      "valueSchema": "JmeDecreeCreatedEvent_v1.0.0.avdl"
                    }
                  ]
                }
                """;
        String newData = """
                {
                  "versions": [
                   {
                      "version": "1.0.0",
                      "valueSchema": "JmeDecreeCreatedEvent_v1.0.0.avdl"
                    },
                    {
                      "version": "2.0.0",
                      "valueSchema": "JmeDecreeCreatedEvent_v2.0.0.avdl",
                      "keySchema": "JmeDecreeCreatedEvent_key_v2.avdl"
                    }
                  ]
                }
                """;
        FileUtils.write(new File(newEvent, "test.json"), newData);
        FileUtils.write(new File(oldEvent, "test.json"), oldData);

        ValidationResult validationResult = FileNotChangedValidator.noExistingSchemasChanged(validationContext);
        Assertions.assertTrue(validationResult.isValid(), String.join(",", validationResult.getErrors()));
    }

    @Test
    void descriptorChanged_oldVersionModified_validationFailed() throws IOException {
        String oldData = """
                {
                  "versions": [
                    {
                      "version": "1.0.0",
                      "valueSchema": "JmeDecreeCreatedEvent_v1.0.0.avdl"
                    }
                  ]
                }
                """;
        String newData = """
                {
                  "versions": [
                    {
                      "version": "1.0.0",
                      "valueSchema": "JmeDecreeCreatedEvent_v1.0.0.avdl",
                      "keySchema": "JmeDecreeCreatedEvent_key_v1.avdl"
                    }
                  ]
                }
                """;
        FileUtils.write(new File(newEvent, "test.json"), newData);
        FileUtils.write(new File(oldEvent, "test.json"), oldData);

        ValidationResult validationResult = FileNotChangedValidator.noExistingSchemasChanged(validationContext);
        Assertions.assertFalse(validationResult.isValid(), "Cannot change schema");
    }

    private static final String OLD_IDL = """
            @namespace("ch.admin.test.domainevent.test.v1")

            /** Value type for test events */
            protocol TestEventProtocol {
                record TestEvent {
                    string id;
                    /** The origin of the request */
                    string origin;
                }
            }
            """;

    @Test
    void avroIdlSchemaWithAddedLineComment_validationOK() throws IOException {
        String newIdl = OLD_IDL.replace(
                "protocol TestEventProtocol {",
                "// A line comment added later\nprotocol TestEventProtocol {");

        ValidationResult validationResult = validateChangedAvroIdl(OLD_IDL, newIdl);

        Assertions.assertTrue(validationResult.isValid(), String.join(",", validationResult.getErrors()));
    }

    @Test
    void avroIdlSchemaWithChangedComments_validationOK() throws IOException {
        String newIdl = OLD_IDL
                .replace("/** Value type for test events */", "/** Value type for all test events of this system */")
                .replace("/** The origin of the request */", "/* The system or person that triggered the test, reworded */");

        ValidationResult validationResult = validateChangedAvroIdl(OLD_IDL, newIdl);

        Assertions.assertTrue(validationResult.isValid(), String.join(",", validationResult.getErrors()));
    }

    @Test
    void avroIdlSchemaWithRemovedComments_validationOK() throws IOException {
        String newIdl = OLD_IDL
                .replace("/** Value type for test events */\n", "")
                .replace("    /** The origin of the request */\n", "");

        ValidationResult validationResult = validateChangedAvroIdl(OLD_IDL, newIdl);

        Assertions.assertTrue(validationResult.isValid(), String.join(",", validationResult.getErrors()));
    }

    @Test
    void avroIdlSchemaReformatted_validationOK() throws IOException {
        String newIdl = OLD_IDL.replace("    ", "\t").replace("{\n", "{\n\n");

        ValidationResult validationResult = validateChangedAvroIdl(OLD_IDL, newIdl);

        Assertions.assertTrue(validationResult.isValid(), String.join(",", validationResult.getErrors()));
    }

    @Test
    void avroIdlSchemaWithoutOptionalWhitespace_validationOK() throws IOException {
        String newIdl = """
                @namespace("ch.admin.test.domainevent.test.v1")
                protocol TestEventProtocol{record TestEvent{string id;string origin;}}""";

        ValidationResult validationResult = validateChangedAvroIdl(OLD_IDL, newIdl);

        Assertions.assertTrue(validationResult.isValid(), String.join(",", validationResult.getErrors()));
    }

    @Test
    void avroIdlSchemaWithCommentBetweenTokens_validationOK() throws IOException {
        String newIdl = OLD_IDL.replace("string origin;", "string/* added comment */origin;");

        ValidationResult validationResult = validateChangedAvroIdl(OLD_IDL, newIdl);

        Assertions.assertTrue(validationResult.isValid(), String.join(",", validationResult.getErrors()));
    }

    @Test
    void avroIdlSchemaWithAddedField_validationFailed() throws IOException {
        String newIdl = OLD_IDL.replace("string origin;", "string origin;\n        string newField;");

        ValidationResult validationResult = validateChangedAvroIdl(OLD_IDL, newIdl);

        Assertions.assertFalse(validationResult.isValid(), "Cannot add a field to an existing schema");
    }

    @Test
    void avroIdlSchemaWithChangedNamespace_validationFailed() throws IOException {
        String newIdl = OLD_IDL.replace("test.v1", "test.v2");

        ValidationResult validationResult = validateChangedAvroIdl(OLD_IDL, newIdl);

        Assertions.assertFalse(validationResult.isValid(), "Cannot change the namespace of an existing schema");
    }

    @Test
    void avroIdlSchemaWithChangedDefaultValueContainingCommentMarkers_validationFailed() throws IOException {
        String oldIdl = OLD_IDL.replace("string origin;", "string origin = \"https://example.com/v1\";");
        String newIdl = OLD_IDL.replace("string origin;", "string origin = \"https://example.com/v2\";");

        ValidationResult validationResult = validateChangedAvroIdl(oldIdl, newIdl);

        Assertions.assertFalse(validationResult.isValid(), "Cannot change a default value of an existing schema");
    }

    @Test
    void avroIdlSchemaWithChangedWhitespaceInDefaultValue_validationFailed() throws IOException {
        String oldIdl = OLD_IDL.replace("string origin;", "string origin = \"a  b\";");
        String newIdl = OLD_IDL.replace("string origin;", "string origin = \"a b\";");

        ValidationResult validationResult = validateChangedAvroIdl(oldIdl, newIdl);

        Assertions.assertFalse(validationResult.isValid(), "Cannot change a default value of an existing schema");
    }

    @Test
    void avroIdlSchemaWithCarriageReturnLineEndings_validationOK() throws IOException {
        String carriageReturnOnly = OLD_IDL.replace("\n", "\r");

        ValidationResult validationResult = validateChangedAvroIdl(carriageReturnOnly, OLD_IDL);

        Assertions.assertTrue(validationResult.isValid(), String.join(",", validationResult.getErrors()));
    }

    @Test
    void avroIdlSchemaWithCarriageReturnCommentChanged_validationOK() throws IOException {
        String carriageReturnOnly = "// comment\r" + OLD_IDL.replace("\n", "\r");
        String changedComment = "// changed comment\r" + OLD_IDL.replace("\n", "\r");

        ValidationResult validationResult = validateChangedAvroIdl(carriageReturnOnly, changedComment);

        Assertions.assertTrue(validationResult.isValid(), String.join(",", validationResult.getErrors()));
    }

    @Test
    void avroIdlSchemaWithFieldAddedAfterCarriageReturnComment_validationFailed() throws IOException {
        String carriageReturnOnly = "// comment\r" + OLD_IDL.replace("\n", "\r");
        String fieldAdded = carriageReturnOnly.replace("string origin;", "string origin;\r    string added;");

        ValidationResult validationResult = validateChangedAvroIdl(carriageReturnOnly, fieldAdded);

        Assertions.assertFalse(validationResult.isValid(), "Cannot add a field to an existing schema");
    }

    @Test
    void avroIdlSchemaWithNonUtf8Comment_validationOK() throws IOException {
        FileUtils.writeByteArrayToFile(new File(oldEvent, "TestEvent_v1.0.0.avdl"),
                nonUtf8Idl("// Gr\u00e4nzfall\r\n"));
        FileUtils.writeByteArrayToFile(new File(newEvent, "TestEvent_v1.0.0.avdl"),
                nonUtf8Idl("// Gr\u00e4nzfall, neu formuliert\r\n"));

        ValidationResult validationResult = FileNotChangedValidator.noExistingSchemasChanged(validationContext);

        Assertions.assertTrue(validationResult.isValid(), String.join(",", validationResult.getErrors()));
    }

    private static byte[] nonUtf8Idl(String comment) {
        // ISO-8859-1 encoded umlaut is not valid UTF-8
        return (comment + "record TestEvent { string id; }").getBytes(StandardCharsets.ISO_8859_1);
    }

    @Test
    void avroIdlSchemaWithUnterminatedComment_validationFailed() throws IOException {
        String brokenIdl = OLD_IDL + "\n/* unterminated comment";

        ValidationResult validationResult = validateChangedAvroIdl(OLD_IDL, brokenIdl);

        Assertions.assertFalse(validationResult.isValid(), "Cannot append an unterminated comment");
        Assertions.assertTrue(validationResult.getErrors().stream()
                .anyMatch(error -> error.contains("Unterminated block comment")), validationResult.getErrors().toString());
    }

    private ValidationResult validateChangedAvroIdl(String oldIdl, String newIdl) throws IOException {
        FileUtils.write(new File(oldEvent, "TestEvent_v1.0.0.avdl"), oldIdl);
        FileUtils.write(new File(newEvent, "TestEvent_v1.0.0.avdl"), newIdl);
        return FileNotChangedValidator.noExistingSchemasChanged(validationContext);
    }
}
