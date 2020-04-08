package experiments;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PopularityCalc {
	
	private static double[] soAvg = {4.597, 1.94771};
	// Questions: {2.04319, 1.52387, 2.00327, 0.61496, 2282.491}
	// Accepted Answers: {4.597, 1.94771}
	private static int soIndex = 0;
	
	private static String[] columns = {"Id,Score,AnswerCount,CommentCount,FavoriteCount,ViewCount", "Id,Score,CommentCount"};
	private static HashMap<String, String> columnMap = new HashMap<String, String>();
	private static Set<String> ids = new HashSet<String>();
	
	private static BufferedReader br;
	private static BufferedWriter bw;
	
	private static int batchSize = 10000;
	
	private static Map<String, String> groupContent(List<String> posts, Map<String, String> postContent){
		String[] postInfo = {};
		String postId = "";
		String content = "";
		Set<String> uniqContent = new HashSet<String>();
		
		for(String post : posts) {
			if(post.contains("Id"))
				continue;
			
			postInfo = post.split(",");
			postId = postInfo[0];
			content = postInfo.length > 1 ? postInfo[1] : "";
			
			if(!postContent.containsKey(postId))
				postContent.put(postId, content);
			else {
				Collections.addAll(uniqContent, content.split(";"));
				Collections.addAll(uniqContent, postContent.get(postId).split(";"));
				uniqContent.remove(""); // Remove empty string so doesn't affect the string join
				postContent.put(postId, String.join(";", uniqContent));
				uniqContent.clear();
			}
		}
		
		return postContent;
	}
	
	public static void combineInfo() {
		try {
			bw = new BufferedWriter(new FileWriter("data\\info\\annotation_question_combined_set_9-23.csv"));
			List<String> posts = Files.readAllLines(new File("data\\sample\\annotation_question_ids.csv").toPath());
			
			List<String> refTitle = Files.readAllLines(new File("data\\info\\refactor_methods\\so_post_refactor_questions_title_refactor_method_set_9-23.csv").toPath());
			List<String> refBody = Files.readAllLines(new File("data\\info\\refactor_methods\\so_post_refactor_questions_body_refactor_method_set_9-23.csv").toPath());
			List<String> refTags = Files.readAllLines(new File("data\\info\\refactor_methods\\so_post_refactor_questions_tags_refactor_method_set_9-23.csv").toPath());
			
			List<String> langTitle = Files.readAllLines(new File("data\\info\\language\\so_post_refactor_questions_title_language_set_9-23.csv").toPath());
			List<String> langBody = Files.readAllLines(new File("data\\info\\language\\so_post_refactor_questions_body_language_set_9-23.csv").toPath());
			List<String> langTags = Files.readAllLines(new File("data\\info\\language\\so_post_refactor_questions_tags_language_set_9-23.csv").toPath());
			
			List<String> ideTitle = Files.readAllLines(new File("data\\info\\ide\\so_post_refactor_questions_title_ide_set_9-23.csv").toPath());
			List<String> ideBody = Files.readAllLines(new File("data\\info\\ide\\so_post_refactor_questions_body_ide_set_9-23.csv").toPath());
			List<String> ideTags = Files.readAllLines(new File("data\\info\\ide\\so_post_refactor_questions_tags_ide_set_9-23.csv").toPath());
			
			HashMap<String, String> refMethods = new HashMap<String, String>();
			HashMap<String, String> languages = new HashMap<String, String>();
			HashMap<String, String> ideMap = new HashMap<String, String>();
			
			String postId = "";
			int postCount = 0;
			
			refMethods = (HashMap<String, String>) groupContent(refTitle, refMethods);
			refMethods = (HashMap<String, String>) groupContent(refBody, refMethods);
			refMethods = (HashMap<String, String>) groupContent(refTags, refMethods);
			
			languages = (HashMap<String, String>) groupContent(langTitle, languages);
			languages = (HashMap<String, String>) groupContent(langBody, languages);
			languages = (HashMap<String, String>) groupContent(langTags, languages);
			
			ideMap = (HashMap<String, String>) groupContent(ideTitle, ideMap);
			ideMap = (HashMap<String, String>) groupContent(ideBody, ideMap);
			ideMap = (HashMap<String, String>) groupContent(ideTags, ideMap);
			
			for(String post : posts) {
				postId = post;
				if(postId.contains("Id,")) {
					bw.write("Id,Refactor,Language,IDE\n");
					continue;
				}
				
				postCount++;
				
				//System.out.println(postId + "," + refMethods.get(postId));
				bw.write(postId + "," + refMethods.get(postId) + "," + languages.get(postId) + "," + ideMap.get(postId) + "\n");
				if(postCount % 10 == 0) {
					//System.out.println("------------------------------");
					postCount = 0;
				}
			}
			
			bw.flush();
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void identifyRefactorMethods(String columnHeader) throws IOException {
		br = new BufferedReader(new FileReader("data\\info\\so_post_refactor_answers_not_accepted_body_set_9-23.csv"));
		bw = new BufferedWriter(new FileWriter("data\\info\\ide\\so_post_refactor_answers_not_accepted_body_ide_set_9-23.csv", true));
		List<String> methods = Files.readAllLines(new File("ide_list.txt").toPath()); 
		String line = "";
		String refactors = "";
		String postId = "";
		
		while(line != null) {
			for(int i = 0; i < batchSize; i++) {
				line = br.readLine();
				
				if(line != null){
					if(line.equals(columnHeader)) {
						bw.write("Id,Refactors");
						continue;
					}
					
					for(String method : methods) {
						// Refactor Method Regex: ".*" + method + ".*"
						// Language/IDE Regex: "\\b" + method + "\\b"
						if(matches("\\b" + method + "\\b", line))
							refactors += method + ";";
					}
					
					if(refactors != "")
						refactors = refactors.substring(0, refactors.length()-1);
					
					postId = line.split(",")[0];
					bw.write(postId + "," + refactors + "\n");
					refactors = "";
					//System.out.println(line);
				} else 
					break;
				
			}
			bw.flush();
		}
	}
	
	public static int[] getPostNumbers(String postType, String post) {
		String[] postNums = post.split(",");
		int[] nums = new int[postNums.length - 1];
		
		for(int i = 1; i < postNums.length; i++) {
			nums[i-1] = Integer.parseInt(postNums[i]);
			if(nums[i-1] == 0) 
				ids.add(postNums[0]);
		}
		
		return nums;
	}
	
	public static double getPopularity(int[] postNums) {
		double popularity = 0;
		
		for(int i = 0; i < postNums.length; i++) {
			popularity += postNums[i] / soAvg[i];
			
			//System.out.println(postNums[i] + "/" + soAvg[i] + " = " + (postNums[i] / soAvg[i]));
		}
		
		return popularity;
	}
	
	private static List<String> orderBy(List<String> items, String param, int limit){
		boolean header = true;
		String[] headerArr = {};
		
		for(String item : items) {
			if(header) {
				headerArr = item.split(","); 
			}
		}
		return items;
	}
	
	public static void popularityCount() {
		String columnLine = columns[1];
		String line = "";
		int[] postNums;
		
		try {
			br = new BufferedReader(new FileReader("so_post_refactor_answers_not_accepted_set_9-23.csv"));
			bw = new BufferedWriter(new FileWriter("so_post_refactor_answers_not_accepted_popularity_set_9-23.csv", true));
			
			while(line != null) {
				for(int i = 0; i < batchSize; i++) {
					line = br.readLine();
					
					if(line != null){
						if(line.equals(columnLine)) {
							bw.write(line + ",Popularity" + "\n");
							continue;
						}
						
						postNums = getPostNumbers("question", line);
						line += "," + getPopularity(postNums);
						//System.out.println(line);
						bw.write(line + "\n");
					} else 
						break;
					
				}
				
				ids.clear(); // Clear out the set once done
				
				bw.flush();
			}
			
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static boolean matches(String regex, String text) {
	    Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
	    Matcher matcher = pattern.matcher(text);
	    
	    return matcher.find();
	}
	
	public static void createSampleContent() {
		try {
			bw = new BufferedWriter(new FileWriter("data//sample//annotated_sample_combined.csv"));
			
			List<String> posts = Files.readAllLines(new File("data//info//annotation_question_combined_set_9-23.csv").toPath());
			List<String> samples = Files.readAllLines(new File("data//sample//annotation_question_ids.csv").toPath());
			List<String> sampleIds = new ArrayList<String>();
			String postId = "";
			
			for(String sample : samples) {
				if(sample.equals("Id"))
					continue;
				sampleIds.add(sample);
			}
			
			for(String post : posts) {
				if(post.contains("Id,")) {
					bw.write(post + "\n");
					continue;
				}
				postId = post.split(",")[0];
				
				if(sampleIds.contains(postId)) {
					//System.out.println(post);
					bw.write(post + "\n");
				}
			}
			
			bw.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void getCountOfContent(String content, File contentFile) {
		try {
			List<String> posts = Files.readAllLines(new File("data//info//so_post_refactor_answers_not_accepted_combined_set_9-23.csv").toPath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		Path path = new File("data//questions_popularity_set.csv").toPath();
		/*try {
			List<String> columns = Files.readAllLines(new File("columnNames.txt").toPath());
			String columnName;
			
			for(String column : columns) {
				columnName = column.split(",")[0];
				columnMap.put(columnName, column.substring(columnName.length()+1));
			}
			
			identifyRefactorMethods(columnMap.get("so_questions_post_info"));
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
		combineInfo();
		
		//createSampleContent();
		
		/*try {
			List<String> posts = Files.readAllLines(path);
			orderBy(posts, "Popularity", 100);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
		
	}
}
