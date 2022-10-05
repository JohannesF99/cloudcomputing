package model

import com.amazonaws.services.ec2.model.Tag
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.*
import java.io.File

/**
 * Wrapper Class for creating an S3-Bucket with the AWS Java SDK.
 */
class MyS3(
    /**
     * The File, which will be uploaded to the S3 Bucket as an S3 Object.
     * Contains the Key (Name) and Path to the File.
     * @see uploadObject
     */
    s3Object: S3ObjectBuilder.S3Object,
    /**
     * The unique Name of the S3 Bucket.
     */
    private val bucketName: String = "bucket-franjo",
    tagKey: String = "DHGE",
    tagValue: String = "FranJo.PI20"
) {
    /**
     * Creates the AWS S3 Client with the default profile.
     */
    private val client = AmazonS3ClientBuilder.defaultClient()
    /**
     * The tag, which will be attached to the instance. This is convenient
     * when working with multiple users or instances.
     * @see createTag
     */
    private val tag = Tag(tagKey, tagValue)

    init {
        createBucket()
        createTag()
        uploadObject(s3Object.component1(), s3Object.component2())
    }

    /**
     * Sends a request to AWS to create the S3 bucket.
     * (Only if currently no other bucket with this name exists)
     * @see bucketName
     */
    private fun createBucket(){
        if (!client.doesBucketExistV2(bucketName)) {
            val createBucketRequest = CreateBucketRequest(bucketName, Region.EU_Frankfurt)
            client.createBucket(createBucketRequest)
            println("Created Bucket $bucketName!")
        } else {
            println("Bucket with Name $bucketName does already exist!")
        }
    }

    /**
     * If a bucket with matching name exists, this method will list all object stored in this bucket.
     * Because a bucket can only be deleted if he's empty, this method will then delete every item in the bucket.
     * Once every item is deleted, the bucket itself can be removed.
     */
    fun deleteBucket(){
        if (client.doesBucketExistV2(bucketName)) {
            //Before the Bucket can be deleted, every Object has to be removed first!
            client.listObjects(bucketName).objectSummaries.forEach {
                client.deleteObject(bucketName, it.key)
                println("Deleted Object ${it.key} in Bucket $bucketName!")
            }
            client.deleteBucket(bucketName)
            println("Deleted Bucket $bucketName!")
        }
    }

    /**
     * Creates the Tag and attaches it to the S3 Bucket.
     * This can be useful, when working with multiple buckets or users.
     * @see tag
     */
    private fun createTag() {
        val bucketTaggingConfiguration = BucketTaggingConfiguration()
            .withTagSets(TagSet(mapOf(Pair(tag.key, tag.value))))
        val setBucketTaggingConfigurationRequest = SetBucketTaggingConfigurationRequest(bucketName, bucketTaggingConfiguration)
        try {
            client.setBucketTaggingConfiguration(setBucketTaggingConfigurationRequest)
            println("Created Tag $tag for Bucket $bucketName!")
        } catch (e: Exception) {
            error(e)
        }
    }

    /**
     * Uploads the file to the bucket. Requires the path to the file and a key, which will be used as a reference
     * for this object inside the bucket.
     * @see S3ObjectBuilder.S3Object
     */
    private fun uploadObject(key: String, file: File){
        try {
            client.putObject(bucketName, key, file)
            println("Uploaded File(${file.absoluteFile}) to Bucket $bucketName!")
        } catch (e: Exception) {
            error(e)
        }
    }

    /**
     * Downloads every object in the bucket except the initial program, which was uploaded prior.
     * Fetches the key of every object and calls the helper function to download this specific object.
     * @see downloadObject
     */
    fun downloadAllObjects(programKey: String, path: String): MyS3 {
        try {
            client.listObjects(bucketName)
                .objectSummaries
                .filter { it.key != programKey }
                .forEach {
                    downloadObject(it.key, path)
                }
        } catch (e: Exception) {
            error(e)
        }
        return this
    }

    /**
     * Downloads an object in the s3 bucket. Creates a File with the given path and S3 key
     * and stores it onto the system.
     * @see downloadAllObjects
     */
    private fun downloadObject(key: String, path: String) {
        val file = File("$path/$key")
        client.getObject(GetObjectRequest(bucketName, key), file)
        println("Download File(${file.absoluteFile}) from Bucket $bucketName!")
    }
}
