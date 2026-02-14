// MedExpertMatch UI - simulated user selector for role-based menu visibility

const MEDEXPERTMATCH_TEST_USERS = [
    {
        id: 'medexpertmatch-user-001',
        email: 'user@example.com',
        roles: ['ROLE_USER'],
        displayName: 'Regular User'
    },
    {
        id: 'medexpertmatch-admin-002',
        email: 'admin@example.com',
        roles: ['ROLE_USER', 'ROLE_ADMIN'],
        displayName: 'Administrator'
    }
];

const MEDEXPERTMATCH_DEFAULT_USER = MEDEXPERTMATCH_TEST_USERS[0];

const STORAGE_KEY = 'medexpertmatch-selected-user';

function getCurrentUser() {
    if (typeof window === 'undefined') {
        return MEDEXPERTMATCH_DEFAULT_USER;
    }
    try {
        const stored = localStorage.getItem(STORAGE_KEY);
        if (stored) {
            const user = JSON.parse(stored);
            const found = MEDEXPERTMATCH_TEST_USERS.find(function (u) {
                return u.id === user.id;
            });
            if (found) {
                return found;
            }
        }
    } catch (e) {
        console.error('Failed to load user from localStorage:', e);
    }
    return MEDEXPERTMATCH_DEFAULT_USER;
}

function setCurrentUser(user) {
    if (typeof window === 'undefined') {
        return;
    }
    try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(user));
        updateUserDisplay();
    } catch (e) {
        console.error('Failed to save user to localStorage:', e);
    }
}

function updateUserDisplay() {
    const currentUser = getCurrentUser();
    const displayElement = document.getElementById('currentUserDisplay');
    if (displayElement) {
        displayElement.textContent = currentUser.displayName;
    }
}

function initializeUserSelector() {
    var currentUser = getCurrentUser();
    var url = new URL(window.location.href);
    var urlUser = url.searchParams.get('user');
    if (currentUser.roles && currentUser.roles.indexOf('ROLE_ADMIN') !== -1 && urlUser !== 'admin') {
        url.searchParams.set('user', 'admin');
        window.location.replace(url.toString());
        return;
    }
    if (!currentUser.roles || currentUser.roles.indexOf('ROLE_ADMIN') === -1) {
        if (urlUser === 'admin') {
            url.searchParams.delete('user');
            window.location.replace(url.toString());
            return;
        }
    }

    var menu = document.getElementById('userSelectorMenu');
    if (!menu) {
        updateUserDisplay();
        return;
    }

    menu.innerHTML = '';

    MEDEXPERTMATCH_TEST_USERS.forEach(function (user) {
        var li = document.createElement('li');
        var button = document.createElement('button');
        button.className = 'dropdown-item';
        button.type = 'button';

        if (getCurrentUser().id === user.id) {
            button.classList.add('active');
        }

        button.innerHTML = '<div class="fw-bold">' + user.displayName + '</div>' +
            '<div class="small text-muted">' + (user.email || 'No email') + '</div>';

        button.addEventListener('click', function () {
            setCurrentUser(user);
            var targetUrl = new URL(window.location.href);
            if (user.roles && user.roles.indexOf('ROLE_ADMIN') !== -1) {
                targetUrl.searchParams.set('user', 'admin');
            } else {
                targetUrl.searchParams.delete('user');
            }
            window.location.href = targetUrl.toString();
        });

        li.appendChild(button);
        menu.appendChild(li);
    });

    updateUserDisplay();
}

function initUserSelector() {
    initializeUserSelector();
}

if (typeof document !== 'undefined') {
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initUserSelector);
    } else {
        initUserSelector();
    }
}
