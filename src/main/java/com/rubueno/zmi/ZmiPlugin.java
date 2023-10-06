package com.rubueno.zmi;

import com.google.common.collect.ImmutableSet;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import org.apache.commons.lang3.ArrayUtils;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@PluginDescriptor(
        name = "ZMI",
        description = "Removes scenery from ZMI to improve performance"
)
@Slf4j
public class ZmiPlugin extends Plugin {
    private static final Set<Integer> HIDE = ImmutableSet.of(
            21011, 21012, // bench
            29630, 29629, // lava pool
            29633, 29632, // lava bubbles
            34780, 34781, 34794, // altar pillar
            25052, // stone pillar
            25087, 25088, 25089 // skeleton
            // 25092, 25093, 24094, 25095, 25096, 25097, 25098 // floor rubble
            // need help hiding this
    );

    private static final Set<Integer> ZmiMapRegions = ImmutableSet.of(
        12118, 12119, 12374, 12375
    );

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Override
    protected void startUp() {
        if (client.getGameState() == GameState.LOGGED_IN) {
            clientThread.invoke(this::hide);
        }
    }

    @Override
    protected void shutDown() {
        clientThread.invoke(() ->
        {
            if (client.getGameState() == GameState.LOGGED_IN) {
                client.setGameState(GameState.LOADING);
            }
        });
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        if (gameStateChanged.getGameState() == GameState.LOGGED_IN) {
            hide();
        }
    }

    private void hide() {
        if (!isInZmi()) {
            return;
        }
        Scene scene = client.getScene();
        Tile[][] tiles = scene.getTiles()[0];
        Player player = client.getLocalPlayer();
        int cnt = 0;
        for (int x = 0; x < Constants.SCENE_SIZE; ++x) {
            for (int y = 0; y < Constants.SCENE_SIZE; ++y) {
                Tile tile = tiles[x][y];
                if (tile == null) {
                    continue;
                }


                for (GameObject gameObject : tile.getGameObjects()) {
                    if (gameObject == null) {
                        continue;
                    }

                    if (HIDE.contains(gameObject.getId())) {
                        scene.removeGameObject(gameObject);
                        ++cnt;
                        break;
                    }
                }
            }
        }

        log.debug("Removed {} objects", cnt);
    }

    private boolean isInZmi() {
        Integer[] asd = ArrayUtils.toObject(client.getMapRegions());
        Set<Integer> currentRegions = new HashSet<Integer>(Arrays.asList(asd));
        currentRegions.retainAll(ZmiMapRegions);
        return currentRegions.stream().count() > 0;
    }
}
