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
import org.apache.nifi.components.state.Scope;
import org.apache.nifi.components.state.StateMap;
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Tags({"example", "some","tags","nifi","custom","processor"})
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

    public static final PropertyDescriptor USER_INPUT = new PropertyDescriptor
            .Builder().name("USER_INPUT")
            .displayName("User Input")
            .description("User Input")
            .required(true)
//            .sensitive(true) // this will make sure you can mark the property as sensitive.
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

        // Add your descriptors here.
        descriptors.add(AWS_CREDENTIALS_PROVIDER);
        descriptors.add(USER_INPUT);
        descriptors = Collections.unmodifiableList(descriptors);

        relationships = new HashSet<>();

        // Add your relations here.
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
        // You can write a code which will be invoked based on the frequency.
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) {
        FlowFile flowFile = session.get();
        if (flowFile == null) {
            return;
        }

        // You can read params here based on your need. Like this.
        try {
            StateMap stateMap = context.getStateManager().getState(Scope.CLUSTER);
            String lastUpdatedOn = stateMap.get("name");
            String current = stateMap.get("current");

            getLogger().debug("Found last logger data " + lastUpdatedOn + " current "+ current);
        } catch (IOException e) {
            getLogger().error("Error while getting state "+ e.getMessage());
        }

        Map<String, String> state = new HashMap<>();
        state.put("name","kaushik");
        state.put("current", Instant.now().toString());

        try {
            context.getStateManager().setState(state, Scope.CLUSTER);
        } catch (IOException e) {

        }


        // You can read the specified params here like this
        String userInput = context.getProperty("USER_INPUT").getValue();

        // To log anything use this logger
        // Do not forget to add this in logger file. Otherwise, you won't see logs.
        getLogger().debug("Some debug log here for the process. " + userInput);

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

        for (Volume v:
             volumes) {

            // Create a new session
            FlowFile newFlowFile = session.create();

            // Write to the session
            newFlowFile = session.write(newFlowFile, out -> {
                out.write(v.volumeId().getBytes(StandardCharsets.UTF_8));
            });

            // Send the session.
            session.transfer(newFlowFile, SUCCESS);
        }

        // You can send original in main flow file in case you need it.
        // You can define other relations here for your use cases.
        session.transfer(flowFile, SUCCESS);
    }
}
