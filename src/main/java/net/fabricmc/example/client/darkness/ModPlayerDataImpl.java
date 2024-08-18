package net.fabricmc.example.client.darkness;

public class ModPlayerDataImpl implements ModPlayerData {
    private boolean hasMod = false;

    @Override
    public void setHasMod(boolean hasMod) {
        this.hasMod = hasMod;
    }

    @Override
    public boolean hasMod() {
        return hasMod;
    }
}