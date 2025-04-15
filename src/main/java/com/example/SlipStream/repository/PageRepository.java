// package com.example.SlipStream.repository;

// import java.util.List;
// import java.util.concurrent.ExecutionException;

// import com.example.SlipStream.model.Page;

// public interface PageRepository {
//     String createPage(Page page) throws ExecutionException, InterruptedException;
//     Page getPage(String pageId) throws ExecutionException, InterruptedException;
//     List<Page> getAllPages() throws ExecutionException, InterruptedException;
//     List<Page> getChildPages(String parentPageId) throws ExecutionException, InterruptedException;
//     boolean updatePageContent(String pageId, String newContent) throws ExecutionException, InterruptedException;
//     boolean deletePage(String pageId) throws ExecutionException, InterruptedException;
// }

package com.example.SlipStream.repository;

import java.util.List;
import java.util.concurrent.ExecutionException;

import com.example.SlipStream.model.PageComponent;

public interface PageRepository {
    String createPage(PageComponent page) throws ExecutionException, InterruptedException;
    PageComponent getPage(String pageId) throws ExecutionException, InterruptedException;
    List<PageComponent> getAllPages() throws ExecutionException, InterruptedException;
    List<PageComponent> getChildPages(String parentPageId) throws ExecutionException, InterruptedException;
    boolean updatePageContent(String pageId, String newContent) throws ExecutionException, InterruptedException;
    boolean deletePage(String pageId) throws ExecutionException, InterruptedException;
    boolean updatePage(PageComponent page) throws ExecutionException, InterruptedException;
}