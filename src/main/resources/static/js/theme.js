// Bascule entre le mode sombre (défaut) et le mode jour.
// Le mode est porté par l'attribut data-bs-theme de <html> (posé avant le rendu
// par le fragment header) et mémorisé dans le navigateur sous « gj-theme ».
(function () {
    // Le bouton annonce le mode vers lequel il bascule.
    function majBouton(bouton, theme) {
        bouton.textContent = theme === 'light' ? '🌙 Sombre' : '☀️ Jour';
        bouton.setAttribute('aria-label', theme === 'light' ? 'Passer en mode sombre' : 'Passer en mode jour');
    }

    document.addEventListener('DOMContentLoaded', function () {
        var bouton = document.getElementById('gj-theme-bascule');
        if (!bouton) {
            return;
        }
        var racine = document.documentElement;
        majBouton(bouton, racine.getAttribute('data-bs-theme'));

        bouton.addEventListener('click', function () {
            var theme = racine.getAttribute('data-bs-theme') === 'light' ? 'dark' : 'light';
            racine.setAttribute('data-bs-theme', theme);
            majBouton(bouton, theme);
            try {
                localStorage.setItem('gj-theme', theme);
            } catch (e) {
                // Stockage indisponible : le choix vaut pour cette page seulement.
            }
        });
    });
})();
