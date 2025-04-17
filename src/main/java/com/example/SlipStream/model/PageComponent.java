package com.example.SlipStream.model;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract base class for all page components in the Composite Pattern
 */
public abstract class PageComponent {
    protected String pageId;
    protected String title;
    protected String owner; // User email or ID of the owner
    protected Date createdAt;
    protected Date lastUpdated;
    protected String parentPageId; // Reference to parent
    protected boolean isPublished; // Flag for public access
    protected Map<String, String> sharingInfo; // Map<UserEmail, AccessLevel("view" or "edit")>

    // Default constructor
    public PageComponent() {
        this.createdAt = new Date();
        this.lastUpdated = new Date();
        this.isPublished = false;
        this.sharingInfo = new HashMap<>();
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

    public boolean isPublished() {
        return isPublished;
    }

    public void setPublished(boolean published) {
        isPublished = published;
        this.lastUpdated = new Date();
    }

    public Map<String, String> getSharingInfo() {
        if (this.sharingInfo == null) {
            this.sharingInfo = new HashMap<>();
        }
        return sharingInfo;
    }

    public void setSharingInfo(Map<String, String> sharingInfo) {
        this.sharingInfo = sharingInfo;
        this.lastUpdated = new Date();
    }

    // Helper methods for sharing
    public void addShare(String userEmail, String accessLevel) {
        if (this.sharingInfo == null) {
            this.sharingInfo = new HashMap<>();
        }
        this.sharingInfo.put(userEmail, accessLevel);
        this.lastUpdated = new Date();
    }

    public void removeShare(String userEmail) {
        if (this.sharingInfo != null) {
            this.sharingInfo.remove(userEmail);
            this.lastUpdated = new Date();
        }
    }

    public String getAccessLevel(String userEmail) {
        if (this.sharingInfo == null) {
            return null;
        }
        return this.sharingInfo.get(userEmail);
    }
}