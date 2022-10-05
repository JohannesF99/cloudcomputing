package model

import model.S3ObjectBuilder.S3Object
import java.io.File
import java.io.FileNotFoundException

/**
 * Builder Pattern to create an S3Object Instance.
 * @see S3Object
 */
class S3ObjectBuilder {
    /**
     * File containing the system path to the object
     */
    private var file = File("")
    /**
     * Reference Name for the object in the s3 bucket
     */
    private var key = ""
    /**
     * Data class storing the key and file.
     * @see key
     * @see file
     */
    data class S3Object(val key: String, val file: File)

    /**
     * Created the File and Key for the S3Object.
     * Checks whether the file exists or not.
     * @throws FileNotFoundException when file does not exist
     * @return The S3ObjectBuilder-Instance, so method calls can be chained together
     */
    fun withPath(path: String): S3ObjectBuilder {
        file = File(path)
        if (!file.exists()) {
            throw FileNotFoundException("File at given path was not found!")
        }
        key = file.name
        return this
    }

    /**
     * Creates an S3Object and returns the instance.
     * @return The S3Object
     */
    fun build(): S3Object {
        return S3Object(key, file)
    }
}
