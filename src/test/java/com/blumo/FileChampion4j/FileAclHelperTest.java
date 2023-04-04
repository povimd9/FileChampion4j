package com.blumo.FileChampion4j;

import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.EnumSet;
import java.util.Set;


public class FileAclHelperTest {

    private Path tempFilePath;
    private String sharedMessages = "Expected error message to start with '";

    @BeforeEach
    void setUp() throws Exception {
        // Create a temporary file
        tempFilePath = Files.createTempFile("test-file", ".txt");

    }

    // Test change permissions
    @ParameterizedTest
    @ValueSource(strings = {"r", "w", "x", "rw", "rwx"})
    void testChangeFileAclWrite(String newPermissions) throws Exception {
        String newOwnerUsername = System.getProperty("user.name");
        FileAclHelper aclHelper = new FileAclHelper();

        // Change the ACL of the temporary file
        String result = aclHelper.changeFileAcl(tempFilePath, newOwnerUsername, newPermissions);

        // Check that the result is success
        assertTrue(result.startsWith("Success"));

        // Check that the new owner is correct
        assertTrue(checkOwner(tempFilePath, newOwnerUsername), "Expected owner to be " + newOwnerUsername + " but was " + Files.getOwner(tempFilePath));

        // Check that the new permissions are correct
        String actualPermissions = "";
        String os = System.getProperty("os.name");
        if (os.startsWith("Windows")) {
            actualPermissions = getAclPermissions(tempFilePath);
        } else {
            actualPermissions = getPosixFilePermissions(tempFilePath);
        }
        assertEquals(newPermissions, actualPermissions);
    }

    // Test non existing permissions
    @Test
    void testChangeFileAclNonExistingPermissions() throws Exception {
        String newPermissions = "invalid-permissions";
        String newOwnerUsername = System.getProperty("user.name");
        FileAclHelper aclHelper = new FileAclHelper();

        // Change the ACL of the temporary file
        String result = aclHelper.changeFileAcl(tempFilePath, newOwnerUsername, newPermissions);

        // Check that the result is success
        String expectedErrMsg = "Error: Invalid permissions";
        assertTrue(result.startsWith(expectedErrMsg), String.format("%s %s' but got: %s", sharedMessages, expectedErrMsg, result));
    }

    // Check getUserPrinciple failure
    @Test
    void testGetUserPrincipleFailure() throws Exception {
        String newPermissions = "rwx";
        FileAclHelper aclHelper = new FileAclHelper();

        // Change the ACL of the temporary file
        String result = aclHelper.changeFileAcl(tempFilePath, "invalid-user-394f84398fn3948dfn239048d023d", newPermissions);

        // Check that the result is success
        String expectedErrMsg = "Error: Could not get user principal";
        assertTrue(result.startsWith(expectedErrMsg), String.format("%s %s' but got: %s", sharedMessages, expectedErrMsg, result));
    }

    // Check access denied failure
    @Test
    void testAccessDeniedFailure() throws Exception {
        String newPermissions = "r";
        FileAclHelper aclHelper = new FileAclHelper();
        String result = "";

        if (System.getProperty("os.name").startsWith("Windows")) {
            aclHelper.changeFileAcl(tempFilePath, System.getProperty("user.name"), newPermissions);
            result = aclHelper.changeFileAcl(tempFilePath, System.getProperty("user.name"), "rwx");
        } else {
            // Create the test user
            Process process = Runtime.getRuntime().exec("sudo useradd -m testuser");
            process.waitFor();
            process = Runtime.getRuntime().exec("echo 'password' | sudo passwd --stdin testuser");
            process.waitFor();
            // Switch to the test user account
            Process lowPrivProcess = Runtime.getRuntime().exec("echo 'password' | sudo -S su testuser");
            lowPrivProcess.waitFor();
            result = aclHelper.changeFileAcl(tempFilePath, "root", "rwx");
        }

        // Check that the result is success
        String expectedErrMsg = "Error: Access denied";
        assertTrue(result.startsWith(expectedErrMsg), String.format("%s %s' but got: %s", sharedMessages, expectedErrMsg, result));
    }

    // Check file system failure
    @Test
    void testFileSystemFailure() throws Exception {
        String newPermissions = "r";
        FileAclHelper aclHelper = new FileAclHelper();
        String newOwnerUsername = System.getProperty("user.name");

        // Change the ACL of the temporary file
        aclHelper.changeFileAcl(tempFilePath, newOwnerUsername, newPermissions);

        String result = aclHelper.changeFileAcl(Paths.get("C:\\invalid\\path\\to\\file.txt"), newOwnerUsername, "rwx");

        // Check that the result is success
        String expectedErrMsg = "Error: File system error";
        assertTrue(result.startsWith(expectedErrMsg), String.format("%s %s' but got: %s", sharedMessages, expectedErrMsg, result));
    }


    /////////////////////////
    // Helper methods

    private String getPosixFilePermissions (Path path) throws Exception {
        // Get the Posix file permissions for the file
        Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(path);

        // Convert the user permissions to rwx format
        StringBuilder rwxPermissions = new StringBuilder();
        for (PosixFilePermission permission : EnumSet.copyOf(permissions)) {
            switch (permission) {
                case OWNER_READ:
                    rwxPermissions.append("r");
                    break;
                case OWNER_WRITE:
                    rwxPermissions.append("w");
                    break;
                case OWNER_EXECUTE:
                    rwxPermissions.append("x");
                    break;
                default:
                    break;
            }
        }

        return rwxPermissions.toString();
    }

    private String getAclPermissions (Path path) throws Exception {
        // Get the ACL entries for the tmp test file
        AclFileAttributeView view = Files.getFileAttributeView(
            Paths.get(path.toUri()), AclFileAttributeView.class);
        
        // Get the ACL entries for the file
        String permissions = "";
        for (AclEntry entry : view.getAcl()) {
            // Check each permission and add the corresponding symbol to the output string
            if (entry.permissions().contains(AclEntryPermission.READ_DATA)) {
                permissions += "r";
            }
            if (entry.permissions().contains(AclEntryPermission.WRITE_DATA)) {
                permissions += "w";
            }
            if (entry.permissions().contains(AclEntryPermission.EXECUTE)) {
                permissions += "x";
            }
        }
        return permissions.replace(String.valueOf("-"), "" );
    }

    private boolean checkOwner (Path path, String owner) throws Exception {
        UserPrincipalLookupService lookupService = path.getFileSystem().getUserPrincipalLookupService();
        UserPrincipal expectedOwner = lookupService.lookupPrincipalByName(owner);
        UserPrincipal actualOwner = Files.getOwner(path);
        return expectedOwner.equals(actualOwner);
    }


}