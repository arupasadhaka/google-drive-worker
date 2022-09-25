package com.oslash.integration;


import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import java.util.HashMap;
import java.util.Map;

//@Configuration
//@EnableBatchProcessing
public class Example {
//
//    @Autowired
//    private JobBuilderFactory jobs;
//
//    @Autowired
//    private StepBuilderFactory steps;
//
//    public static void main(String[] args) throws Exception {
//        ApplicationContext context = new AnnotationConfigApplicationContext(Example.class);
//        JobLauncher jobLauncher = context.getBean(JobLauncher.class);
//        Job job = context.getBean(Job.class);
//        jobLauncher.run(job, new JobParameters());
//    }
//
//    @Bean
//    public Step step1() {
//        return steps.get("step1")
//                .tasklet((contribution, chunkContext) -> {
//                    System.out.println(Thread.currentThread().getName() + ": step1");
//                    return RepeatStatus.FINISHED;
//                })
//                .build();
//    }
//
//    @Bean
//    public Flow step2() {
//        Flow subflow1 = new FlowBuilder<Flow>("step21_master").from(step21_master()).end();
//        Flow subflow2 = new FlowBuilder<Flow>("step22_master").from(step22_master()).end();
//        Flow subflow3 = new FlowBuilder<Flow>("step23_master").from(step23_master()).end();
//
//        return new FlowBuilder<Flow>("splitflow").split(taskExecutor())
//                .add(subflow1, subflow2, subflow3).build();
//    }
//
//    @Bean
//    public Step step21_master() {
//        return steps.get("step21_master")
//                .partitioner("workerStep", partitioner("step21_master"))
//                .step(workerStep())
//                .gridSize(3)
//                .taskExecutor(taskExecutor())
//                .build();
//    }
//
//    @Bean
//    public Step step22_master() {
//        return steps.get("step22_master")
//                .partitioner("workerStep", partitioner("step22_master"))
//                .step(workerStep())
//                .gridSize(3)
//                .taskExecutor(taskExecutor())
//                .build();
//    }
//
//    @Bean
//    public Step step23_master() {
//        return steps.get("step23_master")
//                .partitioner("workerStep", partitioner("step23_master"))
//                .step(workerStep())
//                .gridSize(3)
//                .taskExecutor(taskExecutor())
//                .build();
//    }
//
//    @Bean
//    public Step step3() {
//        return steps.get("step3")
//                .tasklet((contribution, chunkContext) -> {
//                    System.out.println(Thread.currentThread().getName() + ": step3");
//                    return RepeatStatus.FINISHED;
//                })
//                .build();
//    }
//
//    @Bean
//    public Step workerStep() {
//        return steps.get("workerStep")
//                .tasklet(getTasklet(null))
//                .build();
//    }
//
//    @Bean
//    @StepScope
//    public Tasklet getTasklet(@Value("#{stepExecutionContext['data']}") String partitionData) {
//        return (contribution, chunkContext) -> {
//            System.out.println(Thread.currentThread().getName() + " processing partitionData = " + partitionData);
//            return RepeatStatus.FINISHED;
//        };
//    }
//
//    @Bean
//    public Job job() {
//        return jobs.get("job")
//                .flow(step1())
//                .on("*").to(step2())
//                .next(step3())
//                .build()
//                .build();
//    }
//
//    @Bean
//    public SimpleAsyncTaskExecutor taskExecutor() {
//        return new SimpleAsyncTaskExecutor();
//    }
//
//    public Partitioner partitioner(String stepName) {
//        return gridSize -> {
//            Map<String, ExecutionContext> map = new HashMap<>(gridSize);
//            for (int i = 0; i < gridSize; i++) {
//                ExecutionContext executionContext = new ExecutionContext();
//                executionContext.put("data", stepName + ":data" + i);
//                String key = stepName + ":partition" + i;
//                map.put(key, executionContext);
//            }
//            return map;
//        };
//    }

}