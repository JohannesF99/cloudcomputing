package de.johannes.cloudcomputing.model

import com.jcraft.jsch.ChannelShell
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.io.PrintStream
import java.util.concurrent.TimeUnit

/**
 * The SSH Wrapper class, used to connect to the previously established EC2-Instance.
 * @param ipAddress The IP-Address of the EC2-Instance
 */
class SSH(private val ipAddress: String) {
    /**
     * The SSH session. Used to create new channels to communicate with the EC2 Instance.
     */
    private lateinit var session: Session
    /**
     * The SSH Channel. You can query bash commands and execute them.
     */
    private lateinit var channel: ChannelShell

    init {
        copyAwsCredsToEc2Instance()
        connectToEC2()
        installAWSCLI()
    }

    /**
     * In order to access the S3 bucket, the EC2-Instance needs valid AWS Credentials.
     * One way to do this is through the IAM Manager.
     * In this solution, we copy the folder with our AWS Credentials and send it to the EC2-Instance using scp.
     * Important to note: Directly after instantiating, the EC2-Instance refuses connection with SSH.
     * Therefor we wait 10s to make sure the EC2-Instance allows SSH connection and the scp does work.
     */
    private fun copyAwsCredsToEc2Instance(){
        TimeUnit.SECONDS.sleep(10)
        val rt = Runtime.getRuntime()
        val proc = rt.exec(
            "scp -o StrictHostKeyChecking=no -i ./resources/key.pem -r ./resources/.aws ec2-user@$ipAddress:/home/ec2-user/"
        )

        val stdInput = BufferedReader(InputStreamReader(proc.inputStream))

        val stdError = BufferedReader(InputStreamReader(proc.errorStream))

        println("Here is the standard output of the command:\n")
        var s: String?
        while (stdInput.readLine().also { s = it } != null) {
            println(s)
        }

        println("Here is the standard error of the command (if any):\n")
        while (stdError.readLine().also { s = it } != null) {
            println(s)
        }
    }

    /**
     * This Method establishes an SSH Connection using [JSch].
     * Instead of a password, AWS prefers using a private key, which will be created alongside the EC2-Instance in [MyEC2].
     * We also need to set the parameter "StrictHostKeyChecking" to false,
     * because ssh will ask to add the ip-address to the know hosts when connecting to the EC2-Instance for the first time.
     * Finally, we create a new channel with ChannelType "Shell" to send out bash commands to the EC2-Instance.
     */
    private fun connectToEC2(){
        val client = JSch()
        client.addIdentity("./resources/key.pem")
        session = client.getSession("ec2-user", ipAddress, 22)
        session.setConfig("StrictHostKeyChecking", "no")
        session.connect(10000)
        channel = session.openChannel("shell") as ChannelShell
        channel.connect()
        println("SSH-Connection to EC2-Instance established!")
    }

    /**
     * To use AWS from the EC2-Instance, will need to install the AWS CLI.
     * These Commands download the AWS CLI archive, extracts it & installs it.
     */
    private fun installAWSCLI(){
        runCommand("curl \"https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip\" -o \"awscliv2.zip\"")
        runCommand("unzip awscliv2.zip")
        runCommand("sudo ./aws/install")
    }

    /**
     * Helper Function. Contains the logic to run bash commands on the EC2-Instance.
     * @param command the bash command to run on the EC2-Instance
     * @return Returns the SSH class, so these calls can be chained together
     */
    fun runCommand(command: String): SSH {
        val outputStream = ByteArrayOutputStream()
        channel.outputStream = outputStream
        val stream = PrintStream(channel.outputStream)
        stream.println(command)
        stream.flush()
        waitForPrompt(outputStream)
        return this
    }

    /**
     * Helper Function. Prints the output of the commands which did run on the EC2-Instance.
     * @param outputStream the output stream which returned from the EC2-Instance
     */
    private fun waitForPrompt(outputStream: ByteArrayOutputStream) {
        val retries = 5
        for (x in 1 until retries) {
            Thread.sleep(1000)
            if (outputStream.toString().indexOf("$") > 0) {
                print(outputStream.toString())
                outputStream.reset()
                println()
                return
            }
        }
    }

    /**
     * Disconnects the Channel and Session. Should be called, when the ssh connection can be terminated.
     */
    fun disconnect(){
        channel.disconnect()
        session.disconnect()
    }
}
