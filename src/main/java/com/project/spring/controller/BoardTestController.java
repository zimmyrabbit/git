package com.project.spring.controller;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.View;

import com.project.spring.service.BoardTestService;
import com.project.spring.util.Constants;
import com.project.spring.util.ExcelView;
import com.project.spring.util.FileUtil;

@Controller
public class BoardTestController {

	private static final Logger logger = LoggerFactory.getLogger(BoardTestController.class);
	
	@Autowired
	BoardTestService boardTestService;
	
	//리스트
	@RequestMapping(value="/board/list", method=RequestMethod.GET)
	public String boardTestList(Model model) {
		
		ArrayList<Map<String, String>> list = boardTestService.getBoradTestList();
		
		model.addAttribute("list", list);
		
		return "board/list";
	}
	
	//글쓰기
	@RequestMapping(value="/board/write", method=RequestMethod.GET)
	public void write() {}
	
	@RequestMapping(value="/board/write", method=RequestMethod.POST)
	public String writeProc(HttpServletRequest request) throws Exception {
		HashMap<String, String> map = new HashMap<String, String>();
		String file_size = request.getParameter("file_size");
		String content = request.getParameter("content");
		String title = request.getParameter("title");
		
		if(Constants.MAX_FILESIZE < Integer.parseInt(file_size)) {
			throw new Exception("file_size ===> MAX_ERROR");
		}
		if(content == null || "".equals(content)) {
			throw new Exception("content ===> content not exits");
		}else {
			map.put("content", content);
		}
		if(title == null || "".equals(title)) {
			throw new Exception("title ===> title not exits");
		}else {
			map.put("title", title);
		}
		
		List<Map> transFile = null;
		transFile = FileUtil.multiUploadFile(request, Constants.BOARD_DOWN_LOAD_PATH, Constants.BOARD_SUB_FOLDER);
		
		if(transFile != null && transFile.size() > 1) {
			throw new Exception("===================== overFileCnt =====================");
		}
		
		int cnt = boardTestService.writeBoard(map,transFile);
		
		if( cnt < 1) {
			throw new Exception(" /board/write > post ============ ERROR ============");
		}
		
		return "redirect: /board/list";
	}
	
	
	/**
	 * 
	 * 비디오 업로드
	 */
	@RequestMapping(value="/board/videoWrite", method=RequestMethod.GET)
	public void videoWrite() {}
	
	@RequestMapping(value="/board/videoWrite", method=RequestMethod.POST)
	public String videoWrite(HttpServletRequest request) throws Exception {
		HashMap<String, String> map = new HashMap<String, String>();
		String file_size = request.getParameter("file_size");
		String content = request.getParameter("content");
		String title = request.getParameter("title");
		
		if(Constants.MAX_VIDEO_FILESIZE < Integer.parseInt(file_size)) {
			throw new Exception("file_size ===> MAX_ERROR");
		}
		if(content == null || "".equals(content)) {
			throw new Exception("content ===> content not exits");
		}else {
			map.put("content", content);
		}
		if(title == null || "".equals(title)) {
			throw new Exception("title ===> title not exits");
		}else {
			map.put("title", title);
		}
		
		List<Map> transFile = null;
		transFile = FileUtil.ffmpegUploadFile(request, Constants.BOARD_DOWN_LOAD_PATH, Constants.BOARD_SUB_FOLDER);
		
		if(transFile != null && transFile.size() > 1) {
			throw new Exception("===================== overFileCnt =====================");
		}
		
		int cnt = boardTestService.writeBoard(map,transFile);
		
		if( cnt < 1) {
			throw new Exception(" /board/write > post ============ ERROR ============");
		}
		
		return "redirect: /board/list";
	}
	
	//상세보기
	@RequestMapping(value="/board/detail", method=RequestMethod.GET)
	public String boardTestDetail(Model model, int no) throws Exception {
		
		HashMap<String, Object> map = boardTestService.detailBoard(no);
			
		String[] ext = {".png",".jpg",".jpeg",".gif",".pdf"};
		String caseExt = map.get("contentType").toString().toLowerCase();
		boolean contentType = Arrays.asList(ext).contains(caseExt);  // 사진이면 true, 그 외 false
		
		if(caseExt.equals("n")) {  
			map.put("contentType", "no");  // 파일없음.
		}else if(contentType) {
			map.put("contentType", "img");  // 사진파일
		}else if(!contentType) {
			map.put("contentType", "video");  // 동영상 파일
		}else {  // 그 외는 에러로 간주
			throw new Exception(" /board/detail > file detail ============ ERROR ============");
		}
		model.addAttribute("map", map);
		
		return "board/detail";
	}
	
