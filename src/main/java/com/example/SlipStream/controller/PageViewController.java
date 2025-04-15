// package com.example.SlipStream.controller;

// import java.util.concurrent.ExecutionException;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Controller;
// import org.springframework.ui.Model;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.PathVariable;
// import org.springframework.web.bind.annotation.RequestMapping;

// import com.example.SlipStream.model.Page;
// import com.example.SlipStream.service.PageService;

// @Controller
// @RequestMapping("/view/pages")
// public class PageViewController {

//     private final PageService pageService;

//     @Autowired
//     public PageViewController(PageService pageService) {
//         this.pageService = pageService;
//     }

//     @GetMapping("/{pageId}")
//     public String viewPage(@PathVariable String pageId, Model model) {
//         try {
//             Page page = pageService.getPage(pageId);
//             System.out.println(page);
//             if (page != null) {
//                 model.addAttribute("title", page.getTitle());
//                 model.addAttribute("content", page.getContent());
//                 model.addAttribute("pageId", page.getPageId());
//                 model.addAttribute("createdAt", page.getCreatedAt());
//                 model.addAttribute("lastUpdated", page.getLastUpdated());
//                 model.addAttribute("owner", page.getOwner());
//                 model.addAttribute("children", page.getChildren());

//                 System.out.println("Title: " + page.getTitle()); 
//                 System.out.println("Content: " + page.getContent());

                
//                 return "page_template"; // This will look for page_template.html
//             } else {
//                 return "error";
//             }
//         } catch (InterruptedException | ExecutionException e) {
//             model.addAttribute("errorMessage", "Error retrieving page: " + e.getMessage());
//             return "error";
//         }
//     }

//     @GetMapping("/create")
//     public String showCreatePage() {
//         return "create_page"; // This will look for create_page.html
//     }

//     @GetMapping("/edit/{pageId}")
//     public String showEditPage(@PathVariable String pageId, Model model) {
//         try {
//             Page page = pageService.getPage(pageId);
//             if (page != null) {
//                 model.addAttribute("page", page);
//                 return "edit_page"; // This will look for edit_page.html
//             } else {
//                 return "error";
//             }
//         } catch (InterruptedException | ExecutionException e) {
//             model.addAttribute("errorMessage", "Error retrieving page: " + e.getMessage());
//             return "error";
//         }
//     }
// }

package com.example.SlipStream.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.SlipStream.model.ContainerPage;
import com.example.SlipStream.model.ContentPage;
import com.example.SlipStream.model.PageComponent;
import com.example.SlipStream.service.PageService;

@Controller
@RequestMapping("/view/pages")
public class PageViewController {

    private final PageService pageService;

    @Autowired
    public PageViewController(PageService pageService) {
        this.pageService = pageService;
    }

    @GetMapping("/{pageId}")
    public String viewPage(@PathVariable String pageId, Model model) {
        try {
            PageComponent page = pageService.getPage(pageId);
            if (page != null) {
                model.addAttribute("title", page.getTitle());
                model.addAttribute("content", page.getContent());
                model.addAttribute("pageId", page.getPageId());
                model.addAttribute("createdAt", page.getCreatedAt());
                model.addAttribute("lastUpdated", page.getLastUpdated());
                model.addAttribute("owner", page.getOwner());
                model.addAttribute("isContainer", !page.isLeaf());
                
                // Get child pages to display
                List<PageComponent> childPages = new ArrayList<>();
                if (!page.isLeaf()) {
                    childPages = pageService.getChildPages(pageId);
                }
                model.addAttribute("childPages", childPages);
                
                System.out.println("Title: " + page.getTitle()); 
                System.out.println("Content: " + page.getContent());
                
                return "page_template"; // This will look for page_template.html
            } else {
                return "error";
            }
        } catch (InterruptedException | ExecutionException e) {
            model.addAttribute("errorMessage", "Error retrieving page: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/create")
    public String showCreatePage(@RequestParam(required = false) String parentId, Model model) {
        model.addAttribute("parentId", parentId);
        model.addAttribute("pageType", "content"); // Default to content page
        return "create_page"; // This will look for create_page.html
    }
    
    @GetMapping("/create/container")
    public String showCreateContainerPage(@RequestParam(required = false) String parentId, Model model) {
        model.addAttribute("parentId", parentId);
        model.addAttribute("pageType", "container");
        return "create_page"; // This will look for create_page.html with container flag
    }

    @PostMapping("/create")
    public String createPage(@RequestParam String title, 
                            @RequestParam String content,
                            @RequestParam(required = false) String parentId, 
                            @RequestParam String owner,
                            @RequestParam String pageType) {
        try {
            String pageId;
            
            if ("container".equals(pageType)) {
                pageId = pageService.createContainerPage(title, content, parentId, owner);
            } else {
                pageId = pageService.createContentPage(title, content, parentId, owner);
            }
            
            return "redirect:/view/pages/" + pageId;
        } catch (InterruptedException | ExecutionException e) {
            return "error";
        }
    }

    @GetMapping("/edit/{pageId}")
    public String showEditPage(@PathVariable String pageId, Model model) {
        try {
            PageComponent page = pageService.getPage(pageId);
            if (page != null) {
                model.addAttribute("page", page);
                model.addAttribute("isContainer", !page.isLeaf());
                return "edit_page"; // This will look for edit_page.html
            } else {
                return "error";
            }
        } catch (InterruptedException | ExecutionException e) {
            model.addAttribute("errorMessage", "Error retrieving page: " + e.getMessage());
            return "error";
        }
    }
    
    @PostMapping("/edit/{pageId}")
    public String updatePage(@PathVariable String pageId, 
                            @RequestParam String title,
                            @RequestParam String content) {
        try {
            PageComponent page = pageService.getPage(pageId);
            if (page != null) {
                page.setTitle(title);
                
                if (page.isLeaf()) {
                    ((ContentPage) page).setContent(content);
                } else {
                    ((ContainerPage) page).setSummary(content);
                }
                
                pageService.createPage(page); // Update the page
                return "redirect:/view/pages/" + pageId;
            } else {
                return "error";
            }
        } catch (InterruptedException | ExecutionException e) {
            return "error";
        }
    }
}