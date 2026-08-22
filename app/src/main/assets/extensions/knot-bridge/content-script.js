// KNOT Page Bridge -- content script.
//
// Runs in every page (document_idle) and implements the actual DOM
// operations the agent asks for. The background script forwards a command
// here via browser.tabs.sendMessage / browser.runtime.onMessage, and
// whatever this returns goes straight back to the Kotlin side as the tool
// result. This is the real replacement for the old GeckoJs reflective probe:
// every one of these mirrors a script BrowserAgent.kt used to build by hand.

(function () {
  "use strict";

  function readPage() {
    try {
      const article = document.querySelector("article");
      const text = article ? article.innerText : document.body ? document.body.innerText : "";
      return (text || "").substring(0, 12000);
    } catch (e) {
      return "";
    }
  }

  function bodyText() {
    try {
      return document.body ? document.body.innerText : "";
    } catch (e) {
      return "";
    }
  }

  function findText(query) {
    const text = bodyText();
    const found = !!query && text.toLowerCase().indexOf(String(query).toLowerCase()) >= 0;
    return found ? "Found '" + query + "'" : "'" + query + "' not found";
  }

  function tap(selector, text) {
    let el = null;
    if (selector) {
      el = document.querySelector(selector);
    } else if (text) {
      const target = String(text).toLowerCase();
      const all = document.querySelectorAll(
        'a,button,input,[role="button"],[onclick],label,summary',
      );
      for (let i = 0; i < all.length; i++) {
        const t = (all[i].innerText || all[i].value || all[i].getAttribute("aria-label") || "")
          .trim()
          .toLowerCase();
        if (t.indexOf(target) >= 0) {
          el = all[i];
          break;
        }
      }
    }
    if (!el) return "NOT_FOUND";
    el.scrollIntoView({ block: "center", behavior: "instant" });
    el.click();
    return "CLICKED";
  }

  function typeText(value, selector) {
    let el = null;
    if (selector) {
      el = document.querySelector(selector);
      if (!el) return "NOT_FOUND";
    } else {
      el = document.activeElement;
      if (!el || (el.tagName !== "INPUT" && el.tagName !== "TEXTAREA" && !el.isContentEditable)) {
        el = document.querySelector("input:not([type=hidden]),textarea,[contenteditable=true]");
      }
      if (!el) return "NO_INPUT";
    }
    el.focus();
    if (el.isContentEditable) {
      el.innerText = value;
    } else {
      el.value = value;
    }
    el.dispatchEvent(new Event("input", { bubbles: true }));
    el.dispatchEvent(new Event("change", { bubbles: true }));
    return "TYPED";
  }

  function scroll(direction, amount) {
    const amt = amount || 600;
    switch (direction) {
      case "top":
        window.scrollTo(0, 0);
        break;
      case "bottom":
        window.scrollTo(0, document.body.scrollHeight);
        break;
      case "up":
        window.scrollBy(0, -amt);
        break;
      default:
        window.scrollBy(0, amt);
    }
    return "ok";
  }

  function editPage(find, replace, selector) {
    if (!find) return "NO_ROOT";
    const root = (selector && document.querySelector(selector)) || document.body;
    if (!root) return "NO_ROOT";
    let count = 0;
    const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT, null);
    let node;
    while ((node = walker.nextNode())) {
      if (node.nodeValue && node.nodeValue.indexOf(find) >= 0) {
        node.nodeValue = node.nodeValue.split(find).join(replace);
        count++;
      }
    }
    return "EDITED:" + count;
  }

  function selectText(text) {
    if (!text) return "NOT_FOUND";
    const walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, null);
    let node;
    while ((node = walker.nextNode())) {
      const idx = (node.nodeValue || "").indexOf(text);
      if (idx >= 0) {
        const range = document.createRange();
        range.setStart(node, idx);
        range.setEnd(node, idx + text.length);
        const sel = window.getSelection();
        sel.removeAllRanges();
        sel.addRange(range);
        return "SELECTED";
      }
    }
    return "NOT_FOUND";
  }

  function getSelectionText() {
    try {
      return window.getSelection ? window.getSelection().toString() : "";
    } catch (e) {
      return "";
    }
  }

  // Gecko does not support the non-standard CSS `zoom` property (it's a
  // WebKit/Blink-only extension), so `document.body.style.zoom = factor`
  // silently no-ops on every page here. Emulate page zoom the way Firefox's
  // own full-zoom does under the hood: scale the document with a CSS
  // transform and compensate the width so the layout doesn't reflow/clip.
  const ZOOM_STYLE_ID = "__knot_zoom_style__";

  function setZoom(factor) {
    const f = parseFloat(factor);
    const scale = Number.isFinite(f) && f > 0 ? f : 1;

    let style = document.getElementById(ZOOM_STYLE_ID);
    if (!style) {
      style = document.createElement("style");
      style.id = ZOOM_STYLE_ID;
      (document.head || document.documentElement).appendChild(style);
    }

    if (scale === 1) {
      style.textContent = "";
      return "ok";
    }

    const inverse = (100 / scale).toFixed(4);
    style.textContent =
      "html { width: " + inverse + "% !important; }" +
      "html { transform: scale(" + scale + ") !important; " +
      "transform-origin: 0 0 !important; }";
    return "ok";
  }

  function setDesktopViewport(desktop) {
    const meta = document.querySelector('meta[name="viewport"]');
    if (meta) {
      meta.setAttribute(
        "content",
        desktop ? "width=1280" : "width=device-width, initial-scale=1",
      );
    }
    return "ok";
  }

  function getPageInfo() {
    return {
      title: document.title || "",
      url: location.href,
    };
  }

  // execute_js's safety-list is still enforced Kotlin-side (blocked terms are
  // filtered before this ever runs) -- this is just the sandboxed evaluator.
  function runExpression(code) {
    try {
      // eslint-disable-next-line no-new-func
      const fn = new Function('"use strict"; return (' + code + ");");
      const result = fn();
      return String(result);
    } catch (e) {
      return "ERR:" + (e && e.message ? e.message : String(e));
    }
  }

  browser.runtime.onMessage.addListener((message) => {
    const { command, args } = message || {};
    const a = args || {};
    switch (command) {
      case "get_page_info":
        return Promise.resolve(getPageInfo());
      case "read_page":
        return Promise.resolve(readPage());
      case "find_text":
        return Promise.resolve(findText(a.query));
      case "tap":
        return Promise.resolve(tap(a.selector, a.text));
      case "type_text":
        return Promise.resolve(typeText(a.text, a.selector));
      case "scroll":
        return Promise.resolve(scroll(a.direction, a.amount));
      case "edit_page":
        return Promise.resolve(editPage(a.find, a.replace, a.selector));
      case "select_text":
        return Promise.resolve(selectText(a.text));
      case "get_selection":
        return Promise.resolve(getSelectionText());
      case "set_zoom":
        return Promise.resolve(setZoom(a.factor));
      case "set_desktop_viewport":
        return Promise.resolve(setDesktopViewport(a.desktop === true || a.desktop === "true" || a.desktop === 1 || a.desktop === "1"));
      case "execute_js":
        return Promise.resolve(runExpression(a.code));
      default:
        return Promise.resolve("UNKNOWN_COMMAND");
    }
  });
})();
