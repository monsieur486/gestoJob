// Formulaire de suppression d'annonce en attente de confirmation via le modal.
let formuleSuppressionAnnonce = null;

// Ouvre le modal de confirmation ; mémorise le formulaire de suppression à soumettre.
function demanderSuppressionAnnonce(buttonEl) {
    formuleSuppressionAnnonce = buttonEl.closest("form");
    const modalEl = document.getElementById("confirmSuppressionAnnonceModal");

    // Repli : si Bootstrap ou le modal manquent, confirmation native (OK = supprime).
    if (!modalEl || typeof bootstrap === "undefined") {
        if (formuleSuppressionAnnonce && window.confirm("Supprimer définitivement cette annonce ?")) {
            formuleSuppressionAnnonce.submit();
        }
        return;
    }

    bootstrap.Modal.getOrCreateInstance(modalEl).show();
}

document.addEventListener("DOMContentLoaded", function () {
    const modalEl = document.getElementById("confirmSuppressionAnnonceModal");
    const confirmBtn = document.getElementById("confirmSuppressionAnnonceBtn");

    if (confirmBtn) {
        confirmBtn.addEventListener("click", function () {
            if (formuleSuppressionAnnonce) {
                formuleSuppressionAnnonce.submit();
            }
        });
    }

    // À chaque ouverture, redonne le focus à « Annuler » (« cancel par défaut »).
    if (modalEl) {
        modalEl.addEventListener("shown.bs.modal", function () {
            const annuler = modalEl.querySelector(".modal-footer .btn-secondary");
            if (annuler) annuler.focus();
        });
    }
});

function showToast(message, variant) {
    const toastEl = document.getElementById("appToast");
    const bodyEl = document.getElementById("appToastBody");
    if (!toastEl || !bodyEl || typeof bootstrap === "undefined") {
        return;
    }

    bodyEl.textContent = message;

    toastEl.classList.remove("text-bg-dark", "text-bg-success", "text-bg-danger", "text-bg-warning", "text-bg-info");
    switch (variant) {
        case "success":
            toastEl.classList.add("text-bg-success");
            break;
        case "danger":
            toastEl.classList.add("text-bg-danger");
            break;
        case "warning":
            toastEl.classList.add("text-bg-warning");
            break;
        case "info":
            toastEl.classList.add("text-bg-info");
            break;
        default:
            toastEl.classList.add("text-bg-dark");
    }

    bootstrap.Toast.getOrCreateInstance(toastEl, { delay: 1800 }).show();
}

async function copyAnnonceTxtToClipboard(buttonEl) {
    const annonceId = buttonEl.getAttribute("data-annonce-id");
    if (!annonceId) return;

    buttonEl.disabled = true;

    try {
        const response = await fetch(`/contenu/${annonceId}`, {
            method: "GET",
            headers: { "Accept": "text/plain" }
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        const text = await response.text();

        if (navigator.clipboard && navigator.clipboard.writeText) {
            await navigator.clipboard.writeText(text);
        } else {
            const ta = document.createElement("textarea");
            ta.value = text;
            ta.style.position = "fixed";
            ta.style.left = "-9999px";
            document.body.appendChild(ta);
            ta.focus();
            ta.select();
            document.execCommand("copy");
            document.body.removeChild(ta);
        }

        showToast("Texte copié dans le presse-papier.", "success");

        buttonEl.textContent = "Copié";
        setTimeout(() => (buttonEl.textContent = "Txt"), 1200);
    } catch (e) {
        console.error("Erreur copie TXT annonce:", e);
        showToast("Impossible de copier le texte.", "danger");

        buttonEl.textContent = "Erreur";
        setTimeout(() => (buttonEl.textContent = "Txt"), 1500);
    } finally {
        buttonEl.disabled = false;
    }
}

// Éditeurs Quill des modèles d'email : barre limitée au gras, italique,
// soulignement et aux listes à puce.
// Les boutons [data-variable] du formulaire insèrent leur variable au curseur ;
// à la soumission, le HTML est recopié dans le champ caché ciblé, et l'envoi est
// bloqué s'il manque une de ces variables (le serveur fait la même vérification).
if (typeof Quill !== 'undefined') {
    document.querySelectorAll('.editeur-modele').forEach(function (zone) {
        var editeur = new Quill(zone, {
            theme: 'snow',
            modules: {toolbar: [['bold', 'italic', 'underline'], [{list: 'bullet'}]]}
        });
        var champ = document.getElementById(zone.getAttribute('data-cible'));
        var form = zone.closest('form');
        if (!form || !champ) {
            return;
        }
        var boutons = form.querySelectorAll('[data-variable]');
        boutons.forEach(function (bouton) {
            bouton.addEventListener('click', function () {
                var variable = bouton.getAttribute('data-variable');
                // getSelection(true) redonne le focus à l'éditeur ; une sélection est remplacée.
                var plage = editeur.getSelection(true);
                editeur.deleteText(plage.index, plage.length, 'user');
                editeur.insertText(plage.index, variable, 'user');
                editeur.setSelection(plage.index + variable.length, 0, 'user');
            });
        });
        form.addEventListener('submit', function (evenement) {
            champ.value = editeur.root.innerHTML;
            var manquantes = Array.prototype.map.call(boutons, function (bouton) {
                return bouton.getAttribute('data-variable');
            }).filter(function (variable) {
                return champ.value.indexOf(variable) === -1;
            });
            if (manquantes.length > 0) {
                evenement.preventDefault();
                var message = 'Variable(s) manquante(s) : ' + manquantes.join(', ')
                    + '. Le modèle n\'a pas été enregistré.';
                if (document.getElementById('appToast') && typeof bootstrap !== 'undefined') {
                    showToast(message, 'danger');
                } else {
                    window.alert(message);
                }
            }
        });
    });
}
