package model

import com.amazonaws.services.ec2.AmazonEC2ClientBuilder
import com.amazonaws.services.ec2.model.*
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Wrapper Class for creating an EC2-Instance with the AWS Java SDK.
 */
class MyEC2(
    /**
     * The Name of the Security Group for the instance
     * Default: "franjo-sg"
     */
    private val sg: String = "franjo-sg",
    /**
     * The name of the KeyPair to access the instance through ssh.
     * Default: "PrivateKeyFranJo"
     */
    private val kp: String = "PrivateKeyFranJo",
    /**
     * The range of valid ip-addresses to access the instance.
     * Default: "0.0.0.0/0" (all IPs allowed)
     */
    private val ipRange: String = "0.0.0.0/0",
    key: String = "DHGE",
    value: String = "FranJo.PI20"
) {
    /**
     * Creates the AWS EC2 Client with the default profile.
     */
    private val client = AmazonEC2ClientBuilder.defaultClient()
    /**
     * The tag, which will be attached to the instance. This is convenient
     * when working with multiple users or instances.
     * @see createTag
     * @see deleteTag
     */
    private val tag = Tag(key, value)
    /**
     * The ID of the created instance. This is a lateinit property,
     * so access to this variable is only possible after the creation of the instance.
     * @see runInstance
     */
    private lateinit var instance: String
    /**
     * The IP-Adress of the created instance.This is a lateinit property,
     * so access to this variable is only possible after the instance has the state "running"
     * @see fetchIpAddressFromAWS
     * @see getInstanceState
     */
    private lateinit var ipAddress: String

    init {
        createInstance()
    }

    private fun createInstance(){
        createSecurityGroup()
        createIpPermissions()
        createKeyPair()
        runInstance()
        fetchIpAddressFromAWS()
        createTag()
    }

    fun deleteInstance(){
        deleteTag()
        terminateInstance()
        deleteKeyPair()
        waitUntilInstanceDown()
        deleteSecurityGroup()
    }

    /**
     * Every EC2-Instance needs a Security Group.
     * A Security Group controls the access to the attached instance(s).
     * Additionally, one has to create IP-Permissions for the corresponding Security Group.
     * @see createIpPermissions
     */
    private fun createSecurityGroup(){
        val createSecurityGroupRequest = CreateSecurityGroupRequest()
            .withGroupName(sg)
            .withDescription("FranJo.PI20 Security Group")
        try {
            val result = client.createSecurityGroup(createSecurityGroupRequest)
            println("Created Security Group with Name ${createSecurityGroupRequest.groupName} and ID ${result.groupId}!")
        } catch (e: Exception) {
            deleteInstance()
            error(e)
        }
    }

    /**
     * Defines the valid protocol, port & ip-range for any access to the instances of the security group.
     * Attaches the IP-Permission to the existing Security Group
     */
    private fun createIpPermissions(){
        val ipRange1 = IpRange()
            .withCidrIp(ipRange)
        val ipPermission = IpPermission()
            .withIpv4Ranges(listOf(ipRange1))
            .withIpProtocol("tcp")
            .withFromPort(22)
            .withToPort(22)
        val authorizeSecurityGroupIngressRequest = AuthorizeSecurityGroupIngressRequest()
        authorizeSecurityGroupIngressRequest
            .withGroupName(sg)
            .withIpPermissions(ipPermission)
        try {
            client.authorizeSecurityGroupIngress(authorizeSecurityGroupIngressRequest)
            println("Authorized IP-Addresses! (Range: ${ipRange1.cidrIp}, Protocol: ${ipPermission.ipProtocol}, Port: ${ipPermission.fromPort})")
        } catch (e: Exception) {
            deleteInstance()
            error(e)
        }
    }

    /**
     * Creates the KeyPair needed for access through SSH.
     * Generates a private key and saves the key in the "key.pem" file.
     */
    private fun createKeyPair() {
        val createKeyPairRequest = CreateKeyPairRequest()
            .withKeyName("PrivateKeyFranJo")
        try {
            val createKeyPairResult = client.createKeyPair(createKeyPairRequest)
            val keyPair = createKeyPairResult.keyPair
            File("./resources/key.pem").bufferedWriter().use {
                it.write(keyPair.keyMaterial)
            }
            Runtime.getRuntime().exec("chmod 400 ./resources/key.pem")
            println("Created Key-Pair ${keyPair.keyName}!")
        } catch (e: AmazonEC2Exception) {
            deleteInstance()
            error(e)
        }
    }

    /**
     * Creates the new EC2-Instance.
     * The Image ID specifies the operating system used in the virtual machine.
     * The InstanceType specifies the hardware components used in the virtual machine.
     * Attaches the KeyPair and Security Group to the EC2-Instance.
     * After the request has been sent, AWS creates the Instance.
     * Because SSH needs an IP-Address to connect, the method waits till the Instance has reached the state "running"
     * @see createSecurityGroup
     * @see createIpPermissions
     * @see createKeyPair
     * @see getInstanceState
     */
    private fun runInstance() {
        val runInstancesRequest = RunInstancesRequest()
            .withImageId("ami-05ff5eaef6149df49")
            .withInstanceType(InstanceType.T2Micro)
            .withMinCount(1)
            .withMaxCount(1)
            .withKeyName(kp)
            .withSecurityGroups(sg)
        try {
            instance = client.runInstances(runInstancesRequest).reservation.instances.first().instanceId
            println("Created EC2-Instance $instance!")
            print("Waiting for the Instance to be running!")
            do {
                val state = getInstanceState()
                print(".")
                TimeUnit.SECONDS.sleep(2)
            } while (state.code != 16)
            print("\n")
        } catch (e: Exception) {
            deleteInstance()
            error(e)
        }
    }

    /**
     * Help-Method to retrieve the Instance State.
     * The initial state is "pending", which will switch to "running" after a few seconds.
     */
    private fun getInstanceState(): InstanceState {
        val describeInstanceStatusRequest = DescribeInstanceStatusRequest()
        describeInstanceStatusRequest.includeAllInstances = true
        describeInstanceStatusRequest.setInstanceIds(listOf(instance))
        try {
            return client.describeInstanceStatus(describeInstanceStatusRequest).instanceStatuses.first().instanceState
        } catch (e: Exception) {
            deleteInstance()
            error(e)
        }
    }

    /**
     * Fetches the IP-Address once the Instance is running.
     * The Address is required to establish an SSH Connection with the instance.
     */
    private fun fetchIpAddressFromAWS(){
        val describeInstancesRequest = DescribeInstancesRequest()
            .withInstanceIds(instance)
        try {
            val describeInstancesResult = client.describeInstances(describeInstancesRequest)
            ipAddress = describeInstancesResult.reservations.first().instances.first().publicIpAddress
            println("IP-Address is $ipAddress")
        } catch (e: Exception) {
            deleteInstance()
            error(e)
        }
    }

    /**
     * Attaches the created tag to the running instance. Can be useful when working with
     * multiple users or instances
     */
    private fun createTag() {
        val createTagsRequest = CreateTagsRequest()
            .withTags(tag)
            .withResources(listOf(instance))
        try {
            client.createTags(createTagsRequest)
            println("Created Tag $tag!")
        } catch (e: Exception) {
            deleteInstance()
            error(e)
        }
    }

    /**
     * Deletes the tag, which was attached to the instance.
     */
    private fun deleteTag() {
        val deleteTagsRequest = DeleteTagsRequest()
            .withTags(tag)
            .withResources(listOf(instance))
        client.deleteTags(deleteTagsRequest)
        println("Deleted Tag $tag!")
    }

    /**
     * Deletes the KeyPair for the instance.
     */
    private fun deleteKeyPair(){
        val deleteKeyPairRequest = DeleteKeyPairRequest()
            .withKeyName(kp)
        client.deleteKeyPair(deleteKeyPairRequest)
        Runtime.getRuntime().exec("rm -f ./resources/key.pem")
        println("Deleted KeyPair with Name $kp!")
    }

    /**
     * Wait till the instance is shut down.
     * This is necessary, because it's only possible to delete the Security Group
     * once all attaches instances are shut down.
     */
    private fun waitUntilInstanceDown(){
        print("Waiting until Instance has been shut down!")
        do {
            val state = getInstanceState()
            print(".")
            TimeUnit.SECONDS.sleep(2)
        } while (state.code != 48)
        print("\n")
    }

    /**
     * Deletes the Security Group
     */
    private fun deleteSecurityGroup() {
        val deleteSecurityGroupRequest = DeleteSecurityGroupRequest()
            .withGroupName(sg)
        client.deleteSecurityGroup(deleteSecurityGroupRequest)
        println("Deleted Security Group with Name ${deleteSecurityGroupRequest.groupName}!")
    }

    /**
     * Terminates the EC2-Instance
     */
    private fun terminateInstance() {
        val terminateInstancesRequest = TerminateInstancesRequest()
            .withInstanceIds(instance)
        client.terminateInstances(terminateInstancesRequest)
        println("Terminated EC2-Instance $instance!")
    }

    /**
     * Public access to the IP-Address.
     * Needed to establish the SSH Connection later.
     */
    fun getIpAddress(): String{
        return this.ipAddress
    }
}
