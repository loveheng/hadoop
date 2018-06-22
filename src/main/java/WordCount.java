/**
 * Created by zzh on 2018-5-23.
 */
import java.io.IOException;

import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;

import org.apache.hadoop.fs.Path;

import org.apache.hadoop.io.IntWritable;

import org.apache.hadoop.io.Text;

import org.apache.hadoop.mapreduce.Job;

import org.apache.hadoop.mapreduce.Mapper;

import org.apache.hadoop.mapreduce.Reducer;

import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;

import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import org.apache.hadoop.util.GenericOptionsParser;
public class WordCount {

    public static class TokenizerMapper
            extends Mapper<Object, Text, Text, IntWritable> {  //4个类型分别是：输入key类型、输入value类型、输出key类型、输出value类型

        //MapReduce框架读到一行数据侯以key value形式传进来，
        // key默认情况下是mr框架所读到一行文本的起始偏移量（Long类型），value默认情况下是mr框架所读到的一行的数据内容（String类型）

        //这里的数据类型和我们常用的不一样，因为MapReduce程序的输出数据需要在不同机器间传输，所以必须是可序列化的，
        // 例如Long类型，Hadoop中定义了自己的可序列化类型LongWritable，String对应的是Text，int对应的是IntWritable。
        private final static IntWritable one = new IntWritable(1);
        private Text word = new Text();

        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            StringTokenizer itr = new StringTokenizer(value.toString());
            while (itr.hasMoreTokens()) {
                word.set(itr.nextToken());
                context.write(word, one);
            }
        }
    }

    //Reducer 4个类型分别指:输入key的类型、输入value的类型、输出key的类型、输出value的类型
    //这里需要注意的是,reduce方法接受的是:一个字符串类型的key、一个可迭代的数据集,因为reduce任务读取到map
    //任务处理结果是这样的:（good，1）（good，1）（good，1）（good，1）
    //当传给reduce方法时，就变为：
    //key：good
    //value：（1,1,1,1）
    public static class IntSumReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
        private IntWritable result = new IntWritable();

        public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
            int sum = 0;
            for (IntWritable val : values) {
                sum += val.get();
            }
            result.set(sum);
            context.write(key, result);
        }
    }

    public static void main(String[] args) throws Exception {
        //创建配置对象
        System.setProperty("HADOOP_USER_NAME", "hdfs");
        Configuration conf = new Configuration();
        conf.set("mapreduce.app-submission.cross-platform", "true");
        conf.set("mapreduce.framework.name", "yarn");
        conf.set("mapreduce.job.jar","E:\\idea\\hadoop\\target\\hadoop-1.0-SNAPSHOT-jar-with-dependencies.jar");
        String[] ioArgs = new String[] { "hdfs://sy-002.hadoop:8020/zzh/aa.txt", "hdfs://sy-002.hadoop:8020/zzh/out" };
        String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
        if (otherArgs.length < 2) {
            System.err.println("Usage: wordcount <in> [<in>...] <out>");
            System.exit(2);
        }
        //创建job对象
        Job job = Job.getInstance(conf, "word count");
        //设置运行job的类
        job.setJarByClass(WordCount.class);

        //设置map、combine、reduce处理类
        job.setMapperClass(TokenizerMapper.class);
        job.setCombinerClass(IntSumReducer.class);
        job.setReducerClass(IntSumReducer.class);

        //设置输出类型
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        //设置输入输出目录
        for (int i = 0; i < otherArgs.length - 1; ++i) {
            FileInputFormat.addInputPath(job, new Path(otherArgs[i]));
        }
        FileOutputFormat.setOutputPath(job,
                new Path(otherArgs[otherArgs.length - 1]));
        //提交job
        boolean isSuccess = job.waitForCompletion(true);
        System.exit(isSuccess ? 0 : 1);
    }

}
