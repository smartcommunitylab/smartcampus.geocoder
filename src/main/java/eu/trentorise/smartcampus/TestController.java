package eu.trentorise.smartcampus;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class TestController {

	
	@RequestMapping(method=RequestMethod.GET, value="/test")
	public @ResponseBody String echo() {
		return "test message";
	}
	
	@RequestMapping(method=RequestMethod.GET, value="/test/{msg}")
	public @ResponseBody String echo(@PathVariable String msg) {
		return "echo: "+msg;
	}
	
}
