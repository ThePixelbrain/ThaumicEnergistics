package thaumicenergistics.integration.appeng;

import appeng.api.parts.IPartModel;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;

/**
 * @author BrockWS
 */
public class ThEPartModel implements IPartModel {
    private boolean requireCableConnection;
    private List<ResourceLocation> models;

    public ThEPartModel(ResourceLocation models) {
        this(true, models);
    }

    public ThEPartModel(ResourceLocation... models) {
        this(true, models);
    }

    public ThEPartModel(boolean requireCableConnection, ResourceLocation... models) {
        this.models = Arrays.asList(models);
        this.requireCableConnection = requireCableConnection;
    }

    @Override
    public boolean requireCableConnection() {
        return this.requireCableConnection;
    }

    @Nonnull
    @Override
    public List<ResourceLocation> getModels() {
        return this.models;
    }
}
