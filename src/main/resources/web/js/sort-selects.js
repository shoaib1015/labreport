// Sort select options while preserving a leading placeholder option.
(function(){
  function isPlaceholderOption(opt) {
    if (!opt) return false;
    var v = (opt.value || '').trim();
    var t = (opt.text || '').trim();
    if (v === '') return true; // empty value placeholder
    if (/^select/i.test(t)) return true;
    if (/^choose/i.test(t)) return true;
    if (/^loading/i.test(t)) return true;
    return false;
  }

  function sortSelectOptions(select) {
    if (!select || !(select instanceof HTMLSelectElement)) return;
    var opts = Array.from(select.options);
    if (opts.length <= 1) return;

    // preserve leading placeholder options (one only)
    var leading = [];
    if (isPlaceholderOption(opts[0])) {
      leading.push(opts[0]);
      opts = opts.slice(1);
    }

    // Sort remaining options by visible text (case-insensitive, locale-aware)
    opts.sort(function(a,b){
      return a.text.localeCompare(b.text, undefined, {sensitivity: 'base'});
    });

    // Rebuild options
    select.innerHTML = '';
    leading.concat(opts).forEach(function(o){
      select.appendChild(o);
    });
  }

  function sortAllSelects() {
    var selects = document.querySelectorAll('select');
    selects.forEach(function(s){
      // skip selects explicitly opting out
      if (s.hasAttribute('data-no-sort')) return;
      sortSelectOptions(s);
    });
  }

  // Expose utility globally for dynamic usage
  window.sortSelectOptions = sortSelectOptions;
  window.sortAllSelects = sortAllSelects;

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', sortAllSelects);
  } else {
    // run immediately if already ready
    setTimeout(sortAllSelects, 0);
  }
})();
