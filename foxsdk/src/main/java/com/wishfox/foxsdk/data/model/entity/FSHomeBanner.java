package com.wishfox.foxsdk.data.model.entity;

/**
 * 主要功能:
 *
 * @Description:
 * @author: 范为广
 * @date: 2025年10月28日 16:54
 */
public class FSHomeBanner {

    private Long id;
    private String image;
    private String link;

    public FSHomeBanner() {}

    public FSHomeBanner(Long id, String image, String link) {
        this.id = id;
        this.image = image;
        this.link = link;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }
}
