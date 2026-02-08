/**
 * Navigation Toggle Buttons
 * Adds toggle buttons to header to hide/show left and right navigation panels
 */

(function () {
    'use strict';

    // Wait for DOM to be ready
    document.addEventListener('DOMContentLoaded', function () {
        console.log('Navigation toggle script loaded');

        const header = document.querySelector('.md-header');
        const leftNav = document.querySelector('.md-sidebar--primary');
        const rightNav = document.querySelector('.md-sidebar--secondary');
        const mainContent = document.querySelector('.md-main__inner');

        if (!header) {
            console.warn('Could not find .md-header');
            return;
        }

        if (!mainContent) {
            console.warn('Could not find .md-main__inner');
            return;
        }

        console.log('Header found:', header);
        console.log('Left nav found:', !!leftNav);
        console.log('Right nav found:', !!rightNav);

        // Create toggle buttons container
        const toggleContainer = document.createElement('div');
        toggleContainer.className = 'md-header__nav-toggle';
        toggleContainer.style.cssText = 'display: flex; gap: 0.5rem; margin-left: auto;';

        // Create left navigation toggle button
        const leftToggle = document.createElement('button');
        leftToggle.className = 'md-header__nav-toggle-btn md-header__button';
        leftToggle.setAttribute('aria-label', 'Toggle left navigation');
        leftToggle.setAttribute('type', 'button');
        leftToggle.setAttribute('title', 'Toggle left navigation');
        leftToggle.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24"><path d="M3 18h18v-2H3v2zm0-5h18v-2H3v2zm0-7v2h18V6H3z"/></svg>';
        leftToggle.style.cssText = 'background: none; border: none; color: inherit; cursor: pointer; padding: 0.5rem; display: flex; align-items: center; justify-content: center; width: auto; height: auto;';

        // Create right navigation toggle button
        const rightToggle = document.createElement('button');
        rightToggle.className = 'md-header__nav-toggle-btn md-header__button';
        rightToggle.setAttribute('aria-label', 'Toggle right navigation');
        rightToggle.setAttribute('type', 'button');
        rightToggle.setAttribute('title', 'Toggle right navigation');
        rightToggle.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24"><path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-5 14H5v-2h9v2zm2-4H5v-2h11v2zm0-4H5V7h11v2z"/></svg>';
        rightToggle.style.cssText = 'background: none; border: none; color: inherit; cursor: pointer; padding: 0.5rem; display: flex; align-items: center; justify-content: center; width: auto; height: auto;';

        // Add hover effects
        [leftToggle, rightToggle].forEach(btn => {
            btn.addEventListener('mouseenter', function () {
                this.style.opacity = '0.7';
            });
            btn.addEventListener('mouseleave', function () {
                this.style.opacity = '1';
            });
        });

        // Left navigation toggle functionality
        let leftNavHidden = false;
        leftToggle.addEventListener('click', function () {
            leftNavHidden = !leftNavHidden;

            if (leftNav) {
                if (leftNavHidden) {
                    leftNav.style.display = 'none';
                    leftNav.setAttribute('data-hidden', 'true');
                    leftToggle.style.opacity = '0.5';
                } else {
                    leftNav.style.display = '';
                    leftNav.removeAttribute('data-hidden');
                    leftToggle.style.opacity = '1';
                }
            }

            // Save state to localStorage
            localStorage.setItem('md-nav-left-hidden', leftNavHidden);
        });

        // Right navigation toggle functionality
        let rightNavHidden = false;
        rightToggle.addEventListener('click', function () {
            rightNavHidden = !rightNavHidden;

            if (rightNav) {
                if (rightNavHidden) {
                    rightNav.style.display = 'none';
                    rightNav.setAttribute('data-hidden', 'true');
                    rightToggle.style.opacity = '0.5';
                } else {
                    rightNav.style.display = '';
                    rightNav.removeAttribute('data-hidden');
                    rightToggle.style.opacity = '1';
                }
            }

            // Save state to localStorage
            localStorage.setItem('md-nav-right-hidden', rightNavHidden);
        });

        // Restore saved state from localStorage
        const savedLeftHidden = localStorage.getItem('md-nav-left-hidden') === 'true';
        const savedRightHidden = localStorage.getItem('md-nav-right-hidden') === 'true';

        if (savedLeftHidden && leftNav) {
            leftNav.style.display = 'none';
            leftNav.setAttribute('data-hidden', 'true');
            leftNavHidden = true;
            leftToggle.style.opacity = '0.5';
        }

        if (savedRightHidden && rightNav) {
            rightNav.style.display = 'none';
            rightNav.setAttribute('data-hidden', 'true');
            rightNavHidden = true;
            rightToggle.style.opacity = '0.5';
        }

        // Add buttons to container
        toggleContainer.appendChild(leftToggle);
        toggleContainer.appendChild(rightToggle);

        // Find the header inner container
        const headerInner = header.querySelector('.md-header__inner');
        if (!headerInner) {
            console.warn('Could not find .md-header__inner');
            return;
        }

        // Material theme structure: .md-header__inner contains:
        // - .md-header__title (or .md-header__button--home)
        // - .md-header__button (menu toggle, etc.)
        // We want to insert our buttons before the existing buttons but after the title

        // Find existing button container or create insertion point
        const title = headerInner.querySelector('.md-header__title, .md-header__button--home');
        const existingButtons = Array.from(headerInner.querySelectorAll('.md-header__button')).filter(
            btn => !btn.classList.contains('md-header__button--home')
        );

        if (existingButtons.length > 0) {
            // Insert before the first existing button
            headerInner.insertBefore(toggleContainer, existingButtons[0]);
        } else if (title && title.nextSibling) {
            // Insert after title if there are siblings
            headerInner.insertBefore(toggleContainer, title.nextSibling);
        } else {
            // Append to end
            headerInner.appendChild(toggleContainer);
        }

        // Ensure visibility and proper styling
        toggleContainer.style.display = 'flex';
        toggleContainer.style.visibility = 'visible';
        toggleContainer.style.opacity = '1';
        toggleContainer.style.order = '999'; // Ensure it appears at the end

        console.log('Toggle buttons added to header at:', toggleContainer.parentElement);
    });
})();
