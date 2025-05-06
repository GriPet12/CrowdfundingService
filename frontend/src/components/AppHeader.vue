<template>
  <header class="app-header">
    <div class="logo">
      <router-link to="/" class="logo-link">Logo</router-link>
    </div>

    <nav class="nav-menu">
      <router-link to="/" class="nav-link" active-class="active">Home</router-link>
    </nav>

    <div class="mobile-menu-button" @click="toggleMobileMenu">
      <div class="bar"></div>
      <div class="bar"></div>
      <div class="bar"></div>
    </div>

    <div class="mobile-nav" :class="{ 'open': mobileMenuOpen }">
      <router-link to="/" class="mobile-link" @click="closeMobileMenu">Home</router-link>
    </div>
  </header>
</template>

<script>
export default {
  name: 'HeaderApp',
  data() {
    return {
      mobileMenuOpen: false
    };
  },
  methods: {
    toggleMobileMenu() {
      this.mobileMenuOpen = !this.mobileMenuOpen;
    },
    closeMobileMenu() {
      this.mobileMenuOpen = false;
    }
  }
};
</script>

<style scoped>
.app-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 5%;
  height: 70px;
  background-color: white;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
  position: sticky;
  top: 0;
  z-index: 100;
}

.logo {
  font-size: 24px;
  font-weight: bold;
}

.logo-link {
  text-decoration: none;
  color: #333;
}

.nav-menu {
  display: flex;
  gap: 20px;
}

.nav-link {
  text-decoration: none;
  color: #333;
  padding: 8px 12px;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.nav-link:hover {
  background-color: #f1f1f1;
}

.nav-link.active {
  color: #4caf50;
  font-weight: 600;
}

.mobile-menu-button {
  display: none;
  flex-direction: column;
  gap: 5px;
  cursor: pointer;
}

.bar {
  width: 25px;
  height: 3px;
  background-color: #333;
  border-radius: 2px;
}

.mobile-nav {
  display: none;
  position: fixed;
  top: 70px;
  left: 0;
  width: 100%;
  background-color: white;
  flex-direction: column;
  box-shadow: 0 5px 10px rgba(0,0,0,0.1);
  transform: translateY(-100%);
  transition: transform 0.3s ease-in-out;
  z-index: 99;
}

.mobile-nav.open {
  transform: translateY(0);
}

.mobile-link {
  text-decoration: none;
  color: #333;
  padding: 15px 20px;
  border-bottom: 1px solid #eee;
  display: block;
}

.mobile-link:hover {
  background-color: #f9f9f9;
}

@media (max-width: 768px) {
  .nav-menu {
    display: none;
  }

  .mobile-menu-button {
    display: flex;
  }

  .mobile-nav {
    display: flex;
  }
}
</style>