package com.cytricos.races.hook;

import com.cytricos.races.CytricosRaces;

/**
 * Clase puente de PlaceholderAPIHook que delega a CytricosRacesPAPI para retrocompatibilidad total.
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class PlaceholderAPIHook extends CytricosRacesPAPI {
    public PlaceholderAPIHook(CytricosRaces plugin) {
        super(plugin);
    }
}
