/**
 * JS-логика для страницы списка групп
 */
function openDeleteModal(name) {
    const targetEl = document.getElementById('deleteTargetGroup');
    const inputEl = document.getElementById('deleteGroupNameInput');
    const modalEl = document.getElementById('deleteGroupModal');
    
    if (targetEl && inputEl && modalEl) {
        targetEl.textContent = name;
        inputEl.value = name;
        new bootstrap.Modal(modalEl).show();
    }
}