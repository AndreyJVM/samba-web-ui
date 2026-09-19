/**
 * Валидация совпадения паролей в формах
 * Ожидает форму с id, поля password (или newPassword) и confirmPassword, а также alert-box mismatchAlert
 */
document.addEventListener("DOMContentLoaded", function() {
    // Determine the form ID
    let form = document.getElementById('createForm') || document.getElementById('pwdForm');
    
    if (form) {
        form.addEventListener('submit', function(e) {
            let p1Input = document.getElementById('password') || document.getElementById('newPassword');
            let p2Input = document.getElementById('confirmPassword');
            let alertBox = document.getElementById('mismatchAlert');
            
            if (p1Input && p2Input && alertBox) {
                if (p1Input.value !== p2Input.value) {
                    e.preventDefault();
                    alertBox.classList.remove('d-none');
                } else {
                    alertBox.classList.add('d-none');
                }
            }
        });
    }
});