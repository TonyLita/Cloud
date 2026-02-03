<template>
  <div class="login-container">
    <h2>Connexion</h2>
    <form @submit.prevent="handleLogin">
      <div class="form-group">
        <label>Email:</label>
        <input type="email" v-model="email" required>
      </div>
      <div class="form-group">
        <label>Mot de passe:</label>
        <input type="password" v-model="password" required>
      </div>
      <button type="submit">Se connecter</button>
    </form>
    <p v-if="error" class="error">{{ error }}</p>
    <router-link to="/register">Pas encore de compte ? S'inscrire</router-link>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'LoginUser',
  data() {
    return {
      email: '',
      password: '',
      error: ''
    }
  },
  methods: {
    async handleLogin() {
      try {
        const response = await axios.post('http://localhost:8080/api/users/login', {
          email: this.email,
          passwordHash: this.password
        }, { withCredentials: true });
        
        if (response.data) {
          this.$router.push('/accueil');
        }
      } catch (err) {
        if (err.response && err.response.status === 403) {
          this.error = err.response.data || 'Votre compte est bloqué.';
        } else {
          this.error = 'Identifiants invalides';
        }
      }
    }
  }
}
</script>

<style scoped>
.login-container { max-width: 400px; margin: auto; padding: 20px; border: 1px solid #ddd; border-radius: 8px; }
.form-group { margin-bottom: 15px; text-align: left; }
input { width: 100%; padding: 8px; margin-top: 5px; }
button { width: 100%; padding: 10px; background: #42b983; color: white; border: none; cursor: pointer; }
.error { color: red; margin-top: 10px; }
</style>
