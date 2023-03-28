package com.blumo.FileSentry4J;

import org.junit.jupiter.api.*;

import java.io.File;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This class is used to test the FileAclHelper class
 */
public class FileAclHelperTest {
    private static final Logger LOGGER = Logger.getLogger(FileAclHelper.class.getName());

    // Create a temporary file for testing
    private Path tempFile;
    // Get the current username
    private String userName;

    // Detect OS and create a temporary file for testing with the appropriate User/Group permissions
    @BeforeEach
    public void setup() throws Exception {
        // Create a temporary file for testing and get the path
        tempFile = Files.createTempFile("test", ".txt");
        Files.write(tempFile, "test".getBytes());

        // Create a File object for testing
        File testFile = new File(tempFile.toString());
        testFile.deleteOnExit();

        // get the current username
        userName = System.getProperty("user.name");

        // Set up the permissions for the test file
        String newPermissions = "r";
        try {
            FileAclHelper.ChangeFileACL(tempFile, newPermissions, userName);
        } catch (Exception e) {
            LOGGER.severe("FileAclHelper test failed. tmpFile: " + tempFile + ", newPermissions: " + newPermissions+ ", userName: "+ userName + ", error: " + e.getMessage());
        }
    }

    // Test the ChangeFileACL method
    @Test
    void testChangeFileACL() throws Exception {
        String expectedPermissions = "r";
        String actualPermissions = "";
        String fileOwnerName = "";
        String os = System.getProperty("os.name");
        if (os.startsWith("Windows")) {
            // Get the ACL for the file
            AclFileAttributeView aclView = Files.getFileAttributeView(tempFile, AclFileAttributeView.class);

            // Check that the owner has been set correctly
            UserPrincipal owner = aclView.getOwner();
            fileOwnerName = owner.getName().contains("\\") ? owner.getName().substring(owner.getName().indexOf("\\") + 1) : owner.getName();

            // Prep the permissions for comparison
            List<AclEntry> acl = aclView.getAcl();

            if (acl.toString().contains("READ_DATA")) {
                actualPermissions += "r";
            }
            if (acl.toString().contains("WRITE_DATA")) {
                actualPermissions += "w";
            }

        } else {
            // Get the owner of the file
            UserPrincipal owner = Files.getOwner(tempFile);
            fileOwnerName = owner.getName();

            // Get the permissions of the file
            Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(tempFile);

            if (permissions.toString().contains("OWNER_READ")) {
                actualPermissions += "r";
            }
            if (permissions.toString().contains("OWNER_WRITE")) {
                actualPermissions += "w";

            }
        }
        // Check that the owner is set correctly
        assertEquals(userName, fileOwnerName, "Owner not set correctly");

        // Check that the permissions are set correctly
        assertEquals(expectedPermissions, actualPermissions, "Permissions not set correctly");

        // Check that calling with null targetFilePath throws an IllegalArgumentException
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                FileAclHelper.ChangeFileACL(null, "r", userName));
        assertEquals("Missing required parameter: targetFilePath", exception.getMessage(), "Missing targetFilePath check failed");

        // Check that calling with null newPermissions throws an IllegalArgumentException
        exception = assertThrows(IllegalArgumentException.class, () ->
                FileAclHelper.ChangeFileACL(tempFile, null, userName));
        assertEquals("Missing required parameter: newPermissions", exception.getMessage(), "Missing newPermissions check failed");
    }
}
