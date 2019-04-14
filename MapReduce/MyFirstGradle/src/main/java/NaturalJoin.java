import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Reducer.Context;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
//学生学号、姓名、选修课程编号、考试成绩。
public class NaturalJoin {
	public static final String LEFT_FILENAME="student.csv";//SID	NAME	SEX	AGE	BIRTHDAY	DNAME	CLASS
    public static final String RIGHT_FILENAME="student_course.csv";//SID	CID	SCORE	TID
    public static final String LEFT_FILENAME_FLAG="l";
    public static final String RIGHT_FILENAME_FLAG="r";
    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
    	
        Configuration conf=new Configuration();
        Job job=new Job(conf, "Join");
        job.setJarByClass(NaturalJoin.class);
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        job.setMapperClass(MyMapper.class);
        job.setReducerClass(MyReducer.class);
        job.setOutputFormatClass(TextOutputFormat.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        System.out.println(job.waitForCompletion(true)?0:1);
    }

    static class MyMapper extends Mapper<LongWritable, Text, Text, Text>{
	    protected void map(LongWritable key, Text value, Context context) throws java.io.IOException ,InterruptedException {
			String filePath=((FileSplit)context.getInputSplit()).getPath().toString();
			String fileFlag=null;
			String joinKey=null;
			String joinValue=null;
			if(filePath.contains(LEFT_FILENAME)){
			    fileFlag=LEFT_FILENAME_FLAG;
			    joinKey=value.toString().split(",")[0];
			    joinValue=value.toString().split(",")[1];
			}else if(filePath.contains(RIGHT_FILENAME)){
			    fileFlag=RIGHT_FILENAME_FLAG;
			    joinKey=value.toString().split(",")[0];
			    joinValue=value.toString().split(",")[1]+"\t"+value.toString().split(",")[2];
			}
			context.write(new Text(joinKey), new Text(joinValue+"%"+fileFlag));
	    };
    }
    static class MyReducer extends Reducer<Text, Text, Text, Text>{
        protected void reduce(Text key, Iterable<Text> values, Context context) throws java.io.IOException ,InterruptedException {
            Iterator<Text> iterator=values.iterator();
            List<String> student=new ArrayList<String>();
            String studentName="";
            while(iterator.hasNext()){
                String[] infos=iterator.next().toString().split("%");
                if(infos[1].equals(LEFT_FILENAME_FLAG)){
                    studentName=infos[0];
                }else if(infos[1].equals(RIGHT_FILENAME_FLAG)){
                    student.add(infos[0]);
                }
            }
            if(!studentName.equals("")){
	            for(int i=0;i<student.size();i++){
	                context.write(new Text(key),new Text(studentName+"\t"+student.get(i)));
	            }
            }
        };
    }
}