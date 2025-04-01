package com.example.SlipStream.service;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.SlipStream.model.Page;
import com.example.SlipStream.repository.PageRepository;

@Service
public class PageService {

    private final PageRepository pageRepository;

    @Autowired
    public PageService(PageRepository pageRepository) {
        this.pageRepository = pageRepository;
    }

    public String createPage(Page page) throws ExecutionException, InterruptedException {
        return pageRepository.createPage(page);
    }

    public Page getPage(String pageId) throws ExecutionException, InterruptedException {
        return pageRepository.getPage(pageId);
    }

    public List<Page> getAllPages() throws ExecutionException, InterruptedException {
        return pageRepository.getAllPages();
    }

    public List<Page> getChildPages(String parentPageId) throws ExecutionException, InterruptedException {
        return pageRepository.getChildPages(parentPageId);
    }

    public boolean updatePageContent(String pageId, String newContent) throws ExecutionException, InterruptedException {
        return pageRepository.updatePageContent(pageId, newContent);
    }

    public boolean deletePage(String pageId) throws ExecutionException, InterruptedException {
        return pageRepository.deletePage(pageId);
    }
}
