package de.johannes.cloudcomputing

import de.johannes.cloudcomputing.model.MyEC2
import de.johannes.cloudcomputing.model.MyS3
import de.johannes.cloudcomputing.model.S3ObjectBuilder
import de.johannes.cloudcomputing.model.SSH
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class CloudComputingApplication : CommandLineRunner {
	override fun run(vararg args: String) {
		//Forces the program to get called with only one parameter
		if (args.size != 1) {
			throw IllegalArgumentException("path to program-file required")
		}
		//Creates the file object with the given filepath
		val s3object = S3ObjectBuilder()
			.withPath(args.first())
			.build()
		//Creates an EC2 Instance and S3 Bucket
		val ec2 = MyEC2()
		val s3 = MyS3(s3object)
		//Connects to the EC2 Instance through SSH and runs all given commands
		SSH(ec2.getIpAddress())
			.runCommand("mkdir cloud-computing")
			.runCommand("aws s3 sync s3://bucket-franjo ./cloud-computing")
			.runCommand("cd cloud-computing")
			.runCommand("sudo chmod +x ${s3object.key}")
			.runCommand("./${s3object.key}")
			.runCommand("aws s3 cp ../cloud-computing s3://bucket-franjo --recursive --exclude \"${s3object.key}\"")
			.disconnect()
		//Downloads all created objects in the S3 buckets and deletes the bucket in the end
		s3.downloadAllObjects(s3object.key, s3object.file.parent)
			.deleteBucket()
		//Deletes the EC2 Instance
		ec2.deleteInstance()
	}

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			SpringApplication.run(CloudComputingApplication::class.java, *args)
		}
	}
}
