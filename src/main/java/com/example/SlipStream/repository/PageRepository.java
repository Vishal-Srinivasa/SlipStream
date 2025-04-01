package com.example.SlipStream.repository;

import java.util.List;
import java.util.concurrent.ExecutionException;

import com.example.SlipStream.model.Page;

public interface PageRepository {
    String createPage(Page page) throws ExecutionException, InterruptedException;
    Page getPage(String pageId) throws ExecutionException, InterruptedException;
    List<Page> getAllPages() throws ExecutionException, InterruptedException;
    List<Page> getChildPages(String parentPageId) throws ExecutionException, InterruptedException;
    boolean updatePageContent(String pageId, String newContent) throws ExecutionException, InterruptedException;
    boolean deletePage(String pageId) throws ExecutionException, InterruptedException;
}