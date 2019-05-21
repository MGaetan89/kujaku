package io.ona.kujaku.plugin.switcher;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.PopupMenu;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;

import java.util.ArrayList;

import io.ona.kujaku.R;
import io.ona.kujaku.plugin.switcher.layer.BaseLayer;
import io.ona.kujaku.views.KujakuMapView;
import timber.log.Timber;

/**
 * Created by Ephraim Kigamba - ekigamba@ona.io on 2019-05-16
 */

public class BaseLayerSwitcherPlugin implements PopupMenu.OnMenuItemClickListener {

    private KujakuMapView kujakuMapView;
    private MapboxMap mapboxMap;
    private Style style;
    private FloatingActionButton layerSwitcherFab;

    @Nullable
    private BaseLayer currentBaseLayer;
    @Nullable
    private BaseLayer currentlySelectedBaseLayer;

    private ArrayList<BaseLayer> baseLayers = new ArrayList<>();
    private PopupMenu popupMenu;

    public BaseLayerSwitcherPlugin(@NonNull KujakuMapView kujakuMapView, @NonNull MapboxMap mapboxMap, @NonNull Style style) {
        this.kujakuMapView = kujakuMapView;
        this.mapboxMap = mapboxMap;
        this.style = style;
    }

    public boolean addBaseLayer(@NonNull BaseLayer baseLayer, boolean isDefault) {
        if (baseLayers.contains(baseLayer)) {
            // Todo: switch the currentBaseLayer to this one here
            if (isDefault)
            return false;
        }

        for (BaseLayer addedBaseLayer: baseLayers) {
            if (addedBaseLayer.getId().equals(baseLayer.getId())) {
                // Todo: switch the currentBaseLayer to this one here
                return false;
            }
        }

        this.baseLayers.add(baseLayer);
        return true;
    }

    public boolean removeBaseLayerFromMap(@NonNull BaseLayer baseLayer) {
        kujakuMapView.removeLayer(baseLayer);
        return true;
    }

    public boolean removeBaseLayer(@NonNull BaseLayer baseLayer) {
        if (baseLayers.remove(baseLayer)) {
            return true;
        } else {
            for (BaseLayer addedBaseLayer : baseLayers) {
                if (addedBaseLayer.getId().equals(baseLayer.getId())) {
                    baseLayers.remove(addedBaseLayer);
                    return true;
                }
            }

            return false;
        }
    }

    @NonNull
    public ArrayList<BaseLayer> getBaseLayers() {
        return baseLayers;
    }

    public void show() {
        showPluginSwitcherView();
    }

    public void showPluginSwitcherView() {
        if (kujakuMapView != null) {
            if (layerSwitcherFab == null) {
                layerSwitcherFab = kujakuMapView.findViewById(R.id.fab_mapview_layerSwitcher);
            }

            layerSwitcherFab.setVisibility(View.VISIBLE);

            layerSwitcherFab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (popupMenu == null) {
                        showPopup(layerSwitcherFab);
                    } else {
                        popupMenu.dismiss();
                    }
                }
            });
        }
    }

    public void showPopup(@NonNull View anchorView) {
        popupMenu = new PopupMenu(kujakuMapView.getContext(), anchorView, Gravity.START);
        popupMenu.setOnMenuItemClickListener(this);
        popupMenu.inflate(R.menu.layer_switcher_popup_menu);

        // Add the other items
        Menu menu = popupMenu.getMenu();

        int counter = 0;
        for (BaseLayer baseLayer: getBaseLayers()) {
            menu.add(0, baseLayer.hashCode(), counter, baseLayer.getDisplayName());

            if (currentBaseLayer != null && (baseLayer == currentBaseLayer || baseLayer.getId().equals(currentBaseLayer.getId()))) {
                menu.getItem(counter).setChecked(true);
            }

            counter++;
        }

        popupMenu.setOnDismissListener(new PopupMenu.OnDismissListener() {
            @Override
            public void onDismiss(PopupMenu menu) {
                popupMenu = null;
            }
        });
        popupMenu.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (currentBaseLayer != null && item.getItemId() == currentBaseLayer.hashCode()) {
            return true;
        }

        for (BaseLayer baseLayer: baseLayers) {
            if (item.getItemId() == baseLayer.hashCode()) {

                // Disable the current checked item
                if (popupMenu != null) {
                    int size = popupMenu.getMenu().size();

                    for (int i = 0; i < size; i++) {
                        MenuItem menuItem = popupMenu.getMenu().getItem(i);
                        if (menuItem.isChecked()) {
                            menuItem.setChecked(false);
                        }
                    }
                }

                item.setChecked(true);
                showBaseLayer(baseLayer);
                return true;
            }
        }

        return false;
    }

    public void showBaseLayer(@NonNull BaseLayer baseLayer) {
        if (style.isFullyLoaded()) {
            // Remove the previous baseLayer
            if (currentBaseLayer != null) {
                // Todo: Figure out this issue, where "Cannot Add Already Added Layer" or "Memory Violation"
                //removeBaseLayerFromMap(currentBaseLayer);
                kujakuMapView.disableLayer(currentBaseLayer);
                currentBaseLayer = null;
            }

            kujakuMapView.addLayer(baseLayer);
            currentBaseLayer = baseLayer;
        } else {
            Timber.e("The style has not fully loaded and the baseLayer %s of ID(%s) cannot be added"
                    , baseLayer.getDisplayName(), baseLayer.getId());
        }
    }

    private void removePreviousBaseLayer(@NonNull BaseLayer baseLayer) {
        kujakuMapView.removeLayer(baseLayer);
    }
}
