package com.videonasocialmedia.videonamediaframework.model.media.effects;

/**
 * Created by Veronica Lago Fominaya on 25/11/2015.
 */
public class ShaderEffect extends Effect {

    private final int resourceId;

    public ShaderEffect(String identifier, String name, String iconPath, int resourceId,
                        String type) {
        super(identifier, name, iconPath, type);
        this.resourceId = resourceId;
    }

    public ShaderEffect(String identifier, String name, int iconId, int resourceId,
                        String type) {
        super(identifier, name, iconId, type);
        this.resourceId = resourceId;
    }

    public int getResourceId() {
        return resourceId;
    }
}
