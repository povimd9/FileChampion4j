package com.blumo.FileSentry4J;

import org.junit.jupiter.api.*;

import java.io.File;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.List;


import static org.junit.jupiter.api.Assertions.*;

public class FileAclHelperTest {

    private Path tempFile;
    private File testFile;
    private String userName;
    private String groupName;

    @BeforeEach
    public void setup() throws Exception {
        // get the operating system name
        String osName = System.getProperty("os.name");

        // Create a temporary file for testing
        tempFile = Files.createTempFile("test", ".txt");
        Files.write(tempFile, "test".getBytes());
        testFile = new File(tempFile.toString());
        testFile.deleteOnExit();

        // get the current username
        userName = System.getProperty("user.name");

        // check if the platform is Windows, Linux, or macOS
        if (osName.matches(".*Windows.*")) {
            groupName = "Users";
        } else if (osName.matches(".*Linux.*")) {
            Path path = Path.of("/");
            UserPrincipalLookupService lookupService = path.getFileSystem().getUserPrincipalLookupService();
            groupName = lookupService.lookupPrincipalByGroupName("users").getName();
        } else if (osName.matches(".*Mac.*")) {
            groupName = "staff";
        } else if (osName.matches(".*SunOS.*")) {
            groupName = "users";
        } else {
            throw new Exception("Unsupported operating system: " + osName);
        }

        // Set up the permissions for the test file
        String newPermissions = "r";
        FileAclHelper.ChangeFileACL(tempFile, newPermissions, userName, groupName);
    }


    @Test
    void testChangeFileACL() throws Exception {

        AclFileAttributeView aclView = Files.getFileAttributeView(tempFile, AclFileAttributeView.class);
        List<AclEntry> aclEntries = aclView.getAcl();

        // Check that the owner has been set correctly
        UserPrincipal owner = aclView.getOwner();
        String fileOwnerName = owner.getName().contains("\\") ? owner.getName().substring(owner.getName().indexOf("\\") + 1) : owner.getName();
        assertEquals(userName, fileOwnerName, "Owner not set correctly");



        List<AclEntry> acl = aclView.getAcl();
        // Check that the permissions have been set correctly
        String expectedPermissions = "r";
        String actualPermissions = "";
        if (acl.toString().contains("READ_DATA")) {
            actualPermissions += "r";
        }
        if (acl.toString().contains("WRITE_DATA")) {
            actualPermissions += "w";
        }


        assertEquals(expectedPermissions, actualPermissions, "Permissions not set correctly");

        // Check that calling with null targetFilePath throws an IllegalArgumentException
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                FileAclHelper.ChangeFileACL(null, "r", userName, groupName));
        assertEquals("Missing required parameter: targetFilePath", exception.getMessage(), "Missing targetFilePath check failed");

        // Check that calling with null newPermissions throws an IllegalArgumentException
        exception = assertThrows(IllegalArgumentException.class, () ->
                FileAclHelper.ChangeFileACL(tempFile, null, userName, groupName));
        assertEquals("Missing required parameter: newPermissions", exception.getMessage(), "Missing newPermissions check failed");
    }
}
