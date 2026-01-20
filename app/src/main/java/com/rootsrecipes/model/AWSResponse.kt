package com.rootsrecipes.model

data class AWSResponse(
    val `data`: AwsData, val message: String, val statusCode: Int
)

data class AwsData(
    val access_key: String,
    val aws_bucket_name: String,
    val aws_region: String,
    val secret_key: String
)