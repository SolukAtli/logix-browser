package com.logix.browser.adblock

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds the lightweight cosmetic-hiding script: collected selectors are
 * hidden via `display:none !important`, re-applied on DOM mutations so
 * lazily injected ad slots disappear too. No-ops when the list is empty.
 */
@Singleton
class CosmeticFilter @Inject constructor() {

    fun buildScript(selectors: List<String>): String {
        if (selectors.isEmpty()) return ""
        val json = selectors.joinToString(separator = ",", prefix = "[", postfix = "]") {
            "\"" + it.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        }
        return "(function(sels){" +
            "function hide(){try{" +
            "for(var i=0;i<sels.length;i++){" +
            "var els=document.querySelectorAll(sels[i]);" +
            "for(var j=0;j<els.length;j++){els[j].style.setProperty('display','none','important')}}" +
            "}}catch(e){}}" +
            "if(document.readyState==='loading'){document.addEventListener('DOMContentLoaded',hide)}else{hide()}" +
            "try{new MutationObserver(hide).observe(document.documentElement,{childList:true,subtree:true})}catch(e){}" +
            "})($json)"
    }
}
