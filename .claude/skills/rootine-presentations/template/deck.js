/* Rootine deck runtime. No dependencies.
   Keys: Right/Space/Down next, Left/Up previous, Home/End, F fullscreen,
   N speaker notes, T seconds badge. URL hash (#3) opens slide 3. ?export hides the counter. */
(function () {
  var stage = document.querySelector('.stage');
  var slides = Array.prototype.slice.call(document.querySelectorAll('.slide'));
  var counter = document.querySelector('.counter');
  var i = 0;

  if (/[?&]export\b/.test(location.search)) document.body.classList.add('exporting');

  slides.forEach(function (el) {
    var secs = el.getAttribute('data-seconds');
    if (secs) {
      var b = document.createElement('div');
      b.className = 'badge';
      b.textContent = secs + ' s';
      el.appendChild(b);
    }
  });

  function fit() {
    var s = Math.min(window.innerWidth / 1920, window.innerHeight / 1080);
    stage.style.transform = 'translate(-50%,-50%) scale(' + s + ')';
  }
  function go(n) {
    i = Math.max(0, Math.min(slides.length - 1, n));
    slides.forEach(function (el, k) {
      el.classList.toggle('is-active', k === i);
      el.setAttribute('aria-hidden', k === i ? 'false' : 'true');
    });
    counter.textContent = (i + 1) + ' / ' + slides.length;
    try { history.replaceState(null, '', location.search + '#' + (i + 1)); } catch (e) {}
  }
  function fullscreen() {
    if (document.fullscreenElement) document.exitFullscreen();
    else if (document.documentElement.requestFullscreen) document.documentElement.requestFullscreen();
  }
  document.addEventListener('keydown', function (e) {
    if (e.ctrlKey || e.metaKey || e.altKey) return;
    var k = e.key;
    if (k === 'ArrowRight' || k === 'ArrowDown' || k === ' ' || k === 'PageDown') { e.preventDefault(); go(i + 1); }
    else if (k === 'ArrowLeft' || k === 'ArrowUp' || k === 'PageUp') { e.preventDefault(); go(i - 1); }
    else if (k === 'Home') go(0);
    else if (k === 'End') go(slides.length - 1);
    else if (k === 'f' || k === 'F') fullscreen();
    else if (k === 'n' || k === 'N') document.body.classList.toggle('show-notes');
    else if (k === 't' || k === 'T') document.body.classList.toggle('show-time');
  });
  window.addEventListener('resize', fit);
  fit();
  go((parseInt((location.hash || '').slice(1), 10) || 1) - 1);
})();
