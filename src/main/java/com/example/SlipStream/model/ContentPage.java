package com.example.SlipStream.model;

import java.util.Collections;
import java.util.List;
import java.util.Date;


/**
 * Content page is a leaf node in the composite pattern.
 * It represents a basic page with content but no children.
 */
public class ContentPage extends PageComponent {
    private String content;

    // Default constructor for Firebase
    public ContentPage() {
        super();
    }

    // Constructor with fields
    public ContentPage(String title, String content, String parentPageId, String owner) {
        super(title, owner, parentPageId);
        this.content = content;
    }

    @Override
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
        this.lastUpdated = new Date();
    }

    @Override
    public List<PageComponent> getChildren() {
        return Collections.emptyList(); // Leaf nodes have no children
    }

    @Override
    public void addChild(PageComponent component) {
        throw new UnsupportedOperationException("Cannot add children to a content page");
    }

    @Override
    public void removeChild(String componentId) {
        throw new UnsupportedOperationException("Cannot remove children from a content page");
    }

    @Override
    public boolean isLeaf() {
        return true;
    }
}