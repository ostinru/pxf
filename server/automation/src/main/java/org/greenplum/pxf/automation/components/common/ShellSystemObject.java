package org.greenplum.pxf.automation.components.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.aqua.sysobj.conn.CliConnection;
import com.aqua.sysobj.conn.CmdConnection;
import com.aqua.sysobj.conn.LinuxDefaultCliConnection;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.ExecStartCmd;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.StreamType;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.transport.DockerHttpClient;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;
import lombok.SneakyThrows;
import org.buildobjects.process.ProcBuilder;
import org.buildobjects.process.ProcResult;
import org.buildobjects.process.TimeoutException;
import org.greenplum.pxf.automation.utils.curl.CurlUtils;
import jsystem.framework.report.Reporter;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.lang.StringUtils;

import org.greenplum.pxf.automation.components.common.cli.ShellCommandErrorException;

import com.aqua.sysobj.conn.CliCommand;
import org.greenplum.pxf.automation.utils.jsystem.report.ReportUtils;

import com.github.dockerjava.api.DockerClient;

import javax.annotation.Nullable;


/**
 * General Shell system objects, each System Object can extend it or use it.
 */
public class ShellSystemObject extends BaseSystemObject {
    private DockerClient dockerClient;
    // Find your path by running: `docker context ls`
    // FIXME:
    private String dockerHost = "unix:///Users/ostinru/.docker/run/docker.sock";  // "unix:///var/run/docker.sock";

    @Deprecated
    private String host = "localHost";
    @Deprecated
    private String masterHost = "localHost";
    @Deprecated
    private String hostName = "";
    @Deprecated
    private String userName;
    @Deprecated
    private String password;
    @Deprecated
    private String privateKey;
    @Nullable
    private String containerId;

    private String lastCmdResult = "";
    private int lastCommandExitCode = EXIT_CODE_NOT_EXISTS;
    // ignore passing local env vars to ssh connection
    private boolean ignoreEnvVars = false;

    public static final long _1_SECOND = 1000;
    public static final long _2_SECONDS = (_1_SECOND * 2);
    public static final long _5_SECONDS = (_1_SECOND * 5);
    public static final long _10_SECONDS = (_5_SECONDS * 2);
    public static final long _30_SECONDS = (_1_SECOND * 30);
    public static final long _1_MINUTE = (_30_SECONDS * 2);
    public static final long _2_MINUTES = (_1_MINUTE * 2);
    public static final long _5_MINUTES = (_1_MINUTE * 5);
    public static final long _10_MINUTES = _5_MINUTES * 2;
    public static final long _30_MINUTES = _10_MINUTES * 3;

    public static final int MIN_COMMAND_TIMEOUT = 100;

    public static final int EXIT_CODE_SUCCESS = 0;
    public static final int EXIT_CODE_NOT_EXISTS = -1;

    // the max timeout for command execution
    private long commandTimeout = _10_SECONDS;

    private String[] requiredEnvParams = new String[] {
            "JAVA_HOME",
            "GPHOME",
            "GPHD_ROOT",
            "GPDATA",
            "MASTER_DATA_DIRECTORY",
            "PGPORT",
            "PGHOST",
            "PGDATABASE"
    };

    public ShellSystemObject() {

    }

    /**
     * C'tor with option if to use silent mode of jsystem report
     *
     * @param silentReport if true silent else will try to write to jsystem
     *            report for every report
     */
    public ShellSystemObject(boolean silentReport) {
        super(silentReport);
    }

    @Override
    public void init() throws Exception {
        super.init();

        ReportUtils.startLevel(report, getClass(), "init");

        // if no user injected by the user, use "user.name" of the machine
        if (getUserName() == null) {
            setUserName(System.getProperty("user.name"));
        }

        // if no password injected us empty string
        if (getPassword() == null) {
            setPassword("");
        }

        ReportUtils.report(report, getClass(), "Establish connection to docker at: " + dockerHost);

        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerHost)
                .withDockerTlsVerify(false)
                .build();

