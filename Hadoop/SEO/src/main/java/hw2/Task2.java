import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.FileStatus;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URISyntaxException;
import java.net.URI;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;

public class Task2 extends Configured implements Tool {
	static final LongWritable one = new LongWritable(1);
  
  	public static class MyMapper extends Mapper<LongWritable, Text, QueryHost, LongWritable> {
		@Override
   	        protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			String[] query_url = value.toString().split("\t");
			String query = query_url[0].trim();
			URI uri;
			try {
				uri = new URI(query_url[1].trim());
			} catch(URISyntaxException e) {
				return;
			}
			String host = uri.getHost();
			if (host != null) {
			    if (host.startsWith("www.")) {
				host = host.substring(4);
			    }
			    context.write(new QueryHost(query, host), one);
			}
		}
 	 }

 	 public static class MyReducer extends Reducer<QueryHost, LongWritable, Text, LongWritable> {
		@Override
 	        protected void reduce(QueryHost current_host, Iterable<LongWritable> clicks, Context context) throws IOException, InterruptedException {
			long max_clicks = 0, current_clicks = 0;
			QueryHost res_host = new QueryHost(), start_host = new QueryHost();
			start_host.set(current_host);
			for (LongWritable click : clicks) {
				if (start_host.getQuery().equals(current_host.getQuery())) {
					current_clicks += 1;
				} else {
					if (current_clicks > max_clicks) {
						res_host.set(start_host);
						max_clicks = current_clicks;
					}
					start_host.set(current_host);
					current_clicks = 1;
				}
			}
			if (max_clicks >= context.getConfiguration().getLong("mapreduce.reduce.seo.minclicks", 1)) {
				context.write(new Text(res_host.toString()), new LongWritable(max_clicks));
			}
		}
  	}
  	
  	public static class MyPartitioner extends Partitioner<QueryHost, LongWritable> {
		@Override
		public int getPartition(QueryHost key, LongWritable val, int numPartitions) {
		    return Math.abs(key.getHost().hashCode()) % numPartitions;
		}
  	}

    	public static class MyGrouper extends WritableComparator {
        	protected MyGrouper() {
            		super(QueryHost.class, true);
       	 }
		 @Override
		 public int compare(WritableComparable one, WritableComparable two) {
		    Text first_host = ((QueryHost)one).getHost();
		    Text second_host = ((QueryHost)two).getHost();
		    return first_host.compareTo(second_host);
		 }
	}

   	private Job getJobConf(String inputDir, String outputDir) throws Exception {
		Configuration conf = getConf();
		conf.set(TextOutputFormat.SEPERATOR, "\t");
		Job job = Job.getInstance(getConf());
		job.setJarByClass(Task2.class);
		job.setMapperClass(MyMapper.class);
		job.setReducerClass(MyReducer.class);
		job.setPartitionerClass(MyPartitioner.class);
		job.setSortComparatorClass(QueryHost.KeyComparator.class);
		job.setGroupingComparatorClass(MyGrouper.class);
		job.setMapOutputKeyClass(QueryHost.class);
		job.setMapOutputValueClass(LongWritable.class);
		job.setInputFormatClass(TextInputFormat.class);
		job.setOutputFormatClass(TextOutputFormat.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(LongWritable.class);
		Path inputPath = new Path(inputDir);
		FileSystem fs = inputPath.getFileSystem(conf);
		for (FileStatus status : fs.listStatus(inputPath)) {
			Path folder = status.getPath();
			if (!fs.isDirectory(folder)) {
				continue;
			}
			for (FileStatus folderStatus : fs.listStatus(folder)) {
				Path path = folderStatus.getPath();
				if (path.getName().endsWith(".gz")) {
					TextInputFormat.addInputPath(job, path);
				}
			}
		}
    		TextOutputFormat.setOutputPath(job, new Path(outputDir));
		return job;
 	 }

	@Override
 	public int run(String[] args) throws Exception {
   		Job job = getJobConf(args[0], args[1]);
  		return job.waitForCompletion(true) ? 0 : 1;
	}

	static public void main(String[] args) throws Exception {
  		int ret = ToolRunner.run(new Task2(), args);
   		System.exit(ret);
	}
}
