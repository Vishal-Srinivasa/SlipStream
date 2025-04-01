package com.example.SlipStream.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Page {
    private String pageId;
    private String title;
    private String content;
    private String parentPage;
    private List<String> children;
    private String owner;
    private Date createdAt;
    private Date lastUpdated;

    // Default constructor for Firebase
    public Page() {
        this.children = new ArrayList<>();
        this.createdAt = new Date();
        this.lastUpdated = new Date();
    }

    // Constructor with fields
    public Page(String title, String content, String parentPage, String owner) {
        this();
        this.title = title;
        this.content = content;
        this.parentPage = parentPage;
        this.owner = owner;
    }

    // Getters and setters
    public String getPageId() {
        return pageId;
    }

    public void setPageId(String pageId) {
        this.pageId = pageId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
        this.lastUpdated = new Date();
    }

    public String getParentPage() {
        return parentPage;
    }

    public void setParentPage(String parentPage) {
        this.parentPage = parentPage;
    }

    public List<String> getChildren() {
        return children;
    }

    public void setChildren(List<String> children) {
        this.children = children;
    }
    
    public void addChild(String childId) {
        if (this.children == null) {
            this.children = new ArrayList<>();
        }
        this.children.add(childId);
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}