        DockerHttpClient httpClient = new ZerodepDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(commandTimeout))
                .responseTimeout(Duration.ofSeconds(commandTimeout * 10))
                .build();

        dockerClient = DockerClientBuilder.getInstance(config)
                .withDockerHttpClient(httpClient)
                .build();

        // FIXME:
        // if (!ignoreEnvVars) {
        //     runCommand(getExportForRequiredEnvVars());
        // }

        ReportUtils.stopLevel(report);
    }

    @Override
    public void close() {
        if (dockerClient != null) {
            try {
                dockerClient.close();
            } catch (Exception e) {
                // Ignore close errors
            }
        }
        super.close();
    }


    /**
     * execute command-line command, verify exit code to be EXIT_CODE_SUCCESS
     * and store the result in lastCmdResult.
     *
     * @param command command to execute
     * @throws IOException
     * @throws ShellCommandErrorException
     */
    public void runCommand(String command) throws IOException, ShellCommandErrorException {
        runCommand(containerId, command, EXIT_CODE_SUCCESS);
    }

    /**
     * execute command-line command, verify exit code to be EXIT_CODE_SUCCESS
     * and store the result in lastCmdResult.
     *
     * @param containerId container id to run command in
     * @param command command to execute
     * @throws IOException
     * @throws ShellCommandErrorException
     */
    @Deprecated // FIXME: fix usages!
    public void runCommand(String containerId, String command) throws IOException, ShellCommandErrorException {
        runCommand(containerId, command, EXIT_CODE_SUCCESS);
    }

    /**
     * execute command-line command, check expectedExitCode and store the result
     * in lastCmdResult.
     *
     * @param containerId container id to run command in
     * @param command command to execute
     * @param expectedExitCode to check after command execution
     * @throws IOException
     * @throws ShellCommandErrorException
     */
    @Deprecated // FIXME: fix usages!
    public void runCommand(String containerId, String command, int expectedExitCode)
            throws IOException, ShellCommandErrorException {
        String commandAdditionalMessage = "("
                + getHost()
                + ((StringUtils.isEmpty(getHostName())) ? (")") : ("/"
                        + getHostName() + ")"));
        ReportUtils.startLevel(report, getClass(), commandAdditionalMessage,
                command);

        // Create command
        ExecCreateCmdResponse execCreateResp = dockerClient.execCreateCmd(containerId)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withCmd("sh", "-c", command)
                .exec();

        // Run command:
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();

        ExecStartCmd startCmd = dockerClient.execStartCmd(execCreateResp.getId())
                .withDetach(false)
                .withTty(false);

        try {
            startCmd.exec(new ResultCallback.Adapter< Frame >() {
                        @Override
                        public void onNext(Frame frame) {
                            try {
                                if (frame.getStreamType() == StreamType.STDOUT) {
                                    outputStream.write(frame.getPayload());
                                } else if (frame.getStreamType() == StreamType.STDERR) {
                                    errorStream.write(frame.getPayload());
                                }
                            } catch (IOException e) {
                                // ignore
                            }
                        }
                    }).awaitCompletion(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new ShellCommandErrorException(e.getMessage());
        }

        // get stdout & exit code
        lastCmdResult = outputStream.toString("UTF-8") + errorStream.toString("UTF-8");
        long exitCode = dockerClient.inspectExecCmd(execCreateResp.getId())
                .exec()
                .getExitCodeLong();


        ReportUtils.report(report, getClass(), lastCmdResult);

        // if expectedExitCode=EXIT_CODE_NOT_EXISTS it means no need to check
        // exit code
        if (expectedExitCode != EXIT_CODE_NOT_EXISTS) {
            // get the last ran command exit code
            lastCommandExitCode = (int) exitCode;
            // throw exception if last command failed
            if (lastCommandExitCode != expectedExitCode) {
                throw new ShellCommandErrorException("Command: \"" + command
                        + "\" returned exit code " + lastCommandExitCode
                        + " expected: " + expectedExitCode);
            }
        }
        ReportUtils.stopLevel(report);
    }


    /**
     * execute command-line command on local machine, verify exit code to be EXIT_CODE_SUCCESS
     * and store the result in lastCmdResult.
     *
     * @param command command to execute
     * @throws IOException
     * @throws ShellCommandErrorException
     */
    public void runLocalCommand(String command, Map<String, String> env) throws IOException, ShellCommandErrorException {
        runLocalCommand(command, env, EXIT_CODE_SUCCESS);
    }


    /**
     * execute command-line command, check expectedExitCode and store the result
     * in lastCmdResult.
     *
     * @param command command to execute
     * @param expectedExitCode to check after command execution
     * @throws IOException
     * @throws ShellCommandErrorException
     */
    public void runLocalCommand(String command, Map<String, String> env, int expectedExitCode) throws IOException, ShellCommandErrorException {
        ReportUtils.startLevel(report, getClass(), command);

        ProcBuilder procBuilder = new ProcBuilder(command)
                .withTimeoutMillis(commandTimeout * 1000);

        if (env != null) {
            procBuilder.withVars(env);
        }

        ProcResult result;
        try {
            result = procBuilder.run();
        } catch (TimeoutException ex) {
            throw new ShellCommandErrorException(ex.getMessage());
        }

        lastCmdResult = result.getOutputString() + result.getErrorString();
        ReportUtils.report(report, getClass(), lastCmdResult);
        lastCommandExitCode = result.getExitValue();

        // if expectedExitCode=EXIT_CODE_NOT_EXISTS it means no need to check
        // exit code
        if (expectedExitCode != EXIT_CODE_NOT_EXISTS) {
            // get the last ran command exit code
            lastCommandExitCode = getLastLocalExitCode();
            // throw exception if last command failed
            if (lastCommandExitCode != expectedExitCode) {
                throw new ShellCommandErrorException("Command: \"" + command
                        + "\" returned exit code " + lastCommandExitCode
                        + " expected: " + expectedExitCode);
            }
        }
        ReportUtils.stopLevel(report);
    }

    /**
     * get exit code using "echo $?". It highly recommended to call it after
     * performing command otherwise it might not get the exit code and will
     * raise a {@link NumberFormatException}
     */
    private int getLastLocalExitCode() {
        return lastCommandExitCode;
    }

    /**
     * perform jps command
     *
     * @param containerId container id to run command in
     * @throws IOException
     * @throws ShellCommandErrorException
     */
    protected void jps(String containerId) throws IOException, ShellCommandErrorException {
        runCommand(containerId, "jps");
    }

    /**
     * Runs curl command
     *
     * @param host host
     * @param port port
     * @param path path (excluding host and port)
     * @throws Exception if curl command failed or response didn't have expected
     *             format
     */
    public String curl(String host, String port, String path) throws Exception {
        CurlUtils curl = new CurlUtils(host, port, path);
        runLocalCommand(curl.getCommand(), null);
        return parseCurlResponse();
    }

    /**
     * Runs curl command
     *
     * @param host host
     * @param port port
     * @param path path (excluding host and port)
     * @param params params (map of key value pairs of params for post requests)
     * @return parsed curl response
     * @throws Exception if curl command failed or response didn't have expected
     *             format
     */
    public String curl(String host, String port, String path, String requestType, Map<String, String> headers, List<String> params) throws Exception {
        CurlUtils curl = new CurlUtils(host, port, path, requestType, headers, params);
        runLocalCommand(curl.getCommand(), null);
        return parseCurlResponse();
    }

    /**
     * Parses result and get the actual output from the last command result. The
     * actual output is of the form:
     * {@code <command><new line><output><terminator char>} e.g.
     * {@code
     * curl "http://localhost:5888/pxf/ProtocolVersion"
     * PXF protocol version v14#
     * }
     *
     * @return parsed curl response
     * @throws Exception if response doesn't have new line as expected
     */
    private String parseCurlResponse() throws Exception {
        String response = getLastCmdResult();
        if (StringUtils.isEmpty(response)) {
            return response;
        }
        int newLineIndex = response.indexOf("\r\n");
        // response need to have new line (\r\n) and some data afterwards (the
        // actual response). If that's not the case, we have a problem
        if (newLineIndex == -1 || response.length() <= 2) {
            throw new Exception(
                    "Curl response is not formatted as expected (response: "
                            + response + ")");
        }
        return response.substring(newLineIndex + 2, response.length() - 1);
    }

    /**
     * Close shell connection
     */
    @SneakyThrows
    public void disconnect() {
        dockerClient.close();
    }

    /**
     * get shell export command for all required sut or env vars.
     *
     * @return export command for the env vars
     */
    protected String getExportForRequiredEnvVars() {
        String result = "export";
        for (int i = 0; i < requiredEnvParams.length; i++) {
            result += " " + getEnvVarStatement(requiredEnvParams[i]);
        }
        return result;
    }

    /**
     * returns string that includes the required env vatibale key = it value
     *
     * @param envVariable required env variable
     * @return VAR=<value> if exists, else returns empty string.
     */
    private String getEnvVarStatement(String envVariable) {
        // get value of env variable
        String envVarValue = getEnvVar(envVariable);
        // if not empty construct string to return
        if (StringUtils.isNotEmpty(envVarValue)) {
            return (envVariable + "=" + envVarValue);
        }
        // if empty return empty string
        return "";
    }

    /**
     * Read variable from SUT ShellSystemObject element. If value doesn't exist
     * or empty, read it from system env.
     *
     * @param var environment variable name
     * @return environment variable var value
     */
    private String getEnvVar(String var) {
        String result = null;

        try {
            result = sut.getValue("/sut/shellsystemobject/" + var);
        } catch (Exception e) {
            ReportUtils.report(report, getClass(), "Didn't find SUT value for "
                    + var, Reporter.WARNING);
            result = null;
        }

        if ((result == null) || result.isEmpty()) {
            result = System.getenv(var);
        }

        ReportUtils.report(report, getClass(), "Value for var " + var + " is "
                + result);

        return result;
    }

    /**
     * Delete directory recursively
     *
     * @param containerId container id to run command in
     * @param directoryToDelete
     * @throws IOException
     * @throws ShellCommandErrorException
     */
    public void deleteDirectory(String containerId, String directoryToDelete) throws IOException,
            ShellCommandErrorException {
        runCommand(containerId, "rm -rf " + directoryToDelete);
    }

    /**
     * copy from remote machine to local path
     *
     * @param containerId container id to run command in
     * @param fromPath remote path to copy from
     * @param toPath local destination path
     * @throws IOException
     * @throws ShellCommandErrorException
     */
    public void copyFromRemoteMachine(String containerId, String fromPath, String toPath) throws ShellCommandErrorException {
        File dest = new File(toPath);
        if (!dest.exists()) {
            dest.mkdirs();
        }

        try (InputStream tarStream = dockerClient.copyArchiveFromContainerCmd(containerId, fromPath).exec()) {
            // Untar the stream to the destination directory
            try (TarArchiveInputStream tarIn = new TarArchiveInputStream(tarStream)) {
                TarArchiveEntry entry;
                while ((entry = tarIn.getNextTarEntry()) != null) {
                    File outFile = new File(dest, entry.getName());
                    if (entry.isDirectory()) {
                        outFile.mkdirs();
                    } else {
                        outFile.getParentFile().mkdirs();
                        try (OutputStream out = new FileOutputStream(outFile)) {
                            byte[] buffer = new byte[8192];
                            int len;
                            while ((len = tarIn.read(buffer)) != -1) {
                                out.write(buffer, 0, len);
                            }
                        }
                        outFile.setExecutable((entry.getMode() & 0100) != 0, false);
                        outFile.setReadable((entry.getMode() & 0400) != 0, false);
                        outFile.setWritable((entry.getMode() & 0200) != 0, false);
                    }
                }
            }
        } catch (Exception e) {
            throw new ShellCommandErrorException("Failed to copy from container: " + containerId + " > " + fromPath +
                    " to " + toPath, e);
        }
    }

    /**
     * copy from local machine path to remote machine
     *
     * @param containerId container id to run command in
     * @param fromPath local path to copy from
     * @param toPath remote destination path in the container
     * @throws ShellCommandErrorException
     */
    public void copyToRemoteMachine(String containerId, String fromPath, String toPath) throws ShellCommandErrorException {
        File source = new File(fromPath);
        if (!source.exists()) {
            throw new ShellCommandErrorException("Source path does not exist: " + fromPath);
        }

        // Create a tar archive of the source file or directory
        try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
             TarArchiveOutputStream tarOut = new TarArchiveOutputStream(byteOut)) {

            addFileToTar(tarOut, source, source.getName());
            tarOut.finish();

            // Copy the tar archive to the container
            try (InputStream tarInputStream = new ByteArrayInputStream(byteOut.toByteArray())) {
                dockerClient.copyArchiveToContainerCmd(containerId)
                        .withTarInputStream(tarInputStream)
                        .withRemotePath(toPath)
                        .exec();
            }
        } catch (Exception e) {
            throw new ShellCommandErrorException("Failed to copy to container: " + e.getMessage());
        }
    }

    /**
     * Helper method to add a file or directory recursively to a TarArchiveOutputStream.
     */
    private void addFileToTar(TarArchiveOutputStream tarOut, File file, String entryName) throws IOException {
        TarArchiveEntry entry = new TarArchiveEntry(file, entryName);
        tarOut.putArchiveEntry(entry);

        if (file.isFile()) {
            try (FileInputStream in = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    tarOut.write(buffer, 0, len);
                }
            }
            tarOut.closeArchiveEntry();
        } else if (file.isDirectory()) {
            tarOut.closeArchiveEntry();
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    addFileToTar(tarOut, child, entryName + "/" + child.getName());
                }
            }
        }
    }

    /**
     * Copy to List of remote machines with same credentials
     *
     * @param containerIds container id to run command in
     * @param filePath to copy
     * @param target in remote machines
     * @throws IOException
     * @throws ShellCommandErrorException
     */
    public void copyToRemoteMachines(List<String> containerIds, String filePath,
                                     String target) throws IOException,
            ShellCommandErrorException {
        for (String containerId : containerIds) {
            copyToRemoteMachine(containerId, filePath, target);
        }
    }

    /**
     * Run command on remote node
     *
     * @param containerId container id to run command in
     * @param command command to execute
     * @throws IOException
     * @throws ShellCommandErrorException
     */
    @Deprecated
    public void runRemoteCommand(String containerId, String command) throws IOException,
            ShellCommandErrorException {
        runCommand(containerId, command);
    }

    /**
     * Delete file from remote machine using machien's credentials.
     *
     * @param containerId container id to run command in
     * @param filePath of file to delete
     * @throws IOException
     * @throws ShellCommandErrorException
     */
    public void deleteFileFromRemoteMachine(String containerId, String filePath,
                                            boolean sudo) throws IOException,
            ShellCommandErrorException {
        String deleteCmd = "rm -rf " + filePath;
        if (sudo) {
            deleteCmd = "sudo -s " + deleteCmd;
        }
        runRemoteCommand(containerId, deleteCmd);
    }

    /**
     * Check if fileName exists on given path
     *
     * @param path to check if file exists
     * @param fileName to find
     * @return true if file exists in given path
     * @throws IOException
     * @throws ShellCommandErrorException
     */
    public boolean checkFileExists(String containerId, String path, String fileName)
            throws IOException, ShellCommandErrorException {
        // may get error on this command
        runCommand(containerId, "ls " + path + "/" + fileName, EXIT_CODE_NOT_EXISTS);
        // parse the result
        String result = getLastCmdResult().split("\r\n")[1];
        // if equals return true
        return (result.trim().equals(path + "/" + fileName));
    }


    public String getLastCmdResult() {
        return lastCmdResult;
    }

    public void setLastCmdResult(String lastCmdResult) {
        this.lastCmdResult = lastCmdResult;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setMasterHost(String masterHost) {
        this.masterHost = masterHost;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public int getLastCommandExitCode() {
        return lastCommandExitCode;
    }

    public long getCommandTimeout() {
        return commandTimeout;
    }

    public void setCommandTimeout(long commandTimeout) {
        this.commandTimeout = commandTimeout;
    }

    public boolean isIgnoreEnvVars() {
        return ignoreEnvVars;
    }

    public void setIgnoreEnvVars(boolean ignoreEnvVars) {
        this.ignoreEnvVars = ignoreEnvVars;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    @Nullable
    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(@Nullable String containerId) {
        this.containerId = containerId;
    }
}
