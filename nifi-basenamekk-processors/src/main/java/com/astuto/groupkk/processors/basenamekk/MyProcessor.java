/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.astuto.groupkk.processors.basenamekk;

import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.annotation.behavior.ReadsAttribute;
import org.apache.nifi.annotation.behavior.ReadsAttributes;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.util.StandardValidators;

import org.apache.nifi.processors.aws.credentials.provider.service.AWSCredentialsProviderService;


import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.Volume;
import software.amazon.awssdk.regions.Region;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Tags({"example", "astuto"})
@CapabilityDescription("A custom processor to experiment with the controllers. How to access that. ")
@SeeAlso({})
@ReadsAttributes({@ReadsAttribute(attribute="", description="")})
@WritesAttributes({@WritesAttribute(attribute="", description="")})
public class MyProcessor extends AbstractProcessor {

    public static final PropertyDescriptor AWS_CREDENTIALS_PROVIDER = new PropertyDescriptor.Builder()
            .name("AWS Credentials Provider")
            .description("Controller Service that provides AWS credentials")
            .required(true)
            .identifiesControllerService(AWSCredentialsProviderService.class)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final Relationship SUCCESS = new Relationship.Builder()
            .name("SUCCESS")
            .description("Example relationship")
            .build();

    private List<PropertyDescriptor> descriptors;

    private Set<Relationship> relationships;

    @Override
    protected void init(final ProcessorInitializationContext context) {
        descriptors = new ArrayList<>();
        descriptors.add(AWS_CREDENTIALS_PROVIDER);
        descriptors = Collections.unmodifiableList(descriptors);

        relationships = new HashSet<>();
        relationships.add(SUCCESS);
        relationships = Collections.unmodifiableSet(relationships);
    }

    @Override
    public Set<Relationship> getRelationships() {
        return this.relationships;
    }

    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return descriptors;
    }

    @OnScheduled
    public void onScheduled(final ProcessContext context) {



    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) {
        FlowFile flowFile = session.get();
        if (flowFile == null) {
            return;
        }
        // TODO implement
        // Retrieve the AWS credentials provider controller service
        AWSCredentialsProviderService credentialsService = context.getProperty(AWS_CREDENTIALS_PROVIDER)
                .asControllerService(AWSCredentialsProviderService.class);

        software.amazon.awssdk.auth.credentials.AwsCredentialsProvider credentialsProvider = credentialsService.getAwsCredentialsProvider();

        Ec2Client ec2Client = Ec2Client
                .builder()
                .credentialsProvider(credentialsProvider)
                .region(Region.AP_SOUTH_1)
                .build();

        DescribeVolumesRequest request = DescribeVolumesRequest.builder().build();
        DescribeVolumesResponse response = ec2Client.describeVolumes(request);

        List<Volume> volumes = response.volumes();
        HashSet<String> volumeIds = new HashSet<>();

        for (Volume v:
             volumes) {
            volumeIds.add(v.volumeId());
        }

        String res = String.join(",", volumeIds);

        flowFile = session.write(flowFile, out -> {
            out.write(res.getBytes(StandardCharsets.UTF_8));
        });

        session.transfer(flowFile, SUCCESS);
    }
}
