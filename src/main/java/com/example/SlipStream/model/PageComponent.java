package com.example.SlipStream.model;

import java.util.Date;
import java.util.List;

/**
 * Abstract base class for all page components in the Composite Pattern
 */
public abstract class PageComponent {
    protected String pageId;
    protected String title;
    protected String owner;
    protected Date createdAt;
    protected Date lastUpdated;
    protected String parentPageId; // Reference to parent

    // Default constructor
    public PageComponent() {
        this.createdAt = new Date();
        this.lastUpdated = new Date();
    }

    // Constructor with common fields
    public PageComponent(String title, String owner, String parentPageId) {
        this();
        this.title = title;
        this.owner = owner;
        this.parentPageId = parentPageId;
    }

    // Core composite pattern operations
    public abstract String getContent();
    public abstract List<PageComponent> getChildren();
    public abstract void addChild(PageComponent component);
    public abstract void removeChild(String componentId);
    public abstract boolean isLeaf();

    // Common getters and setters
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
        this.lastUpdated = new Date();
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

    public String getParentPageId() {
        return parentPageId;
    }

    public void setParentPageId(String parentPageId) {
        this.parentPageId = parentPageId;
    }
}