	//파일 다운로드
	@RequestMapping(value="/board/fileDown", method=RequestMethod.GET)
	public void boardFileDown(HttpServletRequest request,
			HttpServletResponse response,
			Model model)  throws Exception{
		
		boolean isError = false;
		String errMsg = "";
		
		try {
				String saveFilePath = Constants.BOARD_DOWN_LOAD_PATH + File.separatorChar + Constants.BOARD_SUB_FOLDER;
				String saveName = request.getParameter("saveName"); // 실제 저장 파일
				String fileName = request.getParameter("fileName"); // 원본 파일 네임 ( 다운로드 할때는 원본 파일명 )
				String downPath = saveFilePath+File.separatorChar+saveName; 
				
				String header = getBrowser(request);
				response.setContentType("application/octet-stream");
				if (header.contains("MSIE")) {
				       String docName = URLEncoder.encode(fileName,"UTF-8").replaceAll("\\+", "%20");
				       response.setHeader("Content-Disposition", "attachment;filename=" + docName + ";");
				} else if (header.contains("Firefox")) {
				       String docName = new String(fileName.getBytes("UTF-8"), "ISO-8859-1");
				       response.setHeader("Content-Disposition", "attachment; filename=\"" + docName + "\"");
				} else if (header.contains("Opera")) {
				       String docName = new String(fileName.getBytes("UTF-8"), "ISO-8859-1");
				       response.setHeader("Content-Disposition", "attachment; filename=\"" + docName + "\"");
				} else if (header.contains("Chrome")) {
				       String docName = new String(fileName.getBytes("UTF-8"), "ISO-8859-1");
				       response.setHeader("Content-Disposition", "attachment; filename=\"" + docName + "\"");
				}else{
					throw new IllegalArgumentException("알수없는 브라우저 입니다.");
				}
				
				File file = new File(downPath);	
				byte bytestream[] = new byte[2048000];

					try{
						if(file.exists()) {
							FileInputStream fis = new FileInputStream(file);
				            BufferedInputStream bis = new BufferedInputStream(fis);
				            BufferedOutputStream bos = new BufferedOutputStream(response.getOutputStream());
				            int read = 0;
				            while ((read = bis.read(bytestream)) != -1){
				                bos.write(bytestream , 0, read);
				            }
				            bos.close();
				            bis.close();
						}
					}catch(IllegalArgumentException e){
						throw e;					
					}catch(Exception e){
						throw e;					
					}
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				errMsg = e.getMessage();
				isError = true;
			} catch (Exception e) {
				e.printStackTrace();
				errMsg = e.getMessage();
				isError = true;
			}finally{			
				if(false == isError){
					
				}else{
					response.setContentType("text/html; charset=euc-kr");
					String alert = "<script>alert(\'파일 다운로드에 실패하였습니다.<br/>확인해 주십시요!');self.close();</script>";
					response.getWriter().write(alert);
				}
			}
		}
	
	private String getBrowser(HttpServletRequest request) {
        String header =request.getHeader("User-Agent");
        if (header.contains("MSIE")) { return "MSIE";
        } else if(header.contains("Chrome")) { return "Chrome";
        } else if(header.contains("Opera")) { return "Opera";
        }else {return "Firefox"; }
    }
	
	@RequestMapping(value="/board/map", method=RequestMethod.GET) 
	public void map() { }
	
	@RequestMapping(value="/board/map", method=RequestMethod.POST)
	public @ResponseBody void mapProc(HttpServletRequest request) {
		
		HashMap<String, String> map = new HashMap<>();

		String address = request.getParameter("address");
		StringTokenizer tokens = new StringTokenizer(address, " ");
		String[] newStr = new String[tokens.countTokens()];

		String mainAddress = "";
		String middleAddress = "";
		
		int index = 0;
		while(tokens.hasMoreTokens()) {
			newStr[index] = tokens.nextToken();
			if(index < 2) {
				mainAddress += newStr[index] + " ";
			} else {
				middleAddress += newStr[index] + " ";
			}
			index++;
		}
		
		map.put("mainAddress", mainAddress);
		map.put("middleAddress", middleAddress);
		map.put("latitude", request.getParameter("latitude"));
		map.put("longitude", request.getParameter("longitude"));
		
		boardTestService.setMapInfo(map);
	}
	
	@RequestMapping(value="/board/mapList", method=RequestMethod.GET)
	public @ResponseBody List<Map<String, Object>> mapList() {
		
		List<Map<String, Object>> list = boardTestService.getMapList();

		return list;
	}
	
	@RequestMapping(value="/board/excelDown", method=RequestMethod.POST)
	public View getExcelDown(Model model) {
		
		List<Map<String, String>> list = boardTestService.getBoradTestList();
		model.addAttribute("list", list);

		return new ExcelView();
	}
	
	
}
