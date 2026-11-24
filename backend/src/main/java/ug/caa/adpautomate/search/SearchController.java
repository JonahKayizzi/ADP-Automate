package ug.caa.adpautomate.search;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
public class SearchController {

  private final SearchService searchService;

  public SearchController(SearchService searchService) {
    this.searchService = searchService;
  }

  @GetMapping
  public Map<String, Object> search(@RequestParam(name = "q", defaultValue = "") String query) {
    Map<String, List<SearchService.Hit>> groups = searchService.search(query);
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("query", query == null ? "" : query.trim());
    response.put("groups", groups);
    return response;
  }
}
